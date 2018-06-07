package name.abuchen.portfolio.ui.views;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.swtchart.ISeries;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransaction.Type;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.DedicatedTransaction;
import name.abuchen.portfolio.model.Peer;
import name.abuchen.portfolio.model.PeerList;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MutableMoney;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.AccountSnapshot;
import name.abuchen.portfolio.util.Iban;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPart;
import name.abuchen.portfolio.ui.dialogs.transactions.AccountTransactionDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.AccountTransferDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.OpenDialogAction;
import name.abuchen.portfolio.ui.dialogs.transactions.SecurityTransactionDialog;
import name.abuchen.portfolio.ui.util.AbstractDropDown;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport.ModificationListener;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.DateEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ListEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.SharesLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.viewers.StringEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ValueEditingSupport;
import name.abuchen.portfolio.ui.views.columns.CurrencyColumn;
import name.abuchen.portfolio.ui.views.columns.CurrencyColumn.CurrencyEditingSupport;
import name.abuchen.portfolio.ui.views.columns.NameColumn;
import name.abuchen.portfolio.ui.views.columns.NameColumn.NameColumnLabelProvider;
import name.abuchen.portfolio.ui.views.columns.NoteColumn;
import name.abuchen.portfolio.ui.views.columns.IbanColumn;

public class PeerListView extends AbstractListView implements ModificationListener
{
    private static final String FILTER_ACCOUNTS = "filter-accounts"; //$NON-NLS-1$
    private Account dummyAccount; 

    private TableViewer peers;
    private TableViewer transactions;

    /**
     * Store current balance of account after given transaction has been
     * applied. See {@link #updateBalance(Account)}. Do not store transient
     * balance in persistent AccountTransaction object.
     */
    private Map<AccountTransaction, Money> transaction2balance = new HashMap<>();

    private PeerContextMenu peerMenu = new PeerContextMenu(this);

    private ShowHideColumnHelper peerColumns;
    private ShowHideColumnHelper transactionsColumns;

    private boolean isFiltered = false;

    @Inject
    private ExchangeRateProviderFactory factory;

    @Override
    protected String getDefaultTitle()
    {
        return Messages.LabelPeers;
    }

    @Override
    public void init(PortfolioPart part, Object parameter)
    {
        super.init(part, parameter);

        isFiltered = part.getPreferenceStore().getBoolean(FILTER_ACCOUNTS);
    }
    
    @Override
    protected int getSashStyle()
    {
        return SWT.VERTICAL | SWT.BEGINNING;
    }

    private void resetInput()
    {
        PeerList peers = new PeerList(getClient().getPeers());
        System.err.println(">>>> PeerListView::resetInput() peers A  : " + peers.toString());
        if (!isFiltered)
            peers = peers.addAccounts(getClient().getAccounts());
        System.err.println(">>>> PeerListView::resetInput() peers B  : " + peers.toString());
        this.peers.setInput(peers);
    }

    @Override
    protected void addButtons(ToolBar toolBar)
    {
        addNewButton(toolBar);
        addFilterButton(toolBar);
        addConfigButton(toolBar);
    }

    private void addNewButton(ToolBar toolBar)
    {
        SimpleAction.Runnable newPeerAction = a -> {
            Peer peer = new Peer();
            peer.setName(Messages.LabelNoName);

            getClient().addPeer(peer);
            System.err.println(">>>> PeerListView::newPeerAction() peers   : " + peer.toString());
            markDirty();

            resetInput();
            peers.editElement(peer, 0);
        };

        AbstractDropDown.create(toolBar, Messages.MenuPeerAdd, Images.PLUS.image(), SWT.NONE,
                        (dd, manager) -> {
                            if (getClient().getPeers().findPeer(Iban.IBANNUMBER_DUMMY) == null)
                                manager.add(new SimpleAction(Messages.MenuPeerAdd, newPeerAction));

                           // manager.add(new Separator());

                           // Peer peer = (Peer) peers.getStructuredSelection().getFirstElement();
                           //  new PeerContextMenu(PeerListView.this).menuAboutToShow(manager, peer, null);
                        });

    }

    private void addFilterButton(ToolBar toolBar)
    {
        Action filter = new Action()
        {
            @Override
            public void run()
            {
                isFiltered = !isFiltered;
                getPart().getPreferenceStore().setValue(FILTER_ACCOUNTS, isFiltered);
                setImageDescriptor(isFiltered ? Images.FILTER_ON.descriptor() : Images.FILTER_OFF.descriptor());
                resetInput();
            }
        };
        filter.setImageDescriptor(isFiltered ? Images.FILTER_ON.descriptor() : Images.FILTER_OFF.descriptor());
        filter.setToolTipText(Messages.PeerFilterAccounts);
        new ActionContributionItem(filter).fill(toolBar, -1);
    }

    private void addConfigButton(final ToolBar toolBar)
    {
        new AbstractDropDown(toolBar, Messages.MenuShowHideColumns, Images.CONFIG.image(), SWT.NONE) // NOSONAR
        {
            @Override
            public void menuAboutToShow(IMenuManager manager)
            {
                MenuManager m = new MenuManager(Messages.LabelPeers);
                peerColumns.menuAboutToShow(m);
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
        System.err.println(">>>> PeerListView::notifyModelUpdated() ");
        resetInput();

        Peer peer = (Peer) ((IStructuredSelection) peers.getSelection()).getFirstElement();
        if (getClient().getPeers().contains(peer))
            peers.setSelection(new StructuredSelection(peer));
        else
            peers.setSelection(StructuredSelection.EMPTY);
    }

    @Override
    public void onModified(Object element, Object newValue, Object oldValue)
    {
        System.err.println(">>>> PeerListView::onModified() ");
        if (element instanceof DedicatedTransaction)
        {
            AccountTransaction t = (AccountTransaction) ((DedicatedTransaction) element).getTransaction();
            if (t.getCrossEntry() != null)
                t.getCrossEntry().updateFrom(t);
            peers.refresh(true);
        }

        markDirty();
    }

    // //////////////////////////////////////////////////////////////
    // top table: peers
    // //////////////////////////////////////////////////////////////

    @Override
    protected void createTopTable(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        peers = new TableViewer(container, SWT.FULL_SELECTION);

        ColumnEditingSupport.prepare(peers);

        peerColumns = new ShowHideColumnHelper(PeerListView.class.getSimpleName() + "@top2", //$NON-NLS-1$
                        getPreferenceStore(), peers, layout);

        Column column = new NameColumn("0", Messages.ColumnPeer, SWT.None, 150); //$NON-NLS-1$
        column.setLabelProvider(new NameColumnLabelProvider() // NOSONAR
        {
            @Override
            public String getText(Object e)
            {
                final StringWriter sw = new StringWriter();
                final PrintWriter pw = new PrintWriter(sw, true);
// TEMP                new Exception().printStackTrace(pw);
                System.err.println(">>>> PeerListView::NameColumn::getText => " + sw.getBuffer().toString());
                System.err.println(">>>> PeerListView::NameColumn::getText   : " + e.toString());
//                Peer p = (Peer) e; 
//                if ((p.getName() == null || (p.getName() != null && p.getName().length() == 0)) && p.getAccount() != null)
//                    return p.getAccount().getName();
//                else
                    return ((Peer) e).getName();
                
            }
        });
        column.getEditingSupport().addListener(this);
        peerColumns.addColumn(column);

        column = new IbanColumn();
        column.getEditingSupport().addListener(this);
        peerColumns.addColumn(column);

        column = new NameColumn("2", Messages.ColumnReferenceAccount, SWT.None, 180); //$NON-NLS-1$
        column.setLabelProvider(new NameColumnLabelProvider() // NOSONAR
        {
            @Override
            public String getText(Object e)
            {
                Account a = ((Peer) e).getAccount();
                if (e == null || a == null)
                    return Messages.LabelNoAccount;
                System.err.println(">>>> PeerListView::AccountColumn::getText   : " + e.toString());
                return ((Peer) e).getAccount().toString();
            }

            @Override
            public Color getForeground(Object e)
            {
                Account a = ((Peer) e).getAccount();
                if (e == null || a == null)
                    return null;
                boolean isRetired = a.isRetired();
                return isRetired ? Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY) : null;
            }
        });
        ColumnViewerSorter.create(Peer.class, "account").attachTo(column); //$NON-NLS-1$
        List<Account> accounts = getClient().getAccounts(false);
        dummyAccount = new Account(Messages.LabelNoAccount);
        accounts.add(0, dummyAccount);
        accounts = Collections.unmodifiableList(accounts);
        new ListEditingSupport(Peer.class, "account", accounts)
            .addListener(this).attachTo(column);
        //column.getEditingSupport().addListener(this);
        peerColumns.addColumn(column);
        
        column = new NoteColumn();
        column.getEditingSupport().addListener(this);
        peerColumns.addColumn(column);

        peerColumns.createColumns();

        peers.getTable().setHeaderVisible(true);
        peers.getTable().setLinesVisible(true);

        peers.setContentProvider(ArrayContentProvider.getInstance());
        resetInput();
        peers.refresh();

        peers.addSelectionChangedListener(event -> {
            Peer peer = (Peer) ((IStructuredSelection) event.getSelection()).getFirstElement();

            transactions.setData(Peer.class.toString(), peer);
            transactions.setInput(peer != null ? peer.getTransactions(getClient()) : new ArrayList<DedicatedTransaction>(0));
            transactions.refresh();
        });

        hookContextMenu(peers.getTable(), this::fillPeersContextMenu);
    }

    private void fillPeersContextMenu(IMenuManager manager) // NOSONAR
    {
        final Peer peer = (Peer) ((IStructuredSelection) peers.getSelection()).getFirstElement();
        if (peer == null)
            return;

        peerMenu.menuAboutToShow(manager, peer, null);
        manager.add(new Separator());

        manager.add(new Action(Messages.MenuPeerDelete)
        {
            @Override
            public void run()
            {
                getClient().removePeer(peer);
                markDirty();
                resetInput();
            }
        });
    }

    // //////////////////////////////////////////////////////////////
    // bottom table: transactions
    // //////////////////////////////////////////////////////////////

    // TODO #41 not working at this point

    @Override
    protected void createBottomTable(Composite parent)
    {
        // folder
        CTabFolder folder = new CTabFolder(parent, SWT.BORDER);

        CTabItem item = new CTabItem(folder, SWT.NONE);
        item.setText(Messages.TabTransactions);
        item.setControl(createTransactionTable(folder));

        folder.setSelection(0);

        if (peers.getTable().getItemCount() > 0)
            peers.setSelection(new StructuredSelection(peers.getElementAt(0)), true);
    }

    protected Control createTransactionTable(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        transactions = new TableViewer(container, SWT.FULL_SELECTION | SWT.MULTI);
        ColumnViewerToolTipSupport.enableFor(transactions, ToolTip.NO_RECREATE);

        ColumnEditingSupport.prepare(transactions);

        transactionsColumns = new ShowHideColumnHelper(AccountListView.class.getSimpleName() + "@bottom5", //$NON-NLS-1$
                        getPreferenceStore(), transactions, layout);

        Column column = new Column(Messages.ColumnDate, SWT.None, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                AccountTransaction t = (AccountTransaction) ((DedicatedTransaction) e).getTransaction();
                return Values.Date.format(t.getDate());
            }

            @Override
            public Color getForeground(Object element)
            {
                return colorFor((AccountTransaction) ((DedicatedTransaction) element).getTransaction());
            }
        });
        ColumnViewerSorter.create(new DedicatedTransaction.ByDateAmountAccountTypeAndHashCode()).attachTo(column, SWT.DOWN);
        new DateEditingSupport(AccountTransaction.class, "date").addListener(this).attachTo(column); //$NON-NLS-1$
        transactionsColumns.addColumn(column);

        column = new Column(Messages.ColumnTransactionType, SWT.None, 100);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                AccountTransaction t = (AccountTransaction) ((DedicatedTransaction) e).getTransaction();
                return t.getType().toString();
            }

            @Override
            public Color getForeground(Object element)
            {
                return colorFor((AccountTransaction) ((DedicatedTransaction) element).getTransaction());
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
                AccountTransaction t = (AccountTransaction) ((DedicatedTransaction) e).getTransaction();
                long v = t.getAmount();
                if (t.getType().isDebit())
                    v = -v;
                return Values.Money.format(Money.of(t.getCurrencyCode(), v), getClient().getBaseCurrency());
            }

            @Override
            public Color getForeground(Object element)
            {
                return colorFor((AccountTransaction) ((DedicatedTransaction) element).getTransaction());
            }
        });
        column.setSorter(ColumnViewerSorter.create(AccountTransaction.class, "amount")); //$NON-NLS-1$
        transactionsColumns.addColumn(column);

        column = new Column(Messages.ColumnAccount, SWT.None, 120);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Account a = (Account) ((DedicatedTransaction) e).getAccount();
                return a.getName();
            }

            @Override
            public Color getForeground(Object element)
            {
                return colorFor((AccountTransaction) ((DedicatedTransaction) element).getTransaction());
            }
        });
        transactionsColumns.addColumn(column);


        column = new Column(Messages.ColumnOffsetAccount, SWT.None, 120);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                AccountTransaction t = (AccountTransaction) ((DedicatedTransaction) e).getTransaction();
                return t.getCrossEntry() != null ? t.getCrossEntry().getCrossOwner(t).toString() : null;
            }

            @Override
            public Color getForeground(Object element)
            {
                return colorFor((AccountTransaction) ((DedicatedTransaction) element).getTransaction());
            }
        });
        transactionsColumns.addColumn(column);

        column = new Column(Messages.ColumnNote, SWT.None, 200);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return ((AccountTransaction) ((DedicatedTransaction) e).getTransaction()).getNote();
            }

            @Override
            public Color getForeground(Object element)
            {
                return colorFor((AccountTransaction) ((DedicatedTransaction) element).getTransaction());
            }

            @Override
            public Image getImage(Object e)
            {
                String note = ((AccountTransaction) ((DedicatedTransaction) e).getTransaction()).getNote();
                return note != null && note.length() > 0 ? Images.NOTE.image() : null;
            }
        });
        ColumnViewerSorter.create(AccountTransaction.class, "note").attachTo(column); //$NON-NLS-1$
        new StringEditingSupport(AccountTransaction.class, "note").addListener(this).attachTo(column); //$NON-NLS-1$
        transactionsColumns.addColumn(column);

        transactionsColumns.createColumns();

        transactions.getTable().setHeaderVisible(true);
        transactions.getTable().setLinesVisible(true);

        transactions.setContentProvider(ArrayContentProvider.getInstance());

        hookContextMenu(transactions.getTable(), this::fillTransactionsContextMenu);

        hookKeyListener();

        return container;
    }

    private Color colorFor(AccountTransaction t)
    {
        return Display.getCurrent().getSystemColor(t.getType().isDebit() ? SWT.COLOR_DARK_RED : SWT.COLOR_DARK_GREEN);  
    }

    private void hookKeyListener()
    {
        transactions.getControl().addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {
                if (e.keyCode == 'e' && e.stateMask == SWT.MOD1)
                {
                    Account account = (Account) transactions.getData(Account.class.toString());
                    AccountTransaction transaction = (AccountTransaction) ((IStructuredSelection) transactions
                                    .getSelection()).getFirstElement();

                    if (account != null && transaction != null)
                        createEditAction(account, transaction).run();
                }
            }
        });
    }

    private void fillTransactionsContextMenu(IMenuManager manager) // NOSONAR
    {
        Account account = (Account) transactions.getData(Account.class.toString());
        if (account == null)
            return;

        AccountTransaction transaction = (AccountTransaction) ((IStructuredSelection) transactions.getSelection())
                        .getFirstElement();

        if (transaction != null)
        {
            Action action = createEditAction(account, transaction);
            action.setAccelerator(SWT.MOD1 | 'E');
            manager.add(action);
            manager.add(new Separator());
        }

//        peerMenu.menuAboutToShow(manager, account, transaction != null ? transaction.getSecurity() : null);
//
//        if (transaction != null)
//        {
//            manager.add(new Separator());
//            manager.add(new Action(Messages.AccountMenuDeleteTransaction)
//            {
//                @Override
//                public void run()
//                {
//                    Object[] selection = ((IStructuredSelection) transactions.getSelection()).toArray();
//                    // TODO: NMeed Fix is actually based on Peer...
//                    Account account = (Account) transactions.getData(Account.class.toString());
//
//                    if (selection == null || selection.length == 0 || account == null)
//                        return;
//
//                    for (Object transaction : selection)
//                        account.deleteTransaction((AccountTransaction) transaction, getClient());
//
//                    markDirty();
//                    peers.refresh();
//                    // TODO: transactions.setInput(peer.getTransactions());
//                }
//            });
//        }
    }

    private Action createEditAction(Account account, AccountTransaction transaction)
    {
        // buy / sell
        if (transaction.getCrossEntry() instanceof BuySellEntry)
        {
            BuySellEntry entry = (BuySellEntry) transaction.getCrossEntry();
            return new OpenDialogAction(this, Messages.MenuEditTransaction)
                            .type(SecurityTransactionDialog.class, d -> d.setBuySellEntry(entry))
                            .parameters(entry.getPortfolioTransaction().getType());
        }
        else if (transaction.getCrossEntry() instanceof AccountTransferEntry)
        {
            AccountTransferEntry entry = (AccountTransferEntry) transaction.getCrossEntry();
            return new OpenDialogAction(this, Messages.MenuEditTransaction) //
                            .type(AccountTransferDialog.class, d -> d.setEntry(entry));
        }
        else
        {
            return new OpenDialogAction(this, Messages.MenuEditTransaction) //
                            .type(AccountTransactionDialog.class, d -> d.setTransaction(account, transaction)) //
                            .parameters(transaction.getType());
        }
    }
}
