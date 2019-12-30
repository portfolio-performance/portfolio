package name.abuchen.portfolio.ui.editor;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.model.TaxonomyTemplate;
import name.abuchen.portfolio.model.Watchlist;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.ConfirmAction;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.views.AccountListView;
import name.abuchen.portfolio.ui.views.AllTransactionsView;
import name.abuchen.portfolio.ui.views.BrowserTestView;
import name.abuchen.portfolio.ui.views.HoldingsPieChartView;
import name.abuchen.portfolio.ui.views.InvestmentPlanListView;
import name.abuchen.portfolio.ui.views.PerformanceChartView;
import name.abuchen.portfolio.ui.views.PerformanceView;
import name.abuchen.portfolio.ui.views.PortfolioListView;
import name.abuchen.portfolio.ui.views.ReturnsVolatilityChartView;
import name.abuchen.portfolio.ui.views.SecuritiesPerformanceView;
import name.abuchen.portfolio.ui.views.SecurityListView;
import name.abuchen.portfolio.ui.views.StatementOfAssetsHistoryView;
import name.abuchen.portfolio.ui.views.StatementOfAssetsView;
import name.abuchen.portfolio.ui.views.currency.CurrencyView;
import name.abuchen.portfolio.ui.views.dashboard.DashboardView;
import name.abuchen.portfolio.ui.views.earnings.EarningsView;
import name.abuchen.portfolio.ui.views.settings.SettingsView;
import name.abuchen.portfolio.ui.views.taxonomy.TaxonomyView;
import name.abuchen.portfolio.ui.views.trades.TradeDetailsView;

public final class Navigation
{
    public enum Tag
    {
        /** item is hidden in the sidebar navigation */
        HIDE,
        /** item has a view class attached */
        VIEW,
        /** the initial view to be opened */
        DEFAULT_VIEW;
    }

    public static class Item
    {
        private String label;
        private Images image;

        private IMenuListener actionMenu;
        private IMenuListener contextMenu;

        private Class<? extends AbstractFinanceView> viewClass;
        private Object parameter;

        private List<Item> children = new ArrayList<>();
        private EnumSet<Tag> tags = EnumSet.noneOf(Tag.class);

        private Item(String label)
        {
            this(label, null, null);
        }

        private Item(String label, Class<? extends AbstractFinanceView> viewClass)
        {
            this(label, null, viewClass);
        }

        private Item(String label, Images image, Class<? extends AbstractFinanceView> viewClass)
        {
            this.label = label;
            this.image = image;
            this.viewClass = viewClass;

            if (viewClass != null)
                addTag(Tag.VIEW);
        }

        public String getLabel()
        {
            return label;
        }

        public Images getImage()
        {
            return image;
        }

        public IMenuListener getActionMenu()
        {
            return actionMenu;
        }

        public IMenuListener getContextMenu()
        {
            return contextMenu;
        }

        public Object getParameter()
        {
            return parameter;
        }

        /* package */ void setParameter(Object parameter)
        {
            this.parameter = parameter;
        }

        public Class<? extends AbstractFinanceView> getViewClass()
        {
            return viewClass;
        }

        /* package */ void add(Item child)
        {
            this.children.add(child);
        }

        public Stream<Item> getChildren()
        {
            return this.children.stream();
        }

        public void addTag(Tag tag)
        {
            this.tags.add(tag);
        }

        public boolean contains(Tag tag)
        {
            return this.tags.contains(tag);
        }
    }

    @FunctionalInterface
    public interface Listener
    {
        void changed(Item item);
    }

    private List<Item> roots = new ArrayList<>();
    private List<Listener> listeners = new ArrayList<>();

    /* protected */ Navigation(Client client)
    {
        createGeneralDataSection(client);
        createMasterDataSection();
        createPerformanceSection();
        createTaxonomyDataSection(client);
        createMiscSection();
    }

    public Stream<Item> getRoots()
    {
        return roots.stream();
    }

    public Stream<Item> findAll(Tag tag)
    {
        return findAll(item -> item.contains(tag));
    }

    public Stream<Item> findAll(Predicate<Item> predicate)
    {
        List<Item> result = new ArrayList<>();
        LinkedList<Item> stack = new LinkedList<>(roots);

        while (!stack.isEmpty())
        {
            Item item = stack.removeFirst();

            if (predicate.test(item))
                result.add(item);

            stack.addAll(item.children);
        }

        return result.stream();
    }

    /**
     * Returns the identifier of the navigation item to be stored in
     * preferences. Historically, it was a concatenation of the labels,
     * therefore the identifier is not necessarily unique or stable.
     */
    public String getIdentifier(Item item)
    {
        StringBuilder buffer = new StringBuilder();
        for (Item i : findPathTo(item))
            buffer.append(i.getLabel());

        return buffer.toString();
    }

    private List<Item> findPathTo(Item item)
    {
        if (roots.contains(item))
            return Arrays.asList(item);

        Map<Item, Item> child2parent = new HashMap<>();

        LinkedList<Item> stack = new LinkedList<>(roots);
        while (!stack.isEmpty())
        {
            Item parent = stack.removeFirst();

            for (Item child : parent.children)
            {
                child2parent.put(child, parent);

                if (item.equals(child))
                {
                    stack.clear();
                    break;
                }

                stack.add(child);
            }
        }

        List<Item> path = new ArrayList<>();
        path.add(item);

        Item parent = child2parent.get(item);
        while (parent != null)
        {
            path.add(parent);
            parent = child2parent.get(parent);
        }

        Collections.reverse(path);

        return path;
    }

    public Optional<Item> findByIdentifier(String id)
    {
        return id == null ? Optional.empty() : findByIdentifier(id, "", roots); //$NON-NLS-1$
    }

    private Optional<Item> findByIdentifier(String id, String prefix, List<Item> items)
    {
        for (Item item : items)
        {
            String concat = prefix + item.label;

            if (id.equals(concat))
            {
                return Optional.of(item);
            }
            else if (id.startsWith(concat))
            {
                Optional<Item> child = findByIdentifier(id, concat, item.children);
                if (child.isPresent())
                    return child;
            }
        }

        return Optional.empty();
    }

    public void addListener(Listener listener)
    {
        this.listeners.add(listener);
    }

    public void removeListener(Listener listener)
    {
        this.listeners.remove(listener);
    }

    private void createGeneralDataSection(Client client)
    {
        Item generalData = new Item(Messages.LabelSecurities);
        generalData.actionMenu = manager -> { // NOSONAR
            manager.add(new SimpleAction(Messages.WatchlistNewLabel, Images.PLUS.descriptor(), a -> {
                String name = askWatchlistName(Messages.WatchlistNewLabel);
                if (name == null)
                    return;

                Watchlist watchlist = new Watchlist();
                watchlist.setName(name);
                client.getWatchlists().add(watchlist);

                Item item = createWatchlistItem(generalData, client, watchlist);
                generalData.add(item);

                this.listeners.forEach(l -> l.changed(item));

                client.touch();
            }));
        };

        roots.add(generalData);

        generalData.add(new Item(Messages.LabelAllSecurities, Images.SECURITY, SecurityListView.class));

        for (Watchlist watchlist : client.getWatchlists())
        {
            generalData.add(createWatchlistItem(generalData, client, watchlist));
        }
    }

    private Item createWatchlistItem(Item section, Client client, Watchlist watchlist)
    {
        Item item = new Item(watchlist.getName(), Images.WATCHLIST, SecurityListView.class);
        item.parameter = watchlist;

        item.contextMenu = manager -> {
            manager.add(new SimpleAction(Messages.WatchlistRename, a -> {
                String newName = askWatchlistName(watchlist.getName());
                if (newName != null)
                {
                    watchlist.setName(newName);
                    item.label = newName;

                    this.listeners.forEach(l -> l.changed(item));
                    client.touch();
                }
            }));

            manager.add(new SimpleAction(Messages.WatchlistDelete, a -> {
                client.getWatchlists().remove(watchlist);
                section.children.remove(item);

                this.listeners.forEach(l -> l.changed(item));
                client.touch();
            }));

            manager.add(new Separator());

            List<Watchlist> list = client.getWatchlists();
            int size = list.size();
            int index = list.indexOf(watchlist);

            // section has one more element: all securities. Therefore the index
            // into the children needs an offset
            int offset = (int) section.getChildren().filter(i -> !(i.getParameter() instanceof Watchlist)).count();

            if (index > 0)
            {
                manager.add(new SimpleAction(Messages.MenuMoveUp, a -> {
                    List<Watchlist> watchlists = client.getWatchlists();
                    watchlists.remove(watchlist);
                    watchlists.add(index - 1, watchlist);
                    section.children.remove(item);
                    section.children.add(index - 1 + offset, item);

                    this.listeners.forEach(l -> l.changed(item));
                    client.touch();
                }));
            }

            if (index < size - 1 && size > 1)
            {
                manager.add(new SimpleAction(Messages.MenuMoveDown, a -> {
                    List<Watchlist> watchlists = client.getWatchlists();
                    watchlists.remove(watchlist);
                    watchlists.add(index + 1, watchlist);
                    section.children.remove(item);
                    section.children.add(index + 1 + offset, item);

                    this.listeners.forEach(l -> l.changed(item));
                    client.touch();
                }));
            }
        };

        return item;
    }

    private String askWatchlistName(String initialValue)
    {
        InputDialog dlg = new InputDialog(Display.getDefault().getActiveShell(), Messages.WatchlistEditDialog,
                        Messages.WatchlistEditDialogMsg, initialValue, null);
        if (dlg.open() != InputDialog.OK)
            return null;

        return dlg.getValue();
    }

    private void createMasterDataSection()
    {
        Item masterData = new Item(Messages.ClientEditorLabelClientMasterData);

        roots.add(masterData);

        masterData.add(new Item(Messages.LabelAccounts, Images.ACCOUNT, AccountListView.class));
        masterData.add(new Item(Messages.LabelPortfolios, Images.PORTFOLIO, PortfolioListView.class));
        masterData.add(new Item(Messages.LabelInvestmentPlans, Images.INVESTMENTPLAN, InvestmentPlanListView.class));

        Item allTransactions = new Item(Messages.LabelAllTransactions, AllTransactionsView.class);
        allTransactions.addTag(Tag.HIDE);
        masterData.add(allTransactions);
    }

    private void createPerformanceSection()
    {
        Item section = new Item(Messages.ClientEditorLabelReports);
        roots.add(section);

        Item statementOfAssets = new Item(Messages.LabelStatementOfAssets, StatementOfAssetsView.class);
        statementOfAssets.addTag(Tag.DEFAULT_VIEW);
        section.add(statementOfAssets);

        statementOfAssets.add(new Item(Messages.ClientEditorLabelChart, StatementOfAssetsHistoryView.class));
        statementOfAssets.add(new Item(Messages.ClientEditorLabelHoldings, HoldingsPieChartView.class));

        Item performance = new Item(Messages.ClientEditorLabelPerformance, DashboardView.class);
        section.add(performance);

        performance.add(new Item(Messages.ClientEditorPerformanceCalculation, PerformanceView.class));
        performance.add(new Item(Messages.ClientEditorLabelChart, PerformanceChartView.class));
        performance.add(new Item(Messages.ClientEditorLabelReturnsVolatility, ReturnsVolatilityChartView.class));
        performance.add(new Item(Messages.LabelSecurities, SecuritiesPerformanceView.class));
        performance.add(new Item(Messages.LabelEarningsExpenses, EarningsView.class));

        Item allTrades = new Item(Messages.LabelTrades, TradeDetailsView.class);
        allTrades.addTag(Tag.HIDE);
        performance.add(allTrades);
    }

    private void createTaxonomyDataSection(Client client)
    {
        Item section = new Item(Messages.LabelTaxonomies);
        roots.add(section);

        section.actionMenu = manager -> {
            manager.add(new SimpleAction(Messages.LabelNewTaxonomy, Images.PLUS.descriptor(), a -> {
                String name = askTaxonomyName(Messages.LabelNewTaxonomy);
                if (name == null)
                    return;

                Taxonomy taxonomy = new Taxonomy(name);
                taxonomy.setRootNode(new Classification(UUID.randomUUID().toString(), name));
                client.addTaxonomy(taxonomy);

                Item item = createTaxonomyItem(section, client, taxonomy);
                section.add(item);

                this.listeners.forEach(l -> l.changed(item));
                client.touch();
            }));

            manager.add(new Separator());
            manager.add(new LabelOnly(Messages.LabelTaxonomyTemplates));

            for (final TaxonomyTemplate template : TaxonomyTemplate.list())
            {
                manager.add(new SimpleAction(template.getName(), a -> {
                    Taxonomy taxonomy = template.build();
                    client.addTaxonomy(taxonomy);

                    Item item = createTaxonomyItem(section, client, taxonomy);
                    section.add(item);

                    this.listeners.forEach(l -> l.changed(item));
                    client.touch();
                }));
            }
        };

        for (Taxonomy taxonomy : client.getTaxonomies())
        {
            section.add(createTaxonomyItem(section, client, taxonomy));
        }
    }

    private Item createTaxonomyItem(Item section, Client client, Taxonomy taxonomy)
    {
        Item item = new Item(taxonomy.getName(), TaxonomyView.class);
        item.parameter = taxonomy;

        item.contextMenu = manager -> {
            manager.add(new SimpleAction(Messages.MenuTaxonomyRename, a -> {
                String newName = askTaxonomyName(taxonomy.getName());
                if (newName != null)
                {
                    taxonomy.setName(newName);
                    item.label = newName;

                    this.listeners.forEach(l -> l.changed(item));
                    client.touch();
                }
            }));

            manager.add(new SimpleAction(Messages.MenuTaxonomyCopy, a -> {
                String newName = askTaxonomyName(MessageFormat.format(Messages.LabelNamePlusCopy, taxonomy.getName()));
                if (newName != null)
                {
                    Taxonomy copy = taxonomy.copy();
                    copy.setName(newName);
                    client.addTaxonomy(copy);
                    section.add(createTaxonomyItem(section, client, copy));

                    this.listeners.forEach(l -> l.changed(item));
                    client.touch();
                }
            }));

            manager.add(new ConfirmAction(Messages.MenuTaxonomyDelete,
                            MessageFormat.format(Messages.MenuTaxonomyDeleteConfirm, taxonomy.getName()), a -> {
                                client.removeTaxonomy(taxonomy);
                                section.children.remove(item);

                                this.listeners.forEach(l -> l.changed(item));
                                client.touch();
                            }));

            manager.add(new Separator());

            List<Taxonomy> list = client.getTaxonomies();
            int size = list.size();
            int index = list.indexOf(taxonomy);

            if (index > 0)
            {
                manager.add(new SimpleAction(Messages.MenuMoveUp, a -> {
                    client.removeTaxonomy(taxonomy);
                    client.addTaxonomy(index - 1, taxonomy);
                    section.children.remove(item);
                    section.children.add(index - 1, item);

                    this.listeners.forEach(l -> l.changed(item));
                    client.touch();
                }));
            }

            if (index < size - 1 && size > 1)
            {
                manager.add(new SimpleAction(Messages.MenuMoveDown, a -> {
                    client.removeTaxonomy(taxonomy);
                    client.addTaxonomy(index + 1, taxonomy);
                    section.children.remove(item);
                    section.children.add(index + 1, item);

                    this.listeners.forEach(l -> l.changed(item));
                    client.touch();
                }));
            }
        };

        return item;
    }

    private String askTaxonomyName(String initialValue)
    {
        InputDialog dlg = new InputDialog(Display.getDefault().getActiveShell(), Messages.DialogTaxonomyNameTitle,
                        Messages.DialogTaxonomyNamePrompt, initialValue, null);
        if (dlg.open() != InputDialog.OK)
            return null;

        return dlg.getValue();
    }

    private void createMiscSection()
    {
        Item section = new Item(Messages.ClientEditorLabelGeneralData);
        roots.add(section);

        section.add(new Item(Messages.LabelCurrencies, CurrencyView.class));
        section.add(new Item(Messages.LabelSettings, SettingsView.class));

        if ("yes".equals(System.getProperty("name.abuchen.portfolio.debug"))) //$NON-NLS-1$ //$NON-NLS-2$
            section.add(new Item("Browser Test", BrowserTestView.class)); //$NON-NLS-1$
    }
}
