package name.abuchen.portfolio.ui.views.dashboard;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceSnapshot;
import name.abuchen.portfolio.util.TextUtil;

public class TopContributorsReturnWidget extends AbstractTopContributorsWidget<List<SecurityPerformanceRecord>>
{
    public TopContributorsReturnWidget(Widget widget, DashboardData dashboardData)
    {
        super(widget, dashboardData);

        addConfig(new ReportingPeriodConfig(this));
        addConfig(new DataSeriesConfig(this, false));
        addConfig(new CountConfig(this));
    }

    @Override
    public Supplier<List<SecurityPerformanceRecord>> getUpdateTask()
    {
        return () -> {
            var interval = get(ReportingPeriodConfig.class).getReportingPeriod().toInterval(LocalDate.now());

            var index = getDashboardData().calculate(get(DataSeriesConfig.class).getDataSeries(), interval);

            var snapshot = index.getClientPerformanceSnapshot().orElseThrow(IllegalArgumentException::new);
            var client = snapshot.getClient();
            var converter = getDashboardData().getCurrencyConverter();

            var secSnapshot = SecurityPerformanceSnapshot.create(client, converter, interval);
            return new ArrayList<>(secSnapshot.getRecords());
        };
    }

    @Override
    protected List<DisplayRow> buildDisplayRows(List<SecurityPerformanceRecord> records)
    {
        records.sort((a, b) -> Double.compare(b.getTrueTimeWeightedRateOfReturn(),
                        a.getTrueTimeWeightedRateOfReturn()));

        return records.stream()
                        .map(r -> new DisplayRow(r.getSecurity(), TextUtil.escapeHtml(r.getSecurity().getName()),
                                        Values.Percent2.format(r.getTrueTimeWeightedRateOfReturn()),
                                        r.getTrueTimeWeightedRateOfReturn() >= 0))
                        .toList();
    }
}
