package name.abuchen.portfolio.ui.util.swt;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.util.ContextMenu;

/**
 * A custom sash layout that supports double-clicking to hide and unhide one
 * element. It supports only two children.
 */
public class SashLayout extends Layout
{
    private class SashMouseTrackListener extends MouseTrackAdapter
    {
        @Override
        public void mouseExit(MouseEvent e)
        {
            host.setCursor(null);
            divider.setVisible(false);
        }

        @Override
        public void mouseEnter(MouseEvent e)
        {
            divider.setVisible(true);
        }
    }

    private class SashMouseMoveListener implements MouseMoveListener
    {

        @Override
        public void mouseMove(MouseEvent e)
        {
            if (!isDragging)
            {
                if (sash.contains(e.x, e.y))
                    host.setCursor(host.getDisplay()
                                    .getSystemCursor(isHorizontal ? SWT.CURSOR_SIZEWE : SWT.CURSOR_SIZENS));
                else
                    host.setCursor(host.getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
            }
            else
            {
                adjustSize(e.x, e.y);
                host.layout();
                host.update();
            }
        }

    }

    private class SashMouseListener implements MouseListener
    {
        @Override
        public void mouseUp(MouseEvent e)
        {
            host.setCapture(false);
            isDragging = false;
        }

        @Override
        public void mouseDown(MouseEvent e)
        {
            if (e.button != 1)
                return;

            if (sash.contains(e.x, e.y))
            {
                isDragging = true;
                host.setCapture(true);
            }
        }

        @Override
        public void mouseDoubleClick(MouseEvent e)
        {
            flip();
        }
    }

    private static final int SASH_WIDTH = 10;
    private static final int MIN_WIDHT = 20;

    /**
     * orientation of the column arrangement
     */
    private final boolean isHorizontal;

    /**
     * location of the auto-collapsable column
     */
    private final boolean isBeginning;

    private String tag;

    private Composite host;

    private Label divider;
    private Rectangle sash = new Rectangle(0, 0, 1, 1);

    private boolean isDragging = false;

    public SashLayout(final Composite host, int style)
    {
        this.host = host;
        this.host.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

        this.isHorizontal = (style & SWT.HORIZONTAL) == SWT.HORIZONTAL;
        this.isBeginning = (style & SWT.BEGINNING) == SWT.BEGINNING;

        this.divider = new Label(host, SWT.NONE);
        this.divider.setImage(isHorizontal ? Images.HANDLE_NS.image() : Images.HANDLE_WE.image());
        this.divider.setVisible(false);

        MouseTrackAdapter mouseTrackListener = new SashMouseTrackListener();
        host.addMouseTrackListener(mouseTrackListener);
        divider.addMouseTrackListener(mouseTrackListener);

        MouseMoveListener mouseMoveListener = new SashMouseMoveListener();
        host.addMouseMoveListener(mouseMoveListener);
        divider.addMouseMoveListener(e -> {
            Point p = Display.getCurrent().map(divider, host, new Point(e.x, e.y));
            e.x = p.x;
            e.y = p.y;
            mouseMoveListener.mouseMove(e);
        });

        MouseListener mouseListener = new SashMouseListener();
        host.addMouseListener(mouseListener);
        divider.addMouseListener(new SashMouseListener()
        {
            @Override
            public void mouseDown(MouseEvent e)
            {
                Point p = Display.getCurrent().map(divider, host, new Point(e.x, e.y));
                e.x = p.x;
                e.y = p.y;
                super.mouseDown(e);
            }
        });
    }

    public void addQuickNavigation(IMenuListener menuListener)
    {
        new ContextMenu(divider, menuListener).hook();
    }

    protected void adjustSize(int curX, int curY)
    {
        List<Control> children = getChildren();

        Rectangle left = children.get(0).getBounds();
        Rectangle right = children.get(1).getBounds();
        if (left == null || right == null)
            return;

        int proposedSize = isHorizontal ? curX : curY;
        int totalSize = isHorizontal ? left.width + right.width : left.height + right.height;

        SashLayoutData data = getLayoutData(children.get(isBeginning ? 0 : 1));

        // if collapsed, drag only if proposed size is bigger than min width.
        // That excludes many accidental drags when trying to restore via double
        // click.

        if (data.size > 0 || (proposedSize > MIN_WIDHT && proposedSize < totalSize - MIN_WIDHT))
        {
            // ensure minimum size of a child (if not hidden)
            proposedSize = Math.max(MIN_WIDHT, proposedSize);
            proposedSize = Math.min(totalSize - MIN_WIDHT, proposedSize);
            data.size = isBeginning ? proposedSize : totalSize - proposedSize;
        }
    }

    @Override
    protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache)
    {
        return new Point(500, 200);
    }

    public List<Control> getChildren()
    {
        Control[] children = host.getChildren();

        List<Control> answer = new ArrayList<>();
        for (Control child : children)
            if (child != divider)
                answer.add(child);

        return answer;
    }

    public boolean isHidden()
    {
        SashLayoutData data = getLayoutData(getChildren().get(isBeginning ? 0 : 1));
        return data.size < 0;
    }

    public void flip()
    {
        SashLayoutData data = getLayoutData(getChildren().get(isBeginning ? 0 : 1));
        data.size *= -1;

        host.layout();
        host.update();

        divider.setVisible(false);
    }

    public String getTag()
    {
        return tag;
    }

    public void setTag(String tag)
    {
        this.tag = tag;
    }

    @Override
    protected void layout(Composite composite, boolean flushCache)
    {
        Rectangle bounds = composite.getBounds();
        if (composite instanceof Shell)
        {
            bounds = ((Shell) composite).getClientArea();
        }
        else
        {
            bounds.x = 0;
            bounds.y = 0;
        }

        List<Control> children = getChildren();
        if (children.size() != 2)
        {
            // should not happen, but if previous error just create one element,
            // render this one element fully

            if (children.size() == 1)
                children.get(0).setBounds(bounds);

            return;
        }

        int availableSize = (isHorizontal ? bounds.width : bounds.height) - SASH_WIDTH;

        int fixedSize = Math.max(0, getLayoutData(children.get(isBeginning ? 0 : 1)).size);
        int remaining = Math.max(0, availableSize - fixedSize);

        int pos = isHorizontal ? bounds.x : bounds.y;

        pos += layout(children.get(0), pos, isBeginning ? fixedSize : remaining, bounds);

        pos += layoutDivider(pos, bounds);

        layout(children.get(1), pos, isBeginning ? remaining : fixedSize, bounds);
    }

    private int layout(Control control, int newPosition, int newSize, Rectangle bounds)
    {
        Rectangle subBounds = isHorizontal ? new Rectangle(newPosition, bounds.y, newSize, bounds.height)
                        : new Rectangle(bounds.x, newPosition, bounds.width, newSize);

        control.setBounds(subBounds);

        return newSize;
    }

    private int layoutDivider(final int newPosition, Rectangle bounds)
    {
        // position the divider rectangle
        this.sash = isHorizontal ? new Rectangle(newPosition, bounds.y, SASH_WIDTH, bounds.height)
                        : new Rectangle(bounds.x, newPosition, bounds.width, SASH_WIDTH);
        host.redraw(sash.x, sash.y, sash.width, sash.height, false);

        // position the divider image handle
        Rectangle imageBounds = divider.getImage().getBounds();
        Rectangle dividerBounds = isHorizontal ? //
                        new Rectangle(newPosition + ((SASH_WIDTH - imageBounds.width) / 2),
                                        (sash.height - imageBounds.height) / 2, imageBounds.width, imageBounds.height)
                        : new Rectangle((sash.width - imageBounds.width) / 2,
                                        newPosition + (SASH_WIDTH - imageBounds.height) / 2, imageBounds.width,
                                        imageBounds.height);
        divider.setBounds(dividerBounds);

        return SASH_WIDTH;
    }

    private SashLayoutData getLayoutData(Control control)
    {
        SashLayoutData data = (SashLayoutData) control.getLayoutData();
        if (data == null)
        {
            data = new SashLayoutData();
            control.setLayoutData(data);
        }

        return data;
    }
}
