package name.abuchen.portfolio.ui.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.dialogs.NewPlanDialog;
import name.abuchen.portfolio.ui.util.CellEditorFactory;
import name.abuchen.portfolio.ui.util.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.ShowHideColumnHelper.Column;
import name.abuchen.portfolio.ui.util.ViewerHelper;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.PlatformUI;

public class InvestmentPlanListView extends AbstractListView
{

    private TableViewer plans;
    private PortfolioTransactionsViewer transactions;

    @Override
    protected String getTitle()
    {
        return Messages.LabelInvestmentPlans;
    }

    @Override
    public void notifyModelUpdated()
    {
        plans.setSelection(plans.getSelection());
    }

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

        Column column = new Column(Messages.ColumnName, SWT.None, 100);
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

        Column secCol = new Column(Messages.ColumnSecurity, SWT.NONE, 250);
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

        column = new Column(Messages.ColumnPortfolio, SWT.None, 120);
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

        column = new Column("Start", SWT.None, 80);
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

        column = new Column(Messages.ColumnAmount, SWT.RIGHT, 80);
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

        column = new Column(Messages.ColumnFees, SWT.RIGHT, 80);
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

        column = new Column("Account Transcations?", SWT.None, 50);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return String.valueOf(((InvestmentPlan) e).isBuySellEntry());
            }
        });
        column.setMoveable(false);
        support.addColumn(column);

        support.createColumns();
        plans.getTable().setHeaderVisible(true);
        plans.getTable().setLinesVisible(true);
        plans.setContentProvider(ArrayContentProvider.getInstance());
        plans.setInput(getClient().getPlans());

        if (!support.isUserConfigured())
            ViewerHelper.pack(plans);

        plans.addSelectionChangedListener(new ISelectionChangedListener()
        {
            public void selectionChanged(SelectionChangedEvent event)
            {
                InvestmentPlan plan = (InvestmentPlan) ((IStructuredSelection) event.getSelection()).getFirstElement();

                transactions.setPortfolio(plan != null ? plan.getPortfolio() : null);
                transactions.setInput(plan != null ? plan.getTransactions() : new ArrayList<PortfolioTransaction>(0));
                transactions.refresh();
            }
        });

        List<Security> securities = getClient().getSecurities();
        Collections.sort(securities, new Security.ByName());
        List<Boolean> booleans = new ArrayList<Boolean>();
        booleans.add(true);
        booleans.add(false);

        new CellEditorFactory(plans, InvestmentPlan.class) //
                        .notify(new CellEditorFactory.ModificationListener()
                        {
                            public void onModified(Object element, String property)
                            {
                                markDirty();
                                plans.refresh();
                            }
                        }) //
                        .editable("name") //$NON-NLS-1$
                        .combobox("security", securities, true) //$NON-NLS-1$
                        .readonly("portfolio") //$NON-NLS-1$
                        .editable("start") //$NON-NLS-1$
                        .amount("amount") //$NON-NLS-1$
                        .amount("transactionCost") //$NON-NLS-1$
                        .combobox("isBuySellEntry", booleans, false) //$NON-NLS-1$
                        .apply();

        hookContextMenu(plans.getTable(), new IMenuListener()
        {
            public void menuAboutToShow(IMenuManager manager)
            {
                fillPlansContextMenu(manager);
            }
        });
    }

    private void fillPlansContextMenu(IMenuManager manager)
    {
        final InvestmentPlan plan = (InvestmentPlan) ((IStructuredSelection) plans.getSelection()).getFirstElement();
        if (plan == null)
            return;

        manager.add(new Action("Generate Transactions")
        {
            @Override
            public void run()
            {
                List<PortfolioTransaction> latest = plan.generateTransactions();
                markDirty();

                plans.refresh();
                transactions.markTransactions(latest);
                transactions.setInput(plan.getPortfolio(), plan.getTransactions());
            }
        });

        manager.add(new Action("Delete Investment Plan")
        {
            @Override
            public void run()
            {
                getClient().removePlan(plan);
                markDirty();

                plans.setInput(getClient().getPlans());
                transactions.setInput(null, null);
            }
        });
    }

    @Override
    protected void createBottomTable(Composite parent)
    {
        transactions = new PortfolioTransactionsViewer(parent, this);

        if (!getClient().getPlans().isEmpty())
            plans.setSelection(new StructuredSelection(plans.getElementAt(0)), true);

        transactions.pack();
    }
}
