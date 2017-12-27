package name.abuchen.portfolio.ui.views;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.swtchart.ISeries;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransaction.Type;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MutableMoney;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.AccountSnapshot;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPart;
import name.abuchen.portfolio.ui.dialogs.transactions.AccountTransactionDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.AccountTransferDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.OpenDialogAction;
import name.abuchen.portfolio.ui.dialogs.transactions.SecurityTransactionDialog;
import name.abuchen.portfolio.ui.util.AbstractDropDown;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.chart.TimelineChart;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport.ModificationListener;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.DateTimeEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.SharesLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.viewers.StringEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ValueEditingSupport;
import name.abuchen.portfolio.ui.views.columns.CurrencyColumn;
import name.abuchen.portfolio.ui.views.columns.CurrencyColumn.CurrencyEditingSupport;
import name.abuchen.portfolio.ui.views.columns.NameColumn;
import name.abuchen.portfolio.ui.views.columns.NameColumn.NameColumnLabelProvider;
import name.abuchen.portfolio.ui.views.columns.NoteColumn;

public class AccountListView extends AbstractListView implements ModificationListener
{
    private static final String FILTER_INACTIVE_ACCOUNTS = "filter-redired-accounts"; //$NON-NLS-1$

    private TableViewer accounts;
    private TableViewer transactions;
    private TimelineChart accountBalanceChart;

    /**
     * Store current balance of account after given transaction has been
     * applied. See {@link #updateBalance(Account)}. Do not store transient
     * balance in persistent AccountTransaction object.
     */
    private Map<AccountTransaction, Money> transaction2balance = new HashMap<>();

    private AccountContextMenu accountMenu = new AccountContextMenu(this);

    private ShowHideColumnHelper accountColumns;
    private ShowHideColumnHelper transactionsColumns;

    private boolean isFiltered = false;

    @Inject
    private ExchangeRateProviderFactory factory;

    @Override
    protected String getDefaultTitle()
    {
        return Messages.LabelAccounts;
    }

    @Override
    public void init(PortfolioPart part, Object parameter)
    {
        super.init(part, parameter);

        isFiltered = part.getPreferenceStore().getBoolean(FILTER_INACTIVE_ACCOUNTS);
    }

    @Override
    protected int getSashStyle()
    {
        return SWT.VERTICAL | SWT.BEGINNING;
    }

    private void resetInput()
    {
        accounts.setInput(isFiltered ? getClient().getActiveAccounts() : getClient().getAccounts());
    }

    @Override
    protected void addButtons(ToolBar toolBar)
    {
        addNewButton(toolBar);
        addFilterButton(toolBar);
        addConfigButton(toolBar);
    }

    private void addNewButton(ToolBar toolBar)
    {
        SimpleAction.Runnable newAccountAction = a -> {
            Account account = new Account();
            account.setName(Messages.LabelNoName);
            account.setCurrencyCode(getClient().getBaseCurrency());

            getClient().addAccount(account);
            markDirty();

            resetInput();
            accounts.editElement(account, 0);
        };

        AbstractDropDown.create(toolBar, Messages.MenuCreateAccountOrTransaction, Images.PLUS.image(), SWT.NONE,
                        (dd, manager) -> {

                            manager.add(new SimpleAction(Messages.AccountMenuAdd, newAccountAction));

                            manager.add(new Separator());

                            Account account = (Account) accounts.getStructuredSelection().getFirstElement();
                            new AccountContextMenu(AccountListView.this).menuAboutToShow(manager, account, null);
                        });

    }

    private void addFilterButton(ToolBar toolBar)
    {
        Action filter = new Action()
        {
            @Override
            public void run()
            {
                isFiltered = !isFiltered;
                getPart().getPreferenceStore().setValue(FILTER_INACTIVE_ACCOUNTS, isFiltered);
                setImageDescriptor(isFiltered ? Images.FILTER_ON.descriptor() : Images.FILTER_OFF.descriptor());
                resetInput();
            }
        };
        filter.setImageDescriptor(isFiltered ? Images.FILTER_ON.descriptor() : Images.FILTER_OFF.descriptor());
        filter.setToolTipText(Messages.AccountFilterRetiredAccounts);
        new ActionContributionItem(filter).fill(toolBar, -1);
    }

    private void addConfigButton(final ToolBar toolBar)
    {
        new AbstractDropDown(toolBar, Messages.MenuShowHideColumns, Images.CONFIG.image(), SWT.NONE) // NOSONAR
        {
            @Override
            public void menuAboutToShow(IMenuManager manager)
            {
                MenuManager m = new MenuManager(Messages.LabelAccounts);
                accountColumns.menuAboutToShow(m);
                manager.add(m);

                m = new MenuManager(Messages.LabelTransactions);
                transactionsColumns.menuAboutToShow(m);
                manager.add(m);
            }
        };
    }

    @Override
    public void notifyModelUpdated()
    {
        resetInput();

        Account account = (Account) ((IStructuredSelection) accounts.getSelection()).getFirstElement();
        if (getClient().getAccounts().contains(account))
            accounts.setSelection(new StructuredSelection(account));
        else
            accounts.setSelection(StructuredSelection.EMPTY);
    }

    @Override
    public void onModified(Object element, Object newValue, Object oldValue)
    {
        if (element instanceof AccountTransaction)
        {
            AccountTransaction t = (AccountTransaction) element;
            if (t.getCrossEntry() != null)
                t.getCrossEntry().updateFrom(t);
            accounts.refresh(true);

            updateOnAccountSelected((Account) transactions.getData(Account.class.toString()));
        }

        markDirty();
    }

    // //////////////////////////////////////////////////////////////
    // top table: accounts
    // //////////////////////////////////////////////////////////////

    @Override
    protected void createTopTable(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        accounts = new TableViewer(container, SWT.FULL_SELECTION);

        ColumnEditingSupport.prepare(accounts);

        accountColumns = new ShowHideColumnHelper(AccountListView.class.getSimpleName() + "@top2", //$NON-NLS-1$
                        getPreferenceStore(), accounts, layout);

        Column column = new NameColumn("0", Messages.ColumnAccount, SWT.None, 150); //$NON-NLS-1$
        column.setLabelProvider(new NameColumnLabelProvider() // NOSONAR
        {
            @Override
            public Color getForeground(Object e)
            {
                boolean isRetired = ((Account) e).isRetired();
                return isRetired ? Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY) : null;
            }
        });
        column.getEditingSupport().addListener(this);
        accountColumns.addColumn(column);

        column = new Column(Messages.ColumnBalance, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return Values.Amount.format(((Account) e).getCurrentAmount());
            }
        });
        ColumnViewerSorter.create(Account.class, "currentAmount").attachTo(column); //$NON-NLS-1$
        accountColumns.addColumn(column);

        column = new CurrencyColumn();
        column.setEditingSupport(new CurrencyEditingSupport()
        {
            @Override
            public boolean canEdit(Object element)
            {
                return ((Account) element).getTransactions().isEmpty();
            }
        });
        accountColumns.addColumn(column);

        column = new NoteColumn();
        column.getEditingSupport().addListener(this);
        accountColumns.addColumn(column);

        accountColumns.createColumns();

        accounts.getTable().setHeaderVisible(true);
        accounts.getTable().setLinesVisible(true);

        accounts.setContentProvider(ArrayContentProvider.getInstance());
        resetInput();
        accounts.refresh();

        accounts.addSelectionChangedListener(event -> {
            Account account = (Account) ((IStructuredSelection) event.getSelection()).getFirstElement();

            updateOnAccountSelected(account);

            transactions.setData(Account.class.toString(), account);
            transactions.setInput(account != null ? account.getTransactions() : new ArrayList<AccountTransaction>(0));
            transactions.refresh();
        });

        hookContextMenu(accounts.getTable(), this::fillAccountsContextMenu);
    }

    private void fillAccountsContextMenu(IMenuManager manager) // NOSONAR
    {
        final Account account = (Account) ((IStructuredSelection) accounts.getSelection()).getFirstElement();
        if (account == null)
            return;

        accountMenu.menuAboutToShow(manager, account, null);
        manager.add(new Separator());

        manager.add(new Action(account.isRetired() ? Messages.AccountMenuActivate : Messages.AccountMenuDeactivate)
        {
            @Override
            public void run()
            {
                account.setRetired(!account.isRetired());
                markDirty();
                resetInput();
            }

        });

        manager.add(new Action(Messages.AccountMenuDelete)
        {
            @Override
            public void run()
            {
                getClient().removeAccount(account);
                markDirty();
                resetInput();
            }
        });
    }

    // //////////////////////////////////////////////////////////////
    // bottom table: transactions
    // //////////////////////////////////////////////////////////////

    @Override
    protected void createBottomTable(Composite parent)
    {
        // folder
        CTabFolder folder = new CTabFolder(parent, SWT.BORDER);

        CTabItem item = new CTabItem(folder, SWT.NONE);
        item.setText(Messages.TabTransactions);
        item.setControl(createTransactionTable(folder));

        item = new CTabItem(folder, SWT.NONE);
        item.setText(Messages.TabAccountBalanceChart);
        item.setControl(createAccountBalanceChart(folder));

        folder.setSelection(0);

        if (accounts.getTable().getItemCount() > 0)
            accounts.setSelection(new StructuredSelection(accounts.getElementAt(0)), true);
    }

    protected Control createTransactionTable(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        transactions = new TableViewer(container, SWT.FULL_SELECTION | SWT.MULTI);
        ColumnViewerToolTipSupport.enableFor(transactions, ToolTip.NO_RECREATE);

        ColumnEditingSupport.prepare(transactions);

        transactionsColumns = new ShowHideColumnHelper(AccountListView.class.getSimpleName() + "@bottom5", //$NON-NLS-1$
                        getPreferenceStore(), transactions, layout);

        Column column = new Column(Messages.ColumnDate, SWT.None, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return Values.DateTime.format(((AccountTransaction) e).getDateTime());
            }

            @Override
            public Color getForeground(Object element)
            {
                return colorFor((AccountTransaction) element);
            }
        });
        ColumnViewerSorter.create(new AccountTransaction.ByDateAmountTypeAndHashCode()).attachTo(column, SWT.DOWN);
        new DateTimeEditingSupport(AccountTransaction.class, "dateTime").addListener(this).attachTo(column); //$NON-NLS-1$
        transactionsColumns.addColumn(column);

        column = new Column(Messages.ColumnTransactionType, SWT.None, 100);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                AccountTransaction t = (AccountTransaction) e;
                return t.getType().toString();
            }

            @Override
            public Color getForeground(Object element)
            {
                return colorFor((AccountTransaction) element);
            }
        });
        column.setSorter(ColumnViewerSorter.create(AccountTransaction.class, "type")); //$NON-NLS-1$
        transactionsColumns.addColumn(column);

        column = new Column(Messages.ColumnAmount, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                AccountTransaction t = (AccountTransaction) e;
                long v = t.getAmount();
                if (t.getType().isDebit())
                    v = -v;
                return Values.Money.format(Money.of(t.getCurrencyCode(), v), getClient().getBaseCurrency());
            }

            @Override
            public Color getForeground(Object element)
            {
                return colorFor((AccountTransaction) element);
            }
        });
        column.setSorter(ColumnViewerSorter.create(AccountTransaction.class, "amount")); //$NON-NLS-1$
        transactionsColumns.addColumn(column);

        column = new Column(Messages.Balance, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Money balance = transaction2balance.get(e);
                return balance != null ? Values.Money.format(balance, getClient().getBaseCurrency()) : null;
            }
        });
        column.setSorter(ColumnViewerSorter.create(new AccountTransaction.ByDateAmountTypeAndHashCode()));
        transactionsColumns.addColumn(column);

        column = new Column(Messages.ColumnSecurity, SWT.None, 250);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                AccountTransaction t = (AccountTransaction) e;
                return t.getSecurity() != null ? String.valueOf(t.getSecurity()) : null;
            }

            @Override
            public Color getForeground(Object element)
            {
                return colorFor((AccountTransaction) element);
            }
        });
        column.setSorter(ColumnViewerSorter.create(AccountTransaction.class, "security")); //$NON-NLS-1$
        transactionsColumns.addColumn(column);

        column = new Column(Messages.ColumnShares, SWT.RIGHT, 80);
        column.setLabelProvider(new SharesLabelProvider()
        {
            @Override
            public Long getValue(Object e)
            {
                AccountTransaction t = (AccountTransaction) e;
                if (t.getCrossEntry() instanceof BuySellEntry)
                {
                    return ((BuySellEntry) t.getCrossEntry()).getPortfolioTransaction().getShares();
                }
                else if (t.getType() == Type.DIVIDENDS && t.getShares() != 0)
                {
                    return t.getShares();
                }
                else
                {
                    return null;
                }
            }

            @Override
            public Color getForeground(Object element)
            {
                return colorFor((AccountTransaction) element);
            }
        });
        new ValueEditingSupport(AccountTransaction.class, "shares", Values.Share) //$NON-NLS-1$
        {
            @Override
            public boolean canEdit(Object element)
            {
                AccountTransaction t = (AccountTransaction) element;
                return t.getType() == AccountTransaction.Type.DIVIDENDS;
            }
        }.addListener(this).attachTo(column);
        transactionsColumns.addColumn(column);

        column = new Column(Messages.ColumnPerShare, SWT.RIGHT, 80);
        column.setDescription(Messages.ColumnPerShare_Description);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                AccountTransaction t = (AccountTransaction) e;
                if (t.getCrossEntry() instanceof BuySellEntry)
                {
                    PortfolioTransaction pt = ((BuySellEntry) t.getCrossEntry()).getPortfolioTransaction();
                    return Values.Quote.format(pt.getGrossPricePerShare(), getClient().getBaseCurrency());
                }
                else if (t.getType() == Type.DIVIDENDS && t.getShares() != 0)
                {
                    long dividendPerShare = Math.round(t.getAmount() * Values.Share.divider()
                                    * Values.Quote.factorToMoney() / t.getShares());
                    return Values.Quote.format(Quote.of(t.getCurrencyCode(), dividendPerShare),
                                    getClient().getBaseCurrency());
                }
                else
                {
                    return null;
                }
            }

            @Override
            public Color getForeground(Object element)
            {
                return colorFor((AccountTransaction) element);
            }
        });
        transactionsColumns.addColumn(column);

        column = new Column(Messages.ColumnOffsetAccount, SWT.None, 120);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                AccountTransaction t = (AccountTransaction) e;
                return t.getCrossEntry() != null ? t.getCrossEntry().getCrossOwner(t).toString() : null;
            }

            @Override
            public Color getForeground(Object element)
            {
                return colorFor((AccountTransaction) element);
            }
        });
        transactionsColumns.addColumn(column);

        column = new Column(Messages.ColumnNote, SWT.None, 200);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return ((AccountTransaction) e).getNote();
            }

            @Override
            public Color getForeground(Object element)
            {
                return colorFor((AccountTransaction) element);
            }

            @Override
            public Image getImage(Object e)
            {
                String note = ((AccountTransaction) e).getNote();
                return note != null && note.length() > 0 ? Images.NOTE.image() : null;
            }
        });
        ColumnViewerSorter.create(AccountTransaction.class, "note").attachTo(column); //$NON-NLS-1$
        new StringEditingSupport(AccountTransaction.class, "note").addListener(this).attachTo(column); //$NON-NLS-1$
        transactionsColumns.addColumn(column);

        transactionsColumns.createColumns();

        transactions.getTable().setHeaderVisible(true);
        transactions.getTable().setLinesVisible(true);

        transactions.setContentProvider(ArrayContentProvider.getInstance());

        hookContextMenu(transactions.getTable(), this::fillTransactionsContextMenu);

        hookKeyListener();

        return container;
    }

    private Color colorFor(AccountTransaction t)
    {
        if (t.getType().isDebit())
            return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_RED);
        else
            return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN);
    }

    private void hookKeyListener()
    {
        transactions.getControl().addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {
                if (e.keyCode == 'e' && e.stateMask == SWT.MOD1)
                {
                    Account account = (Account) transactions.getData(Account.class.toString());
                    AccountTransaction transaction = (AccountTransaction) ((IStructuredSelection) transactions
                                    .getSelection()).getFirstElement();

                    if (account != null && transaction != null)
                        createEditAction(account, transaction).run();
                }
            }
        });
    }

    private void fillTransactionsContextMenu(IMenuManager manager) // NOSONAR
    {
        Account account = (Account) transactions.getData(Account.class.toString());
        if (account == null)
            return;

        AccountTransaction transaction = (AccountTransaction) ((IStructuredSelection) transactions.getSelection())
                        .getFirstElement();

        if (transaction != null)
        {
            Action action = createEditAction(account, transaction);
            action.setAccelerator(SWT.MOD1 | 'E');
            manager.add(action);
            manager.add(new Separator());
        }

        accountMenu.menuAboutToShow(manager, account, transaction != null ? transaction.getSecurity() : null);

        if (transaction != null)
        {
            manager.add(new Separator());
            manager.add(new Action(Messages.AccountMenuDeleteTransaction)
            {
                @Override
                public void run()
                {
                    Object[] selection = ((IStructuredSelection) transactions.getSelection()).toArray();
                    Account account = (Account) transactions.getData(Account.class.toString());

                    if (selection == null || selection.length == 0 || account == null)
                        return;

                    for (Object transaction : selection)
                        account.deleteTransaction((AccountTransaction) transaction, getClient());

                    markDirty();
                    transaction2balance.clear();
                    updateBalance(account);
                    accounts.refresh();
                    transactions.setInput(account.getTransactions());
                }
            });
        }
    }

    private Action createEditAction(Account account, AccountTransaction transaction)
    {
        // buy / sell
        if (transaction.getCrossEntry() instanceof BuySellEntry)
        {
            BuySellEntry entry = (BuySellEntry) transaction.getCrossEntry();
            return new OpenDialogAction(this, Messages.MenuEditTransaction)
                            .type(SecurityTransactionDialog.class, d -> d.setBuySellEntry(entry))
                            .parameters(entry.getPortfolioTransaction().getType());
        }
        else if (transaction.getCrossEntry() instanceof AccountTransferEntry)
        {
            AccountTransferEntry entry = (AccountTransferEntry) transaction.getCrossEntry();
            return new OpenDialogAction(this, Messages.MenuEditTransaction) //
                            .type(AccountTransferDialog.class, d -> d.setEntry(entry));
        }
        else
        {
            return new OpenDialogAction(this, Messages.MenuEditTransaction) //
                            .type(AccountTransactionDialog.class, d -> d.setTransaction(account, transaction)) //
                            .parameters(transaction.getType());
        }
    }

    private Control createAccountBalanceChart(Composite parent)
    {
        accountBalanceChart = new TimelineChart(parent);
        accountBalanceChart.getTitle().setVisible(false);

        return accountBalanceChart;
    }

    private void updateOnAccountSelected(Account account)
    {
        updateBalance(account);
        updateChart(account);
    }

    private void updateBalance(Account account)
    {
        transaction2balance.clear();
        if (account == null)
            return;

        List<AccountTransaction> tx = new ArrayList<>(account.getTransactions());
        Collections.sort(tx, new AccountTransaction.ByDateAmountTypeAndHashCode());

        MutableMoney balance = MutableMoney.of(account.getCurrencyCode());
        for (AccountTransaction t : tx)
        {
            switch (t.getType())
            {
                case DEPOSIT:
                case INTEREST:
                case DIVIDENDS:
                case TAX_REFUND:
                case SELL:
                case TRANSFER_IN:
                case FEES_REFUND:
                    balance.add(t.getMonetaryAmount());
                    break;
                case REMOVAL:
                case FEES:
                case INTEREST_CHARGE:
                case TAXES:
                case BUY:
                case TRANSFER_OUT:
                    balance.subtract(t.getMonetaryAmount());
                    break;
                default:
                    throw new IllegalArgumentException();
            }

            transaction2balance.put(t, balance.toMoney());
        }
    }

    private void updateChart(Account account)
    {
        try
        {
            accountBalanceChart.suspendUpdate(true);

            for (ISeries s : accountBalanceChart.getSeriesSet().getSeries())
                accountBalanceChart.getSeriesSet().deleteSeries(s.getId());

            if (account == null)
                return;

            List<AccountTransaction> tx = account.getTransactions();
            if (tx.isEmpty())
                return;

            CurrencyConverter converter = new CurrencyConverterImpl(factory, account.getCurrencyCode());
            Collections.sort(tx, new Transaction.ByDate());

            LocalDate now = LocalDate.now();
            LocalDate start = tx.get(0).getDateTime().toLocalDate();
            LocalDate end = tx.get(tx.size() - 1).getDateTime().toLocalDate();
            if (now.isAfter(end))
                end = now;
            if (now.isBefore(start))
                start = now;

            int days = (int) ChronoUnit.DAYS.between(start, end) + 2;

            LocalDate[] dates = new LocalDate[days];
            double[] values = new double[days];

            dates[0] = start.minusDays(1);
            values[0] = 0d;

            for (int ii = 1; ii < dates.length; ii++)
            {
                values[ii] = AccountSnapshot.create(account, converter, start) //
                                .getFunds().getAmount() / Values.Amount.divider();
                dates[ii] = start;
                start = start.plusDays(1);
            }

            accountBalanceChart.addDateSeries(dates, values, Colors.CASH, account.getName());
            accountBalanceChart.adjustRange();
        }
        finally
        {
            accountBalanceChart.suspendUpdate(false);
        }
    }

}
