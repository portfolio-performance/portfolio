package name.abuchen.portfolio.ui.views.dashboard.heatmap;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.function.DoubleBinaryOperator;
import java.util.function.ToDoubleFunction;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.views.dashboard.DashboardData;
import name.abuchen.portfolio.ui.views.dashboard.DataSeriesConfig;
import name.abuchen.portfolio.ui.views.dashboard.ReportingPeriodConfig;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries;
import name.abuchen.portfolio.util.Interval;

public class PerformanceHeatmapWidget extends AbstractHeatmapWidget<Double>
{
    public PerformanceHeatmapWidget(Widget widget, DashboardData data)
    {
        super(widget, data);

        addConfig(new ColorSchemaConfig(this));
        addConfig(new HeatmapOrnamentConfig(this));
        addConfig(new DataSeriesConfig(this, true));
        addConfig(new ExcessReturnDataSeriesConfig(this));
        addConfig(new ExcessReturnOperatorConfig(this));
    }

    @Override
    protected HeatmapModel<Double> build()
    {
        int numDashboardColumns = getDashboardData().getDashboard().getColumns().size();

        Interval interval = get(ReportingPeriodConfig.class).getReportingPeriod().toInterval(LocalDate.now());

        // adapt interval to include the first and last month fully

        Interval calcInterval = Interval.of(
                        interval.getStart().getDayOfMonth() == interval.getStart().lengthOfMonth() ? interval.getStart()
                                        : interval.getStart().withDayOfMonth(1).minusDays(1),
                        interval.getEnd().with(TemporalAdjusters.lastDayOfMonth()));

        DataSeries dataSeries = get(DataSeriesConfig.class).getDataSeries();
        PerformanceIndex performanceIndex = getDashboardData().calculate(dataSeries, calcInterval);

        // build functions to calculate performance and sum values

        ToDoubleFunction<YearMonth> calculatePerformance = month -> getPerformanceFor(performanceIndex, month);
        ToDoubleFunction<Year> calculateSum = year -> getSumPerformance(performanceIndex, year);

        DataSeries benchmark = get(ExcessReturnDataSeriesConfig.class).getDataSeries();
        if (benchmark != null)
        {
            PerformanceIndex benchmarkIndex = getDashboardData().calculate(benchmark, calcInterval);

            DoubleBinaryOperator operator = get(ExcessReturnOperatorConfig.class).getValue().getOperator();
            calculatePerformance = month -> operator.applyAsDouble( //
                            getPerformanceFor(performanceIndex, month), getPerformanceFor(benchmarkIndex, month));
            calculateSum = year -> operator.applyAsDouble( //
                            getSumPerformance(performanceIndex, year), getSumPerformance(benchmarkIndex, year));
        }

        // build heat map model for actual interval

        Interval actualInterval = performanceIndex.getActualInterval();

        boolean showSum = get(HeatmapOrnamentConfig.class).getValues().contains(HeatmapOrnament.SUM);
        boolean showStandardDeviation = get(HeatmapOrnamentConfig.class).getValues()
                        .contains(HeatmapOrnament.STANDARD_DEVIATION);

        HeatmapModel<Double> model = new HeatmapModel<>(
                        numDashboardColumns == 1 ? Values.Percent : Values.PercentShort);
        model.setCellToolTip(v -> Messages.PerformanceHeatmapToolTip);

        // add header

        addMonthlyHeader(model, numDashboardColumns, showSum, showStandardDeviation, false);

        // build row for each year

        for (Year year : actualInterval.getYears())
        {
            String label = numDashboardColumns > 2 ? String.valueOf(year.getValue() % 100) : String.valueOf(year);
            HeatmapModel.Row<Double> row = new HeatmapModel.Row<>(label);

            // monthly data
            for (YearMonth month = YearMonth.of(year.getValue(), 1);
                 month.getYear() == year.getValue();
                 month = month.plusMonths(1))
            {
                if (actualInterval.intersects(Interval.of(month.atDay(1).minusDays(1), month.atEndOfMonth())))
                    row.addData(calculatePerformance.applyAsDouble(month));
                else
                    row.addData(null);
            }

            // sum
            if (showSum)
                row.addData(calculateSum.applyAsDouble(year));

            if (showStandardDeviation)
                row.addData(standardDeviation(row.getDataSubList(0, 12)));

            model.addRow(row);
        }

        // create geometric mean

        if (get(HeatmapOrnamentConfig.class).getValues().contains(HeatmapOrnament.GEOMETRIC_MEAN))
        {
            HeatmapModel.Row<Double> geometricMean = new HeatmapModel.Row<>("x\u0304 geom"); //$NON-NLS-1$
            for (int index = 0; index < model.getHeaderSize(); index++)
                geometricMean.addData(geometricMean(model.getColumnValues(index)));
            model.addRow(geometricMean);
        }

        return model;
    }

    private double getPerformanceFor(PerformanceIndex index, YearMonth month)
    {
        return index.getPerformance(Interval.of(month.atDay(1).minusDays(1),
                                                month.atEndOfMonth()));
    }

    private double getSumPerformance(PerformanceIndex index, Year year)
    {
        return index.getPerformance(Interval.of(year.atDay(1).minusDays(1),
                                                year.atDay(1).with(TemporalAdjusters.lastDayOfYear())));
    }
}
