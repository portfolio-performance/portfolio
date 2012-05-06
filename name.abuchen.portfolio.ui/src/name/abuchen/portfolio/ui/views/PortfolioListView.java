package name.abuchen.portfolio.ui.views;

import java.util.ArrayList;
import java.util.EnumSet;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.snapshot.PortfolioSnapshot;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.SimpleListContentProvider;
import name.abuchen.portfolio.ui.util.ViewerHelper;
import name.abuchen.portfolio.util.Dates;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

public class PortfolioListView extends AbstractListView
{
    private TableViewer portfolios;
    private TableViewer transactions;
    private TreeViewer statementOfAssets;

    @Override
    protected String getTitle()
    {
        return Messages.LabelPortfolios;
    }

    @Override
    public void notifyModelUpdated()
    {
        portfolios.setSelection(portfolios.getSelection());
    }

    // //////////////////////////////////////////////////////////////
    // top table: accounts
    // //////////////////////////////////////////////////////////////

    protected void createTopTable(Composite parent)
    {
        portfolios = new TableViewer(parent, SWT.FULL_SELECTION);

        TableViewerColumn column = new TableViewerColumn(portfolios, SWT.None);
        column.getColumn().setText(Messages.ColumnPortfolio);
        column.getColumn().setWidth(100);
        ColumnViewerSorter.create(Portfolio.class, "name").attachTo(portfolios, column, true); //$NON-NLS-1$

        column = new TableViewerColumn(portfolios, SWT.None);
        column.getColumn().setText(Messages.ColumnReferenceAccount);
        column.getColumn().setWidth(160);
        ColumnViewerSorter.create(Portfolio.class, "referenceAccount").attachTo(portfolios, column); //$NON-NLS-1$

        portfolios.getTable().setHeaderVisible(true);
        portfolios.getTable().setLinesVisible(true);

        portfolios.setLabelProvider(new PortfolioLabelProvider());
        portfolios.setContentProvider(new SimpleListContentProvider());
        portfolios.setInput(getClient().getPortfolios());
        portfolios.refresh();
        ViewerHelper.pack(portfolios);

        portfolios.addSelectionChangedListener(new ISelectionChangedListener()
        {
            public void selectionChanged(SelectionChangedEvent event)
            {
                Portfolio portfolio = (Portfolio) ((IStructuredSelection) event.getSelection()).getFirstElement();
                transactions.setData(Portfolio.class.toString(), portfolio);
                transactions.setInput(portfolio != null ? portfolio.getTransactions()
                                : new ArrayList<PortfolioTransaction>(0));
                transactions.refresh();

                statementOfAssets.setInput(PortfolioSnapshot.create(portfolio, Dates.today()));
                statementOfAssets.expandAll();
            }
        });

        new CellEditorFactory(portfolios, Portfolio.class) //
                        .notify(new CellEditorFactory.ModificationListener()
                        {
                            public void onModified(Object element, String property)
                            {
                                markDirty();
                                portfolios.refresh(transactions.getData(Account.class.toString()));
                            }
                        }) //
                        .editable("name") // //$NON-NLS-1$
                        .combobox("referenceAccount", getClient().getAccounts()) // //$NON-NLS-1$
                        .apply();

        hookContextMenu(portfolios.getTable(), new IMenuListener()
        {
            public void menuAboutToShow(IMenuManager manager)
            {
                fillPortfolioContextMenu(manager);
            }
        });
    }

    private void fillPortfolioContextMenu(IMenuManager manager)
    {
        manager.add(new Action(Messages.PortfolioMenuDelete)
        {
            @Override
            public void run()
            {
                Portfolio portfolio = (Portfolio) ((IStructuredSelection) portfolios.getSelection()).getFirstElement();

                if (portfolio == null)
                    return;

                getClient().getPortfolios().remove(portfolio);
                markDirty();

                portfolios.setInput(getClient().getPortfolios());
            }
        });

        manager.add(new Action(Messages.PortfolioMenuAdd)
        {
            @Override
            public void run()
            {
                Portfolio portfolio = new Portfolio();
                portfolio.setName(Messages.LabelNoName);

                getClient().addPortfolio(portfolio);
                markDirty();

                portfolios.setInput(getClient().getPortfolios());
            }
        });
    }

    static class PortfolioLabelProvider extends LabelProvider implements ITableLabelProvider
    {

        public Image getColumnImage(Object element, int columnIndex)
        {
            if (columnIndex != 0)
                return null;

            return PortfolioPlugin.getDefault().getImageRegistry().get(PortfolioPlugin.IMG_PORTFOLIO);
        }

        public String getColumnText(Object element, int columnIndex)
        {
            Portfolio p = (Portfolio) element;
            switch (columnIndex)
            {
                case 0:
                    return p.getName();
                case 1:
                    return p.getReferenceAccount() != null ? p.getReferenceAccount().getName() : null;
            }
            return null;
        }

    }

    // //////////////////////////////////////////////////////////////
    // bottom table: transactions
    // //////////////////////////////////////////////////////////////

    protected void createBottomTable(Composite parent)
    {
        CTabFolder folder = new CTabFolder(parent, SWT.BORDER);

        CTabItem item = new CTabItem(folder, SWT.NONE);
        item.setText(Messages.LabelStatementOfAssets);
        statementOfAssets = StatementOfAssetsView.createAssetsViewer(folder);
        item.setControl(statementOfAssets.getTree());

        hookContextMenu(statementOfAssets.getControl(), new IMenuListener()
        {
            public void menuAboutToShow(IMenuManager manager)
            {
                StatementOfAssetsView.createMenuListener(manager, statementOfAssets, PortfolioListView.this);
            }
        });

        createTransactionsTable(folder);
        item = new CTabItem(folder, SWT.NONE);
        item.setText(Messages.TabTransactions);
        item.setControl(transactions.getControl());

        folder.setSelection(0);

        if (!getClient().getPortfolios().isEmpty())
            portfolios.setSelection(new StructuredSelection(portfolios.getElementAt(0)), true);

        ViewerHelper.pack(statementOfAssets);
        ViewerHelper.pack(transactions);
    }

    private void createTransactionsTable(CTabFolder folder)
    {
        transactions = new TableViewer(folder, SWT.FULL_SELECTION);

        TableViewerColumn column = new TableViewerColumn(transactions, SWT.NONE);
        column.getColumn().setText(Messages.ColumnDate);
        column.getColumn().setWidth(80);
        ColumnViewerSorter.create(PortfolioTransaction.class, "date").attachTo(transactions, column, true); //$NON-NLS-1$

        column = new TableViewerColumn(transactions, SWT.NONE);
        column.getColumn().setText(Messages.ColumnTransactionType);
        column.getColumn().setWidth(80);
        ColumnViewerSorter.create(PortfolioTransaction.class, "type").attachTo(transactions, column); //$NON-NLS-1$

        column = new TableViewerColumn(transactions, SWT.NONE);
        column.getColumn().setText(Messages.ColumnSecurity);
        column.getColumn().setWidth(250);
        ColumnViewerSorter.create(PortfolioTransaction.class, "security").attachTo(transactions, column); //$NON-NLS-1$

        column = new TableViewerColumn(transactions, SWT.RIGHT);
        column.getColumn().setText(Messages.ColumnShares);
        column.getColumn().setWidth(80);
        ColumnViewerSorter.create(PortfolioTransaction.class, "shares").attachTo(transactions, column); //$NON-NLS-1$

        column = new TableViewerColumn(transactions, SWT.RIGHT);
        column.getColumn().setText(Messages.ColumnAmount);
        column.getColumn().setWidth(80);
        ColumnViewerSorter.create(PortfolioTransaction.class, "amount").attachTo(transactions, column); //$NON-NLS-1$

        column = new TableViewerColumn(transactions, SWT.RIGHT);
        column.getColumn().setText(Messages.ColumnFees);
        column.getColumn().setWidth(80);
        ColumnViewerSorter.create(PortfolioTransaction.class, "fees").attachTo(transactions, column); //$NON-NLS-1$

        column = new TableViewerColumn(transactions, SWT.RIGHT);
        column.getColumn().setText(Messages.ColumnPurchaseRate);
        column.getColumn().setWidth(80);
        ColumnViewerSorter.create(PortfolioTransaction.class, "actualPurchasePrice").attachTo(transactions, column); //$NON-NLS-1$

        transactions.getTable().setHeaderVisible(true);
        transactions.getTable().setLinesVisible(true);

        transactions.setLabelProvider(new TransactionLabelProvider());
        transactions.setContentProvider(new SimpleListContentProvider());

        new CellEditorFactory(transactions, PortfolioTransaction.class) //
                        .notify(new CellEditorFactory.ModificationListener()
                        {
                            public void onModified(Object element, String property)
                            {
                                markDirty();
                                Portfolio portfolio = (Portfolio) transactions.getData(Portfolio.class.toString());
                                portfolios.refresh(portfolio);
                                transactions.refresh(element);

                                statementOfAssets.setInput(PortfolioSnapshot.create(portfolio, Dates.today()));
                                statementOfAssets.expandAll();
                            }
                        }) //
                        .editable("date") // //$NON-NLS-1$
                        .editable("type") // //$NON-NLS-1$
                        .combobox("security", getClient().getSecurities()) // //$NON-NLS-1$
                        .editable("shares") // //$NON-NLS-1$
                        .amount("amount") // //$NON-NLS-1$
                        .amount("fees") // //$NON-NLS-1$
                        .apply();

        hookContextMenu(transactions.getTable(), new IMenuListener()
        {
            public void menuAboutToShow(IMenuManager manager)
            {
                fillTransactionsContextMenu(manager);
            }
        });
    }

    private void fillTransactionsContextMenu(IMenuManager manager)
    {
        manager.add(new Action(Messages.MenuTransactionDelete)
        {
            @Override
            public void run()
            {
                Transaction transaction = (Transaction) ((IStructuredSelection) transactions.getSelection())
                                .getFirstElement();
                Portfolio portfolio = (Portfolio) transactions.getData(Portfolio.class.toString());

                if (transaction == null || portfolio == null)
                    return;

                portfolio.getTransactions().remove(transaction);
                markDirty();

                portfolios.refresh(transactions.getData(Portfolio.class.toString()));
                transactions.setInput(portfolio.getTransactions());
                transactions.setSelection(new StructuredSelection(transaction), true);

                statementOfAssets.setInput(PortfolioSnapshot.create(portfolio, Dates.today()));
                statementOfAssets.expandAll();
            }
        });

        manager.add(new Action(Messages.MenuTransactionAdd)
        {
            @Override
            public void run()
            {
                Portfolio portfolio = (Portfolio) transactions.getData(Portfolio.class.toString());
                if (portfolio == null)
                    return;

                PortfolioTransaction transaction = new PortfolioTransaction();
                transaction.setDate(Dates.today());
                transaction.setType(PortfolioTransaction.Type.BUY);
                transaction.setSecurity(getClient().getSecurities().get(0));

                portfolio.addTransaction(transaction);

                markDirty();

                portfolios.refresh(transactions.getData(Account.class.toString()));
                transactions.setInput(portfolio.getTransactions());

                statementOfAssets.setInput(PortfolioSnapshot.create(portfolio, Dates.today()));
                statementOfAssets.expandAll();
            }
        });
    }

    private static class TransactionLabelProvider extends LabelProvider implements ITableLabelProvider,
                    ITableColorProvider
    {

        public Image getColumnImage(Object element, int columnIndex)
        {
            return null;
        }

        public String getColumnText(Object element, int columnIndex)
        {
            PortfolioTransaction t = (PortfolioTransaction) element;
            switch (columnIndex)
            {
                case 0:
                    return String.format("%tF", t.getDate()); //$NON-NLS-1$
                case 1:
                    return t.getType().toString();
                case 2:
                    return t.getSecurity() != null ? String.valueOf(t.getSecurity()) : null;
                case 3:
                    return String.format("%,d", t.getShares()); //$NON-NLS-1$
                case 4:
                    return String.format("%,.2f", t.getAmount() / 100d); //$NON-NLS-1$
                case 5:
                    return String.format("%,.2f", t.getFees() / 100d); //$NON-NLS-1$
                case 6:
                    if (t.getShares() == 0)
                        return ""; //$NON-NLS-1$
                    return String.format("%,.2f", t.getActualPurchasePrice() / 100d); //$NON-NLS-1$
            }
            return null;
        }

        public Color getBackground(Object element, int columnIndex)
        {
            return null;
        }

        public Color getForeground(Object element, int columnIndex)
        {
            PortfolioTransaction t = (PortfolioTransaction) element;

            if (EnumSet.of(Type.SELL, Type.TRANSFER_OUT).contains(t.getType()))
                return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_RED);
            else
                return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN);
        }
    }

}
