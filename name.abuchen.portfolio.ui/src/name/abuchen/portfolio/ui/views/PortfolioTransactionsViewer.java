package name.abuchen.portfolio.ui.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.ColumnEditingSupport.ModificationListener;
import name.abuchen.portfolio.ui.util.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.DateEditingSupport;
import name.abuchen.portfolio.ui.util.ListEditingSupport;
import name.abuchen.portfolio.ui.util.SharesLabelProvider;
import name.abuchen.portfolio.ui.util.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.ShowHideColumnHelper.Column;
import name.abuchen.portfolio.ui.util.SimpleListContentProvider;
import name.abuchen.portfolio.ui.util.StringEditingSupport;
import name.abuchen.portfolio.ui.util.ValueEditingSupport;
import name.abuchen.portfolio.ui.util.ViewerHelper;
import name.abuchen.portfolio.ui.util.WebLocationMenu;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;

public final class PortfolioTransactionsViewer implements ModificationListener
{
    private class TransactionLabelProvider extends ColumnLabelProvider
    {
        @Override
        public Color getForeground(Object element)
        {
            if (marked.contains(element))
                return Display.getDefault().getSystemColor(SWT.COLOR_INFO_FOREGROUND);

            PortfolioTransaction t = (PortfolioTransaction) element;

            if (t.getType() == Type.SELL || t.getType() == Type.TRANSFER_OUT)
                return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_RED);
            else
                return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN);
        }

        @Override
        public Color getBackground(Object element)
        {
            return marked.contains(element) ? Display.getDefault().getSystemColor(SWT.COLOR_INFO_BACKGROUND) : null;
        }
    }

    private AbstractFinanceView owner;
    private Portfolio portfolio;
    private Set<PortfolioTransaction> marked = new HashSet<PortfolioTransaction>();

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

        tableViewer = new TableViewer(container, SWT.FULL_SELECTION);
        ColumnEditingSupport.prepare(tableViewer);

        support = new ShowHideColumnHelper(PortfolioTransactionsViewer.class.getSimpleName() + "2", tableViewer, layout); //$NON-NLS-1$

        addColumns();
        support.createColumns();

        tableViewer.getTable().setHeaderVisible(true);
        tableViewer.getTable().setLinesVisible(true);
        tableViewer.setContentProvider(new SimpleListContentProvider());

        hookContextMenu(parent);
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

    public void pack()
    {
        if (!support.isUserConfigured())
            ViewerHelper.pack(tableViewer);
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
        List<Security> securities = new ArrayList<Security>(owner.getClient().getSecurities());
        Collections.sort(securities, new Security.ByName());
        new ListEditingSupport(PortfolioTransaction.class, "security", securities).addListener(this).attachTo(column); //$NON-NLS-1$
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
                return t.getShares() != 0 ? Values.Amount.format(t.getActualPurchasePrice()) : null;
            }
        });
        ColumnViewerSorter.create(PortfolioTransaction.class, "actualPurchasePrice").attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnAmount, SWT.RIGHT, 80);
        column.setLabelProvider(new TransactionLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return Values.Amount.format(((PortfolioTransaction) element).getLumpSumPrice());
            }
        });
        ColumnViewerSorter.create(PortfolioTransaction.class, "lumpSumPrice").attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnFees, SWT.RIGHT, 80);
        column.setLabelProvider(new TransactionLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return Values.Amount.format(((PortfolioTransaction) element).getFees());
            }
        });
        ColumnViewerSorter.create(PortfolioTransaction.class, "fees").attachTo(column); //$NON-NLS-1$
        new ValueEditingSupport(PortfolioTransaction.class, "fees", Values.Amount).addListener(this).attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnLumpSumPrice, SWT.RIGHT, 80);
        column.setLabelProvider(new TransactionLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return Values.Amount.format(((PortfolioTransaction) element).getAmount());
            }
        });
        ColumnViewerSorter.create(PortfolioTransaction.class, "amount").attachTo(column); //$NON-NLS-1$
        new ValueEditingSupport(PortfolioTransaction.class, "amount", Values.Amount).addListener(this).attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnOffsetAccount, SWT.None, 120);
        column.setLabelProvider(new TransactionLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                PortfolioTransaction t = (PortfolioTransaction) e;
                return t.getCrossEntry() != null ? t.getCrossEntry().getCrossEntity(t).toString() : null;
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
                return note != null && note.length() > 0 ? PortfolioPlugin.image(PortfolioPlugin.IMG_NOTE) : null;
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
        menuMgr.addMenuListener(new IMenuListener()
        {
            public void menuAboutToShow(IMenuManager manager)
            {
                fillTransactionsContextMenu(manager);
            }
        });

        contextMenu = menuMgr.createContextMenu(parent.getShell());
        tableViewer.getTable().setMenu(contextMenu);

        tableViewer.getTable().addDisposeListener(new DisposeListener()
        {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                PortfolioTransactionsViewer.this.widgetDisposed();
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

        final PortfolioTransaction transaction = (PortfolioTransaction) ((IStructuredSelection) tableViewer
                        .getSelection()).getFirstElement();

        if (fullContextMenu && transaction != null)
            new SecurityContextMenu(owner).menuAboutToShow(manager, transaction.getSecurity(), portfolio);
        else if (fullContextMenu)
            new SecurityContextMenu(owner).menuAboutToShow(manager, null, portfolio);
        else if (transaction != null)
            manager.add(new WebLocationMenu(transaction.getSecurity()));

        if (transaction != null)
        {
            manager.add(new Separator());
            manager.add(new Action(Messages.MenuTransactionDelete)
            {
                @Override
                public void run()
                {
                    doDeleteTransaction(portfolio, transaction);
                }
            });
        }
    }

    private void doDeleteTransaction(final Portfolio portfolio, final PortfolioTransaction transaction)
    {
        if (transaction.getCrossEntry() != null)
            transaction.getCrossEntry().delete();
        else
            portfolio.getTransactions().remove(transaction);

        // possibly remove from investment plan
        for (InvestmentPlan plan : owner.getClient().getPlans())
            plan.removeTransaction(transaction);

        owner.markDirty();
        owner.notifyModelUpdated();
    }
}
