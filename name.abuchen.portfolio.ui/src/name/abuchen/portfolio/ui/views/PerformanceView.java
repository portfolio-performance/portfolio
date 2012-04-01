package name.abuchen.portfolio.ui.views;

import java.util.Calendar;

import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.ViewerHelper;
import name.abuchen.portfolio.util.Dates;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TreeColumn;

public class PerformanceView extends AbstractHistoricView
{
    private TreeViewer calculation;
    private TreeViewer snapshotStart;
    private TreeViewer snapshotEnd;

    public PerformanceView()
    {
        super(5, 0);
    }

    @Override
    protected String getTitle()
    {
        return Messages.LabelPerformanceCalculation;
    }

    @Override
    protected void reportingPeriodUpdated()
    {
        Calendar startDate = Calendar.getInstance();
        startDate.setTime(Dates.today());
        startDate.add(Calendar.YEAR, -getReportingYears());
        startDate.set(Calendar.DAY_OF_MONTH, 1);
        startDate.add(Calendar.DATE, -1);

        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(getClient(), startDate.getTime(),
                        Dates.today());

        calculation.setInput(snapshot);
        calculation.refresh();
        calculation.expandAll();
        ViewerHelper.pack(calculation);

        snapshotStart.setInput(snapshot.getStartClientSnapshot());
        snapshotStart.refresh();
        snapshotStart.expandAll();
        ViewerHelper.pack(snapshotStart);

        snapshotEnd.setInput(snapshot.getEndClientSnapshot());
        snapshotEnd.refresh();
        snapshotEnd.expandAll();
        ViewerHelper.pack(snapshotEnd);
    }

    @Override
    protected Control buildBody(Composite parent)
    {
        // result tabs
        CTabFolder folder = new CTabFolder(parent, SWT.BORDER);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(folder);

        calculation = createCalculationItem(folder, Messages.PerformanceTabCalculation);
        snapshotStart = createStatementOfAssetsItem(folder, Messages.PerformanceTabAssetsAtStart);
        snapshotEnd = createStatementOfAssetsItem(folder, Messages.PerformanceTabAssetsAtEnd);

        folder.setSelection(0);

        reportingPeriodUpdated();

        return folder;
    }

    private TreeViewer createStatementOfAssetsItem(CTabFolder folder, String title)
    {
        CTabItem item = new CTabItem(folder, SWT.NONE);
        item.setText(title);
        TreeViewer viewer = StatementOfAssetsView.createAssetsViewer(folder);
        item.setControl(viewer.getTree());

        return viewer;
    }

    private TreeViewer createCalculationItem(CTabFolder folder, String title)
    {
        CTabItem item = new CTabItem(folder, SWT.NONE);
        item.setText(title);

        TreeViewer viewer = new TreeViewer(folder, SWT.FULL_SELECTION);

        TreeColumn column = new TreeColumn(viewer.getTree(), SWT.None);
        column.setText(Messages.ColumnLable);
        column.setWidth(350);

        column = new TreeColumn(viewer.getTree(), SWT.None);
        column.setText(Messages.ColumnValue);
        column.setAlignment(SWT.RIGHT);
        column.setWidth(80);

        viewer.getTree().setHeaderVisible(true);
        viewer.getTree().setLinesVisible(true);

        viewer.setLabelProvider(new PerformanceLabelProvider());
        viewer.setContentProvider(new PerformanceContentProvider());

        item.setControl(viewer.getTree());

        return viewer;
    }

    private static class PerformanceContentProvider implements ITreeContentProvider
    {
        private ClientPerformanceSnapshot.Category[] categories;

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
        {
            if (newInput == null)
            {
                this.categories = new ClientPerformanceSnapshot.Category[0];
            }
            else if (newInput instanceof ClientPerformanceSnapshot)
            {
                this.categories = ((ClientPerformanceSnapshot) newInput).getCategories().toArray(
                                new ClientPerformanceSnapshot.Category[0]);
            }
            else
            {
                throw new RuntimeException("Unsupported type: " + newInput.getClass().getName()); //$NON-NLS-1$
            }
        }

        public Object[] getElements(Object inputElement)
        {
            return this.categories;
        }

        public Object[] getChildren(Object parentElement)
        {
            if (parentElement instanceof ClientPerformanceSnapshot.Category)
                return ((ClientPerformanceSnapshot.Category) parentElement).getPositions().toArray(
                                new ClientPerformanceSnapshot.Position[0]);
            return null;
        }

        public Object getParent(Object element)
        {
            if (element instanceof ClientPerformanceSnapshot.Position)
            {
                for (ClientPerformanceSnapshot.Category c : categories)
                {
                    if (c.getPositions().contains(element))
                        return c;
                }

            }
            return null;
        }

        public boolean hasChildren(Object element)
        {
            return element instanceof ClientPerformanceSnapshot.Category
                            && !((ClientPerformanceSnapshot.Category) element).getPositions().isEmpty();
        }

        public void dispose()
        {}

    }

    private static class PerformanceLabelProvider extends LabelProvider implements ITableLabelProvider,
                    ITableFontProvider
    {
        FontRegistry registry = new FontRegistry();

        public Image getColumnImage(Object element, int columnIndex)
        {
            return null;
        }

        public String getColumnText(Object element, int columnIndex)
        {
            if (element instanceof ClientPerformanceSnapshot.Category)
            {
                ClientPerformanceSnapshot.Category cat = (ClientPerformanceSnapshot.Category) element;

                switch (columnIndex)
                {
                    case 0:
                        return cat.getLabel();
                    case 1:
                        return String.format("%,10.2f", cat.getValuation() / 100d); //$NON-NLS-1$
                }
            }
            else if (element instanceof ClientPerformanceSnapshot.Position)
            {
                ClientPerformanceSnapshot.Position pos = (ClientPerformanceSnapshot.Position) element;

                switch (columnIndex)
                {
                    case 0:
                        return pos.getLabel();
                    case 1:
                        return String.format("%,10.2f", pos.getValuation() / 100d); //$NON-NLS-1$
                }
            }
            return null;
        }

        @Override
        public Font getFont(Object element, int columnIndex)
        {
            if (element instanceof ClientPerformanceSnapshot.Category)
            {
                return registry.getBold(Display.getCurrent().getSystemFont().getFontData()[0].getName());
            }
            else
            {
                return null;
            }
        }
    }

}
