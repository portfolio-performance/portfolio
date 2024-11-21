package name.abuchen.portfolio.ui.util.chart;

import java.text.DecimalFormat;
import java.text.Format;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.IAxis;
import org.eclipse.swtchart.IBarSeries;
import org.eclipse.swtchart.ILineSeries;
import org.eclipse.swtchart.ISeries;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.swt.ColoredLabel;
import name.abuchen.portfolio.util.Pair;

public class TimelineChartToolTip extends AbstractChartToolTip
{
    private LocalResourceManager resourceManager;

    private Function<Object, String> xAxisFormat;

    private Format defaultValueFormat = new DecimalFormat("#,##0.00"); //$NON-NLS-1$
    private Map<String, Format> overrideValueFormat = new HashMap<>();

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

    public void setDefaultValueFormat(Format defaultValueFormat)
    {
        this.defaultValueFormat = defaultValueFormat;
    }

    public Format getDefaultValueFormat()
    {
        return this.defaultValueFormat;
    }

    public void overrideValueFormat(String series, Format valueFormat)
    {
        this.overrideValueFormat.put(series, valueFormat);
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
    protected final Object getFocusObjectAt(Event event)
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

    private LocalDate getFocusDateAt(Event event)
    {
        IAxis xAxis = getChart().getAxisSet().getXAxes()[0];

        long epochDay = (long) xAxis.getDataCoordinate(event.x);

        var date = LocalDate.ofEpochDay(epochDay);

        if (showToolTipOnlyForDatesInThisDataSeries == null)
            return date;

        ISeries<?> timeSeries = getChart().getSeriesSet().getSeries(showToolTipOnlyForDatesInThisDataSeries);
        if (timeSeries == null)
            return date;

        var dataModel = (TimelineSeriesModel) timeSeries.getDataModel();
        var xDateSeries = dataModel.getXDateSeries();

        int line = Arrays.binarySearch(xDateSeries, date);

        if (line >= 0)
            return date;

        // otherwise: find closest existing date
        line = -line - 1;

        if (line == 0)
            return xDateSeries[line];

        int length = xDateSeries.length;
        if (line >= length)
            return xDateSeries[length - 1];

        // check which date is closer to the targeted date
        var left = xDateSeries[line - 1];
        var right = xDateSeries[line];

        return epochDay - left.toEpochDay() < right.toEpochDay() - epochDay ? left : right;
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

        List<Pair<ISeries<?>, Double>> values = computeValues(getChart().getSeriesSet().getSeries());

        if (reverseLabels)
            Collections.reverse(values);

        if (isAltPressed())
            Collections.sort(values, (l, r) -> r.getValue().compareTo(l.getValue()));

        for (Pair<ISeries<?>, Double> value : values)
        {
            ISeries<?> series = value.getKey();

            Color color = series instanceof ILineSeries<?> lineSeries ? lineSeries.getLineColor()
                            : ((IBarSeries<?>) series).getBarColor();

            ColoredLabel cl = new ColoredLabel(data, SWT.NONE);
            cl.setBackdropColor(color);
            cl.setText(series.getDescription() != null ? series.getDescription() : series.getId());
            GridDataFactory.fillDefaults().grab(true, false).applyTo(cl);

            right = new Label(data, SWT.RIGHT);
            Format valueFormat = overrideValueFormat.getOrDefault(series.getId(), defaultValueFormat);
            right.setText(valueFormat.format(value.getRight()));
            GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).applyTo(right);
        }

        Object focus = getFocusedObject();
        extraInfoProvider.forEach(provider -> provider.accept(container, focus));

        Label hint = new Label(data, SWT.WRAP);
        hint.setText(MessageFormat.format(Messages.TooltipHintPressAlt,
                        Platform.OS_MACOSX.equals(Platform.getOS()) ? "Option" : "Alt")); //$NON-NLS-1$ //$NON-NLS-2$
        // first set a small width and then update later
        GridData hintData = GridDataFactory.fillDefaults().span(2, 1).hint(10, SWT.DEFAULT).span(2, 1).create();
        hint.setLayoutData(hintData);

        hint.getParent().pack();
        hintData.widthHint = hint.getBounds().width;
        hint.getParent().pack();
        hint.setFont(this.resourceManager
                        .create(FontDescriptor.createFrom(data.getFont()).increaseHeight(-3).withStyle(SWT.ITALIC)));
    }

    private List<Pair<ISeries<?>, Double>> computeValues(ISeries<?>[] allSeries)
    {
        List<Pair<ISeries<?>, Double>> values = new ArrayList<>();

        var focusedObject = getFocusedObject();

        for (ISeries<?> series : allSeries) // NOSONAR
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
                var dataModel = (TimelineSeriesModel) series.getDataModel();
                var xData = dataModel.getXDateSeries();

                int line = Arrays.binarySearch(xData, focusedObject);
                if (line >= 0)
                {
                    // user hit the pixel, show value
                    value = series.getYSeries()[line];
                }
                else if (line == -1 || line == -xData.length - 1)
                {
                    // pixel is before or after data series, show nothing
                    continue;
                }
                else
                {
                    value = series.getYSeries()[Math.max(0, -line - 2)];
                }
            }

            values.add(new Pair<>(series, value));
        }

        return values;
    }

    private String formatXAxisData(Object obj)
    {
        if (xAxisFormat != null)
            return xAxisFormat.apply(obj);
        else if (categoryEnabled && obj instanceof Integer integer)
            return getChart().getAxisSet().getXAxis(0).getCategorySeries()[integer];
        else if (obj instanceof LocalDate date)
            return Values.Date.format(date);
        else
            return String.valueOf(obj);
    }

}
