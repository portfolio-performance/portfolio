package name.abuchen.portfolio.ui.views;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.snapshot.CPIIndex;
import name.abuchen.portfolio.snapshot.ClientIndex;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.SecurityIndex;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.TimelineChart;
import name.abuchen.portfolio.ui.util.TimelineChartCSVExporter;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
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

    @Override
    protected String getTitle()
    {
        return Messages.LabelPerformanceChart;
    }

    @Override
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
                exporter.addDiscontinousSeries(Messages.PerformanceChartLabelCPI);
                exporter.setDateFormat(new SimpleDateFormat("yyyy-MM-dd")); //$NON-NLS-1$
                exporter.setValueFormat(new DecimalFormat("0.##########")); //$NON-NLS-1$
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
        picker = new SecurityPicker(PerformanceChartView.class.getSimpleName(), parent, getClientEditor());
        picker.setListener(new SecurityPicker.SecurityListener()
        {
            @Override
            public void onAddition(Security[] securities)
            {
                refreshChart();
            }

            @Override
            public void onRemoval(Security[] securities)
            {
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

        // force layout, otherwise range calculation of chart does not work
        parent.layout();
        refreshChart();
        return chart;
    }

    @Override
    protected void reportingPeriodUpdated()
    {
        refreshChart();
    }

    protected void refreshChart()
    {
        try
        {
            chart.suspendUpdate(true);

            for (ISeries s : chart.getSeriesSet().getSeries())
                chart.getSeriesSet().deleteSeries(s.getId());

            ArrayList<Exception> warnings = new ArrayList<Exception>();

            ReportingPeriod interval = getReportingPeriod();
            ClientIndex index = ClientIndex.forPeriod(getClient(), interval, warnings);

            addYieldSeries(index);
            addCPISeries(CPIIndex.forClient(index, warnings));
            for (Security security : picker.getSelectedSecurities())
            {
                SecurityIndex si = SecurityIndex.forClient(index, security, warnings);
                addSecuritySeries(security, si);
            }

            if (!warnings.isEmpty())
            {
                for (Exception e : warnings)
                    PortfolioPlugin.log(e);
            }

            chart.getSeriesSet().bringToFront(Messages.PerformanceChartLabelCPI);
            chart.getSeriesSet().bringToFront(Messages.PerformanceChartLabelAccumulatedIRR);

            chart.getAxisSet().adjustRange();
        }
        finally
        {
            chart.suspendUpdate(false);
        }
        chart.redraw();
    }

    private void addYieldSeries(ClientIndex index)
    {
        IBarSeries barSeries = chart.addDateBarSeries(index.getDates(), //
                        index.getDeltaPercentage(), //
                        Messages.PerformanceChartLabelMonthly);
        barSeries.setBarPadding(50);
        barSeries.setBarColor(Display.getDefault().getSystemColor(SWT.COLOR_GRAY));

        chart.addDateSeries(index.getDates(), //
                        index.getAccumulatedPercentage(), //
                        Colors.IRR, Messages.PerformanceChartLabelAccumulatedIRR);
    }

    private void addCPISeries(CPIIndex cpiIndex)
    {
        chart.addDateSeries(cpiIndex.getDates(), //
                        cpiIndex.getAccumulatedPercentage(), //
                        Colors.CPI, Messages.PerformanceChartLabelCPI) //
                        .setLineStyle(LineStyle.DASHDOTDOT);
    }

    private void addSecuritySeries(Security security, SecurityIndex securityIndex)
    {
        int index = getClient().getSecurities().indexOf(security);
        chart.addDateSeries(securityIndex.getDates(), //
                        securityIndex.getAccumulatedPercentage(), //
                        colorWheel.getSegment(index).getColor(), security.getName()) //
                        .setLineStyle(LINE_STYLES[(index / NUM_OF_COLORS) % LINE_STYLES.length]);
    }

}
