package name.abuchen.portfolio.ui.views.dashboard;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.ImageHyperlink;

import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.Colors;

/**
 * Displays a button when the mouse moves into the given controls that allows
 * the user to click a hyperlink. The button is used for example to jump from
 * the dashboard to the underlying view.
 */
public class HoverButton
{
    private Control leadingControl;
    private IHyperlinkListener listener;

    private Composite hover;
    private long timestamp;

    private final Set<Control> watched = new HashSet<>();

    private final Listener parentResizeListener = this::onAnchorResize;
    private final Listener mouseFilter = this::onEnterOrExit;

    private HoverButton(Control leadingControl, Control... others)
    {
        this.leadingControl = leadingControl;

        watched.add(leadingControl);
        watched.addAll(Arrays.asList(others));

        // SWT sends enter and exit events only to the control under the
        // pointer, not to its parents. A filter also catches the children of
        // the watched controls, for example the cells of a heatmap, which are
        // recreated on every update.
        var display = leadingControl.getDisplay();
        display.addFilter(SWT.MouseEnter, mouseFilter);
        display.addFilter(SWT.MouseExit, mouseFilter);

        this.leadingControl.addListener(SWT.Resize, this::onAnchorResize);
        this.leadingControl.addListener(SWT.Move, this::onAnchorResize);

        this.leadingControl.addDisposeListener(e -> {
            display.removeFilter(SWT.MouseEnter, mouseFilter);
            display.removeFilter(SWT.MouseExit, mouseFilter);

            if (hover != null && !hover.isDisposed())
                hover.dispose();

            var p = leadingControl.getParent();
            if (p != null && !p.isDisposed())
                p.removeListener(SWT.Resize, parentResizeListener);
        });

        var parent = leadingControl.getParent();
        if (parent != null)
            parent.addListener(SWT.Resize, parentResizeListener);
    }

    public static HoverButton build(Control leadingControl, Control... others)
    {
        return new HoverButton(leadingControl, others);
    }

    public HoverButton withListener(IHyperlinkListener listener)
    {
        this.listener = listener;
        return this;
    }

    private void onEnterOrExit(Event event)
    {
        if (!(event.widget instanceof Control control) || !isWatched(control))
            return;

        if (event.type == SWT.MouseEnter)
            onMouseEnter(event);
        else
            onMouseExit(event);
    }

    private boolean isWatched(Control control)
    {
        for (var c = control; c != null; c = c.getParent())
        {
            if (watched.contains(c))
                return true;
        }
        return false;
    }

    private void onMouseExit(Event event)
    {
        if (hover != null && !hover.isDisposed() && hover.getVisible())
        {
            timestamp = event.time;

            // Hide only if the pointer has not entered again. Moving from the
            // anchor onto the button fires exit, then enter, and the enter
            // clears the timestamp.
            event.display.timerExec(200, () -> {
                if (timestamp == event.time && hover != null && !hover.isDisposed() && hover.getVisible())
                    hover.setVisible(false);
            });
        }
    }

    private void onMouseEnter(Event event)
    {
        timestamp = 0;

        if (hover == null || hover.isDisposed())
            createHover();

        if (hover != null && !hover.isDisposed() && !hover.getVisible())
        {
            positionHover();
            hover.moveAbove(null);
            hover.setVisible(true);
        }
    }

    private void onAnchorResize(Event event)
    {
        if (hover != null && !hover.isDisposed() && hover.getVisible())
            positionHover();
    }

    private void createHover()
    {
        var parent = leadingControl.getParent();
        if (parent == null || parent.isDisposed())
            return;

        hover = new Composite(parent, SWT.NONE);
        hover.setVisible(false);
        excludeFromParentLayout(parent);
        hover.setData(UIConstants.CSS.CLASS_NAME, "hoverbutton"); //$NON-NLS-1$
        hover.setBackground(Colors.theme().defaultBackground());
        hover.setLayout(new FillLayout());

        var button = new ImageHyperlink(hover, SWT.NONE);
        button.setImage(Images.VIEW_SHARE.image());
        button.setData(UIConstants.CSS.CLASS_NAME, "hoverbutton"); //$NON-NLS-1$
        button.setBackground(Colors.theme().defaultBackground());
        if (listener != null)
            button.addHyperlinkListener(listener);

        watched.add(hover);
    }

    private void positionHover()
    {
        Point size = hover.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        Rectangle bounds = leadingControl.getBounds();
        hover.setBounds(bounds.x + bounds.width - size.x, bounds.y, size.x, size.y);
        hover.layout();
    }

    private void excludeFromParentLayout(Composite parent)
    {
        if (parent.getLayout() instanceof GridLayout)
        {
            var data = new GridData();
            data.exclude = true;
            hover.setLayoutData(data);
        }
    }
}
