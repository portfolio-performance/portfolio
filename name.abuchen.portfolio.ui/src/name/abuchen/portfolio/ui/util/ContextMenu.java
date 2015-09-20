package name.abuchen.portfolio.ui.util;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

public class ContextMenu
{
    private final Control owner;
    private final Menu contextMenu;

    public ContextMenu(Control owner, IMenuListener listener)
    {
        this.owner = owner;

        MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(listener);

        contextMenu = menuMgr.createContextMenu(owner);

        owner.addDisposeListener(e -> dispose());
    }

    public ContextMenu hook()
    {
        owner.setMenu(contextMenu);
        return this;
    }

    public Menu getMenu()
    {
        return contextMenu;
    }

    private void dispose()
    {
        if (!contextMenu.isDisposed())
            contextMenu.dispose();
    }
}
