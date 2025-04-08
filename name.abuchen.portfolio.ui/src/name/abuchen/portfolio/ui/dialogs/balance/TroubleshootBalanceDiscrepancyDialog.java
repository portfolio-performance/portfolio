package name.abuchen.portfolio.ui.dialogs.balance;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.balance.Balance;
import name.abuchen.portfolio.snapshot.balance.Balance.Proposal;
import name.abuchen.portfolio.snapshot.balance.BalanceCollector;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.editor.ClientInput;
import name.abuchen.portfolio.ui.editor.ClientInputListener;
import name.abuchen.portfolio.ui.util.StringToCurrencyConverter;
import name.abuchen.portfolio.ui.util.swt.SashLayout;
import name.abuchen.portfolio.ui.util.swt.SashLayoutData;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.util.TextUtil;

public class TroubleshootBalanceDiscrepancyDialog extends Dialog
{
    private final ClientInput clientInput;
    private final Account account;

    /**
     * The actual balances as computed based on the existing transactions.
     */
    private List<Balance> computedBalances = Collections.emptyList();

    /**
     * The expected balances contain the balance values added by the user. We
     * store them separately to keep them even if we refresh the calculated
     * balances.
     */
    private Map<LocalDate, Money> expectedBalances = new HashMap<>();

    private TableViewer balanceViewer;
    private TreeViewer proposalsViewer;

    private final ClientInputListener clientInputListener = new ClientInputListener()
    {
        @Override
        public void onDisposed()
        {
            if (!getShell().isDisposed())
                close();
        }

        @Override
        public void onDirty(boolean isDirty)
        {
            if (isDirty)
            {
                var client = clientInput.getClient();

                if (!client.getAccounts().contains(account))
                {
                    if (!getShell().isDisposed())
                        close();
                }
                else
                {
                    recompute(true);
                }
            }
        }

    };

    public TroubleshootBalanceDiscrepancyDialog(Shell parentShell, ClientInput clientInput, Account account)
    {
        super(parentShell);

        this.clientInput = clientInput;
        this.account = account;

        clientInput.addListener(clientInputListener);

        setShellStyle(SWT.CLOSE | SWT.MODELESS | SWT.BORDER | SWT.TITLE | SWT.RESIZE);
        setBlockOnOpen(false);
    }

    private void recompute(boolean withBalances)
    {
        var currentSelection = (Balance) balanceViewer.getStructuredSelection().getFirstElement();

        var collector = new BalanceCollector(clientInput.getClient(), account);
        if (withBalances)
        {
            computedBalances = collector.computeBalances();
        }

        collector.updateProposals(computedBalances, expectedBalances);

        if (withBalances)
        {
            balanceViewer.setInput(computedBalances);
            if (currentSelection != null)
            {
                computedBalances.stream().filter(b -> b.getDate().equals(currentSelection.getDate())).findAny()
                                .ifPresent(b -> balanceViewer.setSelection(new StructuredSelection(b)));
            }
        }
        else if (currentSelection != null)
        {
            proposalsViewer.setInput(currentSelection.getProposals());
            proposalsViewer.expandAll();
        }
    }

    @Override
    public boolean close()
    {
        var isClosed = super.close();

        if (isClosed)
        {
            clientInput.removeListener(clientInputListener);
        }

        return isClosed;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite composite = (Composite) super.createDialogArea(parent);

        Composite container = new Composite(composite, SWT.None);
        GridDataFactory.fillDefaults().grab(true, true).hint(510, 400).applyTo(container);
        GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);

        // on windows, add a spacing line as tables
        // have white top and need a border
        int spacing = Platform.OS_WIN32.equals(Platform.getOS()) ? 1 : 0;
        GridLayoutFactory.fillDefaults().spacing(spacing, spacing).applyTo(container);

        Label label = new Label(container, SWT.None);
        label.setText(account.getName());
        GridDataFactory.fillDefaults().grab(true, false).applyTo(label);

        Composite sash = new Composite(container, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(sash);

        SashLayout sl = new SashLayout(sash, SWT.VERTICAL | SWT.END);
        sash.setLayout(sl);

        createBalanceTable(sash);
        var proposals = createProposalsTree(sash);
        proposals.setLayoutData(new SashLayoutData(100));

        recompute(true);

        return composite;
    }

    private void createBalanceTable(Composite sash)
    {
        Composite tableArea = new Composite(sash, SWT.NONE);
        GridDataFactory.fillDefaults().grab(false, true).applyTo(tableArea);
        tableArea.setLayout(new FillLayout());

        TableColumnLayout layout = new TableColumnLayout();
        tableArea.setLayout(layout);

        Table table = new Table(tableArea, SWT.BORDER | SWT.SINGLE);
        balanceViewer = new TableViewer(table);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        ShowHideColumnHelper support = new ShowHideColumnHelper(getClass().getSimpleName() + "-balance@Vxy11", //$NON-NLS-1$
                        clientInput.getPreferenceStore(), balanceViewer, layout);

        Column column = new Column("date", Messages.ColumnDate, SWT.NONE, 100); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                var date = ((Balance) element).getDate();
                var today = LocalDate.now();

                return date.equals(today) ? Messages.LabelToday : Values.Date.format(date);
            }

        });
        column.setSorter(ColumnViewerSorter.create(element -> ((Balance) element).getDate()), SWT.DOWN);
        support.addColumn(column);

        column = new Column("value", Messages.Balance, SWT.RIGHT, 120); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return Values.Money.format(((Balance) element).getValue(), account.getCurrencyCode());
            }

        });
        support.addColumn(column);

        column = new Column("delta", Messages.ColumnDiscrepancy, SWT.RIGHT, 120); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                var balance = (Balance) element;
                var expected = expectedBalances.get(balance.getDate());
                return expected != null
                                ? Values.Money.format(balance.getValue().subtract(expected), account.getCurrencyCode())
                                : null;
            }
        });
        support.addColumn(column);

        column = new Column("expected", Messages.ColumnExpectedBalance, SWT.RIGHT, 120); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                var balance = (Balance) element;
                var expected = expectedBalances.get(balance.getDate());
                return expected != null ? Values.Money.format(expected, account.getCurrencyCode()) : null;
            }

        });
        column.setEditingSupport(new ColumnEditingSupport()
        {
            private final StringToCurrencyConverter stringToLong = new StringToCurrencyConverter(Values.Amount);

            @Override
            public void setValue(Object element, Object value) throws Exception
            {
                var balance = (Balance) element;

                var stringValue = TextUtil.trim((String) value);
                if (stringValue == null || stringValue.length() == 0)
                {
                    expectedBalances.remove(balance.getDate());
                    return;
                }

                Number newValue = stringToLong.convert(stringValue);

                expectedBalances.put(balance.getDate(), Money.of(account.getCurrencyCode(), newValue.longValue()));

                recompute(false);
            }

            @Override
            public Object getValue(Object element) throws Exception
            {
                var balance = (Balance) element;
                var expected = expectedBalances.get(balance.getDate());
                return expected != null ? Values.Amount.format(expected.getAmount()) : ""; //$NON-NLS-1$
            }
        });
        support.addColumn(column);

        support.createColumns();

        balanceViewer.addSelectionChangedListener(event -> {
            var element = event.getStructuredSelection().getFirstElement();
            if (element instanceof Balance balance)
            {
                proposalsViewer.setInput(balance.getProposals());
                proposalsViewer.expandAll();
            }
        });

        balanceViewer.setContentProvider(new ArrayContentProvider());
    }

    private Composite createProposalsTree(Composite sash)
    {
        Composite treeArea = new Composite(sash, SWT.NONE);
        GridDataFactory.fillDefaults().grab(false, true).applyTo(treeArea);
        treeArea.setLayout(new FillLayout());

        TreeColumnLayout layout = new TreeColumnLayout();
        treeArea.setLayout(layout);

        Tree tree = new Tree(treeArea, SWT.BORDER | SWT.SINGLE);
        proposalsViewer = new TreeViewer(tree);
        tree.setHeaderVisible(false);
        tree.setLinesVisible(true);

        TreeColumn column = new TreeColumn(tree, SWT.None);
        layout.setColumnData(column, new ColumnWeightData(100));

        proposalsViewer.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                if (element instanceof Proposal proposal)
                {
                    return proposal.getCategory();
                }
                else if (element instanceof AccountTransaction tx)
                {
                    return tx.toString();
                }
                else
                {
                    return null;
                }
            }

        });

        proposalsViewer.setContentProvider(new ITreeContentProvider()
        {
            @Override
            public boolean hasChildren(Object element)
            {
                if (!(element instanceof Proposal))
                    return false;

                return !((Proposal) element).getCandidates().isEmpty();
            }

            @Override
            public Object getParent(Object element)
            {
                return null;
            }

            @Override
            public Object[] getElements(Object inputElement)
            {
                return ((List<?>) inputElement).toArray();
            }

            @Override
            public Object[] getChildren(Object parentElement)
            {
                if (parentElement instanceof Proposal proposal)
                    return proposal.getCandidates().toArray();

                return new Object[0];
            }
        });

        return treeArea;
    }
}
