package name.abuchen.portfolio.ui.views.dashboard;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.util.InfoToolTip;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries;
import name.abuchen.portfolio.util.Interval;

public class MultiIndicatorWidget<N extends Number> extends WidgetDelegate<N>
{
    public static class Builder<N extends Number>
    {
        private Widget widget;
        private DashboardData dashboardData;
        private int numSeries;
        private Values<N> formatter;
        private BiFunction<List<DataSeries>, Interval, N> provider;
        private BiFunction<List<DataSeries>, Interval, String> tooltip;
        private boolean supportsBenchmarks = true;
        private boolean isValueColored = true;

        public Builder(Widget widget, DashboardData dashboardData)
        {
            this.widget = widget;
            this.dashboardData = dashboardData;
        }

        Builder<N> forSeries(int number)
        {
            this.numSeries = number;
            return this;
        }

        Builder<N> with(Values<N> formatter)
        {
            this.formatter = formatter;
            return this;
        }

        Builder<N> with(BiFunction<List<DataSeries>, Interval, N> provider)
        {
            this.provider = provider;
            return this;
        }

        Builder<N> withTooltip(BiFunction<List<DataSeries>, Interval, String> tooltip)
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

        MultiIndicatorWidget<N> build()
        {
            Objects.requireNonNull(formatter);
            Objects.requireNonNull(provider);

            MultiIndicatorWidget<N> multiIndicatorWidget = new MultiIndicatorWidget<>(widget, dashboardData, numSeries,
                            supportsBenchmarks);
            multiIndicatorWidget.setFormatter(formatter);
            multiIndicatorWidget.setProvider(provider);
            multiIndicatorWidget.setTooltip(tooltip);
            multiIndicatorWidget.setValueColored(isValueColored);
            return multiIndicatorWidget;
        }
    }

    protected Label title;
    protected Label indicator;

    private Values<N> formatter;
    private BiFunction<List<DataSeries>, Interval, N> provider;
    private BiFunction<List<DataSeries>, Interval, String> tooltip;
    private boolean isValueColored = true;

    public MultiIndicatorWidget(Widget widget, DashboardData dashboardData, int amount, boolean supportsBenchmarks)
    {
        super(widget, dashboardData);

        for (int aa = 0; aa < amount; aa++)
        {
            addConfig(new DataSeriesConfig(this, supportsBenchmarks));
        }

        addConfig(new ReportingPeriodConfig(this));
    }

    @Override
    public Control getTitleControl()
    {
        return title;
    }

    public static <N extends Number> Builder<N> create(Widget widget, DashboardData dashboardData)
    {
        return new MultiIndicatorWidget.Builder<>(widget, dashboardData);
    }

    void setFormatter(Values<N> formatter)
    {
        this.formatter = formatter;
    }

    void setProvider(BiFunction<List<DataSeries>, Interval, N> provider)
    {
        this.provider = provider;
    }

    void setTooltip(BiFunction<List<DataSeries>, Interval, String> tooltip)
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
        Composite container = new Composite(parent, SWT.NONE);
        container.setBackground(parent.getBackground());
        GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).applyTo(container);

        title = new Label(container, SWT.NONE);
        title.setText(getWidget().getLabel());
        title.setBackground(container.getBackground());
        GridDataFactory.fillDefaults().grab(true, false).applyTo(title);

        indicator = new Label(container, SWT.NONE);
        indicator.setFont(resources.getKpiFont());
        indicator.setBackground(container.getBackground());
        indicator.setText(""); //$NON-NLS-1$
        GridDataFactory.fillDefaults().grab(true, false).applyTo(indicator);

        if (tooltip != null)
            InfoToolTip.attach(indicator, () -> tooltip.apply(
                            getAll(DataSeriesConfig.class).map(DataSeriesConfig::getDataSeries)
                                            .collect(Collectors.toList()),
                            get(ReportingPeriodConfig.class).getReportingPeriod().toInterval(LocalDate.now())));

        return container;
    }

    @Override
    public Supplier<N> getUpdateTask()
    {
        return () -> provider.apply(
                        getAll(DataSeriesConfig.class).map(DataSeriesConfig::getDataSeries)
                                        .collect(Collectors.toList()),
                        get(ReportingPeriodConfig.class).getReportingPeriod().toInterval(LocalDate.now()));
    }

    @Override
    public void update(N value)
    {
        title.setText(getWidget().getLabel());
        indicator.setText(formatter.format(value));

        if (isValueColored)
            indicator.setForeground(Display.getDefault()
                            .getSystemColor(value.doubleValue() < 0 ? SWT.COLOR_DARK_RED : SWT.COLOR_DARK_GREEN));
    }
}
