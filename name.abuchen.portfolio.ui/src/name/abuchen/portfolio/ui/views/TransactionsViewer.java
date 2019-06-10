package name.abuchen.portfolio.ui.views;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import javax.inject.Inject;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerFilter;
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
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionOwner;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.transactions.AccountTransactionDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.AccountTransferDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.OpenDialogAction;
import name.abuchen.portfolio.ui.dialogs.transactions.SecurityTransactionDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.SecurityTransferDialog;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.selection.SecuritySelection;
import name.abuchen.portfolio.ui.selection.SelectionService;
import name.abuchen.portfolio.ui.util.BookmarkMenu;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.SimpleAction;
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
import name.abuchen.portfolio.util.TextUtil;

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
        public String getText(Object element)
        {
            return label.apply(((TransactionPair<?>) element).getTransaction());
        }

        @Override
        public Color getForeground(Object element)
        {
            if (marked.contains(element))
                return Colors.BLACK;

            Transaction tx = ((TransactionPair<?>) element).getTransaction();

            if (tx instanceof PortfolioTransaction)
            {
                PortfolioTransaction t = (PortfolioTransaction) tx;
                return t.getType().isLiquidation() ? Colors.DARK_RED : Colors.DARK_GREEN;
            }
            else if (tx instanceof AccountTransaction)
            {
                AccountTransaction t = (AccountTransaction) tx;
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

    @Inject
    private SelectionService selectionService;

    private AbstractFinanceView owner;
    private Set<TransactionPair<?>> marked = new HashSet<>();

    private TableViewer tableViewer;
    private ShowHideColumnHelper support;

    private boolean fullContextMenu = true;
    private Menu contextMenu;

    public TransactionsViewer(Composite parent, AbstractFinanceView owner)
    {
        this(TransactionsViewer.class.getSimpleName() + "3", parent, owner); //$NON-NLS-1$
    }

    public TransactionsViewer(String identifier, Composite parent, AbstractFinanceView owner)
    {
        this.owner = owner;

        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        tableViewer = new TableViewer(container, SWT.FULL_SELECTION | SWT.MULTI);
        ColumnViewerToolTipSupport.enableFor(tableViewer, ToolTip.NO_RECREATE);
        ColumnEditingSupport.prepare(tableViewer);

        support = new ShowHideColumnHelper(identifier, owner.getPreferenceStore(), tableViewer, layout);

        addColumns();
        support.createColumns();

        tableViewer.getTable().setHeaderVisible(true);
        tableViewer.getTable().setLinesVisible(true);
        tableViewer.setContentProvider(ArrayContentProvider.getInstance());

        tableViewer.addSelectionChangedListener(event -> {
            if (event.getSelection().isEmpty())
                return;

            TransactionPair<?> tx = (TransactionPair<?>) event.getStructuredSelection().getFirstElement();
            if (tx.getTransaction().getSecurity() != null)
            {
                selectionService.setSelection(
                                new SecuritySelection(owner.getClient(), tx.getTransaction().getSecurity()));
            }
        });

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

    public void markTransactions(List<TransactionPair<?>> transactions)
    {
        marked.addAll(transactions);
    }

    public void addFilter(ViewerFilter filter)
    {
        this.tableViewer.addFilter(filter);
    }

    public void setInput(List<TransactionPair<?>> transactions)
    {
        // preserve selection when (updating with) new transactions

        ISelection selection = tableViewer.getSelection();
        this.tableViewer.setInput(transactions);
        this.tableViewer.setSelection(selection);
    }

    public void refresh()
    {
        tableViewer.refresh();
    }

    public void refresh(boolean updateLabels)
    {
        try
        {
            tableViewer.getControl().setRedraw(false);
            tableViewer.refresh(updateLabels);
            tableViewer.setSelection(tableViewer.getSelection());
        }
        finally
        {
            tableViewer.getControl().setRedraw(true);
        }
    }

    @Override
    public void onModified(Object element, Object newValue, Object oldValue)
    {
        TransactionPair<?> t = (TransactionPair<?>) element;
        if (t.getTransaction().getCrossEntry() != null)
            t.getTransaction().getCrossEntry().updateFrom(t.getTransaction());

        owner.markDirty();
    }

    private void addColumns()
    {
        Column column = new Column("0", Messages.ColumnDate, SWT.None, 80); //$NON-NLS-1$
        column.setLabelProvider(new TransactionLabelProvider(t -> Values.DateTime.format(t.getDateTime())));
        ColumnViewerSorter.create(e -> ((TransactionPair<?>) e).getTransaction().getDateTime()).attachTo(column,
                        SWT.UP);
        new DateTimeEditingSupport(Transaction.class, "dateTime").addListener(this).attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column("1", Messages.ColumnTransactionType, SWT.None, 80); //$NON-NLS-1$
        column.setLabelProvider(new TransactionLabelProvider(t -> {
            if (t instanceof PortfolioTransaction)
                return ((PortfolioTransaction) t).getType().toString();
            else if (t instanceof AccountTransaction)
                return ((AccountTransaction) t).getType().toString();
            else
                return null;
        }));
        ColumnViewerSorter.create(e -> {
            Transaction t = ((TransactionPair<?>) e).getTransaction();
            if (t instanceof PortfolioTransaction)
                return ((PortfolioTransaction) t).getType().toString();
            else if (t instanceof AccountTransaction)
                return ((AccountTransaction) t).getType().toString();
            else
                return null;
        }).attachTo(column);
        new TransactionTypeEditingSupport(owner.getClient()).addListener(this).attachTo(column);
        support.addColumn(column);

        column = new Column("2", Messages.ColumnSecurity, SWT.None, 250); //$NON-NLS-1$
        column.setLabelProvider(
                        new TransactionLabelProvider(t -> t.getSecurity() != null ? t.getSecurity().getName() : null));
        ColumnViewerSorter.create(e -> {
            Security s = ((TransactionPair<?>) e).getTransaction().getSecurity();
            return s != null ? s.getName() : null;
        }).attachTo(column);
        support.addColumn(column);

        column = new Column("3", Messages.ColumnShares, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setLabelProvider(new SharesLabelProvider() // NOSONAR
        {
            private TransactionLabelProvider colors = new TransactionLabelProvider(t -> null);

            @Override
            public Long getValue(Object element)
            {
                long shares = ((TransactionPair<?>) element).getTransaction().getShares();
                return shares != 0 ? Long.valueOf(shares) : null;
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
        ColumnViewerSorter.create(e -> ((TransactionPair<?>) e).getTransaction().getShares()).attachTo(column);
        new ValueEditingSupport(Transaction.class, "shares", Values.Share) //$NON-NLS-1$
                        .setCanEditCheck(e -> ((TransactionPair<?>) e).getTransaction() instanceof PortfolioTransaction
                                        || (((TransactionPair<?>) e).getTransaction() instanceof AccountTransaction
                                                        && ((AccountTransaction) ((TransactionPair<?>) e)
                                                                        .getTransaction())
                                                                                        .getType() == AccountTransaction.Type.DIVIDENDS))
                        .addListener(this).attachTo(column);
        support.addColumn(column);

        column = new Column("4", Messages.ColumnQuote, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setLabelProvider(new TransactionLabelProvider(t -> {
            if (t instanceof PortfolioTransaction)
                return t.getShares() != 0 ? Values.Quote.format(((PortfolioTransaction) t).getGrossPricePerShare(),
                                owner.getClient().getBaseCurrency()) : null;
            else
                return null;
        }));
        ColumnViewerSorter.create(e -> {
            Transaction tx = ((TransactionPair<?>) e).getTransaction();
            return tx instanceof PortfolioTransaction ? ((PortfolioTransaction) tx).getGrossPricePerShare() : null;
        }).attachTo(column);
        support.addColumn(column);

        column = new Column("5", Messages.ColumnAmount, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setLabelProvider(new TransactionLabelProvider(t -> {
            Money m;
            if (t instanceof PortfolioTransaction)
                m = ((PortfolioTransaction) t).getGrossValue();
            else
                m = ((AccountTransaction) t).getGrossValue();
            return Values.Money.format(m, owner.getClient().getBaseCurrency());
        }));
        ColumnViewerSorter.create(e -> {
            Transaction tx = ((TransactionPair<?>) e).getTransaction();
            return tx instanceof PortfolioTransaction ? ((PortfolioTransaction) tx).getGrossValue() : null;
        }).attachTo(column);
        support.addColumn(column);

        column = new Column("6", Messages.ColumnFees, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setLabelProvider(new TransactionLabelProvider(t -> Values.Money
                        .formatNonZero(t.getUnitSum(Transaction.Unit.Type.FEE), owner.getClient().getBaseCurrency())));
        ColumnViewerSorter.create(e -> ((TransactionPair<?>) e).getTransaction().getUnitSum(Transaction.Unit.Type.FEE))
                        .attachTo(column);
        support.addColumn(column);

        column = new Column("7", Messages.ColumnTaxes, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setLabelProvider(new TransactionLabelProvider(t -> Values.Money
                        .formatNonZero(t.getUnitSum(Transaction.Unit.Type.TAX), owner.getClient().getBaseCurrency())));
        ColumnViewerSorter.create(e -> ((TransactionPair<?>) e).getTransaction().getUnitSum(Transaction.Unit.Type.TAX))
                        .attachTo(column);
        support.addColumn(column);

        column = new Column("8", Messages.ColumnNetValue, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setLabelProvider(new TransactionLabelProvider(
                        t -> Values.Money.format(t.getMonetaryAmount(), owner.getClient().getBaseCurrency())));
        ColumnViewerSorter.create(e -> ((TransactionPair<?>) e).getTransaction().getMonetaryAmount()).attachTo(column);
        support.addColumn(column);

        column = new Column("account", Messages.ColumnAccount, SWT.None, 120); //$NON-NLS-1$
        column.setLabelProvider(new TransactionLabelProvider(t -> null) // NOSONAR
        {
            @Override
            public String getText(Object element)
            {
                return ((TransactionPair<?>) element).getOwner().toString();
            }

            @Override
            public Image getImage(Object element)
            {
                TransactionOwner<?> txo = ((TransactionPair<?>) element).getOwner();

                if (txo instanceof Portfolio)
                    return Images.PORTFOLIO.image();
                else if (txo instanceof Account)
                    return Images.ACCOUNT.image();
                else
                    return null;
            }
        });
        new TransactionOwnerListEditingSupport(owner.getClient(), TransactionOwnerListEditingSupport.EditMode.OWNER)
                        .addListener(this).attachTo(column);
        ColumnViewerSorter.create(e -> ((TransactionPair<?>) e).getOwner().toString()).attachTo(column);
        support.addColumn(column);

        column = new Column("9", Messages.ColumnOffsetAccount, SWT.None, 120); //$NON-NLS-1$
        column.setLabelProvider(new TransactionLabelProvider(
                        t -> t.getCrossEntry() != null ? t.getCrossEntry().getCrossOwner(t).toString() : null));
        new TransactionOwnerListEditingSupport(owner.getClient(),
                        TransactionOwnerListEditingSupport.EditMode.CROSSOWNER).addListener(this).attachTo(column);
        ColumnViewerSorter.create(e -> {
            Transaction t = ((TransactionPair<?>) e).getTransaction();
            return t.getCrossEntry() != null ? t.getCrossEntry().getCrossOwner(t).toString() : null;
        }).attachTo(column);
        support.addColumn(column);

        column = new Column("10", Messages.ColumnNote, SWT.None, 200); //$NON-NLS-1$
        column.setLabelProvider(new TransactionLabelProvider(Transaction::getNote) // NOSONAR
        {
            @Override
            public Image getImage(Object e)
            {
                String note = getText(e);
                return note != null && !note.isEmpty() ? Images.NOTE.image() : null;
            }

            @Override
            public String getToolTipText(Object e)
            {
                String note = getText(e);
                return note == null || note.isEmpty() ? null : TextUtil.wordwrap(getText(e));
            }
        });
        ColumnViewerSorter.create(e -> ((TransactionPair<?>) e).getTransaction().getNote()).attachTo(column); // $NON-NLS-1$
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
                    if (selection.isEmpty())
                        return;

                    TransactionPair<?> tx = (TransactionPair<?>) selection.getFirstElement();
                    tx.withAccountTransaction().ifPresent(t -> createEditAccountTransactionAction(t).run());
                    tx.withPortfolioTransaction().ifPresent(t -> createEditPortfolioTransactionAction(t).run());
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
        {
            new SecurityContextMenu(owner).menuAboutToShow(manager, null, null);
        }

        if (selection.size() == 1)
        {
            TransactionPair<?> tx = (TransactionPair<?>) selection.getFirstElement();

            tx.withAccountTransaction().ifPresent(t -> fillContextMenuAccountTx(manager, t));
            tx.withPortfolioTransaction().ifPresent(t -> fillContextMenuPortfolioTx(manager, t));

            manager.add(new Separator());
        }

        if (!selection.isEmpty())
        {
            manager.add(new SimpleAction(Messages.MenuTransactionDelete, a -> {
                for (Object tx : selection.toArray())
                    ((TransactionPair<?>) tx).deleteTransaction(owner.getClient());

                owner.markDirty();
            }));
        }
    }

    private void fillContextMenuAccountTx(IMenuManager manager, TransactionPair<AccountTransaction> tx)
    {
        Action action = createEditAccountTransactionAction(tx);
        action.setAccelerator(SWT.MOD1 | 'E');
        manager.add(action);

        if (fullContextMenu)
        {
            manager.add(new Separator());
            new AccountContextMenu(owner).menuAboutToShow(manager, (Account) tx.getOwner(),
                            tx.getTransaction().getSecurity());
        }
    }

    private void fillContextMenuPortfolioTx(IMenuManager manager, TransactionPair<PortfolioTransaction> tx)
    {
        PortfolioTransaction ptx = tx.getTransaction();

        Action editAction = createEditPortfolioTransactionAction(tx);
        editAction.setAccelerator(SWT.MOD1 | 'E');
        manager.add(editAction);
        manager.add(new Separator());

        if (fullContextMenu && (ptx.getType() == PortfolioTransaction.Type.BUY
                        || ptx.getType() == PortfolioTransaction.Type.SELL))
        {
            manager.add(new ConvertBuySellToDeliveryAction(owner.getClient(), tx));
            manager.add(new Separator());
        }

        if (fullContextMenu && (ptx.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND
                        || ptx.getType() == PortfolioTransaction.Type.DELIVERY_OUTBOUND))
        {
            manager.add(new ConvertDeliveryToBuySellAction(owner.getClient(), tx));
            manager.add(new Separator());
        }

        if (fullContextMenu)
            new SecurityContextMenu(owner).menuAboutToShow(manager, ptx.getSecurity(), (Portfolio) tx.getOwner());
        else
            manager.add(new BookmarkMenu(owner.getPart(), ptx.getSecurity()));
    }

    private Action createEditAccountTransactionAction(TransactionPair<AccountTransaction> tx)
    {
        // buy / sell
        if (tx.getTransaction().getCrossEntry() instanceof BuySellEntry)
        {
            BuySellEntry entry = (BuySellEntry) tx.getTransaction().getCrossEntry();
            return new OpenDialogAction(this.owner, Messages.MenuEditTransaction)
                            .type(SecurityTransactionDialog.class, d -> d.setBuySellEntry(entry))
                            .parameters(entry.getPortfolioTransaction().getType());
        }
        else if (tx.getTransaction().getCrossEntry() instanceof AccountTransferEntry)
        {
            AccountTransferEntry entry = (AccountTransferEntry) tx.getTransaction().getCrossEntry();
            return new OpenDialogAction(this.owner, Messages.MenuEditTransaction) //
                            .type(AccountTransferDialog.class, d -> d.setEntry(entry));
        }
        else
        {
            return new OpenDialogAction(this.owner, Messages.MenuEditTransaction) //
                            .type(AccountTransactionDialog.class,
                                            d -> d.setTransaction((Account) tx.getOwner(), tx.getTransaction())) //
                            .parameters(tx.getTransaction().getType());
        }
    }

    private Action createEditPortfolioTransactionAction(TransactionPair<PortfolioTransaction> tx)
    {
        // buy / sell
        if (tx.getTransaction().getCrossEntry() instanceof BuySellEntry)
        {
            BuySellEntry entry = (BuySellEntry) tx.getTransaction().getCrossEntry();
            return new OpenDialogAction(this.owner, Messages.MenuEditTransaction)
                            .type(SecurityTransactionDialog.class, d -> d.setBuySellEntry(entry))
                            .parameters(entry.getPortfolioTransaction().getType());
        }
        else if (tx.getTransaction().getCrossEntry() instanceof PortfolioTransferEntry)
        {
            PortfolioTransferEntry entry = (PortfolioTransferEntry) tx.getTransaction().getCrossEntry();
            return new OpenDialogAction(this.owner, Messages.MenuEditTransaction) //
                            .type(SecurityTransferDialog.class, d -> d.setEntry(entry));
        }
        else
        {
            return new OpenDialogAction(this.owner, Messages.MenuEditTransaction) //
                            .type(SecurityTransactionDialog.class, d -> d.setDeliveryTransaction(tx)) //
                            .parameters(tx.getTransaction().getType());
        }
    }
}
