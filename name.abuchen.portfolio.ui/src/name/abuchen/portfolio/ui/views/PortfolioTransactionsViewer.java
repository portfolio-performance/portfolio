package name.abuchen.portfolio.ui.views;

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
import name.abuchen.portfolio.ui.util.CellEditorFactory;
import name.abuchen.portfolio.ui.util.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.SharesLabelProvider;
import name.abuchen.portfolio.ui.util.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.ShowHideColumnHelper.Column;
import name.abuchen.portfolio.ui.util.SimpleListContentProvider;
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;

public final class PortfolioTransactionsViewer
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
        support = new ShowHideColumnHelper(PortfolioTransactionsViewer.class.getSimpleName(), tableViewer, layout);

        addColumns();
        support.createColumns();

        tableViewer.getTable().setHeaderVisible(true);
        tableViewer.getTable().setLinesVisible(true);
        tableViewer.setContentProvider(new SimpleListContentProvider());

        addEditingSupport(owner);

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
        column.setSorter(ColumnViewerSorter.create(PortfolioTransaction.class, "date"), SWT.DOWN); //$NON-NLS-1$
        column.setMoveable(false);
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
        column.setSorter(ColumnViewerSorter.create(PortfolioTransaction.class, "type")); //$NON-NLS-1$
        column.setMoveable(false);
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
        column.setSorter(ColumnViewerSorter.create(PortfolioTransaction.class, "security")); //$NON-NLS-1$
        column.setMoveable(false);
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
        column.setSorter(ColumnViewerSorter.create(PortfolioTransaction.class, "shares")); //$NON-NLS-1$
        column.setMoveable(false);
        support.addColumn(column);

        column = new Column(Messages.ColumnPurchasePrice, SWT.RIGHT, 80);
        column.setLabelProvider(new TransactionLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                PortfolioTransaction t = (PortfolioTransaction) element;
                return t.getShares() != 0 ? Values.Amount.format(t.getActualPurchasePrice()) : null;
            }
        });
        column.setSorter(ColumnViewerSorter.create(PortfolioTransaction.class, "actualPurchasePrice")); //$NON-NLS-1$
        column.setMoveable(false);
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
        column.setSorter(ColumnViewerSorter.create(PortfolioTransaction.class, "lumpSumPrice")); //$NON-NLS-1$
        column.setMoveable(false);
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
        column.setSorter(ColumnViewerSorter.create(PortfolioTransaction.class, "fees")); //$NON-NLS-1$
        column.setMoveable(false);
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
        column.setSorter(ColumnViewerSorter.create(PortfolioTransaction.class, "amount")); //$NON-NLS-1$
        column.setMoveable(false);
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
        column.setMoveable(false);
        support.addColumn(column);
    }

    private void addEditingSupport(AbstractFinanceView owner)
    {
        List<Security> securities = owner.getClient().getSecurities();
        Collections.sort(securities, new Security.ByName());

        new CellEditorFactory(tableViewer, PortfolioTransaction.class) //
                        .notify(new CellEditorFactory.ModificationListener()
                        {
                            public void onModified(Object element, String property)
                            {
                                PortfolioTransaction t = (PortfolioTransaction) element;
                                if (t.getCrossEntry() != null)
                                    t.getCrossEntry().updateFrom(t);

                                PortfolioTransactionsViewer.this.owner.markDirty();
                                PortfolioTransactionsViewer.this.owner.notifyModelUpdated();
                            }
                        }) //
                        .editable("date") // //$NON-NLS-1$
                        .readonly("type") // //$NON-NLS-1$
                        .combobox("security", securities) // //$NON-NLS-1$
                        .shares("shares") // //$NON-NLS-1$
                        .readonly("actualPurchasePrice") //$NON-NLS-1$
                        .readonly("lumpSumPrice") //$NON-NLS-1$
                        .amount("fees") // //$NON-NLS-1$
                        .amount("amount") // //$NON-NLS-1$
                        .readonly("crossentry") //$NON-NLS-1$
                        .apply();
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

        if (fullContextMenu)
            new SecurityContextMenu(owner).menuAboutToShow(manager, transaction.getSecurity(), portfolio);
        else
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
