package name.abuchen.portfolio.ui.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.TimelineChart;
import name.abuchen.portfolio.ui.util.TimelineChartCSVExporter;
import name.abuchen.portfolio.ui.views.ChartConfigurator.DataSeries;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.swtchart.IBarSeries;
import org.swtchart.ILineSeries;
import org.swtchart.ISeries;

public class StatementOfAssetsHistoryView extends AbstractHistoricView
{
    private TimelineChart chart;
    private ChartConfigurator picker;

    private Map<Object, Object> dataCache = new HashMap<Object, Object>();

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
            @Override
            public void run()
            {
                TimelineChartCSVExporter exporter = new TimelineChartCSVExporter(chart);
                exporter.addDiscontinousSeries(Messages.LabelTransferals);
                exporter.export(getTitle() + ".csv"); //$NON-NLS-1$
            }
        };
        export.setImageDescriptor(PortfolioPlugin.descriptor(PortfolioPlugin.IMG_EXPORT));
        export.setToolTipText(Messages.MenuExportData);

        new ActionContributionItem(export).fill(toolBar, -1);
    }

    private void addConfigButton(ToolBar toolBar)
    {
        Action save = new Action()
        {
            @Override
            public void run()
            {
                picker.showSaveMenu(getClientEditor().getSite().getShell());
            }
        };
        save.setImageDescriptor(PortfolioPlugin.descriptor(PortfolioPlugin.IMG_SAVE));
        save.setToolTipText(Messages.MenuConfigureChart);
        new ActionContributionItem(save).fill(toolBar, -1);

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
        chart.getTitle().setVisible(false);

        picker = new ChartConfigurator(composite, this, ChartConfigurator.Mode.STATEMENT_OF_ASSETS);
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

            chart.getAxisSet().adjustRange();
        }
        finally
        {
            chart.suspendUpdate(false);
        }
        chart.redraw();
    }

    private void addClient(DataSeries item, List<Exception> warnings)
    {
        PerformanceIndex clientIndex = (PerformanceIndex) dataCache.get(Client.class);
        if (clientIndex == null)
        {
            clientIndex = PerformanceIndex.forClient(getClient(), getReportingPeriod(), warnings);
            dataCache.put(Client.class, clientIndex);
        }

        if (item.getInstance() != null)
        {
            ILineSeries series = chart.addDateSeries(clientIndex.getDates(), //
                            toDouble(clientIndex.getTotals(), Values.Amount.divider()), //
                            Messages.LabelTotalSum);
            item.configure(series);
        }
        else
        {
            IBarSeries barSeries = chart.addDateBarSeries(clientIndex.getDates(), //
                            toDouble(clientIndex.getTransferals(), Values.Amount.divider()), //
                            Messages.LabelTransferals);
            item.configure(barSeries);
        }
    }

    private void addSecurity(DataSeries item, List<Exception> warnings)
    {
        Security security = (Security) item.getInstance();
        PerformanceIndex securityIndex = (PerformanceIndex) dataCache.get(security);
        if (securityIndex == null)
        {
            securityIndex = PerformanceIndex.forInvestment(getClient(), security, getReportingPeriod(), warnings);
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
        PerformanceIndex portfolioIndex = (PerformanceIndex) dataCache.get(portfolio);
        if (portfolioIndex == null)
        {
            portfolioIndex = PerformanceIndex.forPortfolio(getClient(), portfolio, getReportingPeriod(), warnings);
            dataCache.put(portfolio, portfolioIndex);
        }

        ILineSeries series = chart.addDateSeries(portfolioIndex.getDates(), //
                        toDouble(portfolioIndex.getTotals(), Values.Amount.divider()), //
                        portfolio.getName());
        item.configure(series);
    }

    private void addAccount(DataSeries item, List<Exception> warnings)
    {
        Account account = (Account) item.getInstance();
        PerformanceIndex accountIndex = (PerformanceIndex) dataCache.get(account);
        if (accountIndex == null)
        {
            accountIndex = PerformanceIndex.forAccount(getClient(), account, getReportingPeriod(), warnings);
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
            index = PerformanceIndex.forClassification(getClient(), classification, getReportingPeriod(), warnings);
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
}
