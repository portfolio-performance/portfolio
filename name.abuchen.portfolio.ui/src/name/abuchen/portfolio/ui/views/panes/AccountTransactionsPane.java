package name.abuchen.portfolio.ui.views.panes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import jakarta.inject.Inject;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransaction.Type;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CrossEntry;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.TransactionPair;
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
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.ContextMenu;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.LogoManager;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.TableViewerCSVExporter;
import name.abuchen.portfolio.ui.util.searchfilter.TransactionFilterDropDown;
import name.abuchen.portfolio.ui.util.searchfilter.TransactionSearchField;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport.ModificationListener;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.CopyPasteSupport;
import name.abuchen.portfolio.ui.util.viewers.DateTimeEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.DateTimeLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.SharesLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.viewers.TransactionOwnerListEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.TransactionTypeEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ValueEditingSupport;
import name.abuchen.portfolio.ui.views.AccountContextMenu;
import name.abuchen.portfolio.ui.views.AccountListView;
import name.abuchen.portfolio.ui.views.actions.ConvertTransferToDepositRemovalAction;
import name.abuchen.portfolio.ui.views.columns.CalculatedQuoteColumn;
import name.abuchen.portfolio.ui.views.columns.IsinColumn;
import name.abuchen.portfolio.ui.views.columns.NoteColumn;
import name.abuchen.portfolio.ui.views.columns.SymbolColumn;
import name.abuchen.portfolio.ui.views.columns.WknColumn;

public class AccountTransactionsPane implements InformationPanePage, ModificationListener
{
    @Inject
    private Client client;

    @Inject
    private AbstractFinanceView view;

    private TableViewer transactions;

    private TransactionSearchField textFilter;
    private TransactionFilterDropDown transactionFilter;

    private ShowHideColumnHelper transactionsColumns;
    private AccountContextMenu accountMenu;

    private Account account;

    /**
     * Store current balance of account after given transaction has been
     * applied. See {@link #updateBalance(Account)}. Do not store transient
     * balance in persistent AccountTransaction object.
     */
    private Map<AccountTransaction, Money> transaction2balance = new HashMap<>();

    @Inject
    public AccountTransactionsPane(IPreferenceStore preferenceStore)
    {
        transactionFilter = new TransactionFilterDropDown(preferenceStore,
                        AccountTransactionsPane.class.getSimpleName() + "-transaction-type-filter", //$NON-NLS-1$
                        criteria -> onRecalculationNeeded());

        textFilter = new TransactionSearchField(text -> onRecalculationNeeded());
    }

    @Override
    public String getLabel()
    {
        return Messages.SecurityTabTransactions;
    }

    @Override
    public void onModified(Object element, Object newValue, Object oldValue)
    {
        if (element instanceof AccountTransaction t)
        {
            if (t.getCrossEntry() != null)
                t.getCrossEntry().updateFrom(t);
            view.markDirty();
        }
    }

    @Override
    public Control createViewControl(Composite parent)
    {
        accountMenu = new AccountContextMenu(view);

        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        transactions = new TableViewer(container, SWT.FULL_SELECTION | SWT.MULTI | SWT.VIRTUAL);
        ColumnViewerToolTipSupport.enableFor(transactions, ToolTip.NO_RECREATE);
        ColumnEditingSupport.prepare(transactions);
        CopyPasteSupport.enableFor(transactions);

        // needed for virtual tables as otherwise the in-place editing does not
        // update elements properly
        transactions.setUseHashlookup(true);

        transactionsColumns = new ShowHideColumnHelper(AccountListView.class.getSimpleName() + "@bottom5", //$NON-NLS-1$
                        view.getPreferenceStore(), transactions, layout);

        Column column = new Column("0", Messages.ColumnDate, SWT.None, 80); //$NON-NLS-1$
        column.setLabelProvider(new DateTimeLabelProvider(e -> ((AccountTransaction) e).getDateTime())
        {
            @Override
            public Color getForeground(Object element)
            {
                return colorFor((AccountTransaction) element);
            }
        });
        ColumnViewerSorter.create(Transaction.BY_DATE).attachTo(column, SWT.DOWN);
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
        new TransactionTypeEditingSupport(client).addListener(this).attachTo(column);
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
                return Values.Money.format(Money.of(t.getCurrencyCode(), v), client.getBaseCurrency());
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

        column = new Column("fees", Messages.ColumnFees, SWT.RIGHT, 80); //$NON-NLS-1$
        Function<AccountTransaction, Money> getFees = tx -> {
            // fees are stored with the portfolio transaction (for example
            // purchase and sale)
            CrossEntry entry = tx.getCrossEntry();
            if (entry != null && entry.getCrossTransaction(tx) instanceof PortfolioTransaction)
                return entry.getCrossTransaction(tx).getUnitSum(Unit.Type.FEE);

            return tx.getUnitSum(Unit.Type.FEE);
        };
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return Values.Money.formatNonZero(getFees.apply((AccountTransaction) e), client.getBaseCurrency());
            }

            @Override
            public Color getForeground(Object element)
            {
                return colorFor((AccountTransaction) element);
            }
        });
        ColumnViewerSorter.create(element -> getFees.apply((AccountTransaction) element)).attachTo(column);
        column.setVisible(false);
        transactionsColumns.addColumn(column);

        column = new Column("taxes", Messages.ColumnTaxes, SWT.RIGHT, 80); //$NON-NLS-1$
        Function<AccountTransaction, Money> getTaxes = tx -> {
            // taxes are stored with the portfolio transaction (for example
            // purchase and sale)
            CrossEntry entry = tx.getCrossEntry();
            if (entry != null && entry.getCrossTransaction(tx) instanceof PortfolioTransaction)
                return entry.getCrossTransaction(tx).getUnitSum(Unit.Type.TAX);

            return tx.getUnitSum(Unit.Type.TAX);
        };
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return Values.Money.formatNonZero(getTaxes.apply((AccountTransaction) e), client.getBaseCurrency());
            }

            @Override
            public Color getForeground(Object element)
            {
                return colorFor((AccountTransaction) element);
            }
        });
        ColumnViewerSorter.create(element -> getTaxes.apply((AccountTransaction) element)).attachTo(column);
        column.setVisible(false);
        transactionsColumns.addColumn(column);

        column = new Column("3", Messages.Balance, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Money balance = transaction2balance.get(e);
                return balance != null ? Values.Money.format(balance, client.getBaseCurrency()) : null;
            }

            @Override
            public Color getForeground(Object element)
            {
                return colorFor((AccountTransaction) element);
            }
        });

        column.setSorter(ColumnViewerSorter.create((o1, o2) -> {
            Money m1 = transaction2balance.get(o1);
            Money m2 = transaction2balance.get(o2);
            return m1.compareTo(m2);
        }));
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

            @Override
            public Image getImage(Object e)
            {
                AccountTransaction t = (AccountTransaction) e;
                return LogoManager.instance().getDefaultColumnImage(t.getSecurity(), client.getSettings());
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
        column.setLabelProvider(new SharesLabelProvider() // NOSONAR
        {
            @Override
            public Long getValue(Object e)
            {
                AccountTransaction t = (AccountTransaction) e;
                if (t.getCrossEntry() instanceof BuySellEntry entry)
                {
                    return entry.getPortfolioTransaction().getShares();
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

        column = new CalculatedQuoteColumn("6", client, e -> { //$NON-NLS-1$
            AccountTransaction t = (AccountTransaction) e;
            if (t.getCrossEntry() instanceof BuySellEntry entry)
            {
                PortfolioTransaction pt = entry.getPortfolioTransaction();
                return pt.getGrossPricePerShare();
            }
            else if (t.getType() == Type.DIVIDENDS && t.getShares() != 0)
            {
                long perShare = Math.round(t.getGrossValueAmount() * Values.Share.divider()
                                * Values.Quote.factorToMoney() / t.getShares());
                return Quote.of(t.getCurrencyCode(), perShare);
            }
            else
            {
                return null;
            }
        }, element -> colorFor((AccountTransaction) element));
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

            @Override
            public Image getImage(Object e)
            {
                AccountTransaction t = (AccountTransaction) e;
                return t.getCrossEntry() != null ? LogoManager.instance()
                                .getDefaultColumnImage(t.getCrossEntry().getCrossOwner(t), client.getSettings()) : null;
            }
        });
        new TransactionOwnerListEditingSupport(client, TransactionOwnerListEditingSupport.EditMode.CROSSOWNER)
                        .addListener(this).attachTo(column);
        transactionsColumns.addColumn(column);

        column = new NoteColumn("8"); //$NON-NLS-1$
        column.getEditingSupport().addListener(this);
        transactionsColumns.addColumn(column);

        column = new Column("source", Messages.ColumnSource, SWT.None, 120); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return ((AccountTransaction) e).getSource();
            }

            @Override
            public Color getForeground(Object element)
            {
                return colorFor((AccountTransaction) element);
            }
        });
        ColumnViewerSorter.createIgnoreCase(e -> ((AccountTransaction) e).getSource()).attachTo(column); // $NON-NLS-1$
        transactionsColumns.addColumn(column);

        transactionsColumns.createColumns(true);

        transactions.getTable().setHeaderVisible(true);
        transactions.getTable().setLinesVisible(true);

        transactions.setContentProvider(ArrayContentProvider.getInstance());

        transactions.addFilter(textFilter
                        .getViewerFilter(element -> new TransactionPair<>(account, (AccountTransaction) element)));

        transactions.addFilter(new ViewerFilter()
        {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element)
            {
                var tx = (AccountTransaction) element;
                return transactionFilter.getFilterCriteria().matches(tx);
            }
        });

        new ContextMenu(transactions.getTable(), this::fillTransactionsContextMenu).hook();

        hookKeyListener();

        return container;
    }

    @Override
    public void addButtons(ToolBarManager toolBar)
    {
        toolBar.add(textFilter);

        toolBar.add(new Separator());

        transactionFilter.dispose();
        toolBar.add(transactionFilter);

        toolBar.add(new SimpleAction(Messages.MenuExportData, Images.EXPORT,
                        a -> new TableViewerCSVExporter(transactions).export(getLabel(),
                                        account != null ? account.getName() : null)));

        toolBar.add(new DropDown(Messages.MenuShowHideColumns, Images.CONFIG, SWT.NONE,
                        manager -> transactionsColumns.menuAboutToShow(manager)));

    }

    public void notifyModelUpdated()
    {
        onRecalculationNeeded();
    }

    private Color colorFor(AccountTransaction t)
    {
        return t.getType().isDebit() ? Colors.theme().redForeground() : Colors.theme().greenForeground();
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
        if (account == null)
            return;

        IStructuredSelection selection = (IStructuredSelection) transactions.getSelection();
        AccountTransaction transaction = (AccountTransaction) selection.getFirstElement();

        if (transaction != null)
        {
            Action action = createEditAction(account, transaction);
            action.setAccelerator(SWT.MOD1 | 'E');
            manager.add(action);

            manager.add(createCopyAction(account, transaction));

            manager.add(new Separator());
        }

        accountMenu.menuAboutToShow(manager, account, transaction != null ? transaction.getSecurity() : null);

        if (!selection.isEmpty())
        {
            fillTransactionsContextMenuList(manager, selection);
        }

        if (transaction != null)
        {
            manager.add(new Separator());

            manager.add(new Action(Messages.AccountMenuDeleteTransaction)
            {
                @Override
                public void run()
                {
                    Object[] selection = ((IStructuredSelection) transactions.getSelection()).toArray();

                    if (selection == null || selection.length == 0 || account == null)
                        return;

                    for (Object transaction : selection)
                        account.deleteTransaction((AccountTransaction) transaction, client);

                    view.markDirty();
                    updateBalance(account);
                    transactions.setInput(account.getTransactions());
                }
            });
        }
    }

    private void fillTransactionsContextMenuList(IMenuManager manager, IStructuredSelection selection)
    {
        // create collection with all selected transactions
        Collection<AccountTransaction> accountTxCollection = new ArrayList<>(selection.size());
        Iterator<?> it = selection.iterator();
        while (it.hasNext())
        {
            accountTxCollection.add((AccountTransaction) it.next());
        }

        // check if all transaction are transfer actions
        boolean allTransfer = true;
        for (AccountTransaction tx : accountTxCollection)
        {

            allTransfer &= tx.getType() == AccountTransaction.Type.TRANSFER_IN
                            || tx.getType() == AccountTransaction.Type.TRANSFER_OUT;
        }

        // create action to split transfer action into deposit/removal
        if (allTransfer)
        {
            manager.add(new Separator());
            manager.add(new ConvertTransferToDepositRemovalAction(client, accountTxCollection));
        }
    }

    private Action createEditAction(Account account, AccountTransaction transaction)
    {
        // buy / sell
        if (transaction.getCrossEntry() instanceof BuySellEntry entry)
        {
            return new OpenDialogAction(view, Messages.MenuEditTransaction)
                            .type(SecurityTransactionDialog.class, d -> d.setBuySellEntry(entry))
                            .parameters(entry.getPortfolioTransaction().getType());
        }
        else if (transaction.getCrossEntry() instanceof AccountTransferEntry entry)
        {
            return new OpenDialogAction(view, Messages.MenuEditTransaction) //
                            .type(AccountTransferDialog.class, d -> d.setEntry(entry));
        }
        else
        {
            return new OpenDialogAction(view, Messages.MenuEditTransaction) //
                            .type(AccountTransactionDialog.class, d -> d.setTransaction(account, transaction)) //
                            .parameters(transaction.getType());
        }
    }

    private Action createCopyAction(Account account, AccountTransaction transaction)
    {
        // buy / sell
        if (transaction.getCrossEntry() instanceof BuySellEntry entry)
        {
            return new OpenDialogAction(view, Messages.MenuDuplicateTransaction)
                            .type(SecurityTransactionDialog.class, d -> d.presetBuySellEntry(entry))
                            .parameters(entry.getPortfolioTransaction().getType());
        }
        else if (transaction.getCrossEntry() instanceof AccountTransferEntry entry)
        {
            return new OpenDialogAction(view, Messages.MenuDuplicateTransaction) //
                            .type(AccountTransferDialog.class, d -> d.presetEntry(entry));
        }
        else
        {
            return new OpenDialogAction(view, Messages.MenuDuplicateTransaction) //
                            .type(AccountTransactionDialog.class, d -> d.presetTransaction(account, transaction)) //
                            .parameters(transaction.getType());
        }
    }

    @Override
    public void setInput(Object input)
    {
        account = Adaptor.adapt(Account.class, input);

        updateBalance(account);

        if (account != null)
        {
            transactions.setInput(account.getTransactions());
        }
        else
        {
            transactions.setInput(Collections.emptyList());
        }
    }

    @Override
    public void onRecalculationNeeded()
    {
        if (account != null)
            setInput(account);
    }

    private void updateBalance(Account account)
    {
        transaction2balance.clear();
        if (account == null)
            return;

        List<AccountTransaction> tx = new ArrayList<>(account.getTransactions());
        Collections.sort(tx, Transaction.BY_DATE);

        MutableMoney balance = MutableMoney.of(account.getCurrencyCode());
        for (AccountTransaction t : tx)
        {
            if (t.getType().isCredit())
                balance.add(t.getMonetaryAmount());
            else
                balance.subtract(t.getMonetaryAmount());

            transaction2balance.put(t, balance.toMoney());
        }
    }
}
