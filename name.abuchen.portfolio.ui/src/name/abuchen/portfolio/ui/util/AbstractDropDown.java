package name.abuchen.portfolio.ui.util;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

public abstract class AbstractDropDown implements IMenuListener
{
    private ToolBar toolBar;
    private ToolItem dropdown;
    private MenuManager menuMgr;

    public AbstractDropDown(ToolBar toolBar, String label)
    {
        this.toolBar = toolBar;

        dropdown = new ToolItem(toolBar, SWT.DROP_DOWN);
        dropdown.setText(label);
        dropdown.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                AbstractDropDown.this.widgetSelected(e);
            }
        });

        menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(this);
    }

    private void widgetSelected(SelectionEvent event)
    {
        ToolItem item = (ToolItem) event.widget;
        Rectangle rect = item.getBounds();
        Point pt = item.getParent().toDisplay(new Point(rect.x, rect.y));

        Menu menu = menuMgr.createContextMenu(toolBar);
        menu.setLocation(pt.x, pt.y + rect.height);
        menu.setVisible(true);
    }

    protected ToolBar getToolBar()
    {
        return toolBar;
    }

    protected ToolItem getToolItem()
    {
        return dropdown;
    }

    public void setLabel(String label)
    {
        dropdown.setText(label);
        toolBar.getParent().layout();
    }
}
