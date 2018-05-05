package name.abuchen.portfolio.ui.views.dashboard.heatmap;

import java.util.List;
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
import name.abuchen.portfolio.ui.views.dashboard.MultiDataSeriesConfig;
import name.abuchen.portfolio.ui.views.dashboard.ReportingPeriodConfig;
import name.abuchen.portfolio.ui.views.dashboard.WidgetDelegate;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries;
import name.abuchen.portfolio.util.Interval;

public class YearlyPerformanceHeatmapWidget extends WidgetDelegate
{
    private Composite table;
    private Label title;
    private DashboardResources resources;

    public YearlyPerformanceHeatmapWidget(Widget widget, DashboardData data)
    {
        super(widget, data);

        addConfig(new ReportingPeriodConfig(this));
        addConfig(new MultiDataSeriesConfig(this));
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

        List<DataSeries> dataSeries = get(MultiDataSeriesConfig.class).getDataSeries();

        DoubleFunction<Color> coloring = get(ColorSchemaConfig.class).getColorSchema()
                        .buildColorFunction(resources.getResourceManager());

        // adapt interval to include the first and last year fully

        Interval calcInterval = Interval.of(
                        interval.getStart().getDayOfYear() == interval.getStart().lengthOfYear() ? interval.getStart()
                                        : interval.getStart().withDayOfYear(1).minusDays(1),
                        interval.getEnd().withDayOfYear(interval.getEnd().lengthOfYear()));

        GridLayoutFactory.fillDefaults().numColumns(dataSeries.size() + 1).equalWidth(true).spacing(1, 1)
                        .applyTo(table);

        addHeaderRow(dataSeries);

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
        table.layout(true);
    }

    private void addHeaderRow(List<DataSeries> series)
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
