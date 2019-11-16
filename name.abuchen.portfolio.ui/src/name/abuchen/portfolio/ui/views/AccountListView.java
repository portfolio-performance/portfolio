package name.abuchen.portfolio.ui.views;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
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
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransaction.Type;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MutableMoney;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.transactions.AccountTransactionDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.AccountTransferDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.OpenDialogAction;
import name.abuchen.portfolio.ui.dialogs.transactions.SecurityTransactionDialog;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.ConfirmAction;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport.ModificationListener;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.DateTimeEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.SharesLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.viewers.TransactionOwnerListEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.TransactionTypeEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ValueEditingSupport;
import name.abuchen.portfolio.ui.views.columns.AttributeColumn;
import name.abuchen.portfolio.ui.views.columns.CurrencyColumn;
import name.abuchen.portfolio.ui.views.columns.CurrencyColumn.CurrencyEditingSupport;
import name.abuchen.portfolio.ui.views.columns.IsinColumn;
import name.abuchen.portfolio.ui.views.columns.NameColumn;
import name.abuchen.portfolio.ui.views.columns.NameColumn.NameColumnLabelProvider;
import name.abuchen.portfolio.ui.views.columns.NoteColumn;
import name.abuchen.portfolio.ui.views.columns.SymbolColumn;
import name.abuchen.portfolio.ui.views.columns.WknColumn;

public class AccountListView extends AbstractListView implements ModificationListener
{
    private static final String FILTER_INACTIVE_ACCOUNTS = "filter-redired-accounts"; //$NON-NLS-1$

    private TableViewer accounts;
    private TableViewer transactions;
    private AccountBalanceChart accountBalanceChart;

    @Inject
    private ExchangeRateProviderFactory exchangeRateProviderFactory;

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

    @Override
    protected String getDefaultTitle()
    {
        return Messages.LabelAccounts;
    }

    @PostConstruct
    public void setup()
    {
        isFiltered = getPreferenceStore().getBoolean(FILTER_INACTIVE_ACCOUNTS);
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
    protected void addButtons(ToolBarManager manager)
    {
        addNewButton(manager);
        addFilterButton(manager);
        addConfigButton(manager);
    }

    private void addNewButton(ToolBarManager manager)
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

        manager.add(new DropDown(Messages.MenuCreateAccountOrTransaction, Images.PLUS, SWT.NONE,
                        menuListener -> {
                            menuListener.add(new SimpleAction(Messages.AccountMenuAdd, newAccountAction));

                            menuListener.add(new Separator());

                            Account account = (Account) accounts.getStructuredSelection().getFirstElement();
                            new AccountContextMenu(AccountListView.this).menuAboutToShow(menuListener, account, null);
                        }));
    }

    private void addFilterButton(ToolBarManager manager)
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
        manager.add(filter);
    }

    private void addConfigButton(final ToolBarManager manager)
    {
        manager.add(new DropDown(Messages.MenuShowHideColumns, Images.CONFIG, SWT.NONE, mm -> {
            MenuManager m = new MenuManager(Messages.LabelAccounts);
            accountColumns.menuAboutToShow(m);
            mm.add(m);

            m = new MenuManager(Messages.LabelTransactions);
            transactionsColumns.menuAboutToShow(m);
            mm.add(m);
        }));
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
        ColumnViewerToolTipSupport.enableFor(accounts, ToolTip.NO_RECREATE);

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

        column = new Column("1", Messages.ColumnBalance, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setDescription(Messages.ColumnBalance_Description);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return Values.Amount.format(((Account) e).getCurrentAmount(LocalDateTime.now().with(LocalTime.MAX)));
            }
        });
        ColumnViewerSorter.create(o -> ((Account) o).getCurrentAmount(LocalDateTime.now().with(LocalTime.MAX)))
                        .attachTo(column);
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

        addAttributeColumns(accountColumns);

        accountColumns.createColumns();

        accounts.getTable().setHeaderVisible(true);
        accounts.getTable().setLinesVisible(true);

        accounts.setContentProvider(ArrayContentProvider.getInstance());
        resetInput();
        accounts.refresh();

        hookContextMenu(accounts.getTable(), this::fillAccountsContextMenu);
    }

    private void addAttributeColumns(ShowHideColumnHelper support)
    {
        getClient().getSettings() //
                        .getAttributeTypes() //
                        .filter(a -> a.supports(Account.class)) //
                        .forEach(attribute -> {
                            Column column = new AttributeColumn(attribute);
                            column.setVisible(false);
                            column.getEditingSupport().addListener(this);
                            support.addColumn(column);
                        });
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

        manager.add(new ConfirmAction(Messages.AccountMenuDelete,
                        MessageFormat.format(Messages.AccountMenuDeleteConfirm, account.getName()), //
                        a -> {
                            getClient().removeAccount(account);
                            markDirty();
                            resetInput();
                        }));
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
        accountBalanceChart = new AccountBalanceChart(folder);
        folder.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> accountBalanceChart.updateChart(
                        (Account) accounts.getStructuredSelection().getFirstElement(), exchangeRateProviderFactory)));
        item.setControl(accountBalanceChart);

        accounts.addSelectionChangedListener(event -> {
            Account account = (Account) ((IStructuredSelection) event.getSelection()).getFirstElement();

            updateOnAccountSelected(account);

            transactions.setData(Account.class.toString(), account);
            transactions.setInput(account != null ? account.getTransactions() : new ArrayList<AccountTransaction>(0));
            transactions.refresh();
        });

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

        Column column = new Column("0", Messages.ColumnDate, SWT.None, 80); //$NON-NLS-1$
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

        column = new Column("1", Messages.ColumnTransactionType, SWT.None, 100); //$NON-NLS-1$
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
        new TransactionTypeEditingSupport(getClient()).addListener(this).attachTo(column);
        transactionsColumns.addColumn(column);

        column = new Column("2", Messages.ColumnAmount, SWT.RIGHT, 80); //$NON-NLS-1$
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
        column.setSorter(ColumnViewerSorter.create((o1, o2) -> {
            AccountTransaction accountTransaction1 = (AccountTransaction) o1;
            long transactionAmount1 = accountTransaction1.getAmount();
            if (accountTransaction1.getType().isDebit())
                transactionAmount1 = -transactionAmount1;
            AccountTransaction accountTransaction2 = (AccountTransaction) o2;
            long transactionAmount2 = accountTransaction2.getAmount();
            if (accountTransaction2.getType().isDebit())
                transactionAmount2 = -transactionAmount2;
            return Long.compare(transactionAmount1, transactionAmount2);
        }));
        transactionsColumns.addColumn(column);

        column = new Column("3", Messages.Balance, SWT.RIGHT, 80); //$NON-NLS-1$
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

        column = new Column("4", Messages.ColumnSecurity, SWT.None, 250); //$NON-NLS-1$
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

        column = new IsinColumn();
        column.setVisible(false);
        column.getEditingSupport().addListener(this);
        transactionsColumns.addColumn(column);

        column = new SymbolColumn();
        column.setVisible(false);
        column.getEditingSupport().addListener(this);
        transactionsColumns.addColumn(column);

        column = new WknColumn();
        column.setVisible(false);
        column.getEditingSupport().addListener(this);
        transactionsColumns.addColumn(column);

        column = new Column("5", Messages.ColumnShares, SWT.RIGHT, 80); //$NON-NLS-1$
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

        column = new Column("6", Messages.ColumnPerShare, SWT.RIGHT, 80); //$NON-NLS-1$
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
                    long perShare = Math.round(t.getGrossValueAmount() * Values.Share.divider()
                                    * Values.Quote.factorToMoney() / t.getShares());
                    return Values.Quote.format(Quote.of(t.getCurrencyCode(), perShare), getClient().getBaseCurrency());
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

        column = new Column("7", Messages.ColumnOffsetAccount, SWT.None, 120); //$NON-NLS-1$
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
        new TransactionOwnerListEditingSupport(getClient(), TransactionOwnerListEditingSupport.EditMode.CROSSOWNER)
                        .addListener(this).attachTo(column);
        transactionsColumns.addColumn(column);

        column = new NoteColumn("8"); //$NON-NLS-1$
        column.getEditingSupport().addListener(this);
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
        return t.getType().isDebit() ? Colors.DARK_RED : Colors.DARK_GREEN;
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

    private void updateOnAccountSelected(Account account)
    {
        updateBalance(account);
        accountBalanceChart.updateChart(account, exchangeRateProviderFactory);
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
}
