package name.abuchen.portfolio.ui.views.panes;

import java.util.Optional;

import javax.inject.Inject;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionOwner;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.snapshot.security.CalculationLineItem;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceRecord;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.LogoManager;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.SharesLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;

public class CaclulationLineItemPane implements InformationPanePage
{
    @Inject
    private Client client;

    @Inject
    private IPreferenceStore preferences;

    private TableViewer transactions;

    private SecurityPerformanceRecord record;

    @Override
    public String getLabel()
    {
        return Messages.SecurityTabTransactions;
    }

    @Override
    public Control createViewControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);

        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        transactions = new TableViewer(container, SWT.FULL_SELECTION);
        ColumnViewerToolTipSupport.enableFor(transactions, ToolTip.NO_RECREATE);

        ShowHideColumnHelper support = new ShowHideColumnHelper(CaclulationLineItemPane.class.getSimpleName(),
                        preferences, transactions, layout);

        createTransactionColumns(support);
        support.createColumns();

        transactions.getTable().setHeaderVisible(true);
        transactions.getTable().setLinesVisible(true);
        transactions.setContentProvider(ArrayContentProvider.getInstance());
        return container;
    }

    private void createTransactionColumns(ShowHideColumnHelper support)
    {
        // date
        Column column = new Column(Messages.ColumnDate, SWT.None, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return Values.DateTime.format(((CalculationLineItem) e).getDateTime());
            }
        });
        column.setSorter(ColumnViewerSorter.create((o1, o2) -> {

            CalculationLineItem c1 = (CalculationLineItem) o1;
            CalculationLineItem c2 = (CalculationLineItem) o2;

            return c1.getDateTime().compareTo(c2.getDateTime());
        }));
        support.addColumn(column);

        // transaction type
        column = new Column(Messages.ColumnTransactionType, SWT.None, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return ((CalculationLineItem) e).getLabel();
            }
        });
        support.addColumn(column);

        // shares
        column = new Column(Messages.ColumnShares, SWT.None, 80);
        column.setLabelProvider(new SharesLabelProvider() // NOSONAR
        {
            @Override
            public Long getValue(Object e)
            {
                CalculationLineItem data = (CalculationLineItem) e;

                Optional<SecurityPosition> position = data.getSecurityPosition();
                if (position.isPresent())
                    return position.get().getShares();

                Optional<Transaction> transaction = data.getTransaction();
                if (transaction.isPresent() && transaction.get().getShares() != 0L)
                    return transaction.get().getShares();

                return null;
            }
        });
        support.addColumn(column);

        // dividend amount
        column = new Column(Messages.ColumnDividendPayment, SWT.RIGHT, 80);
        column.setDescription(Messages.ColumnGrossDividend);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                CalculationLineItem item = (CalculationLineItem) e;
                if (item instanceof CalculationLineItem.DividendPayment)
                    return Values.Money.format(((CalculationLineItem.DividendPayment) item).getGrossValue(),
                                    client.getBaseCurrency());
                else
                    return null;
            }
        });
        support.addColumn(column);

        // dividend per share
        column = new Column(Messages.ColumnDividendPerShare, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                CalculationLineItem item = (CalculationLineItem) e;
                if (item instanceof CalculationLineItem.DividendPayment)
                    return Values.AmountFraction
                                    .formatNonZero(((CalculationLineItem.DividendPayment) item).getDividendPerShare());
                else
                    return null;
            }
        });
        support.addColumn(column);

        // dividend per share
        column = new Column(Messages.ColumnPersonalDividendYield, SWT.RIGHT, 80);
        column.setDescription(Messages.ColumnPersonalDividendYield_Description);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                CalculationLineItem item = (CalculationLineItem) e;
                if (item instanceof CalculationLineItem.DividendPayment)
                    return Values.Percent2.formatNonZero(
                                    ((CalculationLineItem.DividendPayment) item).getPersonalDividendYield());
                else
                    return null;
            }
        });
        support.addColumn(column);

        // dividend per share (moving average)
        column = new Column(Messages.ColumnPersonalDividendYieldMovingAverage, SWT.RIGHT, 80);
        column.setDescription(Messages.ColumnPersonalDividendYieldMovingAverage_Description);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                CalculationLineItem item = (CalculationLineItem) e;
                if (item instanceof CalculationLineItem.DividendPayment)
                    return Values.Percent2.formatNonZero(((CalculationLineItem.DividendPayment) item)
                                    .getPersonalDividendYieldMovingAverage());
                else
                    return null;
            }
        });
        support.addColumn(column);

        // einstandskurs / bewertung
        column = new Column(Messages.ColumnAmount, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                CalculationLineItem item = (CalculationLineItem) e;
                if (item instanceof CalculationLineItem.DividendPayment)
                    return null;
                else
                    return Values.Money.format(item.getValue(), client.getBaseCurrency());
            }
        });
        support.addColumn(column);

        // purchase quote
        column = new Column(Messages.ColumnQuote, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Optional<Transaction> tx = ((CalculationLineItem) e).getTransaction();

                if (tx.isPresent() && tx.get() instanceof PortfolioTransaction)
                {
                    PortfolioTransaction ptx = (PortfolioTransaction) tx.get();
                    return Values.CalculatedQuote.format(ptx.getGrossPricePerShare(), client.getBaseCurrency());
                }
                else
                {
                    return null;
                }
            }
        });
        support.addColumn(column);

        // gegenkonto
        column = new Column(Messages.ColumnAccount, SWT.None, 120);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return String.valueOf(((CalculationLineItem) e).getOwner());
            }

            @Override
            public Image getImage(Object e)
            {
                TransactionOwner<?> owner = ((CalculationLineItem) e).getOwner();
                return LogoManager.instance().getDefaultColumnImage(owner, client.getSettings());
            }

        });
        support.addColumn(column);

        // note
        column = new Column("note", Messages.ColumnNote, SWT.LEFT, 22); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Optional<Transaction> transaction = ((CalculationLineItem) e).getTransaction();
                return transaction.isPresent() ? transaction.get().getNote() : null;
            }

            @Override
            public Image getImage(Object e)
            {
                String note = getText(e);
                return note != null && note.length() > 0 ? Images.NOTE.image() : null;
            }
        });
        column.setSorter(ColumnViewerSorter.create((o1, o2) -> {

            Optional<Transaction> t1 = ((CalculationLineItem) o1).getTransaction();
            String s1 = t1.isPresent() ? t1.get().getNote() : ""; //$NON-NLS-1$
            Optional<Transaction> t2 = ((CalculationLineItem) o2).getTransaction();
            String s2 = t2.isPresent() ? t2.get().getNote() : ""; //$NON-NLS-1$
            // notes can be null
            if (s1 == null)
                s1 = ""; //$NON-NLS-1$
            if (s2 == null)
                s2 = ""; //$NON-NLS-1$

            return s1.compareTo(s2);
        }));
        support.addColumn(column);
    }

    @Override
    public void setInput(Object input)
    {
        record = Adaptor.adapt(SecurityPerformanceRecord.class, input);

        if (record != null)
        {
            transactions.setInput(record.getLineItems());
            transactions.refresh();
        }
        else
        {
            transactions.setInput(null);
            transactions.refresh();
        }
    }

    @Override
    public void onRecalculationNeeded()
    {
        if (record != null)
            setInput(record);
    }
}
