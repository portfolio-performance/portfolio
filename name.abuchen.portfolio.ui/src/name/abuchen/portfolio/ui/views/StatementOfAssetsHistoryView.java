package name.abuchen.portfolio.ui.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Category;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Security.AssetClass;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.snapshot.AccountIndex;
import name.abuchen.portfolio.snapshot.CategoryIndex;
import name.abuchen.portfolio.snapshot.ClientIndex;
import name.abuchen.portfolio.snapshot.PortfolioIndex;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.SecurityInvestmentIndex;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.TimelineChart;
import name.abuchen.portfolio.ui.util.TimelineChartCSVExporter;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;

public class StatementOfAssetsHistoryView extends AbstractHistoricView
{
    private TimelineChart chart;
    private ChartSeriesPicker picker;
    private ColorWheel colorWheel;
    private ColorWheel securityColorWheel;

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
        picker = new ChartSeriesPicker(StatementOfAssetsHistoryView.class.getSimpleName(), parent, getClientEditor());
        picker.setListener(new ChartSeriesPicker.Listener()
        {
            @Override
            public void onAddition(ChartSeriesPicker.Item[] items)
            {
                rebuildChart();
            }

            @Override
            public void onRemoval(ChartSeriesPicker.Item[] items)
            {
                rebuildChart();
            }
        });

        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new FillLayout());

        colorWheel = new ColorWheel(container, 30);
        securityColorWheel = new ColorWheel(container, 10);

        chart = buildChart(container);

        return container;
    }

    @Override
    protected void reportingPeriodUpdated()
    {
        dataCache.clear();
        rebuildChart();
    }

    private void rebuildChart()
    {
        Composite parent = chart.getParent();
        chart.dispose();
        chart = buildChart(parent);
        parent.layout(true);
    }

    protected TimelineChart buildChart(Composite parent)
    {
        TimelineChart chart = new TimelineChart(parent);
        chart.getTitle().setVisible(false);
        chart.getLegend().setVisible(true);
        chart.getLegend().setPosition(SWT.BOTTOM);

        ReportingPeriod period = getReportingPeriod();

        for (ChartSeriesPicker.Item item : picker.getSelectedItems())
        {
            if (item.getType() == Client.class)
            {
                ClientIndex clientIndex = (ClientIndex) dataCache.get(ClientIndex.class);
                if (clientIndex == null)
                {
                    clientIndex = ClientIndex.forPeriod(getClient(), period, new ArrayList<Exception>());
                    dataCache.put(ClientIndex.class, clientIndex);
                }

                if (item.getInstance() != null)
                {
                    chart.addDateSeries(clientIndex.getDates(),
                                    toDouble(clientIndex.getTotals(), Values.Amount.divider()), Colors.TOTALS,
                                    Messages.LabelTotalSum);
                }
                else
                {
                    chart.addDateBarSeries(clientIndex.getDates(),
                                    toDouble(clientIndex.getTransferals(), Values.Amount.divider()),
                                    Messages.LabelTransferals);
                }
            }
            else if (item.getType() == AssetClass.class)
            {
                ClientIndex clientIndex = (ClientIndex) dataCache.get(ClientIndex.class);
                if (clientIndex == null)
                {
                    clientIndex = ClientIndex.forPeriod(getClient(), period, new ArrayList<Exception>());
                    dataCache.put(ClientIndex.class, clientIndex);
                }

                AssetClass assetClass = (AssetClass) item.getInstance();

                chart.addDateSeries(clientIndex.getDates(),
                                toDouble(clientIndex.byAssetClass(assetClass), Values.Amount.divider()),
                                Colors.valueOf(assetClass.name()), assetClass.toString());
            }
            else if (item.getType() == Portfolio.class)
            {
                Portfolio portfolio = (Portfolio) item.getInstance();
                PortfolioIndex portfolioIndex = (PortfolioIndex) dataCache.get(portfolio);
                if (portfolioIndex == null)
                {
                    portfolioIndex = PortfolioIndex.forPeriod(getClient(), portfolio, period,
                                    new ArrayList<Exception>());
                    dataCache.put(portfolio, portfolioIndex);
                }

                chart.addDateSeries(portfolioIndex.getDates(),
                                toDouble(portfolioIndex.getTotals(), Values.Amount.divider()),
                                colorWheel.getSegment(getClient().getPortfolios().indexOf(portfolio)).getColor(),
                                portfolio.getName());
            }
            else if (item.getType() == Account.class)
            {
                Account account = (Account) item.getInstance();
                AccountIndex accountIndex = (AccountIndex) dataCache.get(account);
                if (accountIndex == null)
                {
                    accountIndex = AccountIndex.forPeriod(getClient(), account, period, new ArrayList<Exception>());
                    dataCache.put(account, accountIndex);
                }

                chart.addDateSeries(accountIndex.getDates(),
                                toDouble(accountIndex.getTotals(), Values.Amount.divider()),
                                colorWheel.getSegment(getClient().getAccounts().indexOf(account) + 10).getColor(),
                                account.getName());
            }
            else if (item.getType() == Category.class)
            {
                Category category = (Category) item.getInstance();
                CategoryIndex categoryIndex = (CategoryIndex) dataCache.get(category);
                if (categoryIndex == null)
                {
                    categoryIndex = CategoryIndex.forPeriod(getClient(), category, period, new ArrayList<Exception>());
                    dataCache.put(category, categoryIndex);
                }

                chart.addDateSeries(categoryIndex.getDates(),
                                toDouble(categoryIndex.getTotals(), Values.Amount.divider()),
                                colorWheel.getSegment(getClient().getRootCategory().flatten().indexOf(category))
                                                .getColor(), category.getName());

            }
            else if (item.getType() == Security.class)
            {
                Security security = (Security) item.getInstance();
                SecurityInvestmentIndex securityIndex = (SecurityInvestmentIndex) dataCache.get(security);
                if (securityIndex == null)
                {
                    securityIndex = SecurityInvestmentIndex.forPeriod(getClient(), security, period,
                                    new ArrayList<Exception>());
                    dataCache.put(security, securityIndex);
                }

                chart.addDateSeries(securityIndex.getDates(),
                                toDouble(securityIndex.getTotals(), Values.Amount.divider()), securityColorWheel
                                                .getSegment(getClient().getSecurities().indexOf(security)).getColor(),
                                security.getName());
            }
        }

        chart.getAxisSet().adjustRange();
        return chart;
    }

    private double[] toDouble(long[] input, double divider)
    {
        double[] answer = new double[input.length];
        for (int ii = 0; ii < answer.length; ii++)
            answer[ii] = input[ii] / divider;
        return answer;
    }
}
