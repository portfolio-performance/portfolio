package name.abuchen.portfolio.ui.util.chart;

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
import org.swtchart.Chart;

import name.abuchen.portfolio.ui.UIConstants;

public abstract class AbstractChartToolTip implements Listener
{
    public static final int PADDING = 5;

    private Chart chart = null;
    private Shell tip = null;
    private Object focus = null;
    private boolean isAltPressed = false;

    public AbstractChartToolTip(Chart chart)
    {
        this.chart = chart;

        Composite plotArea = chart.getPlotArea();
        plotArea.addListener(SWT.MouseDown, this);
        plotArea.addListener(SWT.MouseMove, this);
        plotArea.addListener(SWT.MouseUp, this);
        plotArea.addListener(SWT.Dispose, this);
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
                if (event.button == 1 && (event.stateMask & SWT.MOD1) != SWT.MOD1)
                {
                    isAltPressed = (event.stateMask & SWT.MOD3) == SWT.MOD3;
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
            Point size = createAndMeasureTooltip();

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
