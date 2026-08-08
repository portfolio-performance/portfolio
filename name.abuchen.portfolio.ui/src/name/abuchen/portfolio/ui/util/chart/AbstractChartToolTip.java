package name.abuchen.portfolio.ui.util.chart;

import java.util.Objects;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtchart.Chart;

import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.Colors;

public abstract class AbstractChartToolTip implements Listener
{
    public static final int PADDING = 5;

    private Chart chart = null;

    private Shell tip = null;
    private Object focus = null;

    private boolean isActive = true;
    private boolean showToolTip = false;
    private boolean isAltPressed = false;

    protected AbstractChartToolTip(Chart chart)
    {
        this.chart = chart;

        Composite plotArea = getPlotArea();
        plotArea.addListener(SWT.MouseDown, this);
        plotArea.addListener(SWT.MouseMove, this);
        plotArea.addListener(SWT.MouseUp, this);
        plotArea.addListener(SWT.Dispose, this);
        plotArea.addPaintListener(this::paintFocusLine);
    }

    public void setActive(boolean isActive)
    {
        this.isActive = isActive;
    }

    protected abstract Object getFocusObjectAt(Event event);

    protected abstract void createComposite(Composite parent);

    protected Chart getChart()
    {
        return chart;
    }

    protected Object getFocusedObject()
    {
        return focus;
    }

    protected boolean isAltPressed()
    {
        return isAltPressed;
    }

    @Override
    public void handleEvent(Event event)
    {
        if (!isActive)
            return;

        switch (event.type)
        {
            case SWT.Dispose:
                showToolTip = false;
                closeToolTip();
                break;
            case SWT.MouseUp:
                showToolTip = false;
                closeToolTip();
                redrawPlotArea();
                break;
            case SWT.MouseMove:
                if (showToolTip)
                    moveToolTip(event);
                break;
            case SWT.MouseDown:
                // open tooltip only on left-click or on left click with MOD3
                // (Alt on most platforms, Option on macOS)
                var isValidKey = event.stateMask == 0 || event.stateMask == SWT.MOD3;
                if (event.button == 1 && isValidKey)
                {
                    showToolTip = true;
                    isAltPressed = event.stateMask == SWT.MOD3;
                    showToolTip(event);
                }
                break;
            default:
                break;
        }
    }

    private void closeToolTip()
    {
        if (tip != null)
        {
            tip.dispose();
            tip = null;
        }
    }

    private Point createAndMeasureTooltip()
    {
        createComposite(tip);
        tip.layout();
        return tip.computeSize(SWT.DEFAULT, SWT.DEFAULT);
    }

    private void showToolTip(Event event)
    {
        focus = getFocusObjectAt(event);

        if (focus == null) // nothing to show
            return;

        if (tip != null && !tip.isDisposed())
            tip.dispose();

        tip = new Shell(Display.getDefault().getActiveShell(), SWT.ON_TOP | SWT.TOOL);
        tip.setLayout(new FillLayout());
        tip.setData(UIConstants.CSS.CLASS_NAME, "tooltip"); //$NON-NLS-1$

        Point size = createAndMeasureTooltip();
        Rectangle bounds = calculateBounds(event, size);

        tip.setBounds(bounds);
        tip.setVisible(true);

        redrawPlotArea();
    }

    private void moveToolTip(Event event)
    {
        Object newFocusObject = getFocusObjectAt(event);
        boolean focusObjectChanged = !Objects.equals(focus, newFocusObject);

        boolean exists = tip != null && !tip.isDisposed();

        if (newFocusObject == null)
        {
            focus = null;
            closeToolTip();
            redrawPlotArea();
        }
        else if (focusObjectChanged && exists)
        {
            // delete composite
            for (Control c : tip.getChildren())
                c.dispose();

            // re-create labels
            focus = newFocusObject;
            Point size = createAndMeasureTooltip();

            Rectangle bounds = calculateBounds(event, size);
            tip.setBounds(bounds);

            redrawPlotArea();
        }
        else if (focusObjectChanged && !exists)
        {
            showToolTip(event);
        }
        else if (exists)
        {
            Point size = tip.getSize();
            Rectangle bounds = calculateBounds(event, size);
            tip.setLocation(new Point(bounds.x, bounds.y));
        }
    }

    private Rectangle calculateBounds(Event event, Point size)
    {
        Rectangle plotArea = getPlotArea().getClientArea();

        int x = event.x + (size.x / 2) > plotArea.width ? plotArea.width - size.x : event.x - (size.x / 2);
        x = Math.max(x, 0);

        Point pt = getPlotArea().toDisplay(x, event.y);
        // show above
        int y = pt.y - size.y - PADDING;

        return new Rectangle(pt.x, y, size.x, size.y);
    }

    protected final Composite getPlotArea()
    {
        if (chart != null)
            return (Composite) chart.getPlotArea();
        else
            throw new IllegalArgumentException("no plot area found"); //$NON-NLS-1$
    }

    private void redrawPlotArea()
    {
        // only redraw if this tool tip actually draws a vertical focus line;
        // otherwise we would trigger needless repaints (e.g. pie/scatter)
        if (getFocusPixelX() < 0)
            return;

        Composite plotArea = getPlotArea();
        if (!plotArea.isDisposed())
            plotArea.redraw();
    }

    /**
     * Draws a vertical line at the focused position while the tool tip is
     * shown. This helps to pin point the current position on the chart.
     */
    private void paintFocusLine(PaintEvent e)
    {
        if (!isActive || !showToolTip || focus == null)
            return;

        int x = getFocusPixelX();
        if (x < 0)
            return;

        e.gc.setLineStyle(SWT.LINE_DOT);
        e.gc.setLineWidth(1);
        e.gc.setForeground(Colors.theme().defaultForeground());
        e.gc.drawLine(x, 0, x, e.height);
    }

    /**
     * Returns the horizontal pixel coordinate of the currently focused object
     * within the plot area, or a negative value if no vertical focus line
     * should be drawn. Subclasses that have a meaningful x-axis position
     * override this method.
     */
    protected int getFocusPixelX()
    {
        return -1;
    }
}
