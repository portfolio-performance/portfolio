package name.abuchen.portfolio.ui.views.taxonomy;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.CellEditorFactory;
import name.abuchen.portfolio.ui.util.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.ViewerHelper;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
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

                if (node.getClassification() != null)
                    return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
                else if (node.getBackingSecurity() != null)
                    return PortfolioPlugin.image(PortfolioPlugin.IMG_SECURITY);
                else
                    return PortfolioPlugin.image(PortfolioPlugin.IMG_ACCOUNT);
            }
        });
        ColumnViewerSorter.create(TaxonomyNode.class, "name").attachTo(nodeViewer, column); //$NON-NLS-1$

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
        ColumnViewerSorter.create(TaxonomyNode.class, "weight").attachTo(nodeViewer, column, true); //$NON-NLS-1$

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
        ColumnViewerSorter.create(TaxonomyNode.class, "color").attachTo(nodeViewer, column, true); //$NON-NLS-1$

        new CellEditorFactory(nodeViewer, TaxonomyNode.class) //
                        .notify(new CellEditorFactory.ModificationListener()
                        {
                            public void onModified(Object element, String property)
                            {
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

        if (node.isClassification())
        {
            final Classification classification = node.getClassification();

            if (node.hasWeightError())
            {
                manager.add(new Action("Fix weights")
                {
                    @Override
                    public void run()
                    {
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
                });
            }

            manager.add(new Action("Add new classification")
            {
                @Override
                public void run()
                {
                    Classification newClassification = new Classification(classification, UUID.randomUUID().toString(),
                                    "NEW CLASSIFICATION");
                    newClassification.setWeight(Classification.ONE_HUNDRED_PERCENT - classification.getChildrenWeight());
                    classification.addChild(newClassification);

                    TaxonomyNode newNode = node.addChild(newClassification);

                    nodeViewer.setExpandedState(node, true);
                    onTaxnomyNodeEdited();
                    nodeViewer.editElement(newNode, 0);
                }
            });

            if (!node.isRoot())
            {
                manager.add(new Action("Delete")
                {
                    @Override
                    public void run()
                    {
                        node.getParent().removeChild(node);
                        onTaxnomyNodeEdited();
                    }
                });
            }
        }
    }
}
