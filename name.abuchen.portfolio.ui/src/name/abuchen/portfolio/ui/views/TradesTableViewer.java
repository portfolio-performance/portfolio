package name.abuchen.portfolio.ui.views;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;

import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.model.Named;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.trades.Trade;
import name.abuchen.portfolio.snapshot.trades.TradeCategory;
import name.abuchen.portfolio.snapshot.trades.TradeTotals;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.TabularDataSource;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport.TouchClientListener;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.CopyPasteSupport;
import name.abuchen.portfolio.ui.util.viewers.DateTimeLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.MoneyColorLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.NumberColorLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.SharesLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.viewers.ToolTipCustomProviderSupport;
import name.abuchen.portfolio.ui.views.columns.IsinColumn;
import name.abuchen.portfolio.ui.views.columns.NameColumn;
import name.abuchen.portfolio.ui.views.columns.SymbolColumn;
import name.abuchen.portfolio.ui.views.columns.WknColumn;
import name.abuchen.portfolio.ui.views.trades.TradeElement;
import name.abuchen.portfolio.util.TextUtil;

public class TradesTableViewer
{
    public enum ViewMode
    {
        SINGLE_SECURITY, MULTIPLE_SECURITIES
    }

    private AbstractFinanceView view;

    private TableViewer trades;
    private ShowHideColumnHelper support;
    private Font boldFont;

    public TradesTableViewer(AbstractFinanceView view)
    {
        this.view = view;
    }

    private static TradeElement asElement(Object element)
    {
        return element instanceof TradeElement te ? te : null;
    }

    private static Money applyWeight(Money money, double weight)
    {
        if (money == null || Double.compare(weight, 1.0) == 0)
            return money;
        // TradeElements representing grouped trades expose a fractional weight.
        // scale the monetary value so category rows show weighted totals.
        return money.multiplyAndRound(weight);
    }

    private static Function<Object, Comparable<?>> toComparable(Function<TradeElement, ?> provider)
    {
        return element -> {
            TradeElement tradeElement = asElement(element);
            if (tradeElement == null)
                return null;
            Object value = provider.apply(tradeElement);
            return value instanceof Comparable<?> comparable ? comparable : null;
        };
    }

    private static <T> Function<Object, T> adapt(Function<TradeElement, T> provider)
    {
        return element -> {
            TradeElement tradeElement = asElement(element);
            return tradeElement != null ? provider.apply(tradeElement) : null;
        };
    }

    private static <T> Function<TradeElement, T> tradeValue(Function<Trade, T> tradeGetter)
    {
        return element -> element != null && element.isTrade() ? tradeGetter.apply(element.getTrade()) : null;
    }

    private static <T> Function<TradeElement, T> tradeValue(BiFunction<Trade, TradeElement, T> tradeGetter)
    {
        return element -> element != null && element.isTrade() ? tradeGetter.apply(element.getTrade(), element) : null;
    }

    private static <T> Function<TradeElement, T> tradeAggregateValue(Function<Trade, T> tradeGetter,
                    Function<TradeCategory, T> categoryGetter, Function<TradeTotals, T> totalsGetter)
    {
        return tradeAggregateValue((trade, element) -> tradeGetter.apply(trade), categoryGetter, totalsGetter);
    }

    private static <T> Function<TradeElement, T> tradeAggregateValue(BiFunction<Trade, TradeElement, T> tradeGetter,
                    Function<TradeCategory, T> categoryGetter, Function<TradeTotals, T> totalsGetter)
    {
        return element -> {
            if (element == null)
                return null;

            if (element.isTrade())
                return tradeGetter.apply(element.getTrade(), element);

            if (element.isCategory())
                return categoryGetter.apply(element.getCategory());

            if (element.isTotal())
                return totalsGetter.apply(element.getTotals());

            return null;
        };
    }

    private String formatMoney(Money money)
    {
        return money != null ? Values.Money.format(money, view.getClient().getBaseCurrency()) : null;
    }

    static Double getReturnValue(TradeElement element)
    {
        if (element == null)
            return null;

        if (element.isTrade())
            return element.getTrade().getReturn();

        if (element.isTotal())
            return element.getTotals().getAverageReturn();

        if (element.isCategory())
            return element.getCategory().getAverageReturn();

        return null;
    }

    static Double getReturnMovingAverageValue(TradeElement element)
    {
        if (element == null)
            return null;

        if (element.isTrade())
            return element.getTrade().getReturnMovingAverage();

        if (element.isTotal())
            return element.getTotals().getAverageReturnMovingAverage();

        if (element.isCategory())
            return element.getCategory().getAverageReturnMovingAverage();

        return null;
    }

    public Control createViewControl(Composite parent, ViewMode viewMode)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        trades = new TableViewer(container, SWT.FULL_SELECTION);
        view.getStylingEngine().style(trades.getTable());

        var resources = new LocalResourceManager(JFaceResources.getResources(), trades.getTable());
        boldFont = resources.create(FontDescriptor.createFrom(trades.getTable().getFont()).setStyle(SWT.BOLD));
        trades.getTable().addDisposeListener(e -> resources.dispose());

        ColumnEditingSupport.prepare(view.getEditorActivationState(), trades);
        ToolTipCustomProviderSupport.enableFor(trades, ToolTip.NO_RECREATE);
        CopyPasteSupport.enableFor(trades);

        support = new ShowHideColumnHelper(
                        SecuritiesPerformanceView.class.getSimpleName() + "@trades@" + viewMode.name(), //$NON-NLS-1$
                        view.getPreferenceStore(), trades, layout);
        createTradesColumns(support, viewMode);
        support.createColumns(true);

        trades.getTable().setHeaderVisible(true);
        trades.getTable().setLinesVisible(true);
        trades.setContentProvider(ArrayContentProvider.getInstance());

        return container;
    }

    private void createTradesColumns(ShowHideColumnHelper support, ViewMode viewMode)
    {
        Column column;

        if (viewMode == ViewMode.MULTIPLE_SECURITIES)
        {
            // Custom name column that handles both trades (showing security
            // name)
            // and categories (showing classification name in bold)
            column = new NameColumn(view.getClient());
            column.setLabelProvider(withBoldFont(new ColumnLabelProvider()
            {
                @Override
                public String getText(Object e)
                {
                    TradeElement element = asElement(e);
                    if (element == null)
                        return null;

                    if (element.isTrade())
                        return element.getTrade().getSecurity().getName();

                    if (element.isTotal())
                        return Messages.ColumnSum;

                    if (element.isCategory())
                        return element.getCategory().getClassification().getName();

                    return null;
                }

                @Override
                public Image getImage(Object e)
                {
                    TradeElement element = asElement(e);
                    if (element != null && element.isTrade())
                        return Images.SECURITY.image();
                    return null;
                }
            }));
            column.setSorter(ColumnViewerSorter.create(e -> {
                TradeElement element = asElement(e);
                if (element == null)
                    return null;
                if (element.isTrade())
                    return element.getTrade().getSecurity().getName();
                if (element.isTotal())
                    return ""; //$NON-NLS-1$
                if (element.isCategory())
                    return element.getCategory().getClassification().getName();
                return ""; //$NON-NLS-1$
            }));
            column.getEditingSupport().addListener(new TouchClientListener(view.getClient()));
            column.getEditingSupport().addListener((element, newValue, oldValue) -> trades.refresh(true));
            support.addColumn(column);
        }

        column = new Column("start", Messages.ColumnStartDate, SWT.None, 80); //$NON-NLS-1$
        var startDate = tradeValue(Trade::getStart);
        var startDateProvider = adapt(startDate);
        column.setLabelProvider(withBoldFont(new DateTimeLabelProvider(startDateProvider)));
        column.setSorter(ColumnViewerSorter.create(toComparable(startDate)), SWT.DOWN);
        support.addColumn(column);

        column = new Column("end", Messages.ColumnEndDate, SWT.None, 80); //$NON-NLS-1$
        var endDate = tradeValue(trade -> trade.getEnd().orElse(null));
        var endDateProvider = adapt(endDate);
        var endSortValue = tradeValue((trade, element) -> trade.getEnd().orElse(LocalDateTime.now().plusYears(1)));
        column.setLabelProvider(withBoldFont(new DateTimeLabelProvider(endDateProvider, Messages.LabelOpenTrade)
        {
            @Override
            public String getText(Object e)
            {
                TradeElement element = asElement(e);
                if (element != null && (element.isCategory() || element.isTotal()))
                    return null;
                return super.getText(e);
            }

            @Override
            public Color getBackground(Object e)
            {
                TradeElement element = asElement(e);
                if (element != null && (element.isCategory() || element.isTotal()))
                    return null;
                Trade trade = element != null && element.isTrade() ? element.getTrade() : null;
                return trade != null && trade.isClosed() ? null : Colors.theme().warningBackground();
            }
        }));
        column.setSorter(ColumnViewerSorter.create(toComparable(endSortValue)));
        support.addColumn(column);

        column = new Column("tx", Messages.ColumnNumberOfTransactions, SWT.RIGHT, 80); //$NON-NLS-1$
        var transactionsSize = tradeValue(trade -> trade.getTransactions().size());
        var transactionsSizeProvider = adapt(transactionsSize);
        column.setLabelProvider(withBoldFont(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Integer size = transactionsSizeProvider.apply(e);
                return size != null ? String.valueOf(size) : null;
            }

            @Override
            public Image getImage(Object element)
            {
                return transactionsSizeProvider.apply(element) != null ? Images.INFO.image() : null;
            }
        }));
        column.setToolTipProvider(e -> {
            TradeElement element = asElement(e);
            Trade trade = element != null && element.isTrade() ? element.getTrade() : null;
            if (trade == null)
                return null;

            return new TabularDataSource(Messages.LabelTrades, builder -> {
                builder.addColumns(new TabularDataSource.Column(Messages.ColumnDate, SWT.LEFT, 100) //
                                .withFormatter(o -> Values.DateTime
                                                .format(((TransactionPair<?>) o).getTransaction().getDateTime())), //
                                new TabularDataSource.Column(Messages.ColumnTransactionType, SWT.LEFT), //
                                new TabularDataSource.Column(Messages.ColumnShares) //
                                                .withFormatter(o -> Values.Share.formatNonZero((Long) o)), //
                                new TabularDataSource.Column(Messages.ColumnQuote) //
                                                .withFormatter(o -> Values.CalculatedQuote.format((Quote) o, //
                                                                view.getClient().getBaseCurrency())), //
                                new TabularDataSource.Column(Messages.ColumnAmount) //
                                                .withFormatter(o -> Values.Money.formatNonZero((Money) o,
                                                                view.getClient().getBaseCurrency())), //
                                new TabularDataSource.Column(Messages.ColumnFees) //
                                                .withFormatter(o -> Values.Money.formatNonZero((Money) o,
                                                                view.getClient().getBaseCurrency())), //
                                new TabularDataSource.Column(Messages.ColumnTaxes) //
                                                .withFormatter(o -> Values.Money.formatNonZero((Money) o,
                                                                view.getClient().getBaseCurrency())), //
                                new TabularDataSource.Column(Messages.ColumnTotal).withFormatter(o -> Values.Money
                                                .formatNonZero((Money) o, view.getClient().getBaseCurrency())), //
                                new TabularDataSource.Column(Messages.ColumnPortfolio, SWT.LEFT, 220)
                                                .withFormatter(o -> o instanceof Named n ? n.getName() : null)
                                                .withLogo());

                trade.getTransactions().stream().forEach(pair -> {

                    Object[] row = new Object[9];
                    row[0] = pair;
                    pair.withAccountTransaction().ifPresent(t -> row[1] = t.getTransaction().getType().toString());
                    pair.withPortfolioTransaction().ifPresent(t -> row[1] = t.getTransaction().getType().toString());
                    row[2] = pair.getTransaction().getShares();
                    row[3] = pair.getTransaction().getGrossPricePerShare();
                    row[4] = pair.getTransaction().getGrossValue();
                    row[5] = pair.getTransaction().getUnitSum(Unit.Type.FEE);
                    row[6] = pair.getTransaction().getUnitSum(Unit.Type.TAX);
                    row[7] = pair.getTransaction().getMonetaryAmount();
                    row[8] = pair.getOwner();

                    builder.addRow(row);
                });
            });
        });
        column.setSorter(ColumnViewerSorter.create(toComparable(transactionsSize)));
        support.addColumn(column);

        column = new Column("shares", Messages.ColumnShares, SWT.None, 80); //$NON-NLS-1$
        Function<TradeElement, Long> weightedShares = element -> element != null ? element.getWeightedShares() : null;
        var weightedSharesProvider = adapt(weightedShares);
        column.setLabelProvider(new SharesLabelProvider() // NOSONAR
        {
            @Override
            protected void measure(Event event, Object element)
            {
                Font previous = event.gc.getFont();
                TradeElement tradeElement = asElement(element);
                if (boldFont != null && tradeElement != null && (tradeElement.isCategory() || tradeElement.isTotal()))
                    event.gc.setFont(boldFont);
                super.measure(event, element);
                event.gc.setFont(previous);
            }

            @Override
            protected void paint(Event event, Object element)
            {
                Font previous = event.gc.getFont();
                TradeElement tradeElement = asElement(element);
                if (boldFont != null && tradeElement != null && (tradeElement.isCategory() || tradeElement.isTotal()))
                    event.gc.setFont(boldFont);
                super.paint(event, element);
                event.gc.setFont(previous);
            }

            @Override
            public Long getValue(Object e)
            {
                return weightedSharesProvider.apply(e);
            }
        });
        column.setSorter(ColumnViewerSorter.create(toComparable(weightedShares)));
        support.addColumn(column);

        column = new Column("entryvalue", Messages.ColumnEntryValue, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.ColumnEntryValue);
        column.setMenuLabel(Messages.ColumnEntryValue + " (" + CostMethod.FIFO.getLabel() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        var entryValue = tradeAggregateValue(
                        (trade, element) -> applyWeight(trade.getEntryValue(), element.getWeight()),
                        TradeCategory::getTotalEntryValue, TradeTotals::getTotalEntryValue);
        var entryValueProvider = adapt(entryValue);
        column.setLabelProvider(withBoldFont(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return formatMoney(entryValueProvider.apply(e));
            }
        }));
        column.setSorter(ColumnViewerSorter.create(toComparable(entryValue)));
        support.addColumn(column);

        column = new Column("entryvalue-mvavg", //$NON-NLS-1$
                        Messages.ColumnEntryValue + " (" + CostMethod.MOVING_AVERAGE.getAbbreviation() + ")", //$NON-NLS-1$ //$NON-NLS-2$
                        SWT.RIGHT, 80);
        column.setGroupLabel(Messages.ColumnEntryValue);
        column.setMenuLabel(Messages.ColumnEntryValue + " (" + CostMethod.MOVING_AVERAGE.getLabel() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        var entryValueMovingAverage = tradeAggregateValue(
                        (trade, element) -> applyWeight(trade.getEntryValueMovingAverage(), element.getWeight()),
                        TradeCategory::getTotalEntryValueMovingAverage, TradeTotals::getTotalEntryValueMovingAverage);
        var entryValueMovingAverageProvider = adapt(entryValueMovingAverage);
        column.setLabelProvider(withBoldFont(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return formatMoney(entryValueMovingAverageProvider.apply(e));
            }
        }));
        column.setSorter(ColumnViewerSorter.create(toComparable(entryValueMovingAverage)));
        column.setVisible(false);
        support.addColumn(column);

        Function<Trade, Money> averagePurchasePrice = t -> {
            if (t.getShares() == 0)
                return null;
            Money tradeEntryValue = t.getEntryValue();
            return Money.of(tradeEntryValue.getCurrencyCode(),
                            Math.round(tradeEntryValue.getAmount() / (double) t.getShares() * Values.Share.factor()));
        };

        column = new Column("entryvalue-pershare", Messages.ColumnEntryValue + " (" + Messages.ColumnPerShare + ")", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        SWT.RIGHT, 80);
        column.setGroupLabel(Messages.ColumnEntryValue);
        column.setMenuLabel(Messages.ColumnEntryValue + " (" + Messages.ColumnPerShare + ")" + " (" //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
                        + CostMethod.FIFO.getLabel() + ")"); //$NON-NLS-1$
        var averageEntryPerShare = tradeValue(averagePurchasePrice);
        var averageEntryPerShareProvider = adapt(averageEntryPerShare);
        column.setLabelProvider(withBoldFont(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return formatMoney(averageEntryPerShareProvider.apply(e));
            }
        }));
        column.setSorter(ColumnViewerSorter.create(toComparable(averageEntryPerShare)));
        column.setVisible(false);
        support.addColumn(column);

        Function<Trade, Money> averagePurchasePriceMovingAverage = t -> {
            if (t.getShares() == 0)
                return null;
            Money movingAverageEntry = t.getEntryValueMovingAverage();
            if (movingAverageEntry == null)
                return null;
            return Money.of(movingAverageEntry.getCurrencyCode(), Math
                            .round(movingAverageEntry.getAmount() / (double) t.getShares() * Values.Share.factor()));
        };

        column = new Column("entryvalue-mvavg-pershare", //$NON-NLS-1$
                        Messages.ColumnEntryValue + " (" + Messages.ColumnPerShare + ")" + " (" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                        + CostMethod.MOVING_AVERAGE.getAbbreviation() + ")", //$NON-NLS-1$
                        SWT.RIGHT, 80);
        column.setGroupLabel(Messages.ColumnEntryValue);
        column.setMenuLabel(Messages.ColumnEntryValue + " (" + Messages.ColumnPerShare + ")" + " (" //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
                        + CostMethod.MOVING_AVERAGE.getLabel() + ")"); //$NON-NLS-1$
        var averageEntryPerShareMovingAverage = tradeValue(averagePurchasePriceMovingAverage);
        var averageEntryPerShareMovingAverageProvider = adapt(averageEntryPerShareMovingAverage);
        column.setLabelProvider(withBoldFont(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return formatMoney(averageEntryPerShareMovingAverageProvider.apply(e));
            }
        }));
        column.setSorter(ColumnViewerSorter.create(toComparable(averageEntryPerShareMovingAverage)));
        column.setVisible(false);
        support.addColumn(column);

        column = new Column("exitvalue", Messages.ColumnExitValue, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.ColumnExitValue);
        var exitValue = tradeAggregateValue(
                        (trade, element) -> applyWeight(trade.getExitValue(), element.getWeight()),
                        TradeCategory::getTotalExitValue, TradeTotals::getTotalExitValue);
        var exitValueProvider = adapt(exitValue);
        column.setLabelProvider(withBoldFont(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return formatMoney(exitValueProvider.apply(e));
            }
        }));
        column.setSorter(ColumnViewerSorter.create(toComparable(exitValue)));
        support.addColumn(column);

        Function<Trade, Money> averageSellPrice = t -> {
            if (t.getShares() == 0)
                return null;
            Money tradeExitValue = t.getExitValue();
            return Money.of(tradeExitValue.getCurrencyCode(),
                            Math.round(tradeExitValue.getAmount() / (double) t.getShares() * Values.Share.factor()));
        };

        column = new Column("exitvalue-pershare", Messages.ColumnExitValue + " (" + Messages.ColumnPerShare + ")", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        SWT.RIGHT, 80);
        column.setGroupLabel(Messages.ColumnExitValue);
        var averageExitPerShare = tradeValue(averageSellPrice);
        var averageExitPerShareProvider = adapt(averageExitPerShare);
        column.setLabelProvider(withBoldFont(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return formatMoney(averageExitPerShareProvider.apply(e));
            }
        }));
        column.setSorter(ColumnViewerSorter.create(toComparable(averageExitPerShare)));
        column.setVisible(false);
        support.addColumn(column);

        column = new Column("pl", Messages.ColumnProfitLoss, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.ColumnProfitLoss);
        column.setMenuLabel(Messages.ColumnProfitLoss + " (" + CostMethod.FIFO.getLabel() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        var profitLoss = tradeAggregateValue(
                        (trade, element) -> applyWeight(trade.getProfitLoss(), element.getWeight()),
                        TradeCategory::getTotalProfitLoss, TradeTotals::getTotalProfitLoss);
        column.setLabelProvider(withBoldFont(new MoneyColorLabelProvider(adapt(profitLoss), view.getClient())));
        column.setSorter(ColumnViewerSorter.create(toComparable(profitLoss)));
        support.addColumn(column);

        column = new Column("gpl", Messages.ColumnGrossProfitLoss, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.ColumnProfitLoss);
        column.setMenuLabel(Messages.ColumnGrossProfitLoss + " (" + CostMethod.FIFO.getLabel() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        var grossProfitLoss = tradeAggregateValue(
                        (trade, element) -> applyWeight(trade.getProfitLossWithoutTaxesAndFees(),
                                        element.getWeight()),
                        TradeCategory::getTotalProfitLossWithoutTaxesAndFees,
                        TradeTotals::getTotalProfitLossWithoutTaxesAndFees);
        column.setLabelProvider(withBoldFont(new MoneyColorLabelProvider(adapt(grossProfitLoss), view.getClient())));
        column.setSorter(ColumnViewerSorter.create(toComparable(grossProfitLoss)));
        column.setVisible(false);
        support.addColumn(column);

        column = new Column("pl-mvavg", //$NON-NLS-1$
                        Messages.ColumnProfitLoss + " (" + CostMethod.MOVING_AVERAGE.getAbbreviation() + ")", //$NON-NLS-1$ //$NON-NLS-2$
                        SWT.RIGHT, 80);
        column.setGroupLabel(Messages.ColumnProfitLoss);
        column.setMenuLabel(Messages.ColumnProfitLoss + " (" + CostMethod.MOVING_AVERAGE.getLabel() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        var profitLossMovingAverage = tradeAggregateValue(
                        (trade, element) -> applyWeight(trade.getProfitLossMovingAverage(), element.getWeight()),
                        TradeCategory::getTotalProfitLossMovingAverage, TradeTotals::getTotalProfitLossMovingAverage);
        column.setLabelProvider(
                        withBoldFont(new MoneyColorLabelProvider(adapt(profitLossMovingAverage), view.getClient())));
        column.setSorter(ColumnViewerSorter.create(toComparable(profitLossMovingAverage)));
        column.setVisible(false);
        support.addColumn(column);

        column = new Column("gpl-mavg", //$NON-NLS-1$
                        Messages.ColumnGrossProfitLoss + " (" + CostMethod.MOVING_AVERAGE.getAbbreviation() + ")", //$NON-NLS-1$ //$NON-NLS-2$
                        SWT.RIGHT, 80);
        column.setGroupLabel(Messages.ColumnProfitLoss);
        column.setMenuLabel(Messages.ColumnGrossProfitLoss + " (" + CostMethod.MOVING_AVERAGE.getLabel() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        var grossProfitLossMovingAverage = tradeAggregateValue(
                        (trade, element) -> applyWeight(trade.getProfitLossMovingAverageWithoutTaxesAndFees(),
                                        element.getWeight()),
                        TradeCategory::getTotalProfitLossMovingAverageWithoutTaxesAndFees,
                        TradeTotals::getTotalProfitLossMovingAverageWithoutTaxesAndFees);
        column.setLabelProvider(
                        withBoldFont(new MoneyColorLabelProvider(adapt(grossProfitLossMovingAverage), view.getClient())));
        column.setSorter(ColumnViewerSorter.create(toComparable(grossProfitLossMovingAverage)));
        column.setVisible(false);
        support.addColumn(column);

        column = new Column("holdingperiod", Messages.ColumnHoldingPeriod, SWT.RIGHT, 80); //$NON-NLS-1$
        var holdingPeriod = tradeAggregateValue(Trade::getHoldingPeriod, TradeCategory::getAverageHoldingPeriod,
                        TradeTotals::getAverageHoldingPeriod);
        var holdingPeriodProvider = adapt(holdingPeriod);
        column.setLabelProvider(withBoldFont(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Long value = holdingPeriodProvider.apply(e);
                return value != null ? Long.toString(value) : null;
            }
        }));
        column.setSorter(ColumnViewerSorter.create(toComparable(holdingPeriod)));
        support.addColumn(column);

        column = new Column("latesttrade", Messages.ColumnLatestTrade, SWT.None, 80); //$NON-NLS-1$
        var latestTradeDate = tradeValue(trade -> trade.getLastTransaction().getTransaction().getDateTime());
        column.setLabelProvider(withBoldFont(new DateTimeLabelProvider(adapt(latestTradeDate))));
        column.setSorter(ColumnViewerSorter.create(toComparable(latestTradeDate)));
        column.setVisible(false);
        support.addColumn(column);

        column = new Column("irr", Messages.ColumnIRR, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setMenuLabel(Messages.ColumnIRR_MenuLabel);
        var irrValue = tradeAggregateValue(Trade::getIRR, TradeCategory::getAverageIRR, TradeTotals::getAverageIRR);
        column.setLabelProvider(withBoldFont(new NumberColorLabelProvider<>(Values.Percent2, adapt(irrValue))));
        column.setSorter(ColumnViewerSorter.create(toComparable(irrValue)));
        support.addColumn(column);

        column = new Column("return", Messages.ColumnReturn, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.ColumnReturn);
        column.setMenuLabel(Messages.ColumnReturn + " (" + CostMethod.FIFO.getLabel() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        Function<TradeElement, Double> returnValue = TradesTableViewer::getReturnValue;
        var returnValueProvider = adapt(returnValue);
        column.setLabelProvider(withBoldFont(new NumberColorLabelProvider<>(Values.Percent2, returnValueProvider)));
        column.setSorter(ColumnViewerSorter.create(toComparable(returnValue)));
        column.setVisible(false);
        support.addColumn(column);

        column = new Column("return-mavg", //$NON-NLS-1$
                        Messages.ColumnReturn + " (" + CostMethod.MOVING_AVERAGE.getAbbreviation() + ")", //$NON-NLS-1$ //$NON-NLS-2$
                        SWT.RIGHT, 80);
        column.setGroupLabel(Messages.ColumnReturn);
        column.setMenuLabel(Messages.ColumnReturn + " (" + CostMethod.MOVING_AVERAGE.getLabel() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        Function<TradeElement, Double> returnMovingAverage = TradesTableViewer::getReturnMovingAverageValue;
        var returnMovingAverageProvider = adapt(returnMovingAverage);
        column.setLabelProvider(
                        withBoldFont(new NumberColorLabelProvider<>(Values.Percent2, returnMovingAverageProvider)));
        column.setSorter(ColumnViewerSorter.create(toComparable(returnMovingAverage)));
        column.setVisible(false);
        support.addColumn(column);

        column = new Column("note", Messages.ColumnNote, SWT.LEFT, 80); //$NON-NLS-1$
        var tradeNote = tradeValue(trade -> trade.getLastTransaction().getTransaction().getNote());
        var tradeNoteProvider = adapt(tradeNote);
        column.setLabelProvider(withBoldFont(new ColumnLabelProvider()
        {
            private String getRawText(Object e)
            {
                return tradeNoteProvider.apply(e);
            }

            @Override
            public String getText(Object e)
            {
                String note = getRawText(e);
                return note == null || note.isEmpty() ? null : TextUtil.toSingleLine(note);
            }

            @Override
            public Image getImage(Object e)
            {
                String note = getRawText(e);
                return note != null && note.length() > 0 ? Images.NOTE.image() : null;
            }

            @Override
            public String getToolTipText(Object e)
            {
                String note = getRawText(e);
                return note == null || note.isEmpty() ? null : TextUtil.wordwrap(note);
            }
        }));
        column.setSorter(ColumnViewerSorter.createIgnoreCase(tradeNoteProvider));
        support.addColumn(column);

        column = new Column("portfolio", Messages.ColumnPortfolio, SWT.LEFT, 100); //$NON-NLS-1$
        column.setMenuLabel(Messages.ColumnPortfolio);
        var portfolioName = tradeValue(trade -> trade.getPortfolio().getName());
        var portfolioNameProvider = adapt(portfolioName);
        column.setLabelProvider(withBoldFont(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return portfolioNameProvider.apply(e);
            }
        }));
        column.setSorter(ColumnViewerSorter.createIgnoreCase(portfolioNameProvider));
        column.setVisible(false);
        support.addColumn(column);

        column = new IsinColumn();
        column.setGroupLabel(Messages.ColumnSecurity);
        column.getEditingSupport().addListener(new TouchClientListener(view.getClient()));
        column.setVisible(false);
        support.addColumn(column);

        column = new SymbolColumn();
        column.setGroupLabel(Messages.ColumnSecurity);
        column.getEditingSupport().addListener(new TouchClientListener(view.getClient()));
        column.setVisible(false);
        support.addColumn(column);

        column = new WknColumn();
        column.setGroupLabel(Messages.ColumnSecurity);
        column.getEditingSupport().addListener(new TouchClientListener(view.getClient()));
        column.setVisible(false);
        support.addColumn(column);

        column = new Column("instrumentCurrency", Messages.ColumnCurrency, SWT.LEFT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.ColumnSecurity);
        var instrumentCurrency = tradeValue(trade -> trade.getSecurity().getCurrencyCode());
        var instrumentCurrencyProvider = adapt(instrumentCurrency);
        column.setLabelProvider(withBoldFont(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return instrumentCurrencyProvider.apply(element);
            }
        }));
        column.setSorter(ColumnViewerSorter.createIgnoreCase(instrumentCurrencyProvider));
        column.setVisible(false);
        support.addColumn(column);

        // Wrap all sorters with TradeElementComparator to maintain taxonomy
        // grouping
        support.getColumns().forEach(col -> {
            if (col.getSorter() != null)
                col.getSorter().wrap(TradeElementComparator::new);
        });
    }

    private ColumnLabelProvider withBoldFont(ColumnLabelProvider delegate)
    {
        return new ColumnLabelProvider()
        {
            @Override
            public void update(ViewerCell cell)
            {
                delegate.update(cell);

                Object element = cell.getElement();
                Font font = delegate.getFont(element);
                TradeElement tradeElement = asElement(element);
                if (tradeElement != null && (tradeElement.isCategory() || tradeElement.isTotal()))
                    cell.setFont(boldFont != null ? boldFont : font);
                else
                    cell.setFont(font);
            }

            @Override
            public String getText(Object element)
            {
                return delegate.getText(element);
            }

            @Override
            public Image getImage(Object element)
            {
                return delegate.getImage(element);
            }

            @Override
            public Color getForeground(Object element)
            {
                return delegate.getForeground(element);
            }

            @Override
            public Color getBackground(Object element)
            {
                return delegate.getBackground(element);
            }

            @Override
            public String getToolTipText(Object element)
            {
                return delegate.getToolTipText(element);
            }

            @Override
            public Point getToolTipShift(Object object)
            {
                return delegate.getToolTipShift(object);
            }

            @Override
            public int getToolTipDisplayDelayTime(Object object)
            {
                return delegate.getToolTipDisplayDelayTime(object);
            }

            @Override
            public int getToolTipTimeDisplayed(Object object)
            {
                return delegate.getToolTipTimeDisplayed(object);
            }

            @Override
            public boolean useNativeToolTip(Object object)
            {
                return delegate.useNativeToolTip(object);
            }

            @Override
            public Color getToolTipBackgroundColor(Object object)
            {
                return delegate.getToolTipBackgroundColor(object);
            }

            @Override
            public Color getToolTipForegroundColor(Object object)
            {
                return delegate.getToolTipForegroundColor(object);
            }

            @Override
            public Font getToolTipFont(Object object)
            {
                return delegate.getToolTipFont(object);
            }

            @Override
            public boolean isLabelProperty(Object element, String property)
            {
                return delegate.isLabelProperty(element, property);
            }

            @Override
            public Font getFont(Object element)
            {
                TradeElement tradeElement = asElement(element);
                if (tradeElement != null && (tradeElement.isCategory() || tradeElement.isTotal()))
                    return boldFont != null ? boldFont : delegate.getFont(element);
                return delegate.getFont(element);
            }

            @Override
            public void dispose()
            {
                delegate.dispose();
                super.dispose();
            }
        };
    }

    public void setInput(List<TradeElement> items)
    {
        this.trades.setInput(items);
    }

    @SuppressWarnings("unchecked")
    public List<TradeElement> getInput()
    {
        return (List<TradeElement>) this.trades.getInput();
    }

    public TableViewer getTableViewer()
    {
        return trades;
    }

    public ShowHideColumnHelper getShowHideColumnHelper()
    {
        return support;
    }

    /**
     * Comparator that sorts by TradeElement sortOrder first (to keep taxonomy
     * groups together), then by the wrapped comparator (to sort within each
     * group). Similar to StatementOfAssetsViewer.ElementComparator.
     */
    public static class TradeElementComparator implements Comparator<Object>
    {
        private final Comparator<Object> comparator;

        public TradeElementComparator(Comparator<Object> wrapped)
        {
            this.comparator = wrapped;
        }

        @Override
        public int compare(Object o1, Object o2)
        {
            TradeElement first = (TradeElement) o1;
            TradeElement second = (TradeElement) o2;

            int a = first.getSortOrder();
            int b = second.getSortOrder();

            if (a != b)
            {
                int direction = ColumnViewerSorter.SortingContext.getSortDirection();
                return direction == SWT.UP ? a - b : b - a;
            }

            return comparator.compare(o1, o2);
        }
    }
}
