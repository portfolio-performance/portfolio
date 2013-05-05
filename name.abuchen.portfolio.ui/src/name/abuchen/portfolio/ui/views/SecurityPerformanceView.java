package name.abuchen.portfolio.ui.views;

import java.util.List;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.SecurityPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.SecurityPerformanceSnapshot.Record;
import name.abuchen.portfolio.snapshot.SecurityPerformanceSnapshot.SecurityPositionTransaction;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.dnd.SecurityDragListener;
import name.abuchen.portfolio.ui.dnd.SecurityTransfer;
import name.abuchen.portfolio.ui.util.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.ViewerHelper;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TreeColumn;

public class SecurityPerformanceView extends AbstractHistoricView
{
    private TreeViewer tree;

    @Override
    protected String getTitle()
    {
        return Messages.LabelSecurityPerformance;
    }

    @Override
    protected Control createBody(Composite parent)
    {
        tree = createTreeViewer(parent);

        reportingPeriodUpdated();
        ViewerHelper.pack(tree);

        return tree.getControl();
    }

    @Override
    public void notifyModelUpdated()
    {
        reportingPeriodUpdated();
    }

    @Override
    protected void reportingPeriodUpdated()
    {
        ReportingPeriod period = getReportingPeriod();
        tree.setInput(SecurityPerformanceSnapshot.create(getClient(), period).getRecords());
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

        column = new TreeColumn(tree.getTree(), SWT.RIGHT);
        column.setText(Messages.ColumnAmount);
        column.setWidth(80);

        column = new TreeColumn(tree.getTree(), SWT.RIGHT);
        column.setText(Messages.ColumnShares);
        column.setWidth(80);

        column = new TreeColumn(tree.getTree(), SWT.RIGHT);
        column.setText(Messages.ColumnQuote);
        column.setWidth(80);

        tree.getTree().setHeaderVisible(true);
        tree.getTree().setLinesVisible(true);

        tree.setLabelProvider(new SecurityPerformanceLabelProvider());
        tree.setContentProvider(new SecurityPerformanceContentProvider());

        tree.addDragSupport(DND.DROP_MOVE, //
                        new Transfer[] { SecurityTransfer.getTransfer() }, //
                        new SecurityDragListener(tree));

        hookContextMenu(tree.getTree(), new IMenuListener()
        {
            public void menuAboutToShow(IMenuManager manager)
            {
                fillContextMenu(manager);
            }
        });

        return tree;
    }

    private void fillContextMenu(IMenuManager manager)
    {
        Object selection = ((IStructuredSelection) tree.getSelection()).getFirstElement();
        if (!(selection instanceof Record))
            return;

        Security security = ((Record) selection).getSecurity();
        new SecurityContextMenu(this).menuAboutToShow(manager, security);
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

            return PortfolioPlugin.image(PortfolioPlugin.IMG_SECURITY);
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
                            return ((PortfolioTransaction) t).getType().toString();
                        else if (t instanceof AccountTransaction)
                            return ((AccountTransaction) t).getType().toString();
                        else
                            return Messages.LabelQuote;
                    case 5:
                        return Values.Amount.format(Math.abs(t.getAmount()));
                    case 6:
                        if (t instanceof PortfolioTransaction)
                            return Values.Share.format(((PortfolioTransaction) t).getShares());
                        else if (t instanceof SecurityPositionTransaction)
                            return Values.Share.format(((SecurityPositionTransaction) t).getPosition().getShares());
                        else
                            return null;
                    case 7:
                        if (t instanceof PortfolioTransaction)
                            return Values.Quote.format(((PortfolioTransaction) t).getActualPurchasePrice());
                        else if (t instanceof SecurityPositionTransaction)
                            return Values.Quote.format(((SecurityPositionTransaction) t).getPosition().getPrice()
                                            .getValue());
                        else
                            return null;
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
