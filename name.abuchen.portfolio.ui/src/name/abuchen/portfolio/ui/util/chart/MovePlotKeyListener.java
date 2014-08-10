package name.abuchen.portfolio.ui.util.chart;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.swtchart.Chart;
import org.swtchart.IAxis;

public class MovePlotKeyListener implements Listener
{
    public static void attachTo(Chart chart)
    {
        Listener listener = new MovePlotKeyListener(chart);
        chart.getPlotArea().addListener(SWT.KeyDown, listener);
    }

    private final Chart chart;

    private MovePlotKeyListener(Chart chart)
    {
        this.chart = chart;
    }

    @Override
    public void handleEvent(Event event)
    {
        if (event.keyCode == SWT.ARROW_DOWN)
        {
            if (event.stateMask == SWT.CTRL)
                chart.getAxisSet().zoomOut();
            else
                for (IAxis axis : chart.getAxisSet().getYAxes())
                    axis.scrollDown();
            chart.redraw();
        }
        else if (event.keyCode == SWT.ARROW_UP)
        {
            if (event.stateMask == SWT.CTRL)
                chart.getAxisSet().zoomIn();
            else
                for (IAxis axis : chart.getAxisSet().getYAxes())
                    axis.scrollUp();
            chart.redraw();
        }
        else if (event.keyCode == SWT.ARROW_LEFT)
        {
            for (IAxis axis : chart.getAxisSet().getXAxes())
                axis.scrollDown();
            chart.redraw();
        }
        else if (event.keyCode == SWT.ARROW_RIGHT)
        {
            for (IAxis axis : chart.getAxisSet().getXAxes())
                axis.scrollUp();
            chart.redraw();
        }
    }
}