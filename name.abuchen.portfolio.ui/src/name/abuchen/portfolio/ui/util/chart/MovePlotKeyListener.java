package name.abuchen.portfolio.ui.util.chart;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.swtchart.Chart;
import org.swtchart.IAxis;

public class MovePlotKeyListener implements Listener
{
    private final Chart chart;

    private MovePlotKeyListener(Chart chart)
    {
        this.chart = chart;
    }

    public static void attachTo(Chart chart)
    {
        Listener listener = new MovePlotKeyListener(chart);
        chart.getPlotArea().addListener(SWT.KeyDown, listener);
    }

    @Override
    public void handleEvent(Event event)
    {
        if (event.keyCode == SWT.ARROW_DOWN)
            arrowDown(event);
        else if (event.keyCode == SWT.ARROW_UP)
            arrowUp(event);
        else if (event.keyCode == SWT.ARROW_RIGHT)
            arrowRight(event);
        else if (event.keyCode == SWT.ARROW_LEFT)
            arrowLeft(event);
        else if (event.character == '0')
            adjustRange();
    }

    private void arrowDown(Event event)
    {
        if (event.stateMask == SWT.MOD1)
            for (IAxis axis : chart.getAxisSet().getYAxes())
                axis.zoomOut();
        else
            for (IAxis axis : chart.getAxisSet().getYAxes())
                axis.scrollDown();
        chart.redraw();
    }

    private void arrowUp(Event event)
    {
        if (event.stateMask == SWT.MOD1)
            for (IAxis axis : chart.getAxisSet().getYAxes())
                axis.zoomIn();
        else
            for (IAxis axis : chart.getAxisSet().getYAxes())
                axis.scrollUp();
        chart.redraw();
    }

    private void arrowRight(Event event)
    {
        if (event.stateMask == SWT.MOD1)
            for (IAxis axis : chart.getAxisSet().getXAxes())
                axis.zoomOut();
        else
            for (IAxis axis : chart.getAxisSet().getXAxes())
                axis.scrollUp();
        chart.redraw();
    }

    private void arrowLeft(Event event)
    {
        if (event.stateMask == SWT.MOD1)
            for (IAxis axis : chart.getAxisSet().getXAxes())
                axis.zoomIn();
        else
            for (IAxis axis : chart.getAxisSet().getXAxes())
                axis.scrollDown();
        chart.redraw();
    }

    private void adjustRange()
    {
        if (chart instanceof ScatterChart)
            ((ScatterChart) chart).adjustRange();
        else if (chart instanceof TimelineChart)
            ((TimelineChart) chart).adjustRange();
        else
            chart.getAxisSet().adjustRange();
        chart.redraw();
    }
}
