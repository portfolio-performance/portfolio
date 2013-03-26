package name.abuchen.portfolio.ui.views;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.snapshot.Aggregation;
import name.abuchen.portfolio.snapshot.CPIIndex;
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
    private static final int NUM_OF_COLORS = 10;
    private static final LineStyle[] LINE_STYLES = new LineStyle[] { LineStyle.SOLID, LineStyle.DOT, LineStyle.DASH,
                    LineStyle.DASHDOT, LineStyle.DASHDOTDOT };

    private SecurityPicker picker;

    private ColorWheel colorWheel;
    private TimelineChart chart;

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
        picker = new SecurityPicker(PerformanceChartView.class.getSimpleName(), parent, getClientEditor());
        picker.setListener(new SecurityPicker.SecurityListener()
        {
            @Override
            public void onAddition(Security[] securities)
            {
                ClientIndex index = (ClientIndex) dataCache.get(ClientIndex.class);
                ArrayList<Exception> warnings = new ArrayList<Exception>();
                for (Security security : securities)
                    dataCache.put(security, SecurityIndex.forClient(index, security, warnings));
                PortfolioPlugin.log(warnings);

                refreshChart();
            }

            @Override
            public void onRemoval(Security[] securities)
            {
                for (Security security : securities)
                    dataCache.remove(security);

                refreshChart();
            }
        });

        colorWheel = new ColorWheel(parent, NUM_OF_COLORS);

        chart = new TimelineChart(parent);
        chart.getAxisSet().getYAxis(0).getTick().setFormat(new DecimalFormat("0.#%")); //$NON-NLS-1$
        chart.getTitle().setVisible(false);
        chart.getLegend().setVisible(true);
        chart.getLegend().setPosition(SWT.BOTTOM);
        chart.getToolTip().setValueFormat(new DecimalFormat("0.##%")); //$NON-NLS-1$
        chart.getToolTip().setReferenceSeries(Messages.PerformanceChartLabelAccumulatedIRR);

        // force layout, otherwise range calculation of chart does not work
        parent.layout();
        rebuildDailyData();
        refreshChart();
        return chart;
    }

    @Override
    protected void reportingPeriodUpdated()
    {
        rebuildDailyData();
        refreshChart();
    }

    protected void rebuildDailyData()
    {
        dataCache.clear();

        ArrayList<Exception> warnings = new ArrayList<Exception>();

        ReportingPeriod interval = getReportingPeriod();
        ClientIndex index = ClientIndex.forPeriod(getClient(), interval, warnings);
        dataCache.put(ClientIndex.class, index);

        dataCache.put(CPIIndex.class, CPIIndex.forClient(index, warnings));

        for (Security security : picker.getSelectedSecurities())
        {
            SecurityIndex si = SecurityIndex.forClient(index, security, warnings);
            dataCache.put(security, si);
        }

        PortfolioPlugin.log(warnings);
    }

    protected void refreshChart()
    {
        try
        {
            chart.suspendUpdate(true);

            for (ISeries s : chart.getSeriesSet().getSeries())
                chart.getSeriesSet().deleteSeries(s.getId());

            ClientIndex index = (ClientIndex) dataCache.get(ClientIndex.class);

            for (Security security : picker.getSelectedSecurities())
            {
                PerformanceIndex securityIndex = (PerformanceIndex) dataCache.get(security);
                addSecuritySeries(security, //
                                aggregationPeriod != null ? Aggregation.aggregate(securityIndex, aggregationPeriod)
                                                : securityIndex);
            }

            if (aggregationPeriod == null || aggregationPeriod != Aggregation.Period.YEARLY)
                addCPISeries((CPIIndex) dataCache.get(CPIIndex.class));

            addYieldSeries(aggregationPeriod != null ? Aggregation.aggregate(index, aggregationPeriod) : index);

            chart.getAxisSet().adjustRange();
        }
        finally
        {
            chart.suspendUpdate(false);
        }
        chart.redraw();
    }

    private void addYieldSeries(PerformanceIndex index)
    {
        IBarSeries barSeries = chart.addDateBarSeries(index.getDates(), //
                        index.getDeltaPercentage(), //
                        aggregationPeriod != null ? aggregationPeriod.toString() : Messages.LabelAggregationDaily);
        barSeries.setBarPadding(50);
        barSeries.setBarColor(Display.getDefault().getSystemColor(SWT.COLOR_GRAY));

        chart.addDateSeries(index.getDates(), //
                        index.getAccumulatedPercentage(), //
                        Colors.IRR, Messages.PerformanceChartLabelAccumulatedIRR);
        chart.addDateSeries(index.getDates(), index.getInvested(), Colors.IRR, "Invested");
    }

    private void addCPISeries(CPIIndex cpiIndex)
    {
        if (cpiIndex.getDates().length > 0)
            chart.addDateSeries(cpiIndex.getDates(), //
                            cpiIndex.getAccumulatedPercentage(), //
                            Colors.CPI, Messages.PerformanceChartLabelCPI) //
                            .setLineStyle(LineStyle.DASHDOTDOT);
    }

    private void addSecuritySeries(Security security, PerformanceIndex securityIndex)
    {
        int index = getClient().getSecurities().indexOf(security);
        chart.addDateSeries(securityIndex.getDates(), //
                        securityIndex.getAccumulatedPercentage(), //
                        colorWheel.getSegment(index).getColor(), security.getName()) //
                        .setLineStyle(LINE_STYLES[(index / NUM_OF_COLORS) % LINE_STYLES.length]);
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
                    refreshChart();
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
                        refreshChart();
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
