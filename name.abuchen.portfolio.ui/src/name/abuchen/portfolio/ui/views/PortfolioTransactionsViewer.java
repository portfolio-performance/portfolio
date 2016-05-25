package name.abuchen.portfolio.ui.views;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;

import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.CrossEntry;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionOwner;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.transactions.OpenDialogAction;
import name.abuchen.portfolio.ui.dialogs.transactions.SecurityTransactionDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.SecurityTransferDialog;
import name.abuchen.portfolio.ui.util.BookmarkMenu;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport.ModificationListener;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.DateEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.SharesLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.viewers.SimpleListContentProvider;
import name.abuchen.portfolio.ui.util.viewers.StringEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ValueEditingSupport;

public final class PortfolioTransactionsViewer implements ModificationListener
{
    private class TransactionLabelProvider extends ColumnLabelProvider
    {
        private Color warningColor = new Color(Display.getDefault(), Colors.WARNING.swt());

        @Override
        public Color getForeground(Object element)
        {
            if (marked.contains(element))
                return Display.getDefault().getSystemColor(SWT.COLOR_BLACK);

            PortfolioTransaction t = (PortfolioTransaction) element;

            if (t.getType() == Type.SELL || t.getType() == Type.TRANSFER_OUT || t.getType() == Type.DELIVERY_OUTBOUND)
                return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_RED);
            else
                return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN);
        }

        @Override
        public Color getBackground(Object element)
        {
            return marked.contains(element) ? warningColor : null;
        }

        @Override
        public void dispose()
        {
            warningColor.dispose();
            super.dispose();
        }
    }

    private static class ConvertToDeliveryAction extends Action
    {
        private final AbstractFinanceView owner;
        private final PortfolioTransaction transaction;

        public ConvertToDeliveryAction(AbstractFinanceView owner, PortfolioTransaction transaction)
        {
            this.owner = owner;
            this.transaction = transaction;

            if (transaction.getType() != PortfolioTransaction.Type.BUY
                            && transaction.getType() != PortfolioTransaction.Type.SELL)
                throw new IllegalArgumentException();

            setText(transaction.getType() == PortfolioTransaction.Type.BUY ? Messages.MenuConvertToInboundDelivery
                            : Messages.MenuConvertToOutboundDelivery);
        }

        @Override
        public void run()
        {
            // delete existing transaction
            CrossEntry source = transaction.getCrossEntry();
            @SuppressWarnings("unchecked")
            TransactionOwner<Transaction> portfolio = (TransactionOwner<Transaction>) source.getOwner(transaction);
            portfolio.deleteTransaction(transaction, owner.getClient());

            // create new delivery
            PortfolioTransaction delivery = new PortfolioTransaction();
            delivery.setType(transaction.getType() == PortfolioTransaction.Type.BUY
                            ? PortfolioTransaction.Type.DELIVERY_INBOUND : PortfolioTransaction.Type.DELIVERY_OUTBOUND);
            delivery.setDate(transaction.getDate());
            delivery.setMonetaryAmount(transaction.getMonetaryAmount());
            delivery.setSecurity(transaction.getSecurity());
            delivery.setNote(transaction.getNote());
            delivery.setShares(transaction.getShares());

            transaction.getUnits().forEach(unit -> delivery.addUnit(unit));

            portfolio.addTransaction(delivery);

            owner.markDirty();
            owner.notifyModelUpdated();
        }
    }

    private AbstractFinanceView owner;
    private Portfolio portfolio;
    private Set<PortfolioTransaction> marked = new HashSet<>();

    private TableViewer tableViewer;
    private ShowHideColumnHelper support;

    private boolean fullContextMenu = true;
    private Menu contextMenu;

    public PortfolioTransactionsViewer(Composite parent, AbstractFinanceView owner)
    {
        this.owner = owner;

        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        tableViewer = new TableViewer(container, SWT.FULL_SELECTION | SWT.MULTI);
        ColumnEditingSupport.prepare(tableViewer);

        support = new ShowHideColumnHelper(PortfolioTransactionsViewer.class.getSimpleName() + "3", //$NON-NLS-1$
                        owner.getPreferenceStore(), tableViewer, layout);

        addColumns();
        support.createColumns();

        tableViewer.getTable().setHeaderVisible(true);
        tableViewer.getTable().setLinesVisible(true);
        tableViewer.setContentProvider(new SimpleListContentProvider());

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

    public void markTransactions(List<PortfolioTransaction> transactions)
    {
        marked.addAll(transactions);
    }

    public void setInput(Portfolio portfolio, List<PortfolioTransaction> transactions)
    {
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

    @Override
    public void onModified(Object element, Object newValue, Object oldValue)
    {
        PortfolioTransaction t = (PortfolioTransaction) element;
        if (t.getCrossEntry() != null)
            t.getCrossEntry().updateFrom(t);

        owner.markDirty();
        owner.notifyModelUpdated();
    }

    private void addColumns()
    {
        Column column = new Column(Messages.ColumnDate, SWT.None, 80);
        column.setLabelProvider(new TransactionLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return Values.Date.format(((PortfolioTransaction) element).getDate());
            }
        });
        ColumnViewerSorter.create(PortfolioTransaction.class, "date").attachTo(column, SWT.DOWN); //$NON-NLS-1$
        new DateEditingSupport(PortfolioTransaction.class, "date").addListener(this).attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnTransactionType, SWT.None, 80);
        column.setLabelProvider(new TransactionLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((PortfolioTransaction) element).getType().toString();
            }
        });
        ColumnViewerSorter.create(PortfolioTransaction.class, "type").attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnSecurity, SWT.None, 250);
        column.setLabelProvider(new TransactionLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                PortfolioTransaction t = (PortfolioTransaction) element;
                return t.getSecurity() != null ? String.valueOf(t.getSecurity()) : null;
            }
        });
        ColumnViewerSorter.create(PortfolioTransaction.class, "security").attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnShares, SWT.RIGHT, 80);
        column.setLabelProvider(new SharesLabelProvider()
        {
            private TransactionLabelProvider colors = new TransactionLabelProvider();

            @Override
            public Long getValue(Object element)
            {
                return ((PortfolioTransaction) element).getShares();
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
        column.setLabelProvider(new TransactionLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                PortfolioTransaction t = (PortfolioTransaction) element;
                return t.getShares() != 0
                                ? Values.Quote.format(t.getGrossPricePerShare(), owner.getClient().getBaseCurrency())
                                : null;
            }
        });
        ColumnViewerSorter.create(PortfolioTransaction.class, "grossPricePerShare").attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnAmount, SWT.RIGHT, 80);
        column.setLabelProvider(new TransactionLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return Values.Money.format(((PortfolioTransaction) element).getGrossValue(),
                                owner.getClient().getBaseCurrency());
            }
        });
        ColumnViewerSorter.create(PortfolioTransaction.class, "grossValueAmount").attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnFees, SWT.RIGHT, 80);
        column.setLabelProvider(new TransactionLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                PortfolioTransaction t = (PortfolioTransaction) element;
                return Values.Money.format(t.getUnitSum(Transaction.Unit.Type.FEE),
                                owner.getClient().getBaseCurrency());
            }
        });
        support.addColumn(column);

        column = new Column(Messages.ColumnTaxes, SWT.RIGHT, 80);
        column.setLabelProvider(new TransactionLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                PortfolioTransaction t = (PortfolioTransaction) element;
                return Values.Money.format(t.getUnitSum(Transaction.Unit.Type.TAX),
                                owner.getClient().getBaseCurrency());
            }
        });
        support.addColumn(column);

        column = new Column(Messages.ColumnNetValue, SWT.RIGHT, 80);
        column.setLabelProvider(new TransactionLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                PortfolioTransaction t = (PortfolioTransaction) element;
                return Values.Money.format(t.getMonetaryAmount(), owner.getClient().getBaseCurrency());
            }
        });
        ColumnViewerSorter.create(PortfolioTransaction.class, "amount").attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnOffsetAccount, SWT.None, 120);
        column.setLabelProvider(new TransactionLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                PortfolioTransaction t = (PortfolioTransaction) e;
                return t.getCrossEntry() != null ? t.getCrossEntry().getCrossOwner(t).toString() : null;
            }
        });
        support.addColumn(column);

        column = new Column(Messages.ColumnNote, SWT.None, 200);
        column.setLabelProvider(new TransactionLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return ((PortfolioTransaction) e).getNote();
            }

            @Override
            public Image getImage(Object e)
            {
                String note = ((PortfolioTransaction) e).getNote();
                return note != null && note.length() > 0 ? Images.NOTE.image() : null;
            }
        });
        ColumnViewerSorter.create(PortfolioTransaction.class, "note").attachTo(column); //$NON-NLS-1$
        new StringEditingSupport(PortfolioTransaction.class, "note").addListener(this).attachTo(column); //$NON-NLS-1$
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
        menuMgr.addMenuListener(manager -> fillTransactionsContextMenu(manager));

        contextMenu = menuMgr.createContextMenu(parent.getShell());
        tableViewer.getTable().setMenu(contextMenu);

        tableViewer.getTable().addDisposeListener(e -> PortfolioTransactionsViewer.this.widgetDisposed());
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
                    PortfolioTransaction transaction = (PortfolioTransaction) ((IStructuredSelection) tableViewer
                                    .getSelection()).getFirstElement();

                    if (transaction != null)
                        createEditAction(transaction).run();
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
        if (portfolio == null)
            return;

        PortfolioTransaction firstTransaction = (PortfolioTransaction) ((IStructuredSelection) tableViewer
                        .getSelection()).getFirstElement();

        if (firstTransaction != null)
        {
            Action editAction = createEditAction(firstTransaction);
            editAction.setAccelerator(SWT.MOD1 | 'E');
            manager.add(editAction);
            manager.add(new Separator());
        }

        if (fullContextMenu && firstTransaction != null && (firstTransaction.getType() == PortfolioTransaction.Type.BUY
                        || firstTransaction.getType() == PortfolioTransaction.Type.SELL))
        {
            manager.add(new ConvertToDeliveryAction(owner, firstTransaction));
            manager.add(new Separator());
        }

        if (fullContextMenu && firstTransaction != null)
            new SecurityContextMenu(owner).menuAboutToShow(manager, firstTransaction.getSecurity(), portfolio);
        else if (fullContextMenu)
            new SecurityContextMenu(owner).menuAboutToShow(manager, null, portfolio);
        else if (firstTransaction != null)
            manager.add(new BookmarkMenu(owner.getPart(), firstTransaction.getSecurity()));

        if (firstTransaction != null)
        {
            manager.add(new Separator());
            manager.add(new Action(Messages.MenuTransactionDelete)
            {
                @Override
                public void run()
                {
                    Object[] selection = ((IStructuredSelection) tableViewer.getSelection()).toArray();
                    for (Object transaction : selection)
                        portfolio.deleteTransaction((PortfolioTransaction) transaction, owner.getClient());

                    owner.markDirty();
                    owner.notifyModelUpdated();
                }
            });
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
            TransactionPair<PortfolioTransaction> pair = new TransactionPair<>(portfolio, transaction);
            return new OpenDialogAction(this.owner, Messages.MenuEditTransaction) //
                            .type(SecurityTransactionDialog.class, d -> d.setDeliveryTransaction(pair)) //
                            .parameters(transaction.getType());
        }
    }
}
