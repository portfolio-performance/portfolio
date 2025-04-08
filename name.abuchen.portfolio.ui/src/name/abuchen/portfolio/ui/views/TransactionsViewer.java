package name.abuchen.portfolio.ui.views;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import jakarta.inject.Inject;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
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

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.selection.SecuritySelection;
import name.abuchen.portfolio.ui.selection.SelectionService;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.ContextMenu;
import name.abuchen.portfolio.ui.util.LogoManager;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport.ModificationListener;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.CopyPasteSupport;
import name.abuchen.portfolio.ui.util.viewers.DateTimeEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.DateTimeLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.SharesLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.viewers.StringEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.TransactionOwnerListEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.TransactionTypeEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ValueEditingSupport;
import name.abuchen.portfolio.ui.views.columns.IsinColumn;
import name.abuchen.portfolio.ui.views.columns.SymbolColumn;
import name.abuchen.portfolio.ui.views.columns.WknColumn;
import name.abuchen.portfolio.util.TextUtil;

public final class TransactionsViewer implements ModificationListener
{
    private class TransactionLabelProvider extends ColumnLabelProvider
    {
        private Function<Transaction, String> label;
        private Function<TransactionPair<?>, Object> img;

        public TransactionLabelProvider(Function<Transaction, String> label)
        {
            this(label, null);
        }

        public TransactionLabelProvider(Function<Transaction, String> label, Function<TransactionPair<?>, Object> img)
        {
            this.label = Objects.requireNonNull(label);
            this.img = img;
        }

        public TransactionLabelProvider(ColumnLabelProvider labelProvider)
        {
            this.label = labelProvider::getText;
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

            if (tx instanceof PortfolioTransaction t)
                return t.getType().isLiquidation() ? Colors.theme().redForeground() : Colors.theme().greenForeground();
            else if (tx instanceof AccountTransaction t)
                return t.getType().isDebit() ? Colors.theme().redForeground() : Colors.theme().greenForeground();

            throw new IllegalArgumentException("unsupported transaction type " + tx); //$NON-NLS-1$
        }

        @Override
        public Image getImage(Object element)
        {
            if (img == null)
                return null;
            Object subject = img.apply((TransactionPair<?>) element);
            return LogoManager.instance().getDefaultColumnImage(subject, owner.getClient().getSettings());
        }

        @Override
        public Color getBackground(Object element)
        {
            return marked.contains(element) ? Colors.theme().warningBackground() : null;
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

        tableViewer = new TableViewer(container, SWT.FULL_SELECTION | SWT.MULTI | SWT.VIRTUAL);
        ColumnViewerToolTipSupport.enableFor(tableViewer, ToolTip.NO_RECREATE);
        ColumnEditingSupport.prepare(tableViewer);
        CopyPasteSupport.enableFor(tableViewer);

        tableViewer.setUseHashlookup(true);

        support = new ShowHideColumnHelper(identifier, owner.getPreferenceStore(), tableViewer, layout);

        addColumns();
        support.createColumns(true);

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

    public TableViewer getTableViewer()
    {
        return tableViewer;
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
        TransactionLabelProvider colors = new TransactionLabelProvider(t -> null);

        Column column = new Column("0", Messages.ColumnDate, SWT.None, 80); //$NON-NLS-1$
        column.setLabelProvider(new DateTimeLabelProvider(e -> ((TransactionPair<?>) e).getTransaction().getDateTime())
        {
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

        ColumnViewerSorter.create(TransactionPair.BY_DATE).attachTo(column, SWT.DOWN);
        new DateTimeEditingSupport(Transaction.class, "dateTime").addListener(this).attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column("1", Messages.ColumnTransactionType, SWT.None, 80); //$NON-NLS-1$
        column.setLabelProvider(new TransactionLabelProvider(t -> {
            if (t instanceof PortfolioTransaction pt)
                return pt.getType().toString();
            else if (t instanceof AccountTransaction at)
                return at.getType().toString();
            else
                return null;
        }));
        ColumnViewerSorter.createIgnoreCase(e -> {
            Transaction t = ((TransactionPair<?>) e).getTransaction();
            if (t instanceof PortfolioTransaction pt)
                return pt.getType().toString();
            else if (t instanceof AccountTransaction at)
                return at.getType().toString();
            else
                return null;
        }).attachTo(column);
        new TransactionTypeEditingSupport(owner.getClient()).addListener(this).attachTo(column);
        support.addColumn(column);

        column = new Column("2", Messages.ColumnSecurity, SWT.None, 250); //$NON-NLS-1$
        column.setLabelProvider(
                        new TransactionLabelProvider(t -> t.getSecurity() != null ? t.getSecurity().getName() : null,
                                        t -> t.getTransaction().getSecurity()));
        ColumnViewerSorter.createIgnoreCase(e -> {
            Security s = ((TransactionPair<?>) e).getTransaction().getSecurity();
            return s != null ? s.getName() : null;
        }).attachTo(column);
        support.addColumn(column);

        column = new IsinColumn();
        column.setVisible(false);
        column.setLabelProvider(new TransactionLabelProvider((ColumnLabelProvider) column.getLabelProvider().get()));
        column.getEditingSupport().addListener(this);
        support.addColumn(column);

        column = new SymbolColumn();
        column.setVisible(false);
        column.setLabelProvider(new TransactionLabelProvider((ColumnLabelProvider) column.getLabelProvider().get()));
        column.getEditingSupport().addListener(this);
        support.addColumn(column);

        column = new WknColumn();
        column.setVisible(false);
        column.setLabelProvider(new TransactionLabelProvider((ColumnLabelProvider) column.getLabelProvider().get()));
        column.getEditingSupport().addListener(this);
        support.addColumn(column);

        column = new Column("3", Messages.ColumnShares, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setLabelProvider(new SharesLabelProvider() // NOSONAR
        {
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
            if (t instanceof PortfolioTransaction pt)
                return t.getShares() != 0
                                ? Values.CalculatedQuote.format(pt.getGrossPricePerShare(),
                                                owner.getClient().getBaseCurrency())
                                : null;
            else
                return null;
        }));
        ColumnViewerSorter.create(e -> {
            Transaction tx = ((TransactionPair<?>) e).getTransaction();
            return tx instanceof PortfolioTransaction pt ? pt.getGrossPricePerShare() : null;
        }).attachTo(column);
        support.addColumn(column);

        column = new Column("5", Messages.ColumnAmount, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setLabelProvider(new TransactionLabelProvider(t -> {
            Money m;
            if (t instanceof PortfolioTransaction pt)
                m = pt.getGrossValue();
            else
                m = ((AccountTransaction) t).getGrossValue();
            return Values.Money.format(m, owner.getClient().getBaseCurrency());
        }));
        ColumnViewerSorter.create(e -> {
            Transaction tx = ((TransactionPair<?>) e).getTransaction();
            return tx instanceof PortfolioTransaction pt ? pt.getGrossValue() : null;
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
        column.setLabelProvider(new TransactionLabelProvider(t -> null, t -> t.getOwner()) // NOSONAR
        {
            @Override
            public String getText(Object element)
            {
                return ((TransactionPair<?>) element).getOwner().toString();
            }
        });
        new TransactionOwnerListEditingSupport(owner.getClient(), TransactionOwnerListEditingSupport.EditMode.OWNER)
                        .addListener(this).attachTo(column);
        ColumnViewerSorter.createIgnoreCase(e -> ((TransactionPair<?>) e).getOwner().toString()).attachTo(column);
        support.addColumn(column);

        column = new Column("9", Messages.ColumnOffsetAccount, SWT.None, 120); //$NON-NLS-1$
        column.setLabelProvider(new TransactionLabelProvider(
                        t -> t.getCrossEntry() != null ? t.getCrossEntry().getCrossOwner(t).toString() : null,
                        t -> t.getTransaction().getCrossEntry() != null
                                        ? t.getTransaction().getCrossEntry().getCrossOwner(t.getTransaction())
                                        : null));
        new TransactionOwnerListEditingSupport(owner.getClient(),
                        TransactionOwnerListEditingSupport.EditMode.CROSSOWNER).addListener(this).attachTo(column);
        ColumnViewerSorter.createIgnoreCase(e -> {
            Transaction t = ((TransactionPair<?>) e).getTransaction();
            return t.getCrossEntry() != null ? t.getCrossEntry().getCrossOwner(t).toString() : null;
        }).attachTo(column);
        support.addColumn(column);

        column = new Column("10", Messages.ColumnNote, SWT.None, 200); //$NON-NLS-1$
        column.setLabelProvider(new TransactionLabelProvider(Transaction::getNote) // NOSONAR
        {
            private String getRawText(Object e)
            {
                return ((TransactionPair<?>) e).getTransaction().getNote();
            }

            @Override
            public String getText(Object e)
            {
                String note = getRawText(e);
                return note == null || note.isEmpty() ? null : TextUtil.toSingleLine(note);
            }

            @Override
            public Image getImage(Object e)
            {
                String note = getRawText(e);
                return note != null && !note.isEmpty() ? Images.NOTE.image() : null;
            }

            @Override
            public String getToolTipText(Object e)
            {
                String note = getRawText(e);
                return note == null || note.isEmpty() ? null : TextUtil.wordwrap(note);
            }
        });
        ColumnViewerSorter.createIgnoreCase(e -> ((TransactionPair<?>) e).getTransaction().getNote()).attachTo(column); // $NON-NLS-1$
        new StringEditingSupport(Transaction.class, "note").addListener(this).attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column("source", Messages.ColumnSource, SWT.None, 200); //$NON-NLS-1$
        column.setLabelProvider(new TransactionLabelProvider(Transaction::getSource));
        ColumnViewerSorter.createIgnoreCase(e -> ((TransactionPair<?>) e).getTransaction().getSource())
                        .attachTo(column); // $NON-NLS-1$
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
        tableViewer.getTable().setData(ContextMenu.DEFAULT_MENU, contextMenu);

        tableViewer.getTable().addDisposeListener(e -> TransactionsViewer.this.widgetDisposed());
    }

    private void hookKeyListener()
    {
        tableViewer.getControl().addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {
                new TransactionContextMenu(owner).handleEditKey(e, tableViewer.getStructuredSelection());
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

        new TransactionContextMenu(owner).menuAboutToShow(manager, fullContextMenu, selection);
    }
}
