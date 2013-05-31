package name.abuchen.portfolio.ui.views.taxonomy;

import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.ColumnViewerSorter;

import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.resource.LocalResourceManager;
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
        private Classification root;

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
        {
            root = (Classification) newInput;
        }

        @Override
        public Object[] getElements(Object inputElement)
        {
            return root.getChildren().toArray();
        }

        @Override
        public boolean hasChildren(Object element)
        {
            if (element instanceof Assignment)
                return false;

            Classification classification = (Classification) element;
            return !classification.getChildren().isEmpty() || !classification.getAssignments().isEmpty();
        }

        @Override
        public Object[] getChildren(Object parentElement)
        {
            Classification classification = (Classification) parentElement;

            List<Object> children = new ArrayList<Object>();
            children.addAll(classification.getChildren());
            children.addAll(classification.getAssignments());
            return children.toArray();
        }

        @Override
        public Object getParent(Object element)
        {
            return null;
        }

        @Override
        public void dispose()
        {}
    }

    private Taxonomy taxonomy;

    public DefinitionViewer(Taxonomy taxonomy)
    {
        this.taxonomy = taxonomy;
    }

    public Control createContainer(Composite parent, final LocalResourceManager resources)
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
                if (element instanceof Classification)
                    return ((Classification) element).getName();
                else if (element instanceof Assignment)
                    return ((Assignment) element).getInvestmentVehicle().toString();
                else
                    return null;
            }

            @Override
            public Image getImage(Object element)
            {
                if (element instanceof Classification)
                    return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
                else if (element instanceof Assignment
                                && ((Assignment) element).getInvestmentVehicle() instanceof Security)
                    return PortfolioPlugin.image(PortfolioPlugin.IMG_SECURITY);
                else if (element instanceof Assignment
                                && ((Assignment) element).getInvestmentVehicle() instanceof Account)
                    return PortfolioPlugin.image(PortfolioPlugin.IMG_ACCOUNT);
                else
                    return null;
            }
        });
        ColumnViewerSorter.create(Classification.class, "name").attachTo(viewer, column); //$NON-NLS-1$ // FIXME sorting

        column = new TreeViewerColumn(viewer, SWT.NONE);
        column.getColumn().setText("Id");
        column.getColumn().setWidth(100);
        layout.setColumnData(column.getColumn(), new ColumnPixelData(100));
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                if (element instanceof Classification)
                    return ((Classification) element).getId();
                else
                    return null;
            }
        });
        ColumnViewerSorter.create(Classification.class, "id").attachTo(viewer, column); //$NON-NLS-1$ // FIXME sorting

        column = new TreeViewerColumn(viewer, SWT.RIGHT);
        column.getColumn().setText("Weight");
        column.getColumn().setWidth(60);
        layout.setColumnData(column.getColumn(), new ColumnPixelData(60));
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                if (element instanceof Classification)
                    return Values.Weight.format(((Classification) element).getWeight());
                else if (element instanceof Assignment)
                    return Values.Weight.format(((Assignment) element).getWeight());
                return null;
            }
        });
        ColumnViewerSorter.create(Classification.class, "weight").attachTo(viewer, column, true); //$NON-NLS-1$ // FIXME sorting

        column = new TreeViewerColumn(viewer, SWT.LEFT);
        column.getColumn().setText("Color");
        column.getColumn().setWidth(60);
        layout.setColumnData(column.getColumn(), new ColumnPixelData(60));
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return "";
            }

            @Override
            public Color getBackground(Object element)
            {
                if (element instanceof Classification)
                    return resources.createColor(Colors.toRGB(((Classification) element).getColor()));
                else
                    return null;
            }
        });
        ColumnViewerSorter.create(Classification.class, "color").attachTo(viewer, column, true); //$NON-NLS-1$ // FIXME sorting

        viewer.getTree().setHeaderVisible(true);
        viewer.getTree().setLinesVisible(true);
        viewer.setContentProvider(new ItemContentProvider());
        viewer.setInput(taxonomy.getRoot());

        return container;
    }
}