package name.abuchen.portfolio.ui.views.dashboard;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;
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

    private final Listener parentResizeListener = this::onAnchorResize;

    /**
     * Listens for the pointer entering and leaving {@code leadingControl} and
     * {@code others}, and shows the corner button while the pointer is inside
     * any of them.
     */
    private HoverButton(Control leadingControl, Control... others)
    {
        this.leadingControl = leadingControl;

        this.leadingControl.addListener(SWT.MouseEnter, this::onMouseEnter);
        this.leadingControl.addListener(SWT.MouseExit, this::onMouseExit);
        this.leadingControl.addListener(SWT.Resize, this::onAnchorResize);
        this.leadingControl.addListener(SWT.Move, this::onAnchorResize);

        this.leadingControl.addDisposeListener(e -> {
            if (hover != null && !hover.isDisposed())
            {
                releaseFromParentLayout();
                hover.dispose();
            }

            var p = leadingControl.getParent();
            if (p != null && !p.isDisposed())
                p.removeListener(SWT.Resize, parentResizeListener);
        });

        var parent = leadingControl.getParent();
        if (parent != null)
            parent.addListener(SWT.Resize, parentResizeListener);

        for (Control c : others)
        {
            c.addListener(SWT.MouseEnter, this::onMouseEnter);
            c.addListener(SWT.MouseExit, this::onMouseExit);
        }
    }

    /**
     * Creates a hover button for {@code leadingControl}. The button is placed
     * on that control's top-right corner. Moving into {@code others} shows it
     * as well.
     */
    public static HoverButton build(Control leadingControl, Control... others)
    {
        return new HoverButton(leadingControl, others);
    }

    /**
     * Sets the action run when the corner button is clicked.
     */
    public HoverButton withListener(IHyperlinkListener listener)
    {
        this.listener = listener;
        return this;
    }

    /**
     * Hides the button after a short delay.
     */
    private void onMouseExit(Event event)
    {
        if (hover != null && !hover.isDisposed() && hover.getVisible())
        {
            timestamp = event.time;

            event.display.timerExec(200, () -> {
                if (timestamp == event.time && hover != null && !hover.isDisposed() && hover.getVisible())
                    hover.setVisible(false);
            });
        }
    }

    /**
     * Shows the corner button.
     */
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

    /**
     * Keeps a visible button on the anchor's top-right corner when the anchor
     * or its parent is resized or moved.
     */
    private void onAnchorResize(Event event)
    {
        if (hover != null && !hover.isDisposed() && hover.getVisible())
            positionHover();
    }

    /**
     * Creates the corner button as a child of the anchor's parent.
     */
    private void createHover()
    {
        var parent = leadingControl.getParent();
        if (parent == null || parent.isDisposed())
            return;

        hover = new Composite(parent, SWT.BORDER);
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

        hover.addListener(SWT.MouseEnter, this::onMouseEnter);
        hover.addListener(SWT.MouseExit, this::onMouseExit);
        button.addListener(SWT.MouseEnter, this::onMouseEnter);
        button.addListener(SWT.MouseExit, this::onMouseExit);
    }

    /**
     * Moves the button to the anchor's top-right corner, in the parent's
     * coordinates.
     */
    private void positionHover()
    {
        Point size = hover.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        Rectangle bounds = leadingControl.getBounds();
        hover.setBounds(bounds.x + bounds.width - size.x, bounds.y, size.x, size.y);
        hover.layout();
    }

    /**
     * Leaves the button out of the parent's layout. Its bounds are set
     * directly.
     */
    private void excludeFromParentLayout(Composite parent)
    {
        var layout = parent.getLayout();
        if (layout instanceof GridLayout)
        {
            var data = new GridData();
            data.exclude = true;
            hover.setLayoutData(data);
        }
        else if (layout instanceof RowLayout)
        {
            var data = new RowData();
            data.exclude = true;
            hover.setLayoutData(data);
        }
        else if (layout instanceof FillLayoutSkipping skipping)
            skipping.skip(hover);
        else if (layout instanceof FillLayout fillLayout)
            parent.setLayout(new FillLayoutSkipping(fillLayout, hover));
    }

    /**
     * Removes this button from the parent's fill layout when the anchor is
     * disposed.
     */
    private void releaseFromParentLayout()
    {
        var parent = hover.getParent();
        if (parent == null || parent.isDisposed())
            return;
        if (parent.getLayout() instanceof FillLayoutSkipping skipping)
            skipping.release(hover);
    }

    /**
     * {@link FillLayout} has no exclude flag and is final, so the parent keeps
     * the same margins and spacing while this layout skips the hover.
     */
    private static final class FillLayoutSkipping extends Layout
    {
        private final FillLayout fill;
        private final List<Control> skipped = new ArrayList<>();

        /**
         * Copies {@code fill}'s margins and spacing, and leaves {@code first}
         * out of the arrangement. Further hovers on the same parent are added
         * with {@link #skip(Control)}.
         */
        private FillLayoutSkipping(FillLayout fill, Control first)
        {
            this.fill = fill;
            skipped.add(first);
        }

        /**
         * Records {@code control} so later arrangements leave it out.
         */
        private void skip(Control control)
        {
            if (!skipped.contains(control))
                skipped.add(control);
        }

        /**
         * Stops leaving {@code control} out of the arrangement.
         */
        private void release(Control control)
        {
            skipped.remove(control);
        }

        /**
         * Returns the children {@link FillLayout} would place, without the
         * hover buttons recorded for this parent.
         */
        private Control[] arrangedChildren(Composite composite)
        {
            var children = composite.getChildren();
            var arranged = new Control[children.length];
            var count = 0;
            for (var child : children)
            {
                if (!skipped.contains(child))
                    arranged[count++] = child;
            }
            if (count == children.length)
                return children;
            var trimmed = new Control[count];
            System.arraycopy(arranged, 0, trimmed, 0, count);
            return trimmed;
        }

        /**
         * Measures the parent as {@link FillLayout} would, ignoring every
         * recorded hover button.
         */
        @Override
        protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache)
        {
            var children = arrangedChildren(composite);
            var count = children.length;
            var maxWidth = 0;
            var maxHeight = 0;
            for (var child : children)
            {
                var widthHint = wHint;
                var heightHint = hHint;
                if (count > 0)
                {
                    if (fill.type == SWT.HORIZONTAL && wHint != SWT.DEFAULT)
                        widthHint = Math.max(0, (wHint - (count - 1) * fill.spacing) / count);
                    if (fill.type == SWT.VERTICAL && hHint != SWT.DEFAULT)
                        heightHint = Math.max(0, (hHint - (count - 1) * fill.spacing) / count);
                }
                var size = child.computeSize(widthHint, heightHint, flushCache);
                maxWidth = Math.max(maxWidth, size.x);
                maxHeight = Math.max(maxHeight, size.y);
            }

            int width;
            int height;
            if (fill.type == SWT.HORIZONTAL)
            {
                width = count * maxWidth;
                if (count != 0)
                    width += (count - 1) * fill.spacing;
                height = maxHeight;
            }
            else
            {
                width = maxWidth;
                height = count * maxHeight;
                if (count != 0)
                    height += (count - 1) * fill.spacing;
            }
            width += fill.marginWidth * 2;
            height += fill.marginHeight * 2;
            if (wHint != SWT.DEFAULT)
                width = wHint;
            if (hHint != SWT.DEFAULT)
                height = hHint;
            return new Point(width, height);
        }

        /**
         * Places the children {@link FillLayout} would place, and does not give
         * a cell to any recorded hover button.
         */
        @Override
        protected void layout(Composite composite, boolean flushCache)
        {
            var children = arrangedChildren(composite);
            var count = children.length;
            if (count == 0)
                return;

            var rect = composite.getClientArea();
            var width = rect.width - fill.marginWidth * 2;
            var height = rect.height - fill.marginHeight * 2;
            if (fill.type == SWT.HORIZONTAL)
            {
                width -= (count - 1) * fill.spacing;
                var x = rect.x + fill.marginWidth;
                var extra = width % count;
                var y = rect.y + fill.marginHeight;
                var cellWidth = width / count;
                for (var i = 0; i < count; i++)
                {
                    var childWidth = cellWidth;
                    if (i == 0)
                        childWidth += extra / 2;
                    else if (i == count - 1)
                        childWidth += (extra + 1) / 2;
                    children[i].setBounds(x, y, childWidth, height);
                    x += childWidth + fill.spacing;
                }
            }
            else
            {
                height -= (count - 1) * fill.spacing;
                var x = rect.x + fill.marginWidth;
                var cellHeight = height / count;
                var y = rect.y + fill.marginHeight;
                var extra = height % count;
                for (var i = 0; i < count; i++)
                {
                    var childHeight = cellHeight;
                    if (i == 0)
                        childHeight += extra / 2;
                    else if (i == count - 1)
                        childHeight += (extra + 1) / 2;
                    children[i].setBounds(x, y, width, childHeight);
                    y += childHeight + fill.spacing;
                }
            }
        }
    }
}
