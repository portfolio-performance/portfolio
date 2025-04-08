package name.abuchen.portfolio.ui.views.dashboard;

import java.time.LocalDate;
import java.util.function.Supplier;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.util.Interval;

public class RatioWidget extends AbstractIndicatorWidget<Object>
{
    public static class BaseDataSeriesConfig extends DataSeriesConfig
    {
        public BaseDataSeriesConfig(WidgetDelegate<?> delegate)
        {
            super(delegate, false, false, null, Messages.LabelBaseDataSeries, Dashboard.Config.SECONDARY_DATA_SERIES);
        }

    }

    public RatioWidget(Widget widget, DashboardData dashboardData)
    {
        super(widget, dashboardData, false, null);

        addConfigAfter(DataSeriesConfig.class, new BaseDataSeriesConfig(this));
    }

    @Override
    public void onWidgetConfigEdited(Class<? extends WidgetConfig> type)
    {
        // construct label to indicate the data series (user can manually
        // change the label later)
        getWidget().setLabel(WidgetFactory.valueOf(getWidget().getType()).getLabel() + ", " //$NON-NLS-1$
                        + get(DataSeriesConfig.class).getDataSeries().getLabel() + " / " //$NON-NLS-1$
                        + get(BaseDataSeriesConfig.class).getDataSeries().getLabel());
    }

    @Override
    public Supplier<Object> getUpdateTask()
    {
        return () -> {

            Interval interval = get(ReportingPeriodConfig.class).getReportingPeriod().toInterval(LocalDate.now());

            // as we are only interested in the valuation at the end of the
            // reporting period, we shorten the calculation interval
            interval = Interval.of(interval.getEnd(), interval.getEnd());

            PerformanceIndex index = getDashboardData().calculate(get(DataSeriesConfig.class).getDataSeries(),
                            interval);

            PerformanceIndex base = getDashboardData().calculate(get(BaseDataSeriesConfig.class).getDataSeries(),
                            interval);

            return (double) index.getTotals()[index.getTotals().length - 1]
                            / base.getTotals()[base.getTotals().length - 1];
        };
    }

    @Override
    public void update(Object rateOrValue)
    {
        super.update(rateOrValue);

        Double rate = (Double) rateOrValue;
        this.indicator.setText(!rate.isInfinite() ? Values.Percent2.format(rate) : "-"); //$NON-NLS-1$
    }
}
