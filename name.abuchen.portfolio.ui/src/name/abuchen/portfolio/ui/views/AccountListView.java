package name.abuchen.portfolio.ui.views;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransaction.Type;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MutableMoney;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.balance.TroubleshootBalanceDiscrepancyDialog;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.handlers.ImportCSVHandler;
import name.abuchen.portfolio.ui.handlers.ImportPDFHandler;
import name.abuchen.portfolio.ui.util.ConfirmAction;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.LogoManager;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport.ModificationListener;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.CopyPasteSupport;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.views.columns.AttributeColumn;
import name.abuchen.portfolio.ui.views.columns.CurrencyColumn;
import name.abuchen.portfolio.ui.views.columns.CurrencyColumn.CurrencyEditingSupport;
import name.abuchen.portfolio.ui.views.columns.NameColumn;
import name.abuchen.portfolio.ui.views.columns.NameColumn.NameColumnLabelProvider;
import name.abuchen.portfolio.ui.views.columns.NoteColumn;
import name.abuchen.portfolio.ui.views.panes.AccountBalancePane;
import name.abuchen.portfolio.ui.views.panes.AccountTransactionsPane;
import name.abuchen.portfolio.ui.views.panes.InformationPanePage;

public class AccountListView extends AbstractFinanceView implements ModificationListener
{
    private static final String FILTER_INACTIVE_ACCOUNTS = "filter-redired-accounts"; //$NON-NLS-1$

    private TableViewer accounts;

    /**
     * Store current balance of account after given transaction has been
     * applied. See {@link #updateBalance(Account)}. Do not store transient
     * balance in persistent AccountTransaction object.
     */
    private Map<AccountTransaction, Money> transaction2balance = new HashMap<>();

    private AccountContextMenu accountMenu = new AccountContextMenu(this);

    private ShowHideColumnHelper accountColumns;

    private boolean isFiltered = false;

    @Override
    protected String getDefaultTitle()
    {
        return Messages.LabelAccounts;
    }

    @PostConstruct
    public void setup()
    {
        isFiltered = getPreferenceStore().getBoolean(FILTER_INACTIVE_ACCOUNTS);
    }

    private void resetInput()
    {
        accounts.setInput(isFiltered ? getClient().getActiveAccounts() : getClient().getAccounts());
    }

    @Override
    protected void addButtons(ToolBarManager manager)
    {
        addNewButton(manager);
        addFilterButton(manager);
        addConfigButton(manager);
    }

    private void addNewButton(ToolBarManager manager)
    {
        SimpleAction.Runnable newAccountAction = a -> {
            Account account = new Account();
            account.setName(Messages.LabelNoName);
            account.setCurrencyCode(getClient().getBaseCurrency());

            getClient().addAccount(account);
            markDirty();

            resetInput();
            accounts.editElement(account, 0);
        };

        manager.add(new DropDown(Messages.MenuCreateAccountOrTransaction, Images.PLUS, SWT.NONE, menuListener -> {
            menuListener.add(new SimpleAction(Messages.AccountMenuAdd, newAccountAction));

            menuListener.add(new Separator());

            Account account = (Account) accounts.getStructuredSelection().getFirstElement();
            new AccountContextMenu(AccountListView.this).menuAboutToShow(menuListener, account, null);
        }));
    }

    private void addFilterButton(ToolBarManager manager)
    {
        Action filter = new Action()
        {
            @Override
            public void run()
            {
                isFiltered = !isFiltered;
                getPart().getPreferenceStore().setValue(FILTER_INACTIVE_ACCOUNTS, isFiltered);
                setImageDescriptor(isFiltered ? Images.FILTER_ON.descriptor() : Images.FILTER_OFF.descriptor());
                resetInput();
            }
        };
        filter.setImageDescriptor(isFiltered ? Images.FILTER_ON.descriptor() : Images.FILTER_OFF.descriptor());
        filter.setToolTipText(Messages.AccountFilterRetiredAccounts);
        manager.add(filter);
    }

    private void addConfigButton(final ToolBarManager toolBar)
    {
        toolBar.add(new DropDown(Messages.MenuShowHideColumns, Images.CONFIG, SWT.NONE,
                        manager -> accountColumns.menuAboutToShow(manager)));
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
    protected void notifyViewCreationCompleted()
    {
        resetInput();
        accounts.refresh();

        if (accounts.getTable().getItemCount() > 0)
            accounts.setSelection(new StructuredSelection(accounts.getElementAt(0)), true);
    }

    @Override
    public void onModified(Object element, Object newValue, Object oldValue)
    {
        if (element instanceof AccountTransaction t)
        {
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
    protected Control createBody(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        accounts = new TableViewer(container, SWT.FULL_SELECTION);

        ColumnEditingSupport.prepare(accounts);
        ColumnViewerToolTipSupport.enableFor(accounts, ToolTip.NO_RECREATE);
        CopyPasteSupport.enableFor(accounts);

        accountColumns = new ShowHideColumnHelper(AccountListView.class.getSimpleName() + "@top2", //$NON-NLS-1$
                        getPreferenceStore(), accounts, layout);

        Column column = new NameColumn("0", Messages.ColumnAccount, SWT.None, 150, getClient()); //$NON-NLS-1$
        column.setLabelProvider(new NameColumnLabelProvider(getClient()) // NOSONAR
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

        column = new Column("1", Messages.ColumnBalance, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setDescription(Messages.ColumnBalance_Description);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return Values.Amount.format(((Account) e).getCurrentAmount(LocalDateTime.now().with(LocalTime.MAX)));
            }
        });
        ColumnViewerSorter.create(o -> ((Account) o).getCurrentAmount(LocalDateTime.now().with(LocalTime.MAX)))
                        .attachTo(column);
        accountColumns.addColumn(column);

        column = new CurrencyColumn();
        column.setEditingSupport(new CurrencyEditingSupport()
        {
            @Override
            public boolean canEdit(Object element)
            {
                return ((Account) element).getTransactions().isEmpty();
            }
        });
        accountColumns.addColumn(column);

        column = new NoteColumn();
        column.getEditingSupport().addListener(this);
        accountColumns.addColumn(column);

        addAttributeColumns(accountColumns);

        accountColumns.createColumns(true);

        accounts.getTable().setHeaderVisible(true);
        accounts.getTable().setLinesVisible(true);

        accounts.setContentProvider(ArrayContentProvider.getInstance());

        hookContextMenu(accounts.getTable(), this::fillAccountsContextMenu);

        accounts.addSelectionChangedListener(event -> {
            Account account = (Account) event.getStructuredSelection().getFirstElement();
            updateBalance(account);
            setInformationPaneInput(account);
        });

        return container;
    }

    private void addAttributeColumns(ShowHideColumnHelper support)
    {
        AttributeColumn.createFor(getClient(), Account.class) //
                        .forEach(column -> {
                            column.getEditingSupport().addListener(this);
                            support.addColumn(column);
                        });
    }

    private void fillAccountsContextMenu(IMenuManager manager) // NOSONAR
    {
        final Account account = (Account) ((IStructuredSelection) accounts.getSelection()).getFirstElement();
        if (account == null)
            return;

        accountMenu.menuAboutToShow(manager, account, null);
        manager.add(new Separator());

        manager.add(new SimpleAction(Messages.AccountMenuTroubleshootBalanceDiscrepancy,
                        a -> new TroubleshootBalanceDiscrepancyDialog(getActiveShell(), getPart().getClientInput(),
                                        account).open()));
        manager.add(new Separator());

        manager.add(new SimpleAction(Messages.AccountMenuImportCSV, a -> ImportCSVHandler.runImport(getPart(),
                        Display.getDefault().getActiveShell(), getContext(), null, null, getClient(), account, null)));

        manager.add(new SimpleAction(Messages.AccountMenuImportPDF, a -> ImportPDFHandler.runImport(getPart(),
                        Display.getDefault().getActiveShell(), getClient(), account, null)));

        manager.add(new Separator());

        if (LogoManager.instance().hasCustomLogo(account, getClient().getSettings()))
        {
            manager.add(new SimpleAction(Messages.LabelRemoveLogo, a -> {
                LogoManager.instance().clearCustomLogo(account, getClient().getSettings());
                markDirty();
            }));
        }

        manager.add(new SimpleAction(
                        account.isRetired() ? Messages.AccountMenuActivate : Messages.AccountMenuDeactivate, a -> {
                            account.setRetired(!account.isRetired());
                            markDirty();
                            resetInput();
                        }));

        var label = Messages.AccountMenuDelete;
        if (!account.getTransactions().isEmpty())
            label += " (" + MessageFormat.format(Messages.LabelTransactionCount, account.getTransactions().size()) //$NON-NLS-1$
                            + ")"; //$NON-NLS-1$

        Action action = new ConfirmAction(label,
                        MessageFormat.format(Messages.AccountMenuDeleteConfirm, account.getName()), //
                        a -> {
                            getClient().removeAccount(account);
                            markDirty();
                            resetInput();
                        });

        action.setEnabled(account.getTransactions().isEmpty());
        manager.add(action);
    }

    // //////////////////////////////////////////////////////////////
    // bottom table: transactions
    // //////////////////////////////////////////////////////////////

    @Override
    protected void addPanePages(List<InformationPanePage> pages)
    {
        super.addPanePages(pages);
        pages.add(make(AccountTransactionsPane.class));
        pages.add(make(AccountBalancePane.class));
    }

    private void updateBalance(Account account)
    {
        transaction2balance.clear();
        if (account == null)
            return;

        List<AccountTransaction> tx = new ArrayList<>(account.getTransactions());
        Collections.sort(tx, Transaction.BY_DATE);

        MutableMoney balance = MutableMoney.of(account.getCurrencyCode());
        for (AccountTransaction t : tx)
        {
            Type type = t.getType();
            switch (type)
            {
                case DEPOSIT:
                case INTEREST:
                case DIVIDENDS:
                case TAX_REFUND:
                case SELL:
                case TRANSFER_IN:
                case FEES_REFUND:
                    balance.add(t.getMonetaryAmount());
                    break;
                case REMOVAL:
                case FEES:
                case INTEREST_CHARGE:
                case TAXES:
                case BUY:
                case TRANSFER_OUT:
                    balance.subtract(t.getMonetaryAmount());
                    break;
                default:
                    throw new IllegalArgumentException("unsupported type " + type + " for transaction " + tx); //$NON-NLS-1$ //$NON-NLS-2$
            }

            transaction2balance.put(t, balance.toMoney());
        }
    }
}
