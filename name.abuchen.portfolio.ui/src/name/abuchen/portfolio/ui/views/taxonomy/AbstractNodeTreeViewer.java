package name.abuchen.portfolio.ui.views.taxonomy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.dnd.SecurityTransfer;
import name.abuchen.portfolio.ui.util.CellEditorFactory;
import name.abuchen.portfolio.ui.util.ViewerHelper;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/* package */abstract class AbstractNodeTreeViewer extends Page
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
            return new Object[] { model.getRootNode() };
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
        {}
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
            TaxonomyNode selection = (TaxonomyNode) ((TreeSelection) treeViewer.getSelection()).getFirstElement();
            TaxonomyNodeTransfer.getTransfer().setTaxonomyNode(selection);
            Assignment assignment = selection.getAssignment();
            if (assignment != null && assignment.getInvestmentVehicle() instanceof Security)
                SecurityTransfer.getTransfer().setSecurity((Security) assignment.getInvestmentVehicle());
            else
                SecurityTransfer.getTransfer().setSecurity(null);

            event.data = selection;
        }

        @Override
        public void dragStart(DragSourceEvent event)
        {
            TaxonomyNode selection = (TaxonomyNode) ((TreeSelection) treeViewer.getSelection()).getFirstElement();
            event.doit = !selection.isRoot() && !selection.isUnassignedCategory();
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
        public boolean performDrop(Object data)
        {
            TaxonomyNode droppedNode = TaxonomyNodeTransfer.getTransfer().getTaxonomyNode();
            if (droppedNode == getCurrentTarget())
                return false;

            TaxonomyNode target = (TaxonomyNode) getCurrentTarget();

            if (target.isUnassignedCategory() && droppedNode.isClassification())
                return false;

            // save parent in order to refresh later
            TaxonomyNode droppedParent = droppedNode.getParent();

            switch (getCurrentLocation())
            {
                case ViewerDropAdapter.LOCATION_AFTER:
                    droppedNode.insertAfter(target);
                    viewer.onTaxnomyNodeEdited(droppedParent);
                    break;
                case ViewerDropAdapter.LOCATION_BEFORE:
                    droppedNode.insertBefore(target);
                    viewer.onTaxnomyNodeEdited(droppedParent);
                    break;
                case ViewerDropAdapter.LOCATION_ON:
                    if (droppedNode != target.getParent())
                    {
                        droppedNode.moveTo(target);
                        viewer.onTaxnomyNodeEdited(droppedParent);
                    }
                    break;
                case ViewerDropAdapter.LOCATION_NONE:
                    break;
                default:
                    break;
            }

            return true;
        }

        @Override
        public boolean validateDrop(Object target, int operation, TransferData transferType)
        {
            if (!(target instanceof TaxonomyNode))
                return false;

            TaxonomyNode targetNode = (TaxonomyNode) target;

            int location = determineLocation(this.getCurrentEvent());

            if (targetNode.isClassification())
                return true;
            else if (targetNode.isAssignment())
                return location == LOCATION_AFTER || location == LOCATION_BEFORE;
            else
                return false;
        }
    }

    protected static class NodeModificationListener extends CellEditorFactory.ModificationListener
    {
        private AbstractNodeTreeViewer viewer;

        public NodeModificationListener(AbstractNodeTreeViewer viewer)
        {
            this.viewer = viewer;
        }

        @Override
        public boolean canModify(Object element, String property)
        {
            TaxonomyNode node = (TaxonomyNode) element;

            if (node.isUnassignedCategory())
                return false;

            if ("name".equals(property) && node.isAssignment()) //$NON-NLS-1$
                return false;

            return true;
        }

        @Override
        public void onModified(Object element, String property, Object oldValue)
        {
            if ("weight".equals(property)) //$NON-NLS-1$
            {
                TaxonomyNode node = (TaxonomyNode) element;

                if (node.getWeight() > Classification.ONE_HUNDRED_PERCENT)
                    node.setWeight(Classification.ONE_HUNDRED_PERCENT);
                else if (node.getWeight() < 0)
                    node.setWeight(0);

                if (node.isAssignment())
                {
                    int oldWeight = (Integer) oldValue;
                    viewer.doChangeAssignmentWeight(node, oldWeight);
                }
            }

            viewer.onTaxnomyNodeEdited((TaxonomyNode) element);
        }
    }

    protected static final String MENU_GROUP_DEFAULT_ACTIONS = "defaultActions"; //$NON-NLS-1$
    protected static final String MENU_GROUP_CUSTOM_ACTIONS = "customActions"; //$NON-NLS-1$
    protected static final String MENU_GROUP_DELETE_ACTIONS = "deleteActions"; //$NON-NLS-1$

    private TreeViewer nodeViewer;

    private boolean isFirstView = true;

    public AbstractNodeTreeViewer(TaxonomyModel model, TaxonomyNodeRenderer renderer)
    {
        super(model, renderer);
    }

    protected final TreeViewer getNodeViewer()
    {
        return nodeViewer;
    }

    public final Control createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TreeColumnLayout layout = new TreeColumnLayout();
        container.setLayout(layout);

        nodeViewer = new TreeViewer(container, SWT.FULL_SELECTION);

        addColumns(layout);

        nodeViewer.getTree().setHeaderVisible(true);
        nodeViewer.getTree().setLinesVisible(true);
        nodeViewer.setContentProvider(new ItemContentProvider());

        nodeViewer.addDragSupport(DND.DROP_MOVE | DND.DROP_COPY, new Transfer[] { TaxonomyNodeTransfer.getTransfer(),
                        SecurityTransfer.getTransfer() }, new NodeDragListener(nodeViewer));
        nodeViewer.addDropSupport(DND.DROP_MOVE | DND.DROP_COPY, new Transfer[] { TaxonomyNodeTransfer.getTransfer() },
                        new NodeDropListener(this));

        nodeViewer.setInput(getModel());

        ViewerHelper.pack(nodeViewer);

        ViewerHelper.attachContextMenu(nodeViewer, new IMenuListener()
        {
            @Override
            public void menuAboutToShow(IMenuManager manager)
            {
                fillContextMenu(manager);
            }
        });

        return container;
    }

    protected abstract void addColumns(TreeColumnLayout layout);

    protected void addDimensionColumn(TreeColumnLayout layout)
    {
        TreeViewerColumn column = new TreeViewerColumn(getNodeViewer(), SWT.NONE);
        column.getColumn().setText(Messages.ColumnLevels);
        column.getColumn().setWidth(400);
        layout.setColumnData(column.getColumn(), new ColumnPixelData(400));
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((TaxonomyNode) element).getName();
            }

            @Override
            public Image getImage(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;

                if (node.isUnassignedCategory())
                    return PortfolioPlugin.image(PortfolioPlugin.IMG_UNASSIGNED_CATEGORY);
                else if (node.getClassification() != null)
                    return PortfolioPlugin.image(PortfolioPlugin.IMG_CATEGORY);
                else if (node.getBackingSecurity() != null)
                    return PortfolioPlugin.image(PortfolioPlugin.IMG_SECURITY);
                else
                    return PortfolioPlugin.image(PortfolioPlugin.IMG_ACCOUNT);
            }
        });
    }

    protected void addActualColumns(TreeColumnLayout layout)
    {
        TreeViewerColumn column;
        column = new TreeViewerColumn(getNodeViewer(), SWT.RIGHT);
        column.getColumn().setText(Messages.ColumnActualPercent);
        layout.setColumnData(column.getColumn(), new ColumnPixelData(60));
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                // actual %
                // --> root is compared to target = total assets
                long actual = node.getActual();
                long base = node.getParent() == null ? node.getActual() : node.getParent().getActual();

                if (base == 0d)
                    return Values.Percent.format(0d);
                else
                    return Values.Percent.format(((double) actual / (double) base));
            }
        });

        column = new TreeViewerColumn(getNodeViewer(), SWT.RIGHT);
        column.getColumn().setText(Messages.ColumnActualValue);
        layout.setColumnData(column.getColumn(), new ColumnPixelData(100));
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                return Values.Amount.format(node.getActual());
            }
        });
    }

    private void expandNodes()
    {
        List<TaxonomyNode> expanded = new ArrayList<TaxonomyNode>();
        LinkedList<TaxonomyNode> stack = new LinkedList<TaxonomyNode>();
        stack.push(getModel().getRootNode());
        while (!stack.isEmpty())
        {
            TaxonomyNode node = stack.pop();
            if (node.isClassification() && !node.getClassification().getChildren().isEmpty())
            {
                expanded.add(node);
                stack.addAll(node.getChildren());
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
    }

    @Override
    public void nodeChange(TaxonomyNode node)
    {
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
    {}

    protected void fillContextMenu(IMenuManager manager)
    {
        final TaxonomyNode node = (TaxonomyNode) ((IStructuredSelection) nodeViewer.getSelection()).getFirstElement();
        if (node == null)
            return;

        if (node.isUnassignedCategory())
            return;

        // allow inherited views to contribute to the context menu
        manager.add(new Separator(MENU_GROUP_CUSTOM_ACTIONS));

        manager.add(new Separator(MENU_GROUP_DEFAULT_ACTIONS));

        if (node.isClassification())
        {
            manager.add(new Action(Messages.MenuTaxonomyClassificationCreate)
            {
                @Override
                public void run()
                {
                    doAddClassification(node);
                }
            });

            TaxonomyNode unassigned = getModel().getUnassignedNode();
            if (!unassigned.getChildren().isEmpty())
            {
                MenuManager subMenu = new MenuManager(Messages.MenuTaxonomyMakeAssignment);
                for (final TaxonomyNode assignment : unassigned.getChildren())
                {
                    String label = assignment.getName();

                    if (assignment.getWeight() < Classification.ONE_HUNDRED_PERCENT)
                        label += " (" + Values.Weight.format(assignment.getWeight()) + "%)"; //$NON-NLS-1$ //$NON-NLS-2$

                    subMenu.add(new Action(label)
                    {
                        @Override
                        public void run()
                        {
                            assignment.moveTo(node);
                            nodeViewer.setExpandedState(node, true);
                            onTaxnomyNodeEdited(node);
                        }
                    });
                }
                manager.add(subMenu);
            }

            manager.add(new Separator());

            MenuManager sorting = new MenuManager(Messages.MenuTaxonomySortTreeBy);
            sorting.add(new Action(Messages.MenuTaxonomySortByTypName)
            {
                @Override
                public void run()
                {
                    doSort(node, true);
                }
            });
            sorting.add(new Action(Messages.MenuTaxonomySortByName)
            {
                @Override
                public void run()
                {
                    doSort(node, false);
                }
            });

            manager.add(sorting);

            if (!node.isRoot())
            {
                manager.add(new Separator(MENU_GROUP_DELETE_ACTIONS));
                manager.add(new Action(Messages.MenuTaxonomyClassificationDelete)
                {
                    @Override
                    public void run()
                    {
                        doDeleteClassification(node);
                    }
                });
            }
        }
        else
        {
            // node is assignment, but not in unassigned category
            if (!node.getParent().isUnassignedCategory())
            {
                manager.add(new Action(Messages.MenuTaxonomyAssignmentRemove)
                {
                    @Override
                    public void run()
                    {
                        int oldWeight = node.getWeight();
                        node.setWeight(0);
                        doChangeAssignmentWeight(node, oldWeight);
                        onTaxnomyNodeEdited(getModel().getRootNode());
                    }
                });
            }
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

        node.accept(new TaxonomyModel.NodeVisitor()
        {
            @Override
            public void visit(TaxonomyNode node)
            {
                if (node.isAssignment())
                    node.moveTo(getModel().getUnassignedNode());
            }
        });

        onTaxnomyNodeEdited(getModel().getRootNode());
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

    private void doSort(TaxonomyNode node, final boolean byType)
    {
        Collections.sort(node.getChildren(), new Comparator<TaxonomyNode>()
        {
            @Override
            public int compare(TaxonomyNode node1, TaxonomyNode node2)
            {
                // unassigned category always stays at the end of the list
                if (node1.isUnassignedCategory())
                    return 1;
                if (node2.isUnassignedCategory())
                    return -1;

                if (byType && node1.isClassification() && !node2.isClassification())
                    return -1;
                if (byType && !node1.isClassification() && node2.isClassification())
                    return 1;

                return node1.getName().compareToIgnoreCase(node2.getName());
            }
        });

        int rank = 0;
        for (TaxonomyNode child : node.getChildren())
            child.setRank(rank++);

        getModel().fireTaxonomyModelChange(node);
    }
}
