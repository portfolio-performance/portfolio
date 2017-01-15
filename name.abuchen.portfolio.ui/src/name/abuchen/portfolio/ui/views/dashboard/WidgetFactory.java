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
import name.abuchen.portfolio.ui.views.dataseries.DataSeries;

public enum WidgetFactory
{
    HEADING(Messages.LabelHeading, HeadingWidget::new),

    TTWROR(Messages.LabelTTWROR,
                    (widget, data) -> IndicatorWidget.<Double>create(widget, data) //
                                    .with(Values.Percent2) //
                                    .with((ds, period) -> {
                                        PerformanceIndex index = data.calculate(ds, period);
                                        return index.getFinalAccumulatedPercentage();
                                    }).build()),

    IRR(Messages.LabelIRR,
                    (widget, data) -> IndicatorWidget.<Double>create(widget, data) //
                                    .with(Values.Percent2) //
                                    .with((ds, period) -> {
                                        ClientPerformanceSnapshot snapshot = data.calculate(ds, period)
                                                        .getClientPerformanceSnapshot();
                                        return snapshot.getPerformanceIRR();
                                    }) //
                                    .withBenchmarkDataSeries(false) //
                                    .build()),

    TOTAL_SUM(Messages.LabelTotalSum,
                    (widget, data) -> IndicatorWidget.<Long>create(widget, data) //
                                    .with(Values.Amount) //
                                    .with((ds, period) -> {
                                        PerformanceIndex index = data.calculate(ds, period);
                                        int length = index.getTotals().length;
                                        return index.getTotals()[length - 1];
                                    }) //
                                    .withBenchmarkDataSeries(false) //
                                    .build()),

    ABSOLUTE_CHANGE(Messages.LabelAbsoluteChange,
                    (widget, data) -> IndicatorWidget.<Long>create(widget, data) //
                                    .with(Values.Amount) //
                                    .with((ds, period) -> {
                                        PerformanceIndex index = data.calculate(ds, period);
                                        int length = index.getTotals().length;
                                        return index.getTotals()[length - 1] - index.getTotals()[0];
                                    }) //
                                    .withBenchmarkDataSeries(false) //
                                    .build()),

    DELTA(Messages.LabelAbsoluteDelta, //
                    (widget, data) -> IndicatorWidget.<Long>create(widget, data) //
                                    .with(Values.Amount) //
                                    .with((ds, period) -> {
                                        ClientPerformanceSnapshot snapshot = data.calculate(ds, period)
                                                        .getClientPerformanceSnapshot();
                                        return snapshot.getAbsoluteDelta().getAmount();
                                    }) //
                                    .withBenchmarkDataSeries(false) //
                                    .build()),

    MAXDRAWDOWN(Messages.LabelMaxDrawdown, //
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

    MAXDRAWDOWNDURATION(Messages.LabelMaxDrawdownDuration,
                    (widget, data) -> new MaxDrawdownDurationWidget(widget, data)),

    VOLATILITY(Messages.LabelVolatility,
                    (widget, data) -> IndicatorWidget.<Double>create(widget, data) //
                                    .with(Values.Percent2) //
                                    .with((ds, period) -> {
                                        PerformanceIndex index = data.calculate(ds, period);
                                        return index.getVolatility().getStandardDeviation();
                                    }) //
                                    .withTooltip((ds, period) -> Messages.TooltipVolatility) //
                                    .withColoredValues(false) //
                                    .build()),

    SEMIVOLATILITY(Messages.LabelSemiVolatility,
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

    CALCULATION(Messages.LabelPerformanceCalculation, PerformanceCalculationWidget::new),

    CHART(Messages.LabelPerformanceChart,
                    (widget, data) -> new ChartWidget(widget, data, DataSeries.UseCase.PERFORMANCE)),

    ASSET_CHART(Messages.LabelAssetChart,
                    (widget, data) -> new ChartWidget(widget, data, DataSeries.UseCase.STATEMENT_OF_ASSETS)),

    HEATMAP(Messages.LabelHeatmap, PerformanceHeatmapWidget::new),

    CURRENT_DATE(Messages.LabelCurrentDate, CurrentDateWidget::new);

    private String label;
    private BiFunction<Dashboard.Widget, DashboardData, WidgetDelegate> createFunction;

    private WidgetFactory(String label, BiFunction<Dashboard.Widget, DashboardData, WidgetDelegate> createFunction)
    {
        this.label = label;
        this.createFunction = createFunction;
    }

    public String getLabel()
    {
        return label;
    }

    public WidgetDelegate create(Dashboard.Widget widget, DashboardData data)
    {
        return this.createFunction.apply(widget, data);
    }
}
