package name.abuchen.portfolio.ui.views.dashboard;

import java.time.LocalDate;
import java.util.function.Supplier;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries;
import name.abuchen.portfolio.util.Interval;

public class IRRWidget extends IndicatorWidget<Double>
{
    public IRRWidget(Widget widget, DashboardData dashboardData)
    {
        super(widget, dashboardData, false, null);
        this.setFormatter(Values.Percent2);

        addConfig(new IRRDataSeriesConfig(this));
    }

    @Override
    public Supplier<Double> getUpdateTask()
    {
        return () -> {
            double irrBench = 0;
            Interval reportingPeriod = get(ReportingPeriodConfig.class).getReportingPeriod()
                            .toInterval(LocalDate.now());
            DataSeries dsBench = get(IRRDataSeriesConfig.class).getDataSeries();
            if (dsBench != null)
            {
                irrBench = getDashboardData().calculate(dsBench, reportingPeriod).getPerformanceIRR();
            }

            double irr = getDashboardData().calculate(get(DataSeriesConfig.class).getDataSeries(), reportingPeriod)
                            .getPerformanceIRR();
            return (irrBench != -1) ? (1 + irr) / (1 + irrBench) - 1 : irr - irrBench;
        };
    }
}
