package name.abuchen.portfolio.ui.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ConfigurationSet;
import name.abuchen.portfolio.model.ConfigurationSet.Configuration;
import name.abuchen.portfolio.model.ConfigurationSet.WellKnownConfigurationSets;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.snapshot.filter.ClientFilter;
import name.abuchen.portfolio.snapshot.filter.PortfolioClientFilter;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.EditClientFilterDialog;
import name.abuchen.portfolio.ui.dialogs.ListSelectionDialog;

public final class ClientFilterMenu implements IMenuListener
{
    public static class Item
    {
        /** unique and stable identifier for a filter */
        private final String id;

        /** user-provided label or automatically constructed label */
        private String label;

        /** comma separated list of account or portfolio uuids */
        private String uuids;

        // transient to avoid Gson serialization
        private transient ClientFilter filter; // NOSONAR

        public Item(String id, String label, String uuids, ClientFilter filter)
        {
            this.id = id;
            this.label = label;
            this.uuids = Objects.requireNonNull(uuids);
            this.filter = filter;
        }

        public String getId()
        {
            return id;
        }

        public String getLabel()
        {
            return label;
        }

        public void setLabel(String label)
        {
            this.label = label;
        }

        public String getUUIDs()
        {
            return uuids;
        }

        public void setUUIDs(String uuids)
        {
            this.uuids = uuids;
        }

        public ClientFilter getFilter()
        {
            return filter;
        }

        @Override
        public String toString()
        {
            return label;
        }
    }

    private static final int MAXIMUM_NO_CUSTOM_ITEMS = 30;

    private final Client client;
    private final IPreferenceStore preferences;
    private final List<Consumer<ClientFilter>> listeners = new ArrayList<>();

    private List<Item> defaultItems = new ArrayList<>();
    private LinkedList<Item> customItems = new LinkedList<>();
    private Item selectedItem;
    private ConfigurationSet filterConfig;

    public ClientFilterMenu(Client client, IPreferenceStore preferences)
    {
        this.client = client;
        this.preferences = preferences;
        this.filterConfig = client.getSettings()
                        .getConfigurationSet(WellKnownConfigurationSets.CLIENT_FILTER_DEFINITIONS);

        selectedItem = new Item("", Messages.PerformanceChartLabelEntirePortfolio, "", //$NON-NLS-1$ //$NON-NLS-2$
                        ClientFilter.NO_FILTER);
        defaultItems.add(selectedItem);

        client.getActivePortfolios().forEach(portfolio -> {
            defaultItems.add(new Item(portfolio.getUUID(), portfolio.getName(), portfolio.getUUID(),
                            new PortfolioClientFilter(portfolio)));
            defaultItems.add(new Item(portfolio.getUUID() + "," + portfolio.getReferenceAccount().getUUID(), //$NON-NLS-1$
                            portfolio.getName() + " + " + portfolio.getReferenceAccount().getName(), //$NON-NLS-1$
                            portfolio.getUUID() + "," + portfolio.getReferenceAccount().getUUID(), //$NON-NLS-1$
                            new PortfolioClientFilter(portfolio, portfolio.getReferenceAccount())));
        });

        loadCustomItems();
    }

    public ClientFilterMenu(Client client, IPreferenceStore preferences, Consumer<ClientFilter> listener)
    {
        this(client, preferences);
        this.listeners.add(listener);
    }

    private void loadCustomItems()
    {
        filterConfig.getConfigurations().forEach(conf -> {
            buildItem(conf.getUUID(), conf.getName(), conf.getData(), client).ifPresent(i -> customItems.add(i));
        });
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        defaultItems.forEach(item -> {
            Action action = new SimpleAction(item.label, a -> {
                selectedItem = item;
                listeners.forEach(l -> l.accept(item.filter));
            });
            action.setChecked(item.equals(selectedItem));
            manager.add(action);
        });

        manager.add(new Separator());
        customItems.forEach(item -> {
            Action action = new SimpleAction(item.label, a -> {
                selectedItem = item;
                listeners.forEach(l -> l.accept(item.filter));
            });
            action.setChecked(item.equals(selectedItem));
            manager.add(action);
        });

        manager.add(new Separator());
        manager.add(new SimpleAction(Messages.LabelClientFilterNew, a -> createCustomFilter()));
        manager.add(new SimpleAction(Messages.LabelClientFilterManage, a -> editCustomFilter()));

        manager.add(new SimpleAction(Messages.LabelClientClearCustomItems, a -> {
            if (!MessageDialog.openConfirm(Display.getDefault().getActiveShell(), Messages.LabelClientClearCustomItems,
                            Messages.LabelClientClearCustomItems + "?")) //$NON-NLS-1$
                return;

            if (customItems.contains(selectedItem))
            {
                selectedItem = defaultItems.get(0);
                listeners.forEach(l -> l.accept(selectedItem.filter));
            }

            customItems.clear();
            filterConfig.clear();
            client.touch();
        }));
    }

    private void editCustomFilter()
    {
        if (customItems.isEmpty())
        {
            MessageDialog.openInformation(Display.getDefault().getActiveShell(), Messages.LabelInfo,
                            Messages.LabelClientFilterNoCustomFilterExisting);
            return;
        }

        final boolean isCustomItemSelected = customItems.contains(selectedItem);

        EditClientFilterDialog dialog = new EditClientFilterDialog(Display.getDefault().getActiveShell(), client,
                        preferences);
        dialog.setItems(customItems);
        dialog.open();

        // store changed filters. keep in mind: filters could be deleted or
        // filter data changed. So first delete all stored filters and store
        // current ones
        filterConfig.clear();
        customItems.forEach(cf -> filterConfig.add(new Configuration(cf.getId(), cf.getLabel(), cf.getUUIDs())));
        client.touch();

        if (isCustomItemSelected)
        {
            // previously selected custom filter was deleted in dialog -->
            // select default filter item
            if (!customItems.contains(selectedItem))
                selectedItem = defaultItems.get(0);

            // always update listeners (when custom filter selected) because
            // filter may have been changed in edit filter dialog
            // or default filter was set because selected filter was deleted
            listeners.forEach(l -> l.accept(selectedItem.filter));
        }
    }

    private void createCustomFilter()
    {
        LabelProvider labelProvider = new LabelProvider()
        {
            @Override
            public Image getImage(Object element)
            {
                return LogoManager.instance().getDefaultColumnImage(element, client.getSettings());
            }
        };
        ListSelectionDialog dialog = new ListSelectionDialog(Display.getDefault().getActiveShell(), labelProvider);

        dialog.setTitle(Messages.LabelClientFilterDialogTitle);
        dialog.setMessage(Messages.LabelClientFilterDialogMessage);
        dialog.setPropertyLabel(Messages.ColumnName);

        List<Object> elements = new ArrayList<>();
        elements.addAll(client.getPortfolios());
        elements.addAll(client.getAccounts());
        dialog.setElements(elements);

        if (dialog.open() == Window.OK)
        {
            Object[] selected = dialog.getResult();
            if (selected.length > 0)
            {
                Item newItem = buildItem(UUID.randomUUID().toString(), dialog.getProperty(), selected);

                String label = dialog.getProperty();
                if (!label.isEmpty())
                    newItem.label = label;

                selectedItem = newItem;
                customItems.addFirst(newItem);

                if (customItems.size() > MAXIMUM_NO_CUSTOM_ITEMS)
                    customItems.removeLast();

                filterConfig.add(new Configuration(newItem.getId(), newItem.getLabel(), newItem.getUUIDs()));
                client.touch();

                listeners.forEach(l -> l.accept(newItem.filter));
            }
        }
    }

    public static Optional<Item> buildItem(String id, String name, String uuids, Client client)
    {
        if (uuids == null || uuids.isEmpty())
            return Optional.empty();

        String[] ids = uuids.split(","); //$NON-NLS-1$
        if (ids.length == 0)
            return Optional.empty();

        Map<String, Object> uuid2object = new HashMap<>();
        client.getPortfolios().forEach(p -> uuid2object.put(p.getUUID(), p));
        client.getAccounts().forEach(a -> uuid2object.put(a.getUUID(), a));

        return Optional.of(buildItem(id, name,
                        Arrays.stream(ids).map(uuid2object::get).filter(Objects::nonNull).toArray()));
    }

    private static Item buildItem(String ident, String label, Object[] selected)
    {
        List<Portfolio> portfolios = Arrays.stream(selected).filter(Portfolio.class::isInstance).map(o -> (Portfolio) o)
                        .toList();
        List<Account> accounts = Arrays.stream(selected).filter(Account.class::isInstance).map(o -> (Account) o)
                        .toList();

        if (label.isEmpty())
            label = Arrays.stream(selected).map(String::valueOf).collect(Collectors.joining(", ")); //$NON-NLS-1$

        String uuids = buildUUIDs(selected);

        return new Item(ident, label, uuids, new PortfolioClientFilter(portfolios, accounts));
    }

    public static String buildUUIDs(Object[] selected)
    {
        return Arrays.stream(selected)
                        .map(o -> o instanceof Account account ? account.getUUID() : ((Portfolio) o).getUUID())
                        .collect(Collectors.joining(",")); //$NON-NLS-1$
    }

    public boolean hasActiveFilter()
    {
        return defaultItems.indexOf(selectedItem) != 0;
    }

    public ClientFilter getSelectedFilter()
    {
        return selectedItem.filter;
    }

    public Item getSelectedItem()
    {
        return selectedItem;
    }

    public void addListener(Consumer<ClientFilter> listener)
    {
        listeners.add(listener);
    }

    public Stream<Item> getAllItems()
    {
        return Stream.concat(defaultItems.stream(), customItems.stream());
    }

    public List<Item> getCustomItems()
    {
        return Collections.unmodifiableList(customItems);
    }

    public void select(Item item)
    {
        selectedItem = item;
    }

    public void trackSelectedFilterConfigurationKey(String key)
    {
        selectClientFilter(key);

        this.addListener(filter -> saveSelectedFilter(client, key, this.getSelectedItem().getId()));
    }

    public void selectClientFilter(String configurationKey)
    {
        getSelectedFilterId(client, configurationKey).ifPresent(clientFilterId -> this.getAllItems()
                        .filter(item -> item.getId().equals(clientFilterId)).findAny().ifPresent(this::select));
    }

    public Optional<Item> selectItemFromClientFilterId(String clientFilterId)
    {
        Optional<Item> optionalItem = this.getAllItems().filter(item -> item.getId().equals(clientFilterId)).findAny();
        optionalItem.ifPresent(this::select);
        return optionalItem;
    }

    public static void saveSelectedFilter(Client client, String key, String clientFilterId)
    {
        var set = client.getSettings().getConfigurationSet(WellKnownConfigurationSets.CLIENT_FILTER_SELECTION);

        set.lookup(key).ifPresentOrElse(config -> config.setData(clientFilterId),
                        () -> set.add(new Configuration(key, "", clientFilterId))); //$NON-NLS-1$

        client.touch();
    }

    public static Optional<String> getSelectedFilterId(Client client, String key)
    {
        return client.getSettings().getConfigurationSet(WellKnownConfigurationSets.CLIENT_FILTER_SELECTION)
                        .lookup(key).map(Configuration::getData);
    }
}
