package name.abuchen.portfolio.ui.views.dashboard;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.InfoToolTip;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries;
import name.abuchen.portfolio.util.Interval;

public class MultiIndicatorWidget<N extends Number> extends AbstractIndicatorWidget<N>
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

            MultiIndicatorWidget<N> multiIndicatorWidget = new MultiIndicatorWidget<>(widget, dashboardData, datasSeriesProviderSet);
            multiIndicatorWidget.setFormatter(formatter);
            multiIndicatorWidget.setProvider(provider);
            multiIndicatorWidget.setTooltip(tooltip);
            multiIndicatorWidget.setValueColored(isValueColored);
            multiIndicatorWidget.setTitle(title);
            return multiIndicatorWidget;
        }
    }

    private Values<N> formatter;
    private BiFunction<List<DataSeries>, Interval, N> provider;
    private BiFunction<List<DataSeries>, Interval, String> tooltip;
    private BiFunction<List<DataSeries>, Interval, String> titleProvider;
    private boolean isValueColored = true;

    public MultiIndicatorWidget(Widget widget, DashboardData dashboardData, List<Function<MultiIndicatorWidget<N>, DataSeriesConfig>> datasSeriesProviderSet)
    {
        super(widget, dashboardData);
        
        datasSeriesProviderSet.forEach(p -> addConfig(p.apply(this)));
        addConfig(new ReportingPeriodConfig(this));
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

    void setTitle(BiFunction<List<DataSeries>, Interval, String> title)
    {
        this.titleProvider = title;
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        Composite container = super.createControl(parent, resources);
        
        if (tooltip != null)
            InfoToolTip.attach(indicator, () -> tooltip.apply(
                            getAll(DataSeriesConfig.class).map(DataSeriesConfig::getDataSeries).collect(Collectors.toList()),
                            get(ReportingPeriodConfig.class).getReportingPeriod().toInterval(LocalDate.now())));

        return container;
    }

    @Override
    public Supplier<N> getUpdateTask()
    {
        return () -> provider.apply(
                        getAll(DataSeriesConfig.class).map(DataSeriesConfig::getDataSeries).collect(Collectors.toList()),
                        get(ReportingPeriodConfig.class).getReportingPeriod().toInterval(LocalDate.now()));
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
    
    @Override
    public void updateLabel()
    { 
        if (titleProvider != null)
            getWidget().setLabel(titleProvider.apply(
                            getAll(DataSeriesConfig.class).map(DataSeriesConfig::getDataSeries).collect(Collectors.toList()),
                            get(ReportingPeriodConfig.class).getReportingPeriod().toInterval(LocalDate.now())));  
        else
            super.updateLabel();
    }
}
