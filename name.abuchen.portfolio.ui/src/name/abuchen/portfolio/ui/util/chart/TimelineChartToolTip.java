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
import org.eclipse.swt.layout.RowData;
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
    private Map<String, Color> secondaryTriangleColors = new HashMap<>();

    private boolean categoryEnabled = false;
    private boolean reverseLabels = false;

    private Composite container;
    private Label dateValueLabel;
    private final List<Row> rows = new ArrayList<>();
    private final List<Label> extraInfoLabels = new ArrayList<>();

    private record Row(String seriesId, Color color, Label value, Format format)
    {
    }

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

    public void setSecondaryTriangleColor(String series, Color color)
    {
        this.secondaryTriangleColors.put(series, color);
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

    private List<Function<Object, List<String>>> extraInfoProvider = new ArrayList<>();

    /**
     * Adds lines that are shown below the values of the data series, i.e. the
     * transactions around the focused date. The tool tip keeps the labels it
     * needs to show them, therefore a provider only has to deliver the text.
     */
    public void addExtraInfo(Function<Object, List<String>> extraInfoProvider)
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
        rows.clear();
        extraInfoLabels.clear();
        container = null;
        dateValueLabel = null;

        container = new Composite(parent, SWT.NONE);
        // inherit the background down to every child: a control that is created
        // while the tool tip is already visible is otherwise painted in black
        // until the CSS styles are applied to it
        container.setBackgroundMode(SWT.INHERIT_FORCE);
        RowLayout layout = new RowLayout(SWT.VERTICAL);
        layout.center = true;
        container.setLayout(layout);

        Composite data = new Composite(container, SWT.NONE);
        GridLayoutFactory.swtDefaults().numColumns(2).applyTo(data);

        Label left = new Label(data, SWT.NONE);
        left.setText(categoryEnabled ? getChart().getAxisSet().getXAxis(0).getTitle().getText() : Messages.ColumnDate);

        Label right = new Label(data, SWT.NONE);
        right.setText(formatXAxisData(getFocusedObject()));
        dateValueLabel = right;

        List<Pair<ISeries<?>, Double>> values = computeValues(getChart().getSeriesSet().getSeries());

        if (reverseLabels)
            Collections.reverse(values);

        if (isAltPressed())
            Collections.sort(values, (l, r) -> r.getValue().compareTo(l.getValue()));

        for (Pair<ISeries<?>, Double> value : values)
        {
            ISeries<?> series = value.getKey();

            Color color = colorOf(series);

            ColoredLabel cl = new ColoredLabel(data, SWT.NONE);
            cl.setBackdropColor(color);

            Color secondaryColor = secondaryTriangleColors.get(series.getId());
            if (secondaryColor != null && !color.equals(secondaryColor))
                cl.setSecondaryTriangleColor(secondaryColor);

            cl.setText(series.getDescription() != null ? series.getDescription() : series.getId());
            GridDataFactory.fillDefaults().grab(true, false).applyTo(cl);

            right = new Label(data, SWT.RIGHT);
            Format valueFormat = overrideValueFormat.getOrDefault(series.getId(), defaultValueFormat);
            right.setText(valueFormat.format(value.getRight()));
            GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).applyTo(right);

            rows.add(new Row(series.getId(), color, right, valueFormat));
        }

        updateExtraInfo(container, getFocusedObject());

        Label hint = new Label(data, SWT.WRAP);
        hint.setText(MessageFormat.format(Messages.TooltipHintPressAlt,
                        Platform.OS_MACOSX.equals(Platform.getOS()) ? "Option" : "Alt")); //$NON-NLS-1$ //$NON-NLS-2$

        // the data, and not the hint, determine the width of the tool tip: with
        // a tiny width hint the label cannot influence that width. Measuring the
        // composite (instead of packing it) gets that width without laying out
        // or resizing the tool tip window while it is being built
        GridData hintData = GridDataFactory.fillDefaults().span(2, 1).hint(10, SWT.DEFAULT).create();
        hint.setLayoutData(hintData);
        hintData.widthHint = data.computeSize(SWT.DEFAULT, SWT.DEFAULT, false).x;

        hint.setFont(this.resourceManager
                        .create(FontDescriptor.createFrom(data.getFont()).increaseHeight(-3).withStyle(SWT.ITALIC)));
    }

    @Override
    protected boolean updateContent()
    {
        if (container == null || container.isDisposed() //
                        || dateValueLabel == null || dateValueLabel.isDisposed())
            return false;

        List<Pair<ISeries<?>, Double>> values = computeValues(getChart().getSeriesSet().getSeries());

        if (reverseLabels)
            Collections.reverse(values);

        if (isAltPressed())
            Collections.sort(values, (l, r) -> r.getValue().compareTo(l.getValue()));

        // an update in place needs exactly the same rows, i.e. the same series
        // in the same order and with the same color - otherwise the controls
        // of the tool tip have to be re-created

        if (values.size() != rows.size())
            return false;

        for (int i = 0; i < values.size(); i++)
        {
            ISeries<?> series = values.get(i).getKey();

            if (!series.getId().equals(rows.get(i).seriesId()) || !colorOf(series).equals(rows.get(i).color()))
                return false;
        }

        dateValueLabel.setText(formatXAxisData(getFocusedObject()));

        for (int i = 0; i < values.size(); i++)
            rows.get(i).value().setText(rows.get(i).format().format(values.get(i).getValue()));

        updateExtraInfo(container, getFocusedObject());

        return true;
    }

    /**
     * Shows the extra information of the focused date, re-using the labels of
     * the previous date: a control created while the tool tip is visible is not
     * painted before the next paint cycle.
     */
    private void updateExtraInfo(Composite container, Object focus)
    {
        List<String> lines = new ArrayList<>();
        extraInfoProvider.forEach(provider -> lines.addAll(provider.apply(focus)));

        while (extraInfoLabels.size() < lines.size())
            extraInfoLabels.add(new Label(container, SWT.NONE));

        for (int i = 0; i < extraInfoLabels.size(); i++)
        {
            Label label = extraInfoLabels.get(i);
            boolean show = i < lines.size();

            if (show)
                label.setText(lines.get(i));

            // a row layout keeps the space of an invisible child - the label is
            // only taken out of the layout if its layout data is excluded,
            // otherwise a longer list of a previous date would keep its height
            RowData layoutData = (RowData) label.getLayoutData();
            if (layoutData == null)
            {
                layoutData = new RowData();
                label.setLayoutData(layoutData);
            }
            layoutData.exclude = !show;

            label.setVisible(show);
        }
    }

    private Color colorOf(ISeries<?> series)
    {
        return series instanceof ILineSeries<?> lineSeries ? lineSeries.getLineColor()
                        : ((IBarSeries<?>) series).getBarColor();
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
