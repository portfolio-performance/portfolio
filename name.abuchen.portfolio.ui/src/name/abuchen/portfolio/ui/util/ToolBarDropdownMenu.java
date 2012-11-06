package name.abuchen.portfolio.ui.util;

import name.abuchen.portfolio.ui.PortfolioPlugin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

public abstract class ToolBarDropdownMenu<E> extends SelectionAdapter
{
    private static final String INDEX = "$index"; //$NON-NLS-1$

    private ToolBar toolBar;
    private ToolItem dropdown;
    private Menu menu;

    public ToolBarDropdownMenu(ToolBar toolBar)
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
        return (Integer) dropdown.getData(INDEX);
    }

    public void select(int index)
    {
        int selected = index >= 0 && index < menu.getItemCount() ? index : 0;
        menu.getItem(selected).notifyListeners(SWT.Selection, new Event());
    }

    public void select(E data)
    {
        for (MenuItem item : menu.getItems())
        {
            if (item.getData() == data)
            {
                item.notifyListeners(SWT.Selection, new Event());
                return;
            }
        }

        select(0);
    }

    public final void add(final E data, String label, String imageKey)
    {
        MenuItem menuItem = new MenuItem(menu, SWT.CHECK);
        menuItem.setText(label);
        if (imageKey != null)
            menuItem.setImage(PortfolioPlugin.image(imageKey));

        menuItem.setData(data);
        menuItem.setData(INDEX, Integer.valueOf(menu.getItemCount() - 1));

        menuItem.addSelectionListener(new SelectionAdapter()
        {
            public void widgetSelected(SelectionEvent event)
            {
                MenuItem selected = (MenuItem) event.widget;

                for (MenuItem item : menu.getItems())
                    item.setSelection(false);
                selected.setSelection(true);

                if (selected.getImage() != null)
                    dropdown.setImage(selected.getImage());
                else
                    dropdown.setText(selected.getText());

                dropdown.setToolTipText(selected.getText());
                dropdown.setData(INDEX, selected.getData(INDEX));

                toolBar.getParent().layout();

                itemSelected(data);
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

    protected abstract void itemSelected(E data);
}
