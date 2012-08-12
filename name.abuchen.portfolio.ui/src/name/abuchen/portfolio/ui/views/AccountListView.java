package name.abuchen.portfolio.ui.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransaction.Type;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.dialogs.BuySellSecurityDialog;
import name.abuchen.portfolio.ui.dialogs.DividendsDialog;
import name.abuchen.portfolio.ui.dialogs.OtherAccountTransactionsDialog;
import name.abuchen.portfolio.ui.dialogs.TransferDialog;
import name.abuchen.portfolio.ui.util.CellEditorFactory;
import name.abuchen.portfolio.ui.util.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.SimpleListContentProvider;
import name.abuchen.portfolio.ui.util.UITransactionHelper;
import name.abuchen.portfolio.ui.util.ViewerHelper;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;

public class AccountListView extends AbstractListView
{
    private TableViewer accounts;
    private TableViewer transactions;

    @Override
    protected String getTitle()
    {
        return Messages.LabelAccounts;
    }

    @Override
    protected void addButtons(ToolBar toolBar)
    {
        Action createPortfolio = new Action()
        {
            @Override
            public void run()
            {
                Account account = new Account();
                account.setName(Messages.LabelNoName);

                getClient().addAccount(account);
                markDirty();

                accounts.setInput(getClient().getAccounts());
                accounts.editElement(account, 0);
            }
        };
        createPortfolio.setImageDescriptor(PortfolioPlugin.descriptor(PortfolioPlugin.IMG_PLUS));
        createPortfolio.setToolTipText(Messages.AccountMenuAdd);

        new ActionContributionItem(createPortfolio).fill(toolBar, -1);
    }

    // //////////////////////////////////////////////////////////////
    // top table: accounts
    // //////////////////////////////////////////////////////////////

    @Override
    protected void createTopTable(Composite parent)
    {
        accounts = new TableViewer(parent, SWT.FULL_SELECTION);

        TableViewerColumn column = new TableViewerColumn(accounts, SWT.None);
        column.getColumn().setText(Messages.ColumnAccount);
        column.getColumn().setWidth(150);
        ColumnViewerSorter.create(Account.class, "name").attachTo(accounts, column, true); //$NON-NLS-1$

        column = new TableViewerColumn(accounts, SWT.RIGHT);
        column.getColumn().setText(Messages.ColumnBalance);
        column.getColumn().setWidth(80);
        ColumnViewerSorter.create(Account.class, "currentAmount").attachTo(accounts, column); //$NON-NLS-1$

        accounts.getTable().setHeaderVisible(true);
        accounts.getTable().setLinesVisible(true);

        for (Account account : getClient().getAccounts())
        {
            Collections.sort(account.getTransactions());
        }

        accounts.setLabelProvider(new AccountLabelProvider());
        accounts.setContentProvider(new SimpleListContentProvider());
        accounts.setInput(getClient().getAccounts());
        accounts.refresh();
        ViewerHelper.pack(accounts);

        accounts.addSelectionChangedListener(new ISelectionChangedListener()
        {
            public void selectionChanged(SelectionChangedEvent event)
            {
                Account account = (Account) ((IStructuredSelection) event.getSelection()).getFirstElement();
                transactions.setData(Account.class.toString(), account);
                transactions.setInput(account != null ? account.getTransactions()
                                : new ArrayList<AccountTransaction>(0));
                transactions.refresh();
            }
        });

        new CellEditorFactory(accounts, Account.class) //
                        .notify(new CellEditorFactory.ModificationListener()
                        {
                            public void onModified(Object element, String property)
                            {
                                markDirty();
                                accounts.refresh(transactions.getData(Account.class.toString()));
                            }
                        }) //
                        .editable("name") // //$NON-NLS-1$
                        .readonly("balance") // //$NON-NLS-1$
                        .apply();

        hookContextMenu(accounts.getTable(), new IMenuListener()
        {
            public void menuAboutToShow(IMenuManager manager)
            {
                fillAccountsContextMenu(manager);
            }
        });
    }

    private void fillAccountsContextMenu(IMenuManager manager)
    {
        final Account account = (Account) ((IStructuredSelection) accounts.getSelection()).getFirstElement();
        if (account == null)
            return;

        manager.add(new Action(Messages.AccountMenuDelete)
        {
            @Override
            public void run()
            {
                getClient().getAccounts().remove(account);
                markDirty();

                accounts.setInput(getClient().getAccounts());
            }
        });
    }

    static class AccountLabelProvider extends LabelProvider implements ITableLabelProvider
    {

        public Image getColumnImage(Object element, int columnIndex)
        {
            if (columnIndex != 0)
                return null;

            return PortfolioPlugin.image(PortfolioPlugin.IMG_ACCOUNT);
        }

        public String getColumnText(Object element, int columnIndex)
        {
            Account p = (Account) element;
            switch (columnIndex)
            {
                case 0:
                    return p.getName();
                case 1:
                    return Values.Amount.format(p.getCurrentAmount());
            }
            return null;
        }

    }

    // //////////////////////////////////////////////////////////////
    // bottom table: transactions
    // //////////////////////////////////////////////////////////////

    protected void createBottomTable(Composite parent)
    {
        transactions = new TableViewer(parent, SWT.FULL_SELECTION);

        TableViewerColumn column = new TableViewerColumn(transactions, SWT.None);
        column.getColumn().setText(Messages.ColumnDate);
        column.getColumn().setWidth(80);
        ColumnViewerSorter.create(AccountTransaction.class, "date").attachTo(transactions, column, true); //$NON-NLS-1$

        column = new TableViewerColumn(transactions, SWT.None);
        column.getColumn().setText(Messages.ColumnTransactionType);
        column.getColumn().setWidth(100);
        ColumnViewerSorter.create(AccountTransaction.class, "type").attachTo(transactions, column); //$NON-NLS-1$

        column = new TableViewerColumn(transactions, SWT.RIGHT);
        column.getColumn().setText(Messages.ColumnAmount);
        column.getColumn().setWidth(80);
        ColumnViewerSorter.create(AccountTransaction.class, "amount").attachTo(transactions, column); //$NON-NLS-1$

        column = new TableViewerColumn(transactions, SWT.None);
        column.getColumn().setText(Messages.ColumnSecurity);
        column.getColumn().setWidth(250);
        ColumnViewerSorter.create(AccountTransaction.class, "security").attachTo(transactions, column); //$NON-NLS-1$

        transactions.getTable().setHeaderVisible(true);
        transactions.getTable().setLinesVisible(true);

        transactions.setLabelProvider(new TransactionLabelProvider());
        transactions.setContentProvider(new SimpleListContentProvider());

        List<Security> securities = getClient().getSecurities();
        Collections.sort(securities, new Security.ByName());

        new CellEditorFactory(transactions, AccountTransaction.class) //
                        .notify(new CellEditorFactory.ModificationListener()
                        {
                            public void onModified(Object element, String property)
                            {
                                markDirty();
                                accounts.refresh(transactions.getData(Account.class.toString()));
                                transactions.refresh(element);
                            }
                        }) //
                        .editable("date") // //$NON-NLS-1$
                        .editable("type") // //$NON-NLS-1$
                        .amount("amount") // //$NON-NLS-1$
                        .combobox("security", securities, true) // //$NON-NLS-1$
                        .apply();

        hookContextMenu(transactions.getTable(), new IMenuListener()
        {
            public void menuAboutToShow(IMenuManager manager)
            {
                fillTransactionsContextMenu(manager);
            }
        });

        if (!getClient().getAccounts().isEmpty())
            accounts.setSelection(new StructuredSelection(accounts.getElementAt(0)), true);
        ViewerHelper.pack(transactions);
    }

    private abstract class AbstractDialogAction extends Action
    {

        public AbstractDialogAction(String text)
        {
            super(text);
        }

        @Override
        public final void run()
        {
            Account account = (Account) transactions.getData(Account.class.toString());

            if (account == null)
                return;

            Dialog dialog = createDialog(account);
            if (dialog.open() == TransferDialog.OK)
            {
                markDirty();
                accounts.refresh();
                transactions.setInput(account.getTransactions());
            }
        }

        abstract Dialog createDialog(Account account);
    }

    private void fillTransactionsContextMenu(IMenuManager manager)
    {
        manager.add(new AbstractDialogAction(Messages.AccountMenuTransfer)
        {
            @Override
            Dialog createDialog(Account account)
            {
                return new TransferDialog(getClientEditor().getSite().getShell(), getClient(), account);
            }
        });

        manager.add(new AbstractDialogAction(Messages.AccountMenuOther)
        {
            @Override
            Dialog createDialog(Account account)
            {
                return new OtherAccountTransactionsDialog(getClientEditor().getSite().getShell(), getClient(), account);
            }
        });

        manager.add(new Separator());
        manager.add(new AbstractDialogAction(Messages.SecurityMenuBuy)
        {
            @Override
            Dialog createDialog(Account account)
            {
                return new BuySellSecurityDialog(getClientEditor().getSite().getShell(), getClient(), null,
                                PortfolioTransaction.Type.BUY);
            }
        });

        manager.add(new AbstractDialogAction(Messages.SecurityMenuSell)
        {
            @Override
            Dialog createDialog(Account account)
            {
                return new BuySellSecurityDialog(getClientEditor().getSite().getShell(), getClient(), null,
                                PortfolioTransaction.Type.SELL);
            }
        });

        manager.add(new AbstractDialogAction(Messages.SecurityMenuDividends)
        {
            @Override
            Dialog createDialog(Account account)
            {
                return new DividendsDialog(getClientEditor().getSite().getShell(), getClient(), null);
            }
        });

        manager.add(new Separator());
        manager.add(new Action(Messages.AccountMenuDeleteTransaction)
        {
            @Override
            public void run()
            {
                AccountTransaction transaction = (AccountTransaction) ((IStructuredSelection) transactions
                                .getSelection()).getFirstElement();
                Account account = (Account) transactions.getData(Account.class.toString());

                if (transaction == null || account == null)
                    return;

                if (!UITransactionHelper.deleteCounterTransaction(getClientEditor().getSite().getShell(), getClient(),
                                transaction))
                    return;

                account.getTransactions().remove(transaction);
                markDirty();

                accounts.refresh();
                transactions.setInput(account.getTransactions());
            }
        });
    }

    static class TransactionLabelProvider extends LabelProvider implements ITableLabelProvider, ITableColorProvider
    {

        public Image getColumnImage(Object element, int columnIndex)
        {
            return null;
        }

        public String getColumnText(Object element, int columnIndex)
        {
            AccountTransaction t = (AccountTransaction) element;
            switch (columnIndex)
            {
                case 0:
                    return Values.Date.format(t.getDate());
                case 1:
                    return t.getType().toString();
                case 2:
                    long v = t.getAmount();
                    if (EnumSet.of(Type.REMOVAL, Type.FEES, Type.TAXES, Type.BUY, Type.TRANSFER_OUT).contains(
                                    t.getType()))
                        v = -v;
                    return Values.Amount.format(v);
                case 3:
                    return t.getSecurity() != null ? String.valueOf(t.getSecurity()) : null;
            }
            return null;
        }

        public Color getBackground(Object element, int columnIndex)
        {
            return null;
        }

        public Color getForeground(Object element, int columnIndex)
        {
            AccountTransaction t = (AccountTransaction) element;

            if (EnumSet.of(Type.REMOVAL, Type.FEES, Type.TAXES, Type.BUY, Type.TRANSFER_OUT).contains(t.getType()))
                return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_RED);
            else
                return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN);
        }

    }

}
