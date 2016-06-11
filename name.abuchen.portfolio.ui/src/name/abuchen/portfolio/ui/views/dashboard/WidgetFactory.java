package name.abuchen.portfolio.ui.views.dashboard;

import java.util.function.BiFunction;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.PerformanceIndex;

public enum WidgetFactory
{
    HEADING((widget, data) -> new HeadingWidget(widget, data)),

    TTWROR((widget, data) -> new IndicatorWidget<Double>(widget, data, true, Values.Percent2, (ds, period) -> {
        PerformanceIndex index = data.calculate(ds, period);
        return index.getFinalAccumulatedPercentage();
    })),

    IRR((widget, data) -> new IndicatorWidget<Double>(widget, data, false, Values.Percent2, (ds, period) -> {
        ClientPerformanceSnapshot snapshot = data.calculate(ds, period).getClientPerformanceSnapshot();
        return snapshot.getPerformanceIRR();
    })),

    ABSOLUTE_CHANGE((widget, data) -> new IndicatorWidget<Long>(widget, data, false, Values.Amount, (ds, period) -> {
        PerformanceIndex index = data.calculate(ds, period);
        int length = index.getTotals().length;
        return index.getTotals()[length - 1] - index.getTotals()[0];
    })),

    DELTA((widget, data) -> new IndicatorWidget<Long>(widget, data, false, Values.Amount, (ds, period) -> {
        ClientPerformanceSnapshot snapshot = data.calculate(ds, period).getClientPerformanceSnapshot();
        return snapshot.getAbsoluteDelta().getAmount();
    })),

    MAXDRAWDOWN((widget, data) -> new IndicatorWidget<Double>(widget, data, true, Values.Percent2, (ds, period) -> {
        PerformanceIndex index = data.calculate(ds, period);
        return index.getDrawdown().getMaxDrawdown();
    })),

    MAXDRAWDOWNDURATION((widget, data) -> new MaxDrawdownDurationWidget(widget, data)),

    VOLATILITY((widget, data) -> new IndicatorWidget<Double>(widget, data, true, Values.Percent2, (ds, period) -> {
        PerformanceIndex index = data.calculate(ds, period);
        return index.getVolatility().getStandardDeviation();
    })),

    SEMIVOLATILITY((widget, data) -> new IndicatorWidget<Double>(widget, data, true, Values.Percent2, (ds, period) -> {
        PerformanceIndex index = data.calculate(ds, period);
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
