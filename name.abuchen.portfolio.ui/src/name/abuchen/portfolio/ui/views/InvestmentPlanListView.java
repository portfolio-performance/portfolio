package name.abuchen.portfolio.ui.views;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Attributable;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.InvestmentPlan.Type;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.dialogs.transactions.InvestmentPlanDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.OpenDialogAction;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.editor.PortfolioPart;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.LogoManager;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.viewers.BooleanEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport.ModificationListener;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.CopyPasteSupport;
import name.abuchen.portfolio.ui.util.viewers.DateEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.DateLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.ListEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.viewers.ValueEditingSupport;
import name.abuchen.portfolio.ui.views.columns.AttributeColumn;
import name.abuchen.portfolio.ui.views.columns.NameColumn;
import name.abuchen.portfolio.ui.views.columns.NoteColumn;
import name.abuchen.portfolio.ui.views.panes.HistoricalPricesPane;
import name.abuchen.portfolio.ui.views.panes.InformationPanePage;
import name.abuchen.portfolio.ui.views.panes.SecurityPriceChartPane;
import name.abuchen.portfolio.ui.views.panes.TransactionsPane;

public class InvestmentPlanListView extends AbstractFinanceView implements ModificationListener
{
    private TableViewer plans;
    private ShowHideColumnHelper planColumns;

    @Inject
    private ExchangeRateProviderFactory factory;

    @Inject
    private PortfolioPart part;

    @Override
    protected String getDefaultTitle()
    {
        return Messages.LabelInvestmentPlans;
    }

    @Override
    public void notifyModelUpdated()
    {
        plans.setInput(getClient().getPlans());
        plans.setSelection(plans.getSelection());
    }

    @Override
    protected void notifyViewCreationCompleted()
    {
        if (plans.getTable().getItemCount() > 0)
            plans.setSelection(new StructuredSelection(plans.getElementAt(0)), true);
    }

    @Override
    public void onModified(Object element, Object newValue, Object oldValue)
    {
        markDirty();
    }

    @Override
    protected void addButtons(ToolBarManager toolBar)
    {
        addNewInvestmentPlanButton(toolBar);
        addConfigButton(toolBar);
    }

    private void addNewInvestmentPlanButton(ToolBarManager toolBar)
    {
        toolBar.add(new DropDown(Messages.InvestmentPlanMenuCreate, Images.PLUS, SWT.NONE, manager -> {

            manager.add(new OpenDialogAction(this, Messages.InvestmentPlanTypeBuyDelivery) //
                            .type(InvestmentPlanDialog.class) //
                            .parameters(InvestmentPlan.Type.BUY_OR_DELIVERY));
            manager.add(new OpenDialogAction(this, Messages.InvestmentPlanTypeDeposit) //
                            .type(InvestmentPlanDialog.class) //
                            .parameters(InvestmentPlan.Type.DEPOSIT));
            manager.add(new OpenDialogAction(this, Messages.InvestmentPlanTypeRemoval) //
                            .type(InvestmentPlanDialog.class) //
                            .parameters(InvestmentPlan.Type.REMOVAL));
        }));
    }

    private void addConfigButton(final ToolBarManager toolBar)
    {
        toolBar.add(new DropDown(Messages.MenuShowHideColumns, Images.CONFIG, SWT.NONE,
                        manager -> planColumns.menuAboutToShow(manager)));
    }

    @Override
    protected Control createBody(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        plans = new TableViewer(container, SWT.FULL_SELECTION);

        ColumnEditingSupport.prepare(plans);
        CopyPasteSupport.enableFor(plans);

        planColumns = new ShowHideColumnHelper(InvestmentPlanListView.class.getSimpleName() + "@top", //$NON-NLS-1$
                        getPreferenceStore(), plans, layout);

        addColumns(planColumns);
        addAttributeColumns(planColumns);

        planColumns.createColumns(true);
        plans.getTable().setHeaderVisible(true);
        plans.getTable().setLinesVisible(true);
        plans.setContentProvider(ArrayContentProvider.getInstance());
        plans.setInput(getClient().getPlans());

        plans.addSelectionChangedListener(event -> setInformationPaneInput(
                        ((IStructuredSelection) event.getSelection()).getFirstElement()));

        hookContextMenu(plans.getTable(), this::fillPlansContextMenu);

        return container;
    }

    private Image maybeGetLogo(Attributable object)
    {
        return LogoManager.instance().getDefaultColumnImage(object, getClient().getSettings());
    }

    private void addColumns(ShowHideColumnHelper support)
    {
        Column column = new NameColumn("0", Messages.ColumnName, SWT.None, 100, part.getClient()); //$NON-NLS-1$
        column.getEditingSupport().addListener(this);
        support.addColumn(column);

        column = new Column(Messages.ColumnSecurity, SWT.NONE, 250);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                InvestmentPlan plan = (InvestmentPlan) e;
                return plan.getSecurity() != null ? plan.getSecurity().getName() : null;
            }

            @Override
            public Image getImage(Object e)
            {
                InvestmentPlan plan = (InvestmentPlan) e;
                return maybeGetLogo(plan.getSecurity());
            }
        });
        ColumnViewerSorter.create(Security.class, "name").attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnPortfolio, SWT.None, 120);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                InvestmentPlan plan = (InvestmentPlan) e;
                if (plan.getPortfolio() != null)
                {
                    return plan.getPortfolio().getName();
                }
                else
                {
                    if (plan.getPlanType() == Type.DEPOSIT)
                        return Messages.InvestmentPlanOptionDeposit;
                    else
                        return Messages.InvestmentPlanOptionRemoval;
                }
            }

            @Override
            public Image getImage(Object e)
            {
                InvestmentPlan plan = (InvestmentPlan) e;
                return maybeGetLogo(plan.getPortfolio());
            }
        });
        ColumnViewerSorter.create(InvestmentPlan.class, "portfolio").attachTo(column); //$NON-NLS-1$
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
                return maybeGetLogo(plan.getAccount());
            }
        });
        ColumnViewerSorter.create(Account.class, "name").attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnStartDate, SWT.None, 80);
        column.setLabelProvider(new DateLabelProvider(e -> ((InvestmentPlan) e).getStart()));
        ColumnViewerSorter.create(InvestmentPlan.class, "start").attachTo(column); //$NON-NLS-1$
        new DateEditingSupport(InvestmentPlan.class, "start").addListener(this).attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnLastDate, SWT.None, 80);
        column.setLabelProvider(new DateLabelProvider(e -> ((InvestmentPlan) e).getLastDate().orElse(null)));
        ColumnViewerSorter.create(InvestmentPlan.class, "LastDate").attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnNextDate, SWT.None, 80);
        column.setLabelProvider(
                        new DateLabelProvider(e -> ((InvestmentPlan) e).getDateOfNextTransactionToBeGenerated()));
        ColumnViewerSorter.create(InvestmentPlan.class, "DateOfNextTransactionToBeGenerated").attachTo(column); //$NON-NLS-1$
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
        List<Integer> available = new ArrayList<>();
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
                InvestmentPlan plan = (InvestmentPlan) e;
                return Values.Money.format(Money.of(plan.getCurrencyCode(), plan.getAmount()));
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
                InvestmentPlan plan = (InvestmentPlan) e;
                return Values.Money.format(Money.of(plan.getCurrencyCode(), plan.getFees()));
            }
        });
        ColumnViewerSorter.create(InvestmentPlan.class, "fees").attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnAutoGenerate, SWT.LEFT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return ""; //$NON-NLS-1$
            }

            @Override
            public Image getImage(Object e)
            {
                return ((InvestmentPlan) e).isAutoGenerate() ? Images.CHECK.image() : null;
            }
        });
        ColumnViewerSorter.create(InvestmentPlan.class, "autoGenerate").attachTo(column); //$NON-NLS-1$
        new BooleanEditingSupport(InvestmentPlan.class, "autoGenerate").addListener(this).attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new NoteColumn();
        column.getEditingSupport().addListener(this);
        column.setVisible(false);
        support.addColumn(column);
    }

    private void addAttributeColumns(ShowHideColumnHelper support)
    {
        AttributeColumn.createFor(getClient(), InvestmentPlan.class) //
                        .forEach(column -> {
                            column.getEditingSupport().addListener(this);
                            support.addColumn(column);
                        });
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
                try
                {
                    CurrencyConverterImpl converter = new CurrencyConverterImpl(factory, getClient().getBaseCurrency());
                    List<TransactionPair<?>> latest = plan.generateTransactions(converter);

                    if (latest.isEmpty())
                    {
                        MessageDialog.openInformation(getActiveShell(), Messages.LabelInfo, MessageFormat.format(
                                        Messages.InvestmentPlanInfoNoTransactionsGenerated,
                                        Values.Date.format(plan.getDateOfNextTransactionToBeGenerated())));
                    }
                    else
                    {
                        lookup(TransactionsPane.class).ifPresent(p -> p.markTransactions(latest));

                        markDirty();
                        plans.refresh();

                        setInformationPaneInput(plan);
                    }
                }
                catch (IOException e)
                {
                    MessageDialog.openError(getActiveShell(), Messages.LabelError, e.getMessage());
                    PortfolioPlugin.log(e);
                }
            }
        });

        manager.add(new Separator());

        new OpenDialogAction(this, Messages.MenuEditInvestmentPlan) //
                        .type(InvestmentPlanDialog.class, d -> d.setPlan(plan)) //
                        .parameters(plan.getPlanType()).addTo(manager);

        manager.add(new Separator());

        if (LogoManager.instance().hasCustomLogo(plan, getClient().getSettings()))
        {
            manager.add(new SimpleAction(Messages.LabelRemoveLogo, a -> {
                LogoManager.instance().clearCustomLogo(plan, getClient().getSettings());
                markDirty();
            }));
        }

        manager.add(new Action(Messages.InvestmentPlanMenuDelete)
        {
            @Override
            public void run()
            {
                getClient().removePlan(plan);
                markDirty();

                plans.setInput(getClient().getPlans());
                setInformationPaneInput(null);
            }
        });
    }

    @Override
    protected void addPanePages(List<InformationPanePage> pages)
    {
        super.addPanePages(pages);
        pages.add(make(TransactionsPane.class));
        pages.add(make(SecurityPriceChartPane.class));
        pages.add(make(HistoricalPricesPane.class));
    }
}
