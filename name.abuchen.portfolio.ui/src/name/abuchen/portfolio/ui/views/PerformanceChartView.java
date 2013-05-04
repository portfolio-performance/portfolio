package name.abuchen.portfolio.ui.views;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Category;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ConsumerPriceIndex;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.snapshot.AccountIndex;
import name.abuchen.portfolio.snapshot.Aggregation;
import name.abuchen.portfolio.snapshot.CPIIndex;
import name.abuchen.portfolio.snapshot.CategoryIndex;
import name.abuchen.portfolio.snapshot.ClientIndex;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.PortfolioIndex;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.SecurityIndex;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.AbstractCSVExporter;
import name.abuchen.portfolio.ui.util.AbstractDropDown;
import name.abuchen.portfolio.ui.util.TimelineChart;
import name.abuchen.portfolio.ui.util.TimelineChartCSVExporter;
import name.abuchen.portfolio.ui.views.ChartConfigurator.DataSeries;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuManager;
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
    private TimelineChart chart;
    private ChartConfigurator picker;

    private Aggregation.Period aggregationPeriod;

    private Map<Object, Object> dataCache = new HashMap<Object, Object>();

    @Override
    protected String getTitle()
    {
        return Messages.LabelPerformanceChart;
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
        Action config = new Action()
        {
            @Override
            public void run()
            {
                picker.showMenu(getClientEditor().getSite().getShell());
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
        chart.getAxisSet().getYAxis(0).getTick().setFormat(new DecimalFormat("0.#%")); //$NON-NLS-1$
        chart.getTitle().setVisible(false);
        chart.getToolTip().setValueFormat(new DecimalFormat("0.##%")); //$NON-NLS-1$
        chart.getToolTip().setReferenceSeries(Messages.PerformanceChartLabelAccumulatedIRR);

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

        // force layout, otherwise range calculation of chart does not work
        composite.layout();
        updateChart();

        return composite;
    }

    @Override
    protected void reportingPeriodUpdated()
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

            List<Exception> warnings = new ArrayList<Exception>();

            for (DataSeries item : picker.getSelectedDataSeries())
            {
                if (item.getType() == Client.class)
                    addClient(item, (Client) item.getInstance(), warnings);
                else if (item.getType() == ConsumerPriceIndex.class)
                    addConsumerPriceIndex(item, warnings);
                else if (item.getType() == Security.class)
                    addSecurity(item, (Security) item.getInstance(), warnings);
                else if (item.getType() == Portfolio.class)
                    addPortfolio(item, (Portfolio) item.getInstance(), warnings);
                else if (item.getType() == Account.class)
                    addAccount(item, (Account) item.getInstance(), warnings);
                else if (item.getType() == Category.class)
                    addCategory(item, (Category) item.getInstance(), warnings);
            }

            PortfolioPlugin.log(warnings);

            chart.getAxisSet().adjustRange();
        }
        finally
        {
            chart.suspendUpdate(false);
        }
        chart.redraw();
    }

    private ClientIndex getClientIndex(List<Exception> warnings)
    {
        ClientIndex index = (ClientIndex) dataCache.get(ClientIndex.class);
        if (index == null)
        {
            ReportingPeriod interval = getReportingPeriod();
            index = ClientIndex.forPeriod(getClient(), interval, warnings);
            dataCache.put(ClientIndex.class, index);
        }
        return index;
    }

    private void addClient(DataSeries item, Client client, List<Exception> warnings)
    {
        ClientIndex clientIndex = getClientIndex(warnings);
        PerformanceIndex aggregatedIndex = aggregationPeriod != null ? Aggregation.aggregate(clientIndex,
                        aggregationPeriod) : clientIndex;

        if (client != null)
        {
            ILineSeries series = chart.addDateSeries(aggregatedIndex.getDates(), //
                            aggregatedIndex.getAccumulatedPercentage(), //
                            Messages.PerformanceChartLabelAccumulatedIRR);
            item.configure(series);
        }
        else
        {
            IBarSeries barSeries = chart.addDateBarSeries(aggregatedIndex.getDates(), //
                            aggregatedIndex.getDeltaPercentage(), //
                            aggregationPeriod != null ? aggregationPeriod.toString() : Messages.LabelAggregationDaily);
            item.configure(barSeries);
        }
    }

    private void addConsumerPriceIndex(DataSeries item, List<Exception> warnings)
    {
        CPIIndex cpiIndex = (CPIIndex) dataCache.get(CPIIndex.class);

        if (cpiIndex == null)
        {
            ClientIndex clientIndex = getClientIndex(warnings);
            cpiIndex = CPIIndex.forClient(clientIndex, warnings);
            dataCache.put(CPIIndex.class, cpiIndex);
        }

        if (cpiIndex.getDates().length > 0
                        && (aggregationPeriod == null || aggregationPeriod != Aggregation.Period.YEARLY))
        {
            ILineSeries series = chart.addDateSeries(cpiIndex.getDates(), //
                            cpiIndex.getAccumulatedPercentage(), //
                            Messages.PerformanceChartLabelCPI);
            item.configure(series);
        }
    }

    private void addSecurity(DataSeries item, Security security, List<Exception> warnings)
    {
        PerformanceIndex securityIndex = (PerformanceIndex) dataCache.get(security);

        if (securityIndex == null)
        {
            ClientIndex clientIndex = getClientIndex(warnings);
            securityIndex = SecurityIndex.forClient(clientIndex, security, warnings);
            dataCache.put(security, securityIndex);
        }

        if (aggregationPeriod != null)
            securityIndex = Aggregation.aggregate(securityIndex, aggregationPeriod);

        ILineSeries series = chart.addDateSeries(securityIndex.getDates(), //
                        securityIndex.getAccumulatedPercentage(), //
                        security.getName());
        item.configure(series);
    }

    private void addPortfolio(DataSeries item, Portfolio portfolio, List<Exception> warnings)
    {
        PerformanceIndex portfolioIndex = (PerformanceIndex) dataCache.get(portfolio);

        if (portfolioIndex == null)
        {
            portfolioIndex = PortfolioIndex.forPeriod(getClient(), portfolio, getReportingPeriod(), warnings);
            dataCache.put(portfolio, portfolioIndex);
        }

        if (aggregationPeriod != null)
            portfolioIndex = Aggregation.aggregate(portfolioIndex, aggregationPeriod);

        ILineSeries series = chart.addDateSeries(portfolioIndex.getDates(), //
                        portfolioIndex.getAccumulatedPercentage(), //
                        portfolio.getName());
        item.configure(series);
    }

    private void addAccount(DataSeries item, Account account, List<Exception> warnings)
    {
        PerformanceIndex accountIndex = (PerformanceIndex) dataCache.get(account);

        if (accountIndex == null)
        {
            accountIndex = AccountIndex.forPeriod(getClient(), account, getReportingPeriod(), warnings);
            dataCache.put(account, accountIndex);
        }

        if (aggregationPeriod != null)
            accountIndex = Aggregation.aggregate(accountIndex, aggregationPeriod);

        ILineSeries series = chart.addDateSeries(accountIndex.getDates(), //
                        accountIndex.getAccumulatedPercentage(), //
                        account.getName());
        item.configure(series);
    }

    private void addCategory(DataSeries item, Category category, List<Exception> warnings)
    {
        PerformanceIndex categoryIndex = (PerformanceIndex) dataCache.get(category);
        if (categoryIndex == null)
        {
            categoryIndex = CategoryIndex.forPeriod(getClient(), category, getReportingPeriod(), warnings);
            dataCache.put(category, categoryIndex);
        }

        if (aggregationPeriod != null)
            categoryIndex = Aggregation.aggregate(categoryIndex, aggregationPeriod);

        ILineSeries series = chart.addDateSeries(categoryIndex.getDates(), categoryIndex.getAccumulatedPercentage(),
                        category.getName());
        item.configure(series);
    }

    private final class AggregationPeriodDropDown extends AbstractDropDown
    {
        private AggregationPeriodDropDown(ToolBar toolBar)
        {
            super(toolBar, Messages.LabelAggregationDaily);
        }

        @Override
        public void menuAboutToShow(IMenuManager manager)
        {
            manager.add(new Action(Messages.LabelAggregationDaily)
            {
                @Override
                public void run()
                {
                    setLabel(Messages.LabelAggregationDaily);
                    aggregationPeriod = null;
                    updateChart();
                }
            });

            for (final Aggregation.Period period : Aggregation.Period.values())
            {
                manager.add(new Action(period.toString())
                {
                    @Override
                    public void run()
                    {
                        setLabel(period.toString());
                        aggregationPeriod = period;
                        updateChart();
                    }
                });
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

            manager.add(new Action(Messages.MenuExportPerformanceCalculation)
            {
                @Override
                public void run()
                {
                    AbstractCSVExporter exporter = new AbstractCSVExporter()
                    {
                        @Override
                        protected void writeToFile(File file) throws IOException
                        {
                            PerformanceIndex index = (ClientIndex) dataCache.get(ClientIndex.class);
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
                    exporter.export(getTitle() + ".csv"); //$NON-NLS-1$
                }
            });
        }
    }
}
