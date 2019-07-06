package name.abuchen.portfolio.ui.views.dashboard;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
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
import name.abuchen.portfolio.util.TextUtil;

public class MultiIndicatorWidget<N extends Number> extends WidgetDelegate<N>
{
    public static class Builder<N extends Number>
    {
        private Widget widget;
        private DashboardData dashboardData;
        private Values<N> formatter;
        private List<Function<MultiIndicatorWidget<N>, DataSeriesConfig>> datasSeriesProviderSet = new ArrayList<>();
        private BiFunction<List<DataSeries>, Interval, N> provider;
        private BiFunction<List<DataSeries>, Interval, String> tooltip;
        private BiFunction<List<DataSeries>, Interval, String> title;
        private boolean isValueColored = true;

        public Builder(Widget widget, DashboardData dashboardData)
        {
            this.widget = widget;
            this.dashboardData = dashboardData;
        }

        Builder<N> withDataSeries(Function<MultiIndicatorWidget<N>, DataSeriesConfig> datasSeriesProvider)
        {
            this.datasSeriesProviderSet.add(datasSeriesProvider);
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

        Builder<N> withColoredValues(boolean isValueColored)
        {
            this.isValueColored = isValueColored;
            return this;
        }

        Builder<N> withTitle(BiFunction<List<DataSeries>, Interval, String> title)
        {
            this.title = title;
            return this;
        }

        MultiIndicatorWidget<N> build()
        {
            Objects.requireNonNull(formatter);
            Objects.requireNonNull(provider);
            Objects.requireNonNull(datasSeriesProviderSet);

            MultiIndicatorWidget<N> multiIndicatorWidget = new MultiIndicatorWidget<>(widget, dashboardData, title,
                            datasSeriesProviderSet);
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
    private MultiDataSeriesConfigConfig config;
    private BiFunction<List<DataSeries>, Interval, N> provider;
    private BiFunction<List<DataSeries>, Interval, String> tooltip;
    private BiFunction<List<DataSeries>, Interval, String> titleProvider;
    private boolean isValueColored = true;

    public MultiIndicatorWidget(Widget widget, DashboardData dashboardData,
                    BiFunction<List<DataSeries>, Interval, String> title,
                    List<Function<MultiIndicatorWidget<N>, DataSeriesConfig>> datasSeriesProviderSet)
    {
        super(widget, dashboardData);
        this.titleProvider = title;

        config = new MultiDataSeriesConfigConfig(this);
        addConfig(config);
        addConfig(new ReportingPeriodConfig(this));
        config.load(datasSeriesProviderSet.stream().map(p -> p.apply(this)).collect(Collectors.toList()));
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
    public Control getTitleControl()
    {
        return title;
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        Composite container = new Composite(parent, SWT.NONE);
        container.setBackground(parent.getBackground());
        GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).applyTo(container);

        title = new Label(container, SWT.NONE);
        title.setText(TextUtil.tooltip(getWidget().getLabel()));
        title.setBackground(container.getBackground());
        GridDataFactory.fillDefaults().grab(true, false).applyTo(title);

        indicator = new Label(container, SWT.NONE);
        indicator.setFont(resources.getKpiFont());
        indicator.setBackground(container.getBackground());
        indicator.setText(""); //$NON-NLS-1$
        GridDataFactory.fillDefaults().grab(true, false).applyTo(indicator);

        if (tooltip != null)
            InfoToolTip.attach(indicator, () -> tooltip.apply(config.getDataSeries(),
                            get(ReportingPeriodConfig.class).getReportingPeriod().toInterval(LocalDate.now())));

        return container;
    }

    @Override
    public Supplier<N> getUpdateTask()
    {
        return () -> provider.apply(config.getDataSeries(),
                        get(ReportingPeriodConfig.class).getReportingPeriod().toInterval(LocalDate.now()));
    }

    @Override
    public void update(N value)
    {
        title.setText(TextUtil.tooltip(getWidget().getLabel()));
        indicator.setText(formatter.format(value));

        if (isValueColored)
            indicator.setForeground(Display.getDefault()
                            .getSystemColor(value.doubleValue() < 0 ? SWT.COLOR_DARK_RED : SWT.COLOR_DARK_GREEN));
    }

    @Override
    public void updateLabel()
    {
        if (titleProvider != null)
            getWidget().setLabel(titleProvider.apply(config.getDataSeries(),
                            get(ReportingPeriodConfig.class).getReportingPeriod().toInterval(LocalDate.now())));
        else
            getWidget().setLabel(WidgetFactory.valueOf(getWidget().getType()).getLabel());
    }
}
