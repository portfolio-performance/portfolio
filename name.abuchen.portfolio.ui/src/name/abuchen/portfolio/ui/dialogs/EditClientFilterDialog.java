package name.abuchen.portfolio.ui.dialogs;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.snapshot.filter.PortfolioClientFilter;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.ClientFilterMenu;
import name.abuchen.portfolio.ui.util.ContextMenu;
import name.abuchen.portfolio.ui.util.LogoManager;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.CopyPasteSupport;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.viewers.StringEditingSupport;

public class EditClientFilterDialog extends Dialog
{
    private static class ContentProvider implements ITreeContentProvider
    {
        private final Map<String, Object> uuid2object = new HashMap<>();
        private List<ClientFilterMenu.Item> items = new ArrayList<>();

        public ContentProvider(Client client)
        {
            client.getPortfolios().forEach(p -> uuid2object.put(p.getUUID(), p));
            client.getAccounts().forEach(a -> uuid2object.put(a.getUUID(), a));
        }

        @SuppressWarnings("unchecked")
        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
        {
            this.items = (List<ClientFilterMenu.Item>) newInput;
        }

        @Override
        public Object[] getElements(Object inputElement)
        {
            return this.items.toArray();
        }

        @Override
        public Object[] getChildren(Object parentElement)
        {
            if (parentElement instanceof ClientFilterMenu.Item item)
            {
                String[] uuids = item.getUUIDs().split(","); //$NON-NLS-1$

                return Arrays.stream(uuids).map(uuid2object::get).filter(Objects::nonNull).toArray();
            }
            else
            {
                return new Object[0];
            }
        }

        @Override
        public Object getParent(Object element)
        {
            return null;
        }

        @Override
        public boolean hasChildren(Object element)
        {
            return getChildren(element).length > 0;
        }
    }

    private final Client client;
    private final IPreferenceStore preferences;
    private TreeViewer treeViewer;
    private List<ClientFilterMenu.Item> items;

    public EditClientFilterDialog(Shell parentShell, Client client, IPreferenceStore preferences)
    {
        super(parentShell);
        this.client = client;
        this.preferences = Objects.requireNonNull(preferences);
    }

    public void setItems(List<ClientFilterMenu.Item> items)
    {
        this.items = items;
    }

    @Override
    protected void configureShell(Shell shell)
    {
        super.configureShell(shell);
        shell.setText(Messages.LabelClientFilter);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite container = (Composite) super.createDialogArea(parent);

        Composite treeArea = new Composite(container, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, true).minSize(400, 400).applyTo(treeArea);

        TreeColumnLayout layout = new TreeColumnLayout();
        treeArea.setLayout(layout);

        treeViewer = new TreeViewer(treeArea, SWT.BORDER);
        final Tree tree = treeViewer.getTree();
        tree.setHeaderVisible(false);
        tree.setLinesVisible(false);

        ColumnEditingSupport.prepare(treeViewer);
        ColumnViewerToolTipSupport.enableFor(treeViewer, ToolTip.NO_RECREATE);
        CopyPasteSupport.enableFor(treeViewer);

        ShowHideColumnHelper columns = new ShowHideColumnHelper(EditClientFilterDialog.class.toString(), preferences,
                        treeViewer, layout);

        Column column = new Column(Messages.ColumnName, SWT.NONE, 100);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return String.valueOf(element);
            }

            @Override
            public Image getImage(Object element)
            {
                if (element instanceof ClientFilterMenu.Item)
                    return Images.FILTER_OFF.image();
                else
                    return LogoManager.instance().getDefaultColumnImage(element, client.getSettings());
            }
        });
        new StringEditingSupport(ClientFilterMenu.Item.class, "label").setMandatory(true) //$NON-NLS-1$
                        .addListener((e, n, o) -> treeViewer.refresh(e)).attachTo(column);

        columns.addColumn(column);

        columns.createColumns();

        // retrofit the column width to 100%
        layout.setColumnData(tree.getColumn(0), new ColumnWeightData(100));

        treeViewer.setContentProvider(new ContentProvider(client));
        treeViewer.setInput(items);

        setupDnD();

        new ContextMenu(treeViewer.getTree(), this::fillContextMenu).hook();

        Label info = new Label(container, SWT.NONE);
        info.setText(Messages.LabelClientFilterEditTooltip);

        return container;
    }

    private void setupDnD()
    {
        Transfer[] types = new Transfer[] { LocalSelectionTransfer.getTransfer() };
        treeViewer.addDragSupport(DND.DROP_MOVE, types, new DragSourceAdapter()
        {
            @Override
            public void dragStart(DragSourceEvent event)
            {
                // only allow dragging of filters
                IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
                event.doit = selection.size() == 1 && selection.getFirstElement() instanceof ClientFilterMenu.Item;
            }

            @Override
            public void dragSetData(DragSourceEvent event)
            {
                IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
                LocalSelectionTransfer.getTransfer().setSelection(selection);
                LocalSelectionTransfer.getTransfer().setSelectionSetTime(event.time & 0xFFFFFFFFL);
            }
        });

        treeViewer.addDropSupport(DND.DROP_MOVE, types, new ViewerDropAdapter(treeViewer)
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

                treeViewer.refresh();

                return true;
            }
        });
    }

    private void fillContextMenu(IMenuManager manager)
    {
        if (treeViewer.getStructuredSelection()
                        .getFirstElement() instanceof ClientFilterMenu.Item selectedFilterElement)
        {
            // insert new sub element (child) to filter
            manager.add(new Action(Messages.MenuReportingPeriodInsert)
            {
                @Override
                public void run()
                {
                    if (!(selectedFilterElement.getFilter() instanceof PortfolioClientFilter))
                        return;

                    PortfolioClientFilter filter = (PortfolioClientFilter) selectedFilterElement.getFilter();

                    LabelProvider labelProvider = new LabelProvider()
                    {
                        @Override
                        public Image getImage(Object element)
                        {
                            return LogoManager.instance().getDefaultColumnImage(element, client.getSettings());
                        }
                    };

                    ListSelectionDialog dialog = new ListSelectionDialog(Display.getDefault().getActiveShell(),
                                    labelProvider);

                    dialog.setTitle(Messages.LabelClientFilterDialogTitle);
                    dialog.setMessage(Messages.LabelClientFilterDialogMessage);

                    List<Object> elements = new ArrayList<>();
                    elements.addAll(client.getPortfolios());
                    elements.addAll(client.getAccounts());
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
                            treeViewer.refresh();
                        }
                    }
                }
            });
        }

        if ((treeViewer.getStructuredSelection().getFirstElement() instanceof ClientFilterMenu.Item)
                        || (treeViewer.getStructuredSelection().getFirstElement() instanceof Portfolio)
                        || (treeViewer.getStructuredSelection().getFirstElement() instanceof Account))
        {
            manager.add(new Action(Messages.MenuReportingPeriodDelete)
            {
                // delete filter (parent node), portfolio (child) or account
                // (child)
                @Override
                public void run()
                {
                    if (!(treeViewer.getSelection() instanceof TreeSelection))
                        return;

                    TreeSelection selection = (TreeSelection) treeViewer.getSelection();
                    TreePath[] paths = selection.getPaths();

                    for (TreePath p : paths)
                    {
                        if (p.getSegmentCount() == 1)
                        {
                            // parent node clicked (filter itself)
                            ClientFilterMenu.Item parentFilter = (ClientFilterMenu.Item) p.getFirstSegment();
                            String message = MessageFormat.format(Messages.MenuReportingPeriodDeleteConfirm,
                                            parentFilter.getLabel());
                            if (MessageDialog.openConfirm(Display.getDefault().getActiveShell(), getText(), message))
                                items.remove(p.getFirstSegment());
                        }
                        else if (p.getSegmentCount() == 2)
                        {
                            // child node clicked (portfolio or account)
                            items.forEach(it -> {
                                if (it == p.getFirstSegment() && it.getFilter() instanceof PortfolioClientFilter filter)
                                { // found parent item --> now remove selected
                                  // child item
                                    filter.removeElement(p.getLastSegment());

                                    // important step: update UUIDs because this
                                    // is basic information in settings
                                    it.setUUIDs(ClientFilterMenu.buildUUIDs(filter.getAllElements()));
                                }
                            });
                        }
                    }

                    treeViewer.refresh();
                }
            });
        }
    }
}
