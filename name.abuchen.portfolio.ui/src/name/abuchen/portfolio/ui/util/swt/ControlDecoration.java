package name.abuchen.portfolio.ui.util.swt;

import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.ui.util.InfoToolTip;

/**
 * Simplified ControlDecoration based on
 * org.eclipse.jface.fieldassist.ControlDecoration. Supports CSS styling by
 * using plain old tool tips.
 */
public class ControlDecoration
{
    private Control control;

    private Image image;

    private String descriptionText;

    private int position;

    private boolean visible = true;

    private int marginWidth = 0;

    private DisposeListener disposeListener;
    private PaintListener paintListener;
    private MouseTrackListener mouseTrackListener;

    private InfoToolTip tooltip;

    public ControlDecoration(Control control, int position)
    {
        this.position = position;
        this.control = control;

        tooltip = InfoToolTip.attach(control, ToolTip.RECREATE, this::getDescriptionText);

        addControlListeners();
    }

    public void dispose()
    {
        if (control == null)
            return;

        removeControlListeners();
        control = null;
    }

    private void addControlListeners()
    {
        disposeListener = event -> dispose();
        control.addDisposeListener(disposeListener);

        paintListener = event -> {
            if (shouldShowDecoration())
            {
                Rectangle rect = getDecorationRectangle((Control) event.widget);
                event.gc.drawImage(getImage(), rect.x, rect.y);
            }
        };

        mouseTrackListener = new MouseTrackListener()
        {
            @Override
            public void mouseExit(MouseEvent event)
            {
                tooltip.hide();
            }

            @Override
            public void mouseHover(MouseEvent event)
            {
                Rectangle decorationRectangle = getDecorationRectangle((Control) event.widget);
                if (decorationRectangle.contains(event.x, event.y))
                    tooltip.show(new Point(0, 0));
            }

            @Override
            public void mouseEnter(MouseEvent event)
            {
                // Nothing to do until a hover occurs.
            }
        };

        // We do not know which parent in the control hierarchy
        // is providing the decoration space, so hook all the way up, until
        // the shell or the specified parent composite is reached.
        Composite c = control.getParent();
        while (c != null)
        {
            installCompositeListeners(c);
            if (c instanceof Shell)
            {
                // We just installed on a shell, so don't go further
                c = null;
            }
            else if (c instanceof ScrolledComposite)
            {
                // ScrolledComposites manage their contents
                c = null;
            }
            else
            {
                c = c.getParent();
            }
        }

        // force a redraw of the decoration area so our paint listener
        // is notified.
        update();
    }

    private void installCompositeListeners(Composite c)
    {
        if (!c.isDisposed())
        {
            c.addPaintListener(paintListener);
            c.addMouseTrackListener(mouseTrackListener);
        }
    }

    private void removeCompositeListeners(Composite c)
    {
        if (!c.isDisposed())
        {
            c.removePaintListener(paintListener);
            c.removeMouseTrackListener(mouseTrackListener);
        }
    }

    public void show()
    {
        if (!visible)
        {
            visible = true;
            update();
        }
    }

    public void hide()
    {
        if (visible)
        {
            visible = false;
            tooltip.hide();
            update();
        }
    }

    public String getDescriptionText()
    {
        return descriptionText;
    }

    public void setDescriptionText(String text)
    {
        this.descriptionText = text;
        update();
    }

    public Image getImage()
    {
        return image;
    }

    public void setImage(Image image)
    {
        this.image = image;
        update();
    }

    public int getMarginWidth()
    {
        return marginWidth;
    }

    public void setMarginWidth(int marginWidth)
    {
        this.marginWidth = marginWidth;
        update();
    }

    /**
     * Something has changed, requiring redraw. Redraw the decoration and update
     * the hover text if appropriate.
     */
    protected void update()
    {
        if (control == null || control.isDisposed())
            return;
        Rectangle rect = getDecorationRectangle(control.getShell());
        control.getShell().redraw(rect.x, rect.y, rect.width, rect.height, true);
        control.getShell().update();
    }

    /*
     * Remove any listeners installed on the controls.
     */
    private void removeControlListeners()
    {
        if (control == null)
            return;

        control.removeDisposeListener(disposeListener);
        disposeListener = null;

        Composite c = control.getParent();
        while (c != null)
        {
            removeCompositeListeners(c);
            if (c instanceof Shell)
            {
                // We previously installed listeners only up to the first Shell
                // encountered, so stop.
                c = null;
            }
            else
            {
                c = c.getParent();
            }
        }
        paintListener = null;
        mouseTrackListener = null;
    }

    protected Rectangle getDecorationRectangle(Control targetControl)
    {
        if (getImage() == null || control == null)
            return new Rectangle(0, 0, 0, 0);

        Rectangle imageBounds = getImage().getBounds();
        Rectangle controlBounds = control.getBounds();
        int x;
        int y;

        // Compute x
        if ((position & SWT.RIGHT) == SWT.RIGHT)
        {
            x = controlBounds.x + controlBounds.width + marginWidth;
        }
        else
        {
            // default is left
            x = controlBounds.x - imageBounds.width - marginWidth;
        }

        // Compute y
        if ((position & SWT.TOP) == SWT.TOP)
        {
            y = controlBounds.y;
        }
        else if ((position & SWT.BOTTOM) == SWT.BOTTOM)
        {
            y = controlBounds.y + control.getBounds().height - imageBounds.height;
        }
        else
        {
            // default is center
            y = controlBounds.y + (control.getBounds().height - imageBounds.height) / 2;
        }

        // Now convert to coordinates relative to the target control.
        Point globalPoint = control.getParent().toDisplay(x, y);
        Point targetPoint;
        if (targetControl == null)
        {
            targetPoint = globalPoint;
        }
        else
        {
            targetPoint = targetControl.toControl(globalPoint);
        }

        return new Rectangle(targetPoint.x, targetPoint.y, imageBounds.width, imageBounds.height);
    }

    /*
     * Return true if the decoration should be shown, false if it should not.
     */
    private boolean shouldShowDecoration()
    {
        if (!visible)
            return false;
        if (control == null || control.isDisposed() || getImage() == null)
            return false;

        return control.isVisible();
    }

}
