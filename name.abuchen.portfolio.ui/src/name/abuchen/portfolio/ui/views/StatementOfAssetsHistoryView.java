package name.abuchen.portfolio.ui.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security.AssetClass;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.snapshot.ClientIndex;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
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
