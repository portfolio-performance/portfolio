package name.abuchen.portfolio.ui.views;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.dialogs.NewPlanDialog;
import name.abuchen.portfolio.ui.util.AbstractDropDown;
import name.abuchen.portfolio.ui.util.Column;
import name.abuchen.portfolio.ui.util.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.ColumnEditingSupport.ModificationListener;
import name.abuchen.portfolio.ui.util.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.DateEditingSupport;
import name.abuchen.portfolio.ui.util.ListEditingSupport;
import name.abuchen.portfolio.ui.util.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.ValueEditingSupport;
import name.abuchen.portfolio.ui.util.ViewerHelper;
import name.abuchen.portfolio.ui.views.columns.NameColumn;
import name.abuchen.portfolio.ui.views.columns.NoteColumn;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;

public class InvestmentPlanListView extends AbstractListView implements ModificationListener
{

    private TableViewer plans;
    private PortfolioTransactionsViewer transactions;
    private ShowHideColumnHelper planColumns;

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
    public void onModified(Object element, Object newValue, Object oldValue)
    {
        InvestmentPlan plan = (InvestmentPlan) element;
        if (plan.getAccount() != null && plan.getAccount().equals(NewPlanDialog.DELIVERY))
            plan.setAccount(null);

        markDirty();
    }

    @Override
    protected void addButtons(ToolBar toolBar)
    {
        addNewInvestmentPlanButton(toolBar);
        addConfigButton(toolBar);
    }

    private void addNewInvestmentPlanButton(ToolBar toolBar)
    {
        Action action = new Action()
        {
            @Override
            public void run()
            {
                NewPlanDialog dialog = new NewPlanDialog(Display.getCurrent().getActiveShell(), getClient());
                if (dialog.open() == Dialog.OK)
                {
                    markDirty();
                    plans.setInput(getClient().getPlans());
                }
            }
        };
        action.setImageDescriptor(PortfolioPlugin.descriptor(PortfolioPlugin.IMG_PLUS));
        action.setToolTipText(Messages.InvestmentPlanMenuCreate);

        new ActionContributionItem(action).fill(toolBar, -1);
    }

    private void addConfigButton(final ToolBar toolBar)
    {
        new AbstractDropDown(toolBar, Messages.MenuShowHideColumns, //
                        PortfolioPlugin.image(PortfolioPlugin.IMG_CONFIG), SWT.NONE)
        {
            @Override
            public void menuAboutToShow(IMenuManager manager)
            {
                MenuManager m = new MenuManager(Messages.LabelInvestmentPlans);
                planColumns.menuAboutToShow(m);
                manager.add(m);

                m = new MenuManager(Messages.LabelTransactions);
                transactions.getColumnSupport().menuAboutToShow(m);
                manager.add(m);
            }
        };
    }

    @Override
    protected void createTopTable(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        plans = new TableViewer(container, SWT.FULL_SELECTION);

        ColumnEditingSupport.prepare(plans);

        planColumns = new ShowHideColumnHelper(InvestmentPlanListView.class.getSimpleName() + "@top", //$NON-NLS-1$
                        getPreferenceStore(), plans, layout);

        addColumns(planColumns);

        planColumns.createColumns();
        plans.getTable().setHeaderVisible(true);
        plans.getTable().setLinesVisible(true);
        plans.setContentProvider(ArrayContentProvider.getInstance());
        plans.setInput(getClient().getPlans());

        if (!planColumns.isUserConfigured())
            ViewerHelper.pack(plans);

        plans.addSelectionChangedListener(new ISelectionChangedListener()
        {
            public void selectionChanged(SelectionChangedEvent event)
            {
                InvestmentPlan plan = (InvestmentPlan) ((IStructuredSelection) event.getSelection()).getFirstElement();

                if (plan != null)
                    transactions.setInput(plan.getPortfolio(), plan.getTransactions());
                else
                    transactions.setInput(null, null);

                transactions.refresh();
            }
        });

        hookContextMenu(plans.getTable(), new IMenuListener()
        {
            public void menuAboutToShow(IMenuManager manager)
            {
                fillPlansContextMenu(manager);
            }
        });
    }

    private void addColumns(ShowHideColumnHelper support)
    {
        Column column = new NameColumn("0", Messages.ColumnName, SWT.None, 100); //$NON-NLS-1$
        column.getEditingSupport().addListener(this);
        support.addColumn(column);

        column = new Column(Messages.ColumnSecurity, SWT.NONE, 250);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return ((InvestmentPlan) e).getSecurity().getName();
            }

            @Override
            public Image getImage(Object element)
            {
                return PortfolioPlugin.image(PortfolioPlugin.IMG_SECURITY);
            }
        });
        ColumnViewerSorter.create(Security.class, "name").attachTo(column); //$NON-NLS-1$
        List<Security> securities = new ArrayList<Security>(getClient().getSecurities());
        Collections.sort(securities, new Security.ByName());
        new ListEditingSupport(InvestmentPlan.class, "security", securities).addListener(this).attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

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
        ColumnViewerSorter.create(InvestmentPlan.class, "portfolio").attachTo(column); //$NON-NLS-1$
        new ListEditingSupport(InvestmentPlan.class, "portfolio", getClient().getPortfolios()).addListener(this).attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnAccount, SWT.None, 120);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                InvestmentPlan plan = (InvestmentPlan) e;
                return plan.getAccount() != null ? plan.getAccount().getName() : Messages.InvestmentPlanOptionDelivery;
            }

            @Override
            public Image getImage(Object e)
            {
                InvestmentPlan plan = (InvestmentPlan) e;
                return plan.getAccount() != null ? PortfolioPlugin.image(PortfolioPlugin.IMG_ACCOUNT) : null;
            }
        });
        ColumnViewerSorter.create(Account.class, "name").attachTo(column); //$NON-NLS-1$
        List<Account> accounts = new ArrayList<Account>();
        accounts.add(NewPlanDialog.DELIVERY);
        accounts.addAll(getClient().getAccounts());
        new ListEditingSupport(InvestmentPlan.class, "account", accounts).addListener(this).attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnStartDate, SWT.None, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return Values.Date.format(((InvestmentPlan) e).getStart());
            }
        });
        ColumnViewerSorter.create(InvestmentPlan.class, "start").attachTo(column); //$NON-NLS-1$
        new DateEditingSupport(InvestmentPlan.class, "start").addListener(this).attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnInterval, SWT.None, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return MessageFormat.format(Messages.InvestmentPlanIntervalLabel, ((InvestmentPlan) e).getInterval());
            }
        });
        ColumnViewerSorter.create(InvestmentPlan.class, "interval").attachTo(column); //$NON-NLS-1$
        List<Integer> available = new ArrayList<Integer>();
        for (int ii = 1; ii <= 12; ii++)
            available.add(ii);
        new ListEditingSupport(InvestmentPlan.class, "interval", available).addListener(this).attachTo(column); //$NON-NLS-1$
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
        ColumnViewerSorter.create(InvestmentPlan.class, "amount").attachTo(column); //$NON-NLS-1$
        new ValueEditingSupport(InvestmentPlan.class, "amount", Values.Amount).addListener(this).attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnFees, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return Values.Amount.format(((InvestmentPlan) e).getFees());
            }
        });
        ColumnViewerSorter.create(InvestmentPlan.class, "fees").attachTo(column); //$NON-NLS-1$
        new ValueEditingSupport(InvestmentPlan.class, "fees", Values.Amount).addListener(this).attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new NoteColumn();
        column.getEditingSupport().addListener(this);
        column.setVisible(false);
        support.addColumn(column);
    }

    private void fillPlansContextMenu(IMenuManager manager)
    {
        final InvestmentPlan plan = (InvestmentPlan) ((IStructuredSelection) plans.getSelection()).getFirstElement();
        if (plan == null)
            return;

        manager.add(new Action(Messages.InvestmentPlanMenuGenerateTransactions)
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

        manager.add(new Action(Messages.InvestmentPlanMenuDelete)
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
        transactions.setFullContextMenu(false);

        if (!getClient().getPlans().isEmpty())
            plans.setSelection(new StructuredSelection(plans.getElementAt(0)), true);

        transactions.pack();
    }
}
