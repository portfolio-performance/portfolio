package name.abuchen.portfolio.ui.views.dashboard.heatmap;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.views.dashboard.DashboardData;
import name.abuchen.portfolio.ui.views.dashboard.MultiDataSeriesConfig;
import name.abuchen.portfolio.ui.views.dashboard.ReportingPeriodConfig;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries;
import name.abuchen.portfolio.util.Interval;

public class YearlyPerformanceHeatmapWidget extends AbstractHeatmapWidget<Double>
{
    public YearlyPerformanceHeatmapWidget(Widget widget, DashboardData data)
    {
        super(widget, data);

        addConfig(new ColorSchemaConfig(this));
        addConfig(new HeatmapOrnamentConfig(this));
        addConfig(new MultiDataSeriesConfig(this));
    }

    @Override
    protected HeatmapModel<Double> build()
    {
        int numDashboardColumns = getDashboardData().getDashboard().getColumns().size();

        LocalDate now = LocalDate.now();

        // fill the table lines according to the supplied period
        // calculate the performance with a temporary reporting period
        // calculate the color interpolated between red and green with yellow as
        // the median
        Interval interval = get(ReportingPeriodConfig.class).getReportingPeriod().toInterval(now);

        List<DataSeries> dataSeries = get(MultiDataSeriesConfig.class).getDataSeries();

        // adapt interval to include the first and last year fully

        Interval calcInterval = Interval.of(
                        interval.getStart().getDayOfYear() == interval.getStart().lengthOfYear() ? interval.getStart()
                                        : interval.getStart().withDayOfYear(1).minusDays(1),
                        interval.getEnd().withDayOfYear(interval.getEnd().lengthOfYear()));

        HeatmapModel<Double> model = new HeatmapModel<>(
                        numDashboardColumns == 1 ? Values.Percent : Values.PercentShort);
        model.setCellToolTip(v -> Messages.YearlyPerformanceHeatmapToolTip);

        // add header
        for (DataSeries s : dataSeries)
            model.addHeader(s.getLabel());

        for (Integer year : calcInterval.iterYears())
        {
            String label = numDashboardColumns > 2 ? String.valueOf(year % 100) : String.valueOf(year);
            HeatmapModel.Row<Double> row = new HeatmapModel.Row<>(label);

            // yearly data
            for (DataSeries series : dataSeries)
            {
                PerformanceIndex performanceIndex = getDashboardData().calculate(series,
                                new ReportingPeriod.YearX(year).toInterval(now));
                row.addData(performanceIndex.getFinalAccumulatedPercentage());
            }

            model.addRow(row);
        }

        // add sum
        HeatmapModel.Row<Double> sum = null;
        if (get(HeatmapOrnamentConfig.class).getValues().contains(HeatmapOrnament.SUM))
        {
            sum = new HeatmapModel.Row<>("\u03A3", HeatmapOrnament.SUM.toString()); //$NON-NLS-1$
            for (DataSeries series : dataSeries)
            {
                PerformanceIndex performanceIndex = getDashboardData().calculate(series, calcInterval);
                sum.addData(performanceIndex.getFinalAccumulatedPercentage());
            }
        }

        // add standard deviation
        HeatmapModel.Row<Double> sd = null;
        if (get(HeatmapOrnamentConfig.class).getValues().contains(HeatmapOrnament.STANDARD_DEVIATION))
        {
            sd = new HeatmapModel.Row<>("s", HeatmapOrnament.STANDARD_DEVIATION.toString()); //$NON-NLS-1$

            for (int ii = 0; ii < dataSeries.size(); ii++)
                sd.addData(standardDeviation(model.getColumnValues(ii)));
        }

        if (sum != null)
            model.addRow(sum);
        if (sd != null)
            model.addRow(sd);

        // add geometric mean
        if (get(HeatmapOrnamentConfig.class).getValues().contains(HeatmapOrnament.GEOMETRIC_MEAN))
        {
            model.addHeader("x\u0304 geom"); //$NON-NLS-1$
            model.getRows().forEach(r -> r
                            .addData(geometricMean(r.getData().filter(Objects::nonNull).collect(Collectors.toList()))));
        }

        return model;
    }
}
