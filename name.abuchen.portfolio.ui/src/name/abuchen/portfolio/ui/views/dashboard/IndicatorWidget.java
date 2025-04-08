package name.abuchen.portfolio.ui.views.dashboard;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.eclipse.swt.widgets.Composite;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.InfoToolTip;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries;
import name.abuchen.portfolio.util.Interval;

public class IndicatorWidget<N> extends AbstractIndicatorWidget<N>
{
    public static class Builder<N>
    {
        private Widget widget;
        private DashboardData dashboardData;
        private Values<N> formatter;
        private BiFunction<DataSeries, Interval, N> provider;
        private BiFunction<DataSeries, Interval, String> tooltip;
        private boolean supportsBenchmarks = true;
        private Predicate<DataSeries> predicate;
        private boolean isValueColored = true;
        private List<Function<WidgetDelegate<?>, WidgetConfig>> additionalConfig = new ArrayList<>();

        public Builder(Widget widget, DashboardData dashboardData)
        {
            this.widget = widget;
            this.dashboardData = dashboardData;
        }

        Builder<N> with(Values<N> formatter)
        {
            this.formatter = formatter;
            return this;
        }

        Builder<N> with(Predicate<DataSeries> predicate)
        {
            this.predicate = predicate;
            return this;
        }

        Builder<N> with(BiFunction<DataSeries, Interval, N> provider)
        {
            this.provider = provider;
            return this;
        }

        Builder<N> withTooltip(BiFunction<DataSeries, Interval, String> tooltip)
        {
            this.tooltip = tooltip;
            return this;
        }

        Builder<N> withBenchmarkDataSeries(boolean supportsBenchmarks)
        {
            this.supportsBenchmarks = supportsBenchmarks;
            return this;
        }

        Builder<N> withColoredValues(boolean isValueColored)
        {
            this.isValueColored = isValueColored;
            return this;
        }

        Builder<N> withConfig(Function<WidgetDelegate<?>, WidgetConfig> config)
        {
            this.additionalConfig.add(config);
            return this;
        }

        IndicatorWidget<N> build()
        {
            Objects.requireNonNull(formatter);
            Objects.requireNonNull(provider);

            IndicatorWidget<N> indicatorWidget = new IndicatorWidget<>(widget, dashboardData, supportsBenchmarks,
                            predicate);
            indicatorWidget.setFormatter(formatter);
            indicatorWidget.setProvider(provider);
            indicatorWidget.setTooltip(tooltip);
            indicatorWidget.setValueColored(isValueColored);

            additionalConfig.forEach(config -> indicatorWidget.addConfig(config.apply(indicatorWidget)));

            return indicatorWidget;
        }
    }

    private Values<N> formatter;
    private BiFunction<DataSeries, Interval, N> provider;
    private BiFunction<DataSeries, Interval, String> tooltip;
    private boolean isValueColored = true;

    public IndicatorWidget(Widget widget, DashboardData dashboardData, boolean supportsBenchmarks,
                    Predicate<DataSeries> predicate)
    {
        super(widget, dashboardData, supportsBenchmarks, predicate);
    }

    public static <N> Builder<N> create(Widget widget, DashboardData dashboardData)
    {
        return new IndicatorWidget.Builder<>(widget, dashboardData);
    }

    void setFormatter(Values<N> formatter)
    {
        this.formatter = formatter;
    }

    void setProvider(BiFunction<DataSeries, Interval, N> provider)
    {
        this.provider = provider;
    }

    void setTooltip(BiFunction<DataSeries, Interval, String> tooltip)
    {
        this.tooltip = tooltip;
    }

    void setValueColored(boolean isValueColored)
    {
        this.isValueColored = isValueColored;
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        Composite container = super.createControl(parent, resources);

        if (tooltip != null)
            InfoToolTip.attach(indicator, () -> tooltip.apply(get(DataSeriesConfig.class).getDataSeries(),
                            get(ReportingPeriodConfig.class).getReportingPeriod().toInterval(LocalDate.now())));

        return container;
    }

    @Override
    public Supplier<N> getUpdateTask()
    {
        return () -> provider.apply(get(DataSeriesConfig.class).getDataSeries(),
                        get(ReportingPeriodConfig.class).getReportingPeriod().toInterval(LocalDate.now()));
    }

    @Override
    public void update(N value)
    {
        super.update(value);

        boolean isNegative = false;

        if (value instanceof Money money)
        {
            indicator.setText(Values.Money.format(money, getDashboardData().getClient().getBaseCurrency()));
            isNegative = money.isNegative();
        }
        else if (value instanceof Number number)
        {
            indicator.setText(formatter.format(value));
            isNegative = number.doubleValue() < 0;
        }
        else
        {
            indicator.setText(formatter.format(value));
        }

        if (isValueColored)
        {
            indicator.setTextColor(isNegative ? Colors.theme().redForeground() : Colors.theme().greenForeground());
        }

    }
}
