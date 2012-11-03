package name.abuchen.portfolio.ui.views;

import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.ui.Messages;
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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

class IndustryClassificationTreeViewer
{
    private Composite container;
    private TreeViewer viewer;

    public IndustryClassificationTreeViewer(Composite parent, int style)
    {
        container = new Composite(parent, style);
        TreeColumnLayout layout = new TreeColumnLayout();
        container.setLayout(layout);

        viewer = new TreeViewer(container);

        TreeViewerColumn column = new TreeViewerColumn(viewer, SWT.NONE);
        column.getColumn().setText(Messages.ShortLabelIndustry);
        column.getColumn().setWidth(400);
        layout.setColumnData(column.getColumn(), new ColumnPixelData(400));
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((TreeMapItem) element).getLabel();
            }

            @Override
            public Image getImage(Object element)
            {
                TreeMapItem node = (TreeMapItem) element;

                if (node.isCategory())
                    return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
                else if (node.isSecurity())
                    return PortfolioPlugin.image(PortfolioPlugin.IMG_SECURITY);
                else if (node.isAccount())
                    return PortfolioPlugin.image(PortfolioPlugin.IMG_ACCOUNT);
                else
                    return null;
            }
        });
        ColumnViewerSorter.create(TreeMapItem.class, "label").attachTo(viewer, column); //$NON-NLS-1$

        column = new TreeViewerColumn(viewer, SWT.RIGHT);
        column.getColumn().setText(Messages.ColumnActualPercent);
        column.getColumn().setWidth(60);
        layout.setColumnData(column.getColumn(), new ColumnPixelData(60));
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                double percentage = ((TreeMapItem) element).getPercentage();
                return String.format("%,10.1f", percentage * 100d); //$NON-NLS-1$
            }
        });
        ColumnViewerSorter.create(TreeMapItem.class, "percentage").attachTo(viewer, column, true); //$NON-NLS-1$

        column = new TreeViewerColumn(viewer, SWT.RIGHT);
        column.getColumn().setText(Messages.ColumnActualValue);
        column.getColumn().setWidth(100);
        layout.setColumnData(column.getColumn(), new ColumnPixelData(100));
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                long valuation = ((TreeMapItem) element).getValuation();
                return Values.Amount.format(valuation);
            }
        });
        ColumnViewerSorter.create(TreeMapItem.class, "valuation").attachTo(viewer, column); //$NON-NLS-1$

        viewer.getTree().setHeaderVisible(true);
        viewer.getTree().setLinesVisible(true);
        viewer.setContentProvider(new ItemContentProvider());
    }

    public void setInput(TreeMapItem rootItem)
    {
        viewer.setInput(rootItem);
    }

    public Control getControl()
    {
        return container;
    }
    
    public TreeViewer getTreeViewer()
    {
        return viewer;
    }

    private static class ItemContentProvider implements ITreeContentProvider
    {
        private TreeMapItem root;

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
        {
            root = (TreeMapItem) newInput;
        }

        @Override
        public Object[] getElements(Object inputElement)
        {
            return root.getChildren().toArray();
        }

        @Override
        public boolean hasChildren(Object element)
        {
            return !((TreeMapItem) element).getChildren().isEmpty();
        }

        @Override
        public Object[] getChildren(Object parentElement)
        {
            return ((TreeMapItem) parentElement).getChildren().toArray();
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

}
