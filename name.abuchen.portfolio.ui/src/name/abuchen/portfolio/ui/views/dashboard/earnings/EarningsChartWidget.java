package name.abuchen.portfolio.ui.views.dashboard.earnings;

import java.util.function.Supplier;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.swtchart.Chart;
import org.swtchart.IAxis;
import org.swtchart.ISeries;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.snapshot.filter.ClientFilter;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.CacheKey;
import name.abuchen.portfolio.ui.util.chart.PlainChart;
import name.abuchen.portfolio.ui.views.dashboard.ChartHeightConfig;
import name.abuchen.portfolio.ui.views.dashboard.ChartShowYAxisConfig;
import name.abuchen.portfolio.ui.views.dashboard.ClientFilterConfig;
import name.abuchen.portfolio.ui.views.dashboard.DashboardData;
import name.abuchen.portfolio.ui.views.dashboard.DashboardResources;
import name.abuchen.portfolio.ui.views.dashboard.WidgetDelegate;
import name.abuchen.portfolio.ui.views.payments.PaymentsChartBuilder;
import name.abuchen.portfolio.ui.views.payments.PaymentsPerMonthChartBuilder;
import name.abuchen.portfolio.ui.views.payments.PaymentsPerQuarterChartBuilder;
import name.abuchen.portfolio.ui.views.payments.PaymentsPerYearChartBuilder;
import name.abuchen.portfolio.ui.views.payments.PaymentsViewModel;
import name.abuchen.portfolio.util.TextUtil;

public class EarningsChartWidget extends WidgetDelegate<PaymentsViewModel>
{
    private Label title;
    private Chart chart;
    private PaymentsChartBuilder chartBuilder;

    private CurrencyConverter converter;

    private EarningsChartWidget(Widget widget, DashboardData data, PaymentsChartBuilder chartBuilder,
                    int defaultYearOffset)
    {
        super(widget, data);
        this.chartBuilder = chartBuilder;

        addConfig(new ChartShowYAxisConfig(this, true));

        addConfig(new ClientFilterConfig(this));
        addConfig(new StartYearConfig(this, defaultYearOffset));
        addConfig(new EarningTypeConfig(this));
        addConfig(new GrossNetTypeConfig(this));
        addConfig(new ChartHeightConfig(this));

        this.converter = data.getCurrencyConverter();
    }

    public static EarningsChartWidget perYear(Widget widget, DashboardData data)
    {
        return new EarningsChartWidget(widget, data, new PaymentsPerYearChartBuilder(), 10);
    }

    public static EarningsChartWidget perQuarter(Widget widget, DashboardData data)
    {
        return new EarningsChartWidget(widget, data, new PaymentsPerQuarterChartBuilder(), 4);
    }

    public static EarningsChartWidget perMonth(Widget widget, DashboardData data)
    {
        return new EarningsChartWidget(widget, data, new PaymentsPerMonthChartBuilder(), 4);
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        Composite container = new Composite(parent, SWT.NONE);
        container.setData(UIConstants.CSS.CLASS_NAME, this.getContainerCssClassNames());
        GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).applyTo(container);
        container.setBackground(parent.getBackground());

        title = new Label(container, SWT.NONE);
        title.setBackground(container.getBackground());
        title.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.TITLE);
        title.setText(TextUtil.tooltip(getWidget().getLabel()));
        GridDataFactory.fillDefaults().grab(true, false).applyTo(title);

        chart = new PlainChart(container, SWT.NONE);
        chart.setData(UIConstants.CSS.CLASS_NAME, "chart"); //$NON-NLS-1$
        getDashboardData().getStylingEngine().style(chart);

        chart.setBackground(container.getBackground());
        chart.getTitle().setVisible(false);
        chart.getTitle().setText(title.getText());

        chart.getLegend().setVisible(false);

        chartBuilder.configure(chart);

        IAxis yAxis = chart.getAxisSet().getYAxis(0);
        yAxis.getTick().setVisible(get(ChartShowYAxisConfig.class).getIsShowYAxis());

        int yHint = get(ChartHeightConfig.class).getPixel();
        GridDataFactory.fillDefaults().hint(SWT.DEFAULT, yHint).grab(true, false).applyTo(chart);

        chart.getPlotArea().addTraverseListener(event -> event.doit = true);

        container.layout();

        return container;
    }

    @Override
    public Supplier<PaymentsViewModel> getUpdateTask()
    {
        ClientFilter clientFilter = get(ClientFilterConfig.class).getSelectedFilter();
        int startYear = get(StartYearConfig.class).getStartYear();
        EarningType earningsType = get(EarningTypeConfig.class).getValue();
        GrossNetType grossNetType = get(GrossNetTypeConfig.class).getValue();

        CacheKey key = new CacheKey(PaymentsViewModel.class, clientFilter, startYear, earningsType, grossNetType);

        return () -> (PaymentsViewModel) getDashboardData().getCache().computeIfAbsent(key, k -> {
            PaymentsViewModel model = new PaymentsViewModel(converter, getClient());

            PaymentsViewModel.Mode mode = PaymentsViewModel.Mode.EARNINGS;
            if (earningsType == EarningType.DIVIDENDS)
                mode = PaymentsViewModel.Mode.DIVIDENDS;
            else if (earningsType == EarningType.INTEREST)
                mode = PaymentsViewModel.Mode.INTEREST;

            model.configure(startYear, mode, grossNetType == GrossNetType.GROSS, false);
            model.setFilteredClient(clientFilter.filter(getClient()));
            model.recalculate();
            return model;
        });
    }

    @Override
    public void update(PaymentsViewModel model)
    {
        try
        {
            title.setText(TextUtil.tooltip(getWidget().getLabel()));

            chart.suspendUpdate(true);

            GridData data = (GridData) chart.getLayoutData();

            int oldHeight = data.heightHint;
            int newHeight = get(ChartHeightConfig.class).getPixel();

            if (oldHeight != newHeight)
            {
                data.heightHint = newHeight;
                title.getParent().layout(true);
                title.getParent().getParent().layout(true);
            }

            chart.getTitle().setText(title.getText());

            for (ISeries s : chart.getSeriesSet().getSeries())
                chart.getSeriesSet().deleteSeries(s.getId());

            chart.getAxisSet().getYAxis(0).getTick().setVisible(get(ChartShowYAxisConfig.class).getIsShowYAxis());

            chartBuilder.createSeries(chart, model);

            chart.getAxisSet().adjustRange();
        }
        finally
        {
            chart.suspendUpdate(false);
        }
        chart.redraw();
    }

    @Override
    public Control getTitleControl()
    {
        return title;
    }
}
