package name.abuchen.portfolio.ui.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.model.AccountTransaction.Type;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.CellEditorFactory;
import name.abuchen.portfolio.ui.util.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.ShowHideColumnHelper.Column;
import name.abuchen.portfolio.ui.util.SimpleListContentProvider;
import name.abuchen.portfolio.ui.util.ViewerHelper;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

public class InvestmentPlanListView extends AbstractListView
{

    private TableViewer plans;
    private TableViewer transactions;

    @Override
    protected void createTopTable(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);
        
        plans = new TableViewer(container, SWT.FULL_SELECTION);
        ShowHideColumnHelper support = new ShowHideColumnHelper(InvestmentPlanListView.class.getSimpleName() + "@top", //$NON-NLS-1$
                        plans, layout);
        
        Column column = new Column("Plan", SWT.None, 150);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return ((InvestmentPlan) e).getName();
            }

            @Override
            public Image getImage(Object element)
            {
                return PortfolioPlugin.image(PortfolioPlugin.IMG_WATCHLIST);
            }
        });
        column.setSorter(ColumnViewerSorter.create(InvestmentPlan.class, "name"), SWT.DOWN); //$NON-NLS-1$
        column.setMoveable(false);
        support.addColumn(column);
        
        support.createColumns();

        plans.getTable().setHeaderVisible(true);
        plans.getTable().setLinesVisible(true);
        
        plans.setContentProvider(new SimpleListContentProvider());
        plans.setInput(getClient().getPlans());
        plans.refresh();
        ViewerHelper.pack(plans);
        
        plans.addSelectionChangedListener(new ISelectionChangedListener()
        {
            public void selectionChanged(SelectionChangedEvent event)
            {
                InvestmentPlan plan = (InvestmentPlan) ((IStructuredSelection) event.getSelection()).getFirstElement();
                transactions.setData(InvestmentPlan.class.toString(), plan);
                transactions.setInput(plan != null ? plan.getTransactions()
                                : new ArrayList<PortfolioTransaction>(0));
                transactions.refresh();
            }
        });
    }
    
    @Override
    protected void createBottomTable(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        transactions = new TableViewer(container, SWT.FULL_SELECTION);

        ShowHideColumnHelper support = new ShowHideColumnHelper(InvestmentPlan.class.getSimpleName() + "@bottom", //$NON-NLS-1$
                        transactions, layout);
        support.setDoSaveState(false);

        Column column = new Column(Messages.ColumnDate, SWT.None, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                PortfolioTransaction t = (PortfolioTransaction) e;
                return Values.Date.format(t.getDate());
            }

            @Override
            public Color getForeground(Object element)
            {
                return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN);
            }
        });
        column.setSorter(ColumnViewerSorter.create(PortfolioTransaction.class, "date"), SWT.DOWN); //$NON-NLS-1$
        column.setMoveable(false);
        support.addColumn(column);

        column = new Column(Messages.ColumnTransactionType, SWT.None, 100);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                PortfolioTransaction t = (PortfolioTransaction) e;
                return t.getType().toString();
            }

            @Override
            public Color getForeground(Object element)
            {
                return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN);
            }
        });
        column.setSorter(ColumnViewerSorter.create(PortfolioTransaction.class, "type")); //$NON-NLS-1$
        column.setMoveable(false);
        support.addColumn(column);

        column = new Column(Messages.ColumnAmount, SWT.None, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                PortfolioTransaction t = (PortfolioTransaction) e;
                return Values.Amount.format(t.getAmount());
            }

            @Override
            public Color getForeground(Object element)
            {
                return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN);
            }
        });
        column.setSorter(ColumnViewerSorter.create(PortfolioTransaction.class, "amount")); //$NON-NLS-1$
        column.setMoveable(false);
        support.addColumn(column);

        column = new Column(Messages.ColumnSecurity, SWT.None, 250);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                PortfolioTransaction t = (PortfolioTransaction) e;
                return t.getSecurity() != null ? String.valueOf(t.getSecurity()) : null;
            }

            @Override
            public Color getForeground(Object element)
            {
                return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN);
            }
        });
        column.setSorter(ColumnViewerSorter.create(PortfolioTransaction.class, "security")); //$NON-NLS-1$
        column.setMoveable(false);
        support.addColumn(column);

        column = new Column(Messages.ColumnOffsetAccount, SWT.None, 120);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                PortfolioTransaction t = (PortfolioTransaction) e;
                return t.getCrossEntry() != null ? t.getCrossEntry().getCrossEntity(t).toString() : null;
            }

            @Override
            public Color getForeground(Object element)
            {
                return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN);
            }
        });
        column.setMoveable(false);
        support.addColumn(column);

        support.createColumns();

        transactions.getTable().setHeaderVisible(true);
        transactions.getTable().setLinesVisible(true);

        transactions.setContentProvider(new SimpleListContentProvider());

        List<Security> securities = getClient().getSecurities();
        Collections.sort(securities, new Security.ByName());

        new CellEditorFactory(transactions, PortfolioTransaction.class) //
                        .notify(new CellEditorFactory.ModificationListener()
                        {
                            public void onModified(Object element, String property)
                            {
                                PortfolioTransaction t = (PortfolioTransaction) element;
                                if (t.getCrossEntry() != null)
                                    t.getCrossEntry().updateFrom(t);

                                markDirty();
                                plans.refresh();
                                transactions.refresh(element);
                            }
                        }) //
                        .editable("date") //$NON-NLS-1$
                        .readonly("type") //$NON-NLS-1$
                        .amount("amount") //$NON-NLS-1$
                        .combobox("security", securities, true) //$NON-NLS-1$
                        .readonly("crossentry") //$NON-NLS-1$
                        .apply();

        hookContextMenu(transactions.getTable(), new IMenuListener()
        {
            public void menuAboutToShow(IMenuManager manager)
            {
                fillTransactionsContextMenu(manager);
            }
        });

        if (!getClient().getPlans().isEmpty())
            plans.setSelection(new StructuredSelection(plans.getElementAt(0)), true);
        ViewerHelper.pack(transactions);
    }
   
    private void fillTransactionsContextMenu(IMenuManager manager)
    {
        InvestmentPlan plan = (InvestmentPlan) transactions.getData(InvestmentPlan.class.toString());
        if (plan == null)
            return;

        boolean hasTransactionSelected = ((IStructuredSelection) transactions.getSelection()).getFirstElement() != null;
        if (hasTransactionSelected)
        {
            manager.add(new Separator());
            manager.add(new Action("Delete")
            {
                @Override
                public void run()
                {
                    PortfolioTransaction transaction = (PortfolioTransaction) ((IStructuredSelection) transactions
                                    .getSelection()).getFirstElement();
                    InvestmentPlan plan= (InvestmentPlan) transactions.getData(InvestmentPlan.class.toString());

                    if (transaction == null || plan == null)
                        return;

                    if (transaction.getCrossEntry() != null)
                        transaction.getCrossEntry().delete();
                    else
                        plan.getTransactions().remove(transaction);
                    markDirty();

                    plans.refresh();
                    transactions.setInput(plan.getTransactions());
                }
            });
        }
    }

    @Override
    protected String getTitle()
    {
        return "Investment Plans";
    }

}
