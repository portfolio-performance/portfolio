package name.abuchen.portfolio.ui.views;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.model.Category;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ConsumerPriceIndex;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.snapshot.Aggregation;
import name.abuchen.portfolio.snapshot.CPIIndex;
import name.abuchen.portfolio.snapshot.CategoryIndex;
import name.abuchen.portfolio.snapshot.ClientIndex;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.SecurityIndex;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.AbstractCSVExporter;
import name.abuchen.portfolio.ui.util.AbstractDropDown;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.TimelineChart;
import name.abuchen.portfolio.ui.util.TimelineChartCSVExporter;
import name.abuchen.portfolio.ui.views.ChartSeriesPicker.Item;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.swtchart.IBarSeries;
import org.swtchart.ISeries;
import org.swtchart.LineStyle;

public class PerformanceChartView extends AbstractHistoricView
{
    private TimelineChart chart;
    private ChartSeriesPicker picker;
    private ColorWheel colorWheel;
    private ColorWheel securityColorWheel;

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
        picker = new ChartSeriesPicker(PerformanceChartView.class.getSimpleName(), parent, getClientEditor(),
                        ChartSeriesPicker.Mode.PERFORMANCE);
        picker.setListener(new ChartSeriesPicker.Listener()
        {
            @Override
            public void onUpdate()
            {
                updateChart();
            }
        });

        colorWheel = new ColorWheel(parent, 30);
        securityColorWheel = new ColorWheel(parent, 10);

        chart = new TimelineChart(parent);
        chart.getAxisSet().getYAxis(0).getTick().setFormat(new DecimalFormat("0.#%")); //$NON-NLS-1$
        chart.getTitle().setVisible(false);
        chart.getLegend().setVisible(true);
        chart.getLegend().setPosition(SWT.BOTTOM);
        chart.getToolTip().setValueFormat(new DecimalFormat("0.##%")); //$NON-NLS-1$
        chart.getToolTip().setReferenceSeries(Messages.PerformanceChartLabelAccumulatedIRR);

        // force layout, otherwise range calculation of chart does not work
        parent.layout();
        updateChart();

        return chart;
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

            for (Item item : picker.getSelectedItems())
            {
                if (item.getType() == Client.class)
                    addClient((Client) item.getInstance(), warnings);
                else if (item.getType() == ConsumerPriceIndex.class)
                    addConsumerPriceIndex(warnings);
                else if (item.getType() == Security.class)
                    addSecurity((Security) item.getInstance(), warnings);
                else if (item.getType() == Category.class)
                    addCategory((Category) item.getInstance(), warnings);
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

    private void addClient(Client client, List<Exception> warnings)
    {
        ClientIndex clientIndex = getClientIndex(warnings);
        PerformanceIndex aggregatedIndex = aggregationPeriod != null ? Aggregation.aggregate(clientIndex,
                        aggregationPeriod) : clientIndex;

        if (client != null)
        {
            chart.addDateSeries(aggregatedIndex.getDates(), //
                            aggregatedIndex.getAccumulatedPercentage(), //
                            Colors.IRR, Messages.PerformanceChartLabelAccumulatedIRR);
        }
        else
        {
            IBarSeries barSeries = chart.addDateBarSeries(aggregatedIndex.getDates(), //
                            aggregatedIndex.getDeltaPercentage(), //
                            aggregationPeriod != null ? aggregationPeriod.toString() : Messages.LabelAggregationDaily);
            barSeries.setBarPadding(50);
            barSeries.setBarColor(Display.getDefault().getSystemColor(SWT.COLOR_GRAY));
        }
    }

    private void addConsumerPriceIndex(List<Exception> warnings)
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
            chart.addDateSeries(cpiIndex.getDates(), //
                            cpiIndex.getAccumulatedPercentage(), //
                            Colors.CPI, Messages.PerformanceChartLabelCPI) //
                            .setLineStyle(LineStyle.DASHDOTDOT);
        }
    }

    private void addSecurity(Security security, List<Exception> warnings)
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

        int index = getClient().getSecurities().indexOf(security);
        chart.addDateSeries(securityIndex.getDates(), //
                        securityIndex.getAccumulatedPercentage(), //
                        securityColorWheel.getSegment(index).getColor(), security.getName());
    }

    private void addCategory(Category category, List<Exception> warnings)
    {
        PerformanceIndex categoryIndex = (PerformanceIndex) dataCache.get(category);
        if (categoryIndex == null)
        {
            categoryIndex = CategoryIndex.forPeriod(getClient(), category, getReportingPeriod(), warnings);
            dataCache.put(category, categoryIndex);
        }

        if (aggregationPeriod != null)
            categoryIndex = Aggregation.aggregate(categoryIndex, aggregationPeriod);

        chart.addDateSeries(categoryIndex.getDates(), categoryIndex.getAccumulatedPercentage(),
                        colorWheel.getSegment(getClient().getRootCategory().flatten().indexOf(category)).getColor(),
                        category.getName());
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
