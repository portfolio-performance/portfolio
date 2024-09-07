package name.abuchen.portfolio.ui.views.taxonomy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.google.common.base.Strings;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.Named;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.dnd.SecurityTransfer;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.editor.PortfolioPart;
import name.abuchen.portfolio.ui.selection.SecuritySelection;
import name.abuchen.portfolio.ui.selection.SelectionService;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.ContextMenu;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.TreeViewerCSVExporter;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport.ModificationListener;
import name.abuchen.portfolio.ui.util.viewers.CopyPasteSupport;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.viewers.StringEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ValueEditingSupport;
import name.abuchen.portfolio.ui.views.SecurityContextMenu;
import name.abuchen.portfolio.ui.views.columns.AttributeColumn;
import name.abuchen.portfolio.ui.views.columns.IsinColumn;
import name.abuchen.portfolio.ui.views.columns.NameColumn;
import name.abuchen.portfolio.ui.views.columns.NameColumn.NameColumnLabelProvider;
import name.abuchen.portfolio.ui.views.columns.NoteColumn;
import name.abuchen.portfolio.ui.views.columns.SymbolColumn;
import name.abuchen.portfolio.util.TextUtil;

/* package */abstract class AbstractNodeTreeViewer extends Page implements ModificationListener
{
    private static class ItemContentProvider implements ITreeContentProvider
    {
        private TaxonomyModel model;

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
        {
            model = (TaxonomyModel) newInput;
        }

        @Override
        public Object[] getElements(Object inputElement)
        {
            return model.getVirtualRootNode().getChildren().toArray();
        }

        @Override
        public boolean hasChildren(Object element)
        {
            return !((TaxonomyNode) element).getChildren().isEmpty();
        }

        @Override
        public Object[] getChildren(Object parentElement)
        {
            return ((TaxonomyNode) parentElement).getChildren().toArray();
        }

        @Override
        public Object getParent(Object element)
        {
            return ((TaxonomyNode) element).getParent();
        }

        @Override
        public void dispose()
        {
            // no resources allocated
        }
    }

    private static class NodeDragListener extends DragSourceAdapter
    {
        private TreeViewer treeViewer;

        public NodeDragListener(TreeViewer treeViewer)
        {
            this.treeViewer = treeViewer;
        }

        @Override
        public void dragSetData(DragSourceEvent event)
        {
            @SuppressWarnings("unchecked")
            List<TaxonomyNode> nodes = ((TreeSelection) treeViewer.getSelection()).toList();
            TaxonomyNodeTransfer.getTransfer().setTaxonomyNodes(nodes);

            // if only one node is dragged and the node is of type security,
            // then also enable the drag and drop of securities into a watchlist

            Assignment assignment = nodes.size() == 1 ? nodes.get(0).getAssignment() : null;
            if (assignment != null && assignment.getInvestmentVehicle() instanceof Security security)
            {
                List<Security> securities = new ArrayList<>();
                securities.add(security);
                SecurityTransfer.getTransfer().setSecurities(securities);
            }
            else
            {
                SecurityTransfer.getTransfer().setSecurities(null);
            }

            event.data = nodes;
        }

        @Override
        public void dragStart(DragSourceEvent event)
        {
            // drag data must not include the two visible root nodes, i.e. the
            // unassigned category and the root category of the classification
            @SuppressWarnings("unchecked")
            List<TaxonomyNode> nodes = ((TreeSelection) treeViewer.getSelection()).toList();

            event.doit = nodes.stream().noneMatch(n -> n.getParent().isRoot());
        }
    }

    private static class NodeDropListener extends ViewerDropAdapter
    {
        private AbstractNodeTreeViewer viewer;

        public NodeDropListener(AbstractNodeTreeViewer viewer)
        {
            super(viewer.getNodeViewer());
            this.viewer = viewer;
        }

        @Override
        public boolean performDrop(Object data) // NOSONAR
        {
            List<TaxonomyNode> droppedNodes = getSubtreeNodes(TaxonomyNodeTransfer.getTransfer().getTaxonomyNodes());

            final TaxonomyNode target = (TaxonomyNode) getCurrentTarget();

            // do not drop upon itself
            if (droppedNodes.contains(target))
                return false;

            // do not allow dragging a parent into the child
            for (TaxonomyNode n : target.getPath())
            {
                if (droppedNodes.contains(n))
                    return false;
            }

            // do not allow dragging into virtual root
            if (target.isRoot() || target.getParent().isRoot())
                return false;

            // do not allow dragging of categories into the "unassigned
            // category" (must be deleted via right-click instead)
            if (target.getPath().stream().anyMatch(TaxonomyNode::isUnassignedCategory)
                            && droppedNodes.stream().anyMatch(TaxonomyNode::isClassification))
                return false;

            switch (getCurrentLocation())
            {
                case ViewerDropAdapter.LOCATION_AFTER:
                    TaxonomyNode t = target;
                    for (TaxonomyNode node : droppedNodes)
                    {
                        node.insertAfter(t);
                        t = node;
                    }

                    viewer.onTaxnomyNodeEdited(viewer.getModel().getVirtualRootNode());
                    break;
                case ViewerDropAdapter.LOCATION_BEFORE:
                    for (TaxonomyNode node : droppedNodes)
                        node.insertBefore(target);

                    viewer.onTaxnomyNodeEdited(viewer.getModel().getVirtualRootNode());
                    break;
                case ViewerDropAdapter.LOCATION_ON:
                    // do not drag parent into child
                    if (target.getPath().stream().anyMatch(droppedNodes::contains))
                        return false;
                    // do not move node into its own parent
                    droppedNodes.stream().filter(n -> !n.getParent().equals(target)).forEach(n -> n.moveTo(target));

                    viewer.onTaxnomyNodeEdited(viewer.getModel().getVirtualRootNode());
                    break;
                case ViewerDropAdapter.LOCATION_NONE:
                default:
                    break;
            }

            return true;
        }

        /**
         * Returns the unique subtrees, e.g. removes the all nodes whose parent
         * is already selected
         */
        private List<TaxonomyNode> getSubtreeNodes(List<TaxonomyNode> nodes)
        {
            List<TaxonomyNode> answer = new ArrayList<>();
            for (TaxonomyNode node : nodes)
            {
                List<TaxonomyNode> path = node.getPath();
                if (path.subList(0, path.size() - 1).stream().noneMatch(nodes::contains))
                    answer.add(node);
            }
            return answer;
        }

        @Override
        public boolean validateDrop(Object target, int operation, TransferData transferType)
        {
            if (!(target instanceof TaxonomyNode targetNode))
                return false;

            int location = determineLocation(this.getCurrentEvent());

            if (targetNode.isClassification())
                return true;
            else if (targetNode.isAssignment())
                return location == LOCATION_AFTER || location == LOCATION_BEFORE;
            else
                return false;
        }
    }

    private enum SortCriterion
    {
        TYPE, NAME, ACTUAL
    }

    protected static final String MENU_GROUP_DEFAULT_ACTIONS = "defaultActions"; //$NON-NLS-1$
    protected static final String MENU_GROUP_CUSTOM_ACTIONS = "customActions"; //$NON-NLS-1$
    protected static final String MENU_GROUP_DELETE_ACTIONS = "deleteActions"; //$NON-NLS-1$

    @Inject
    private SelectionService selectionService;

    @Inject
    private PortfolioPart part;

    private boolean useIndirectQuotation = false;

    private final AbstractFinanceView view;
    private TreeViewer nodeViewer;
    private ShowHideColumnHelper support;

    private boolean isFirstView = true;

    public AbstractNodeTreeViewer(AbstractFinanceView view, TaxonomyModel model, TaxonomyNodeRenderer renderer)
    {
        super(model, renderer);
        this.view = view;
    }

    @Inject
    private void setUseIndirectQuotation(
                    @Preference(value = UIConstants.Preferences.USE_INDIRECT_QUOTATION) boolean useIndirectQuotation)
    {
        this.useIndirectQuotation = useIndirectQuotation;

        if (nodeViewer != null)
            nodeViewer.refresh();
    }

    protected abstract String readExpansionState();

    protected abstract void storeExpansionState(String expanded);

    protected final TreeViewer getNodeViewer()
    {
        return nodeViewer;
    }

    @Override
    public void onModified(Object element, Object newValue, Object oldValue)
    {
        onTaxnomyNodeEdited((TaxonomyNode) element);
    }

    public void onWeightModified(Object element, Object newValue, Object oldValue)
    {
        TaxonomyNode node = (TaxonomyNode) element;

        if (node.getWeight() > Classification.ONE_HUNDRED_PERCENT)
            node.setWeight(Classification.ONE_HUNDRED_PERCENT);
        else if (node.getWeight() < 0)
            node.setWeight(0);

        if (node.isAssignment())
        {
            int oldWeight = (Integer) oldValue;
            doChangeAssignmentWeight(node, oldWeight);
        }

        onModified(element, newValue, oldValue);
    }

    @Override
    public void configMenuAboutToShow(IMenuManager manager)
    {
        support.menuAboutToShow(manager);
    }

    @Override
    public void exportMenuAboutToShow(IMenuManager manager)
    {
        manager.add(new SimpleAction(Messages.MenuExportData, action -> new TreeViewerCSVExporter(nodeViewer)
                        .export(getModel().getTaxonomy().getName() + ".csv"))); //$NON-NLS-1$
    }

    @Override
    public final Control createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TreeColumnLayout layout = new TreeColumnLayout();
        container.setLayout(layout);

        nodeViewer = new TreeViewer(container, SWT.FULL_SELECTION | SWT.MULTI);

        ColumnEditingSupport.prepare(nodeViewer);
        ColumnViewerToolTipSupport.enableFor(nodeViewer, ToolTip.NO_RECREATE);
        CopyPasteSupport.enableFor(nodeViewer);

        support = new ShowHideColumnHelper(getClass().getSimpleName() + '-' + getModel().getTaxonomy().getId(),
                        getPreferenceStore(), nodeViewer, layout);

        addColumns(support);

        support.createColumns(true);

        nodeViewer.getTree().setHeaderVisible(true);
        nodeViewer.getTree().setLinesVisible(true);
        nodeViewer.setContentProvider(new ItemContentProvider());

        nodeViewer.addDragSupport(DND.DROP_MOVE | DND.DROP_COPY,
                        new Transfer[] { TaxonomyNodeTransfer.getTransfer(), SecurityTransfer.getTransfer() },
                        new NodeDragListener(nodeViewer));
        nodeViewer.addDropSupport(DND.DROP_MOVE | DND.DROP_COPY, new Transfer[] { TaxonomyNodeTransfer.getTransfer() },
                        new NodeDropListener(this));

        nodeViewer.addFilter(new ViewerFilter()
        {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;

                if (node.isAssignment())
                {
                    // is assignment = leaf node
                    for (Predicate<TaxonomyNode> predicate : getModel().getNodeFilters())
                        if (!predicate.test(node))
                            return false;

                    Pattern filterPattern = getModel().getFilterPattern();
                    if (filterPattern != null)
                    {
                        String label = node.getName();
                        return filterPattern.matcher(label).matches();
                    }
                }
                else
                {
                    // is classification = not a leaf node -> search children
                    Pattern filterPattern = getModel().getFilterPattern();
                    if (filterPattern != null)
                    {
                        if (filterPattern.matcher(node.getName()).matches())
                            return true;

                        ITreeContentProvider provider = (ITreeContentProvider) nodeViewer.getContentProvider();
                        for (Object child : provider.getChildren(element))
                        {
                            if (select(viewer, element, child))
                                return true;
                        }

                        return false;
                    }
                }
                return true;
            }
        });

        nodeViewer.addSelectionChangedListener(event -> {
            TaxonomyNode node = ((TaxonomyNode) ((IStructuredSelection) event.getSelection()).getFirstElement());
            view.setInformationPaneInput(node);

            // use a set because securities can show up multiple times in the
            // hierarchy of the taxonomy
            var securities = event.getStructuredSelection().stream()
                            .filter(e -> ((TaxonomyNode) e).getBackingSecurity() != null)
                            .map(e -> ((TaxonomyNode) e).getBackingSecurity()).collect(Collectors.toSet());
            if (!securities.isEmpty())
                selectionService.setSelection(
                                new SecuritySelection(getModel().getClient(), new ArrayList<>(securities)));
            else
                selectionService.setSelection(null);
        });

        nodeViewer.setInput(getModel());

        new ContextMenu(nodeViewer.getControl(), this::fillContextMenu).hook();

        return container;
    }

    protected abstract void addColumns(ShowHideColumnHelper support);

    protected void addDimensionColumn(ShowHideColumnHelper support)
    {
        Column column = new NameColumn("txname", Messages.ColumnLevels, SWT.NONE, 400, part.getClient()); //$NON-NLS-1$
        column.setLabelProvider(new NameColumnLabelProvider(part.getClient()) // NOSONAR
        {
            @Override
            public Image getImage(Object e)
            {
                if (((TaxonomyNode) e).isUnassignedCategory())
                    return Images.UNASSIGNED_CATEGORY.image();
                return super.getImage(e);
            }

            @Override
            public String getToolTipText(Object e)
            {
                TaxonomyNode node = (TaxonomyNode) e;

                if (!node.isClassification())
                    return super.getToolTipText(e);

                String note = node.getClassification().getNote();

                return Strings.isNullOrEmpty(note) ? super.getToolTipText(e)
                                : TextUtil.wordwrap(node.getName() + "\n\n" + note); //$NON-NLS-1$
            }

        });
        new StringEditingSupport(Named.class, "name") //$NON-NLS-1$
        {
            @Override
            public boolean canEdit(Object element)
            {
                if (((TaxonomyNode) element).isUnassignedCategory())
                    return false;
                else
                    return super.canEdit(element);
            }
        }.setMandatory(true).addListener(this).attachTo(column);
        column.setRemovable(false);
        // drag & drop sorting does not work well with auto sorting
        column.setSorter(null);
        support.addColumn(column);

        column = new Column("classificationKey", Messages.ColumnCategoryKey, SWT.RIGHT, 100); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider() // NOSONAR
        {
            @Override
            public String getText(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;

                if (node.isClassification())
                    return node.getClassification().getKey();
                return null;
            }
        });
        new StringEditingSupport(TaxonomyNode.class, "key") //$NON-NLS-1$
                        .setMandatory(false).setCanEditCheck(n -> (((TaxonomyNode) n).isClassification()))
                        .addListener(this).attachTo(column);
        column.setSorter(null);
        column.setVisible(false);
        support.addColumn(column);

        column = new SymbolColumn();
        column.setSorter(null);
        column.setVisible(false);
        support.addColumn(column);

        column = new IsinColumn();
        column.getEditingSupport().addListener(this);
        column.setSorter(null);
        column.setVisible(false);
        support.addColumn(column);

        column = new NoteColumn();
        column.getEditingSupport().addListener(this);
        column.setSorter(null);
        column.setVisible(false);
        support.addColumn(column);

        addWeightColumn(support);
    }

    private void addWeightColumn(ShowHideColumnHelper support) // NOSONAR
    {
        Column column;
        column = new Column("weight", Messages.ColumnWeight, SWT.RIGHT, 70); //$NON-NLS-1$
        column.setDescription(Messages.ColumnWeight_Description);
        column.setLabelProvider(new ColumnLabelProvider() // NOSONAR
        {
            @Override
            public String getText(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                return node.isAssignment() ? Values.Weight.format(node.getWeight()) : null;
            }

            @Override
            public Color getForeground(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                return node.isAssignment() && getModel().hasWeightError(node) ? Colors.BLACK : null;
            }

            @Override
            public Color getBackground(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                return node.isAssignment() && getModel().hasWeightError(node) ? Colors.theme().warningBackground()
                                : null;
            }

            @Override
            public Image getImage(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                return node.isAssignment() && getModel().hasWeightError(node) ? Images.QUICKFIX.image() : null;
            }

        });
        new ValueEditingSupport(TaxonomyNode.class, "weight", Values.Weight) //$NON-NLS-1$
        {
            @Override
            public boolean canEdit(Object element)
            {
                if (((TaxonomyNode) element).isUnassignedCategory())
                    return false;
                if (((TaxonomyNode) element).isClassification())
                    return false;
                return super.canEdit(element);
            }
        }.addListener(this::onWeightModified).attachTo(column);
        support.addColumn(column);
    }

    protected void addActualColumns(ShowHideColumnHelper support)
    {
        Column column = new Column("act%", Messages.ColumnActualPercent, SWT.RIGHT, 60); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                // actual %
                // --> root is compared to target = total assets
                long actual = node.getActual().getAmount();
                long base = node.getParent() == null ? node.getActual().getAmount()
                                : node.getParent().getActual().getAmount();

                if (base == 0)
                    return Values.Percent.format(0d);
                else
                    return Values.Percent.format((double) actual / (double) base);
            }
        });
        support.addColumn(column);

        // Column which shows percentage of this asset class in relationship to
        // total assets
        column = new Column("amGV%", Messages.ColumnPctOfTotal, SWT.RIGHT, 60); //$NON-NLS-1$
        column.setMenuLabel(Messages.ColumnPctOfTotal_MenuLabel);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                // Divide amount in this asset class by amount of total assets
                // (root of asset class tree)
                long actual = node.getActual().getAmount();
                long total = node.getRoot().getActual().getAmount();

                if (total == 0)
                    return Values.Percent.format(0d);
                else
                    return Values.Percent.format((double) actual / (double) total);
            }
        });
        support.addColumn(column);

        column = new Column("act", Messages.ColumnActualValue, SWT.RIGHT, 100); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                return Values.Money.format(node.getActual(), getModel().getCurrencyCode());
            }
        });
        support.addColumn(column);
    }

    protected void addAdditionalColumns(ShowHideColumnHelper support) // NOSONAR
    {
        Column column = new Column("exchangeRate", Messages.ColumnExchangeRate, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.ColumnForeignCurrencies);
        column.setLabelProvider(new ColumnLabelProvider() // NOSONAR
        {
            @Override
            public String getText(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                if (!node.isAssignment())
                    return null;

                String baseCurrency = node.getAssignment().getInvestmentVehicle().getCurrencyCode();
                if (baseCurrency == null)
                    return null;

                CurrencyConverter converter = getModel().getCurrencyConverter();
                ExchangeRate rate = converter.getRate(LocalDate.now(), baseCurrency);

                if (useIndirectQuotation)
                    rate = rate.inverse();

                return Values.ExchangeRate.format(rate.getValue());
            }

            @Override
            public String getToolTipText(Object e)
            {
                String text = getText(e);
                if (text == null)
                    return null;

                String term = getModel().getCurrencyConverter().getTermCurrency();
                String base = ((TaxonomyNode) e).getAssignment().getInvestmentVehicle().getCurrencyCode();

                return text + ' ' + (useIndirectQuotation ? base + '/' + term : term + '/' + base);
            }
        });
        column.setVisible(false);
        support.addColumn(column);

        column = new Column("actBaseCurrency", Messages.ColumnActualValue + Messages.BaseCurrencyCue, SWT.RIGHT, 100); //$NON-NLS-1$
        column.setDescription(Messages.ColumnActualValueBaseCurrency);
        column.setGroupLabel(Messages.ColumnForeignCurrencies);
        column.setLabelProvider(new ColumnLabelProvider() // NOSONAR
        {
            @Override
            public String getText(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;

                if (node.isClassification() || getModel().getCurrencyCode()
                                .equals(node.getAssignment().getInvestmentVehicle().getCurrencyCode()))
                {
                    // if it is a classification
                    // *or* it is an assignment, but currency code matches
                    // then no currency conversion is needed

                    return Values.Money.format(node.getActual(), getModel().getCurrencyCode());
                }
                else if (node.getAssignment().getInvestmentVehicle().getCurrencyCode() != null)
                {
                    // convert into target currency if investment vehicle has a
                    // currency code (e.g. is not an stock market index)
                    return Values.Money.format(
                                    getModel().getCurrencyConverter()
                                                    .with(node.getAssignment().getInvestmentVehicle().getCurrencyCode())
                                                    .convert(LocalDate.now(), node.getActual()),
                                    getModel().getCurrencyCode());
                }
                else
                {
                    return null;
                }
            }
        });
        column.setVisible(false);
        support.addColumn(column);

        getModel().getAttachedModels().forEach(m -> m.addColumns(support));

        AttributeColumn.createFor(getModel().getClient(), Security.class) //
                        .forEach(c -> {
                            c.setSorter(null);
                            c.getEditingSupport().addListener(this);
                            support.addColumn(c);
                        });
    }

    private void expandNodes()
    {
        List<TaxonomyNode> expanded = new ArrayList<>();

        // check if we have expansion state in preferences
        String expansion = readExpansionState();
        if (expansion != null && !expansion.isEmpty())
        {
            Set<String> uuid = new HashSet<>(Arrays.asList(expansion.split(","))); //$NON-NLS-1$
            getModel().visitAll(node -> {
                if (node.isClassification() && uuid.contains(node.getClassification().getId()))
                    expanded.add(node);
            });
        }
        else
        {
            // fall back -> expand all classification nodes with children
            LinkedList<TaxonomyNode> stack = new LinkedList<>();
            stack.push(getModel().getVirtualRootNode());
            while (!stack.isEmpty())
            {
                TaxonomyNode node = stack.pop();
                if (node.isClassification() && !node.getClassification().getChildren().isEmpty())
                {
                    expanded.add(node);
                    stack.addAll(node.getChildren());
                }
            }
        }

        nodeViewer.getTree().setRedraw(false);
        try
        {
            nodeViewer.setExpandedElements(expanded.toArray());
        }
        finally
        {
            nodeViewer.getTree().setRedraw(true);
        }
    }

    protected void onTaxnomyNodeEdited(TaxonomyNode node)
    {
        getModel().recalculate();
        getModel().fireTaxonomyModelChange(node);
        getModel().markDirty();
    }

    @Override
    public void nodeChange(TaxonomyNode node)
    {
        if (!nodeViewer.getTree().isDisposed())
            nodeViewer.refresh();
    }

    @Override
    public void beforePage()
    {
        if (isFirstView)
        {
            expandNodes();
            isFirstView = false;
        }
    }

    @Override
    public void afterPage()
    {
    }

    @Override
    public void dispose()
    {
        // store expansion state to model
        StringJoiner expansionState = new StringJoiner(","); //$NON-NLS-1$
        for (Object element : nodeViewer.getExpandedElements())
        {
            TaxonomyNode node = (TaxonomyNode) element;
            if (!node.isClassification())
                continue;
            expansionState.add(node.getClassification().getId());
        }
        storeExpansionState(expansionState.toString());

        super.dispose();
    }

    protected void fillContextMenu(IMenuManager manager) // NOSONAR
    {
        // do not show context menu if multiple nodes are selected
        IStructuredSelection selection = (IStructuredSelection) nodeViewer.getSelection();
        if (selection.isEmpty() || selection.size() > 1)
            return;

        TaxonomyNode node = (TaxonomyNode) selection.getFirstElement();
        if (node.isUnassignedCategory())
            return;

        // allow inherited views to contribute to the context menu
        manager.add(new Separator(MENU_GROUP_CUSTOM_ACTIONS));

        manager.add(new Separator(MENU_GROUP_DEFAULT_ACTIONS));

        if (node.isClassification())
        {
            manager.add(new SimpleAction(Messages.MenuTaxonomyClassificationCreate, a -> doAddClassification(node)));

            TaxonomyNode unassigned = getModel().getUnassignedNode();
            if (!unassigned.getChildren().isEmpty())
            {
                MenuManager subMenu = new MenuManager(Messages.MenuTaxonomyMakeAssignment);
                addAvailableAssignments(subMenu, node);
                manager.add(subMenu);
            }

            manager.add(new Separator());

            MenuManager sorting = new MenuManager(Messages.MenuTaxonomySortTreeBy);
            sorting.add(new SimpleAction(
                            String.join(", ", Messages.MenuTaxonomySortByType, Messages.MenuTaxonomySortByName), //$NON-NLS-1$
                            a -> doSort(node, SortCriterion.TYPE, SortCriterion.NAME)));
            sorting.add(new SimpleAction(String.join(", ", Messages.MenuTaxonomySortByType, Messages.ColumnActualValue), //$NON-NLS-1$
                            a -> doSort(node, SortCriterion.TYPE, SortCriterion.ACTUAL)));
            sorting.add(new SimpleAction(Messages.MenuTaxonomySortByName, a -> doSort(node, SortCriterion.NAME)));
            sorting.add(new SimpleAction(Messages.ColumnActualValue, a -> doSort(node, SortCriterion.ACTUAL)));

            manager.add(sorting);

            if (!node.isRoot())
            {
                manager.add(new Separator(MENU_GROUP_DELETE_ACTIONS));
                manager.add(new SimpleAction(Messages.MenuTaxonomyClassificationDelete,
                                a -> doDeleteClassification(node)));
            }

            manager.add(new Separator());
            manager.add(new SimpleAction(Messages.LabelExpandAll,
                            a -> nodeViewer.expandToLevel(node, AbstractTreeViewer.ALL_LEVELS)));
            manager.add(new SimpleAction(Messages.LabelCollapseAll,
                            a -> nodeViewer.collapseToLevel(node, AbstractTreeViewer.ALL_LEVELS)));
        }
        else
        {
            // node is assignment, but not in unassigned category
            if (!node.getParent().isUnassignedCategory())
            {
                manager.add(new SimpleAction(Messages.MenuTaxonomyAssignmentRemove, a -> {
                    int oldWeight = node.getWeight();
                    node.setWeight(0);
                    doChangeAssignmentWeight(node, oldWeight);
                    onTaxnomyNodeEdited(getModel().getVirtualRootNode());
                }));
            }

            Security security = node.getBackingSecurity();
            if (security != null)
            {
                manager.add(new Separator());
                new SecurityContextMenu(this.view).menuAboutToShow(manager, security);
            }
        }
    }

    private void addAvailableAssignments(MenuManager manager, TaxonomyNode targetNode)
    {
        for (final TaxonomyNode assignment : getModel().getUnassignedNode().getChildren())
        {
            String label = assignment.getName();

            if (assignment.getWeight() < Classification.ONE_HUNDRED_PERCENT)
                label += " (" + Values.Weight.format(assignment.getWeight()) + "%)"; //$NON-NLS-1$ //$NON-NLS-2$

            manager.add(new Action(label)
            {
                @Override
                public void run()
                {
                    assignment.moveTo(targetNode);
                    onTaxnomyNodeEdited(targetNode);
                    nodeViewer.setExpandedState(targetNode, true);
                }
            });
        }
    }

    private void doAddClassification(TaxonomyNode parent)
    {
        Classification newClassification = new Classification(null, UUID.randomUUID().toString(),
                        Messages.LabelNewClassification);

        TaxonomyNode newNode = parent.addChild(newClassification);

        nodeViewer.setExpandedState(parent, true);
        onTaxnomyNodeEdited(parent);
        nodeViewer.editElement(newNode, 0);
    }

    private void doDeleteClassification(TaxonomyNode node)
    {
        if (node.isRoot() || node.isUnassignedCategory())
            return;

        node.getParent().removeChild(node);

        node.accept(node1 -> {
            if (node1.isAssignment())
                node1.moveTo(getModel().getUnassignedNode());
        });

        onTaxnomyNodeEdited(getModel().getVirtualRootNode());
    }

    private void doChangeAssignmentWeight(TaxonomyNode node, int oldWeight)
    {
        int change = oldWeight - node.getWeight();

        if (change == 0) // was 'fixed' after editing, e.g. was >= 100%
            return;

        // if new weight = 0, then remove assignment

        if (node.getWeight() == 0)
            node.getParent().removeChild(node);

        // change total weight as recorded in the model

        InvestmentVehicle investmentVehicle = node.getAssignment().getInvestmentVehicle();
        final int totalWeight = getModel().getWeightByInvestmentVehicle(investmentVehicle) - change;
        getModel().setWeightByInvestmentVehicle(investmentVehicle, totalWeight);

        // check if change is fixing weight errors -> no new unassigned vehicles

        change = Math.min(change, Classification.ONE_HUNDRED_PERCENT - totalWeight);
        if (change == 0)
            return;

        // change existing unassigned node *or* create new unassigned node

        TaxonomyNode unassigned = getModel().getUnassignedNode().getChildByInvestmentVehicle(investmentVehicle);

        if (unassigned != null)
        {
            // change existing node in unassigned category

            int newWeight = unassigned.getWeight() + change;
            if (newWeight <= 0)
            {
                getModel().getUnassignedNode().removeChild(unassigned);
                getModel().setWeightByInvestmentVehicle(investmentVehicle, totalWeight - unassigned.getWeight());
            }
            else
            {
                unassigned.setWeight(newWeight);
                getModel().setWeightByInvestmentVehicle(investmentVehicle, totalWeight + change);
            }
        }
        else if (change > 0)
        {
            // create a new node, but only if change is positive

            Assignment assignment = new Assignment(investmentVehicle);
            assignment.setWeight(change);
            getModel().getUnassignedNode().addChild(assignment);
            getModel().setWeightByInvestmentVehicle(investmentVehicle, totalWeight + change);
        }

        // do not fire model change -> called within modification listener
    }

    private void doSort(TaxonomyNode node, SortCriterion... criteria) // NOSONAR
    {
        Collections.sort(node.getChildren(), (node1, node2) -> { // NOSONAR
            // unassigned category always stays at the end of the list
            if (node1.isUnassignedCategory())
                return 1;
            if (node2.isUnassignedCategory())
                return -1;

            for (int ii = 0; ii < criteria.length; ii++)
            {
                switch (criteria[ii])
                {
                    case TYPE:
                        if (node1.isClassification() && !node2.isClassification())
                            return -1;
                        if (!node1.isClassification() && node2.isClassification())
                            return 1;
                        break;
                    case NAME:
                        int cn = TextUtil.compare(node1.getName(), node2.getName());
                        if (cn != 0)
                            return cn;
                        break;
                    case ACTUAL:
                        int ca = node2.getActual().compareTo(node1.getActual());
                        if (ca != 0)
                            return ca;
                        break;
                    default:

                }
            }

            return 0;
        });

        int rank = 0;
        for (TaxonomyNode child : node.getChildren())
            child.setRank(rank++);

        getModel().markDirty();
        getModel().fireTaxonomyModelChange(node);
    }
}
