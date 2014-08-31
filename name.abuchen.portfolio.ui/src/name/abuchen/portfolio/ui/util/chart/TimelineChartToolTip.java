package name.abuchen.portfolio.ui.util.chart;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import name.abuchen.portfolio.ui.Messages;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
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
    public static final int PADDING = 5;

    private Chart chart = null;
    private Shell tip = null;
    private Object focus = null;

    private String dateFormat = "%tF"; //$NON-NLS-1$
    private DecimalFormat valueFormat = new DecimalFormat("#,##0.00"); //$NON-NLS-1$

    private boolean categoryEnabled = false;

    public TimelineChartToolTip(Chart chart)
    {
        this.chart = chart;

        Composite plotArea = chart.getPlotArea();
        plotArea.addListener(SWT.MouseDown, this);
        plotArea.addListener(SWT.MouseMove, this);
        plotArea.addListener(SWT.MouseUp, this);
        plotArea.addListener(SWT.Dispose, this);
    }

    public void enableCategory(boolean enabled)
    {
        categoryEnabled = enabled;
    }

    public void handleEvent(Event event)
    {
        switch (event.type)
        {
            case SWT.Dispose:
            case SWT.MouseUp:
                closeToolTip();
                break;
            case SWT.MouseMove:
                moveToolTip(event);
                break;
            case SWT.MouseDown:
                if (event.button == 1 && event.stateMask != SWT.MOD1)
                    showToolTip(event);
                break;
        }
    }

    public void setDateFormat(String dateFormat)
    {
        this.dateFormat = dateFormat;
    }

    public void setValueFormat(DecimalFormat valueFormat)
    {
        this.valueFormat = valueFormat;
    }

    private void closeToolTip()
    {
        if (tip != null)
        {
            tip.dispose();
            tip = null;
        }
    }

    private void showToolTip(Event event)
    {
        focus = getFocusObjectAt(event);

        if (tip != null && !tip.isDisposed())
            tip.dispose();

        tip = new Shell(Display.getDefault().getActiveShell(), SWT.ON_TOP | SWT.TOOL);
        tip.setLayout(new FillLayout());

        Point size = createComposite();
        Rectangle bounds = calculateBounds(event, size);

        tip.setBounds(bounds);
        tip.setVisible(true);
    }

    private void moveToolTip(Event event)
    {
        if (tip == null || tip.isDisposed())
            return;

        Object newTipDate = getFocusObjectAt(event);
        boolean dateChanged = focus != null && !focus.equals(newTipDate);

        if (dateChanged)
        {
            // delete composite
            for (Control c : tip.getChildren())
                c.dispose();

            // re-create labels
            focus = newTipDate;
            Point size = createComposite();

            size = tip.computeSize(SWT.DEFAULT, SWT.DEFAULT);
            Rectangle bounds = calculateBounds(event, size);
            tip.setBounds(bounds);
        }
        else
        {
            Point size = tip.getSize();
            Rectangle bounds = calculateBounds(event, size);
            tip.setLocation(new Point(bounds.x, bounds.y));
        }
    }

    private Object getFocusObjectAt(Event event)
    {
        return categoryEnabled ? getFocusCategoryAt(event) : getFocusDateAt(event);
    }

    private Integer getFocusCategoryAt(Event event)
    {
        IAxis xAxis = chart.getAxisSet().getXAxes()[0];
        int coordinate = (int) xAxis.getDataCoordinate(event.x);

        String[] categories = xAxis.getCategorySeries();

        if (coordinate < 0)
            coordinate = 0;
        else if (coordinate > categories.length - 1)
            coordinate = categories.length - 1;

        return coordinate;
    }

    private Date getFocusDateAt(Event event)
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

        int length = timeSeries.getXDateSeries().length;
        if (line >= length)
            return timeSeries.getXDateSeries()[length - 1];

        // check which date is closer to the targeted date
        long target = cal.getTimeInMillis();
        Date left = timeSeries.getXDateSeries()[line - 1];
        Date right = timeSeries.getXDateSeries()[line];

        return target - left.getTime() < right.getTime() - target ? left : right;
    }

    private Point createComposite()
    {
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
        right.setText(categoryEnabled ? chart.getAxisSet().getXAxis(0).getCategorySeries()[(Integer) focus] : String
                        .format(dateFormat, focus));

        for (ISeries series : chart.getSeriesSet().getSeries())
        {
            double value;

            if (categoryEnabled)
            {
                int line = (Integer) focus;
                value = series.getYSeries()[line];
            }
            else
            {
                int line = Arrays.binarySearch(series.getXDateSeries(), focus);
                if (line < 0)
                    continue;
                value = series.getYSeries()[line];
            }

            left = new Label(container, SWT.NONE);
            left.setForeground(foregroundColor);
            left.setText(series.getId());

            right = new Label(container, SWT.RIGHT);
            right.setForeground(foregroundColor);
            right.setText(valueFormat.format(value));
            GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).applyTo(right);
        }

        tip.layout();
        return tip.computeSize(SWT.DEFAULT, SWT.DEFAULT);
    }

    private Rectangle calculateBounds(Event event, Point size)
    {
        Rectangle plotArea = chart.getPlotArea().getClientArea();

        int x = event.x + (size.x / 2) > plotArea.width ? plotArea.width - size.x : event.x - (size.x / 2);
        x = Math.max(x, 0);

        int y = event.y + size.y + PADDING > plotArea.height ? event.y - size.y - PADDING : event.y + PADDING;
        y = Math.max(y, 0);
        y = Math.min(y, plotArea.height - size.y - PADDING);

        Point pt = chart.getPlotArea().toDisplay(x, y);
        return new Rectangle(pt.x, pt.y, size.x, size.y);
    }

}
