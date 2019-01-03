package name.abuchen.portfolio.ui.editor;

import java.text.MessageFormat;
import java.util.List;
import java.util.UUID;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.model.TaxonomyTemplate;
import name.abuchen.portfolio.model.Watchlist;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dnd.SecurityTransfer;
import name.abuchen.portfolio.ui.editor.Sidebar.Entry;
import name.abuchen.portfolio.ui.editor.Sidebar.EntryAction;
import name.abuchen.portfolio.ui.util.ConfirmAction;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SimpleAction;

/* package */class ClientEditorSidebar
{
    private PortfolioPart editor;

    private Menu taxonomyMenu;

    private ScrolledComposite scrolledComposite;
    private Sidebar sidebar;
    private Entry allSecurities;
    private Entry statementOfAssets;
    private Entry taxonomies;

    public ClientEditorSidebar(PortfolioPart editor)
    {
        this.editor = editor;
    }

    public Control createSidebarControl(Composite parent)
    {
        scrolledComposite = new ScrolledComposite(parent, SWT.V_SCROLL);

        sidebar = new Sidebar(scrolledComposite, SWT.NONE);

        createGeneralDataSection(sidebar);
        createMasterDataSection(sidebar);
        createPerformanceSection(sidebar);
        createTaxonomyDataSection(sidebar);
        createMiscSection(sidebar);

        scrolledComposite.setContent(sidebar);
        scrolledComposite.setExpandVertical(true);
        scrolledComposite.setExpandHorizontal(true);

        parent.getParent().addControlListener(new ControlAdapter()
        {
            @Override
            public void controlResized(ControlEvent e)
            {
                scrolledComposite.setMinSize(sidebar.computeSize(SWT.DEFAULT, SWT.DEFAULT));
            }
        });

        return scrolledComposite;
    }

    public void menuAboutToShow(IMenuManager menuManager)
    {
        // entries is a flat list of all entries

        MenuManager subMenu = null;
        for (Entry entry : sidebar.getEntries())
        {
            int indent = entry.getIndent();
            EntryAction action = entry.getAction();

            if (indent == 0)
            {
                subMenu = new MenuManager(entry.getLabel());
                menuManager.add(subMenu);
            }
            else
            {
                if (subMenu == null || action == null)
                    continue;

                String text = indent > Sidebar.STEP ? "- " + entry.getLabel() : entry.getLabel(); //$NON-NLS-1$
                SimpleAction menuAction = new SimpleAction(text, a -> sidebar.select(entry));
                if (entry.getImage() != null)
                    menuAction.setImageDescriptor(entry.getImage().descriptor());
                subMenu.add(menuAction);
            }
        }
    }

    public void selectDefaultView()
    {
        String defaultView = editor.getSelectedViewId();

        if (defaultView == null)
        {
            sidebar.select(statementOfAssets);
        }
        else
        {
            Entry entry = sidebar.selectById(defaultView);
            if (entry == null)
                sidebar.select(statementOfAssets);
        }
    }

    private void createGeneralDataSection(final Sidebar sidebar)
    {
        final Entry section = new Entry(sidebar, Messages.LabelSecurities);
        section.setAction(entry -> {
            String name = askWatchlistName(Messages.WatchlistNewLabel);
            if (name == null)
                return;

            Watchlist watchlist = new Watchlist();
            watchlist.setName(name);
            editor.getClient().getWatchlists().add(watchlist);
            editor.markDirty();

            createWatchlistEntry(section, watchlist);
            sidebar.layout();
            scrolledComposite.setMinSize(sidebar.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        }, Images.PLUS);

        allSecurities = new Entry(section, Messages.LabelAllSecurities,
                        e -> editor.activateView("SecurityList", e.getId()), Images.SECURITY); //$NON-NLS-1$

        for (Watchlist watchlist : editor.getClient().getWatchlists())
            createWatchlistEntry(section, watchlist);
    }

    private void createWatchlistEntry(Entry section, final Watchlist watchlist)
    {
        Entry entry = new Entry(section, watchlist.getName());
        entry.setAction(e -> editor.activateView("SecurityList", e.getId(), watchlist), Images.WATCHLIST); //$NON-NLS-1$

        entry.setContextMenu((e, m) -> watchlistContextMenuAboutToShow(watchlist, e, m));

        entry.addDropSupport(DND.DROP_MOVE, new Transfer[] { SecurityTransfer.getTransfer() }, new DropTargetAdapter()
        {
            @Override
            public void drop(DropTargetEvent event)
            {
                if (SecurityTransfer.getTransfer().isSupportedType(event.currentDataType))
                {
                    List<Security> securities = SecurityTransfer.getTransfer().getSecurities();
                    if (securities != null)
                    {
                        for (Security security : securities)
                        {
                            // if the security is dragged from another file, add
                            // a deep copy to the client's securities list
                            if (!editor.getClient().getSecurities().contains(security))
                            {
                                security = security.deepCopy();
                                editor.getClient().addSecurity(security);
                            }

                            if (!watchlist.getSecurities().contains(security))
                                watchlist.addSecurity(security);
                        }

                        editor.markDirty();
                    }
                }
            }
        });
    }

    private void watchlistContextMenuAboutToShow(Watchlist watchlist, Entry entry, IMenuManager manager)
    {
        manager.add(new Action(Messages.WatchlistRename)
        {
            @Override
            public void run()
            {
                String newName = askWatchlistName(watchlist.getName());
                if (newName != null)
                {
                    watchlist.setName(newName);
                    editor.markDirty();
                    entry.setLabel(newName);
                }
            }
        });

        manager.add(new Action(Messages.WatchlistDelete)
        {
            @Override
            public void run()
            {
                editor.getClient().getWatchlists().remove(watchlist);
                editor.markDirty();
                entry.dispose();
                allSecurities.select();
                scrolledComposite.setMinSize(sidebar.computeSize(SWT.DEFAULT, SWT.DEFAULT));
            }
        });
        manager.add(new Separator());

        addMoveUpAndDownActions(watchlist, entry, manager);
        manager.add(new Separator());
    }

    private void addMoveUpAndDownActions(Watchlist watchlist, Entry entry, IMenuManager manager)
    {
        List<Watchlist> list = editor.getClient().getWatchlists();
        int size = list.size();
        int index = list.indexOf(watchlist);

        if (index > 0)
        {
            manager.add(new Action(Messages.MenuMoveUp)
            {
                @Override
                public void run()
                {
                    Client client = editor.getClient();
                    List<Watchlist> watchlists = client.getWatchlists();
                    watchlists.remove(watchlist);
                    watchlists.add(index - 1, watchlist);
                    client.markDirty();

                    entry.moveUp();
                    sidebar.layout();
                }
            });
        }

        if (index < size - 1 && size > 1)
        {
            manager.add(new Action(Messages.MenuMoveDown)
            {
                @Override
                public void run()
                {
                    Client client = editor.getClient();
                    List<Watchlist> watchlists = client.getWatchlists();
                    watchlists.remove(watchlist);
                    watchlists.add(index + 1, watchlist);
                    client.markDirty();

                    entry.findNeighbor(SWT.ARROW_DOWN).moveUp();
                    sidebar.layout();
                }
            });
        }
    }

    private String askWatchlistName(String initialValue)
    {
        InputDialog dlg = new InputDialog(Display.getDefault().getActiveShell(), Messages.WatchlistEditDialog,
                        Messages.WatchlistEditDialogMsg, initialValue, null);
        if (dlg.open() != InputDialog.OK)
            return null;

        return dlg.getValue();
    }

    private void createMasterDataSection(Sidebar sidebar)
    {
        Entry section = new Entry(sidebar, Messages.ClientEditorLabelClientMasterData);
        new Entry(section, Messages.LabelAccounts, e -> editor.activateView("AccountList", e.getId()), //$NON-NLS-1$
                        Images.ACCOUNT);
        new Entry(section, Messages.LabelPortfolios, e -> editor.activateView("PortfolioList", e.getId()), //$NON-NLS-1$
                        Images.PORTFOLIO);
        new Entry(section, Messages.LabelInvestmentPlans, e -> editor.activateView("InvestmentPlanList", e.getId()), //$NON-NLS-1$
                        Images.INVESTMENTPLAN);
    }

    private void createPerformanceSection(Sidebar sidebar)
    {
        Entry section = new Entry(sidebar, Messages.ClientEditorLabelReports);

        statementOfAssets = new Entry(section, Messages.LabelStatementOfAssets,
                        e -> editor.activateView("StatementOfAssets", e.getId())); //$NON-NLS-1$

        new Entry(statementOfAssets, Messages.ClientEditorLabelChart,
                        e -> editor.activateView("StatementOfAssetsHistory", e.getId())); //$NON-NLS-1$
        new Entry(statementOfAssets, Messages.ClientEditorLabelHoldings,
                        e -> editor.activateView("HoldingsPieChart", e.getId())); //$NON-NLS-1$

        Entry performance = new Entry(section, Messages.ClientEditorLabelPerformance,
                        e -> editor.activateView("dashboard.Dashboard", e.getId())); //$NON-NLS-1$

        new Entry(performance, Messages.ClientEditorPerformanceCalculation,
                        e -> editor.activateView("Performance", e.getId())); //$NON-NLS-1$
        new Entry(performance, Messages.ClientEditorLabelChart,
                        e -> editor.activateView("PerformanceChart", e.getId())); //$NON-NLS-1$
        new Entry(performance, Messages.ClientEditorLabelReturnsVolatility,
                        e -> editor.activateView("ReturnsVolatilityChart", e.getId())); //$NON-NLS-1$
        new Entry(performance, Messages.LabelSecurities, e -> editor.activateView("SecuritiesPerformance", e.getId())); //$NON-NLS-1$
        new Entry(performance, Messages.LabelDividends, e -> editor.activateView("dividends.Dividends", e.getId())); //$NON-NLS-1$
    }

    private void createTaxonomyDataSection(final Sidebar sidebar)
    {
        taxonomies = new Entry(sidebar, Messages.LabelTaxonomies);
        taxonomies.setAction(entry -> showCreateTaxonomyMenu(), Images.PLUS);

        for (Taxonomy taxonomy : editor.getClient().getTaxonomies())
            createTaxonomyEntry(taxonomies, taxonomy);
    }

    private Entry createTaxonomyEntry(Entry section, final Taxonomy taxonomy)
    {
        Entry entry = new Entry(section, taxonomy.getName());
        entry.setAction(e -> editor.activateView("taxonomy.Taxonomy", e.getId(), taxonomy), null); //$NON-NLS-1$
        entry.setContextMenu((e, m) -> taxonomyContextMenuAboutToShow(taxonomy, e, m));
        return entry;
    }

    private void showCreateTaxonomyMenu()
    {
        if (taxonomyMenu == null)
        {
            MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
            menuMgr.setRemoveAllWhenShown(true);
            menuMgr.addMenuListener(this::taxonomyCreateMenuAboutToShow);
            taxonomyMenu = menuMgr.createContextMenu(sidebar.getShell());

            sidebar.addDisposeListener(e -> taxonomyMenu.dispose());
        }
        taxonomyMenu.setVisible(true);
    }

    private void taxonomyCreateMenuAboutToShow(IMenuManager manager)
    {
        manager.add(new Action(Messages.MenuTaxonomyCreate)
        {
            @Override
            public void run()
            {
                String name = askTaxonomyName(Messages.LabelNewTaxonomy);
                if (name == null)
                    return;

                Taxonomy taxonomy = new Taxonomy(name);
                taxonomy.setRootNode(new Classification(UUID.randomUUID().toString(), name));

                addAndOpenTaxonomy(taxonomy);
            }
        });

        manager.add(new Separator());
        manager.add(new LabelOnly(Messages.LabelTaxonomyTemplates));

        for (final TaxonomyTemplate template : TaxonomyTemplate.list())
        {
            manager.add(new Action(template.getName())
            {
                @Override
                public void run()
                {
                    addAndOpenTaxonomy(template.build());
                }
            });
        }
    }

    private void taxonomyContextMenuAboutToShow(Taxonomy taxonomy, Entry entry, IMenuManager manager)
    {
        manager.add(new Action(Messages.MenuTaxonomyRename)
        {
            @Override
            public void run()
            {
                String newName = askTaxonomyName(taxonomy.getName());
                if (newName != null)
                {
                    taxonomy.setName(newName);
                    editor.markDirty();
                    entry.setLabel(newName);
                }
            }
        });

        manager.add(new Action(Messages.MenuTaxonomyCopy)
        {
            @Override
            public void run()
            {
                String newName = askTaxonomyName(MessageFormat.format(Messages.LabelNamePlusCopy, taxonomy.getName()));
                if (newName != null)
                {
                    Taxonomy copy = taxonomy.copy();
                    copy.setName(newName);
                    addAndOpenTaxonomy(copy);
                }
            }
        });

        manager.add(new ConfirmAction(Messages.MenuTaxonomyDelete,
                        MessageFormat.format(Messages.MenuTaxonomyDeleteConfirm, taxonomy.getName()),
                        a -> deleteTaxonomyAndDisposeEntry(taxonomy, entry)));
        manager.add(new Separator());

        addMoveUpAndDownActions(taxonomy, entry, manager);
        manager.add(new Separator());
    }

    private void addMoveUpAndDownActions(Taxonomy taxonomy, Entry entry, IMenuManager manager)
    {
        List<Taxonomy> list = editor.getClient().getTaxonomies();
        int size = list.size();
        int index = list.indexOf(taxonomy);

        if (index > 0)
        {
            manager.add(new Action(Messages.MenuMoveUp)
            {
                @Override
                public void run()
                {
                    Client client = editor.getClient();
                    client.removeTaxonomy(taxonomy);
                    client.addTaxonomy(index - 1, taxonomy);
                    client.markDirty();

                    entry.moveUp();
                    sidebar.layout();
                }
            });
        }

        if (index < size - 1 && size > 1)
        {
            manager.add(new Action(Messages.MenuMoveDown)
            {
                @Override
                public void run()
                {
                    Client client = editor.getClient();
                    client.removeTaxonomy(taxonomy);
                    client.addTaxonomy(index + 1, taxonomy);
                    client.markDirty();

                    entry.findNeighbor(SWT.ARROW_DOWN).moveUp();
                    sidebar.layout();
                }
            });
        }
    }

    private void addAndOpenTaxonomy(Taxonomy taxonomy)
    {
        editor.getClient().addTaxonomy(taxonomy);
        editor.markDirty();
        Entry entry = createTaxonomyEntry(taxonomies, taxonomy);

        sidebar.select(entry);
        sidebar.layout();
        scrolledComposite.setMinSize(sidebar.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    }

    private String askTaxonomyName(String initialValue)
    {
        InputDialog dlg = new InputDialog(Display.getDefault().getActiveShell(), Messages.DialogTaxonomyNameTitle,
                        Messages.DialogTaxonomyNamePrompt, initialValue, null);
        if (dlg.open() != InputDialog.OK)
            return null;

        return dlg.getValue();
    }

    private void createMiscSection(Sidebar sidebar)
    {
        Entry section = new Entry(sidebar, Messages.ClientEditorLabelGeneralData);
        new Entry(section, Messages.LabelConsumerPriceIndex,
                        e -> editor.activateView("ConsumerPriceIndexList", e.getId())); //$NON-NLS-1$
        new Entry(section, Messages.LabelCurrencies, e -> editor.activateView("currency.Currency", e.getId())); //$NON-NLS-1$
        new Entry(section, Messages.LabelSettings, e -> editor.activateView("settings.Settings", e.getId())); //$NON-NLS-1$

        if ("yes".equals(System.getProperty("name.abuchen.portfolio.debug"))) //$NON-NLS-1$ //$NON-NLS-2$
            new Entry(section, "Browser Test", e -> editor.activateView("BrowserTest", e.getId())); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private void deleteTaxonomyAndDisposeEntry(Taxonomy taxonomy, Entry entry)
    {
        editor.getClient().removeTaxonomy(taxonomy);
        editor.markDirty();
        entry.dispose();
        statementOfAssets.select();
        scrolledComposite.setMinSize(sidebar.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    }
}
