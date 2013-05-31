package name.abuchen.portfolio.ui.views.taxonomy;

import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.ColumnViewerSorter;

import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/* package */class DefinitionViewer
{
    private static class ItemContentProvider implements ITreeContentProvider
    {
        private TaxonomyNode root;

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
        {
            root = (TaxonomyNode) newInput;
        }

        @Override
        public Object[] getElements(Object inputElement)
        {
            return root.getChildren().toArray();
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

    private TaxonomyNode model;

    public DefinitionViewer(TaxonomyNode model)
    {
        this.model = model;
    }

    public Control createContainer(Composite parent, final TaxonomyNodeRenderer renderer)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TreeColumnLayout layout = new TreeColumnLayout();
        container.setLayout(layout);

        TreeViewer viewer = new TreeViewer(container, SWT.FULL_SELECTION);

        TreeViewerColumn column = new TreeViewerColumn(viewer, SWT.NONE);
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
        ColumnViewerSorter.create(TaxonomyNode.class, "name").attachTo(viewer, column); //$NON-NLS-1$

        column = new TreeViewerColumn(viewer, SWT.NONE);
        column.getColumn().setText("Id");
        column.getColumn().setWidth(100);
        layout.setColumnData(column.getColumn(), new ColumnPixelData(100));
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((TaxonomyNode) element).getId();
            }
        });
        ColumnViewerSorter.create(TaxonomyNode.class, "id").attachTo(viewer, column); //$NON-NLS-1$

        column = new TreeViewerColumn(viewer, SWT.RIGHT);
        column.getColumn().setText("Weight");
        column.getColumn().setWidth(60);
        layout.setColumnData(column.getColumn(), new ColumnPixelData(60));
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                return Values.Weight.format(node.getWeight());
            }
        });
        ColumnViewerSorter.create(TaxonomyNode.class, "weight").attachTo(viewer, column, true); //$NON-NLS-1$

        column = new TreeViewerColumn(viewer, SWT.LEFT);
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
        ColumnViewerSorter.create(TaxonomyNode.class, "color").attachTo(viewer, column, true); //$NON-NLS-1$

        viewer.getTree().setHeaderVisible(true);
        viewer.getTree().setLinesVisible(true);
        viewer.setContentProvider(new ItemContentProvider());
        viewer.setInput(model);

        return container;
    }
}
