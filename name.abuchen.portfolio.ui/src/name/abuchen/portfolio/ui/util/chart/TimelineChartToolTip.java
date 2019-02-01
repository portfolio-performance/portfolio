package name.abuchen.portfolio.ui.util.chart;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.swtchart.Chart;
import org.swtchart.IAxis;
import org.swtchart.IBarSeries;
import org.swtchart.ILineSeries;
import org.swtchart.ISeries;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.util.TextUtil;

public class TimelineChartToolTip extends AbstractChartToolTip
{
    private String dateFormat = "%tF"; //$NON-NLS-1$
    private DecimalFormat valueFormat = new DecimalFormat("#,##0.00"); //$NON-NLS-1$

    private boolean categoryEnabled = false;
    private boolean reverseLabels = false;

    private Set<String> excludeFromTooltip = new HashSet<>();

    public TimelineChartToolTip(Chart chart)
    {
        super(chart);
    }

    public void enableCategory(boolean enabled)
    {
        categoryEnabled = enabled;
    }

    public void reverseLabels(boolean reverseLabels)
    {
        this.reverseLabels = reverseLabels;
    }

    public void setDateFormat(String dateFormat)
    {
        this.dateFormat = dateFormat;
    }

    public void setValueFormat(DecimalFormat valueFormat)
    {
        this.valueFormat = valueFormat;
    }

    /**
     * Add a series id which is not displayed in the tool tip.
     */
    public void addSeriesExclude(String id)
    {
        this.excludeFromTooltip.add(id);
    }

    private List<BiConsumer<Composite, Object>> extraInfoProvider = new ArrayList<>();

    public void addExtraInfo(BiConsumer<Composite, Object> extraInfoProvider)
    {
        this.extraInfoProvider.add(extraInfoProvider);
    }

    @Override
    protected Object getFocusObjectAt(Event event)
    {
        return categoryEnabled ? getFocusCategoryAt(event) : getFocusDateAt(event);
    }

    private Integer getFocusCategoryAt(Event event)
    {
        IAxis xAxis = getChart().getAxisSet().getXAxes()[0];
        int coordinate = (int) xAxis.getDataCoordinate(event.x);

        String[] categories = xAxis.getCategorySeries();

        if (coordinate < 0)
            coordinate = 0;
        else if (coordinate > categories.length - 1)
            coordinate = categories.length - 1;

        return coordinate;
    }

    private Date getFocusDateAt(Event event)
    {
        IAxis xAxis = getChart().getAxisSet().getXAxes()[0];

        long time = (long) xAxis.getDataCoordinate(event.x);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        ISeries[] allSeries = getChart().getSeriesSet().getSeries();
        if (allSeries.length == 0)
            return null;

        ISeries timeSeries = allSeries[0];
        int line = Arrays.binarySearch(timeSeries.getXDateSeries(), cal.getTime());

        if (line >= 0)
            return cal.getTime();

        // otherwise: find closest existing date
        line = -line - 1;

        if (line == 0)
            return timeSeries.getXDateSeries()[line];

        int length = timeSeries.getXDateSeries().length;
        if (line >= length)
            return timeSeries.getXDateSeries()[length - 1];

        // check which date is closer to the targeted date
        long target = cal.getTimeInMillis();
        Date left = timeSeries.getXDateSeries()[line - 1];
        Date right = timeSeries.getXDateSeries()[line];

        return target - left.getTime() < right.getTime() - target ? left : right;
    }

    @Override
    protected void createComposite(Composite parent)
    {
        final Composite container = new Composite(parent, SWT.NONE);
        container.setBackgroundMode(SWT.INHERIT_FORCE);
        RowLayout layout = new RowLayout(SWT.VERTICAL);
        layout.center = true;
        container.setLayout(layout);

        Color foregroundColor = Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
        container.setForeground(foregroundColor);
        container.setBackground(Colors.INFO_TOOLTIP_BACKGROUND);

        Composite data = new Composite(container, SWT.NONE);
        GridLayoutFactory.swtDefaults().numColumns(2).applyTo(data);

        Label left = new Label(data, SWT.NONE);
        left.setForeground(foregroundColor);
        left.setText(Messages.ColumnDate);

        Label right = new Label(data, SWT.NONE);
        right.setForeground(foregroundColor);
        right.setText(categoryEnabled
                        ? getChart().getAxisSet().getXAxis(0).getCategorySeries()[(Integer) getFocusedObject()]
                        : String.format(dateFormat, getFocusedObject()));

        ISeries[] allSeries = getChart().getSeriesSet().getSeries();
        if (reverseLabels)
            Collections.reverse(Arrays.asList(allSeries));

        for (ISeries series : allSeries)
        {
            if (excludeFromTooltip.contains(series.getId()))
                continue;

            double value;

            if (categoryEnabled)
            {
                int line = (Integer) getFocusedObject();
                if (line >= series.getYSeries().length)
                    continue;
                value = series.getYSeries()[line];
            }
            else
            {
                int line = Arrays.binarySearch(series.getXDateSeries(), getFocusedObject());
                if (line < 0)
                    continue;
                value = series.getYSeries()[line];
            }

            Color color = series instanceof ILineSeries ? ((ILineSeries) series).getLineColor()
                            : ((IBarSeries) series).getBarColor();

            left = new Label(data, SWT.NONE);
            left.setBackground(color);
            left.setForeground(Colors.getTextColor(color));
            left.setText(TextUtil.tooltip(series.getId()));
            GridDataFactory.fillDefaults().grab(true, false).applyTo(left);

            right = new Label(data, SWT.RIGHT);
            right.setForeground(foregroundColor);
            right.setText(valueFormat.format(value));
            GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).applyTo(right);
        }

        Object focus = getFocusedObject();
        extraInfoProvider.forEach(provider -> provider.accept(container, focus));
    }

}
