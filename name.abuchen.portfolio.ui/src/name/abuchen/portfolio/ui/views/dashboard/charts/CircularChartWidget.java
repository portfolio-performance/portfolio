package name.abuchen.portfolio.ui.views.dashboard.charts;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swtchart.ICircularSeries;
import org.eclipse.swtchart.ISeries;
import org.eclipse.swtchart.ISeries.SeriesType;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.chart.CircularChart;
import name.abuchen.portfolio.ui.util.chart.CircularChart.RenderLabelsCenteredInPie;
import name.abuchen.portfolio.ui.util.chart.CircularChartToolTip;
import name.abuchen.portfolio.ui.views.dashboard.ChartHeightConfig;
import name.abuchen.portfolio.ui.views.dashboard.ClientFilterConfig;
import name.abuchen.portfolio.ui.views.dashboard.DashboardData;
import name.abuchen.portfolio.ui.views.dashboard.DashboardResources;
import name.abuchen.portfolio.ui.views.dashboard.WidgetDelegate;
import name.abuchen.portfolio.util.TextUtil;

public abstract class CircularChartWidget<T> extends WidgetDelegate<T>
{
    private Label title;
    private CircularChart chart;

    protected CircularChartWidget(Widget widget, DashboardData data)
    {
        super(widget, data);

        addConfig(new ClientFilterConfig(this));
        addConfig(new ChartHeightConfig(this));
    }

    protected CircularChart getChart()
    {
        return chart;
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).applyTo(container);

        title = new Label(container, SWT.NONE);
        title.setText(TextUtil.tooltip(getWidget().getLabel()));
        GridDataFactory.fillDefaults().grab(true, false).applyTo(title);

        chart = new CircularChart(container, SeriesType.DOUGHNUT);
        chart.getTitle().setVisible(false);
        chart.addLabelPainter(new RenderLabelsCenteredInPie(chart,
                        n -> Values.Percent2.format(n.getValue() / n.getParent().getValue())));

        int yHint = get(ChartHeightConfig.class).getPixel();
        GridDataFactory.fillDefaults().hint(SWT.DEFAULT, yHint).grab(true, false).applyTo(chart);

        configureTooltip(chart.getToolTip());

        // Create an empty series to avoid SWTchart painting an (also empty)
        // line chart grid.
        ICircularSeries<?> circularSeries = (ICircularSeries<?>) chart.getSeriesSet().createSeries(SeriesType.DOUGHNUT,
                        Messages.ClientEditorLabelChart);
        circularSeries.setSeries(new String[] {}, new double[] {});

        getDashboardData().getStylingEngine().style(chart);

        container.layout();

        return container;
    }

    protected abstract void configureTooltip(CircularChartToolTip toolTip);

    @Override
    public void update(T object)
    {
        title.setText(TextUtil.tooltip(getWidget().getLabel()));

        try
        {
            chart.suspendUpdate(true);

            get(ChartHeightConfig.class).updateGridData(chart, title.getParent());

            chart.getTitle().setText(title.getText());

            for (ISeries<?> s : chart.getSeriesSet().getSeries())
                chart.getSeriesSet().deleteSeries(s.getId());

            createCircularSeries(object);

            chart.updateAngleBounds();
        }
        finally
        {
            chart.suspendUpdate(false);
        }
        chart.redraw();
    }

    protected abstract void createCircularSeries(T object);

    @Override
    public Control getTitleControl()
    {
        return title;
    }

}
