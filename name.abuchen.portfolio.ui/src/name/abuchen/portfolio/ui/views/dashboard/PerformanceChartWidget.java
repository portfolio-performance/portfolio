package name.abuchen.portfolio.ui.views.dashboard;

import java.io.IOException;
import java.text.DecimalFormat;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.swtchart.ILineSeries;
import org.swtchart.ISeries;
import org.swtchart.LineStyle;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.ContextMenu;
import name.abuchen.portfolio.ui.util.chart.TimelineChart;

public class PerformanceChartWidget extends WidgetDelegate
{
    private static final String CONFIG_PERIOD = "period"; //$NON-NLS-1$

    private ReportingPeriod reportingPeriod;

    private Label title;
    private TimelineChart chart;

    public PerformanceChartWidget(Widget widget, DashboardData dashboardData)
    {
        super(widget, dashboardData);

        String config = widget.getConfiguration().get(CONFIG_PERIOD);
        if (config == null || config.isEmpty())
            config = "L1Y0"; //$NON-NLS-1$

        try
        {
            this.reportingPeriod = ReportingPeriod.from(config);
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
            this.reportingPeriod = new ReportingPeriod.LastX(1, 0);
        }
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).applyTo(container);
        container.setBackground(parent.getBackground());

        title = new Label(container, SWT.NONE);
        title.setText(getWidget().getLabel());
        GridDataFactory.fillDefaults().grab(true, false).applyTo(title);

        chart = new TimelineChart(container);
        chart.getTitle().setVisible(false);
        chart.getAxisSet().getYAxis(0).getTick().setVisible(false);
        chart.getToolTip().setValueFormat(new DecimalFormat("0.##%")); //$NON-NLS-1$

        GC gc = new GC(container);
        gc.setFont(resources.getKpiFont());
        Point stringExtend = gc.stringExtent("X"); //$NON-NLS-1$
        gc.dispose();

        GridDataFactory.fillDefaults().hint(SWT.DEFAULT, stringExtend.y * 6).grab(true, false).applyTo(chart);

        return container;
    }

    @Override
    public void attachContextMenu(IMenuListener listener)
    {
        new ContextMenu(title, listener).hook();
    }

    @Override
    public void update()
    {
        title.setText(getWidget().getLabel());

        PerformanceIndex index = getDashboardData().calculate(PerformanceIndex.class, reportingPeriod);

        try
        {
            chart.suspendUpdate(true);

            for (ISeries s : chart.getSeriesSet().getSeries())
                chart.getSeriesSet().deleteSeries(s.getId());

            ILineSeries series = chart.addDateSeries(index.getDates(), //
                            index.getAccumulatedPercentage(), //
                            Messages.PerformanceChartLabelAccumulatedIRR);

            series.setLineColor(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
            series.setSymbolColor(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
            series.enableArea(false);
            series.setLineStyle(LineStyle.SOLID);

            chart.adjustRange();
        }
        finally
        {
            chart.suspendUpdate(false);
        }
        chart.redraw();
    }

}
