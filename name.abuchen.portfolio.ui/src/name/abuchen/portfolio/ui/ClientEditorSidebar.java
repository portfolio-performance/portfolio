package name.abuchen.portfolio.ui;

import java.util.UUID;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.model.TaxonomyTemplate;
import name.abuchen.portfolio.model.Watchlist;
import name.abuchen.portfolio.ui.Sidebar.Entry;
import name.abuchen.portfolio.ui.dnd.SecurityTransfer;
import name.abuchen.portfolio.ui.util.LabelOnly;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

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

    private ClientEditor editor;

    private Menu taxonomyMenu;

    private Sidebar sidebar;
    private Entry allSecurities;
    private Entry statementOfAssets;
    private Entry taxonomies;

    public ClientEditorSidebar(ClientEditor editor)
    {
        this.editor = editor;
    }

    public Control createSidebarControl(Composite parent)
    {
        sidebar = new Sidebar(parent, SWT.NONE);

        createGeneralDataSection(sidebar);
        createMasterDataSection(sidebar);
        createPerformanceSection(sidebar);
        createTaxonomyDataSection(sidebar);
        createMiscSection(sidebar);

        return sidebar;
    }

    public void selectDefaultView()
    {
        sidebar.select(statementOfAssets);
    }

    private void createGeneralDataSection(final Sidebar sidebar)
    {
        final Entry section = new Entry(sidebar, Messages.LabelSecurities);
        section.setAction(new Action(Messages.LabelSecurities, PortfolioPlugin.descriptor(PortfolioPlugin.IMG_PLUS))
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
            }
        });

        allSecurities = new Entry(section, new ActivateViewAction(Messages.LabelAllSecurities, "SecurityList", //$NON-NLS-1$
                        PortfolioPlugin.descriptor(PortfolioPlugin.IMG_SECURITY)));

        for (Watchlist watchlist : editor.getClient().getWatchlists())
            createWatchlistEntry(section, watchlist);
    }

    private void createWatchlistEntry(Entry section, final Watchlist watchlist)
    {
        final Entry entry = new Entry(section, watchlist.getName());
        entry.setAction(new ActivateViewAction(watchlist.getName(), "SecurityList", watchlist, //$NON-NLS-1$
                        PortfolioPlugin.descriptor(PortfolioPlugin.IMG_WATCHLIST)));

        entry.setContextMenu(new IMenuListener()
        {
            @Override
            public void menuAboutToShow(IMenuManager manager)
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
                    }
                });
            }
        });

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

    private String askWatchlistName(String initialValue)
    {
        InputDialog dlg = new InputDialog(editor.getSite().getShell(), Messages.WatchlistEditDialog,
                        Messages.WatchlistEditDialogMsg, initialValue, null);
        if (dlg.open() != InputDialog.OK)
            return null;

        return dlg.getValue();
    }

    private void createMasterDataSection(Sidebar sidebar)
    {
        Entry section = new Entry(sidebar, Messages.ClientEditorLabelClientMasterData);
        new Entry(section, new ActivateViewAction(Messages.LabelAccounts, "AccountList", //$NON-NLS-1$
                        PortfolioPlugin.descriptor(PortfolioPlugin.IMG_ACCOUNT)));
        new Entry(section, new ActivateViewAction(Messages.LabelPortfolios, "PortfolioList", //$NON-NLS-1$
                        PortfolioPlugin.descriptor(PortfolioPlugin.IMG_PORTFOLIO)));
        new Entry(section, new ActivateViewAction(Messages.LabelInvestmentPlans, "InvestmentPlanList", //$NON-NLS-1$
                        PortfolioPlugin.descriptor(PortfolioPlugin.IMG_INVESTMENTPLAN)));
    }

    private void createPerformanceSection(Sidebar sidebar)
    {
        Entry section = new Entry(sidebar, Messages.ClientEditorLabelReports);

        statementOfAssets = new Entry(section, new ActivateViewAction(Messages.LabelStatementOfAssets,
                        "StatementOfAssets")); //$NON-NLS-1$
        new Entry(statementOfAssets,
                        new ActivateViewAction(Messages.ClientEditorLabelChart, "StatementOfAssetsHistory")); //$NON-NLS-1$
        new Entry(statementOfAssets, new ActivateViewAction(Messages.ClientEditorLabelHoldings, "HoldingsPieChart")); //$NON-NLS-1$

        Entry performance = new Entry(section, new ActivateViewAction(Messages.ClientEditorLabelPerformance,
                        "Performance")); //$NON-NLS-1$
        new Entry(performance, new ActivateViewAction(Messages.ClientEditorLabelChart, "PerformanceChart")); //$NON-NLS-1$
        new Entry(performance, new ActivateViewAction(Messages.LabelSecurities, "SecurityPerformance")); //$NON-NLS-1$
    }

    private void createTaxonomyDataSection(final Sidebar sidebar)
    {
        taxonomies = new Entry(sidebar, "Taxonomies");
        taxonomies.setAction(new Action("Taxonomies", PortfolioPlugin.descriptor(PortfolioPlugin.IMG_PLUS))
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

    private void createTaxonomyEntry(Entry section, final Taxonomy taxonomy)
    {
        final Entry entry = new Entry(section, taxonomy.getName());
        entry.setAction(new ActivateViewAction(taxonomy.getName(), "taxonomy.Taxonomy", taxonomy, null)); //$NON-NLS-1$
        entry.setContextMenu(new IMenuListener()
        {
            @Override
            public void menuAboutToShow(IMenuManager manager)
            {
                manager.add(new Action("Rename Taxonomy")
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

                manager.add(new Action("Delete Taxonomy")
                {
                    @Override
                    public void run()
                    {
                        editor.getClient().removeTaxonomy(taxonomy);
                        editor.markDirty();
                        entry.dispose();
                        statementOfAssets.select();
                    }
                });
            }
        });
    }

    private void showCreateTaxonomyMenu()
    {
        if (taxonomyMenu == null)
        {
            MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
            menuMgr.setRemoveAllWhenShown(true);
            menuMgr.addMenuListener(new IMenuListener()
            {
                @Override
                public void menuAboutToShow(IMenuManager manager)
                {
                    taxonomyMenuAboutToShow(manager);
                }
            });
            taxonomyMenu = menuMgr.createContextMenu(sidebar.getShell());

            sidebar.addDisposeListener(new DisposeListener()
            {
                @Override
                public void widgetDisposed(DisposeEvent e)
                {
                    taxonomyMenu.dispose();
                }
            });
        }
        taxonomyMenu.setVisible(true);
    }

    private void taxonomyMenuAboutToShow(IMenuManager manager)
    {
        manager.add(new Action("Neu...")
        {
            @Override
            public void run()
            {
                String name = askTaxonomyName("New Taxonomy");
                if (name == null)
                    return;

                Taxonomy taxonomy = new Taxonomy(UUID.randomUUID().toString(), name);
                taxonomy.setRootNode(new Classification(UUID.randomUUID().toString(), name));

                editor.getClient().addTaxonomy(taxonomy);
                editor.markDirty();
                createTaxonomyEntry(taxonomies, taxonomy);
                sidebar.layout();
            }
        });

        manager.add(new Separator());
        manager.add(new LabelOnly("Templates"));

        for (final TaxonomyTemplate template : TaxonomyTemplate.list())
        {
            manager.add(new Action(template.getName())
            {
                @Override
                public void run()
                {
                    Taxonomy taxonomy = template.build();

                    editor.getClient().addTaxonomy(taxonomy);
                    editor.markDirty();
                    createTaxonomyEntry(taxonomies, taxonomy);
                    sidebar.layout();
                }
            });
        }
    }

    private String askTaxonomyName(String initialValue)
    {
        InputDialog dlg = new InputDialog(editor.getSite().getShell(), "Edit Taxonomy Name",
                        "Give the taxonomy a name", initialValue, null);
        if (dlg.open() != InputDialog.OK)
            return null;

        return dlg.getValue();
    }

    private void createMiscSection(Sidebar sidebar)
    {
        Entry section = new Entry(sidebar, Messages.ClientEditorLabelGeneralData);
        new Entry(section, new ActivateViewAction(Messages.LabelConsumerPriceIndex, "ConsumerPriceIndexList")); //$NON-NLS-1$
    }

}
