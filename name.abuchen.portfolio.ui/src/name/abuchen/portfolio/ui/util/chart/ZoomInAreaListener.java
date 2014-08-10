package name.abuchen.portfolio.ui.util.chart;

import name.abuchen.portfolio.ui.PortfolioPlugin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tracker;
import org.swtchart.Chart;
import org.swtchart.IAxis;
import org.swtchart.Range;

public class ZoomInAreaListener implements Listener
{
    public static void attachTo(Chart chart)
    {
        Listener listener = new ZoomInAreaListener(chart);
        chart.getPlotArea().addListener(SWT.MouseDown, listener);
        chart.getPlotArea().addListener(SWT.MouseUp, listener);
        chart.getPlotArea().addListener(SWT.MouseMove, listener);
    }

    private final Chart chart;

    private ZoomInAreaListener(Chart chart)
    {
        this.chart = chart;
    }

    private int x, y;
    private long mouseDownTime;

    @Override
    public void handleEvent(Event event)
    {

        switch (event.type)
        {
            case SWT.MouseDown:
                if (event.button == 1)
                {
                    x = event.x;
                    y = event.y;
                    mouseDownTime = System.currentTimeMillis();
                }
                break;
            case SWT.MouseUp:
                mouseDownTime = -1;
                break;
            case SWT.MouseMove:
                if (mouseDownTime > 0 && mouseDownTime + 100 < System.currentTimeMillis())
                {
                    Tracker tracker = new Tracker(chart.getPlotArea(), SWT.RESIZE);
                    tracker.setRectangles(new Rectangle[] { new Rectangle(x, y, 0, 0) });
                    if (tracker.open())
                    {
                        Rectangle rectangle = tracker.getRectangles()[0];

                        try
                        {
                            for (IAxis axis : chart.getAxisSet().getXAxes())
                            {
                                Range range = new Range(axis.getDataCoordinate(rectangle.x),
                                                axis.getDataCoordinate(rectangle.x + rectangle.width));
                                axis.setRange(range);
                            }

                            for (IAxis axis : chart.getAxisSet().getYAxes())
                            {
                                Range range = new Range(axis.getDataCoordinate(rectangle.y),
                                                axis.getDataCoordinate(rectangle.y + rectangle.height));
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
                    mouseDownTime = -1;
                }
                break;
            default:
        }
    }
}
