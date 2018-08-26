package name.abuchen.portfolio.ui.views;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.transactions.AccountTransactionDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.AccountTransferDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.OpenDialogAction;
import name.abuchen.portfolio.ui.dialogs.transactions.SecurityTransactionDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.SecurityTransferDialog;
import name.abuchen.portfolio.ui.util.BookmarkMenu;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport.ModificationListener;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.DateTimeEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.SharesLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.viewers.StringEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.TransactionOwnerListEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.TransactionTypeEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ValueEditingSupport;
import name.abuchen.portfolio.ui.views.actions.ConvertBuySellToDeliveryAction;
import name.abuchen.portfolio.ui.views.actions.ConvertDeliveryToBuySellAction;

public final class TransactionsViewer implements ModificationListener
{
    private class TransactionLabelProvider extends ColumnLabelProvider
    {
        private Function<Transaction, String> label;

        public TransactionLabelProvider(Function<Transaction, String> label)
        {
            this.label = Objects.requireNonNull(label);
        }

        @Override
        public final String getText(Object element)
        {
            return label.apply((Transaction) element);
        }

        @Override
        public Color getForeground(Object element)
        {
            if (marked.contains(element))
                return Colors.BLACK;

            if (element instanceof PortfolioTransaction)
            {
                PortfolioTransaction t = (PortfolioTransaction) element;
                return t.getType().isLiquidation() ? Colors.DARK_RED : Colors.DARK_GREEN;
            }
            else if (element instanceof AccountTransaction)
            {
                AccountTransaction t = (AccountTransaction) element;
                return t.getType().isDebit() ? Colors.DARK_RED : Colors.DARK_GREEN;
            }

            throw new IllegalArgumentException();
        }

        @Override
        public Color getBackground(Object element)
        {
            return marked.contains(element) ? Colors.WARNING : null;
        }
    }

    private AbstractFinanceView owner;
    private Account account;
    private Portfolio portfolio;
    private Set<Transaction> marked = new HashSet<>();

    private TableViewer tableViewer;
    private ShowHideColumnHelper support;

    private boolean fullContextMenu = true;
    private Menu contextMenu;

    public TransactionsViewer(Composite parent, AbstractFinanceView owner)
    {
        this.owner = owner;

        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        tableViewer = new TableViewer(container, SWT.FULL_SELECTION | SWT.MULTI);
        ColumnViewerToolTipSupport.enableFor(tableViewer, ToolTip.NO_RECREATE);
        ColumnEditingSupport.prepare(tableViewer);

        support = new ShowHideColumnHelper(TransactionsViewer.class.getSimpleName() + "3", //$NON-NLS-1$
                        owner.getPreferenceStore(), tableViewer, layout);

        addColumns();
        support.createColumns();

        tableViewer.getTable().setHeaderVisible(true);
        tableViewer.getTable().setLinesVisible(true);
        tableViewer.setContentProvider(ArrayContentProvider.getInstance());

        hookContextMenu(parent);
        hookKeyListener();
    }

    public void setFullContextMenu(boolean fullContextMenu)
    {
        this.fullContextMenu = fullContextMenu;
    }

    public Control getControl()
    {
        return tableViewer.getControl().getParent();
    }

    public void markTransactions(List<Transaction> transactions)
    {
        marked.addAll(transactions);
    }

    public void setInput(Account account, Portfolio portfolio, List<? extends Transaction> transactions)
    {
        this.account = account;
        this.portfolio = portfolio;
        this.tableViewer.setInput(transactions);
    }

    public void refresh()
    {
        tableViewer.refresh();
    }

    public Portfolio getPortfolio()
    {
        return portfolio;
    }

    public Account getAccount()
    {
        return account;
    }

    @Override
    public void onModified(Object element, Object newValue, Object oldValue)
    {
        Transaction t = (Transaction) element;
        if (t.getCrossEntry() != null)
            t.getCrossEntry().updateFrom(t);

        owner.markDirty();
        owner.notifyModelUpdated();
    }

    /**
     * Returns the owner of the transaction. Because an investment plan can be
     * updated, older transactions do not necessarily belong to the account that
     * is currently configured for by the plan.
     */
    private Account lookupOwner(AccountTransaction t)
    {
        if (account != null && account.getTransactions().contains(t))
            return account;

        return owner.getClient().getAccounts().stream().filter(a -> a.getTransactions().contains(t)).findAny()
                        .orElseThrow(IllegalArgumentException::new);
    }

    /**
     * Returns the owner of the transaction. Because an investment plan can be
     * updated, older transactions do not necessarily belong to the portfolio
     * that is currently configured for the plan.
     */
    private Portfolio lookupOwner(PortfolioTransaction t)
    {
        if (portfolio != null && portfolio.getTransactions().contains(t))
            return portfolio;

        return owner.getClient().getPortfolios().stream().filter(a -> a.getTransactions().contains(t)).findAny()
                        .orElseThrow(IllegalArgumentException::new);
    }

    private void addColumns()
    {
        Column column = new Column(Messages.ColumnDate, SWT.None, 80);
        column.setLabelProvider(new TransactionLabelProvider(t -> Values.DateTime.format(t.getDateTime())));
        ColumnViewerSorter.create(Transaction.class, "dateTime").attachTo(column, SWT.DOWN); //$NON-NLS-1$
        new DateTimeEditingSupport(Transaction.class, "dateTime").addListener(this).attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnTransactionType, SWT.None, 80);
        column.setLabelProvider(new TransactionLabelProvider(t -> {
            if (t instanceof PortfolioTransaction)
                return ((PortfolioTransaction) t).getType().toString();
            else if (t instanceof AccountTransaction)
                return ((AccountTransaction) t).getType().toString();
            else
                return null;
        }));
        ColumnViewerSorter.create(PortfolioTransaction.class, "type").attachTo(column); //$NON-NLS-1$
        new TransactionTypeEditingSupport(owner.getClient()).addListener(this).attachTo(column);
        support.addColumn(column);

        column = new Column(Messages.ColumnSecurity, SWT.None, 250);
        column.setLabelProvider(new TransactionLabelProvider(t -> {
            if (t instanceof PortfolioTransaction)
                return ((PortfolioTransaction) t).getSecurity().getName();
            else
                return null;
        }));
        ColumnViewerSorter.create(PortfolioTransaction.class, "security").attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnShares, SWT.RIGHT, 80);
        column.setLabelProvider(new SharesLabelProvider()
        {
            private TransactionLabelProvider colors = new TransactionLabelProvider(t -> null);

            @Override
            public Long getValue(Object element)
            {
                if (element instanceof PortfolioTransaction)
                    return ((PortfolioTransaction) element).getShares();
                else if (element instanceof AccountTransaction)
                    return null;
                else
                    throw new IllegalArgumentException(element.toString());
            }

            @Override
            public Color getForeground(Object element)
            {
                return colors.getForeground(element);
            }

            @Override
            public Color getBackground(Object element)
            {
                return colors.getBackground(element);
            }
        });
        ColumnViewerSorter.create(PortfolioTransaction.class, "shares").attachTo(column); //$NON-NLS-1$
        new ValueEditingSupport(PortfolioTransaction.class, "shares", Values.Share).addListener(this).attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnQuote, SWT.RIGHT, 80);
        column.setLabelProvider(new TransactionLabelProvider(t -> {
            if (t instanceof PortfolioTransaction)
                return t.getShares() != 0 ? Values.Quote.format(((PortfolioTransaction) t).getGrossPricePerShare(),
                                owner.getClient().getBaseCurrency()) : null;
            else
                return null;
        }));
        ColumnViewerSorter.create(PortfolioTransaction.class, "grossPricePerShare").attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnAmount, SWT.RIGHT, 80);
        column.setLabelProvider(new TransactionLabelProvider(t -> {
            Money m;
            if (t instanceof PortfolioTransaction)
                m = ((PortfolioTransaction) t).getGrossValue();
            else
                m = t.getMonetaryAmount();
            return Values.Money.format(m, owner.getClient().getBaseCurrency());
        }));
        ColumnViewerSorter.create(PortfolioTransaction.class, "grossValueAmount").attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnFees, SWT.RIGHT, 80);
        column.setLabelProvider(new TransactionLabelProvider(t -> Values.Money
                        .formatNonZero(t.getUnitSum(Transaction.Unit.Type.FEE), owner.getClient().getBaseCurrency())));
        support.addColumn(column);

        column = new Column(Messages.ColumnTaxes, SWT.RIGHT, 80);
        column.setLabelProvider(new TransactionLabelProvider(t -> Values.Money
                        .formatNonZero(t.getUnitSum(Transaction.Unit.Type.TAX), owner.getClient().getBaseCurrency())));
        support.addColumn(column);

        column = new Column(Messages.ColumnNetValue, SWT.RIGHT, 80);
        column.setLabelProvider(new TransactionLabelProvider(
                        t -> Values.Money.format(t.getMonetaryAmount(), owner.getClient().getBaseCurrency())));
        ColumnViewerSorter.create(PortfolioTransaction.class, "amount").attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnOffsetAccount, SWT.None, 120);
        column.setLabelProvider(new TransactionLabelProvider(
                        t -> t.getCrossEntry() != null ? t.getCrossEntry().getCrossOwner(t).toString() : null));
        new TransactionOwnerListEditingSupport(owner.getClient(),
                        TransactionOwnerListEditingSupport.EditMode.CROSSOWNER).addListener(this).attachTo(column);
        support.addColumn(column);

        column = new Column(Messages.ColumnNote, SWT.None, 200);
        column.setLabelProvider(new TransactionLabelProvider(Transaction::getNote)
        {
            @Override
            public Image getImage(Object e)
            {
                String note = ((Transaction) e).getNote();
                return note != null && note.length() > 0 ? Images.NOTE.image() : null;
            }
        });
        ColumnViewerSorter.create(Transaction.class, "note").attachTo(column); //$NON-NLS-1$
        new StringEditingSupport(Transaction.class, "note").addListener(this).attachTo(column); //$NON-NLS-1$
        support.addColumn(column);
    }

    public ShowHideColumnHelper getColumnSupport()
    {
        return support;
    }

    private void hookContextMenu(Composite parent)
    {
        MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(this::fillTransactionsContextMenu);

        contextMenu = menuMgr.createContextMenu(parent.getShell());
        tableViewer.getTable().setMenu(contextMenu);

        tableViewer.getTable().addDisposeListener(e -> TransactionsViewer.this.widgetDisposed());
    }

    private void hookKeyListener()
    {
        tableViewer.getControl().addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {
                if (e.keyCode == 'e' && e.stateMask == SWT.MOD1)
                {
                    IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
                    if (selection.getFirstElement() instanceof AccountTransaction)
                    {
                        AccountTransaction transaction = (AccountTransaction) selection.getFirstElement();
                        if (transaction != null)
                            createEditAction(transaction).run();
                    }
                    else if (selection.getFirstElement() instanceof PortfolioTransaction)
                    {
                        PortfolioTransaction transaction = (PortfolioTransaction) selection.getFirstElement();
                        if (transaction != null)
                            createEditAction(transaction).run();
                    }
                }
            }
        });
    }

    private void widgetDisposed()
    {
        if (contextMenu != null)
            contextMenu.dispose();
    }

    private void fillTransactionsContextMenu(IMenuManager manager)
    {
        IStructuredSelection selection = tableViewer.getStructuredSelection();

        if (selection.isEmpty() && fullContextMenu)
            new SecurityContextMenu(owner).menuAboutToShow(manager, null, portfolio);
        else if (selection.getFirstElement() instanceof AccountTransaction)
            fillContextMenuAccountTx(manager, selection);
        else if (selection.getFirstElement() instanceof PortfolioTransaction)
            fillContextMenuPortfolioTx(manager, selection);
    }

    private void fillContextMenuAccountTx(IMenuManager manager, IStructuredSelection selection)
    {
        AccountTransaction firstTransaction = (AccountTransaction) selection.getFirstElement();

        if (selection.size() == 1)
        {
            Action action = createEditAction(firstTransaction);
            action.setAccelerator(SWT.MOD1 | 'E');
            manager.add(action);

            manager.add(new Separator());
        }

        manager.add(new Action(Messages.AccountMenuDeleteTransaction)
        {
            @Override
            public void run()
            {
                for (Object tx : selection.toArray())
                    lookupOwner((AccountTransaction) tx).deleteTransaction((AccountTransaction) tx, owner.getClient());

                owner.markDirty();
                owner.notifyModelUpdated();
            }
        });
    }

    private void fillContextMenuPortfolioTx(IMenuManager manager, IStructuredSelection selection)
    {
        PortfolioTransaction firstTransaction = (PortfolioTransaction) selection.getFirstElement();

        if (selection.size() == 1)
        {
            Action editAction = createEditAction(firstTransaction);
            editAction.setAccelerator(SWT.MOD1 | 'E');
            manager.add(editAction);
            manager.add(new Separator());

            if (fullContextMenu && (firstTransaction.getType() == PortfolioTransaction.Type.BUY
                            || firstTransaction.getType() == PortfolioTransaction.Type.SELL))
            {
                manager.add(new ConvertBuySellToDeliveryAction(owner.getClient(),
                                new TransactionPair<>(lookupOwner(firstTransaction), firstTransaction)));
                manager.add(new Separator());
            }

            if (fullContextMenu && (firstTransaction.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND
                            || firstTransaction.getType() == PortfolioTransaction.Type.DELIVERY_OUTBOUND))
            {
                manager.add(new ConvertDeliveryToBuySellAction(owner.getClient(),
                                new TransactionPair<>(lookupOwner(firstTransaction), firstTransaction)));
                manager.add(new Separator());
            }

            if (fullContextMenu)
                new SecurityContextMenu(owner).menuAboutToShow(manager, firstTransaction.getSecurity(), portfolio);
            else
                manager.add(new BookmarkMenu(owner.getPart(), firstTransaction.getSecurity()));
        }

        manager.add(new Separator());
        manager.add(new Action(Messages.MenuTransactionDelete)
        {
            @Override
            public void run()
            {
                for (Object tx : selection.toArray())
                    lookupOwner((PortfolioTransaction) tx).deleteTransaction((PortfolioTransaction) tx,
                                    owner.getClient());

                owner.markDirty();
                owner.notifyModelUpdated();
            }
        });
    }

    private Action createEditAction(AccountTransaction transaction)
    {
        // buy / sell
        if (transaction.getCrossEntry() instanceof BuySellEntry)
        {
            BuySellEntry entry = (BuySellEntry) transaction.getCrossEntry();
            return new OpenDialogAction(this.owner, Messages.MenuEditTransaction)
                            .type(SecurityTransactionDialog.class, d -> d.setBuySellEntry(entry))
                            .parameters(entry.getPortfolioTransaction().getType());
        }
        else if (transaction.getCrossEntry() instanceof AccountTransferEntry)
        {
            AccountTransferEntry entry = (AccountTransferEntry) transaction.getCrossEntry();
            return new OpenDialogAction(this.owner, Messages.MenuEditTransaction) //
                            .type(AccountTransferDialog.class, d -> d.setEntry(entry));
        }
        else
        {
            return new OpenDialogAction(this.owner, Messages.MenuEditTransaction) //
                            .type(AccountTransactionDialog.class,
                                            d -> d.setTransaction(lookupOwner(transaction), transaction)) //
                            .parameters(transaction.getType());
        }
    }

    private Action createEditAction(PortfolioTransaction transaction)
    {
        // buy / sell
        if (transaction.getCrossEntry() instanceof BuySellEntry)
        {
            BuySellEntry entry = (BuySellEntry) transaction.getCrossEntry();
            return new OpenDialogAction(this.owner, Messages.MenuEditTransaction)
                            .type(SecurityTransactionDialog.class, d -> d.setBuySellEntry(entry))
                            .parameters(entry.getPortfolioTransaction().getType());
        }
        else if (transaction.getCrossEntry() instanceof PortfolioTransferEntry)
        {
            PortfolioTransferEntry entry = (PortfolioTransferEntry) transaction.getCrossEntry();
            return new OpenDialogAction(this.owner, Messages.MenuEditTransaction) //
                            .type(SecurityTransferDialog.class, d -> d.setEntry(entry));
        }
        else
        {
            TransactionPair<PortfolioTransaction> pair = new TransactionPair<>(lookupOwner(transaction), transaction);
            return new OpenDialogAction(this.owner, Messages.MenuEditTransaction) //
                            .type(SecurityTransactionDialog.class, d -> d.setDeliveryTransaction(pair)) //
                            .parameters(transaction.getType());
        }
    }
}
