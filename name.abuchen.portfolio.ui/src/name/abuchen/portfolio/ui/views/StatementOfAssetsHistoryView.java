package name.abuchen.portfolio.ui.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.swtchart.IBarSeries;
import org.swtchart.ILineSeries;
import org.swtchart.ISeries;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.chart.TimelineChart;
import name.abuchen.portfolio.ui.util.chart.TimelineChartCSVExporter;
import name.abuchen.portfolio.ui.views.ChartConfigurator.ClientDataSeries;
import name.abuchen.portfolio.ui.views.ChartConfigurator.DataSeries;

public class StatementOfAssetsHistoryView extends AbstractHistoricView
{
    private CurrencyConverter converter;

    private TimelineChart chart;
    private ChartConfigurator picker;

    private Map<Object, Object> dataCache = new HashMap<Object, Object>();

    @PostConstruct
    private void setupCurrencyConverter(Client client, ExchangeRateProviderFactory factory)
    {
        converter = new CurrencyConverterImpl(factory, client.getBaseCurrency());
    }

    @Override
    protected String getTitle()
    {
        return Messages.LabelStatementOfAssetsHistory;
    }

    protected void addButtons(ToolBar toolBar)
    {
        super.addButtons(toolBar);
        addExportButton(toolBar);
        addConfigButton(toolBar);
    }

    private void addExportButton(ToolBar toolBar)
    {
        Action export = new Action()
        {
            private Menu menu;

            @Override
            public void run()
            {
                if (menu == null)
                {
                    menu = createContextMenu(getActiveShell(), new IMenuListener()
                    {
                        @Override
                        public void menuAboutToShow(IMenuManager manager)
                        {
                            exportMenuAboutToShow(manager);
                        }
                    });
                }
                menu.setVisible(true);
            }
        };
        export.setImageDescriptor(Images.EXPORT.descriptor());
        export.setToolTipText(Messages.MenuExportData);

        new ActionContributionItem(export).fill(toolBar, -1);
    }

    private void exportMenuAboutToShow(IMenuManager manager)
    {
        manager.add(new Action(Messages.MenuExportChartData)
        {
            @Override
            public void run()
            {
                TimelineChartCSVExporter exporter = new TimelineChartCSVExporter(chart);
                exporter.addDiscontinousSeries(Messages.LabelTransferals);
                exporter.export(getTitle() + ".csv"); //$NON-NLS-1$
            }
        });
        manager.add(new Separator());
        chart.exportMenuAboutToShow(manager, getTitle());
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
        save.setImageDescriptor(Images.SAVE.descriptor());
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
        config.setImageDescriptor(Images.CONFIG.descriptor());
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

        picker = new ChartConfigurator(composite, this, ChartConfigurator.Mode.STATEMENT_OF_ASSETS);
        picker.setListener(() -> updateChart());

        updateTitle(Messages.LabelStatementOfAssetsHistory + " (" + picker.getConfigurationName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$

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
    public void notifyModelUpdated()
    {
        dataCache.clear();
        updateChart();
    }

    @Override
    public void reportingPeriodUpdated()
    {
        dataCache.clear();
        updateChart();
    }

    private void updateChart()
    {
        try
        {
            updateTitle(Messages.LabelStatementOfAssetsHistory + " (" + picker.getConfigurationName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
            
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
                addClient(item, warnings);
            else if (item.getType() == Security.class)
                addSecurity(item, warnings);
            else if (item.getType() == Portfolio.class)
                addPortfolio(item, warnings);
            else if (item.getType() == Account.class)
                addAccount(item, warnings);
            else if (item.getType() == Classification.class)
                addClassification(item, warnings);
        }

        PortfolioPlugin.log(warnings);
    }

    private void addClient(DataSeries item, List<Exception> warnings)
    {
        PerformanceIndex clientIndex = (PerformanceIndex) dataCache.get(Client.class);
        if (clientIndex == null)
        {
            clientIndex = PerformanceIndex.forClient(getClient(), converter, getReportingPeriod(), warnings);
            dataCache.put(Client.class, clientIndex);
        }

        switch ((ClientDataSeries) item.getInstance())
        {
            case TOTALS:
                ILineSeries tSeries = chart.addDateSeries(clientIndex.getDates(), //
                                toDouble(clientIndex.getTotals(), Values.Amount.divider()), //
                                Messages.LabelTotalSum);
                item.configure(tSeries);
                break;
            case TRANSFERALS:
                IBarSeries tfSeries = chart.addDateBarSeries(clientIndex.getDates(), //
                                toDouble(clientIndex.getTransferals(), Values.Amount.divider()), //
                                Messages.LabelTransferals);
                item.configure(tfSeries);
                break;
            case INVESTED_CAPITAL:
                ILineSeries ivSeries = chart.addDateSeries(clientIndex.getDates(), //
                                toDouble(clientIndex.calculateInvestedCapital(), Values.Amount.divider()), //
                                item.getLabel());
                item.configure(ivSeries);
                break;
            case ABSOLUTE_DELTA:
                ILineSeries dSeries = chart.addDateSeries(clientIndex.getDates(), //
                                toDouble(clientIndex.calculateAbsoluteDelta(), Values.Amount.divider()), //
                                item.getLabel());
                item.configure(dSeries);
                break;
            case TAXES:
                ILineSeries txSeries = chart.addDateSeries(clientIndex.getDates(), //
                                accumulateAndToDouble(clientIndex.getTaxes(), Values.Amount.divider()), //
                                item.getLabel());
                item.configure(txSeries);
                break;
            case DIVIDENDS:
                IBarSeries deSeries = chart.addDateBarSeries(clientIndex.getDates(), //
                                toDouble(clientIndex.getDividends(), Values.Amount.divider()), //
                                item.getLabel());
                item.configure(deSeries);
                break;
            case DIVIDENDS_ACCUMULATED:
                ILineSeries daSeries = chart.addDateSeries(clientIndex.getDates(), //
                                accumulateAndToDouble(clientIndex.getDividends(), Values.Amount.divider()), //
                                item.getLabel());
                item.configure(daSeries);
                break;
            case INTEREST:
                IBarSeries ieSeries = chart.addDateBarSeries(clientIndex.getDates(), //
                                toDouble(clientIndex.getInterest(), Values.Amount.divider()), //
                                item.getLabel());
                item.configure(ieSeries);
                break;
            case INTEREST_ACCUMULATED:
                ILineSeries iaSeries = chart.addDateSeries(clientIndex.getDates(), //
                                accumulateAndToDouble(clientIndex.getInterest(), Values.Amount.divider()), //
                                item.getLabel());
                item.configure(iaSeries);
                break;

        }
    }

    private void addSecurity(DataSeries item, List<Exception> warnings)
    {
        Security security = (Security) item.getInstance();
        PerformanceIndex securityIndex = (PerformanceIndex) dataCache.get(security);
        if (securityIndex == null)
        {
            securityIndex = PerformanceIndex.forInvestment(getClient(), converter, security, getReportingPeriod(),
                            warnings);
            dataCache.put(security, securityIndex);
        }

        ILineSeries series = chart.addDateSeries(securityIndex.getDates(), //
                        toDouble(securityIndex.getTotals(), Values.Amount.divider()), //
                        security.getName());
        item.configure(series);
    }

    private void addPortfolio(DataSeries item, List<Exception> warnings)
    {
        Portfolio portfolio = (Portfolio) item.getInstance();

        Object cacheKey = item.isPortfolioPlus() ? portfolio.getUUID() : portfolio;
        PerformanceIndex portfolioIndex = (PerformanceIndex) dataCache.get(cacheKey);
        if (portfolioIndex == null)
        {
            portfolioIndex = item.isPortfolioPlus() ? PerformanceIndex //
                            .forPortfolioPlusAccount(getClient(), converter, portfolio, getReportingPeriod(), warnings)
                            : PerformanceIndex.forPortfolio(getClient(), converter, portfolio, getReportingPeriod(),
                                            warnings);
            dataCache.put(cacheKey, portfolioIndex);
        }

        ILineSeries series = chart.addDateSeries(portfolioIndex.getDates(), //
                        toDouble(portfolioIndex.getTotals(), Values.Amount.divider()), //
                        item.getLabel());
        item.configure(series);
    }

    private void addAccount(DataSeries item, List<Exception> warnings)
    {
        Account account = (Account) item.getInstance();
        PerformanceIndex accountIndex = (PerformanceIndex) dataCache.get(account);
        if (accountIndex == null)
        {
            accountIndex = PerformanceIndex.forAccount(getClient(), converter, account, getReportingPeriod(), warnings);
            dataCache.put(account, accountIndex);
        }

        ILineSeries series = chart.addDateSeries(accountIndex.getDates(), //
                        toDouble(accountIndex.getTotals(), Values.Amount.divider()), //
                        account.getName());
        item.configure(series);
    }

    private void addClassification(DataSeries item, List<Exception> warnings)
    {
        Classification classification = (Classification) item.getInstance();
        PerformanceIndex index = (PerformanceIndex) dataCache.get(classification);
        if (index == null)
        {
            index = PerformanceIndex.forClassification(getClient(), converter, classification, getReportingPeriod(),
                            warnings);
            dataCache.put(classification, index);
        }

        ILineSeries series = chart.addDateSeries(index.getDates(), //
                        toDouble(index.getTotals(), Values.Amount.divider()), //
                        classification.getName());
        item.configure(series);
    }

    private double[] toDouble(long[] input, double divider)
    {
        double[] answer = new double[input.length];
        for (int ii = 0; ii < answer.length; ii++)
            answer[ii] = input[ii] / divider;
        return answer;
    }

    private double[] accumulateAndToDouble(long[] input, double divider)
    {
        double[] answer = new double[input.length];
        long current = 0;
        for (int ii = 0; ii < answer.length; ii++)
        {
            current += input[ii];
            answer[ii] = current / divider;
        }
        return answer;
    }
}
