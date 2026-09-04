package name.abuchen.portfolio.ui.views.dashboard;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceSnapshot;
import name.abuchen.portfolio.ui.views.dataseries.PerformanceMetric;
import name.abuchen.portfolio.util.TextUtil;

public class TopContributorsReturnWidget extends AbstractTopContributorsWidget<List<LazySecurityPerformanceRecord>>
{
    public TopContributorsReturnWidget(Widget widget, DashboardData dashboardData)
    {
        super(widget, dashboardData);

        addConfig(new ReportingPeriodConfig(this));
        addConfig(new DataSeriesConfig(this, false));
        addConfig(new PerformanceMetricConfig(this));
        addConfig(new CountConfig(this));
    }

    @Override
    public Supplier<List<LazySecurityPerformanceRecord>> getUpdateTask()
    {
        return () -> {
            var interval = get(ReportingPeriodConfig.class).getReportingPeriod().toInterval(LocalDate.now());

            var index = getDashboardData().calculate(get(DataSeriesConfig.class).getDataSeries(), interval);

            var snapshot = index.getClientPerformanceSnapshot().orElseThrow(IllegalArgumentException::new);
            var client = snapshot.getClient();
            var converter = getDashboardData().getCurrencyConverter();

            var secSnapshot = LazySecurityPerformanceSnapshot.create(client, converter, interval);
            return new ArrayList<>(secSnapshot.getRecords());
        };
    }

    @Override
    protected List<DisplayRow> buildDisplayRows(List<LazySecurityPerformanceRecord> records)
    {
        PerformanceMetric metric = get(PerformanceMetricConfig.class).getMetric();

        records.sort((a, b) -> Double.compare(getPerformance(b, metric), getPerformance(a, metric)));

        return records.stream().map(r -> {
            double performance = getPerformance(r, metric);
            return new DisplayRow(r.getSecurity(), TextUtil.escapeHtml(r.getSecurity().getName()),
                            Values.Percent2.format(performance), performance >= 0);
        }).toList();
    }

    private double getPerformance(LazySecurityPerformanceRecord record, PerformanceMetric metric)
    {
        return metric == PerformanceMetric.IRR ? record.getIrr() : record.getTrueTimeWeightedRateOfReturn();
    }
}
