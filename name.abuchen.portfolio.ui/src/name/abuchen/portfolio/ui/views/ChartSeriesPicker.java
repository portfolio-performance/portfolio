package name.abuchen.portfolio.ui.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Category;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Security.AssetClass;
import name.abuchen.portfolio.ui.ClientEditor;
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
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

/* package */class ChartSeriesPicker implements IMenuListener
{
    private final class SecurityLabelProvider extends LabelProvider
    {
        @Override
        public Image getImage(Object element)
        {
            return ((Item) element).getImage();
        }

        @Override
        public String getText(Object element)
        {
            return ((Item) element).getLabel();
        }
    }

    public class Item
    {
        private Class<?> type;
        private Object instance;
        private String label;

        private Item(Class<?> type, Object instance, String label)
        {
            this.type = type;
            this.instance = instance;
            this.label = label;
        }

        public String getLabel()
        {
            return label;
        }

        public Class<?> getType()
        {
            return type;
        }

        public Object getInstance()
        {
            return instance;
        }

        public Image getImage()
        {
            if (type == Security.class)
                return PortfolioPlugin.image(PortfolioPlugin.IMG_SECURITY);
            else if (type == Account.class)
                return PortfolioPlugin.image(PortfolioPlugin.IMG_ACCOUNT);
            else if (type == Portfolio.class)
                return PortfolioPlugin.image(PortfolioPlugin.IMG_PORTFOLIO);
            else if (type == Category.class)
                return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
            else
                return null;
        }

        public String getUUID()
        {
            if (type == Security.class)
                return Security.class.getSimpleName() + ((Security) instance).getUUID();
            else if (type == AssetClass.class)
                return AssetClass.class.getSimpleName() + ((AssetClass) instance).name();
            else if (type == Client.class)
                return Client.class.getSimpleName() + (instance != null ? "-totals" : "-transferals"); //$NON-NLS-1$ //$NON-NLS-2$
            else if (type == Account.class)
                return Account.class.getSimpleName() + ((Account) instance).getName();
            else if (type == Portfolio.class)
                return Portfolio.class.getSimpleName() + ((Portfolio) instance).getName();
            else if (type == Category.class)
                return Category.class.getSimpleName() + ((Category) instance).getName();

            throw new UnsupportedOperationException();
        }
    }

    public interface Listener
    {
        void onAddition(Item[] items);

        void onRemoval(Item[] items);
    }

    private final String identifier;
    private final ClientEditor clientEditor;
    private final Client client;

    private ChartSeriesPicker.Listener listener;

    private final List<Item> availableItems = new ArrayList<Item>();
    private final List<Item> selectedItems = new ArrayList<Item>();

    private Menu contextMenu;

    public ChartSeriesPicker(String identifier, Control owner, ClientEditor clientEditor)
    {
        this.identifier = identifier + "-PICKER"; //$NON-NLS-1$
        this.clientEditor = clientEditor;
        this.client = clientEditor.getClient();

        owner.addDisposeListener(new DisposeListener()
        {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                ChartSeriesPicker.this.widgetDisposed();
            }
        });

        buildAvailableItemList();
        load();

        if (selectedItems.isEmpty())
        {
            for (Item item : availableItems)
            {
                if (item.getType() == Client.class || item.getType() == AssetClass.class)
                    selectedItems.add(item);
            }
        }
    }

    public void setListener(ChartSeriesPicker.Listener listener)
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

    public List<Item> getSelectedItems()
    {
        return selectedItems;
    }

    private void widgetDisposed()
    {
        persist();

        if (contextMenu != null)
            contextMenu.dispose();
    }

    private void buildAvailableItemList()
    {
        availableItems.add(new Item(Client.class, client, Messages.LabelTotalSum));

        for (AssetClass assetClass : AssetClass.values())
            availableItems.add(new Item(AssetClass.class, assetClass, assetClass.toString()));

        availableItems.add(new Item(Client.class, null, Messages.LabelTransferals));

        for (Security security : client.getSecurities())
            availableItems.add(new Item(Security.class, security, security.getName()));

        for (Account account : client.getAccounts())
            availableItems.add(new Item(Account.class, account, account.getName()));

        for (Portfolio portfolio : client.getPortfolios())
            availableItems.add(new Item(Portfolio.class, portfolio, portfolio.getName()));

        LinkedList<Category> stack = new LinkedList<Category>();
        stack.add(client.getRootCategory());

        while (!stack.isEmpty())
        {
            Category category = stack.removeFirst();
            for (Category child : category.getChildren())
            {
                availableItems.add(new Item(Category.class, child, child.getName()));
                stack.add(child);
            }
        }
    }

    private void load()
    {
        String config = clientEditor.getPreferenceStore().getString(identifier);
        if (config == null || config.trim().length() == 0)
            return;

        Map<String, Item> uuid2items = new HashMap<String, Item>();
        for (Item item : availableItems)
            uuid2items.put(item.getUUID(), item);

        String[] uuids = config.split(","); //$NON-NLS-1$
        for (String uuid : uuids)
        {
            Item s = uuid2items.get(uuid);
            if (s != null)
                selectedItems.add(s);
        }
    }

    private void persist()
    {
        StringBuilder buf = new StringBuilder();
        for (Item s : selectedItems)
        {
            if (buf.length() > 0)
                buf.append(',');
            buf.append(s.getUUID());
        }
        clientEditor.getPreferenceStore().setValue(identifier, buf.toString());
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        for (final Item security : selectedItems)
        {
            Action action = new Action(security.getLabel())
            {
                @Override
                public void run()
                {
                    selectedItems.remove(security);

                    if (listener != null)
                        listener.onRemoval(new Item[] { security });
                }
            };
            action.setChecked(true);
            manager.add(action);
        }

        manager.add(new Separator());

        manager.add(new Action(Messages.ChartSeriesPickerAddItem)
        {
            @Override
            public void run()
            {
                List<Item> list = new ArrayList<Item>(availableItems);
                for (Item s : selectedItems)
                    list.remove(s);

                ElementListSelectionDialog dialog = new ElementListSelectionDialog(contextMenu.getShell(),
                                new SecurityLabelProvider());
                dialog.setElements(list.toArray());
                dialog.setTitle(Messages.ChartSeriesPickerTitle);
                dialog.setMessage(Messages.ChartSeriesPickerTitle);
                dialog.setMultipleSelection(true);

                if (dialog.open() != Window.OK)
                    return;

                Object[] result = dialog.getResult();
                for (Object object : result)
                    selectedItems.add((Item) object);

                if (listener != null && result.length != 0)
                {
                    Item[] s = new Item[result.length];
                    System.arraycopy(result, 0, s, 0, result.length);
                    listener.onAddition(s);
                }
            }
        });
    }
}
