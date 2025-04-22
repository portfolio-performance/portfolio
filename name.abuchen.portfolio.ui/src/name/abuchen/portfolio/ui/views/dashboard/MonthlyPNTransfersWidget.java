package name.abuchen.portfolio.ui.views.dashboard;

import java.time.LocalDate;
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

        for (int year = interval.getStart().getYear(); year <= interval.getEnd().getYear(); year++)
        {
            int row = year - startYear;

            for (int month = 1; month <= 12; month++)
            {
                int col = month - 1;

                // Skip if this month is outside our interval
                LocalDate startOfMonth = LocalDate.of(year, month, 1);
                LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);
                if (endOfMonth.isBefore(interval.getStart()) || startOfMonth.isAfter(interval.getEnd()))
                    continue;

                // Calculate performance index of the month (including the day
                // before for proper calculation)
                Interval monthInterval = Interval.of(startOfMonth.minusDays(1), endOfMonth);
                PerformanceIndex monthlyIndex = getDashboardData().calculate(series, monthInterval);

                // Get transferals and sum them, skipping the first element (day
                // before start)
                long[] transferals = monthlyIndex.getTransferals();
                long monthlyTransfers = transferals.length > 1 ? LongStream.of(transferals).skip(1).sum() : 0L;

                // Set the value in the model
                Long oldValue = model.getRow(row).getData(col);
                if (oldValue != null)
                {
                    model.getRow(row).setData(col, monthlyTransfers);
                }
            }
        }
    }
}
