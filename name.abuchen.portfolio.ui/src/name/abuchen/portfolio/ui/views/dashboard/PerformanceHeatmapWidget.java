package name.abuchen.portfolio.ui.views.dashboard;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.util.Interval;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class PerformanceHeatmapWidget extends ReportingPeriodWidget
{
    ReportingPeriod period;

    public PerformanceHeatmapWidget(Widget widget, DashboardData data)
    {
        super(widget, data);
        period = data.getDefaultReportingPeriod();
    }

    @Override
    Composite createControl(Composite parent, DashboardResources resources)
    {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(13).margins(5, 5).applyTo(container);
        container.setBackground(parent.getBackground());
        // Top Left is empty
        Label spacer = new Label(container, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(spacer);
        // then the legend of the months
        Interval interval = period.toInterval();
        int year = interval.getStart().getYear();
        Label currLabel;
        for (LocalDate legendMonth = LocalDate.of(year, 1, 1); legendMonth.getYear() == year; legendMonth = legendMonth
                        .plusMonths(1))
        {
            currLabel = new Label(container, SWT.NONE);
            currLabel.setText(legendMonth.getMonth().getDisplayName(TextStyle.NARROW, Locale.GERMAN));
            GridDataFactory.fillDefaults().grab(true, false).applyTo(currLabel);
        }
        // now loop the years
        for (LocalDate yearDate : interval.iterYears())
        {
            currLabel = new Label(container, SWT.NONE);
            currLabel.setText(((Integer) yearDate.getYear()).toString());
            GridDataFactory.fillDefaults().grab(true, false).applyTo(currLabel);
            for (LocalDate currMonth = LocalDate.of(yearDate.getYear(), 1, 1); currMonth.getYear() == yearDate
                            .getYear(); currMonth = currMonth.plusMonths(1))
            {
                currLabel = new Label(container, SWT.NONE);
                if (interval.contains(currMonth))
                {
                    currLabel.setText("!");
                }
                GridDataFactory.fillDefaults().grab(true, false).applyTo(currLabel);
            }
        }
        return container;
    }

    @Override
    void update()
    {

    }

    @Override
    void attachContextMenu(IMenuListener listener)
    {

    }

}
