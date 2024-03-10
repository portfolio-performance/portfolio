package name.abuchen.portfolio.ui.util.chart;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.IAxis;
import org.eclipse.swtchart.IAxis.Position;
import org.eclipse.swtchart.IBarSeries;
import org.eclipse.swtchart.ICustomPaintListener;
import org.eclipse.swtchart.ILineSeries;
import org.eclipse.swtchart.ILineSeries.PlotSymbolType;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.swtchart.LineStyle;
import org.eclipse.swtchart.Range;

import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.Colors;

public class TimelineChart extends Chart // NOSONAR
{

    private static class MarkerLine
    {
        private LocalDate date;
        private Color color;
        private String label;
        private Double value;

        private MarkerLine(LocalDate date, Color color, String label, Double value)
        {
            this.date = date;
            this.color = color;
            this.label = label;
            this.value = value;
        }

        public long getTimeMillis()
        {
            return Date.from(date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()).getTime();
        }
    }

    private static class NonTradingDayMarker
    {
        private LocalDate date;
        private Color color;

        private NonTradingDayMarker(LocalDate date, Color color)
        {
            this.date = date;
            this.color = color;
        }

        public long getTimeMillis()
        {
            return Date.from(date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()).getTime();
        }
    }

    private List<MarkerLine> markerLines = new ArrayList<>();
    private List<NonTradingDayMarker> nonTradingDayMarkers = new ArrayList<>();
    private Map<Object, IAxis> addedAxis = new HashMap<>();

    private ChartToolsManager chartTools;
    private TimelineChartToolTip toolTip;
    private ChartContextMenu contextMenu;

    public TimelineChart(Composite parent)
    {
        super(parent, SWT.NONE);

        setData(UIConstants.CSS.CLASS_NAME, "chart"); //$NON-NLS-1$

        getLegend().setVisible(false);

        // x axis
        IAxis xAxis = getAxisSet().getXAxis(0);
        xAxis.getTitle().setVisible(false);
        xAxis.getTick().setVisible(false);
        xAxis.getGrid().setStyle(LineStyle.NONE);

        // y axis
        IAxis yAxis = getAxisSet().getYAxis(0);
        yAxis.getTitle().setVisible(false);
        yAxis.setPosition(Position.Secondary);

        // 2nd y axis
        int axisId = getAxisSet().createYAxis();
        IAxis y2Axis = getAxisSet().getYAxis(axisId);
        y2Axis.getTitle().setVisible(false);
        y2Axis.getTick().setVisible(false);
        y2Axis.getGrid().setStyle(LineStyle.NONE);
        y2Axis.setPosition(Position.Primary);

        // 3rd y axis (percentage)
        int axisId3rd = getAxisSet().createYAxis();
        IAxis y3Axis = getAxisSet().getYAxis(axisId3rd);
        y3Axis.getTitle().setVisible(false);
        y3Axis.getTick().setVisible(false);
        y3Axis.getTick().setFormat(new DecimalFormat("+#.##%;-#.##%")); //$NON-NLS-1$
        y3Axis.getGrid().setStyle(LineStyle.NONE);
        y3Axis.setPosition(Position.Primary);

        getPlotArea().addCustomPaintListener(new ICustomPaintListener()
        {
            @Override
            public void paintControl(PaintEvent e)
            {
                paintTimeGrid(e);
                paintNonTradingDayMarker(e);
            }

            @Override
            public boolean drawBehindSeries()
            {
                return true;
            }
        });

        getPlotArea().getControl().addPaintListener(this::paintMarkerLines);

        toolTip = new TimelineChartToolTip(this);

        chartTools = new ChartToolsManager(this);

        ZoomMouseWheelListener.attachTo(this);
        MovePlotKeyListener.attachTo(this);
        ZoomInAreaListener.attachTo(this);
        getPlotArea().getControl().addTraverseListener(event -> event.doit = true);

        this.contextMenu = new ChartContextMenu(this);
    }

    public void addMarkerLine(LocalDate date, Color color, String label)
    {
        addMarkerLine(date, color, label, null);
    }

    public void addMarkerLine(LocalDate date, Color color, String label, Double value)
    {
        this.markerLines.add(new MarkerLine(date, color, label, value));
        Collections.sort(this.markerLines, (ml1, ml2) -> ml1.date.compareTo(ml2.date));
    }

    public void clearMarkerLines()
    {
        this.markerLines.clear();
    }

    public void addNonTradingDayMarker(LocalDate date, Color color)
    {
        this.nonTradingDayMarkers.add(new NonTradingDayMarker(date, color));
        Collections.sort(this.nonTradingDayMarkers, (ml1, ml2) -> ml1.date.compareTo(ml2.date));
    }

    public void removeNonTradingDayMarker(LocalDate date, Color color)
    {
        this.nonTradingDayMarkers.remove(new NonTradingDayMarker(date, color));
    }

    public void clearNonTradingDayMarker()
    {
        this.nonTradingDayMarkers.clear();
    }

    public void addPlotPaintListener(PaintListener listener)
    {
        getPlotArea().addCustomPaintListener(new ICustomPaintListener()
        {
            @Override
            public void paintControl(PaintEvent e)
            {
                listener.paintControl(e);
            }

            @Override
            public boolean drawBehindSeries()
            {
                return false;
            }
        });
    }

    public ILineSeries addDateSeries(String id, LocalDate[] dates, double[] values, String label)
    {
        return addDateSeries(id, dates, values, Colors.BLACK, false, label);
    }

    public ILineSeries addDateSeries(String id, LocalDate[] dates, double[] values, Color color, String label)
    {
        return addDateSeries(id, dates, values, color, false, label);
    }

    private ILineSeries addDateSeries(String id, LocalDate[] dates, double[] values, Color color, boolean showArea,
                    String label)
    {
        ILineSeries lineSeries = (ILineSeries) getSeriesSet().createSeries(SeriesType.LINE, id);
        lineSeries.setDescription(label);
        lineSeries.setXDateSeries(toJavaUtilDate(dates));
        lineSeries.enableArea(showArea);
        lineSeries.setLineWidth(2);
        lineSeries.setSymbolType(PlotSymbolType.NONE);
        lineSeries.setYSeries(values);
        lineSeries.setLineColor(color);
        lineSeries.setAntialias(SWT.ON);
        return lineSeries;
    }

    public IBarSeries addDateBarSeries(String id, LocalDate[] dates, double[] values, String label)
    {
        IBarSeries barSeries = (IBarSeries) getSeriesSet().createSeries(SeriesType.BAR, id);
        barSeries.setDescription(label);
        barSeries.setXDateSeries(toJavaUtilDate(dates));
        barSeries.setYSeries(values);
        barSeries.setBarColor(Colors.DARK_GRAY);
        barSeries.setBarPadding(100);

        return barSeries;
    }

    public TimelineChartToolTip getToolTip()
    {
        return toolTip;
    }

    public ChartToolsManager getChartToolsManager()
    {
        return chartTools;
    }

    private void paintTimeGrid(PaintEvent e)
    {
        IAxis xAxis = getAxisSet().getXAxis(0);
        Range range = xAxis.getRange();

        ZoneId zoneId = ZoneId.systemDefault();
        LocalDate start = Instant.ofEpochMilli((long) range.lower).atZone(zoneId).toLocalDate();
        LocalDate end = Instant.ofEpochMilli((long) range.upper).atZone(zoneId).toLocalDate();

        TimeGridHelper.paintTimeGrid(this, e, start, end,
                        cursor -> xAxis.getPixelCoordinate(cursor.atStartOfDay(zoneId).toInstant().toEpochMilli()));
    }

    private void paintMarkerLines(PaintEvent e) // NOSONAR
    {
        if (markerLines.isEmpty())
            return;

        IAxis xAxis = getAxisSet().getXAxis(0);
        IAxis yAxis = getAxisSet().getYAxis(0);

        int labelExtentX = 0;
        int labelStackY = 0;

        for (MarkerLine marker : markerLines)
        {
            int x = xAxis.getPixelCoordinate(marker.getTimeMillis());

            e.gc.setLineStyle(SWT.LINE_SOLID);
            e.gc.setForeground(marker.color);
            e.gc.setLineWidth(2);
            e.gc.drawLine(x, 0, x, e.height);

            if (marker.label != null)
            {
                Point textExtent = e.gc.textExtent(marker.label);
                boolean flip = x + 5 + textExtent.x > e.width;
                int textX = flip ? x - 5 - textExtent.x : x + 5;
                labelStackY = labelExtentX > textX ? labelStackY + textExtent.y : 0;
                labelExtentX = x + 5 + textExtent.x;

                e.gc.drawText(marker.label, textX, e.height - 20 - labelStackY, true);
            }

            if (marker.value != null)
            {
                int y = yAxis.getPixelCoordinate(marker.value);
                e.gc.drawLine(x - 5, y, x + 5, y);
            }
        }
    }

    private void paintNonTradingDayMarker(PaintEvent eventNonTradingDay) // NOSONAR
    {
        if (nonTradingDayMarkers.isEmpty())
            return;
        IAxis xAxis = getAxisSet().getXAxis(0);
        Range range = xAxis.getRange();
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDate start = Instant.ofEpochMilli((long) range.lower).atZone(zoneId).toLocalDate();
        LocalDate end = Instant.ofEpochMilli((long) range.upper).atZone(zoneId).toLocalDate();
        long days = ChronoUnit.DAYS.between(start, end);
        double dayWidth = Math.ceil((eventNonTradingDay.width) / (days - 1d));
        int markerWidthXLeft = (int) (dayWidth / 2);
        ArrayList<LocalDate> nonTradingDayDates = new ArrayList<>();
        for (NonTradingDayMarker marker : nonTradingDayMarkers)
        {
            nonTradingDayDates.add(marker.date);
        }
        for (NonTradingDayMarker marker : nonTradingDayMarkers)
        {
            int x = xAxis.getPixelCoordinate(marker.getTimeMillis());
            double barWidth = 0;
            for (LocalDate date = marker.date; date.isBefore(end); date = date.plusDays(1))
            {
                if (!nonTradingDayDates.contains(date))
                    break;
                barWidth = barWidth + dayWidth;
            }
            if (nonTradingDayDates.contains(marker.date.minusDays(1)))
                continue;

            if (barWidth < 3)
                barWidth = 3;
            eventNonTradingDay.gc.setBackground(marker.color);
            eventNonTradingDay.gc.setLineWidth(0);
            eventNonTradingDay.gc.setAlpha(100);
            eventNonTradingDay.gc.fillRectangle(x - markerWidthXLeft, 25, (int) barWidth,
                            eventNonTradingDay.height - 25);
            eventNonTradingDay.gc.setBackgroundPattern(null);
            eventNonTradingDay.gc.setAlpha(255); // Dirty fix to avoid that the
                                                 // whole chart is adjusted
        }
    }

    public void exportMenuAboutToShow(IMenuManager manager, String label)
    {
        this.contextMenu.exportMenuAboutToShow(manager, label);
    }

    public static Date[] toJavaUtilDate(LocalDate[] dates)
    {
        ZoneId zoneId = ZoneId.systemDefault();
        Date[] answer = new Date[dates.length];
        for (int ii = 0; ii < answer.length; ii++)
            answer[ii] = Date.from(dates[ii].atStartOfDay().atZone(zoneId).toInstant());
        return answer;
    }

    public void resetAxes()
    {
        for (IAxis axis : getAxisSet().getAxes())
            axis.setRange(new Range(0, 1));
    }

    public void adjustRange()
    {
        try
        {
            setRedraw(false);

            getAxisSet().adjustRange();
            ChartUtil.addYMargins(this, 0.08);
        }
        finally
        {
            setRedraw(true);
        }
    }

    @Override
    public void save(String filename, int format)
    {
        ChartUtil.save(this, filename, format);
    }

    @Override
    public boolean setFocus()
    {
        return getPlotArea().getControl().setFocus();
    }

    public IAxis getOrCreateAxis(Object key, Supplier<IAxis> axisFactory)
    {
        return addedAxis.computeIfAbsent(key, x -> {
            IAxis axis = axisFactory.get();

            // As we are dynamically adding the series, we have to check whether
            // the updates on the chart are suspended. Otherwise, the layout
            // information is not available for the axis and then #adjustRange
            // will fail

            if (isUpdateSuspended())
            {
                // resuming updates will trigger #updateLayout
                suspendUpdate(false);
                suspendUpdate(true);
            }

            return axis;
        });
    }
}
