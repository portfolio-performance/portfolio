package name.abuchen.portfolio.ui.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.ToolBar;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.snapshot.filter.ClientFilter;
import name.abuchen.portfolio.snapshot.filter.PortfolioClientFilter;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.ListSelectionDialog;

public final class ClientFilterDropDown extends AbstractDropDown
{
    private static class Item
    {
        private String label;
        private String uuids;
        private ClientFilter filter;

        public Item(String label, String uuids, ClientFilter filter)
        {
            this.label = label;
            this.uuids = uuids;
            this.filter = filter;
        }
    }

    private static final int MAXIMUM_NO_CUSTOM_ITEMS = 5;

    private final Client client;
    private final IPreferenceStore preferences;
    private final Consumer<ClientFilter> listener;

    private List<ClientFilterDropDown.Item> defaultItems = new ArrayList<>();
    private LinkedList<ClientFilterDropDown.Item> customItems = new LinkedList<>();
    private ClientFilterDropDown.Item selectedItem;

    public ClientFilterDropDown(ToolBar toolBar, Client client, IPreferenceStore preferences,
                    Consumer<ClientFilter> listener)
    {
        super(toolBar, Messages.MenuChooseClientFilter, Images.FILTER_OFF.image(), SWT.NONE);

        this.client = client;
        this.preferences = preferences;
        this.listener = listener;

        selectedItem = new Item(Messages.PerformanceChartLabelEntirePortfolio, null, c -> c);
        defaultItems.add(selectedItem);

        client.getPortfolios().forEach(portfolio -> {
            defaultItems.add(new Item(portfolio.getName(), null, new PortfolioClientFilter(portfolio)));
            defaultItems.add(new Item(portfolio.getName() + " + " + portfolio.getReferenceAccount().getName(), //$NON-NLS-1$
                            null, new PortfolioClientFilter(portfolio, portfolio.getReferenceAccount())));
        });

        loadCustomItems();
    }

    private void loadCustomItems()
    {
        String code = preferences.getString(ClientFilterDropDown.class.getSimpleName());
        if (code == null || code.isEmpty())
            return;

        Map<String, Object> uuid2object = new HashMap<>();
        client.getPortfolios().forEach(p -> uuid2object.put(p.getUUID(), p));
        client.getAccounts().forEach(a -> uuid2object.put(a.getUUID(), a));

        String[] items = code.split(";"); //$NON-NLS-1$
        for (String item : items)
        {
            String[] uuids = item.split(","); //$NON-NLS-1$
            Object[] objects = Arrays.stream(uuids).map(uuid2object::get).filter(o -> o != null).toArray();

            if (objects.length > 0)
            {
                Item newItem = buildItem(objects);
                customItems.add(newItem);
            }
        }
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        defaultItems.forEach(item -> {
            Action action = new SimpleAction(item.label, a -> {
                selectedItem = item;
                getToolItem().setImage(defaultItems.indexOf(selectedItem) == 0 ? Images.FILTER_OFF.image()
                                : Images.FILTER_ON.image());

                listener.accept(item.filter);
            });
            action.setChecked(item.equals(selectedItem));
            manager.add(action);
        });

        manager.add(new Separator());
        customItems.forEach(item -> {
            Action action = new SimpleAction(item.label, a -> {
                selectedItem = item;
                getToolItem().setImage(Images.FILTER_ON.image());

                customItems.remove(item);
                customItems.addFirst(item);

                listener.accept(item.filter);
            });
            action.setChecked(item.equals(selectedItem));
            manager.add(action);
        });

        manager.add(new Separator());
        manager.add(new SimpleAction(Messages.LabelClientFilterNew, a -> createCustomFilter()));
        manager.add(new SimpleAction(Messages.LabelClientClearCustomItems, a -> {
            if (customItems.contains(selectedItem))
            {
                selectedItem = defaultItems.get(0);
                getToolItem().setImage(Images.FILTER_OFF.image());
                listener.accept(selectedItem.filter);
            }

            customItems.clear();
            preferences.setToDefault(ClientFilterDropDown.class.getSimpleName());
        }));
    }

    private void createCustomFilter()
    {
        ListSelectionDialog dialog = new ListSelectionDialog(getToolBar().getShell(), new LabelProvider()
        {
            @Override
            public Image getImage(Object element)
            {
                return element instanceof Account ? Images.ACCOUNT.image() : Images.PORTFOLIO.image();
            }
        });

        dialog.setTitle(Messages.LabelClientFilterDialogTitle);
        dialog.setMessage(Messages.LabelClientFilterDialogMessage);

        List<Object> elements = new ArrayList<>();
        elements.addAll(client.getPortfolios());
        elements.addAll(client.getAccounts());
        dialog.setElements(elements);

        if (dialog.open() == ListSelectionDialog.OK)
        {
            Object[] selected = dialog.getResult();
            if (selected.length > 0)
            {
                Item newItem = buildItem(selected);

                selectedItem = newItem;
                customItems.addFirst(newItem);

                if (customItems.size() > MAXIMUM_NO_CUSTOM_ITEMS)
                    customItems.removeLast();

                preferences.putValue(ClientFilterDropDown.class.getSimpleName(),
                                String.join(";", customItems.stream().map(i -> i.uuids).collect(Collectors.toList()))); //$NON-NLS-1$

                getToolItem().setImage(Images.FILTER_ON.image());
                listener.accept(newItem.filter);
            }
        }
    }

    private Item buildItem(Object[] selected)
    {
        List<Portfolio> portfolios = Arrays.stream(selected).filter(o -> o instanceof Portfolio).map(o -> (Portfolio) o)
                        .collect(Collectors.toList());
        List<Account> accounts = Arrays.stream(selected).filter(o -> o instanceof Account).map(o -> (Account) o)
                        .collect(Collectors.toList());

        String label = String.join(", ", //$NON-NLS-1$
                        Arrays.stream(selected).map(String::valueOf).collect(Collectors.toList()));

        String uuids = String.join(",", //$NON-NLS-1$
                        Arrays.stream(selected).map(
                                        o -> o instanceof Account ? ((Account) o).getUUID() : ((Portfolio) o).getUUID())
                                        .collect(Collectors.toList()));

        return new Item(label, uuids, new PortfolioClientFilter(portfolios, accounts));
    }

    public ClientFilter getSelectedFilte()
    {
        return selectedItem.filter;
    }
}
