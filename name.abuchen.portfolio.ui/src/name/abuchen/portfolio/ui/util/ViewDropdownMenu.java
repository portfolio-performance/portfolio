package name.abuchen.portfolio.ui.util;

import name.abuchen.portfolio.ui.PortfolioPlugin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

public class ViewDropdownMenu extends SelectionAdapter
{
    private ToolBar toolBar;
    private ToolItem dropdown;
    private Menu menu;

    public ViewDropdownMenu(ToolBar toolBar)
    {
        this.toolBar = toolBar;

        dropdown = new ToolItem(toolBar, SWT.DROP_DOWN);
        dropdown.addSelectionListener(this);

        menu = new Menu(dropdown.getParent().getShell());

        toolBar.addDisposeListener(new DisposeListener()
        {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                if (!menu.isDisposed())
                    menu.dispose();
            }
        });
    }

    public int getSelectedIndex()
    {
        return (Integer) dropdown.getData();
    }

    public void select(int index)
    {
        int selected = index >= 0 && index < menu.getItemCount() ? index : 0;
        menu.getItem(selected).notifyListeners(SWT.Selection, new Event());
    }

    public void add(String item, String imageKey, final Control viewer)
    {
        MenuItem menuItem = new MenuItem(menu, SWT.NONE);
        menuItem.setText(item);
        menuItem.setImage(PortfolioPlugin.image(imageKey));
        menuItem.setData(Integer.valueOf(menu.getItemCount() - 1));
        menuItem.addSelectionListener(new SelectionAdapter()
        {
            public void widgetSelected(SelectionEvent event)
            {
                MenuItem selected = (MenuItem) event.widget;
                dropdown.setImage(selected.getImage());
                dropdown.setToolTipText(selected.getText());
                dropdown.setData(selected.getData());

                toolBar.getParent().layout();

                Composite parent = viewer.getParent();
                parent.layout();

                StackLayout layout = (StackLayout) parent.getLayout();
                layout.topControl = viewer;
                parent.layout();
            }
        });
    }

    public void widgetSelected(SelectionEvent event)
    {
        ToolItem item = (ToolItem) event.widget;
        Rectangle rect = item.getBounds();
        Point pt = item.getParent().toDisplay(new Point(rect.x, rect.y));
        menu.setLocation(pt.x, pt.y + rect.height);
        menu.setVisible(true);
    }
}
