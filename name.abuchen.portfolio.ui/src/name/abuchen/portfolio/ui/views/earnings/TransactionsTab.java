package name.abuchen.portfolio.ui.views.earnings;

import java.text.MessageFormat;

import javax.inject.Inject;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.selection.SecuritySelection;
import name.abuchen.portfolio.ui.selection.SelectionService;
import name.abuchen.portfolio.ui.util.TableViewerCSVExporter;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.SharesLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.util.TextUtil;

public class TransactionsTab implements EarningsTab
{
    @Inject
    private Client client;

    @Inject
    private EarningsViewModel model;

    @Inject
    private SelectionService selectionService;

    @Inject
    private IPreferenceStore preferences;

    private TableViewer tableViewer;

    @Override
    public String getLabel()
    {
        return Messages.LabelTransactions;
    }

    @Override
    public void addExportActions(IMenuManager manager)
    {
        manager.add(new Action(MessageFormat.format(Messages.LabelExport, Messages.LabelTransactions))
        {
            @Override
            public void run()
            {
                new TableViewerCSVExporter(tableViewer).export(Messages.LabelTransactions + ".csv"); //$NON-NLS-1$
            }
        });
    }

    @Override
    public Control createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        tableViewer = new TableViewer(container, SWT.FULL_SELECTION | SWT.MULTI);
        ColumnViewerToolTipSupport.enableFor(tableViewer, ToolTip.NO_RECREATE);

        ShowHideColumnHelper support = new ShowHideColumnHelper(TransactionsTab.class.getSimpleName() + "@v2", //$NON-NLS-1$
                        preferences, tableViewer, layout);

        addColumns(support);
        support.createColumns();

        tableViewer.getTable().setHeaderVisible(true);
        tableViewer.getTable().setLinesVisible(true);
        tableViewer.setContentProvider(ArrayContentProvider.getInstance());

        tableViewer.addSelectionChangedListener(event -> {
            TransactionPair<?> tx = ((TransactionPair<?>) ((IStructuredSelection) event.getSelection())
                            .getFirstElement());
            if (tx != null && tx.getTransaction().getSecurity() != null)
                selectionService.setSelection(
                                new SecuritySelection(model.getClient(), tx.getTransaction().getSecurity()));
        });

        tableViewer.setInput(model.getTransactions());

        model.addUpdateListener(() -> tableViewer.setInput(model.getTransactions()));

        return container;
    }

    private void addColumns(ShowHideColumnHelper support)
    {
        Column column = new Column(Messages.ColumnDate, SWT.None, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return Values.DateTime.format(((TransactionPair<?>) element).getTransaction().getDateTime());
            }
        });
        ColumnViewerSorter.create(e -> ((TransactionPair<?>) e).getTransaction().getDateTime()).attachTo(column, SWT.UP);
        support.addColumn(column);

        column = new Column(Messages.ColumnSecurity, SWT.None, 250);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                Security security = ((TransactionPair<?>) element).getTransaction().getSecurity();
                return security != null ? security.getName() : null;
            }

            @Override
            public Image getImage(Object element)
            {
                Security security = ((TransactionPair<?>) element).getTransaction().getSecurity();
                return security != null ? Images.SECURITY.image() : null;
            }
        });
        ColumnViewerSorter.create(e -> ((TransactionPair<?>) e).getTransaction().getSecurity().getName())
                        .attachTo(column);
        support.addColumn(column);

        column = new Column(Messages.ColumnShares, SWT.RIGHT, 80);
        column.setLabelProvider(new SharesLabelProvider()
        {
            @Override
            public Long getValue(Object element)
            {
                return ((TransactionPair<?>) element).getTransaction().getShares();
            }
        });
        ColumnViewerSorter.create(e -> ((TransactionPair<?>) e).getTransaction().getShares()).attachTo(column);
        support.addColumn(column);

        column = new Column(Messages.ColumnGrossValue, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return Values.Money.format(
                                ((AccountTransaction) ((TransactionPair<?>) element).getTransaction()).getGrossValue(),
                                client.getBaseCurrency());
            }
        });
        ColumnViewerSorter.create(e -> ((TransactionPair<?>) e).getTransaction().getMonetaryAmount()).attachTo(column);
        support.addColumn(column);

        column = new Column(Messages.ColumnTaxes, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return Values.Money.format(((TransactionPair<?>) element).getTransaction().getUnitSum(Unit.Type.TAX),
                                client.getBaseCurrency());
            }
        });
        ColumnViewerSorter.create(e -> ((TransactionPair<?>) e).getTransaction().getUnitSum(Unit.Type.TAX))
                        .attachTo(column);
        support.addColumn(column);

        column = new Column(Messages.ColumnAmount, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return Values.Money.format(((TransactionPair<?>) element).getTransaction().getMonetaryAmount(),
                                client.getBaseCurrency());
            }
        });
        ColumnViewerSorter.create(e -> ((TransactionPair<?>) e).getTransaction().getMonetaryAmount()).attachTo(column);
        support.addColumn(column);

        column = new Column(Messages.ColumnOffsetAccount, SWT.None, 120);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((TransactionPair<?>) element).getOwner().toString();
            }

            @Override
            public Image getImage(Object element)
            {
                return Images.ACCOUNT.image();
            }
        });
        ColumnViewerSorter.create(e -> ((TransactionPair<?>) e).getOwner().toString()).attachTo(column);
        support.addColumn(column);

        column = new Column(Messages.ColumnNote, SWT.None, 200);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((TransactionPair<?>) element).getTransaction().getNote();
            }

            @Override
            public Image getImage(Object element)
            {
                String note = ((TransactionPair<?>) element).getTransaction().getNote();
                return note != null && note.length() > 0 ? Images.NOTE.image() : null;
            }

            @Override
            public String getToolTipText(Object e)
            {
                return TextUtil.wordwrap(getText(e));
            }
        });
        ColumnViewerSorter.create(e -> ((TransactionPair<?>) e).getTransaction().getNote()).attachTo(column);
        support.addColumn(column);
    }
}
