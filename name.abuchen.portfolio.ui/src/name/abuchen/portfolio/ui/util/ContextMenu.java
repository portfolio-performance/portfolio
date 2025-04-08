package name.abuchen.portfolio.ui.util;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

public class ContextMenu
{
    public static final String DEFAULT_MENU = "$defaultMenu$"; //$NON-NLS-1$

    private final Control owner;
    private final Menu menu;

    public ContextMenu(Control owner, IMenuListener listener)
    {
        this.owner = owner;

        MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(listener);

        menu = menuMgr.createContextMenu(owner);

        owner.addDisposeListener(e -> dispose());
    }

    public ContextMenu hook()
    {
        owner.setData(DEFAULT_MENU, menu);
        owner.setMenu(menu);
        return this;
    }

    public Menu getMenu()
    {
        return menu;
    }

    private void dispose()
    {
        if (!menu.isDisposed())
            menu.dispose();
    }
}
