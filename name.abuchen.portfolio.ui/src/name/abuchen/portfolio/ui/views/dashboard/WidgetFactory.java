package name.abuchen.portfolio.ui.views.dashboard;

import java.util.function.Function;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.PerformanceIndex;

public enum WidgetFactory
{
    HEADING(widget -> new HeadingWidget(widget)),

    TTWROR(widget -> new IndicatorWidget<Double>(widget, Values.Percent2, (data, period) -> {
        PerformanceIndex index = data.calculate(PerformanceIndex.class, period);
        return index.getFinalAccumulatedPercentage();
    })),

    IRR(widget -> new IndicatorWidget<Double>(widget, Values.Percent2, (data, period) -> {
        ClientPerformanceSnapshot snapshot = data.calculate(ClientPerformanceSnapshot.class, period);
        return snapshot.getPerformanceIRR();
    })),

    ABSOLUTE_CHANGE(widget -> new IndicatorWidget<Long>(widget, Values.Amount, (data, period) -> {
        PerformanceIndex index = data.calculate(PerformanceIndex.class, period);
        int length = index.getTotals().length;
        return index.getTotals()[length - 1] - index.getTotals()[0];
    })),

    DELTA(widget -> new IndicatorWidget<Long>(widget, Values.Amount, (data, period) -> {
        ClientPerformanceSnapshot snapshot = data.calculate(ClientPerformanceSnapshot.class, period);
        return snapshot.getAbsoluteDelta().getAmount();
    })),

    MAXDRAWDOWN(widget -> new IndicatorWidget<Double>(widget, Values.Percent2, (data, period) -> {
        PerformanceIndex index = data.calculate(PerformanceIndex.class, period);
        return index.getDrawdown().getMaxDrawdown();
    })),

    MAXDRAWDOWNDURATION(widget -> new MaxDrawdownDurationWidget(widget)),

    VOLATILITY(widget -> new IndicatorWidget<Double>(widget, Values.Percent2, (data, period) -> {
        PerformanceIndex index = data.calculate(PerformanceIndex.class, period);
        return index.getVolatility().getStandardDeviation();
    })),

    SEMIVOLATILITY(widget -> new IndicatorWidget<Double>(widget, Values.Percent2, (data, period) -> {
        PerformanceIndex index = data.calculate(PerformanceIndex.class, period);
        return index.getVolatility().getSemiDeviation();
    })),

    CALCULATION(widget -> new PerformanceCalculationWidget(widget));

    private Function<Dashboard.Widget, WidgetDelegate> createFunction;

    private WidgetFactory(Function<Widget, WidgetDelegate> createFunction)
    {
        this.createFunction = createFunction;
    }

    public WidgetDelegate create(Widget widget)
    {
        return this.createFunction.apply(widget);
    }
}
