package name.abuchen.portfolio.ui.views.dashboard.charts;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.function.Supplier;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.chart.TimelineChart;
import name.abuchen.portfolio.ui.util.format.AxisTickPercentNumberFormat;
import name.abuchen.portfolio.ui.views.dashboard.ChartHeightConfig;
import name.abuchen.portfolio.ui.views.dashboard.ChartShowYAxisConfig;
import name.abuchen.portfolio.ui.views.dashboard.DashboardData;
import name.abuchen.portfolio.ui.views.dashboard.DashboardResources;
import name.abuchen.portfolio.ui.views.dashboard.DataSeriesConfig;
import name.abuchen.portfolio.ui.views.dashboard.ReportingPeriodConfig;
import name.abuchen.portfolio.ui.views.dashboard.WidgetDelegate;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries;
import name.abuchen.portfolio.ui.views.dataseries.DataSeriesCache;
import name.abuchen.portfolio.util.Interval;
import name.abuchen.portfolio.util.TextUtil;

public class DrawdownChartWidget extends WidgetDelegate<Object>
{
    private Label title;
    private TimelineChart chart;
    private static final Color colorDrawdown = Display.getDefault().getSystemColor(SWT.COLOR_RED);

    public DrawdownChartWidget(Widget widget, DashboardData dashboardData)
    {
        super(widget, dashboardData);

        addConfig(new DataSeriesConfig(this, true));
        addConfig(new ReportingPeriodConfig(this));
        addConfig(new ChartShowYAxisConfig(this, true));
        addConfig(new ChartHeightConfig(this));
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).applyTo(container);

        title = new Label(container, SWT.NONE);
        title.setText(TextUtil.tooltip(getWidget().getLabel()));
        GridDataFactory.fillDefaults().grab(true, false).applyTo(title);

        chart = new TimelineChart(container);
        chart.getTitle().setVisible(false);
        chart.getTitle().setText(title.getText());
        chart.getAxisSet().getYAxis(0).getTick().setVisible(get(ChartShowYAxisConfig.class).getIsShowYAxis());
        chart.getToolTip().setDefaultValueFormat(new DecimalFormat("0.##%")); //$NON-NLS-1$
        chart.getToolTip().reverseLabels(true);

        int yHint = get(ChartHeightConfig.class).getPixel();
        GridDataFactory.fillDefaults().hint(SWT.DEFAULT, yHint).grab(true, false).span(2, 1).applyTo(chart);

        getDashboardData().getStylingEngine().style(chart);

        container.layout();

        return container;
    }

    @Override
    public Control getTitleControl()
    {
        return title;
    }

    @Override
    public Supplier<Object> getUpdateTask()
    {
        // just fill the cache - the chart series builder will look it up and
        // pass it directly to the chart

        DataSeriesCache cache = getDashboardData().getDataSeriesCache();

        DataSeries serie = get(DataSeriesConfig.class).getDataSeries();
        Interval interval = get(ReportingPeriodConfig.class).getReportingPeriod().toInterval(LocalDate.now());

        return () -> {
            cache.lookup(serie, interval);
            return null;
        };
    }

    @Override
    public void update(Object object)
    {
        title.setText(TextUtil.tooltip(getWidget().getLabel()));

        try
        {
            chart.suspendUpdate(true);

            get(ChartHeightConfig.class).updateGridData(chart, title.getParent());

            chart.getTitle().setText(title.getText());

            for (var s : chart.getSeriesSet().getSeries())
                chart.getSeriesSet().deleteSeries(s.getId());

            chart.getAxisSet().getYAxis(0).getTick().setFormat(new AxisTickPercentNumberFormat("0.#%")); //$NON-NLS-1$
            chart.getAxisSet().getYAxis(0).getTick().setVisible(get(ChartShowYAxisConfig.class).getIsShowYAxis());

            Interval reportingPeriod = get(ReportingPeriodConfig.class).getReportingPeriod()
                            .toInterval(LocalDate.now());

            PerformanceIndex index = getDashboardData().getDataSeriesCache()
                            .lookup(get(DataSeriesConfig.class).getDataSeries(), reportingPeriod);

            addDrawdown(index);

            chart.adjustRange();
        }
        finally
        {
            chart.suspendUpdate(false);
        }
        chart.redraw();
    }

    private void addDrawdown(PerformanceIndex index)
    {
        double[] values = index.getDrawdown().getMaxDrawdownSerie();
        String lineID = get(DataSeriesConfig.class).getDataSeries().getUUID() + Messages.LabelDrawdown;
        String label = Messages.LabelDrawdown + " (" + get(DataSeriesConfig.class).getDataSeries().getLabel() + ")"; //$NON-NLS-1$//$NON-NLS-2$

        var lineSeries = chart.addDateSeries(lineID, index.getDates(), values, colorDrawdown, label);
        lineSeries.enableArea(true);
    }
}
