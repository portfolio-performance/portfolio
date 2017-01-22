package name.abuchen.portfolio.ui.views.dashboard;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.InfoToolTip;
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
    }

    @Override
    Composite createControl(Composite parent, DashboardResources resources)
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
        GridLayoutFactory.fillDefaults().numColumns(13).spacing(1, 1).applyTo(table);
        table.setBackground(container.getBackground());
        fillTable();

        return container;
    }

    private Color getScaledColorForPerformance(double performance)
    {
        double max = 0.07;
        double p = Math.min(max, Math.abs(performance));
        int colorValue = (int) (255 * (1 - p / max));
        RGB color = performance > 0d ? new RGB(colorValue, 255, colorValue) : new RGB(255, colorValue, colorValue);
        return resources.getResourceManager().createColor(color);
    }

    private void fillTable()
    {
        // fill the table lines according to the supplied period
        // calculate the performance with a temporary reporting period
        // calculate the color interpolated between red and green with white as
        // the median
        Interval interval = get(ReportingPeriodConfig.class).getReportingPeriod().toInterval();
        ReportingPeriod currPeriod;

        // Top Left is empty
        Label topLeft = new Label(table, SWT.NONE);
        topLeft.setText(""); //$NON-NLS-1$

        // then the legend of the months
        // no harm in hardcoding the year as each year has the same months
        for (LocalDate legendMonth = LocalDate.of(2016, 1, 1); legendMonth.getYear() == 2016; legendMonth = legendMonth
                        .plusMonths(1))
        {
            Label currLabel = new Label(table, SWT.NONE);
            currLabel.setText(legendMonth.getMonth().getDisplayName(TextStyle.NARROW, Locale.GERMAN));
            currLabel.setAlignment(SWT.CENTER);
            GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(currLabel);
        }

        // now loop the years
        DataSeries dataSeries = get(DataSeriesConfig.class).getDataSeries();

        for (Integer year : interval.iterYears())
        {
            Label currLabel = new Label(table, SWT.NONE);
            currLabel.setText(year.toString());
            GridDataFactory.fillDefaults().grab(true, false).applyTo(currLabel);

            for (LocalDate currMonth = LocalDate.of(year, 1, 1); currMonth.getYear() == year; currMonth = currMonth
                            .plusMonths(1))
            {
                currLabel = new Label(table, SWT.RIGHT);
                if (interval.contains(currMonth))
                {
                    currPeriod = new ReportingPeriod.FromXtoY(currMonth.minusDays(1),
                                    currMonth.withDayOfMonth(currMonth.lengthOfMonth()));
                    PerformanceIndex performance = getDashboardData().calculate(dataSeries, currPeriod);

                    currLabel.setFont(resources.getSmallFont());
                    currLabel.setAlignment(SWT.CENTER);
                    currLabel.setText(Values.PercentShort.format(performance.getFinalAccumulatedPercentage()));
                    currLabel.setBackground(getScaledColorForPerformance(performance.getFinalAccumulatedPercentage()));
                    InfoToolTip.attach(currLabel, Messages.PerformanceHeatmapToolTip);
                }
                GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(currLabel);
            }
        }
        table.layout(true);
    }

    @Override
    void update()
    {
        title.setText(getWidget().getLabel() != null ? getWidget().getLabel() : ""); //$NON-NLS-1$
        for (Control child : table.getChildren())
            child.dispose();
        fillTable();
        table.getParent().layout(true);
        table.getParent().getParent().layout(true);
    }

    @Override
    Control getTitleControl()
    {
        return title;
    }

}
