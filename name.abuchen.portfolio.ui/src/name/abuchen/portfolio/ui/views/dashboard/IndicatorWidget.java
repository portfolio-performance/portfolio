package name.abuchen.portfolio.ui.views.dashboard;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.util.InfoToolTip;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries;

public class IndicatorWidget<N extends Number> extends AbstractIndicatorWidget<N>
{
    public static class Builder<N extends Number>
    {
        private Widget widget;
        private DashboardData dashboardData;
        private Values<N> formatter;
        private BiFunction<DataSeries, ReportingPeriod, N> provider;
        private BiFunction<DataSeries, ReportingPeriod, String> tooltip;
        private boolean supportsBenchmarks = true;
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

        Builder<N> with(BiFunction<DataSeries, ReportingPeriod, N> provider)
        {
            this.provider = provider;
            return this;
        }

        Builder<N> withTooltip(BiFunction<DataSeries, ReportingPeriod, String> tooltip)
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

            IndicatorWidget<N> indicatorWidget = new IndicatorWidget<>(widget, dashboardData, supportsBenchmarks);
            indicatorWidget.setFormatter(formatter);
            indicatorWidget.setProvider(provider);
            indicatorWidget.setTooltip(tooltip);
            indicatorWidget.setValueColored(isValueColored);
            return indicatorWidget;
        }
    }

    private Values<N> formatter;
    private BiFunction<DataSeries, ReportingPeriod, N> provider;
    private BiFunction<DataSeries, ReportingPeriod, String> tooltip;
    private boolean isValueColored = true;

    public IndicatorWidget(Widget widget, DashboardData dashboardData, boolean supportsBenchmarks)
    {
        super(widget, dashboardData, supportsBenchmarks);
    }

    public static <N extends Number> Builder<N> create(Widget widget, DashboardData dashboardData)
    {
        return new IndicatorWidget.Builder<>(widget, dashboardData);
    }

    void setFormatter(Values<N> formatter)
    {
        this.formatter = formatter;
    }

    void setProvider(BiFunction<DataSeries, ReportingPeriod, N> provider)
    {
        this.provider = provider;
    }

    void setTooltip(BiFunction<DataSeries, ReportingPeriod, String> tooltip)
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
                            get(ReportingPeriodConfig.class).getReportingPeriod()));

        return container;
    }

    @Override
    public Supplier<N> getUpdateTask()
    {
        return () -> provider.apply(get(DataSeriesConfig.class).getDataSeries(),
                        get(ReportingPeriodConfig.class).getReportingPeriod());
    }

    @Override
    public void update(N value)
    {
        super.update(value);

        indicator.setText(formatter.format(value));

        if (isValueColored)
            indicator.setForeground(Display.getDefault()
                            .getSystemColor(value.doubleValue() < 0 ? SWT.COLOR_DARK_RED : SWT.COLOR_DARK_GREEN));
    }
}
