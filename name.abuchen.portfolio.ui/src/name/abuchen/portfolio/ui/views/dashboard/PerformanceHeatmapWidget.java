package name.abuchen.portfolio.ui.views.dashboard;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.util.ContextMenu;
import name.abuchen.portfolio.util.Interval;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

public class PerformanceHeatmapWidget extends ReportingPeriodWidget
{

    private Composite table;
    private Label title;
    private Label currLabel;
    private PerformanceIndex performance;
    private DashboardResources resources;

    public PerformanceHeatmapWidget(Widget widget, DashboardData data)
    {
        super(widget, data);
    }

    @Override
    Composite createControl(Composite parent, DashboardResources resources)
    {
        this.resources = resources;
        table = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(13).spacing(1, 1).applyTo(table);
        table.setBackground(parent.getBackground());
        fillTable();
        return table;
    }

    private Color getScaledColorForPerformance(double performance)
    {
        double max = 0.05;
        double min = max * (-1d);
        performance = Math.min(max, performance);
        performance = Math.max(min, performance);
        if (performance > 0d)
        {
            return resources.getResourceManager().createColor(
                            new RGB(new Double(255 * (1 - performance / max)).intValue(), 255, new Double(
                                            255 * (1 - performance / max)).intValue()));
        }
        else
        {
            return resources.getResourceManager().createColor(
                            new RGB(255, new Double(255 * (1 - performance / min)).intValue(), new Double(
                                            255 * (1 - performance / min)).intValue()));
        }
    }

    private void fillTable()
    {
        Interval interval = getReportingPeriod().toInterval();
        ReportingPeriod currPeriod;
        // Top Left is empty
        title = new Label(table, SWT.NONE);
        title.setText("Heatmap");
        GridDataFactory.fillDefaults().grab(false, true).applyTo(title);
        // then the legend of the months
        // no harm in hardcoding the year as each year has the same months
        for (LocalDate legendMonth = LocalDate.of(2016, 1, 1); legendMonth.getYear() == 2016; legendMonth = legendMonth
                        .plusMonths(1))
        {
            currLabel = new Label(table, SWT.NONE);
            currLabel.setText(legendMonth.getMonth().getDisplayName(TextStyle.NARROW, Locale.GERMAN));
            currLabel.setAlignment(SWT.CENTER);
            GridDataFactory.fillDefaults().grab(false, true).applyTo(currLabel);
        }
        // now loop the years
        for (LocalDate yearDate : interval.iterYears())
        {
            currLabel = new Label(table, SWT.NONE);
            currLabel.setText(((Integer) yearDate.getYear()).toString());
            GridDataFactory.fillDefaults().grab(true, false).applyTo(currLabel);
            for (LocalDate currMonth = LocalDate.of(yearDate.getYear(), 1, 1); currMonth.getYear() == yearDate
                            .getYear(); currMonth = currMonth.plusMonths(1))
            {
                currLabel = new Label(table, SWT.NONE);
                if (interval.contains(currMonth))
                {
                    currLabel.setFont(resources.getSmallFont());
                    currPeriod = new ReportingPeriod.FromXtoY(currMonth, currMonth.plusMonths(1));
                    performance = getDashboardData().calculate(PerformanceIndex.class, currPeriod);
                    currLabel.setText(Values.PercentShort.format(performance.getFinalAccumulatedPercentage()));
                    currLabel.setBackground(getScaledColorForPerformance(performance.getFinalAccumulatedPercentage()));
                }
                GridDataFactory.fillDefaults().grab(true, false).applyTo(currLabel);
            }
        }
        table.getParent().layout(true);
        table.layout(true);
    }

    @Override
    void update()
    {
        for (Control child : table.getChildren())
        {
            child.dispose();
        }
        fillTable();

    }

    @Override
    void attachContextMenu(IMenuListener listener)
    {
        new ContextMenu(title, listener).hook();
    }

}
