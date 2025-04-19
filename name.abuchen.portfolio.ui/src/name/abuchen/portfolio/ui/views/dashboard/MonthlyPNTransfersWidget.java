package name.abuchen.portfolio.ui.views.dashboard;

import java.util.stream.LongStream;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.ui.views.dashboard.heatmap.AbstractMonhtlyHeatmapWidget;
import name.abuchen.portfolio.ui.views.dashboard.heatmap.HeatmapModel;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries;
import name.abuchen.portfolio.util.Interval;

public class MonthlyPNTransfersWidget extends AbstractMonhtlyHeatmapWidget
{
    public MonthlyPNTransfersWidget(Widget widget, DashboardData data)
    {
        super(widget, data);
        addConfig(new DataSeriesConfig(this, false));
    }

    @Override
    protected void linkActivated()
    {
        // Nothing to do when clicking the title for now
    }

    @Override
    protected void processTransactions(int startYear, Interval interval, HeatmapModel<Long> model,
                    Client filteredClient)
    {
        Client client = getDashboardData().getClient();
        DataSeries series = get(DataSeriesConfig.class).getDataSeries();

        client.getAccounts().stream().flatMap(a -> a.getTransactions().stream())
                        .filter(t -> interval.contains(t.getDateTime())).forEach(t -> {
                            int row = t.getDateTime().getYear() - startYear;
                            int col = t.getDateTime().getMonth().getValue() - 1;

                            Interval monthInterval = Interval.of(t.getDateTime().toLocalDate().withDayOfMonth(1),
                                            t.getDateTime().toLocalDate().plusMonths(1).withDayOfMonth(1).minusDays(1));
                            PerformanceIndex monthlyIndex = getDashboardData().calculate(series, monthInterval);

                            long[] transferals = monthlyIndex.getTransferals();
                            long monthlyTransfers = transferals.length > 1 ? LongStream.of(transferals).skip(1).sum()
                                            : 0L;

                            Long oldValue = model.getRow(row).getData(col);
                            if (oldValue != null)
                            {
                                model.getRow(row).setData(col, monthlyTransfers);
                            }
                        });
    }
}
