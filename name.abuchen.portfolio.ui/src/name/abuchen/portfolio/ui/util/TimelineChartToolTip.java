package name.abuchen.portfolio.ui.util;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import name.abuchen.portfolio.ui.Messages;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.swtchart.Chart;
import org.swtchart.IAxis;
import org.swtchart.ISeries;

public class TimelineChartToolTip implements Listener
{
    private Chart chart = null;
    private Shell tip = null;
    private String dateFormat = "%tF"; //$NON-NLS-1$

    public TimelineChartToolTip(Chart chart)
    {
        this.chart = chart;

        Composite plotArea = chart.getPlotArea();
        plotArea.addListener(SWT.Dispose, this);
        plotArea.addListener(SWT.KeyDown, this);
        plotArea.addListener(SWT.MouseMove, this);
        plotArea.addListener(SWT.MouseHover, this);
    }

    public void handleEvent(Event event)
    {
        switch (event.type)
        {
            case SWT.Dispose:
            case SWT.KeyDown:
            case SWT.MouseMove:
                closeToolTip();
                break;
            case SWT.MouseHover:
                onMouseHover(event);
                break;
        }
    }

    public void setDateFormat(String dateFormat)
    {
        this.dateFormat = dateFormat;
    }

    private void closeToolTip()
    {
        if (tip != null)
        {
            tip.dispose();
            tip = null;
        }
    }

    private void onMouseHover(Event event)
    {
        Date hoverDate = getHoverDate(event);

        if (tip != null && !tip.isDisposed())
            tip.dispose();

        tip = new Shell(Display.getDefault().getActiveShell(), SWT.ON_TOP | SWT.TOOL);
        tip.setLayout(new FillLayout());

        final Composite container = new Composite(tip, SWT.NONE);
        container.setBackgroundMode(SWT.INHERIT_FORCE);
        GridLayoutFactory.swtDefaults().numColumns(2).applyTo(container);

        Color foregroundColor = tip.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND);
        container.setForeground(foregroundColor);
        container.setBackground(tip.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));

        Label left = new Label(container, SWT.NONE);
        left.setForeground(foregroundColor);
        left.setText(Messages.ColumnDate);

        Label right = new Label(container, SWT.NONE);
        right.setForeground(foregroundColor);
        right.setText(String.format(dateFormat, hoverDate));

        for (ISeries series : chart.getSeriesSet().getSeries())
        {
            int line = Arrays.binarySearch(series.getXDateSeries(), hoverDate);
            if (line < 0)
                continue;
            double value = series.getYSeries()[line];

            left = new Label(container, SWT.NONE);
            left.setForeground(foregroundColor);
            left.setText(series.getId());

            right = new Label(container, SWT.RIGHT);
            right.setForeground(foregroundColor);
            right.setText(String.format("%,.2f", value)); //$NON-NLS-1$
            GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).applyTo(right);
        }

        Point size = tip.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        Point pt = chart.getPlotArea().toDisplay(event.x, event.y);
        tip.setBounds(pt.x, pt.y, size.x, size.y);
        tip.setVisible(true);

        // close tool tip if focus is lost (e.g. when application is switched)
        container.addListener(SWT.FocusOut, new Listener()
        {
            @Override
            public void handleEvent(Event event)
            {
                closeToolTip();
            }
        });
        container.forceFocus();
    }

    private Date getHoverDate(Event event)
    {
        IAxis xAxis = chart.getAxisSet().getXAxes()[0];

        long time = (long) xAxis.getDataCoordinate(event.x);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        ISeries timeSeries = chart.getSeriesSet().getSeries()[0];
        int line = Arrays.binarySearch(timeSeries.getXDateSeries(), cal.getTime());

        if (line >= 0)
            return cal.getTime();

        // otherwise: find closest existing date
        line = -line - 1;

        if (line == 0)
            return timeSeries.getXDateSeries()[line];

        // check which date is closer to the targeted date
        long target = cal.getTimeInMillis();
        Date left = timeSeries.getXDateSeries()[line - 1];
        Date right = timeSeries.getXDateSeries()[line];

        return target - left.getTime() < right.getTime() - target ? left : right;
    }
}
