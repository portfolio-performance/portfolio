package name.abuchen.portfolio.ui.views;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ConsumerPriceIndex;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.snapshot.Aggregation;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPart;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.AbstractCSVExporter;
import name.abuchen.portfolio.ui.util.AbstractDropDown;
import name.abuchen.portfolio.ui.util.chart.TimelineChart;
import name.abuchen.portfolio.ui.util.chart.TimelineChartCSVExporter;
import name.abuchen.portfolio.ui.views.ChartConfigurator.ClientDataSeries;
import name.abuchen.portfolio.ui.views.ChartConfigurator.DataSeries;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.swtchart.IBarSeries;
import org.swtchart.ILineSeries;
import org.swtchart.ISeries;

public class PerformanceChartView extends AbstractHistoricView
{
    private static final String KEY_AGGREGATION_PERIOD = "performance-chart-aggregation-period"; //$NON-NLS-1$

    private CurrencyConverter converter;

    private TimelineChart chart;
    private ChartConfigurator picker;

    private Aggregation.Period aggregationPeriod;

    private Map<Object, PerformanceIndex> dataCache = new HashMap<Object, PerformanceIndex>();

    @PostConstruct
    private void setupCurrencyConverter(ExchangeRateProviderFactory factory, Client client)
    {
        converter = new CurrencyConverterImpl(factory, client.getBaseCurrency());
    }

    @Override
    protected String getTitle()
    {
        return Messages.LabelPerformanceChart;
    }

    @Override
    public void init(PortfolioPart part, Object parameter)
    {
        super.init(part, parameter);

        String key = part.getPreferenceStore().getString(KEY_AGGREGATION_PERIOD);
        if (key != null && key.length() > 0)
        {
            try
            {
                aggregationPeriod = Aggregation.Period.valueOf(key);
            }
            catch (IllegalArgumentException ignore)
            {
                // ignore if key is not a valid aggregation period
            }
        }
    }

    @Override
    protected void addButtons(ToolBar toolBar)
    {
        super.addButtons(toolBar);
        new AggregationPeriodDropDown(toolBar);
        new ExportDropDown(toolBar);
        addConfigButton(toolBar);
    }

    private void addConfigButton(ToolBar toolBar)
    {
        Action save = new Action()
        {
            @Override
            public void run()
            {
                picker.showSaveMenu(getActiveShell());
            }
        };
        save.setImageDescriptor(PortfolioPlugin.descriptor(PortfolioPlugin.IMG_SAVE));
        save.setToolTipText(Messages.MenuSaveChart);
        new ActionContributionItem(save).fill(toolBar, -1);

        Action config = new Action()
        {
            @Override
            public void run()
            {
                picker.showMenu(getActiveShell());
            }
        };
        config.setImageDescriptor(PortfolioPlugin.descriptor(PortfolioPlugin.IMG_CONFIG));
        config.setToolTipText(Messages.MenuConfigureChart);
        new ActionContributionItem(config).fill(toolBar, -1);
    }

    @Override
    protected Composite createBody(Composite parent)
    {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

        chart = new TimelineChart(composite);
        chart.getTitle().setText(getTitle());
        chart.getTitle().setVisible(false);
        chart.getAxisSet().getYAxis(0).getTick().setFormat(new DecimalFormat("0.#%")); //$NON-NLS-1$
        chart.getToolTip().setValueFormat(new DecimalFormat("0.##%")); //$NON-NLS-1$

        picker = new ChartConfigurator(composite, this, ChartConfigurator.Mode.PERFORMANCE);
        picker.setListener(new ChartConfigurator.Listener()
        {
            @Override
            public void onUpdate()
            {
                updateChart();
            }
        });

        GridLayoutFactory.fillDefaults().numColumns(1).margins(0, 0).spacing(0, 0).applyTo(composite);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(chart);
        GridDataFactory.fillDefaults().grab(true, false).align(SWT.CENTER, SWT.FILL).applyTo(picker);

        setChartSeries();

        return composite;
    }

    @Override
    public void setFocus()
    {
        chart.getAxisSet().adjustRange();
        chart.setFocus();
    }

    @Override
    public void reportingPeriodUpdated()
    {
        dataCache.clear();
        updateChart();
    }

    @Override
    public void notifyModelUpdated()
    {
        dataCache.clear();
        updateChart();
    }

    private void updateChart()
    {
        try
        {
            chart.suspendUpdate(true);
            for (ISeries s : chart.getSeriesSet().getSeries())
                chart.getSeriesSet().deleteSeries(s.getId());

            setChartSeries();

            chart.getAxisSet().adjustRange();
        }
        finally
        {
            chart.suspendUpdate(false);
        }
        chart.redraw();
    }

    private void setChartSeries()
    {
        List<Exception> warnings = new ArrayList<Exception>();

        for (DataSeries item : picker.getSelectedDataSeries())
        {
            if (item.getType() == Client.class)
                addClient(item, (ClientDataSeries) item.getInstance(), warnings);
            else if (item.getType() == ConsumerPriceIndex.class)
                addConsumerPriceIndex(item, warnings);
            else if (item.getType() == Security.class)
                addSecurity(item, (Security) item.getInstance(), warnings);
            else if (item.getType() == Portfolio.class)
                addPortfolio(item, (Portfolio) item.getInstance(), warnings);
            else if (item.getType() == Account.class)
                addAccount(item, (Account) item.getInstance(), warnings);
            else if (item.getType() == Classification.class)
                addClassification(item, (Classification) item.getInstance(), warnings);
        }

        PortfolioPlugin.log(warnings);
    }

    private PerformanceIndex getClientIndex(List<Exception> warnings)
    {
        PerformanceIndex index = dataCache.get(Client.class);
        if (index == null)
        {
            ReportingPeriod interval = getReportingPeriod();
            index = PerformanceIndex.forClient(getClient(), converter, interval, warnings);
            dataCache.put(Client.class, index);
        }
        return index;
    }

    private void addClient(DataSeries item, ClientDataSeries type, List<Exception> warnings)
    {
        PerformanceIndex clientIndex = getClientIndex(warnings);
        PerformanceIndex aggregatedIndex = aggregationPeriod != null ? Aggregation.aggregate(clientIndex,
                        aggregationPeriod) : clientIndex;

        switch (type)
        {
            case TOTALS:
                ILineSeries series = chart.addDateSeries(aggregatedIndex.getDates(), //
                                aggregatedIndex.getAccumulatedPercentage(), //
                                Messages.PerformanceChartLabelAccumulatedIRR);
                item.configure(series);
                break;
            case TRANSFERALS:
                IBarSeries barSeries = chart.addDateBarSeries(aggregatedIndex.getDates(), //
                                aggregatedIndex.getDeltaPercentage(), //
                                aggregationPeriod != null ? aggregationPeriod.toString()
                                                : Messages.LabelAggregationDaily);
                item.configure(barSeries);
                break;
            default:
                break;
        }
    }

    private void addConsumerPriceIndex(DataSeries item, List<Exception> warnings)
    {
        PerformanceIndex cpiIndex = dataCache.get(ConsumerPriceIndex.class);

        if (cpiIndex == null)
        {
            PerformanceIndex clientIndex = getClientIndex(warnings);
            cpiIndex = PerformanceIndex.forConsumerPriceIndex(clientIndex, warnings);
            dataCache.put(ConsumerPriceIndex.class, cpiIndex);
        }

        if (cpiIndex.getDates().length > 0
                        && (aggregationPeriod == null || aggregationPeriod != Aggregation.Period.YEARLY))
        {
            ILineSeries series = chart.addDateSeries(cpiIndex.getDates(), //
                            cpiIndex.getAccumulatedPercentage(), //
                            item.getLabel());
            item.configure(series);
        }
    }

    private void addSecurity(DataSeries item, Security security, List<Exception> warnings)
    {
        if (item.isBenchmark())
            addSecurityBenchmark(item, security, warnings);
        else
            addSecurityPerformance(item, security, warnings);
    }

    private void addSecurityBenchmark(DataSeries item, Security security, List<Exception> warnings)
    {
        PerformanceIndex securityIndex = dataCache.get(security);

        if (securityIndex == null)
        {
            PerformanceIndex clientIndex = getClientIndex(warnings);
            securityIndex = PerformanceIndex.forSecurity(clientIndex, security, warnings);
            dataCache.put(security, securityIndex);
        }

        if (aggregationPeriod != null)
            securityIndex = Aggregation.aggregate(securityIndex, aggregationPeriod);

        ILineSeries series = chart.addDateSeries(securityIndex.getDates(), //
                        securityIndex.getAccumulatedPercentage(), //
                        item.getLabel());
        item.configure(series);
    }

    private void addSecurityPerformance(DataSeries item, Security security, List<Exception> warnings)
    {
        PerformanceIndex securityIndex = dataCache.get(security.getUUID());

        if (securityIndex == null)
        {
            securityIndex = PerformanceIndex.forInvestment(getClient(), converter, security, getReportingPeriod(),
                            warnings);
            dataCache.put(security.getUUID(), securityIndex);
        }

        if (aggregationPeriod != null)
            securityIndex = Aggregation.aggregate(securityIndex, aggregationPeriod);

        ILineSeries series = chart.addDateSeries(securityIndex.getDates(), //
                        securityIndex.getAccumulatedPercentage(), //
                        item.getLabel());
        item.configure(series);
    }

    private void addPortfolio(DataSeries item, Portfolio portfolio, List<Exception> warnings)
    {
        Object cacheKey = item.isPortfolioPlus() ? portfolio.getUUID() : portfolio;
        PerformanceIndex portfolioIndex = dataCache.get(cacheKey);

        if (portfolioIndex == null)
        {
            portfolioIndex = item.isPortfolioPlus() ? PerformanceIndex //
                            .forPortfolioPlusAccount(getClient(), converter, portfolio, getReportingPeriod(), warnings)
                            : PerformanceIndex.forPortfolio(getClient(), converter, portfolio, getReportingPeriod(),
                                            warnings);
            dataCache.put(cacheKey, portfolioIndex);
        }

        if (aggregationPeriod != null)
            portfolioIndex = Aggregation.aggregate(portfolioIndex, aggregationPeriod);

        ILineSeries series = chart.addDateSeries(portfolioIndex.getDates(), //
                        portfolioIndex.getAccumulatedPercentage(), //
                        item.getLabel());
        item.configure(series);
    }

    private void addAccount(DataSeries item, Account account, List<Exception> warnings)
    {
        PerformanceIndex accountIndex = dataCache.get(account);

        if (accountIndex == null)
        {
            accountIndex = PerformanceIndex.forAccount(getClient(), converter, account, getReportingPeriod(), warnings);
            dataCache.put(account, accountIndex);
        }

        if (aggregationPeriod != null)
            accountIndex = Aggregation.aggregate(accountIndex, aggregationPeriod);

        ILineSeries series = chart.addDateSeries(accountIndex.getDates(), //
                        accountIndex.getAccumulatedPercentage(), //
                        account.getName());
        item.configure(series);
    }

    private void addClassification(DataSeries item, Classification classification, List<Exception> warnings)
    {
        PerformanceIndex index = dataCache.get(classification);

        if (index == null)
        {
            index = PerformanceIndex.forClassification(getClient(), converter, classification, getReportingPeriod(),
                            warnings);
            dataCache.put(classification, index);
        }

        if (aggregationPeriod != null)
            index = Aggregation.aggregate(index, aggregationPeriod);

        ILineSeries series = chart.addDateSeries(index.getDates(), //
                        index.getAccumulatedPercentage(), //
                        item.getLabel());
        item.configure(series);
    }

    private final class AggregationPeriodDropDown extends AbstractDropDown
    {
        private AggregationPeriodDropDown(ToolBar toolBar)
        {
            super(toolBar, aggregationPeriod == null ? Messages.LabelAggregationDaily : aggregationPeriod.toString());
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

    private final class ExportDropDown extends AbstractDropDown
    {
        private ExportDropDown(ToolBar toolBar)
        {
            super(toolBar, Messages.MenuExportData, PortfolioPlugin.image(PortfolioPlugin.IMG_EXPORT), SWT.NONE);
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

            Set<Class<?>> exportTypes = new HashSet<Class<?>>(Arrays.asList(new Class<?>[] { //
                            Client.class, Security.class, Portfolio.class, Account.class }));

            for (DataSeries series : picker.getSelectedDataSeries())
            {
                if (exportTypes.contains(series.getType()))
                    addMenu(manager, series);
            }

            manager.add(new Separator());
            chart.exportMenuAboutToShow(manager, getTitle());
        }

        private void addMenu(IMenuManager manager, final DataSeries series)
        {
            manager.add(new Action(MessageFormat.format(Messages.LabelExport, series.getLabel()))
            {
                @Override
                public void run()
                {
                    AbstractCSVExporter exporter = new AbstractCSVExporter()
                    {
                        @Override
                        protected void writeToFile(File file) throws IOException
                        {
                            PerformanceIndex index = null;

                            if (series.getType() == Client.class)
                                index = dataCache.get(Client.class);
                            else if (series.isPortfolioPlus())
                                index = dataCache.get(((Portfolio) series.getInstance()).getUUID());
                            else if (series.getType() == Security.class && !series.isBenchmark())
                                index = dataCache.get(((Security) series.getInstance()).getUUID());
                            else
                                index = dataCache.get(series.getInstance());

                            if (aggregationPeriod != null)
                                index = Aggregation.aggregate(index, aggregationPeriod);
                            index.exportTo(file);
                        }

                        @Override
                        protected Control getControl()
                        {
                            return ExportDropDown.this.getToolBar();
                        }
                    };
                    exporter.export(getTitle() + "_" + series.getLabel() + ".csv"); //$NON-NLS-1$ //$NON-NLS-2$
                }
            });
        }
    }
}
