package name.abuchen.portfolio.ui.views.taxonomy;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Values;
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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/* package */class DefinitionViewer
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
        public NodeDropListener(TreeViewer treeViewer)
        {
            super(treeViewer);
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
                    break;
                case ViewerDropAdapter.LOCATION_BEFORE:
                    droppedNode.insertBefore(target);
                    break;
                case ViewerDropAdapter.LOCATION_ON:
                    if (droppedNode != target.getParent())
                        droppedNode.moveTo(target);
                    break;
                case ViewerDropAdapter.LOCATION_NONE:
                    break;
                default:
                    break;
            }

            TreeViewer treeViewer = (TreeViewer) getViewer();
            treeViewer.refresh(target.getParent(), true);
            treeViewer.refresh(droppedParent, true);

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

    private TaxonomyModel model;
    private TreeViewer nodeViewer;

    public DefinitionViewer(TaxonomyModel model)
    {
        this.model = model;
    }

    public Control createContainer(Composite parent, final TaxonomyNodeRenderer renderer)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TreeColumnLayout layout = new TreeColumnLayout();
        container.setLayout(layout);

        nodeViewer = new TreeViewer(container, SWT.FULL_SELECTION);

        TreeViewerColumn column = new TreeViewerColumn(nodeViewer, SWT.NONE);
        column.getColumn().setText("Dimensionen");
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
                    return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
                else if (node.getBackingSecurity() != null)
                    return PortfolioPlugin.image(PortfolioPlugin.IMG_SECURITY);
                else
                    return PortfolioPlugin.image(PortfolioPlugin.IMG_ACCOUNT);
            }
        });

        column = new TreeViewerColumn(nodeViewer, SWT.RIGHT);
        column.getColumn().setText("Weight");
        column.getColumn().setWidth(70);
        layout.setColumnData(column.getColumn(), new ColumnPixelData(70));
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                if (node.isUnassignedCategory())
                    return "n/a";
                else
                    return Values.Weight.format(node.getWeight());
            }

            @Override
            public Color getForeground(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                return node.hasWeightError() ? Display.getDefault().getSystemColor(SWT.COLOR_INFO_FOREGROUND) : null;
            }

            @Override
            public Color getBackground(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                return node.hasWeightError() ? Display.getDefault().getSystemColor(SWT.COLOR_INFO_BACKGROUND) : null;
            }

            @Override
            public Image getImage(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                return node.hasWeightError() ? PortfolioPlugin.image(PortfolioPlugin.IMG_QUICKFIX) : null;
            }
        });

        column = new TreeViewerColumn(nodeViewer, SWT.LEFT);
        column.getColumn().setText("Color");
        column.getColumn().setWidth(60);
        layout.setColumnData(column.getColumn(), new ColumnPixelData(60));
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return null;
            }

            @Override
            public Color getBackground(Object element)
            {
                return renderer.getColorFor((TaxonomyNode) element);
            }
        });

        new CellEditorFactory(nodeViewer, TaxonomyNode.class) //
                        .notify(new CellEditorFactory.ModificationListener()
                        {
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

                            public void onModified(Object element, String property)
                            {
                                // check weights
                                // (must be between >= 0 and <= 100)

                                TaxonomyNode node = (TaxonomyNode) element;

                                if (node.getWeight() > Classification.ONE_HUNDRED_PERCENT)
                                    node.setWeight(Classification.ONE_HUNDRED_PERCENT);
                                else if (node.getWeight() < 0)
                                    node.setWeight(0);

                                onTaxnomyNodeEdited();
                            }
                        }) //
                        .editable("name") // //$NON-NLS-1$
                        .decimal("weight", Values.Weight) // //$NON-NLS-1$
                        .readonly("color") //$NON-NLS-1$
                        .apply();

        nodeViewer.getTree().setHeaderVisible(true);
        nodeViewer.getTree().setLinesVisible(true);
        nodeViewer.setContentProvider(new ItemContentProvider());

        nodeViewer.addDragSupport(DND.DROP_MOVE | DND.DROP_COPY, new Transfer[] { TaxonomyNodeTransfer.getTransfer(),
                        SecurityTransfer.getTransfer() }, new NodeDragListener(nodeViewer));
        nodeViewer.addDropSupport(DND.DROP_MOVE | DND.DROP_COPY, new Transfer[] { TaxonomyNodeTransfer.getTransfer() },
                        new NodeDropListener(nodeViewer));

        nodeViewer.setInput(model);

        expandNodes();

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

    private void expandNodes()
    {
        List<TaxonomyNode> expanded = new ArrayList<TaxonomyNode>();
        LinkedList<TaxonomyNode> stack = new LinkedList<TaxonomyNode>();
        stack.push(model.getRootNode());
        while (!stack.isEmpty())
        {
            TaxonomyNode node = stack.pop();
            if (node.isClassification() && !node.getClassification().getChildren().isEmpty())
            {
                expanded.add(node);
                stack.addAll(node.getChildren());
            }
        }
        nodeViewer.setExpandedElements(expanded.toArray());
    }

    protected void onTaxnomyNodeEdited()
    {
        model.recalculate();
        nodeViewer.refresh(true);
    }

    protected void fillContextMenu(IMenuManager manager)
    {
        final TaxonomyNode node = (TaxonomyNode) ((IStructuredSelection) nodeViewer.getSelection()).getFirstElement();
        if (node == null)
            return;

        if (node.isUnassignedCategory())
        {
            // no actions on unassigned category
        }
        else if (node.isClassification())
        {
            if (node.hasWeightError())
            {
                manager.add(new Action("Fix weights")
                {
                    @Override
                    public void run()
                    {
                        doFixClassificationWeights(node);
                    }
                });
            }

            manager.add(new Action("Add new classification")
            {
                @Override
                public void run()
                {
                    doAddClassification(node);
                }
            });

            TaxonomyNode unassigned = model.getUnassignedNode();
            if (!unassigned.getChildren().isEmpty())
            {
                MenuManager subMenu = new MenuManager("Assign");
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
                            onTaxnomyNodeEdited();
                        }
                    });
                }
                manager.add(subMenu);
            }

            if (!node.isRoot())
            {
                manager.add(new Separator());
                manager.add(new Action("Delete")
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
                manager.add(new Action("Remove")
                {
                    @Override
                    public void run()
                    {
                        node.moveTo(model.getUnassignedNode());
                        nodeViewer.setExpandedState(node, true);
                        onTaxnomyNodeEdited();
                    }
                });
            }
        }
    }

    private void doFixClassificationWeights(TaxonomyNode node)
    {
        Classification classification = node.getClassification();

        if (node.isRoot())
        {
            classification.setWeight(Classification.ONE_HUNDRED_PERCENT);
        }
        else
        {
            classification.setWeight(0);
            int weight = Math.max(0, Classification.ONE_HUNDRED_PERCENT
                            - classification.getParent().getChildrenWeight());
            classification.setWeight(weight);
        }
        onTaxnomyNodeEdited();
    }

    private void doAddClassification(TaxonomyNode parent)
    {
        Classification newClassification = new Classification(null, UUID.randomUUID().toString(), "NEW CLASSIFICATION");

        TaxonomyNode newNode = parent.addChild(newClassification);

        nodeViewer.setExpandedState(parent, true);
        onTaxnomyNodeEdited();
        nodeViewer.editElement(newNode, 0);
    }

    private void doDeleteClassification(TaxonomyNode node)
    {
        if (node.isRoot() || node.isUnassignedCategory())
            return;

        node.getParent().removeChild(node);

        new TaxonomyModel.NodeVisitor()
        {
            @Override
            public void visit(TaxonomyNode node)
            {
                if (node.isAssignment())
                    node.moveTo(model.getUnassignedNode());
            }

        }.startWith(node);

        onTaxnomyNodeEdited();
    }
}
