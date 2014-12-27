package name.abuchen.portfolio.ui.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransaction.Type;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPart;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.dialogs.transactions.BuySellSecurityDialog;
import name.abuchen.portfolio.ui.util.AbstractDropDown;
import name.abuchen.portfolio.ui.util.Column;
import name.abuchen.portfolio.ui.util.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.ColumnEditingSupport.ModificationListener;
import name.abuchen.portfolio.ui.util.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.DateEditingSupport;
import name.abuchen.portfolio.ui.util.ListEditingSupport;
import name.abuchen.portfolio.ui.util.SharesLabelProvider;
import name.abuchen.portfolio.ui.util.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.SimpleListContentProvider;
import name.abuchen.portfolio.ui.util.StringEditingSupport;
import name.abuchen.portfolio.ui.util.ValueEditingSupport;
import name.abuchen.portfolio.ui.util.ViewerHelper;
import name.abuchen.portfolio.ui.views.columns.CurrencyColumn;
import name.abuchen.portfolio.ui.views.columns.NameColumn;
import name.abuchen.portfolio.ui.views.columns.NameColumn.NameColumnLabelProvider;
import name.abuchen.portfolio.ui.views.columns.NoteColumn;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
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

public class AccountListView extends AbstractListView implements ModificationListener
{
    private static final String FILTER_INACTIVE_ACCOUNTS = "filter-redired-accounts"; //$NON-NLS-1$

    private TableViewer accounts;
    private TableViewer transactions;

    private AccountContextMenu accountMenu = new AccountContextMenu(this);

    private ShowHideColumnHelper accountColumns;
    private ShowHideColumnHelper transactionsColumns;

    private boolean isFiltered = false;

    @Override
    protected String getTitle()
    {
        return Messages.LabelAccounts;
    }

    @Override
    public void init(PortfolioPart part, Object parameter)
    {
        super.init(part, parameter);

        isFiltered = part.getPreferenceStore().getBoolean(FILTER_INACTIVE_ACCOUNTS);
    }

    private void resetInput()
    {
        accounts.setInput(isFiltered ? getClient().getActiveAccounts() : getClient().getAccounts());
    }

    @Override
    protected void addButtons(ToolBar toolBar)
    {
        addNewAccountButton(toolBar);
        addFilterButton(toolBar);
        addConfigButton(toolBar);
    }

    private void addNewAccountButton(ToolBar toolBar)
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
    }

    private void addFilterButton(ToolBar toolBar)
    {
        Action filter = new Action()
        {
            @Override
            public void run()
            {
                isFiltered = isChecked();
                getPart().getPreferenceStore().setValue(FILTER_INACTIVE_ACCOUNTS, isFiltered);
                resetInput();
            }
        };
        filter.setChecked(isFiltered);
        filter.setImageDescriptor(PortfolioPlugin.descriptor(PortfolioPlugin.IMG_FILTER));
        filter.setToolTipText(Messages.AccountFilterRetiredAccounts);
        new ActionContributionItem(filter).fill(toolBar, -1);
    }

    private void addConfigButton(final ToolBar toolBar)
    {
        new AbstractDropDown(toolBar, Messages.MenuShowHideColumns, //
                        PortfolioPlugin.image(PortfolioPlugin.IMG_CONFIG), SWT.NONE)
        {
            @Override
            public void menuAboutToShow(IMenuManager manager)
            {
                MenuManager m = new MenuManager(Messages.LabelAccounts);
                accountColumns.menuAboutToShow(m);
                manager.add(m);

                m = new MenuManager(Messages.LabelTransactions);
                transactionsColumns.menuAboutToShow(m);
                manager.add(m);
            }
        };
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

    @Override
    public void onModified(Object element, Object newValue, Object oldValue)
    {
        if (element instanceof AccountTransaction)
        {
            AccountTransaction t = (AccountTransaction) element;
            if (t.getCrossEntry() != null)
                t.getCrossEntry().updateFrom(t);
            accounts.refresh(true);
        }

        markDirty();
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

        ColumnEditingSupport.prepare(accounts);

        accountColumns = new ShowHideColumnHelper(AccountListView.class.getSimpleName() + "@top2", //$NON-NLS-1$
                        getPreferenceStore(), accounts, layout);

        Column column = new NameColumn("0", Messages.ColumnAccount, SWT.None, 150); //$NON-NLS-1$
        column.setLabelProvider(new NameColumnLabelProvider()
        {
            @Override
            public Color getForeground(Object e)
            {
                boolean isRetired = ((Account) e).isRetired();
                return isRetired ? Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY) : null;
            }
        });
        column.getEditingSupport().addListener(this);
        accountColumns.addColumn(column);

        column = new Column(Messages.ColumnBalance, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return Values.Amount.format(((Account) e).getCurrentAmount());
            }
        });
        ColumnViewerSorter.create(Account.class, "currentAmount").attachTo(column); //$NON-NLS-1$
        accountColumns.addColumn(column);

        column = new CurrencyColumn();
        accountColumns.addColumn(column);

        column = new NoteColumn();
        column.getEditingSupport().addListener(this);
        accountColumns.addColumn(column);

        accountColumns.createColumns();

        accounts.getTable().setHeaderVisible(true);
        accounts.getTable().setLinesVisible(true);

        for (Account account : getClient().getAccounts())
        {
            Collections.sort(account.getTransactions(), new Transaction.ByDate());
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

        ColumnEditingSupport.prepare(transactions);

        transactionsColumns = new ShowHideColumnHelper(AccountListView.class.getSimpleName() + "@bottom5", //$NON-NLS-1$
                        getPreferenceStore(), transactions, layout);

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
        ColumnViewerSorter.create(AccountTransaction.class, "date").attachTo(column, SWT.DOWN); //$NON-NLS-1$
        new DateEditingSupport(AccountTransaction.class, "date").addListener(this).attachTo(column); //$NON-NLS-1$
        transactionsColumns.addColumn(column);

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
        transactionsColumns.addColumn(column);

        column = new Column(Messages.ColumnAmount, SWT.RIGHT, 80);
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
        new ValueEditingSupport(AccountTransaction.class, "amount", Values.Amount).addListener(this).attachTo(column); //$NON-NLS-1$
        transactionsColumns.addColumn(column);

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
        List<Security> securities = new ArrayList<Security>(getClient().getSecurities());
        Collections.sort(securities, new Security.ByName());
        new ListEditingSupport(AccountTransaction.class, "security", securities) //$NON-NLS-1$
        {
            @Override
            public boolean canEdit(Object element)
            {
                AccountTransaction t = (AccountTransaction) element;

                return t.getType() == AccountTransaction.Type.BUY //
                                || t.getType() == AccountTransaction.Type.SELL //
                                || t.getType() == AccountTransaction.Type.DIVIDENDS //
                                || t.getType() == AccountTransaction.Type.TAX_REFUND;
            }

            @Override
            public boolean canBeNull(Object element)
            {
                AccountTransaction t = (AccountTransaction) element;
                return t.getType() == AccountTransaction.Type.TAX_REFUND;
            }

        }.addListener(this).attachTo(column);
        transactionsColumns.addColumn(column);

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
                else if (t.getType() == Type.DIVIDENDS && t.getShares() != 0)
                {
                    return Values.Amount.format(Math.round(t.getAmount() * Values.Share.divider() / t.getShares()));
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
        transactionsColumns.addColumn(column);

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
        transactionsColumns.addColumn(column);

        column = new Column(Messages.ColumnNote, SWT.None, 200);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return ((AccountTransaction) e).getNote();
            }

            @Override
            public Color getForeground(Object element)
            {
                return colorFor((AccountTransaction) element);
            }

            @Override
            public Image getImage(Object e)
            {
                String note = ((AccountTransaction) e).getNote();
                return note != null && note.length() > 0 ? PortfolioPlugin.image(PortfolioPlugin.IMG_NOTE) : null;
            }
        });
        ColumnViewerSorter.create(AccountTransaction.class, "note").attachTo(column); //$NON-NLS-1$
        new StringEditingSupport(AccountTransaction.class, "note").addListener(this).attachTo(column); //$NON-NLS-1$
        transactionsColumns.addColumn(column);

        transactionsColumns.createColumns();

        transactions.getTable().setHeaderVisible(true);
        transactions.getTable().setLinesVisible(true);

        transactions.setContentProvider(new SimpleListContentProvider());

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

        AccountTransaction transaction = (AccountTransaction) ((IStructuredSelection) transactions.getSelection())
                        .getFirstElement();

        if (transaction != null && transaction.getCrossEntry() instanceof BuySellEntry)
        {
            manager.add(new Action(Messages.MenuEditTransaction)
            {
                @Override
                public void run()
                {
                    BuySellSecurityDialog dialog = new BuySellSecurityDialog(Display.getDefault().getActiveShell(),
                                    getClient(), (BuySellEntry) transaction.getCrossEntry());

                    if (dialog.open() == BuySellSecurityDialog.OK)
                    {
                        markDirty();
                        notifyModelUpdated();
                    }
                }
            });

            manager.add(new Separator());
        }

        accountMenu.menuAboutToShow(manager, account);

        if (transaction != null)
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

                    account.deleteTransaction(transaction, getClient());
                    markDirty();

                    accounts.refresh();
                    transactions.setInput(account.getTransactions());
                }
            });
        }
    }
}
