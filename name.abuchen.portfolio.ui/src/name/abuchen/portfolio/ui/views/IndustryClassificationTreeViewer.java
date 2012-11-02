package name.abuchen.portfolio.ui.views;

import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.ColumnViewerSorter;
import name.abuchen.portfolio.ui.views.IndustryClassificationView.Item;

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
                Item node = (Item) element;

                if (node.isCategory())
                    return node.getCategory().getLabel();
                else if (node.isSecurity())
                    return node.getSecurity().getName();
                else if (node.isAccount())
                    return node.getAccount().getName();
                else
                    return null;
            }

            @Override
            public Image getImage(Object element)
            {
                Item node = (Item) element;

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
        ColumnViewerSorter.create(Item.class, "label").attachTo(viewer, column); //$NON-NLS-1$

        column = new TreeViewerColumn(viewer, SWT.RIGHT);
        column.getColumn().setText(Messages.ColumnActualPercent);
        column.getColumn().setWidth(60);
        layout.setColumnData(column.getColumn(), new ColumnPixelData(60));
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                double percentage = ((Item) element).getPercentage();
                return String.format("%,10.1f", percentage * 100d); //$NON-NLS-1$
            }
        });
        ColumnViewerSorter.create(Item.class, "percentage").attachTo(viewer, column, true); //$NON-NLS-1$

        column = new TreeViewerColumn(viewer, SWT.RIGHT);
        column.getColumn().setText(Messages.ColumnActualValue);
        column.getColumn().setWidth(100);
        layout.setColumnData(column.getColumn(), new ColumnPixelData(100));
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                long valuation = ((Item) element).getValuation();
                return Values.Amount.format(valuation);
            }
        });
        ColumnViewerSorter.create(Item.class, "valuation").attachTo(viewer, column); //$NON-NLS-1$

        viewer.getTree().setHeaderVisible(true);
        viewer.getTree().setLinesVisible(true);
        viewer.setContentProvider(new ItemContentProvider());
    }

    public void setInput(Item rootItem)
    {
        viewer.setInput(rootItem);
    }

    public Control getControl()
    {
        return container;
    }

    private static class ItemContentProvider implements ITreeContentProvider
    {
        private Item root;

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
        {
            root = (Item) newInput;
        }

        @Override
        public Object[] getElements(Object inputElement)
        {
            return root.getChildren().toArray();
        }

        @Override
        public boolean hasChildren(Object element)
        {
            return !((Item) element).getChildren().isEmpty();
        }

        @Override
        public Object[] getChildren(Object parentElement)
        {
            return ((Item) parentElement).getChildren().toArray();
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
