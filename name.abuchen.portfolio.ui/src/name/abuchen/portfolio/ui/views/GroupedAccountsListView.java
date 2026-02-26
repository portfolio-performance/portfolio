package name.abuchen.portfolio.ui.views;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import jakarta.annotation.PostConstruct;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.ConfigurationSet;
import name.abuchen.portfolio.model.ConfigurationSet.Configuration;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.AccountSnapshot;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.PortfolioSnapshot;
import name.abuchen.portfolio.snapshot.filter.ClientFilter;
import name.abuchen.portfolio.snapshot.filter.PortfolioClientFilter;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.EditClientFilterDialog.ContentProvider;
import name.abuchen.portfolio.ui.dialogs.ListSelectionDialog;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.ClientFilterMenu;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.LogoManager;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.swt.StyledLabel;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport.ModificationListener;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.CopyPasteSupport;
import name.abuchen.portfolio.ui.util.viewers.LocaleSenstiveViewerComparator;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.viewers.StringEditingSupport;
import name.abuchen.portfolio.ui.views.columns.NameColumn;
import name.abuchen.portfolio.ui.views.columns.NameColumn.NameColumnLabelProvider;
import name.abuchen.portfolio.ui.views.columns.NoteColumn;
import name.abuchen.portfolio.ui.views.panes.GroupedAccountBalancePane;
import name.abuchen.portfolio.ui.views.panes.InformationPanePage;
import name.abuchen.portfolio.ui.views.panes.PortfolioHoldingsPane;
import name.abuchen.portfolio.ui.views.panes.StatementOfAssetsPane;

public class GroupedAccountsListView extends AbstractFinanceView implements ModificationListener
{
    private static final String EXPANSION_STATE = GroupedAccountsListView.class.getSimpleName()
                    + "-EXPANSION-DEFINITION"; //$NON-NLS-1$

    private CurrencyConverter converter;

    private TreeViewer groupedAccounts;
    private ConfigurationSet filterConfig;
    private String expansionStateDefinition;
    private ShowHideColumnHelper groupedAccountColumns;
    private LinkedList<ClientFilterMenu.Item> items = new LinkedList<>();
    private ClientFilterMenu clientFilterMenu;

    @Override
    protected String getDefaultTitle()
    {
        return Messages.LabelGroupedAccounts;
    }

    @PostConstruct
    public void setup(ExchangeRateProviderFactory factory)
    {
        converter = new CurrencyConverterImpl(factory, getClient().getBaseCurrency());

        clientFilterMenu = new ClientFilterMenu(getClient(), getPreferenceStore());
        filterConfig = clientFilterMenu.getfilterConfig();
        items = clientFilterMenu.getModifiableCustomItems();
        expansionStateDefinition = getPreferenceStore().getString(EXPANSION_STATE);
    }

    private void setInput()
    {
        groupedAccountColumns.invalidateCache();
        groupedAccounts.setInput(items);
    }

    @Override
    public void dispose()
    {
        // store expansion state
        StringJoiner expansionState = new StringJoiner(","); //$NON-NLS-1$
        for (Object element : groupedAccounts.getExpandedElements())
        {
            ClientFilterMenu.Item node = (ClientFilterMenu.Item) element;
            if (!(node instanceof ClientFilterMenu.Item))
                continue;
            expansionState.add(node.getId());
        }
        getPreferenceStore().setValue(EXPANSION_STATE, expansionState.toString());

        // keep tree order if drag and drop occurred or new grouped account was
        // created
        storeChangedFilter();

        super.dispose();
    }

    private void storeChangedFilter()
    {
        // store changed filters. keep in mind: filters could be deleted or
        // filter data changed. So first delete all stored filters and store
        // current ones
        filterConfig.clear();
        items.forEach(cf -> filterConfig.add(new Configuration(cf.getId(), cf.getLabel(), cf.getUUIDs())));
        getClient().touch();
    }

    private void expandNodes()
    {
        List<ClientFilterMenu.Item> expanded = new ArrayList<>();

        // check if we have expansion state in preferences
        if (expansionStateDefinition != null && !expansionStateDefinition.isEmpty())
        {
            Set<String> uuid = new HashSet<>(Arrays.asList(expansionStateDefinition.split(","))); //$NON-NLS-1$
            for (TreeItem element : groupedAccounts.getTree().getItems())
            {
                ClientFilterMenu.Item node = (ClientFilterMenu.Item) element.getData();
                if (node instanceof ClientFilterMenu.Item && uuid.contains(node.getId()))
                    expanded.add(node);
            }

            groupedAccounts.getTree().setRedraw(false);
            try
            {
                groupedAccounts.setExpandedElements(expanded.toArray());
            }
            finally
            {
                groupedAccounts.getTree().setRedraw(true);
            }
        }
        else
        {
            groupedAccounts.collapseAll();
        }
    }

    @Override
    protected void notifyViewCreationCompleted()
    {
        setInput();

        groupedAccounts.refresh();

        if (groupedAccounts.getTree().getItemCount() > 0 && !items.isEmpty())
            groupedAccounts.setSelection(new StructuredSelection(items.getFirst()));

        expandNodes();
    }

    @Override
    public void notifyModelUpdated()
    {
        // update currency converter (in case the base currency changes)
        converter = converter.with(getClient().getBaseCurrency());
        groupedAccounts.refresh();
    }

    @Override
    public void onModified(Object element, Object newValue, Object oldValue)
    {
        storeChangedFilter();
        markDirty();
    }

    @Override
    protected void addButtons(ToolBarManager toolBar)
    {
        addNewButton(toolBar);
        addConfigButton(toolBar);
    }

    private void addNewButton(ToolBarManager toolBar)
    {
        toolBar.add(new DropDown(Messages.LabelClientFilterNew, Images.PLUS, SWT.NONE,
                        manager -> manager.add(new SimpleAction(Messages.LabelClientFilterNew,
                                        a -> clientFilterMenu.createCustomFilter().ifPresent(newItem -> {
                                            groupedAccounts.refresh();
                                            // expand the selected node
                                            groupedAccounts.setExpandedState(newItem, true);
                                            // select the newly created account
                                            groupedAccounts.setSelection(new StructuredSelection(newItem));
                                        })))));
    }

    private void addConfigButton(final ToolBarManager toolBar)
    {
        toolBar.add(new DropDown(Messages.MenuShowHideColumns, Images.CONFIG, SWT.NONE,
                        manager -> groupedAccountColumns.menuAboutToShow(manager)));
    }

    // //////////////////////////////////////////////////////////////
    // top table: accounts
    // //////////////////////////////////////////////////////////////

    @Override
    protected Control createBody(Composite parent)
    {
        var parentComposite = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(1).applyTo(parentComposite);

        if (items.isEmpty())
        {
            // show a help message if no items exist
            var label = new StyledLabel(parentComposite, SWT.WRAP);
            GridDataFactory.fillDefaults().grab(true, false).indent(5, 0).applyTo(label);
            label.setText(Messages.HelpGroupedAccountsListView);
        }

        Composite container = new Composite(parentComposite, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
        TreeColumnLayout layout = new TreeColumnLayout();
        container.setLayout(layout);

        groupedAccounts = new TreeViewer(container, SWT.FULL_SELECTION);
        final Tree tree = groupedAccounts.getTree();
        tree.setHeaderVisible(true);
        tree.setLinesVisible(true);

        ColumnEditingSupport.prepare(getEditorActivationState(), groupedAccounts);
        ColumnViewerToolTipSupport.enableFor(groupedAccounts, ToolTip.NO_RECREATE);
        CopyPasteSupport.enableFor(groupedAccounts);

        groupedAccountColumns = new ShowHideColumnHelper(GroupedAccountsListView.class.getSimpleName() + "@top", //$NON-NLS-1$
                        getPreferenceStore(), groupedAccounts, layout);

        Column column = new NameColumn("name", Messages.ClientEditorLabelClientMasterData, SWT.None, 200, getClient()); //$NON-NLS-1$
        column.setLabelProvider(new NameColumnLabelProvider(getClient())
        {
            @Override
            public String getText(Object element)
            {
                return element instanceof ClientFilterMenu.Item item ? item.getLabel() : super.getText(element);
            }

            @Override
            public Image getImage(Object element)
            {
                if (element instanceof ClientFilterMenu.Item)
                    return Images.GROUPEDACCOUNTS.image();
                else
                    return super.getImage(element);
            }
        });
        new StringEditingSupport(ClientFilterMenu.Item.class, "label").setMandatory(true) //$NON-NLS-1$
                        .addListener(this).attachTo(column);
        column.setRemovable(false);
        // top level nodes order is manually sorted by the user via drag & drop
        column.setSorter(ColumnViewerSorter.create(o -> switch (o)
        {
            case Portfolio portfolio -> portfolio.getName();
            case Account account -> account.getName();
            default -> null;
        }));

        groupedAccountColumns.addColumn(column);

        column = new Column("volume", Messages.ColumnBalance, SWT.RIGHT, 100); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                if (element instanceof Portfolio portfolio)
                {
                    PortfolioSnapshot snapshot = PortfolioSnapshot.create(portfolio, converter, LocalDate.now());
                    return Values.Money.format(snapshot.getValue(), getClient().getBaseCurrency());
                }
                else if (element instanceof Account account)
                {
                    AccountSnapshot snapshot = AccountSnapshot.create(account, converter, LocalDate.now());
                    // show unconverted amount
                    return Values.Money.format(snapshot.getUnconvertedFunds(), getClient().getBaseCurrency());
                }
                else if (element instanceof ClientFilterMenu.Item groupedAccount)
                {
                    ClientFilter clientFilter = groupedAccount.getFilter();
                    ClientSnapshot snapshot = ClientSnapshot.create(clientFilter.filter(getClient()), converter,
                                    LocalDate.now());
                    return Values.Money.format(snapshot.getMonetaryAssets(), getClient().getBaseCurrency());
                }
                return null;
            }
        });
        // add a sorter
        column.setSorter(ColumnViewerSorter.create(o -> {
            if (o instanceof Portfolio portfolio)
            {
                PortfolioSnapshot snapshot = PortfolioSnapshot.create(portfolio, converter, LocalDate.now());
                return snapshot.getValue();
            }
            else if (o instanceof Account account)
            {
                AccountSnapshot snapshot = AccountSnapshot.create(account, converter, LocalDate.now());
                return snapshot.getFunds(); // converted amount to sort
            }
            return null;
        }));
        groupedAccountColumns.addColumn(column);

        column = new NoteColumn();
        column.getEditingSupport().addListener(this);
        groupedAccountColumns.addColumn(column);

        groupedAccountColumns.createColumns(true);

        groupedAccounts.setContentProvider(new ContentProvider(getClient()));

        groupedAccounts.addSelectionChangedListener(event -> {
            Object treeItem = event.getStructuredSelection().getFirstElement();
            setInformationPaneInput(treeItem);
        });

        setupDnD();

        hookContextMenu(tree, this::fillContextMenu);

        return parentComposite;
    }

    private void setupDnD()
    {
        Transfer[] types = new Transfer[] { LocalSelectionTransfer.getTransfer() };
        groupedAccounts.addDragSupport(DND.DROP_MOVE, types, new DragSourceAdapter()
        {
            @Override
            public void dragStart(DragSourceEvent event)
            {
                // only allow dragging of filters
                IStructuredSelection selection = (IStructuredSelection) groupedAccounts.getSelection();
                event.doit = selection.size() == 1 && selection.getFirstElement() instanceof ClientFilterMenu.Item;
            }

            @Override
            public void dragSetData(DragSourceEvent event)
            {
                IStructuredSelection selection = (IStructuredSelection) groupedAccounts.getSelection();
                LocalSelectionTransfer.getTransfer().setSelection(selection);
                LocalSelectionTransfer.getTransfer().setSelectionSetTime(event.time & 0xFFFFFFFFL);
            }
        });

        groupedAccounts.addDropSupport(DND.DROP_MOVE, types, new ViewerDropAdapter(groupedAccounts)
        {
            @Override
            public boolean validateDrop(Object target, int operation, TransferData transferType)
            {
                return target instanceof ClientFilterMenu.Item;
            }

            @Override
            public boolean performDrop(Object data)
            {
                IStructuredSelection selection = (IStructuredSelection) data;

                List<ClientFilterMenu.Item> movedItems = new ArrayList<>();
                for (Object o : selection.toList())
                    movedItems.add((ClientFilterMenu.Item) o);

                items.removeAll(movedItems);

                Object destination = getCurrentTarget();
                int index = items.indexOf(destination);
                if (index >= 0)
                {
                    int location = getCurrentLocation();
                    if (location == ViewerDropAdapter.LOCATION_ON || location == ViewerDropAdapter.LOCATION_AFTER)
                        index++;

                    items.addAll(index, movedItems);
                }
                else
                {
                    items.addAll(movedItems);
                }

                groupedAccounts.refresh();

                return true;
            }
        });
    }

    private void fillContextMenu(IMenuManager manager)
    {
        var treeSelection = (TreeSelection) groupedAccounts.getSelection();
        var element = treeSelection.getFirstElement();
        if (element == null)
            return;

        if (element instanceof ClientFilterMenu.Item selectedFilterElement)
        {
            manager.add(new SimpleAction(Messages.MenuReportingPeriodInsert, a -> {
                insertElementInFilter(selectedFilterElement);
                storeChangedFilter();
                onRecalculationNeeded();
            }));

            manager.add(new Separator());

            manager.add(new SimpleAction(Messages.MenuReportingPeriodDelete, a -> {
                deleteFilter(selectedFilterElement);
                storeChangedFilter();
                if (groupedAccounts.getTree().getItemCount() > 0 && !items.isEmpty())
                    groupedAccounts.setSelection(new StructuredSelection(items.getFirst()));
                onRecalculationNeeded();
            }));

        }
        else if (element instanceof Portfolio || element instanceof Account)
        {
            var filterItem = (ClientFilterMenu.Item) treeSelection.getPathsFor(element)[0].getFirstSegment();

            manager.add(new SimpleAction(Messages.ChartSeriesPickerRemove, a -> {
                deleteElementInFilter(element, filterItem);
                storeChangedFilter();
                if (element instanceof ClientFilterMenu.Item && groupedAccounts.getTree().getItemCount() > 0
                                && !items.isEmpty())
                    groupedAccounts.setSelection(new StructuredSelection(items.getFirst()));
                onRecalculationNeeded();
            }));
        }
    }

    private void deleteFilter(ClientFilterMenu.Item filterItem)
    {
        String message = MessageFormat.format(Messages.MenuReportingPeriodDeleteConfirm, filterItem.getLabel());
        if (MessageDialog.openConfirm(Display.getDefault().getActiveShell(), Messages.MenuReportingPeriodDelete,
                        message))
            items.remove(filterItem);
    }

    private void deleteElementInFilter(Object element, ClientFilterMenu.Item filterItem)
    {
        if (filterItem.getFilter() instanceof PortfolioClientFilter filter)
        {
            filter.removeElement(element);
            // important step: update UUIDs because this is basic
            // information in settings
            filterItem.setUUIDs(ClientFilterMenu.buildUUIDs(filter.getAllElements()));
        }
    }

    private void insertElementInFilter(ClientFilterMenu.Item selectedFilterElement)
    {
        if (!(selectedFilterElement.getFilter() instanceof PortfolioClientFilter))
            return;

        PortfolioClientFilter filter = (PortfolioClientFilter) selectedFilterElement.getFilter();

        LabelProvider labelProvider = new LabelProvider()
        {
            @Override
            public Image getImage(Object element)
            {
                return LogoManager.instance().getDefaultColumnImage(element, getClient().getSettings());
            }
        };

        ListSelectionDialog dialog = new ListSelectionDialog(Display.getDefault().getActiveShell(), labelProvider);

        dialog.setTitle(Messages.LabelClientFilterDialogTitle);
        dialog.setMessage(Messages.LabelClientFilterDialogMessage);
        dialog.setViewerComparator(new LocaleSenstiveViewerComparator(labelProvider));

        List<Object> elements = new ArrayList<>();
        elements.addAll(getClient().getPortfolios());
        elements.addAll(getClient().getAccounts());
        for (Object el : filter.getAllElements())
            elements.remove(el); // remove already assigned elements

        dialog.setElements(elements);

        if (dialog.open() == Window.OK)
        {
            Object[] selected = dialog.getResult();
            if (selected.length > 0)
            {
                for (Object sel : selected)
                {
                    filter.addElement(sel);
                }

                // important step: update UUIDs because this is
                // basic information in settings
                selectedFilterElement.setUUIDs(ClientFilterMenu.buildUUIDs(filter.getAllElements()));
            }
        }
    }

    // //////////////////////////////////////////////////////////////
    // bottom table: statement of assets and charts
    // //////////////////////////////////////////////////////////////

    @Override
    protected void addPanePages(List<InformationPanePage> pages)
    {
        super.addPanePages(pages);
        pages.add(make(StatementOfAssetsPane.class));
        pages.add(make(GroupedAccountBalancePane.class));
        pages.add(make(PortfolioHoldingsPane.class));
    }
}
