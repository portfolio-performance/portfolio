package name.abuchen.portfolio.ui.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.dialogs.NewPlanDialog;
import name.abuchen.portfolio.ui.util.CellEditorFactory;
import name.abuchen.portfolio.ui.util.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.ShowHideColumnHelper.Column;
import name.abuchen.portfolio.ui.util.SimpleListContentProvider;
import name.abuchen.portfolio.ui.util.ViewerHelper;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.Dialog;
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
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.PlatformUI;

public class InvestmentPlanListView extends AbstractListView
{
    private class TransactionLabelProvider extends ColumnLabelProvider
    {
        @Override
        public Color getForeground(Object element)
        {
            return newTransactions.contains(element) ? Display.getDefault().getSystemColor(SWT.COLOR_INFO_FOREGROUND)
                            : null;
        }

        @Override
        public Color getBackground(Object element)
        {
            return newTransactions.contains(element) ? Display.getDefault().getSystemColor(SWT.COLOR_INFO_BACKGROUND)
                            : null;
        }
    }

    private TableViewer plans;
    private TableViewer transactions;
    private List<Transaction> newTransactions = Collections.emptyList();

    @Override
    protected void addButtons(ToolBar toolBar)
    {
        Action action = new Action()
        {
            @Override
            public void run()
            {
                NewPlanDialog newDialog = new NewPlanDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                                .getShell(), getClient());
                if (newDialog.open() == Dialog.OK)
                {
                    markDirty();
                    plans.setInput(getClient().getPlans());
                }
            }
        };
        action.setImageDescriptor(PortfolioPlugin.descriptor(PortfolioPlugin.IMG_PLUS));
        action.setToolTipText("New Plan...");

        new ActionContributionItem(action).fill(toolBar, -1);
    }

    @Override
    protected void createTopTable(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);
        plans = new TableViewer(container, SWT.FULL_SELECTION);
        ShowHideColumnHelper support = new ShowHideColumnHelper(InvestmentPlanListView.class.getSimpleName() + "@top", //$NON-NLS-1$
                        plans, layout);
        Column column = new Column("Plan", SWT.None, 100);
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
        Column secCol = new Column("Security", SWT.NONE, 100);
        secCol.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return ((InvestmentPlan) e).getSecurity().getName();
            }
        });
        secCol.setMoveable(false);
        support.addColumn(secCol);
        column = new Column("Portfolio", SWT.None, 100);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return ((InvestmentPlan) e).getPortfolio().getName();
            }

            @Override
            public Image getImage(Object element)
            {
                return PortfolioPlugin.image(PortfolioPlugin.IMG_PORTFOLIO);
            }
        });
        column.setSorter(ColumnViewerSorter.create(InvestmentPlan.class, "portfolio"), SWT.DOWN); //$NON-NLS-1$
        column.setMoveable(false);
        support.addColumn(column);
        column = new Column("Amount", SWT.None, 50);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return Values.Amount.format(((InvestmentPlan) e).getAmount());
            }
        });
        column.setMoveable(false);
        support.addColumn(column);
        column = new Column("Cost", SWT.None, 70);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return Values.Amount.format(((InvestmentPlan) e).getTransactionCost());
            }
        });
        column.setMoveable(false);
        support.addColumn(column);
        column = new Column("Start", SWT.None, 70);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return Values.Date.format(((InvestmentPlan) e).getStart());
            }
        });
        column.setMoveable(false);
        support.addColumn(column);
        column = new Column("Account Transcations?", SWT.None, 50);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return "" + ((InvestmentPlan) e).isBuySellEntry();
            }
        });
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
                transactions.setInput(plan != null ? plan.getTransactions() : new ArrayList<PortfolioTransaction>(0));
                transactions.refresh();
            }
        });
        List<Security> securities = getClient().getSecurities();
        Collections.sort(securities, new Security.ByName());
        List<Boolean> booleans = new ArrayList<Boolean>();
        booleans.add(true);
        booleans.add(false);
        new CellEditorFactory(plans, InvestmentPlan.class).notify(new CellEditorFactory.ModificationListener()
        {
            public void onModified(Object element, String property)
            {
                markDirty();
                plans.refresh();
                transactions.refresh(element);
            }
        }).editable("name") //$NON-NLS-1$
                        .combobox("security", securities, true) //$NON-NLS-1$
                        .readonly("portfolio").amount("amount").amount("transactionCost").editable("start")
                        .combobox("isBuySellEntry", booleans, false).apply();
        hookContextMenu(plans.getTable(), new IMenuListener()
        {
            public void menuAboutToShow(IMenuManager manager)
            {
                final InvestmentPlan plan = (InvestmentPlan) ((IStructuredSelection) plans.getSelection())
                                .getFirstElement();
                if (plan == null) { return; }

                manager.add(new Action("Generate Transactions")
                {
                    @Override
                    public void run()
                    {
                        newTransactions = plan.generateTransactions();
                        markDirty();
                        plans.refresh();
                        transactions.setInput(plan.getTransactions());
                    }
                });
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
        Column column = new Column(Messages.ColumnDate, SWT.None, 80);
        column.setLabelProvider(new TransactionLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                PortfolioTransaction t = (PortfolioTransaction) e;
                return Values.Date.format(t.getDate());
            }
        });
        column.setSorter(ColumnViewerSorter.create(PortfolioTransaction.class, "date"), SWT.DOWN); //$NON-NLS-1$
        column.setMoveable(false);
        support.addColumn(column);
        column = new Column(Messages.ColumnTransactionType, SWT.None, 100);
        column.setLabelProvider(new TransactionLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                PortfolioTransaction t = (PortfolioTransaction) e;
                return t.getType().toString();
            }
        });
        column.setMoveable(false);
        support.addColumn(column);
        column = new Column(Messages.ColumnAmount, SWT.None, 80);
        column.setLabelProvider(new TransactionLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                PortfolioTransaction t = (PortfolioTransaction) e;
                return Values.Amount.format(t.getAmount());
            }
        });
        column.setMoveable(false);
        support.addColumn(column);
        column = new Column(Messages.ColumnSecurity, SWT.None, 250);
        column.setLabelProvider(new TransactionLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                PortfolioTransaction t = (PortfolioTransaction) e;
                return t.getSecurity() != null ? String.valueOf(t.getSecurity()) : null;
            }
        });
        column.setMoveable(false);
        support.addColumn(column);
        column = new Column(Messages.ColumnOffsetAccount, SWT.None, 120);
        column.setLabelProvider(new TransactionLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                PortfolioTransaction t = (PortfolioTransaction) e;
                return t.getCrossEntry() != null ? t.getCrossEntry().getCrossEntity(t).toString() : null;
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
            manager.add(new Action("Delete")
            {
                @Override
                public void run()
                {
                    PortfolioTransaction transaction = (PortfolioTransaction) ((IStructuredSelection) transactions
                                    .getSelection()).getFirstElement();
                    InvestmentPlan plan = (InvestmentPlan) transactions.getData(InvestmentPlan.class.toString());

                    if (transaction == null || plan == null)
                        return;

                    if (transaction.getCrossEntry() != null)
                    {
                        transaction.getCrossEntry().delete();
                    }
                    else
                    {
                        plan.getPortfolio().getTransactions().remove(transaction);
                    }
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
