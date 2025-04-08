package name.abuchen.portfolio.ui.views.dashboard;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;

import com.google.common.collect.Lists;

import name.abuchen.portfolio.model.ConfigurationSet;
import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.snapshot.Aggregation;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.editor.PortfolioPart;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.chart.TimelineChart;
import name.abuchen.portfolio.ui.util.format.AmountNumberFormat;
import name.abuchen.portfolio.ui.util.format.AxisTickPercentNumberFormat;
import name.abuchen.portfolio.ui.util.format.ThousandsNumberFormat;
import name.abuchen.portfolio.ui.views.ChartViewConfig;
import name.abuchen.portfolio.ui.views.PerformanceChartView;
import name.abuchen.portfolio.ui.views.StatementOfAssetsHistoryView;
import name.abuchen.portfolio.ui.views.dataseries.BasicDataSeriesConfigurator;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries;
import name.abuchen.portfolio.ui.views.dataseries.DataSeriesCache;
import name.abuchen.portfolio.ui.views.dataseries.DataSeriesSerializer;
import name.abuchen.portfolio.ui.views.dataseries.DataSeriesSet;
import name.abuchen.portfolio.ui.views.dataseries.PerformanceChartSeriesBuilder;
import name.abuchen.portfolio.ui.views.dataseries.StatementOfAssetsSeriesBuilder;
import name.abuchen.portfolio.util.Interval;
import name.abuchen.portfolio.util.TextUtil;

public class ChartWidget extends WidgetDelegate<Object>
{
    private class ChartConfig implements WidgetConfig
    {
        private WidgetDelegate<?> delegate;
        private ConfigurationSet configSet;
        private ConfigurationSet.Configuration config;

        public ChartConfig(WidgetDelegate<?> delegate, DataSeries.UseCase useCase)
        {
            this.delegate = delegate;

            String configName = (useCase == DataSeries.UseCase.STATEMENT_OF_ASSETS ? StatementOfAssetsHistoryView.class
                            : PerformanceChartView.class).getSimpleName()
                            + BasicDataSeriesConfigurator.IDENTIFIER_POSTFIX;
            configSet = delegate.getClient().getSettings().getConfigurationSet(configName);
            String uuid = delegate.getWidget().getConfiguration().get(Dashboard.Config.CONFIG_UUID.name());
            config = configSet.lookup(uuid).orElseGet(() -> configSet.getConfigurations().findFirst()
                            .orElseGet(() -> new ConfigurationSet.Configuration(Messages.LabelNoName, ""))); //$NON-NLS-1$

            addConfig(new ChartShowYAxisConfig(delegate, false));
        }

        @Override
        public void menuAboutToShow(IMenuManager manager)
        {
            manager.appendToGroup(DashboardView.INFO_MENU_GROUP_NAME,
                            new LabelOnly(config != null ? config.getName() : Messages.LabelNoName));

            MenuManager subMenu = new MenuManager(Messages.ClientEditorLabelChart);

            this.configSet.getConfigurations().forEach(c -> {
                SimpleAction action = new SimpleAction(c.getName(), a -> {
                    config = c;
                    delegate.getWidget().getConfiguration().put(Dashboard.Config.CONFIG_UUID.name(), c.getUUID());

                    delegate.update();
                    delegate.getClient().touch();
                });
                action.setChecked(c.equals(config));
                subMenu.add(action);
            });

            manager.add(subMenu);
        }

        public String getData()
        {
            return config != null ? config.getData() : null;
        }

        public String getUUID()
        {
            return config != null ? config.getUUID() : null;
        }

        @Override
        public String getLabel()
        {
            return Messages.ClientEditorLabelChart + ": " //$NON-NLS-1$
                            + (config != null ? config.getName() : Messages.LabelNoName);
        }
    }

    private class AggregationConfig implements WidgetConfig
    {
        private WidgetDelegate<?> delegate;
        private Aggregation.Period aggregation;

        public AggregationConfig(WidgetDelegate<?> delegate)
        {
            this.delegate = delegate;

            try
            {
                String code = delegate.getWidget().getConfiguration().get(Dashboard.Config.AGGREGATION.name());
                if (code != null)
                    this.aggregation = Aggregation.Period.valueOf(code);
            }
            catch (IllegalArgumentException ignore)
            {
                PortfolioPlugin.log(ignore);
            }
        }

        @Override
        public void menuAboutToShow(IMenuManager manager)
        {
            manager.appendToGroup(DashboardView.INFO_MENU_GROUP_NAME, new LabelOnly(
                            aggregation != null ? aggregation.toString() : Messages.LabelAggregationDaily));

            MenuManager subMenu = new MenuManager(Messages.LabelAggregation);

            Action action = new SimpleAction(Messages.LabelAggregationDaily, a -> {
                aggregation = null;
                delegate.getWidget().getConfiguration().remove(Dashboard.Config.AGGREGATION.name());

                delegate.update();
                delegate.getClient().touch();
            });
            action.setChecked(aggregation == null);
            subMenu.add(action);

            Arrays.stream(Aggregation.Period.values()).forEach(a -> {
                Action menu = new SimpleAction(a.toString(), x -> {
                    aggregation = a;
                    delegate.getWidget().getConfiguration().put(Dashboard.Config.AGGREGATION.name(), a.name());

                    delegate.update();
                    delegate.getClient().touch();
                });
                menu.setChecked(aggregation == a);
                subMenu.add(menu);
            });

            manager.add(subMenu);
        }

        public Aggregation.Period getAggregation()
        {
            return aggregation;
        }

        @Override
        public String getLabel()
        {
            return Messages.LabelAggregation + ": " + //$NON-NLS-1$
                            (aggregation != null ? aggregation.toString() : Messages.LabelAggregationDaily);
        }
    }

    private DataSeries.UseCase useCase;
    private DataSeriesSet dataSeriesSet;

    private Label title;
    private TimelineChart chart;

    @Inject
    private PortfolioPart part;

    public ChartWidget(Widget widget, DashboardData dashboardData, DataSeries.UseCase useCase)
    {
        super(widget, dashboardData);

        this.useCase = useCase;
        this.dataSeriesSet = new DataSeriesSet(dashboardData.getClient(), dashboardData.getPreferences(), useCase);

        addConfig(new ChartConfig(this, useCase));
        if (useCase == DataSeries.UseCase.PERFORMANCE)
            addConfig(new AggregationConfig(this));
        addConfig(new ReportingPeriodConfig(this));
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
        if (useCase != DataSeries.UseCase.STATEMENT_OF_ASSETS)
            chart.getToolTip().setDefaultValueFormat(new DecimalFormat("0.##%")); //$NON-NLS-1$
        else
            chart.getToolTip().setDefaultValueFormat(new AmountNumberFormat());
        chart.getToolTip().reverseLabels(true);

        int yHint = get(ChartHeightConfig.class).getPixel();
        GridDataFactory.fillDefaults().hint(SWT.DEFAULT, yHint).grab(true, false).span(2, 1).applyTo(chart);

        getDashboardData().getStylingEngine().style(chart);

        HoverButton.build(title, container, chart, chart.getPlotArea().getControl()).withListener(new HyperlinkAdapter()
        {
            @Override
            public void linkActivated(HyperlinkEvent e)
            {
                Class<? extends AbstractFinanceView> view = useCase == DataSeries.UseCase.STATEMENT_OF_ASSETS //
                                ? StatementOfAssetsHistoryView.class
                                : PerformanceChartView.class;

                ChartViewConfig config = new ChartViewConfig(get(ChartConfig.class).getUUID(),
                                get(ReportingPeriodConfig.class).getReportingPeriod());

                part.activateView(view, config);
            }
        });

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

        List<DataSeries> series = new DataSeriesSerializer().fromString(dataSeriesSet,
                        get(ChartConfig.class).getData());

        Interval interval = get(ReportingPeriodConfig.class).getReportingPeriod().toInterval(LocalDate.now());

        return () -> {
            series.forEach(s -> cache.lookup(s, interval));
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

            List<DataSeries> series = Lists.reverse(
                            new DataSeriesSerializer().fromString(dataSeriesSet, get(ChartConfig.class).getData()));

            if (useCase == DataSeries.UseCase.STATEMENT_OF_ASSETS)
                chart.getAxisSet().getYAxis(0).getTick().setFormat(new ThousandsNumberFormat());
            else
                chart.getAxisSet().getYAxis(0).getTick().setFormat(new AxisTickPercentNumberFormat("0.#%")); //$NON-NLS-1$

            chart.getAxisSet().getYAxis(0).getTick().setVisible(get(ChartShowYAxisConfig.class).getIsShowYAxis());

            Interval reportingPeriod = get(ReportingPeriodConfig.class).getReportingPeriod()
                            .toInterval(LocalDate.now());

            switch (useCase)
            {
                case STATEMENT_OF_ASSETS:
                    buildAssetSeries(series, reportingPeriod);
                    break;
                case PERFORMANCE:
                    buildPerformanceSeries(series, reportingPeriod);
                    break;
                case RETURN_VOLATILITY:
                    throw new UnsupportedOperationException();
                default:
                    throw new IllegalArgumentException("unsupported use case " + useCase); //$NON-NLS-1$
            }

            chart.adjustRange();
        }
        finally
        {
            chart.suspendUpdate(false);
        }
        chart.redraw();
    }

    private void buildAssetSeries(List<DataSeries> series, Interval reportingPeriod)
    {
        StatementOfAssetsSeriesBuilder b1 = new StatementOfAssetsSeriesBuilder(chart,
                        getDashboardData().getDataSeriesCache());
        series.forEach(s -> b1.build(s, reportingPeriod));
    }

    private void buildPerformanceSeries(List<DataSeries> series, Interval reportingPeriod)
    {
        PerformanceChartSeriesBuilder b2 = new PerformanceChartSeriesBuilder(chart,
                        getDashboardData().getDataSeriesCache());
        series.forEach(s -> b2.build(s, reportingPeriod, get(AggregationConfig.class).getAggregation()));
    }
}
