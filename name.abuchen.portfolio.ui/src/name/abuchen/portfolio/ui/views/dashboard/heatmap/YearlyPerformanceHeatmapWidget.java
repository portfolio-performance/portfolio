package name.abuchen.portfolio.ui.views.dashboard.heatmap;

import java.util.List;
import java.util.function.DoubleFunction;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.InfoToolTip;
import name.abuchen.portfolio.ui.views.dashboard.DashboardData;
import name.abuchen.portfolio.ui.views.dashboard.DashboardResources;
import name.abuchen.portfolio.ui.views.dashboard.MultiDataSeriesConfig;
import name.abuchen.portfolio.ui.views.dashboard.ReportingPeriodConfig;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries;
import name.abuchen.portfolio.util.Interval;

public class YearlyPerformanceHeatmapWidget extends AbstractHeatmapWidget
{
    public YearlyPerformanceHeatmapWidget(Widget widget, DashboardData data)
    {
        super(widget, data);

        addConfig(new MultiDataSeriesConfig(this));
    }

    @Override
    protected void fillTable(Composite table, DashboardResources resources)
    {
        // fill the table lines according to the supplied period
        // calculate the performance with a temporary reporting period
        // calculate the color interpolated between red and green with yellow as
        // the median
        Interval interval = get(ReportingPeriodConfig.class).getReportingPeriod().toInterval();

        List<DataSeries> dataSeries = get(MultiDataSeriesConfig.class).getDataSeries();

        DoubleFunction<Color> coloring = get(ColorSchemaConfig.class).getValue()
                        .buildColorFunction(resources.getResourceManager());

        // adapt interval to include the first and last year fully

        Interval calcInterval = Interval.of(
                        interval.getStart().getDayOfYear() == interval.getStart().lengthOfYear() ? interval.getStart()
                                        : interval.getStart().withDayOfYear(1).minusDays(1),
                        interval.getEnd().withDayOfYear(interval.getEnd().lengthOfYear()));

        GridLayoutFactory.fillDefaults().numColumns(dataSeries.size() + 1).equalWidth(true).spacing(1, 1)
                        .applyTo(table);

        addHeaderRow(table, dataSeries);

        int numDashboardColumns = getDashboardData().getDashboard().getColumns().size();

        for (Integer year : calcInterval.iterYears())
        {
            // year
            String label = numDashboardColumns > 2 ? String.valueOf(year % 100) : String.valueOf(year);
            Cell cell = new Cell(table, new CellDataProvider(label));
            GridDataFactory.fillDefaults().grab(true, false).applyTo(cell);

            // yearly data
            for (DataSeries series : dataSeries)
            {
                PerformanceIndex performanceIndex = getDashboardData().calculate(series,
                                new ReportingPeriod.YearX(year));

                cell = new Cell(table, new CellDataProvider(
                                coloring.apply(performanceIndex.getFinalAccumulatedPercentage()),
                                resources.getSmallFont(),
                                Values.Percent.format(performanceIndex.getFinalAccumulatedPercentage())));

                InfoToolTip.attach(cell, Messages.YearlyPerformanceHeatmapToolTip);
                GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(cell);
            }
        }

        Cell cell = new Cell(table, new CellDataProvider("\u03A3")); //$NON-NLS-1$
        GridDataFactory.fillDefaults().grab(true, false).applyTo(cell);
        for (DataSeries series : dataSeries)
        {
            PerformanceIndex performanceIndex = getDashboardData().calculate(series, new ReportingPeriod.FromXtoY(calcInterval));

            cell = new Cell(table,
                            new CellDataProvider(coloring.apply(performanceIndex.getFinalAccumulatedPercentage()),
                                            resources.getSmallFont(),
                                            Values.Percent.format(performanceIndex.getFinalAccumulatedPercentage())));

            InfoToolTip.attach(cell, Messages.YearlyPerformanceHeatmapToolTip);
            GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(cell);
        }

        table.layout(true);
    }

    private void addHeaderRow(Composite table, List<DataSeries> series)
    {
        // top left cell
        new Cell(table, new CellDataProvider("")); //$NON-NLS-1$

        for (DataSeries s : series)
        {
            Cell cell = new Cell(table, new CellDataProvider(s.getLabel()));
            InfoToolTip.attach(cell, s.getLabel());
            GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(cell);
        }
    }
}
