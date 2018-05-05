package name.abuchen.portfolio.ui.views.dashboard.heatmap;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.DoubleFunction;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.InfoToolTip;
import name.abuchen.portfolio.ui.views.dashboard.DashboardData;
import name.abuchen.portfolio.ui.views.dashboard.DashboardResources;
import name.abuchen.portfolio.ui.views.dashboard.DataSeriesConfig;
import name.abuchen.portfolio.ui.views.dashboard.ReportingPeriodConfig;
import name.abuchen.portfolio.ui.views.dashboard.WidgetDelegate;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries;
import name.abuchen.portfolio.util.Interval;

public class PerformanceHeatmapWidget extends WidgetDelegate
{
    private Composite table;
    private Label title;
    private DashboardResources resources;

    public PerformanceHeatmapWidget(Widget widget, DashboardData data)
    {
        super(widget, data);

        addConfig(new ReportingPeriodConfig(this));
        addConfig(new DataSeriesConfig(this, true));
        addConfig(new ColorSchemaConfig(this));
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        this.resources = resources;

        Composite container = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).applyTo(container);
        container.setBackground(parent.getBackground());

        title = new Label(container, SWT.NONE);
        title.setText(getWidget().getLabel() != null ? getWidget().getLabel() : ""); //$NON-NLS-1$
        GridDataFactory.fillDefaults().grab(true, false).applyTo(title);

        table = new Composite(container, SWT.NONE);
        // 13 columns, one for the legend and 12 for the months
        GridDataFactory.fillDefaults().grab(true, false).applyTo(table);
        GridLayoutFactory.fillDefaults().numColumns(13).equalWidth(true).spacing(1, 1).applyTo(table);
        table.setBackground(container.getBackground());

        fillTable();

        return container;
    }

    private void fillTable()
    {
        // fill the table lines according to the supplied period
        // calculate the performance with a temporary reporting period
        // calculate the color interpolated between red and green with yellow as
        // the median
        Interval interval = get(ReportingPeriodConfig.class).getReportingPeriod().toInterval();

        DoubleFunction<Color> coloring = get(ColorSchemaConfig.class).getColorSchema()
                        .buildColorFunction(resources.getResourceManager());

        addHeaderRow();

        DataSeries dataSeries = get(DataSeriesConfig.class).getDataSeries();

        // adapt interval to include the first and last month fully

        Interval calcInterval = Interval.of(
                        interval.getStart().getDayOfMonth() == interval.getStart().lengthOfMonth() ? interval.getStart()
                                        : interval.getStart().withDayOfMonth(1).minusDays(1),
                        interval.getEnd().withDayOfMonth(interval.getEnd().lengthOfMonth()));

        PerformanceIndex performanceIndex = getDashboardData().calculate(dataSeries,
                        new ReportingPeriod.FromXtoY(calcInterval));

        Interval actualInterval = performanceIndex.getActualInterval();

        int numDashboardColumns = getDashboardData().getDashboard().getColumns().size();

        for (Integer year : actualInterval.iterYears())
        {
            // year
            String label = numDashboardColumns > 2 ? String.valueOf(year % 100) : String.valueOf(year);
            Cell cell = new Cell(table, new CellDataProvider(label));
            GridDataFactory.fillDefaults().grab(true, false).applyTo(cell);

            // monthly data
            for (LocalDate month = LocalDate.of(year, 1, 1); month.getYear() == year; month = month.plusMonths(1))
            {
                if (actualInterval.contains(month))
                {
                    cell = createCell(performanceIndex, month, coloring);
                    InfoToolTip.attach(cell, Messages.PerformanceHeatmapToolTip);
                }
                else
                {
                    cell = new Cell(table, new CellDataProvider("")); //$NON-NLS-1$
                }
                GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(cell);
            }
        }
        table.layout(true);
    }

    private Cell createCell(PerformanceIndex index, LocalDate month, DoubleFunction<Color> coloring)
    {
        int start = Arrays.binarySearch(index.getDates(), month.minusDays(1));
        // should not happen, but let's be defensive this time
        if (start < 0)
            return new Cell(table, new CellDataProvider("")); //$NON-NLS-1$

        int end = Arrays.binarySearch(index.getDates(), month.withDayOfMonth(month.lengthOfMonth()));
        // make sure there is an end index if the binary search returns a
        // negative value (i.e. if the current month is not finished)
        if (end < 0)
        {
            // take the last available date
            end = index.getDates().length - 1;
        }

        double performance = ((index.getAccumulatedPercentage()[end] + 1)
                        / (index.getAccumulatedPercentage()[start] + 1)) - 1;

        return new Cell(table, new CellDataProvider(coloring.apply(performance), resources.getSmallFont(),
                        Values.PercentShort.format(performance)));
    }

    private void addHeaderRow()
    {
        // Top Left is empty
        new Cell(table, new CellDataProvider("")); //$NON-NLS-1$

        int numColumns = getDashboardData().getDashboard().getColumns().size();
        TextStyle textStyle;
        if (numColumns == 1)
            textStyle = TextStyle.FULL;
        else if (numColumns == 2)
            textStyle = TextStyle.SHORT;
        else
            textStyle = TextStyle.NARROW;

        // then the legend of the months
        // no harm in hardcoding the year as each year has the same months
        for (LocalDate m = LocalDate.of(2016, 1, 1); m.getYear() == 2016; m = m.plusMonths(1))
        {
            Cell cell = new Cell(table,
                            new CellDataProvider(m.getMonth().getDisplayName(textStyle, Locale.getDefault())));
            GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(cell);
        }
    }

    @Override
    public void update()
    {
        title.setText(getWidget().getLabel() != null ? getWidget().getLabel() : ""); //$NON-NLS-1$

        for (Control child : table.getChildren())
            child.dispose();

        fillTable();

        table.getParent().layout(true);
        table.getParent().getParent().layout(true);
    }

    @Override
    public Control getTitleControl()
    {
        return title;
    }

}
