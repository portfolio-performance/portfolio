package name.abuchen.portfolio.ui.views;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.trades.Trade;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.MoneyColorLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.SharesLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;

public class TradesTableViewer
{
    private AbstractFinanceView view;

    private TableViewer trades;

    public TradesTableViewer(AbstractFinanceView view)
    {
        this.view = view;
    }

    public Control createViewControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        trades = new TableViewer(container, SWT.FULL_SELECTION);

        ColumnViewerToolTipSupport.enableFor(trades, ToolTip.NO_RECREATE);

        ShowHideColumnHelper support = new ShowHideColumnHelper(
                        SecuritiesPerformanceView.class.getSimpleName() + "@trades-beta", //$NON-NLS-1$
                        view.getPreferenceStore(), trades, layout);
        createTradesColumns(support);
        support.createColumns();

        trades.getTable().setHeaderVisible(true);
        trades.getTable().setLinesVisible(true);
        trades.setContentProvider(ArrayContentProvider.getInstance());

        return container;
    }

    private void createTradesColumns(ShowHideColumnHelper support)
    {
        Column column = new Column("start", Messages.ColumnStartDate, SWT.None, 80); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Trade t = (Trade) e;
                return Values.DateTime.format(t.getStart());
            }
        });
        column.setSorter(ColumnViewerSorter.create(e -> ((Trade) e).getStart()), SWT.DOWN);
        support.addColumn(column);

        column = new Column("end", Messages.ColumnEndDate, SWT.None, 80); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Trade t = (Trade) e;
                return t.getEnd().isPresent() ? Values.DateTime.format(t.getEnd().get()) : Messages.LabelOpenTrade; // NOSONAR
            }
        });
        column.setSorter(ColumnViewerSorter.create(e -> ((Trade) e).getEnd().get()));
        support.addColumn(column);

        column = new Column("tx", Messages.ColumnNumberOfTransactions, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Trade t = (Trade) e;
                return String.valueOf(t.getTransactions().size());
            }

            @Override
            public Image getImage(Object element)
            {
                return Images.INFO.image();
            }

            @Override
            public String getToolTipText(Object e)
            {
                Trade t = (Trade) e;
                return String.join("\n", //$NON-NLS-1$
                                t.getTransactions().stream().map(tx -> String.format("%s %10s Stk. %-10s %s  %s", //$NON-NLS-1$
                                                Values.DateTime.format(tx.getTransaction().getDateTime()),
                                                Values.Share.format(tx.getTransaction().getShares()), //
                                                tx.getTransaction().getType(),
                                                Values.Money.format(tx.getTransaction().getMonetaryAmount()),
                                                tx.getOwner().toString())).collect(Collectors.toList()));
            }
        });
        column.setSorter(ColumnViewerSorter.create(e -> ((Trade) e).getTransactions().size()));
        support.addColumn(column);

        column = new Column("shares", Messages.ColumnShares, SWT.None, 80); //$NON-NLS-1$
        column.setLabelProvider(new SharesLabelProvider() // NOSONAR
        {
            @Override
            public Long getValue(Object e)
            {
                Trade t = (Trade) e;
                return t.getShares();
            }
        });
        column.setSorter(ColumnViewerSorter.create(e -> ((Trade) e).getShares()));
        support.addColumn(column);

        column = new Column("entryvalue", Messages.ColumnEntryValue, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Trade t = (Trade) e;
                return Values.Money.format(t.getEntryValue(), view.getClient().getBaseCurrency());
            }
        });
        column.setSorter(ColumnViewerSorter.create(e -> ((Trade) e).getEntryValue()));
        support.addColumn(column);

        column = new Column("exitvalue", Messages.ColumnExitValue, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Trade t = (Trade) e;
                return Values.Money.format(t.getExitValue(), view.getClient().getBaseCurrency());
            }
        });
        column.setSorter(ColumnViewerSorter.create(e -> ((Trade) e).getExitValue()));
        support.addColumn(column);

        column = new Column("pl", Messages.ColumnProfitLoss, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setLabelProvider(new MoneyColorLabelProvider(element -> ((Trade) element).getProfitLoss(),
                        view.getClient().getBaseCurrency()));
        column.setSorter(ColumnViewerSorter.create(e -> ((Trade) e).getProfitLoss()));
        support.addColumn(column);

        column = new Column("holdingperiod", Messages.ColumnHoldingPeriod, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Trade t = (Trade) e;
                return Long.toString(t.getHoldingPeriod());
            }
        });
        column.setSorter(ColumnViewerSorter.create(e -> ((Trade) e).getHoldingPeriod()));
        support.addColumn(column);

    }

    public void setInput(List<Trade> trades)
    {
        this.trades.setInput(trades);
    }
}
