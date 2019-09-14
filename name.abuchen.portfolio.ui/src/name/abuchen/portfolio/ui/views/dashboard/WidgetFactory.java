package name.abuchen.portfolio.ui.views.dashboard;

import java.text.MessageFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.function.BiFunction;

import name.abuchen.portfolio.math.Risk.Drawdown;
import name.abuchen.portfolio.math.Risk.Volatility;
import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.views.dashboard.heatmap.EarningsHeatmapWidget;
import name.abuchen.portfolio.ui.views.dashboard.heatmap.PerformanceHeatmapWidget;
import name.abuchen.portfolio.ui.views.dashboard.heatmap.YearlyPerformanceHeatmapWidget;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries;

public enum WidgetFactory
{
    HEADING(Messages.LabelHeading, Messages.LabelCommon, HeadingWidget::new),

    TOTAL_SUM(Messages.LabelTotalSum, Messages.LabelStatementOfAssets, //
                    (widget, data) -> IndicatorWidget.<Long>create(widget, data) //
                                    .with(Values.Amount) //
                                    .with((ds, period) -> {
                                        PerformanceIndex index = data.calculate(ds, period);
                                        int length = index.getTotals().length;
                                        return index.getTotals()[length - 1];
                                    }) //
                                    .withBenchmarkDataSeries(false) //
                                    .build()),

    TTWROR(Messages.LabelTTWROR, Messages.ClientEditorLabelPerformance, //
                    (widget, data) -> IndicatorWidget.<Double>create(widget, data) //
                                    .with(Values.Percent2) //
                                    .with((ds, period) -> {
                                        PerformanceIndex index = data.calculate(ds, period);
                                        return index.getFinalAccumulatedPercentage();
                                    }).build()),

    IRR(Messages.LabelIRR, Messages.ClientEditorLabelPerformance, //
                    (widget, data) -> IndicatorWidget.<Double>create(widget, data) //
                                    .with(Values.Percent2) //
                                    .with((ds, period) -> data.calculate(ds, period).getPerformanceIRR()) //
                                    .withBenchmarkDataSeries(false) //
                                    .build()),

    ABSOLUTE_CHANGE(Messages.LabelAbsoluteChange, Messages.LabelStatementOfAssets, //
                    (widget, data) -> IndicatorWidget.<Long>create(widget, data) //
                                    .with(Values.Amount) //
                                    .with((ds, period) -> {
                                        PerformanceIndex index = data.calculate(ds, period);
                                        int length = index.getTotals().length;
                                        return index.getTotals()[length - 1] - index.getTotals()[0];
                                    }) //
                                    .withBenchmarkDataSeries(false) //
                                    .build()),

    DELTA(Messages.LabelAbsoluteDelta, Messages.LabelStatementOfAssets, //
                    (widget, data) -> IndicatorWidget.<Long>create(widget, data) //
                                    .with(Values.Amount) //
                                    .with((ds, period) -> {
                                        ClientPerformanceSnapshot snapshot = data.calculate(ds, period)
                                                        .getClientPerformanceSnapshot()
                                                        .orElseThrow(IllegalArgumentException::new);
                                        return snapshot.getAbsoluteDelta().getAmount();
                                    }) //
                                    .withBenchmarkDataSeries(false) //
                                    .build()),

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

    CHART(Messages.LabelPerformanceChart, Messages.ClientEditorLabelPerformance,
                    (widget, data) -> new ChartWidget(widget, data, DataSeries.UseCase.PERFORMANCE)),

    ASSET_CHART(Messages.LabelAssetChart, Messages.LabelStatementOfAssets,
                    (widget, data) -> new ChartWidget(widget, data, DataSeries.UseCase.STATEMENT_OF_ASSETS)),

    HEATMAP(Messages.LabelHeatmap, Messages.ClientEditorLabelPerformance, PerformanceHeatmapWidget::new),

    HEATMAP_YEARLY(Messages.LabelYearlyHeatmap, Messages.ClientEditorLabelPerformance,
                    YearlyPerformanceHeatmapWidget::new),

    HEATMAP_EARNINGS(Messages.LabelHeatmapEarnings, Messages.LabelEarnings, EarningsHeatmapWidget::new),

    TRADES_BASIC_STATISTICS(Messages.LabelTradesBasicStatistics, Messages.LabelTrades, TradesWidget::new),

    TRADES_PROFIT_LOSS(Messages.LabelTradesProfitLoss, Messages.LabelTrades, TradesProfitLossWidget::new),

    CURRENT_DATE(Messages.LabelCurrentDate, Messages.LabelCommon, CurrentDateWidget::new),

    EXCHANGE_RATE(Messages.LabelExchangeRate, Messages.LabelCommon, ExchangeRateWidget::new),

    ACTIVITY_CHART(Messages.LabelTradingActivityChart, Messages.LabelCommon, ActivityWidget::new),

    // typo is API now!!
    VERTICAL_SPACEER(Messages.LabelVerticalSpacer, Messages.LabelCommon, VerticalSpacerWidget::new);

    private String label;
    private String group;
    private BiFunction<Dashboard.Widget, DashboardData, WidgetDelegate<?>> createFunction;

    private WidgetFactory(String label, String group,
                    BiFunction<Dashboard.Widget, DashboardData, WidgetDelegate<?>> createFunction)
    {
        this.label = label;
        this.group = group;
        this.createFunction = createFunction;
    }

    public String getLabel()
    {
        return label;
    }

    public String getGroup()
    {
        return group;
    }

    public WidgetDelegate<?> create(Dashboard.Widget widget, DashboardData data)
    {
        return this.createFunction.apply(widget, data);
    }
}
