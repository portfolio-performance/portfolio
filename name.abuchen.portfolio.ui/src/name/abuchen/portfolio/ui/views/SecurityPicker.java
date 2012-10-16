package name.abuchen.portfolio.ui.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

/* package */class SecurityPicker implements IMenuListener
{
    public static interface SecurityListener
    {
        void onAddition(Security[] securities);

        void onRemoval(Security[] securities);
    }

    private final String identifier;
    private final Client client;

    private SecurityPicker.SecurityListener listener;

    private final List<Security> securities = new ArrayList<Security>();

    private Menu contextMenu;

    public SecurityPicker(String identifier, Control owner, Client client)
    {
        this.identifier = identifier + "-SECURITIES"; //$NON-NLS-1$
        this.client = client;

        owner.addDisposeListener(new DisposeListener()
        {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                SecurityPicker.this.widgetDisposed();
            }
        });

        load();
    }

    public void setListener(SecurityPicker.SecurityListener listener)
    {
        this.listener = listener;
    }

    public void showMenu(Shell shell)
    {
        if (contextMenu == null)
        {
            MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
            menuMgr.setRemoveAllWhenShown(true);
            menuMgr.addMenuListener(this);

            contextMenu = menuMgr.createContextMenu(shell);
        }

        contextMenu.setVisible(true);
    }

    public List<Security> getSelectedSecurities()
    {
        return securities;
    }

    private void widgetDisposed()
    {
        persist();

        if (contextMenu != null)
            contextMenu.dispose();
    }

    private void persist()
    {
        StringBuilder buf = new StringBuilder();
        for (Security s : securities)
        {
            if (buf.length() > 0)
                buf.append(',');
            buf.append(s.getUUID());
        }
        PortfolioPlugin.getDefault().getPreferenceStore().setValue(identifier, buf.toString());
    }

    private void load()
    {
        String config = PortfolioPlugin.getDefault().getPreferenceStore().getString(identifier);
        if (config == null || config.trim().length() == 0)
            return;

        Map<String, Security> uuid2security = new HashMap<String, Security>();
        for (Security s : client.getSecurities())
            uuid2security.put(s.getUUID(), s);

        String[] uuids = config.split(","); //$NON-NLS-1$
        for (String uuid : uuids)
        {
            Security s = uuid2security.get(uuid);
            if (s != null)
                securities.add(s);
        }
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        for (final Security security : securities)
        {
            Action action = new Action(security.getName())
            {
                @Override
                public void run()
                {
                    securities.remove(security);

                    if (listener != null)
                        listener.onRemoval(new Security[] { security });
                }
            };
            action.setChecked(true);
            manager.add(action);
        }

        manager.add(new Separator());

        manager.add(new Action(Messages.SecurityPickerMenuAddSecurity)
        {
            @Override
            public void run()
            {
                ElementListSelectionDialog dialog = new ElementListSelectionDialog(contextMenu.getShell(),
                                new LabelProvider()
                                {

                                    @Override
                                    public Image getImage(Object element)
                                    {
                                        return PortfolioPlugin.image(PortfolioPlugin.IMG_SECURITY);
                                    }

                                    @Override
                                    public String getText(Object element)
                                    {
                                        return ((Security) element).getName();
                                    }

                                });
                dialog.setElements(client.getSecurities().toArray());
                dialog.setTitle(Messages.SecurityPickerTitle);
                dialog.setMessage(Messages.SecurityPickerTitle);
                dialog.setMultipleSelection(true);

                if (dialog.open() != Window.OK)
                    return;

                Object[] result = dialog.getResult();
                for (Object object : result)
                    securities.add((Security) object);

                if (listener != null && result.length != 0)
                {
                    Security[] s = new Security[result.length];
                    System.arraycopy(result, 0, s, 0, result.length);
                    listener.onAddition(s);
                }
            }
        });
    }

}
