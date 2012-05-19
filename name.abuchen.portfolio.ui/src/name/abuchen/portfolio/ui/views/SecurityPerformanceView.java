package name.abuchen.portfolio.ui.views;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.snapshot.SecurityPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.SecurityPerformanceSnapshot.Record;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.ViewerHelper;
import name.abuchen.portfolio.util.Dates;

import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TreeColumn;

public class SecurityPerformanceView extends AbstractHistoricView
{
    private TreeViewer tree;

    public SecurityPerformanceView()
    {
        super(5, 0);
    }

    @Override
    protected String getTitle()
    {
        return Messages.LabelSecurityPerformance;
    }

    @Override
    protected Control buildBody(Composite parent)
    {
        tree = createTreeViewer(parent);

        reportingPeriodUpdated();
        ViewerHelper.pack(tree);

        return tree.getControl();
    }

    @Override
    protected void reportingPeriodUpdated()
    {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -getReportingYears());
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.add(Calendar.DATE, -1);

        Date startDate = cal.getTime();
        Date today = Dates.today();

        tree.setInput(SecurityPerformanceSnapshot.create(getClient(), startDate, today).getRecords());
        tree.refresh();
    }

    private TreeViewer createTreeViewer(Composite parent)
    {
        TreeViewer tree = new TreeViewer(parent, SWT.FULL_SELECTION);

        TreeViewerColumn col = new TreeViewerColumn(tree, SWT.None);
        col.getColumn().setText(Messages.ColumnName);
        col.getColumn().setWidth(300);
        ColumnViewerSorter.create(Record.class, "security").attachTo(tree, col, true); //$NON-NLS-1$

        col = new TreeViewerColumn(tree, SWT.RIGHT);
        col.getColumn().setText(Messages.ColumnIRR);
        col.getColumn().setWidth(100);
        ColumnViewerSorter.create(Record.class, "irr").attachTo(tree, col); //$NON-NLS-1$

        col = new TreeViewerColumn(tree, SWT.RIGHT);
        col.getColumn().setText(Messages.ColumnDelta);
        col.getColumn().setWidth(100);
        ColumnViewerSorter.create(Record.class, "delta").attachTo(tree, col); //$NON-NLS-1$

        TreeColumn column = new TreeColumn(tree.getTree(), SWT.None);
        column.setText(Messages.ColumnDate);
        column.setWidth(80);

        column = new TreeColumn(tree.getTree(), SWT.None);
        column.setText(Messages.ColumnTransactionType);
        column.setWidth(80);

        column = new TreeColumn(tree.getTree(), SWT.None);
        column.setText(Messages.ColumnAmount);
        column.setAlignment(SWT.RIGHT);
        column.setWidth(80);

        tree.getTree().setHeaderVisible(true);
        tree.getTree().setLinesVisible(true);

        tree.setLabelProvider(new SecurityPerformanceLabelProvider());
        tree.setContentProvider(new SecurityPerformanceContentProvider());

        return tree;
    }

    private static class SecurityPerformanceContentProvider implements ITreeContentProvider
    {
        private Record[] records;

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
        {
            @SuppressWarnings("unchecked")
            List<Record> r = (List<Record>) newInput;
            this.records = r != null ? r.toArray(new Record[0]) : new Record[0];
        }

        public Object[] getElements(Object inputElement)
        {
            return this.records;
        }

        public Object[] getChildren(Object parentElement)
        {
            if (parentElement instanceof Record)
                return ((Record) parentElement).getTransactions().toArray();
            return null;
        }

        public Object getParent(Object element)
        {
            if (element instanceof Transaction)
            {
                Transaction t = (Transaction) element;
                for (Record r : records)
                {
                    if (t.getSecurity().equals(r.getSecurity()))
                        return r;
                }

            }
            return null;
        }

        public boolean hasChildren(Object element)
        {
            return element instanceof Record;
        }

        public void dispose()
        {}

    }

    private static class SecurityPerformanceLabelProvider extends LabelProvider implements ITableLabelProvider,
                    ITableColorProvider
    {

        public Image getColumnImage(Object element, int columnIndex)
        {
            if (columnIndex != 0 || !(element instanceof Record))
                return null;

            return PortfolioPlugin.getDefault().getImageRegistry().get(PortfolioPlugin.IMG_SECURITY);
        }

        public String getColumnText(Object element, int columnIndex)
        {
            if (element instanceof Record)
            {
                Record record = (Record) element;

                switch (columnIndex)
                {
                    case 0:
                        return record.getSecurity().getName();
                    case 1:
                        return String.format("%,.2f %%", record.getIrr() * 100); //$NON-NLS-1$
                    case 2:
                        return Values.Amount.format(record.getDelta());
                }
            }
            else if (element instanceof Transaction)
            {
                Transaction t = (Transaction) element;

                switch (columnIndex)
                {
                    case 3:
                        return Values.Date.format(t.getDate());
                    case 4:
                        if (t instanceof PortfolioTransaction)
                            return ((PortfolioTransaction) t).getType().name();
                        else if (t instanceof AccountTransaction)
                            return ((AccountTransaction) t).getType().name();
                        else
                            return "QUOTE"; //$NON-NLS-1$
                    case 5:
                        return Values.Amount.format(Math.abs(t.getAmount()));
                }

            }
            return null;
        }

        @Override
        public Color getForeground(Object element, int columnIndex)
        {
            if (element instanceof Record)
            {
                Record record = (Record) element;

                switch (columnIndex)
                {
                    case 1:
                        return record.getIrr() >= 0 ? Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN)
                                        : Display.getCurrent().getSystemColor(SWT.COLOR_DARK_RED);
                    case 2:
                        return record.getDelta() >= 0 ? Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN)
                                        : Display.getCurrent().getSystemColor(SWT.COLOR_DARK_RED);
                }
            }
            return null;
        }

        @Override
        public Color getBackground(Object element, int columnIndex)
        {
            return null;
        }
    }
}
