package name.abuchen.portfolio.ui.views;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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
import name.abuchen.portfolio.model.TaxesAndFees;
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
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.LogoManager;
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

    /**
     * Helper method to extract Trade from either a Trade or TradeElement
     * 
     * @return the Trade, or null if the element is a category
     */
    private static Trade asTrade(Object element)
    {
        return switch (element)
        {
            case Trade trade -> trade;
            case TradeElement te -> te.isTrade() ? te.getTrade() : null;
            default -> null;
        };
    }

    private static double getTradeWeight(Object element)
    {
        if (element instanceof TradeElement te && te.isTrade())
            return te.getWeight();
        return 1.0;
    }

    private static Money applyWeight(Money money, double weight)
    {
        if (money == null || Double.compare(weight, 1.0) == 0)
            return money;
        // TradeElements representing grouped trades expose a fractional weight.
        // scale the monetary value so category rows show weighted totals.
        return money.multiplyAndRound(weight);
    }

    private static Function<Object, Comparable<?>> toComparable(Function<Object, ?> provider)
    {
        return element -> {
            Object value = provider.apply(element);
            return value instanceof Comparable<?> comparable ? comparable : null;
        };
    }

    private static <T> Function<Object, T> tradeValue(Function<Trade, T> tradeGetter)
    {
        return element -> {
            Trade trade = asTrade(element);
            return trade != null ? tradeGetter.apply(trade) : null;
        };
    }

    private static <T> Function<Object, T> tradeValue(BiFunction<Trade, Object, T> tradeGetter)
    {
        return element -> {
            Trade trade = asTrade(element);
            return trade != null ? tradeGetter.apply(trade, element) : null;
        };
    }

    private static <T> Function<Object, T> tradeAggregateValue(Function<Trade, T> tradeGetter,
                    Function<TradeCategory, T> categoryGetter, Function<TradeTotals, T> totalsGetter)
    {
        return tradeAggregateValue((trade, element) -> tradeGetter.apply(trade), categoryGetter, totalsGetter);
    }

    private static <T> Function<Object, T> tradeAggregateValue(BiFunction<Trade, Object, T> tradeGetter,
                    Function<TradeCategory, T> categoryGetter, Function<TradeTotals, T> totalsGetter)
    {
        return element -> {
            Trade trade = asTrade(element);
            if (trade != null)
                return tradeGetter.apply(trade, element);

            TradeCategory category = asCategory(element);
            if (category != null)
                return categoryGetter.apply(category);

            TradeTotals totals = asTotals(element);
            return totals != null ? totalsGetter.apply(totals) : null;
        };
    }

    private String formatMoney(Money money)
    {
        return money != null ? Values.Money.format(money, view.getClient().getBaseCurrency()) : null;
    }

    /**
     * Helper method to check if an element is a category row
     */
    private static boolean isCategory(Object element)
    {
        return element instanceof TradeElement te && te.isCategory();
    }

    /**
     * Helper method to extract TradeCategory from a TradeElement
     * 
     * @return the TradeCategory, or null if not a category
     */
    private static TradeCategory asCategory(Object element)
    {
        if (element instanceof TradeElement te)
            return te.isCategory() ? te.getCategory() : null;
        else
            return null;
    }

    /* package */ static Double getReturnValue(Object element, CostMethod costMethod)
    {
        Objects.requireNonNull(costMethod);

        Trade trade = asTrade(element);
        if (trade != null)
            return trade.getReturn(costMethod);

        TradeTotals totals = asTotals(element);
        if (totals != null)
            return totals.getAverageReturn(costMethod);

        TradeCategory category = asCategory(element);
        return category != null ? category.getAverageReturn(costMethod) : null;
    }

    /**
     * Helper method to check if an element is a totals row
     */
    private static boolean isTotal(Object element)
    {
        return element instanceof TradeElement te && te.isTotal();
    }

    /**
     * Helper method to extract TradeTotals from a TradeElement
     *
     * @return the TradeTotals, or null if not a totals row
     */
    private static TradeTotals asTotals(Object element)
    {
        if (element instanceof TradeElement te)
            return te.isTotal() ? te.getTotals() : null;
        else
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
        CostMethod costMethod = view.getGlobalCostMethod();
        createTradesColumns(support, viewMode, costMethod);
        support.createColumns(true);

        trades.getTable().setHeaderVisible(true);
        trades.getTable().setLinesVisible(true);
        trades.setContentProvider(ArrayContentProvider.getInstance());

        return container;
    }

    void createTradesColumns(ShowHideColumnHelper support, ViewMode viewMode, CostMethod costMethod)
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
                    Trade trade = asTrade(e);
                    if (trade != null)
                        return trade.getSecurity().getName();

                    TradeTotals totals = asTotals(e);
                    if (totals != null)
                        return Messages.ColumnSum;

                    TradeCategory category = asCategory(e);
                    if (category != null)
                        return category.getClassification().getName();

                    return null;
                }

                @Override
                public Image getImage(Object e)
                {
                    Trade trade = asTrade(e);
                    if (trade != null)
                        return LogoManager.instance().getDefaultColumnImage(trade.getSecurity(),
                                        view.getClient().getSettings());
                    return null;
                }
            }));
            column.setSorter(ColumnViewerSorter.create(e -> {
                Trade trade = asTrade(e);
                if (trade != null)
                    return trade.getSecurity().getName();
                if (isTotal(e))
                    return ""; //$NON-NLS-1$
                TradeCategory category = asCategory(e);
                return category != null ? category.getClassification().getName() : ""; //$NON-NLS-1$
            }));
            column.getEditingSupport().addListener(new TouchClientListener(view.getClient()));
            column.getEditingSupport().addListener((element, newValue, oldValue) -> trades.refresh(true));
            support.addColumn(column);
        }

        column = new Column("tradeStartDate", Messages.ColumnStartDate, SWT.None, 80).addAliasIDs("start"); //$NON-NLS-1$ //$NON-NLS-2$
        var startDate = tradeValue(Trade::getStart);
        column.setLabelProvider(withBoldFont(new DateTimeLabelProvider(startDate)));
        column.setSorter(ColumnViewerSorter.create(toComparable(startDate)), SWT.DOWN);
        support.addColumn(column);

        column = new Column("tradeEndDate", Messages.ColumnEndDate, SWT.None, 80).addAliasIDs("end"); //$NON-NLS-1$ //$NON-NLS-2$
        var endDate = tradeValue(trade -> trade.getEnd().orElse(null));
        var endSortValue = tradeValue((trade, element) -> trade.getEnd().orElse(LocalDateTime.now().plusYears(1)));
        column.setLabelProvider(withBoldFont(new DateTimeLabelProvider(endDate, Messages.LabelOpenTrade)
        {
            @Override
            public String getText(Object e)
            {
                if (isCategory(e) || isTotal(e))
                    return null;
                return super.getText(e);
            }

            @Override
            public Color getBackground(Object e)
            {
                if (isCategory(e) || isTotal(e))
                    return null;
                Trade trade = asTrade(e);
                return trade != null && trade.isClosed() ? null : Colors.theme().warningBackground();
            }
        }));
        column.setSorter(ColumnViewerSorter.create(toComparable(endSortValue)));
        support.addColumn(column);

        column = new Column("transactionCount", Messages.ColumnNumberOfTransactions, SWT.RIGHT, 80) //$NON-NLS-1$
                        .addAliasIDs("tx"); //$NON-NLS-1$
        var transactionsSize = tradeValue(trade -> trade.getTransactions().size());
        column.setLabelProvider(withBoldFont(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Integer size = transactionsSize.apply(e);
                return size != null ? String.valueOf(size) : null;
            }

            @Override
            public Image getImage(Object element)
            {
                return transactionsSize.apply(element) != null ? Images.INFO.image() : null;
            }
        }));
        column.setToolTipProvider(e -> {
            Trade trade = asTrade(e);
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

        column = new Column("sharesOwned", Messages.ColumnShares, SWT.None, 80).addAliasIDs("shares"); //$NON-NLS-1$ //$NON-NLS-2$
        var weightedShares = tradeValue((trade, element) -> {
            if (element instanceof TradeElement te)
                return te.getWeightedShares();
            return trade.getShares();
        });
        column.setLabelProvider(new SharesLabelProvider() // NOSONAR
        {
            @Override
            protected void measure(Event event, Object element)
            {
                Font previous = event.gc.getFont();
                if (boldFont != null && (isCategory(element) || isTotal(element)))
                    event.gc.setFont(boldFont);
                super.measure(event, element);
                event.gc.setFont(previous);
            }

            @Override
            protected void paint(Event event, Object element)
            {
                Font previous = event.gc.getFont();
                if (boldFont != null && (isCategory(element) || isTotal(element)))
                    event.gc.setFont(boldFont);
                super.paint(event, element);
                event.gc.setFont(previous);
            }

            @Override
            public Long getValue(Object e)
            {
                return weightedShares.apply(e);
            }
        });
        column.setSorter(ColumnViewerSorter.create(toComparable(weightedShares)));
        support.addColumn(column);

        column = new Column("entryValueIncludingTaxesAndFees", Messages.ColumnEntryValue, SWT.RIGHT, 80) //$NON-NLS-1$
                        .addAliasIDs("entryValueGross", "entryValue", "entryvalue", "entryvalue-mvavg"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        column.setGroupLabel(Messages.ColumnEntryValue);
        var entryValueGross = tradeAggregateValue(
                        (trade, element) -> applyWeight(
                                        trade.getEntryValue(costMethod, TaxesAndFees.INCLUDED),
                                        getTradeWeight(element)),
                        category -> category.getTotalEntryValue(costMethod), totals -> totals.getTotalEntryValue(costMethod));
        column.setLabelProvider(withBoldFont(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return formatMoney(entryValueGross.apply(e));
            }
        }));
        column.setSorter(ColumnViewerSorter.create(toComparable(entryValueGross)));
        support.addColumn(column);

        Function<Trade, Money> averageEntryPriceGross = trade -> {
            if (trade.getShares() == 0)
                return null;

            Money value = trade.getEntryValue(costMethod, TaxesAndFees.INCLUDED);
            if (value == null)
                return null;

            return Money.of(value.getCurrencyCode(),
                            Math.round(value.getAmount() / (double) trade.getShares() * Values.Share.factor()));
        };

        column = new Column("entryValuePerShareIncludingTaxesAndFees", //$NON-NLS-1$
                        Messages.ColumnEntryValue + " (" + Messages.ColumnPerShare + ")", SWT.RIGHT, 80) //$NON-NLS-1$ //$NON-NLS-2$
                        .addAliasIDs("entryValuePerShareGross", "entryValuePerShare", "entryvalue-pershare", "entryvalue-mvavg-pershare"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        column.setGroupLabel(Messages.ColumnEntryValue);
        var averageEntryPerShare = tradeValue(averageEntryPriceGross);
        column.setLabelProvider(withBoldFont(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return formatMoney(averageEntryPerShare.apply(e));
            }
        }));
        column.setSorter(ColumnViewerSorter.create(toComparable(averageEntryPerShare)));
        column.setVisible(false);
        support.addColumn(column);

        column = new Column("exitValue", Messages.ColumnExitValue, SWT.RIGHT, 80).addAliasIDs("exitvalue"); //$NON-NLS-1$ //$NON-NLS-2$
        column.setGroupLabel(Messages.ColumnExitValue);
        var exitValue = tradeAggregateValue(
                        (trade, element) -> applyWeight(trade.getExitValue(), getTradeWeight(element)),
                        TradeCategory::getTotalExitValue, TradeTotals::getTotalExitValue);
        column.setLabelProvider(withBoldFont(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return formatMoney(exitValue.apply(e));
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

        column = new Column("exitValuePerShare", //$NON-NLS-1$
                        Messages.ColumnExitValue + " (" + Messages.ColumnPerShare + ")", SWT.RIGHT, 80) //$NON-NLS-1$ //$NON-NLS-2$
                        .addAliasIDs("exitvalue-pershare"); //$NON-NLS-1$
        column.setGroupLabel(Messages.ColumnExitValue);
        var averageExitPerShare = tradeValue(averageSellPrice);
        column.setLabelProvider(withBoldFont(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return formatMoney(averageExitPerShare.apply(e));
            }
        }));
        column.setSorter(ColumnViewerSorter.create(toComparable(averageExitPerShare)));
        column.setVisible(false);
        support.addColumn(column);

        column = new Column("profitLossIncludingTaxesAndFees", Messages.ColumnProfitLoss, SWT.RIGHT, 80) //$NON-NLS-1$
                        .addAliasIDs("profitLossNet", "profitLoss", "pl", "pl-mvavg"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        column.setGroupLabel(Messages.ColumnProfitLoss);
        var profitLossNet = tradeAggregateValue(
                        (trade, element) -> applyWeight(
                                        trade.getProfitLoss(costMethod, TaxesAndFees.INCLUDED),
                                        getTradeWeight(element)),
                        category -> category.getTotalProfitLoss(costMethod, TaxesAndFees.INCLUDED),
                        totals -> totals.getTotalProfitLoss(costMethod, TaxesAndFees.INCLUDED));
        column.setLabelProvider(withBoldFont(new MoneyColorLabelProvider(profitLossNet, view.getClient())));
        column.setSorter(ColumnViewerSorter.create(toComparable(profitLossNet)));
        support.addColumn(column);

        column = new Column("profitLossExcludingTaxesAndFees", Messages.ColumnGrossProfitLoss, SWT.RIGHT, 80) //$NON-NLS-1$
                        .addAliasIDs("profitLossGross", "grossProfitLoss", "gpl", "gpl-mavg"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        column.setGroupLabel(Messages.ColumnProfitLoss);
        var profitLossGross = tradeAggregateValue(
                        (trade, element) -> applyWeight(
                                        trade.getProfitLoss(costMethod, TaxesAndFees.NOT_INCLUDED),
                                        getTradeWeight(element)),
                        category -> category.getTotalProfitLoss(costMethod, TaxesAndFees.NOT_INCLUDED),
                        totals -> totals.getTotalProfitLoss(costMethod, TaxesAndFees.NOT_INCLUDED));
        column.setLabelProvider(withBoldFont(new MoneyColorLabelProvider(profitLossGross, view.getClient())));
        column.setSorter(ColumnViewerSorter.create(toComparable(profitLossGross)));
        column.setVisible(false);
        support.addColumn(column);

        column = new Column("holdingPeriod", Messages.ColumnHoldingPeriod, SWT.RIGHT, 80) //$NON-NLS-1$
                        .addAliasIDs("holdingperiod"); //$NON-NLS-1$
        var holdingPeriod = tradeAggregateValue(Trade::getHoldingPeriod, TradeCategory::getAverageHoldingPeriod,
                        TradeTotals::getAverageHoldingPeriod);
        column.setLabelProvider(withBoldFont(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Long value = holdingPeriod.apply(e);
                return value != null ? Long.toString(value) : null;
            }
        }));
        column.setSorter(ColumnViewerSorter.create(toComparable(holdingPeriod)));
        support.addColumn(column);

        column = new Column("latestTradeDate", Messages.ColumnLatestTrade, SWT.None, 80) //$NON-NLS-1$
                        .addAliasIDs("latesttrade"); //$NON-NLS-1$
        var latestTradeDate = tradeValue(trade -> trade.getLastTransaction().getTransaction().getDateTime());
        column.setLabelProvider(withBoldFont(new DateTimeLabelProvider(latestTradeDate)));
        column.setSorter(ColumnViewerSorter.create(toComparable(latestTradeDate)));
        column.setVisible(false);
        support.addColumn(column);

        column = new Column("internalRateOfReturn", Messages.ColumnIRR, SWT.RIGHT, 80).addAliasIDs("irr"); //$NON-NLS-1$ //$NON-NLS-2$
        column.setMenuLabel(Messages.ColumnIRR_MenuLabel);
        var irrValue = tradeAggregateValue(Trade::getIRR, TradeCategory::getAverageIRR, TradeTotals::getAverageIRR);
        column.setLabelProvider(withBoldFont(new NumberColorLabelProvider<>(Values.Percent2, irrValue)));
        column.setSorter(ColumnViewerSorter.create(toComparable(irrValue)));
        support.addColumn(column);

        column = new Column("returnRate", Messages.ColumnReturn, SWT.RIGHT, 80) //$NON-NLS-1$
                        .addAliasIDs("return", "return-mavg"); //$NON-NLS-1$ //$NON-NLS-2$
        Function<Object, Double> returnValue = element -> getReturnValue(element, costMethod);
        column.setLabelProvider(withBoldFont(new NumberColorLabelProvider<>(Values.Percent2, returnValue::apply)));
        column.setSorter(ColumnViewerSorter.create(toComparable(returnValue)));
        column.setVisible(false);
        support.addColumn(column);

        column = new Column("note", Messages.ColumnNote, SWT.LEFT, 80); //$NON-NLS-1$
        var tradeNote = tradeValue(trade -> trade.getLastTransaction().getTransaction().getNote());
        column.setLabelProvider(withBoldFont(new ColumnLabelProvider()
        {
            private String getRawText(Object e)
            {
                return tradeNote.apply(e);
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
        column.setSorter(ColumnViewerSorter.createIgnoreCase(tradeNote));
        support.addColumn(column);

        column = new Column("portfolio", Messages.ColumnPortfolio, SWT.LEFT, 100); //$NON-NLS-1$
        column.setMenuLabel(Messages.ColumnPortfolio);
        var portfolioName = tradeValue(trade -> trade.getPortfolio().getName());
        column.setLabelProvider(withBoldFont(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return portfolioName.apply(e);
            }

            @Override
            public Image getImage(Object e)
            {
                Trade trade = asTrade(e);
                if (trade != null)
                    return LogoManager.instance().getDefaultColumnImage(trade.getPortfolio(),
                                    view.getClient().getSettings());
                return null;
            }
        }));
        column.setSorter(ColumnViewerSorter.createIgnoreCase(portfolioName));
        column.setVisible(false);
        support.addColumn(column);

        column = new IsinColumn();
        column.setGroupLabel(Messages.ColumnSecurity);
        column.getEditingSupport().addListener(new TouchClientListener(view.getClient()));
        column.setVisible(false);
        support.addColumn(column);

        column = new SymbolColumn("tickerSymbol").addAliasIDs("symbol"); //$NON-NLS-1$ //$NON-NLS-2$
        column.setGroupLabel(Messages.ColumnSecurity);
        column.getEditingSupport().addListener(new TouchClientListener(view.getClient()));
        column.setVisible(false);
        support.addColumn(column);

        column = new WknColumn();
        column.setGroupLabel(Messages.ColumnSecurity);
        column.getEditingSupport().addListener(new TouchClientListener(view.getClient()));
        column.setVisible(false);
        support.addColumn(column);

        column = new Column("currency", Messages.ColumnCurrency, SWT.LEFT, 80).addAliasIDs("instrumentCurrency"); //$NON-NLS-1$ //$NON-NLS-2$
        column.setGroupLabel(Messages.ColumnSecurity);
        var instrumentCurrency = tradeValue(trade -> trade.getSecurity().getCurrencyCode());
        column.setLabelProvider(withBoldFont(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return instrumentCurrency.apply(element);
            }
        }));
        column.setSorter(ColumnViewerSorter.createIgnoreCase(instrumentCurrency));
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
                if (isCategory(element) || isTotal(element))
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
                if (isCategory(element) || isTotal(element))
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

    public void setInput(List<?> items)
    {
        this.support.invalidateCache();
        this.trades.setInput(items);
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

    /**
     * Comparator that sorts by TradeElement sortOrder first (to keep taxonomy
     * groups together), then by the wrapped comparator (to sort within each
     * group). Similar to StatementOfAssetsViewer.ElementComparator.
     */
    public static class TradeElementComparator implements Comparator<Object>
    {
        private Comparator<Object> comparator;

        public TradeElementComparator(Comparator<Object> wrapped)
        {
            this.comparator = wrapped;
        }

        @Override
        public int compare(Object o1, Object o2)
        {
            // Extract sortOrder from TradeElements, otherwise use 0 for plain
            // Trades
            int a = o1 instanceof TradeElement te ? te.getSortOrder() : 0;
            int b = o2 instanceof TradeElement te ? te.getSortOrder() : 0;

            if (a != b)
            {
                int direction = ColumnViewerSorter.SortingContext.getSortDirection();
                // The viewer applies the comparator in both ascending and
                // descending order; respect that by flipping the taxonomy
                // order comparison when descending is requested.
                return direction == SWT.UP ? a - b : b - a;
            }

            // Same sortOrder, use wrapped comparator to sort within the group
            return comparator.compare(o1, o2);
        }
    }
}
