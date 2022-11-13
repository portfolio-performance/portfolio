package name.abuchen.portfolio.ui.views.dashboard;

import java.time.LocalDate;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.eclipse.swt.widgets.Composite;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.InfoToolTip;
import name.abuchen.portfolio.ui.views.dashboard.DataSeriesConfig.DataSeriesConfigElement;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries;
import name.abuchen.portfolio.util.Interval;

public class IndicatorWidget<N extends Number> extends AbstractIndicatorWidget<N>
{
    public static class Builder<N extends Number>
    {
        private Widget widget;
        private DashboardData dashboardData;
        private Values<N> formatter;
        private BiFunction<DataSeries, Interval, N> provider;
        private BiFunction<DataSeries, Interval, String> tooltip;
        private boolean supportsBenchmarks = true;
        private Predicate<DataSeries> predicate;
        private boolean isValueColored = true;

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

        IndicatorWidget<N> build()
        {
            Objects.requireNonNull(formatter);
            Objects.requireNonNull(provider);

            IndicatorWidget<N> indicatorWidget = new IndicatorWidget<>(widget, dashboardData, supportsBenchmarks,
                            predicate);
            indicatorWidget.setFormatter(formatter);
            BiFunction<DataSeries[], Interval, N> newProvider = (ds, interval) -> provider.apply(ds[0], interval); 
            indicatorWidget.setProvider(newProvider);
            BiFunction<DataSeries[], Interval, String> newTooltip = (ds, intveral) -> tooltip.apply(ds[0], intveral);
            indicatorWidget.setTooltip(newTooltip);
            indicatorWidget.setValueColored(isValueColored);
            return indicatorWidget;
        }
    }
    public static class BuilderForMultipleDataseries<N extends Number>
    {
        private Widget widget;
        private DashboardData dashboardData;
        
        private DataSeriesConfigElement[] dataSeriesConfigElements;
        private int counter;
        
        private Values<N> formatter;
        private BiFunction<DataSeries[], Interval, N> provider;
        private BiFunction<DataSeries[], Interval, String> tooltip;
        private boolean isValueColored = true;

        public BuilderForMultipleDataseries(Widget widget, DashboardData dashboardData, int numberOfDataSeries)
        {
            this.widget = widget;
            this.dashboardData = dashboardData;
            dataSeriesConfigElements = new DataSeriesConfigElement[numberOfDataSeries];
            counter = 0;
        }
        
        BuilderForMultipleDataseries<N> addDataSeries(String label, boolean isBenchmark,
                        boolean isEmptyAllowed, Predicate<DataSeries> predicate)
        {
            dataSeriesConfigElements[counter] = new DataSeriesConfigElement(label, isBenchmark, predicate, isEmptyAllowed);
            counter++;
            return this;
        }

        BuilderForMultipleDataseries<N> with(Values<N> formatter)
        {
            this.formatter = formatter;
            return this;
        }

        BuilderForMultipleDataseries<N> with(BiFunction<DataSeries[], Interval, N> provider)
        {
            this.provider = provider;
            return this;
        }

        BuilderForMultipleDataseries<N> withTooltip(BiFunction<DataSeries[], Interval, String> tooltip)
        {
            this.tooltip = tooltip;
            return this;
        }

        BuilderForMultipleDataseries<N> withColoredValues(boolean isValueColored)
        {
            this.isValueColored = isValueColored;
            return this;
        }

        IndicatorWidget<N> build()
        {
            Objects.requireNonNull(formatter);
            Objects.requireNonNull(provider);
            if(counter != dataSeriesConfigElements.length)
                throw new IllegalArgumentException("Not enouh data series configurations where provided"); //$NON-NLS-1$

            IndicatorWidget<N> indicatorWidget = new IndicatorWidget<>(widget, dashboardData, dataSeriesConfigElements);
            indicatorWidget.setFormatter(formatter);
            indicatorWidget.setProvider(provider);
            indicatorWidget.setTooltip(tooltip);
            indicatorWidget.setValueColored(isValueColored);
            return indicatorWidget;
        }
    }

    private Values<N> formatter;
    private BiFunction<DataSeries[], Interval, N> provider;
    private BiFunction<DataSeries[], Interval, String> tooltip;
    private boolean isValueColored = true;

    public IndicatorWidget(Widget widget, DashboardData dashboardData, boolean supportsBenchmarks,
                    Predicate<DataSeries> predicate)
    {
        super(widget, dashboardData, supportsBenchmarks, predicate);
    }
    
    public IndicatorWidget(Widget widget, DashboardData dashboardData, DataSeriesConfigElement[] dataSeriesConfigElements)
    {
        super(widget, dashboardData, dataSeriesConfigElements);
    }

    public static <N extends Number> Builder<N> create(Widget widget, DashboardData dashboardData)
    {
        return new IndicatorWidget.Builder<>(widget, dashboardData);
    }

    public static <N extends Number> BuilderForMultipleDataseries<N> createForMultipleDataseries(Widget widget,
                    DashboardData dashboardData, int numberOfDataSeries)
    {
        return new IndicatorWidget.BuilderForMultipleDataseries<>(widget, dashboardData, numberOfDataSeries);
    }

    void setFormatter(Values<N> formatter)
    {
        this.formatter = formatter;
    }

    void setProvider(BiFunction<DataSeries[], Interval, N> provider)
    {
        this.provider = provider;
    }

    void setTooltip(BiFunction<DataSeries[], Interval, String> tooltip)
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
        {
            InfoToolTip.attach(indicator, () -> tooltip.apply(get(DataSeriesConfig.class).getAllDataSeries(),
                            get(ReportingPeriodConfig.class).getReportingPeriod().toInterval(LocalDate.now())));
        }

        return container;
    }

    @Override
    public Supplier<N> getUpdateTask()
    {
        return () -> provider.apply(get(DataSeriesConfig.class).getAllDataSeries(),
                        get(ReportingPeriodConfig.class).getReportingPeriod().toInterval(LocalDate.now()));
    }

    @Override
    public void update(N value)
    {
        super.update(value);

        indicator.setText(formatter.format(value));

        if (isValueColored)
            indicator.setTextColor(value.doubleValue() < 0 ? Colors.theme().redForeground()
                            : Colors.theme().greenForeground());
    }
}
