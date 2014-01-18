package name.abuchen.portfolio.ui.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransaction.Type;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.ui.ClientEditor;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.CellEditorFactory;
import name.abuchen.portfolio.ui.util.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.SharesLabelProvider;
import name.abuchen.portfolio.ui.util.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.ShowHideColumnHelper.Column;
import name.abuchen.portfolio.ui.util.SimpleListContentProvider;
import name.abuchen.portfolio.ui.util.ViewerHelper;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;

public class AccountListView extends AbstractListView
{
    private static final String FILTER_INACTIVE_ACCOUNTS = "filter-redired-accounts"; //$NON-NLS-1$

    private TableViewer accounts;
    private TableViewer transactions;
    private AccountContextMenu accountMenu = new AccountContextMenu(this);

    private boolean isFiltered = false;

    @Override
    protected String getTitle()
    {
        return Messages.LabelAccounts;
    }

    @Override
    public void init(ClientEditor clientEditor, Object parameter)
    {
        super.init(clientEditor, parameter);

        isFiltered = getClientEditor().getPreferenceStore().getBoolean(FILTER_INACTIVE_ACCOUNTS);
    }

    private void resetInput()
    {
        if (isFiltered)
        {
            List<Account> list = new ArrayList<Account>();
            for (Account a : getClient().getAccounts())
                if (!a.isRetired())
                    list.add(a);
            accounts.setInput(list);
        }
        else
        {
            accounts.setInput(getClient().getAccounts());
        }
    }

    @Override
    protected void addButtons(ToolBar toolBar)
    {
        Action action = new Action()
        {
            @Override
            public void run()
            {
                Account account = new Account();
                account.setName(Messages.LabelNoName);

                getClient().addAccount(account);
                markDirty();

                resetInput();
                accounts.editElement(account, 0);
            }
        };
        action.setImageDescriptor(PortfolioPlugin.descriptor(PortfolioPlugin.IMG_PLUS));
        action.setToolTipText(Messages.AccountMenuAdd);
        new ActionContributionItem(action).fill(toolBar, -1);

        Action filter = new Action()
        {
            @Override
            public void run()
            {
                isFiltered = isChecked();
                getClientEditor().getPreferenceStore().setValue(FILTER_INACTIVE_ACCOUNTS, isFiltered);
                resetInput();
            }
        };
        filter.setChecked(isFiltered);
        filter.setImageDescriptor(PortfolioPlugin.descriptor(PortfolioPlugin.IMG_FILTER));
        filter.setToolTipText(Messages.AccountFilterRetiredAccounts);
        new ActionContributionItem(filter).fill(toolBar, -1);
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

        ShowHideColumnHelper support = new ShowHideColumnHelper(AccountListView.class.getSimpleName() + "@top", //$NON-NLS-1$
                        accounts, layout);

        Column column = new Column(Messages.ColumnAccount, SWT.None, 150);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return ((Account) e).getName();
            }

            @Override
            public Image getImage(Object element)
            {
                return PortfolioPlugin.image(PortfolioPlugin.IMG_ACCOUNT);
            }

            @Override
            public Color getForeground(Object e)
            {
                boolean isRetired = ((Account) e).isRetired();
                return isRetired ? Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY) : null;
            }
        });
        column.setSorter(ColumnViewerSorter.create(Account.class, "name"), SWT.DOWN); //$NON-NLS-1$
        column.setMoveable(false);
        support.addColumn(column);

        column = new Column(Messages.ColumnBalance, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return Values.Amount.format(((Account) e).getCurrentAmount());
            }
        });
        column.setSorter(ColumnViewerSorter.create(Account.class, "currentAmount")); //$NON-NLS-1$
        column.setMoveable(false);
        support.addColumn(column);

        support.createColumns();

        accounts.getTable().setHeaderVisible(true);
        accounts.getTable().setLinesVisible(true);

        for (Account account : getClient().getAccounts())
        {
            Collections.sort(account.getTransactions());
        }

        accounts.setContentProvider(new SimpleListContentProvider());
        resetInput();
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

        accountMenu.menuAboutToShow(manager, account);
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

        manager.add(new Action(Messages.AccountMenuDelete)
        {
            @Override
            public void run()
            {
                getClient().removeAccount(account);
                markDirty();
                resetInput();
            }
        });
    }

    // //////////////////////////////////////////////////////////////
    // bottom table: transactions
    // //////////////////////////////////////////////////////////////

    protected void createBottomTable(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        transactions = new TableViewer(container, SWT.FULL_SELECTION);

        ShowHideColumnHelper support = new ShowHideColumnHelper(AccountListView.class.getSimpleName() + "@bottom4", //$NON-NLS-1$
                        transactions, layout);

        Column column = new Column(Messages.ColumnDate, SWT.None, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                AccountTransaction t = (AccountTransaction) e;
                return Values.Date.format(t.getDate());
            }

            @Override
            public Color getForeground(Object element)
            {
                return colorFor((AccountTransaction) element);
            }
        });
        column.setSorter(ColumnViewerSorter.create(AccountTransaction.class, "date"), SWT.DOWN); //$NON-NLS-1$
        column.setMoveable(false);
        support.addColumn(column);

        column = new Column(Messages.ColumnTransactionType, SWT.None, 100);
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
        column.setMoveable(false);
        support.addColumn(column);

        column = new Column(Messages.ColumnAmount, SWT.None, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                AccountTransaction t = (AccountTransaction) e;
                long v = t.getAmount();
                if (EnumSet.of(Type.REMOVAL, Type.FEES, Type.TAXES, Type.BUY, Type.TRANSFER_OUT).contains(t.getType()))
                    v = -v;
                return Values.Amount.format(v);
            }

            @Override
            public Color getForeground(Object element)
            {
                return colorFor((AccountTransaction) element);
            }
        });
        column.setSorter(ColumnViewerSorter.create(AccountTransaction.class, "amount")); //$NON-NLS-1$
        column.setMoveable(false);
        support.addColumn(column);

        column = new Column(Messages.ColumnSecurity, SWT.None, 250);
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
        column.setMoveable(false);
        support.addColumn(column);

        column = new Column(Messages.ColumnShares, SWT.RIGHT, 80);
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
                else if (t.getType() == Type.DIVIDENDS)
                {
                    if (t.getShares() != 0)
                        return t.getShares();
                }
                return null;
            }

            @Override
            public Color getForeground(Object element)
            {
                return colorFor((AccountTransaction) element);
            }
        });
        column.setMoveable(false);
        support.addColumn(column);

        column = new Column(Messages.ColumnPurchasePrice, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                AccountTransaction t = (AccountTransaction) e;
                if (t.getCrossEntry() instanceof BuySellEntry)
                {
                    PortfolioTransaction portfolioTransaction = ((BuySellEntry) t.getCrossEntry())
                                    .getPortfolioTransaction();
                    return Values.Amount.format(portfolioTransaction.getActualPurchasePrice());
                }
                else if (t.getType() == Type.DIVIDENDS)
                {
                    if (t.getShares() != 0)
                        return Values.Amount.format(Math.round(t.getAmount() * Values.Share.divider() / t.getShares()));
                }

                return null;
            }

            @Override
            public Color getForeground(Object element)
            {
                return colorFor((AccountTransaction) element);
            }
        });
        column.setMoveable(false);
        support.addColumn(column);

        column = new Column(Messages.ColumnOffsetAccount, SWT.None, 120);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                AccountTransaction t = (AccountTransaction) e;
                return t.getCrossEntry() != null ? t.getCrossEntry().getCrossEntity(t).toString() : null;
            }

            @Override
            public Color getForeground(Object element)
            {
                return colorFor((AccountTransaction) element);
            }
        });
        column.setMoveable(false);
        support.addColumn(column);

        support.createColumns();

        transactions.getTable().setHeaderVisible(true);
        transactions.getTable().setLinesVisible(true);

        transactions.setContentProvider(new SimpleListContentProvider());

        List<Security> securities = new ArrayList<Security>(getClient().getSecurities());
        Collections.sort(securities, new Security.ByName());

        new CellEditorFactory(transactions, AccountTransaction.class) //
                        .notify(new CellEditorFactory.ModificationListener()
                        {
                            @Override
                            public boolean canModify(Object element, String property)
                            {
                                if ("shares".equals(property)) //$NON-NLS-1$
                                    return ((AccountTransaction) element).getType() == AccountTransaction.Type.DIVIDENDS;

                                return true;
                            }

                            @Override
                            public void onModified(Object element, String property)
                            {
                                AccountTransaction t = (AccountTransaction) element;
                                if (t.getCrossEntry() != null)
                                    t.getCrossEntry().updateFrom(t);

                                markDirty();
                                accounts.refresh();
                                transactions.refresh(element);
                            }
                        }) //
                        .editable("date") //$NON-NLS-1$
                        .readonly("type") //$NON-NLS-1$
                        .amount("amount") //$NON-NLS-1$
                        .combobox("security", securities, true) //$NON-NLS-1$
                        .shares("shares") //$NON-NLS-1$
                        .readonly("actualPurchasePrice") //$NON-NLS-1$
                        .readonly("crossentry") //$NON-NLS-1$
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

        if (!support.isUserConfigured())
            ViewerHelper.pack(transactions);
    }

    private Color colorFor(AccountTransaction t)
    {
        if (EnumSet.of(Type.REMOVAL, Type.FEES, Type.TAXES, Type.BUY, Type.TRANSFER_OUT).contains(t.getType()))
            return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_RED);
        else
            return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN);
    }

    private void fillTransactionsContextMenu(IMenuManager manager)
    {
        Account account = (Account) transactions.getData(Account.class.toString());
        if (account == null)
            return;

        accountMenu.menuAboutToShow(manager, account);

        boolean hasTransactionSelected = ((IStructuredSelection) transactions.getSelection()).getFirstElement() != null;
        if (hasTransactionSelected)
        {
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

                    if (transaction.getCrossEntry() != null)
                    {
                        transaction.getCrossEntry().delete();

                        // possibly remove from investment plan
                        Transaction t = transaction.getCrossEntry().getCrossTransaction(transaction);
                        if (t instanceof PortfolioTransaction)
                        {
                            for (InvestmentPlan plan : getClient().getPlans())
                                plan.removeTransaction((PortfolioTransaction) t);
                        }
                    }
                    else
                    {
                        account.getTransactions().remove(transaction);
                    }
                    markDirty();

                    accounts.refresh();
                    transactions.setInput(account.getTransactions());
                }
            });
        }
    }
}
