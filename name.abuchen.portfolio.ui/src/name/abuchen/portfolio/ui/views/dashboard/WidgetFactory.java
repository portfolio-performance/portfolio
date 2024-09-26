package name.abuchen.portfolio.ui.views.dashboard;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.LongStream;

import name.abuchen.portfolio.math.AllTimeHigh;
import name.abuchen.portfolio.math.Risk.Drawdown;
import name.abuchen.portfolio.math.Risk.Volatility;
import name.abuchen.portfolio.model.ClientProperties;
import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot.CategoryType;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.views.dashboard.charts.ClientDataSeriesChartWidget;
import name.abuchen.portfolio.ui.views.dashboard.charts.DrawdownChartWidget;
import name.abuchen.portfolio.ui.views.dashboard.charts.HoldingsChartWidget;
import name.abuchen.portfolio.ui.views.dashboard.charts.TaxonomyChartWidget;
import name.abuchen.portfolio.ui.views.dashboard.earnings.EarningsByTaxonomyChartWidget;
import name.abuchen.portfolio.ui.views.dashboard.earnings.EarningsChartWidget;
import name.abuchen.portfolio.ui.views.dashboard.earnings.EarningsHeatmapWidget;
import name.abuchen.portfolio.ui.views.dashboard.earnings.EarningsListWidget;
import name.abuchen.portfolio.ui.views.dashboard.earnings.EarningsListWidget.ExpansionSetting;
import name.abuchen.portfolio.ui.views.dashboard.heatmap.CostHeatmapWidget;
import name.abuchen.portfolio.ui.views.dashboard.heatmap.InvestmentHeatmapWidget;
import name.abuchen.portfolio.ui.views.dashboard.heatmap.PerformanceHeatmapWidget;
import name.abuchen.portfolio.ui.views.dashboard.heatmap.YearlyPerformanceHeatmapWidget;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries;
import name.abuchen.portfolio.ui.views.payments.PaymentsViewModel;

public enum WidgetFactory
{
    HEADING(Messages.LabelHeading, Messages.LabelCommon, HeadingWidget::new),

    DESCRIPTION(Messages.LabelDescription, Messages.LabelCommon, DescriptionWidget::new),

    TOTAL_SUM(Messages.LabelTotalSum, Messages.LabelStatementOfAssets, //
                    (widget, data) -> IndicatorWidget.<Money>create(widget, data) //
                                    .with(Values.Money) //
                                    .with((ds, period) -> {
                                        PerformanceIndex index = data.calculate(ds, period);
                                        int length = index.getTotals().length;
                                        return Money.of(index.getCurrency(), index.getTotals()[length - 1]);
                                    }) //
                                    .withBenchmarkDataSeries(false) //
                                    .build()),

    TTWROR(Messages.LabelTTWROR, Messages.ClientEditorLabelPerformance, // cumulative
                    (widget, data) -> IndicatorWidget.<Double>create(widget, data) //
                                    .with(Values.Percent2) //
                                    .with((ds, period) -> {
                                        PerformanceIndex index = data.calculate(ds, period);
                                        return index.getFinalAccumulatedPercentage();
                                    }).build()),

    TTWROR_ANNUALIZED(Messages.LabelTTWROR_Annualized, Messages.ClientEditorLabelPerformance, //
                    (widget, data) -> IndicatorWidget.<Double>create(widget, data) //
                                    .with(Values.AnnualizedPercent2) //
                                    .with((ds, period) -> {
                                        PerformanceIndex index = data.calculate(ds, period);
                                        return index.getFinalAccumulatedAnnualizedPercentage();
                                    }).build()),

    IRR(Messages.LabelIRR, Messages.ClientEditorLabelPerformance, //
                    (widget, data) -> IndicatorWidget.<Double>create(widget, data) //
                                    .with(Values.AnnualizedPercent2) //
                                    .with((ds, period) -> data.calculate(ds, period).getPerformanceIRR()) //
                                    .build()),

    ABSOLUTE_CHANGE(Messages.LabelAbsoluteChange, Messages.LabelStatementOfAssets, //
                    (widget, data) -> IndicatorWidget.<Money>create(widget, data) //
                                    .with(Values.Money) //
                                    .with((ds, period) -> {
                                        PerformanceIndex index = data.calculate(ds, period);
                                        int length = index.getTotals().length;
                                        return Money.of(index.getCurrency(),
                                                        index.getTotals()[length - 1] - index.getTotals()[0]);
                                    }) //
                                    .withBenchmarkDataSeries(false) //
                                    .build()),

    DELTA(Messages.LabelDelta, Messages.LabelStatementOfAssets, //
                    (widget, data) -> IndicatorWidget.<Money>create(widget, data) //
                                    .with(Values.Money) //
                                    .with((ds, period) -> {
                                        long[] d = data.calculate(ds, period).calculateDelta();
                                        return Money.of(data.getTermCurrency(), d.length > 0 ? d[d.length - 1] : 0L);
                                    }) //
                                    .withBenchmarkDataSeries(false) //
                                    .build()),

    ABSOLUTE_DELTA(Messages.LabelAbsoluteDelta, Messages.LabelStatementOfAssets, //
                    (widget, data) -> IndicatorWidget.<Money>create(widget, data) //
                                    .with(Values.Money) //
                                    .with((ds, period) -> {
                                        long[] d = data.calculate(ds, period).calculateAbsoluteDelta();
                                        return Money.of(data.getTermCurrency(), d.length > 0 ? d[d.length - 1] : 0L);
                                    }) //
                                    .withBenchmarkDataSeries(false) //
                                    .build()),

    SAVINGS(Messages.LabelPNTransfers, Messages.LabelStatementOfAssets, //
                    (widget, data) -> IndicatorWidget.<Money>create(widget, data) //
                                    .with(Values.Money) //
                                    .with((ds, period) -> {
                                        long[] d = data.calculate(ds, period).getTransferals();
                                        // skip d[0] because it refers to the
                                        // day before start
                                        return Money.of(data.getTermCurrency(),
                                                        d.length > 1 ? LongStream.of(d).skip(1).sum() : 0L);
                                    }) //
                                    .withBenchmarkDataSeries(false) //
                                    .build()),

    INVESTED_CAPITAL(Messages.LabelInvestedCapital, Messages.LabelStatementOfAssets, //
                    (widget, data) -> IndicatorWidget.<Money>create(widget, data) //
                                    .with(Values.Money) //
                                    .with((ds, period) -> {
                                        long[] d = data.calculate(ds, period).calculateInvestedCapital();
                                        return Money.of(data.getTermCurrency(), d.length > 0 ? d[d.length - 1] : 0L);
                                    }) //
                                    .withBenchmarkDataSeries(false) //
                                    .build()),

    ABSOLUTE_INVESTED_CAPITAL(Messages.LabelAbsoluteInvestedCapital, Messages.LabelStatementOfAssets, //
                    (widget, data) -> IndicatorWidget.<Money>create(widget, data) //
                                    .with(Values.Money) //
                                    .with((ds, period) -> {
                                        long[] d = data.calculate(ds, period).calculateAbsoluteInvestedCapital();
                                        return Money.of(data.getTermCurrency(), d.length > 0 ? d[d.length - 1] : 0L);
                                    }) //
                                    .withBenchmarkDataSeries(false) //
                                    .build()),

    RATIO(Messages.LabelRatio, Messages.LabelStatementOfAssets, RatioWidget::new),

    MAXDRAWDOWN(Messages.LabelMaxDrawdown, Messages.LabelRiskIndicators, //
                    (widget, data) -> IndicatorWidget.<Double>create(widget, data) //
                                    .with(Values.Percent2) //
                                    .with((ds, period) -> {
                                        PerformanceIndex index = data.calculate(ds, period);
                                        return index.getDrawdown().getMaxDrawdown();
                                    }) //
                                    .withTooltip((ds, period) -> {
                                        DateTimeFormatter formatter = DateTimeFormatter
                                                        .ofLocalizedDate(FormatStyle.LONG)
                                                        .withZone(ZoneId.systemDefault());
                                        PerformanceIndex index = data.calculate(ds, period);
                                        Drawdown drawdown = index.getDrawdown();
                                        return MessageFormat.format(Messages.TooltipMaxDrawdown,
                                                        formatter.format(
                                                                        drawdown.getIntervalOfMaxDrawdown().getStart()),
                                                        formatter.format(drawdown.getIntervalOfMaxDrawdown().getEnd()));
                                    }) //
                                    .withColoredValues(false) //
                                    .build()),

    MAXDRAWDOWNDURATION(Messages.LabelMaxDrawdownDuration, Messages.LabelRiskIndicators,
                    MaxDrawdownDurationWidget::new),

    DRAWDOWN_CHART(Messages.LabelMaxDrawdownChart, Messages.LabelRiskIndicators, Images.VIEW_LINECHART,
                    DrawdownChartWidget::new),

    VOLATILITY(Messages.LabelVolatility, Messages.LabelRiskIndicators, //
                    (widget, data) -> IndicatorWidget.<Double>create(widget, data) //
                                    .with(Values.Percent2) //
                                    .with((ds, period) -> {
                                        PerformanceIndex index = data.calculate(ds, period);
                                        return index.getVolatility().getStandardDeviation();
                                    }) //
                                    .withTooltip((ds, period) -> Messages.TooltipVolatility) //
                                    .withColoredValues(false) //
                                    .build()),

    SHARPE_RATIO(Messages.LabelSharpeRatio, Messages.LabelRiskIndicators, //
                    (widget, data) -> IndicatorWidget.<Double>create(widget, data) //
                                    .with(Values.PercentPlain) //
                                    .withColoredValues(false) //
                                    .withConfig(delegate -> new RiskFreeRateOfReturnConfig(delegate)) //
                                    .with((ds, period) -> {
                                        PerformanceIndex index = data.calculate(ds, period);
                                        double r = index.getPerformanceIRR();
                                        double rf = new ClientProperties(data.getClient()).getRiskFreeRateOfReturn();
                                        double volatility = index.getVolatility().getStandardDeviation();

                                        // handle invalid rf value
                                        if (Double.isNaN(rf))
                                            return Double.NaN;

                                        double excessReturn = r - rf;
                                        return excessReturn / volatility;
                                    }) //
                                    .withTooltip((ds, period) -> {
                                        PerformanceIndex index = data.calculate(ds, period);
                                        double r = index.getPerformanceIRR();
                                        double rf = new ClientProperties(data.getClient()).getRiskFreeRateOfReturn();
                                        double volatility = index.getVolatility().getStandardDeviation();
                                        double sharpeRatio = (r - rf) / volatility;
                                        return MessageFormat.format(Messages.TooltipSharpeRatio,
                                                        Values.Percent5.format(r), Values.Percent2.format(rf),
                                                        volatility, sharpeRatio);
                                    }) //
                                    .build()),

    SEMIVOLATILITY(Messages.LabelSemiVolatility, Messages.LabelRiskIndicators, //
                    (widget, data) -> IndicatorWidget.<Double>create(widget, data) //
                                    .with(Values.Percent2) //
                                    .with((ds, period) -> {
                                        PerformanceIndex index = data.calculate(ds, period);
                                        return index.getVolatility().getSemiDeviation();
                                    }) //
                                    .withTooltip((ds, period) -> {
                                        PerformanceIndex index = data.calculate(ds, period);
                                        Volatility vola = index.getVolatility();
                                        return MessageFormat.format(Messages.TooltipSemiVolatility,
                                                        Values.Percent5.format(vola.getExpectedSemiDeviation()),
                                                        vola.getNormalizedSemiDeviationComparison(),
                                                        Values.Percent5.format(vola.getStandardDeviation()),
                                                        Values.Percent5.format(vola.getSemiDeviation()));
                                    }) //
                                    .withColoredValues(false) //
                                    .build()),

    CALCULATION(Messages.LabelPerformanceCalculation, Messages.ClientEditorLabelPerformance,
                    PerformanceCalculationWidget::new),

    CHART(Messages.LabelPerformanceChart, Messages.ClientEditorLabelPerformance, Images.VIEW_LINECHART,
                    (widget, data) -> new ChartWidget(widget, data, DataSeries.UseCase.PERFORMANCE)),

    ASSET_CHART(Messages.LabelAssetChart, Messages.LabelStatementOfAssets, Images.VIEW_LINECHART,
                    (widget, data) -> new ChartWidget(widget, data, DataSeries.UseCase.STATEMENT_OF_ASSETS)),

    HOLDINGS_CHART(Messages.LabelStatementOfAssetsHoldings, Messages.LabelStatementOfAssets, Images.VIEW_PIECHART,
                    HoldingsChartWidget::new),

    CLIENT_DATA_SERIES_CHART(Messages.LabelStatementOfAssetsDerivedDataSeries, Messages.LabelStatementOfAssets,
                    Images.VIEW_LINECHART, ClientDataSeriesChartWidget::new),

    TAXONOMY_CHART(Messages.LabelTaxonomies, Messages.LabelStatementOfAssets, Images.VIEW_PIECHART,
                    TaxonomyChartWidget::new),

    HEATMAP(Messages.LabelHeatmap, Messages.ClientEditorLabelPerformance, PerformanceHeatmapWidget::new),

    HEATMAP_YEARLY(Messages.LabelYearlyHeatmap, Messages.ClientEditorLabelPerformance,
                    YearlyPerformanceHeatmapWidget::new),

    EARNINGS(Messages.LabelEarningsTransactionList, Messages.LabelEarnings, //
                    config -> config.put(Dashboard.Config.LAYOUT.name(), ExpansionSetting.EXPAND_CURRENT_MONTH.name()),
                    EarningsListWidget::new),

    HEATMAP_EARNINGS(Messages.LabelHeatmapEarnings, Messages.LabelEarnings, EarningsHeatmapWidget::new),

    EARNINGS_PER_YEAR_CHART(Messages.LabelEarningsPerYear, Messages.LabelEarnings, Images.VIEW_BARCHART,
                    EarningsChartWidget::perYear),

    EARNINGS_PER_QUARTER_CHART(Messages.LabelEarningsPerQuarter, Messages.LabelEarnings, Images.VIEW_BARCHART,
                    EarningsChartWidget::perQuarter),

    EARNINGS_PER_MONTH_CHART(Messages.LabelEarningsPerMonth, Messages.LabelEarnings, Images.VIEW_BARCHART,
                    EarningsChartWidget::perMonth),

    EARNINGS_BY_TAXONOMY(Messages.LabelEarningsByTaxonomy, Messages.LabelEarnings, Images.VIEW_PIECHART,
                    EarningsByTaxonomyChartWidget::new),

    TRADES_BASIC_STATISTICS(Messages.LabelTradesBasicStatistics, Messages.LabelTrades, TradesWidget::new),

    TRADES_PROFIT_LOSS(Messages.LabelTradesProfitLoss, Messages.LabelTrades, TradesProfitLossWidget::new),

    TRADES_AVERAGE_HOLDING_PERIOD(Messages.LabelAverageHoldingPeriod, Messages.LabelTrades,
                    TradesAverageHoldingPeriodWidget::new),

    TRADES_TURNOVER_RATIO(Messages.LabelTradesTurnoverRate, Messages.LabelTrades, //
                    (widget, data) -> IndicatorWidget.<Double>create(widget, data) //
                                    .with(Values.Percent2) //
                                    .with((ds, period) -> {
                                        PerformanceIndex index = data.calculate(ds, period);
                                        OptionalDouble average = LongStream.of(index.getTotals()).average();
                                        if (!average.isPresent() || average.getAsDouble() <= 0)
                                            return 0.0;
                                        long buy = LongStream.of(index.getBuys()).sum();
                                        long sell = LongStream.of(index.getSells()).sum();
                                        return Long.min(buy, sell) / average.getAsDouble();
                                    }) //
                                    .withBenchmarkDataSeries(false) //
                                    .withTooltip((ds, period) -> {
                                        PerformanceIndex index = data.calculate(ds, period);
                                        String currency = data.getCurrencyConverter().getTermCurrency();
                                        OptionalDouble average = LongStream.of(index.getTotals()).average();
                                        long buy = LongStream.of(index.getBuys()).sum();
                                        long sell = LongStream.of(index.getSells()).sum();
                                        return MessageFormat.format(Messages.TooltipTurnoverRate,
                                                        Values.Money.format(Money.of(currency, buy)),
                                                        Values.Money.format(Money.of(currency, sell)),
                                                        Values.Money.format(
                                                                        Money.of(currency, (long) average.orElse(0))),
                                                        Values.Percent2.format(
                                                                        average.isPresent() && average.getAsDouble() > 0
                                                                                        ? Long.min(buy, sell) / average
                                                                                                        .getAsDouble()
                                                                                        : 0));
                                    }) //
                                    .withColoredValues(false).build()),

    HEATMAP_INVESTMENTS(Messages.LabelHeatmapInvestments, Messages.LabelTrades, InvestmentHeatmapWidget::new),

    HEATMAP_TAXES(Messages.LabelHeatmapTaxes, Messages.LabelTrades,
                    (widget, data) -> new CostHeatmapWidget(widget, data, PaymentsViewModel.Mode.TAXES)),

    HEATMAP_FEES(Messages.LabelHeatmapFees, Messages.LabelTrades,
                    (widget, data) -> new CostHeatmapWidget(widget, data, PaymentsViewModel.Mode.FEES)),

    PORTFOLIO_TAX_RATE(Messages.LabelPortfolioTaxRate, Messages.ClientEditorLabelPerformance, //
                    (widget, data) -> new PortfolioTaxOrFeeRateWidget(widget, data, s -> {
                        double rate = s.getPortfolioTaxRate();
                        return Double.isNaN(rate) ? s.getValue(CategoryType.TAXES) : rate;
                    }, Messages.TooltipPortfolioTaxRate)),

    PORTFOLIO_FEE_RATE(Messages.LabelPortfolioFeeRate, Messages.ClientEditorLabelPerformance, //
                    (widget, data) -> new PortfolioTaxOrFeeRateWidget(widget, data, s -> {
                        double rate = s.getPortfolioFeeRate();
                        return Double.isNaN(rate) ? s.getValue(CategoryType.FEES) : rate;
                    }, Messages.TooltipPortfolioFeeRate)),

    CURRENT_DATE(Messages.LabelCurrentDate, Messages.LabelCommon, CurrentDateWidget::new),

    EXCHANGE_RATE(Messages.LabelExchangeRate, Messages.LabelCommon, ExchangeRateWidget::new),

    ACTIVITY_CHART(Messages.LabelTradingActivityChart, Messages.LabelCommon, Images.VIEW_BARCHART, ActivityWidget::new),

    LIMIT_EXCEEDED(Messages.SecurityListFilterLimitPriceExceeded, Messages.LabelCommon, LimitExceededWidget::new),

    FOLLOW_UP(Messages.SecurityListFilterDateReached, Messages.LabelCommon, FollowUpWidget::new),

    LATEST_SECURITY_PRICE(Messages.LabelSecurityLatestPrice, Messages.LabelCommon, //
                    (widget, data) -> IndicatorWidget.<Long>create(widget, data) //
                                    .with(Values.Quote) //
                                    .with((ds, period) -> {
                                        if (!(ds.getInstance() instanceof Security))
                                            return 0L;

                                        Security security = (Security) ds.getInstance();
                                        return security.getSecurityPrice(LocalDate.now()).getValue();
                                    }) //
                                    .withBenchmarkDataSeries(false) //
                                    .with(ds -> ds.getInstance() instanceof Security) //
                                    .withColoredValues(false) //
                                    .withTooltip((ds, period) -> {
                                        if (!(ds.getInstance() instanceof Security))
                                            return ""; //$NON-NLS-1$

                                        Security security = (Security) ds.getInstance();

                                        return MessageFormat.format(Messages.TooltipSecurityLatestPrice,
                                                        security.getName(), Values.Date.format(security
                                                                        .getSecurityPrice(LocalDate.now()).getDate()));
                                    }) //
                                    .build()),

    WEBSITE(Messages.Website, Messages.LabelCommon, BrowserWidget::new),

    DISTANCE_TO_ATH(Messages.SecurityListFilterDistanceFromAth, Messages.LabelCommon, //
                    (widget, data) -> IndicatorWidget.<Double>create(widget, data) //
                                    .with(Values.Percent2) //
                                    .with((ds, period) -> {
                                        if (!(ds.getInstance() instanceof Security))
                                            return (double) 0;

                                        Security security = (Security) ds.getInstance();

                                        Double distance = new AllTimeHigh(security, period).getDistance();
                                        if (distance == null)
                                            return (double) 0;

                                        return distance;
                                    }) //
                                    .withBenchmarkDataSeries(false) //
                                    .with(ds -> ds.getInstance() instanceof Security) //
                                    .withColoredValues(false) //
                                    .withTooltip((ds, period) -> {
                                        if (!(ds.getInstance() instanceof Security))
                                            return null;

                                        Security security = (Security) ds.getInstance();
                                        AllTimeHigh ath = new AllTimeHigh(security, period);
                                        if (ath.getValue() == null)
                                            return null;

                                        return MessageFormat.format(Messages.TooltipAllTimeHigh, period.getDays(),
                                                        Values.Date.format(ath.getDate()),
                                                        ath.getValue() / Values.Quote.divider(),
                                                        security.getSecurityPrice(LocalDate.now()).getValue()
                                                                        / Values.Quote.divider(),
                                                        Values.Date.format(security.getSecurityPrice(LocalDate.now())
                                                                        .getDate()));
                                    }) //
                                    .build()),

    // typo is API now!!
    VERTICAL_SPACEER(Messages.LabelVerticalSpacer, Messages.LabelCommon, VerticalSpacerWidget::new);

    private String label;
    private String group;
    private Images image;
    private Consumer<Map<String, String>> defaultConfigFunction;
    private BiFunction<Dashboard.Widget, DashboardData, WidgetDelegate<?>> createFunction;

    private WidgetFactory(String label, String group, Images image, Consumer<Map<String, String>> defaultConfigFunction,
                    BiFunction<Dashboard.Widget, DashboardData, WidgetDelegate<?>> createFunction)
    {
        this.label = label;
        this.group = group;
        this.image = image;
        this.defaultConfigFunction = defaultConfigFunction;
        this.createFunction = createFunction;
    }

    private WidgetFactory(String label, String group, Consumer<Map<String, String>> defaultConfigFunction,
                    BiFunction<Dashboard.Widget, DashboardData, WidgetDelegate<?>> createFunction)
    {
        this(label, group, null, defaultConfigFunction, createFunction);
    }

    private WidgetFactory(String label, String group, Images image,
                    BiFunction<Dashboard.Widget, DashboardData, WidgetDelegate<?>> createFunction)
    {
        this(label, group, image, null, createFunction);
    }

    private WidgetFactory(String label, String group,
                    BiFunction<Dashboard.Widget, DashboardData, WidgetDelegate<?>> createFunction)
    {
        this(label, group, null, null, createFunction);
    }

    public String getLabel()
    {
        return label;
    }

    public String getGroup()
    {
        return group;
    }

    public Images getImage()
    {
        return image;
    }

    public WidgetDelegate<?> constructDelegate(Dashboard.Widget widget, DashboardData data)
    {
        return this.createFunction.apply(widget, data);
    }

    public Widget constructWidget()
    {
        Dashboard.Widget widget = new Dashboard.Widget();
        widget.setLabel(label);
        widget.setType(name());

        if (defaultConfigFunction != null)
            defaultConfigFunction.accept(widget.getConfiguration());

        return widget;
    }
}
