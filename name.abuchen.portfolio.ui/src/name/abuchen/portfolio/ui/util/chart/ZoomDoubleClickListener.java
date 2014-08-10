package name.abuchen.portfolio.ui.util.chart;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.swtchart.Chart;
import org.swtchart.IAxis;

public class ZoomDoubleClickListener implements Listener
{
    public static void attachTo(Chart chart)
    {
        Listener listener = new ZoomDoubleClickListener(chart);
        chart.getPlotArea().addListener(SWT.MouseDoubleClick, listener);
    }

    private final Chart chart;

    private ZoomDoubleClickListener(Chart chart)
    {
        this.chart = chart;
    }

    @Override
    public void handleEvent(Event event)
    {
        for (IAxis axis : chart.getAxisSet().getXAxes())
        {
            double coordinate = axis.getDataCoordinate(event.x);
            if (event.button == 1)
                axis.zoomIn(coordinate);
            else
                axis.zoomOut(coordinate);
        }

        for (IAxis axis : chart.getAxisSet().getYAxes())
        {
            double coordinate = axis.getDataCoordinate(event.y);
            if (event.button == 1)
                axis.zoomIn(coordinate);
            else
                axis.zoomOut(coordinate);
        }
        chart.redraw();
    }
}