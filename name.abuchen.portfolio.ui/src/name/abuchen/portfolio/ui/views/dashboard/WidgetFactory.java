package name.abuchen.portfolio.ui.views.dashboard;

import java.util.function.BiFunction;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.PerformanceIndex;

public enum WidgetFactory
{
    HEADING((widget, data) -> new HeadingWidget(widget, data)),

    TTWROR((widget, data) -> new IndicatorWidget<Double>(widget, data, Values.Percent2, (d, period) -> {
        PerformanceIndex index = d.calculate(PerformanceIndex.class, period);
        return index.getFinalAccumulatedPercentage();
    })),

    IRR((widget, data) -> new IndicatorWidget<Double>(widget, data, Values.Percent2, (d, period) -> {
        ClientPerformanceSnapshot snapshot = d.calculate(ClientPerformanceSnapshot.class, period);
        return snapshot.getPerformanceIRR();
    })),

    ABSOLUTE_CHANGE((widget, data) -> new IndicatorWidget<Long>(widget, data, Values.Amount, (d, period) -> {
        PerformanceIndex index = d.calculate(PerformanceIndex.class, period);
        int length = index.getTotals().length;
        return index.getTotals()[length - 1] - index.getTotals()[0];
    })),

    DELTA((widget, data) -> new IndicatorWidget<Long>(widget, data, Values.Amount, (d, period) -> {
        ClientPerformanceSnapshot snapshot = d.calculate(ClientPerformanceSnapshot.class, period);
        return snapshot.getAbsoluteDelta().getAmount();
    })),

    MAXDRAWDOWN((widget, data) -> new IndicatorWidget<Double>(widget, data, Values.Percent2, (d, period) -> {
        PerformanceIndex index = d.calculate(PerformanceIndex.class, period);
        return index.getDrawdown().getMaxDrawdown();
    })),

    MAXDRAWDOWNDURATION((widget, data) -> new MaxDrawdownDurationWidget(widget, data)),

    VOLATILITY((widget, data) -> new IndicatorWidget<Double>(widget, data, Values.Percent2, (d, period) -> {
        PerformanceIndex index = d.calculate(PerformanceIndex.class, period);
        return index.getVolatility().getStandardDeviation();
    })),

    SEMIVOLATILITY((widget, data) -> new IndicatorWidget<Double>(widget, data, Values.Percent2, (d, period) -> {
        PerformanceIndex index = d.calculate(PerformanceIndex.class, period);
        return index.getVolatility().getSemiDeviation();
    })),

    CALCULATION((widget, data) -> new PerformanceCalculationWidget(widget, data)),

    CHART((widget, data) -> new PerformanceChartWidget(widget, data));

    private BiFunction<Dashboard.Widget, DashboardData, WidgetDelegate> createFunction;

    private WidgetFactory(BiFunction<Dashboard.Widget, DashboardData, WidgetDelegate> createFunction)
    {
        this.createFunction = createFunction;
    }

    public WidgetDelegate create(Dashboard.Widget widget, DashboardData data)
    {
        return this.createFunction.apply(widget, data);
    }
}
