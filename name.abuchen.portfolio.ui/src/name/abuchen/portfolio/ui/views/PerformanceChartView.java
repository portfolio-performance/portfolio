package name.abuchen.portfolio.ui.views;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.swtchart.ISeries;

import com.google.common.collect.Lists;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.Aggregation;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.filter.ReadOnlyClient;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.AbstractCSVExporter;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.chart.TimelineChart;
import name.abuchen.portfolio.ui.util.chart.TimelineChartCSVExporter;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries;
import name.abuchen.portfolio.ui.views.dataseries.DataSeriesCache;
import name.abuchen.portfolio.ui.views.dataseries.DataSeriesChartLegend;
import name.abuchen.portfolio.ui.views.dataseries.DataSeriesConfigurator;
import name.abuchen.portfolio.ui.views.dataseries.PerformanceChartSeriesBuilder;
import name.abuchen.portfolio.ui.views.panes.HistoricalPricesPane;
import name.abuchen.portfolio.ui.views.panes.InformationPanePage;
import name.abuchen.portfolio.ui.views.panes.SecurityEventsPane;
import name.abuchen.portfolio.ui.views.panes.SecurityPriceChartPane;
import name.abuchen.portfolio.ui.views.panes.TradesPane;
import name.abuchen.portfolio.ui.views.panes.TransactionsPane;
import name.abuchen.portfolio.util.Interval;

public class PerformanceChartView extends AbstractHistoricView
{
    public static final int TOTALS_YAXIS_INDEX = 0;
    public static final int DELTA_PERCENTAGE_YAXIS_INDEX = 2;
    
    private static final String KEY_AGGREGATION_PERIOD = "performance-chart-aggregation-period"; //$NON-NLS-1$

    private static class MinusOneDecimalFormat extends DecimalFormat
    {
        public MinusOneDecimalFormat(String format)
        {
            super(format);
        }
        
        @Override
        public StringBuffer format(double number, StringBuffer result, FieldPosition fieldPosition)
        {
            return super.format(number - 1d, result, fieldPosition);
        }
    }
    
    private enum ChartDetails
    {
        SCALING_LINEAR(Messages.LabelChartDetailChartScalingLinear),
        SCALING_LOG(Messages.LabelChartDetailChartScalingLog);

        private final String label;

        private ChartDetails(String label)
        {
            this.label = label;
        }

        @Override
        public String toString()
        {
            return label;
        }
    }

    private static final String PREF_KEY = "performance-chart-details"; //$NON-NLS-1$

    private TimelineChart chart;
    private DataSeriesConfigurator picker;
    private ChartViewConfig chartViewConfig;
    private EnumSet<ChartDetails> chartConfig = EnumSet.of(ChartDetails.SCALING_LINEAR);

    private Aggregation.Period aggregationPeriod;

    private PerformanceChartSeriesBuilder seriesBuilder;

    @Override
    protected String getDefaultTitle()
    {
        return Messages.LabelPerformanceChart;
    }

    @PostConstruct
    public void setup()
    {
        String key = getPreferenceStore().getString(KEY_AGGREGATION_PERIOD);
        if (key != null && key.length() > 0)
        {
            try
            {
                aggregationPeriod = Aggregation.Period.valueOf(key);
            }
            catch (IllegalArgumentException ignore)
            {
                // ignore if key is not a valid aggregation period
                PortfolioPlugin.log(ignore);
            }
        }
    }

    @Inject
    @Optional
    public void setup(@Named(UIConstants.Parameter.VIEW_PARAMETER) ChartViewConfig config)
    {
        this.chartViewConfig = config;
    }

    @Override
    protected void addButtons(ToolBarManager toolBar)
    {
        super.addButtons(toolBar);
        toolBar.add(new AggregationPeriodDropDown());
        toolBar.add(new ExportDropDown());
        toolBar.add(new DropDown(Messages.MenuConfigureChart, Images.CONFIG, SWT.NONE, new IMenuListener()
        {
            @Override
            public void menuAboutToShow(IMenuManager manager)
            {
                picker.configMenuAboutToShow(manager);

                manager.add(new Separator());
                MenuManager subMenuChartScaling = new MenuManager(Messages.LabelChartDetailChartScaling, null);
                subMenuChartScaling.add(addMenuAction(ChartDetails.SCALING_LINEAR));
                subMenuChartScaling.add(addMenuAction(ChartDetails.SCALING_LOG));
                manager.add(subMenuChartScaling);
            }
        }));
        
    }
    
    private Action addMenuAction(ChartDetails detail)
    {
        Action action = new SimpleAction(detail.toString(), a -> {
            boolean isActive = chartConfig.contains(detail);

            if (isActive)
                chartConfig.remove(detail);
            else
                chartConfig.add(detail);

            if (!isActive)
            {
                switch (detail)
                {
                    case SCALING_LINEAR:
                        chartConfig.remove(ChartDetails.SCALING_LOG);
                        break;
                    case SCALING_LOG:
                        chartConfig.remove(ChartDetails.SCALING_LINEAR);
                        break;
                    default:
                        break;
                }
            }
            if (!chartConfig.contains(ChartDetails.SCALING_LINEAR) && !chartConfig.contains(ChartDetails.SCALING_LOG))
                chartConfig.add(ChartDetails.SCALING_LINEAR);

            // If we would have a client object, we could store the configuration this way.
            //ReadOnlyClient.unwrap(client).setProperty(PREF_KEY, String.join(",", //$NON-NLS-1$
            //                chartConfig.stream().map(ChartDetails::name).collect(Collectors.toList())));

            updateChart();

        });

        action.setChecked(chartConfig.contains(detail));
        return action;
    }

    @Override
    protected Composite createBody(Composite parent)
    {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

        chart = new TimelineChart(composite);
        chart.getTitle().setText(getTitle());
        chart.getTitle().setVisible(false);
        chart.getAxisSet().getYAxis(TOTALS_YAXIS_INDEX).getTick().setFormat(new MinusOneDecimalFormat("0.#%")); //$NON-NLS-1$
        int axisId = chart.getAxisSet().createYAxis();
        assert axisId == DELTA_PERCENTAGE_YAXIS_INDEX;
        chart.getAxisSet().getYAxis(TOTALS_YAXIS_INDEX).getTitle().setText("Kummulierte Performance");
        chart.getAxisSet().getYAxis(TOTALS_YAXIS_INDEX).getTitle().setVisible(true);
        chart.getAxisSet().getYAxis(DELTA_PERCENTAGE_YAXIS_INDEX).getTitle().setText("Tägliche Performance");
        chart.getAxisSet().getYAxis(DELTA_PERCENTAGE_YAXIS_INDEX).getTick().setFormat(new DecimalFormat("0.#%")); //$NON-NLS-1$
        chart.getToolTip().setDefaultValueFormat(new MinusOneDecimalFormat(Values.Percent2.pattern()));
        chart.getToolTip().reverseLabels(true);

        DataSeriesCache cache = make(DataSeriesCache.class);
        seriesBuilder = new PerformanceChartSeriesBuilder(chart, cache);

        picker = new DataSeriesConfigurator(this, DataSeries.UseCase.PERFORMANCE);
        if (chartViewConfig != null)
        {
            // do *not* update reporting period as it changes the default for
            // all other views as well --> unexpected UX
            picker.activate(chartViewConfig.getUUID());
        }

        picker.addListener(this::updateChart);
        picker.setToolBarManager(getViewToolBarManager());

        DataSeriesChartLegend legend = new DataSeriesChartLegend(composite, picker);
        legend.addSelectionChangedListener(e -> setInformationPaneInput(e.getStructuredSelection().getFirstElement()));

        updateTitle(Messages.LabelPerformanceChart + " (" + picker.getConfigurationName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        chart.getTitle().setText(getTitle());

        GridLayoutFactory.fillDefaults().numColumns(1).margins(0, 0).spacing(0, 0).applyTo(composite);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(chart);
        GridDataFactory.fillDefaults().grab(true, false).align(SWT.CENTER, SWT.FILL).applyTo(legend);

        setChartSeries();

        return composite;
    }

    @Override
    protected void addPanePages(List<InformationPanePage> pages)
    {
        super.addPanePages(pages);
        pages.add(make(SecurityPriceChartPane.class));
        pages.add(make(HistoricalPricesPane.class));
        pages.add(make(TransactionsPane.class));
        pages.add(make(TradesPane.class));
        pages.add(make(SecurityEventsPane.class));
    }

    @Override
    public void setFocus()
    {
        chart.adjustRange();
        chart.setFocus();
    }

    @Override
    public void reportingPeriodUpdated()
    {
        notifyModelUpdated();
    }

    @Override
    public void notifyModelUpdated()
    {
        seriesBuilder.getCache().clear();
        updateChart();
    }

    // If we would have a client object, we could load the configuration this way.
    private final void readChartConfig(Client client)
    {
        String pref = ReadOnlyClient.unwrap(client).getProperty(PREF_KEY);
        if (pref == null)
            return;

        chartConfig.clear();
        for (String key : pref.split(",")) //$NON-NLS-1$
        {
            try
            {
                chartConfig.add(ChartDetails.valueOf(key));
            }
            catch (IllegalArgumentException ignore)
            {
                // do not print exception to the log as it confuses users. The
                // old SMA200 label has been renamed, nothing we can change
                // anymore
            }
        }
    }

    private void updateChart()
    {
        try
        {
            updateTitle(Messages.LabelPerformanceChart + " (" + picker.getConfigurationName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$

            chart.suspendUpdate(true);
            chart.getTitle().setText(getTitle());
            for (ISeries s : chart.getSeriesSet().getSeries())
                chart.getSeriesSet().deleteSeries(s.getId());

            setChartSeries();

            chart.adjustRange();
        }
        finally
        {
            chart.suspendUpdate(false);
        }
        chart.getAxisSet().getYAxis(TOTALS_YAXIS_INDEX).enableLogScale(chartConfig.contains(ChartDetails.SCALING_LOG));
        
        chart.redraw();

        // re-layout in case chart legend changed
        chart.getParent().layout(true);
    }

    private void setChartSeries()
    {
        Interval interval = getReportingPeriod().toInterval(LocalDate.now());
        Lists.reverse(picker.getSelectedDataSeries())
                        .forEach(series -> seriesBuilder.build(series, interval, aggregationPeriod));
    }

    private final class AggregationPeriodDropDown extends DropDown implements IMenuListener
    {
        private AggregationPeriodDropDown()
        {
            super(aggregationPeriod == null ? Messages.LabelAggregationDaily : aggregationPeriod.toString());
            setMenuListener(this);
        }

        @Override
        public void menuAboutToShow(IMenuManager manager)
        {
            Action daily = new Action(Messages.LabelAggregationDaily)
            {
                @Override
                public void run()
                {
                    setLabel(Messages.LabelAggregationDaily);
                    aggregationPeriod = null;
                    getPart().getPreferenceStore().setValue(KEY_AGGREGATION_PERIOD, ""); //$NON-NLS-1$
                    updateChart();
                }
            };
            daily.setChecked(aggregationPeriod == null);
            manager.add(daily);

            for (final Aggregation.Period period : Aggregation.Period.values())
            {
                Action action = new Action(period.toString())
                {
                    @Override
                    public void run()
                    {
                        setLabel(period.toString());
                        aggregationPeriod = period;
                        getPart().getPreferenceStore().setValue(KEY_AGGREGATION_PERIOD, period.name());
                        updateChart();
                    }
                };
                action.setChecked(aggregationPeriod == period);
                manager.add(action);
            }
        }
    }

    private final class ExportDropDown extends DropDown implements IMenuListener
    {
        private ExportDropDown()
        {
            super(Messages.MenuExportData, Images.EXPORT, SWT.NONE);
            setMenuListener(this);
        }

        @Override
        public void menuAboutToShow(IMenuManager manager)
        {
            manager.add(new Action(Messages.MenuExportChartData)
            {
                @Override
                public void run()
                {
                    TimelineChartCSVExporter exporter = new TimelineChartCSVExporter(chart);
                    exporter.addDiscontinousSeries(Messages.PerformanceChartLabelCPI);
                    exporter.setDateFormat(new SimpleDateFormat("yyyy-MM-dd")); //$NON-NLS-1$
                    exporter.setValueFormat(new DecimalFormat("0.##########")); //$NON-NLS-1$
                    exporter.export(getTitle() + ".csv"); //$NON-NLS-1$
                }
            });

            picker.getSelectedDataSeries().stream().forEach(ds -> addMenu(manager, ds));

            manager.add(new Separator());
            chart.exportMenuAboutToShow(manager, getTitle());
        }

        private void addMenu(IMenuManager manager, final DataSeries series)
        {
            manager.add(new SimpleAction(MessageFormat.format(Messages.LabelExport, series.getLabel()), a -> {
                AbstractCSVExporter exporter = new AbstractCSVExporter()
                {
                    @Override
                    protected void writeToFile(File file) throws IOException
                    {
                        PerformanceIndex index = seriesBuilder.getCache().lookup(series,
                                        getReportingPeriod().toInterval(LocalDate.now()));
                        if (aggregationPeriod != null)
                            index = Aggregation.aggregate(index, aggregationPeriod);
                        index.exportTo(file);
                    }

                    @Override
                    protected Shell getShell()
                    {
                        return chart.getShell();
                    }
                };
                exporter.export(getTitle() + "_" + series.getLabel() + ".csv"); //$NON-NLS-1$ //$NON-NLS-2$
            }));
        }
    }
}
