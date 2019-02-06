package name.abuchen.portfolio.ui.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import name.abuchen.portfolio.ui.Images;

public class DropDown extends ContributionItem
{
    private String label;
    private String toolTip;
    private Images image;
    private int style;

    /**
     * The menu listener that creates the drop down menu
     */
    private IMenuListener menuListener;

    /**
     * If available, the default action is run when the user clicks on the text
     * (as opposed to a click on the down arrow to open the drop down menu).
     */
    private IAction defaultAction;

    /**
     * The ToolItem created for this item; <code>null</code> before creation and
     * after disposal.
     */
    private ToolItem widget = null;

    /**
     * The dispose listeners that will be attached to the ToolItem once it has
     * been created.
     */
    private List<DisposeListener> disposeListeners = new ArrayList<>();

    public DropDown(String label)
    {
        this(label, null, SWT.DROP_DOWN, null);
    }

    public DropDown(String label, Images image)
    {
        this(label, image, SWT.DROP_DOWN, null);
    }

    public DropDown(String label, IMenuListener menuListener)
    {
        this(label, null, SWT.DROP_DOWN, menuListener);
    }

    public DropDown(String label, Images image, int style)
    {
        this(label, image, style, null);
    }

    public DropDown(String label, Images image, int style, IMenuListener menuListener)
    {
        this.label = label;
        this.image = image;
        this.style = style;
        this.menuListener = menuListener;
    }

    public final String getLabel()
    {
        return label;
    }

    public final void setLabel(String label)
    {
        this.label = label;

        if (widget != null && !widget.isDisposed())
        {
            if (image != null && toolTip == null)
                widget.setToolTipText(label);

            if (image == null || style == SWT.DROP_DOWN)
                widget.setText(label);

            widget.getParent().getParent().layout();
        }
    }

    public final String getToolTip()
    {
        return toolTip;
    }

    public final void setToolTip(String toolTip)
    {
        this.toolTip = toolTip;

        if (widget != null && !widget.isDisposed())
            widget.setToolTipText(toolTip);
    }

    public Images getImage()
    {
        return image;
    }

    public final void setImage(Images image)
    {
        this.image = image;

        if (image != null && widget != null && !widget.isDisposed())
            widget.setImage(image.image());
    }

    public final IMenuListener getMenuListener()
    {
        return menuListener;
    }

    public final void setMenuListener(IMenuListener menuListener)
    {
        this.menuListener = menuListener;
    }

    public void setDefaultAction(IAction defaultAction)
    {
        this.defaultAction = defaultAction;
    }

    public final void addDisposeListener(DisposeListener listener)
    {
        disposeListeners.add(listener);
    }

    @Override
    public void fill(ToolBar parent, int index)
    {
        if (widget != null || parent == null)
            return;

        ToolItem ti = new ToolItem(parent, style, index);

        if (image != null)
        {
            ti.setImage(image.image());
        }

        if (image == null || style == SWT.DROP_DOWN)
        {
            ti.setText(label);
        }

        ti.setToolTipText(toolTip != null ? toolTip : label);

        ti.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            if (e.detail == SWT.ARROW || defaultAction == null)
            {
                ToolItem item = (ToolItem) e.widget;
                Rectangle rect = item.getBounds();
                Point pt = item.getParent().toDisplay(new Point(rect.x, rect.y));

                MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
                menuMgr.setRemoveAllWhenShown(true);
                menuMgr.addMenuListener(menuListener);

                Menu menu = menuMgr.createContextMenu(item.getParent());
                menu.setLocation(pt.x, pt.y + rect.height);
                menu.setVisible(true);

                item.addDisposeListener(event -> menu.dispose());
            }
            else
            {
                defaultAction.run();
            }
        }));

        ti.addDisposeListener(e -> disposeListeners.forEach(l -> l.widgetDisposed(e)));

        widget = ti;
    }

    @Override
    public void dispose()
    {
        if (widget != null)
        {
            widget.dispose();
            widget = null;
        }

        super.dispose();
    }
}
