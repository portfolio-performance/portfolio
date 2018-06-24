package name.abuchen.portfolio.ui;

import java.util.List;
import java.util.UUID;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.resource.ImageDescriptor;
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

import com.ibm.icu.text.MessageFormat;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.model.TaxonomyTemplate;
import name.abuchen.portfolio.model.Watchlist;
import name.abuchen.portfolio.ui.Sidebar.Entry;
import name.abuchen.portfolio.ui.dnd.SecurityTransfer;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SimpleAction;

/* package */class ClientEditorSidebar
{
    private final class ActivateViewAction extends Action
    {
        private String view;
        private Object parameter;

        private ActivateViewAction(String label, String view)
        {
            this(label, view, null, null);
        }

        private ActivateViewAction(String text, String view, ImageDescriptor image)
        {
            this(text, view, null, image);
        }

        public ActivateViewAction(String text, String view, Object parameter, ImageDescriptor image)
        {
            super(text, image);
            this.view = view;
            this.parameter = parameter;
        }

        @Override
        public void run()
        {
            editor.activateView(view, parameter);
        }
    }

    private static final String START_PAGE_KEY = "pp-start-page"; //$NON-NLS-1$

    private PortfolioPart editor;

    private Menu taxonomyMenu;

    private ScrolledComposite scrolledComposite;
    private Sidebar sidebar;
    private Entry allSecurities;
    private Entry statementOfAssets;
    private Entry taxonomies;

    private Sidebar.MenuListener setAsStartPage = (entry, manager) -> manager
                    .add(new Action(Messages.MenuLabelSetAsStartPage)
                    {
                        @Override
                        public void run()
                        {
                            editor.getPreferenceStore().setValue(START_PAGE_KEY, entry.getId());
                        }
                    });

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
            Action action = entry.getAction();

            if (indent == 0)
            {
                subMenu = new MenuManager(entry.getLabel());
                menuManager.add(subMenu);
            }
            else
            {
                if (subMenu == null || action == null)
                    continue;

                // cannot use the original action b/c it will not highlight the selected entry
                // in the sidebar
                String text = indent > Sidebar.STEP ? "- " + action.getText() : action.getText(); //$NON-NLS-1$
                SimpleAction menuAction = new SimpleAction(text, a -> sidebar.select(entry));
                menuAction.setImageDescriptor(action.getImageDescriptor());
                subMenu.add(menuAction);
            }
        }
    }

    public void selectDefaultView()
    {
        String defaultView = editor.getPreferenceStore().getString(START_PAGE_KEY);

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
        section.setAction(new Action(Messages.LabelSecurities, Images.PLUS.descriptor())
        {
            @Override
            public void run()
            {
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
            }
        });

        allSecurities = new Entry(section, new ActivateViewAction(Messages.LabelAllSecurities, "SecurityList", //$NON-NLS-1$
                        Images.SECURITY.descriptor()));
        allSecurities.setContextMenu(setAsStartPage);

        for (Watchlist watchlist : editor.getClient().getWatchlists())
            createWatchlistEntry(section, watchlist);
    }

    private void createWatchlistEntry(Entry section, final Watchlist watchlist)
    {
        Entry entry = new Entry(section, watchlist.getName());
        entry.setAction(new ActivateViewAction(watchlist.getName(), "SecurityList", watchlist, //$NON-NLS-1$
                        Images.WATCHLIST.descriptor()));

        entry.setContextMenu((e, m) -> watchlistContextMenuAboutToShow(watchlist, e, m));

        entry.addDropSupport(DND.DROP_MOVE, new Transfer[] { SecurityTransfer.getTransfer() }, new DropTargetAdapter()
        {
            @Override
            public void drop(DropTargetEvent event)
            {
                if (SecurityTransfer.getTransfer().isSupportedType(event.currentDataType))
                {
                    Security security = SecurityTransfer.getTransfer().getSecurity();
                    if (security != null)
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

                        editor.markDirty();

                        editor.notifyModelUpdated();
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

        setAsStartPage.menuAboutToShow(entry, manager);
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
        new Entry(section, new ActivateViewAction(Messages.LabelAccounts, "AccountList", //$NON-NLS-1$
                        Images.ACCOUNT.descriptor())).setContextMenu(setAsStartPage);
        new Entry(section, new ActivateViewAction(Messages.LabelPortfolios, "PortfolioList", //$NON-NLS-1$
                        Images.PORTFOLIO.descriptor())).setContextMenu(setAsStartPage);
        new Entry(section, new ActivateViewAction(Messages.LabelInvestmentPlans, "InvestmentPlanList", //$NON-NLS-1$
                        Images.INVESTMENTPLAN.descriptor())).setContextMenu(setAsStartPage);
    }

    private void createPerformanceSection(Sidebar sidebar)
    {
        Entry section = new Entry(sidebar, Messages.ClientEditorLabelReports);

        statementOfAssets = new Entry(section,
                        new ActivateViewAction(Messages.LabelStatementOfAssets, "StatementOfAssets")); //$NON-NLS-1$
        statementOfAssets.setContextMenu(setAsStartPage);

        new Entry(statementOfAssets,
                        new ActivateViewAction(Messages.ClientEditorLabelChart, "StatementOfAssetsHistory")) //$NON-NLS-1$
                                        .setContextMenu(setAsStartPage);
        new Entry(statementOfAssets, new ActivateViewAction(Messages.ClientEditorLabelHoldings, "HoldingsPieChart")) //$NON-NLS-1$
                        .setContextMenu(setAsStartPage);

        Entry performance = new Entry(section,
                        new ActivateViewAction(Messages.ClientEditorLabelPerformance, "dashboard.Dashboard")); //$NON-NLS-1$
        performance.setContextMenu(setAsStartPage);
        new Entry(performance, new ActivateViewAction(Messages.ClientEditorPerformanceCalculation, "Performance")) //$NON-NLS-1$
                        .setContextMenu(setAsStartPage);
        new Entry(performance, new ActivateViewAction(Messages.ClientEditorLabelChart, "PerformanceChart")) //$NON-NLS-1$
                        .setContextMenu(setAsStartPage);
        new Entry(performance,
                        new ActivateViewAction(Messages.ClientEditorLabelReturnsVolatility, "ReturnsVolatilityChart")) //$NON-NLS-1$
                                        .setContextMenu(setAsStartPage);
        new Entry(performance, new ActivateViewAction(Messages.LabelSecurities, "SecuritiesPerformance")) //$NON-NLS-1$
                        .setContextMenu(setAsStartPage);
        new Entry(performance, new ActivateViewAction(Messages.LabelDividends, "dividends.Dividends")) //$NON-NLS-1$
                        .setContextMenu(setAsStartPage);
    }

    private void createTaxonomyDataSection(final Sidebar sidebar)
    {
        taxonomies = new Entry(sidebar, Messages.LabelTaxonomies);
        taxonomies.setAction(new Action(Messages.LabelTaxonomies, Images.PLUS.descriptor())
        {
            @Override
            public void run()
            {
                showCreateTaxonomyMenu();
            }
        });

        for (Taxonomy taxonomy : editor.getClient().getTaxonomies())
            createTaxonomyEntry(taxonomies, taxonomy);
    }

    private Entry createTaxonomyEntry(Entry section, final Taxonomy taxonomy)
    {
        Entry entry = new Entry(section, taxonomy.getName());
        entry.setAction(new ActivateViewAction(taxonomy.getName(), "taxonomy.Taxonomy", taxonomy, null)); //$NON-NLS-1$
        entry.setContextMenu((e, m) -> taxonomyContextMenuAboutToShow(taxonomy, e, m));
        return entry;
    }

    private void showCreateTaxonomyMenu()
    {
        if (taxonomyMenu == null)
        {
            MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
            menuMgr.setRemoveAllWhenShown(true);
            menuMgr.addMenuListener(manager -> taxonomyCreateMenuAboutToShow(manager));
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

        manager.add(new Action(Messages.MenuTaxonomyDelete)
        {
            @Override
            public void run()
            {
                editor.getClient().removeTaxonomy(taxonomy);
                editor.markDirty();
                entry.dispose();
                statementOfAssets.select();
                scrolledComposite.setMinSize(sidebar.computeSize(SWT.DEFAULT, SWT.DEFAULT));
            }
        });
        manager.add(new Separator());

        addMoveUpAndDownActions(taxonomy, entry, manager);
        manager.add(new Separator());

        setAsStartPage.menuAboutToShow(entry, manager);
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
        new Entry(section, new ActivateViewAction(Messages.LabelConsumerPriceIndex, "ConsumerPriceIndexList")) //$NON-NLS-1$
                        .setContextMenu(setAsStartPage);
        new Entry(section, new ActivateViewAction(Messages.LabelCurrencies, "currency.Currency")) //$NON-NLS-1$
                        .setContextMenu(setAsStartPage);
        new Entry(section, new ActivateViewAction(Messages.LabelSettings, "settings.Settings")) //$NON-NLS-1$
                        .setContextMenu(setAsStartPage);

        if ("yes".equals(System.getProperty("name.abuchen.portfolio.debug"))) //$NON-NLS-1$ //$NON-NLS-2$
            new Entry(section, new ActivateViewAction("Browser Test", "BrowserTest")); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
