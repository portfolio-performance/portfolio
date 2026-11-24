package name.abuchen.portfolio.ui.util.chart;

import java.util.Objects;

import org.eclipse.swt.SWT;
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
    }

    public void setActive(boolean isActive)
    {
        this.isActive = isActive;

        if (!isActive)
        {
            // while inactive no event is received anymore: close an open tool
            // tip as the SWT.MouseUp event that closes it is never seen
            showToolTip = false;
            closeToolTip();
        }
    }

    protected abstract Object getFocusObjectAt(Event event);

    protected abstract void createComposite(Composite parent);

    /**
     * Updates the existing controls of the tool tip instead of disposing and
     * re-creating all of them whenever the mouse moves to another focus object.
     * Returns <code>true</code> if the content could be updated in place, and
     * <code>false</code> if the controls have to be re-created.
     */
    protected boolean updateContent()
    {
        return false;
    }

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
            case SWT.Dispose, SWT.MouseUp:
                showToolTip = false;
                closeToolTip();
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
        tip.layout(true, true);
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

        // the CSS styles are applied after the shell has been created: set a
        // background color right away so that areas that are not (yet) covered
        // by a child are not painted in black
        tip.setBackground(Colors.theme().defaultBackground());

        Point size = createAndMeasureTooltip();
        Rectangle bounds = calculateBounds(event, size);

        tip.setBounds(bounds);
        tip.setVisible(true);

        // paint now: otherwise the window is visible for a moment before its
        // children are painted
        tip.update();
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
        }
        else if (focusObjectChanged && exists)
        {
            focus = newFocusObject;

            // re-creating every control makes the tool tip lag behind the
            // mouse: update the existing ones whenever that is possible
            Point size = updateContent() ? relayout() : recreateContent();

            Rectangle bounds = calculateBounds(event, size);
            if (!bounds.equals(tip.getBounds()))
                tip.setBounds(bounds);

            // paint the new or re-created content right away: the area of a
            // freshly created control stays black until it is painted
            tip.update();
        }
        else if (focusObjectChanged && !exists)
        {
            showToolTip(event);
        }
        else if (exists)
        {
            Point size = tip.getSize();
            Rectangle bounds = calculateBounds(event, size);

            Point location = tip.getLocation();
            if (location.x != bounds.x || location.y != bounds.y)
                tip.setLocation(bounds.x, bounds.y);
        }
    }

    private Point recreateContent()
    {
        // delete composite
        for (Control c : tip.getChildren())
            c.dispose();

        // re-create labels
        return createAndMeasureTooltip();
    }

    private Point relayout()
    {
        tip.layout(true, true);
        return tip.computeSize(SWT.DEFAULT, SWT.DEFAULT);
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
            return (Composite) chart.getPlotArea().getControl();
        else
            throw new IllegalArgumentException("no plot area found"); //$NON-NLS-1$
    }
}
