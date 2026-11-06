package name.abuchen.portfolio.ui.views.dashboard.performance;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swtchart.Range;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.util.chart.TimelineChart;
import name.abuchen.portfolio.ui.util.format.AxisTickPercentNumberFormat;
import name.abuchen.portfolio.ui.views.dashboard.ChartHeightConfig;
import name.abuchen.portfolio.ui.views.dashboard.ChartShowYAxisConfig;
import name.abuchen.portfolio.ui.views.dashboard.DashboardData;
import name.abuchen.portfolio.ui.views.dashboard.DashboardResources;
import name.abuchen.portfolio.ui.views.dashboard.DataSeriesConfig;
import name.abuchen.portfolio.ui.views.dashboard.WidgetDelegate;
import name.abuchen.portfolio.ui.views.dashboard.earnings.StartYearConfig;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries;
import name.abuchen.portfolio.ui.views.payments.PaymentsColors;
import name.abuchen.portfolio.util.TextUtil;

/**
 * Shows the cumulative time-weighted rate of return of several calendar years
 * on top of each other: one line per year, each starting at 0% on January 1st.
 * <p>
 * The dates of all years are projected onto one reference year so that they
 * share the date axis and can be compared at the same day of the year.
 */
public class PerformancePerYearWidget extends WidgetDelegate<List<PerformancePerYearWidget.YearSeries>>
{
    /* package */ record YearSeries(int year, LocalDate[] dates, double[] values)
    {
    }

    /**
     * Reference year onto which all calendar years are projected. It is a leap
     * year, therefore February 29th has a slot.
     */
    private static final int REFERENCE_YEAR = 2000;

    // the reference year is an implementation detail and must not be shown
    private static final DateTimeFormatter AXIS_FORMAT = DateTimeFormatter.ofPattern("LLL"); //$NON-NLS-1$
    private static final DateTimeFormatter TOOLTIP_FORMAT = DateTimeFormatter.ofPattern("d. LLLL"); //$NON-NLS-1$

    private Label title;
    private TimelineChart chart;

    public PerformancePerYearWidget(Widget widget, DashboardData dashboardData)
    {
        super(widget, dashboardData);

        addConfig(new DataSeriesConfig(this, false));
        addConfig(new StartYearConfig(this, 5));
        addConfig(new ChartShowYAxisConfig(this, true));
        addConfig(new ChartHeightConfig(this));
    }

    /**
     * Projects the dates of the reporting period of the given calendar year
     * onto the reference year. The period starts on December 31st of the
     * previous year; that date is projected onto the year before the reference
     * year and therefore keeps sorting before January 1st.
     */
    /* package */ static LocalDate[] project(LocalDate[] dates, int year)
    {
        var answer = new LocalDate[dates.length];
        for (int ii = 0; ii < dates.length; ii++)
            answer[ii] = dates[ii].withYear(REFERENCE_YEAR + dates[ii].getYear() - year);
        return answer;
    }

    /**
     * Returns true if no assets are held on any day of the period. Such years
     * are skipped because they would only add a flat line at 0%.
     */
    /* package */ static boolean hasNoHoldings(long[] totals)
    {
        for (long total : totals)
        {
            if (total != 0)
                return false;
        }
        return true;
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        var container = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).applyTo(container);

        title = new Label(container, SWT.NONE);
        title.setText(TextUtil.tooltip(getWidget().getLabel()));
        GridDataFactory.fillDefaults().grab(true, false).applyTo(title);

        chart = new TimelineChart(container);
        chart.getTitle().setVisible(false);
        chart.getTitle().setText(title.getText());
        chart.getAxisSet().getYAxis(0).getTick().setVisible(get(ChartShowYAxisConfig.class).getIsShowYAxis());

        chart.setXAxisDateFormat(AXIS_FORMAT);
        chart.getToolTip().setXAxisFormat(
                        obj -> obj instanceof LocalDate date ? TOOLTIP_FORMAT.format(date) : String.valueOf(obj));
        chart.getToolTip().setDefaultValueFormat(new DecimalFormat("0.##%")); //$NON-NLS-1$
        chart.getToolTip().reverseLabels(true);

        chart.getLegend().setVisible(true);
        chart.getLegend().setPosition(SWT.BOTTOM);

        int yHint = get(ChartHeightConfig.class).getPixel();
        GridDataFactory.fillDefaults().hint(SWT.DEFAULT, yHint).grab(true, false).span(2, 1).applyTo(chart);

        getDashboardData().getStylingEngine().style(chart);

        container.layout();

        return container;
    }

    @Override
    public Control getTitleControl()
    {
        return title;
    }

    @Override
    public Supplier<List<YearSeries>> getUpdateTask()
    {
        DataSeries series = get(DataSeriesConfig.class).getDataSeries();
        int startYear = get(StartYearConfig.class).getStartYear();

        return () -> {
            var now = LocalDate.now();
            var answer = new ArrayList<YearSeries>();

            for (int year = startYear; year <= now.getYear(); year++)
            {
                PerformanceIndex index = getDashboardData().calculate(series,
                                new ReportingPeriod.YearX(year).toInterval(now));

                if (hasNoHoldings(index.getTotals()))
                    continue;

                answer.add(new YearSeries(year, project(index.getDates(), year),
                                index.getAccumulatedPercentage()));
            }

            return answer;
        };
    }

    @Override
    public void update(List<YearSeries> data)
    {
        title.setText(TextUtil.tooltip(getWidget().getLabel()));

        try
        {
            chart.suspendUpdate(true);

            get(ChartHeightConfig.class).updateGridData(chart, title.getParent());

            chart.getTitle().setText(title.getText());

            for (var s : chart.getSeriesSet().getSeries())
                chart.getSeriesSet().deleteSeries(s.getId());

            chart.getAxisSet().getYAxis(0).getTick().setFormat(new AxisTickPercentNumberFormat("0.#%")); //$NON-NLS-1$
            chart.getAxisSet().getYAxis(0).getTick().setVisible(get(ChartShowYAxisConfig.class).getIsShowYAxis());

            // the dashboard passes null if no result has been calculated yet
            if (data != null)
            {
                for (YearSeries series : data)
                {
                    var label = String.valueOf(series.year());
                    chart.addDateSeries(label, series.dates(), series.values(),
                                    PaymentsColors.getColor(series.year()), label);
                }
            }

            chart.adjustRange();

            // pin the axis to the reference year: the reporting period starts
            // on December 31st of the previous year and #adjustRange pads the
            // range, both of which would show line segments and grid lines
            // outside the calendar year
            chart.getAxisSet().getXAxis(0).setRange(new Range(LocalDate.of(REFERENCE_YEAR, 1, 1).toEpochDay(),
                            LocalDate.of(REFERENCE_YEAR, 12, 31).toEpochDay()));
        }
        finally
        {
            chart.suspendUpdate(false);
        }
        chart.redraw();
    }
}
