package name.abuchen.portfolio.ui.views.dashboard.heatmap;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.Locale;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.views.dashboard.DashboardData;
import name.abuchen.portfolio.ui.views.dashboard.DataSeriesConfig;
import name.abuchen.portfolio.ui.views.dashboard.ReportingPeriodConfig;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries;
import name.abuchen.portfolio.util.Interval;

public class PerformanceHeatmapWidget extends AbstractHeatmapWidget
{
    public PerformanceHeatmapWidget(Widget widget, DashboardData data)
    {
        super(widget, data);

        addConfig(new DataSeriesConfig(this, true));
    }

    @Override
    protected HeatmapModel build()
    {
        int numDashboardColumns = getDashboardData().getDashboard().getColumns().size();

        // fill the table lines according to the supplied period
        // calculate the performance with a temporary reporting period
        // calculate the color interpolated between red and green with yellow as
        // the median
        Interval interval = get(ReportingPeriodConfig.class).getReportingPeriod().toInterval();

        DataSeries dataSeries = get(DataSeriesConfig.class).getDataSeries();

        // adapt interval to include the first and last month fully

        Interval calcInterval = Interval.of(
                        interval.getStart().getDayOfMonth() == interval.getStart().lengthOfMonth() ? interval.getStart()
                                        : interval.getStart().withDayOfMonth(1).minusDays(1),
                        interval.getEnd().withDayOfMonth(interval.getEnd().lengthOfMonth()));

        PerformanceIndex performanceIndex = getDashboardData().calculate(dataSeries,
                        new ReportingPeriod.FromXtoY(calcInterval));

        Interval actualInterval = performanceIndex.getActualInterval();

        boolean showSum = get(HeatmapOrnamentConfig.class).getValues().contains(HeatmapOrnament.SUM);

        HeatmapModel model = new HeatmapModel();
        model.setCellToolTip(Messages.PerformanceHeatmapToolTip);

        // add header
        addHeader(model, numDashboardColumns, showSum);

        for (Integer year : actualInterval.iterYears())
        {
            String label = numDashboardColumns > 2 ? String.valueOf(year % 100) : String.valueOf(year);
            HeatmapModel.Row row = new HeatmapModel.Row(label);

            // monthly data
            for (LocalDate month = LocalDate.of(year, 1, 1); month.getYear() == year; month = month.plusMonths(1))
            {
                if (actualInterval.contains(month))
                    row.addData(getPerformanceFor(performanceIndex, month));
                else
                    row.addData(null);
            }

            // sum
            if (showSum)
                row.addData(getSumPerformance(performanceIndex, LocalDate.of(year, 1, 1)));
 
            model.addRow(row);
        }

        // create geometric mean

        if (get(HeatmapOrnamentConfig.class).getValues().contains(HeatmapOrnament.GEOMETRIC_MEAN))
        {
            HeatmapModel.Row geometricMean = new HeatmapModel.Row("x\u0304 geom"); //$NON-NLS-1$
            for (int index = 0; index < model.getHeaderSize(); index++)
                geometricMean.addData(geometricMean(model.getColumnValues(index)));
            model.addRow(geometricMean);
        }

        return model;
    }

    private void addHeader(HeatmapModel model, int numDashboardColumns, boolean showSum)
    {
        TextStyle textStyle;
        if (numDashboardColumns == 1)
            textStyle = TextStyle.FULL;
        else if (numDashboardColumns == 2)
            textStyle = TextStyle.SHORT;
        else
            textStyle = TextStyle.NARROW;

        // no harm in hardcoding the year as each year has the same months
        for (LocalDate m = LocalDate.of(2016, 1, 1); m.getYear() == 2016; m = m.plusMonths(1))
            model.addHeader(m.getMonth().getDisplayName(textStyle, Locale.getDefault()));
        if (showSum)
            model.addHeader("\u03A3"); //$NON-NLS-1$
    }

    private Double getPerformanceFor(PerformanceIndex index, LocalDate month)
    {
        int start = Arrays.binarySearch(index.getDates(), month.minusDays(1));
        // should not happen, but let's be defensive this time
        if (start < 0)
            return null;

        int end = Arrays.binarySearch(index.getDates(), month.withDayOfMonth(month.lengthOfMonth()));
        // make sure there is an end index if the binary search returns a
        // negative value (i.e. if the current month is not finished)
        if (end < 0)
        {
            // take the last available date
            end = index.getDates().length - 1;
        }

        return ((index.getAccumulatedPercentage()[end] + 1) / (index.getAccumulatedPercentage()[start] + 1)) - 1;
    }

    private Double getSumPerformance(PerformanceIndex index, LocalDate year)
    {
        int start = Arrays.binarySearch(index.getDates(), year.minusDays(1));
        if (start < 0)
            start = 0;

        int end = Arrays.binarySearch(index.getDates(), year.withDayOfYear(year.lengthOfYear()));
        if (end < 0)
            end = index.getDates().length - 1;

        return ((index.getAccumulatedPercentage()[end] + 1) / (index.getAccumulatedPercentage()[start] + 1)) - 1;
    }

}
