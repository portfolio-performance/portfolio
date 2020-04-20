package name.abuchen.portfolio.ui.views;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.trades.Trade;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.MoneyColorLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.NumberColorLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.SharesLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.views.columns.NameColumn;

public class TradesTableViewer
{
    public enum ViewMode
    {
        SINGLE_SECURITY, MULTIPLE_SECURITES
    }

    private AbstractFinanceView view;

    private TableViewer trades;
    private ShowHideColumnHelper support;

    public TradesTableViewer(AbstractFinanceView view)
    {
        this.view = view;
    }

    public Control createViewControl(Composite parent, ViewMode viewMode)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        trades = new TableViewer(container, SWT.FULL_SELECTION);

        ColumnViewerToolTipSupport.enableFor(trades, ToolTip.NO_RECREATE);

        support = new ShowHideColumnHelper(
                        SecuritiesPerformanceView.class.getSimpleName() + "@trades@" + viewMode.name(), //$NON-NLS-1$
                        view.getPreferenceStore(), trades, layout);
        createTradesColumns(support, viewMode);
        support.createColumns();

        trades.getTable().setHeaderVisible(true);
        trades.getTable().setLinesVisible(true);
        trades.setContentProvider(ArrayContentProvider.getInstance());

        return container;
    }

    private void createTradesColumns(ShowHideColumnHelper support, ViewMode viewMode)
    {
        if (viewMode == ViewMode.MULTIPLE_SECURITES)
            support.addColumn(new NameColumn());

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

            @Override
            public Color getBackground(Object e)
            {
                return ((Trade) e).getEnd().isPresent() ? null : Colors.WARNING;
            }
        });
        column.setSorter(ColumnViewerSorter.create(e -> {
            Optional<LocalDateTime> date = ((Trade) e).getEnd();
            return date.isPresent() ? date.get() : null;
        }));
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
                return ((Trade) e).getTransactions().stream().map(TransactionPair::toString)
                                .collect(Collectors.joining("\n")); //$NON-NLS-1$
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
        column.setLabelProvider(
                        new MoneyColorLabelProvider(element -> ((Trade) element).getProfitLoss(), view.getClient()));
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

        column = new Column("irr", Messages.ColumnIRR, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setMenuLabel(Messages.ColumnIRR_MenuLabel);
        column.setLabelProvider(new NumberColorLabelProvider<>(Values.Percent2, t -> ((Trade) t).getIRR()));
        column.setSorter(ColumnViewerSorter.create(e -> ((Trade) e).getIRR()));
        support.addColumn(column);

        column = new Column("portfolio", Messages.ColumnPortfolio, SWT.LEFT, 100); //$NON-NLS-1$
        column.setMenuLabel(Messages.ColumnPortfolio);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return ((Trade) e).getPortfolio().getName();
            }
        });
        column.setSorter(ColumnViewerSorter.create(e -> ((Trade) e).getPortfolio().getName()));
        column.setVisible(false);
        support.addColumn(column);
    }

    public void setInput(List<Trade> trades)
    {
        this.trades.setInput(trades);
    }

    public Object getInput()
    {
        return this.trades.getInput();
    }

    public TableViewer getTableViewer()
    {
        return trades;
    }

    public ShowHideColumnHelper getShowHideColumnHelper()
    {
        return support;
    }
}
