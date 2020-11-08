package name.abuchen.portfolio.ui.util.chart;

import java.text.DecimalFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.swtchart.Chart;
import org.swtchart.IAxis;
import org.swtchart.IBarSeries;
import org.swtchart.ILineSeries;
import org.swtchart.ISeries;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.swt.ColoredLabel;
import name.abuchen.portfolio.util.Pair;
import name.abuchen.portfolio.util.TextUtil;

public class TimelineChartToolTip extends AbstractChartToolTip
{
    private LocalResourceManager resourceManager;

    private Function<Object, String> xAxisFormat;

    private DecimalFormat valueFormat = new DecimalFormat("#,##0.00"); //$NON-NLS-1$

    private boolean categoryEnabled = false;
    private boolean reverseLabels = false;

    /**
     * If given, the tool tip is shown only for dates that are present in the
     * given data series id. That is primarily used for the quote chart because
     * for weekends there are no data points and the tool tip would start
     * flickering.
     */
    private String showToolTipOnlyForDatesInThisDataSeries = null;

    private Set<String> excludeFromTooltip = new HashSet<>();

    public TimelineChartToolTip(Chart chart)
    {
        super(chart);

        this.resourceManager = new LocalResourceManager(JFaceResources.getResources(), chart);
    }

    public void enableCategory(boolean enabled)
    {
        categoryEnabled = enabled;
    }

    public void reverseLabels(boolean reverseLabels)
    {
        this.reverseLabels = reverseLabels;
    }

    public void setXAxisFormat(Function<Object, String> format)
    {
        this.xAxisFormat = format;
    }

    public void setValueFormat(DecimalFormat valueFormat)
    {
        this.valueFormat = valueFormat;
    }

    /**
     * Add a series id which is not displayed in the tool tip.
     */
    public void addSeriesExclude(String seriesId)
    {
        this.excludeFromTooltip.add(seriesId);
    }

    /**
     * Sets data series for which to show tool tips only.
     */
    public void showToolTipOnlyForDatesInDataSeries(String seriesId)
    {
        this.showToolTipOnlyForDatesInThisDataSeries = seriesId;
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

        if (showToolTipOnlyForDatesInThisDataSeries == null)
            return cal.getTime();

        ISeries timeSeries = getChart().getSeriesSet().getSeries(showToolTipOnlyForDatesInThisDataSeries);
        if (timeSeries == null)
            return cal.getTime();

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
        RowLayout layout = new RowLayout(SWT.VERTICAL);
        layout.center = true;
        container.setLayout(layout);

        Composite data = new Composite(container, SWT.NONE);
        GridLayoutFactory.swtDefaults().numColumns(2).applyTo(data);

        Label left = new Label(data, SWT.NONE);
        left.setText(categoryEnabled ? getChart().getAxisSet().getXAxis(0).getTitle().getText() : Messages.ColumnDate);

        Label right = new Label(data, SWT.NONE);
        right.setText(formatXAxisData(getFocusedObject()));

        List<Pair<ISeries, Double>> values = computeValues(getChart().getSeriesSet().getSeries());

        if (reverseLabels)
            Collections.reverse(values);

        if (isAltPressed())
            Collections.sort(values, (l, r) -> r.getValue().compareTo(l.getValue()));

        for (Pair<ISeries, Double> value : values)
        {
            ISeries series = value.getKey();

            Color color = series instanceof ILineSeries ? ((ILineSeries) series).getLineColor()
                            : ((IBarSeries) series).getBarColor();

            ColoredLabel cl = new ColoredLabel(data, SWT.NONE);
            cl.setBackdropColor(color);
            cl.setText(TextUtil.tooltip(series.getId()));
            GridDataFactory.fillDefaults().grab(true, false).applyTo(cl);

            right = new Label(data, SWT.RIGHT);
            right.setText(valueFormat.format(value.getRight()));
            GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).applyTo(right);
        }

        Object focus = getFocusedObject();
        extraInfoProvider.forEach(provider -> provider.accept(container, focus));

        Label hint = new Label(data, SWT.NONE);
        hint.setText(Messages.TooltipHintPressAlt);
        hint.setFont(this.resourceManager.createFont(
                        FontDescriptor.createFrom(data.getFont()).increaseHeight(-3).withStyle(SWT.ITALIC)));
        GridDataFactory.fillDefaults().span(2, 1).applyTo(hint);
    }

    private List<Pair<ISeries, Double>> computeValues(ISeries[] allSeries)
    {
        List<Pair<ISeries, Double>> values = new ArrayList<>();

        for (ISeries series : allSeries) // NOSONAR
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

            values.add(new Pair<>(series, value));
        }

        return values;
    }

    private String formatXAxisData(Object obj)
    {
        if (xAxisFormat != null)
            return xAxisFormat.apply(obj);
        else if (categoryEnabled && obj instanceof Integer)
            return getChart().getAxisSet().getXAxis(0).getCategorySeries()[(Integer) obj];
        else if (obj instanceof Date)
            return Values.Date.format(((Date) obj).toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        else
            return String.valueOf(obj);
    }

}
