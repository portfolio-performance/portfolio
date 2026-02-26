package name.abuchen.portfolio.ui.util.chart;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tracker;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.IAxis;
import org.eclipse.swtchart.Range;

import name.abuchen.portfolio.ui.PortfolioPlugin;

public class ZoomInAreaListener implements Listener
{
    public static void attachTo(Chart chart)
    {
        Listener listener = new ZoomInAreaListener(chart);
        chart.getPlotArea().getControl().addListener(SWT.MouseDown, listener);
    }

    private final Chart chart;

    private ZoomInAreaListener(Chart chart)
    {
        this.chart = chart;
    }

    @Override
    public void handleEvent(Event event)
    {
        if (event.button == 1 && event.stateMask == SWT.MOD1)
        {
            Tracker tracker = new Tracker((Composite) chart.getPlotArea().getControl(), SWT.RESIZE);
            tracker.setRectangles(new Rectangle[] { new Rectangle(event.x, event.y, 0, 0) });
            if (tracker.open())
            {
                Rectangle rectangle = tracker.getRectangles()[0];

                try
                {
                    for (IAxis axis : chart.getAxisSet().getXAxes())
                    {
                        Range range = new Range(axis.getDataCoordinate(rectangle.x), axis.getDataCoordinate(rectangle.x
                                        + rectangle.width));
                        axis.setRange(range);
                    }

                    for (IAxis axis : chart.getAxisSet().getYAxes())
                    {
                        Range range = new Range(axis.getDataCoordinate(rectangle.y), axis.getDataCoordinate(rectangle.y
                                        + rectangle.height));
                        axis.setRange(range);
                    }
                }
                catch (IllegalArgumentException ignore)
                {
                    PortfolioPlugin.log(ignore);
                }
                chart.redraw();
            }
            tracker.dispose();
        }
    }
}
