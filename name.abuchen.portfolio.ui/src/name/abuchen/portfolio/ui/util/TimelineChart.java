package name.abuchen.portfolio.ui.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.swtchart.Chart;
import org.swtchart.IAxis;
import org.swtchart.IAxis.Position;
import org.swtchart.IBarSeries;
import org.swtchart.ICustomPaintListener;
import org.swtchart.ILineSeries;
import org.swtchart.ILineSeries.PlotSymbolType;
import org.swtchart.IPlotArea;
import org.swtchart.ISeries.SeriesType;
import org.swtchart.LineStyle;
import org.swtchart.Range;

public class TimelineChart extends Chart
{
    private static class MarkerLine
    {
        private Date date;
        private RGB color;
        private String label;

        private MarkerLine(Date date, RGB color, String label)
        {
            this.date = date;
            this.color = color;
            this.label = label;
        }
    }

    private List<MarkerLine> markerLines = new ArrayList<MarkerLine>();
    private TimelineChartToolTip toolTip;
    private final LocalResourceManager resources;

    public TimelineChart(Composite parent)
    {
        super(parent, SWT.NONE);

        resources = new LocalResourceManager(JFaceResources.getResources(), this);

        setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        getTitle().setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
        getLegend().setVisible(false);

        // x axis
        IAxis xAxis = getAxisSet().getXAxis(0);
        xAxis.getTitle().setVisible(false);
        xAxis.getTick().setVisible(false);
        xAxis.getGrid().setStyle(LineStyle.NONE);

        // y axis
        IAxis yAxis = getAxisSet().getYAxis(0);
        yAxis.getTitle().setVisible(false);
        yAxis.getTick().setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
        yAxis.setPosition(Position.Secondary);

        ((IPlotArea) getPlotArea()).addCustomPaintListener(new ICustomPaintListener()
        {
            @Override
            public void paintControl(PaintEvent e)
            {
                paintTimeGrid(e);
            }

            @Override
            public boolean drawBehindSeries()
            {
                return true;
            }
        });

        getPlotArea().addPaintListener(new PaintListener()
        {
            @Override
            public void paintControl(PaintEvent e)
            {
                paintMarkerLines(e);
            }
        });

        toolTip = new TimelineChartToolTip(this);
    }

    public void addMarkerLine(Date date, RGB color, String label)
    {
        this.markerLines.add(new MarkerLine(date, color, label));
        Collections.sort(this.markerLines, new Comparator<MarkerLine>()
        {
            @Override
            public int compare(MarkerLine ml1, MarkerLine ml2)
            {
                return ml1.date.compareTo(ml2.date);
            }
        });
    }

    public void clearMarkerLines()
    {
        this.markerLines.clear();
    }

    public ILineSeries addDateSeries(Date[] dates, double[] values, String label)
    {
        return addDateSeries(dates, values, Display.getDefault().getSystemColor(SWT.COLOR_BLACK), false, label);
    }

    public ILineSeries addDateSeries(Date[] dates, double[] values, Colors color, String label)
    {
        return addDateSeries(dates, values, resources.createColor(color.swt()), false, label);
    }

    public ILineSeries addDateSeries(Date[] dates, double[] values, Color color, String label)
    {
        return addDateSeries(dates, values, color, false, label);
    }

    public void addDateSeries(Date[] dates, double[] values, Colors color, boolean showArea)
    {
        addDateSeries(dates, values, resources.createColor(color.swt()), showArea, color.name());
    }

    private ILineSeries addDateSeries(Date[] dates, double[] values, Color color, boolean showArea, String label)
    {
        ILineSeries lineSeries = (ILineSeries) getSeriesSet().createSeries(SeriesType.LINE, label);
        lineSeries.setXDateSeries(dates);
        lineSeries.enableArea(showArea);
        lineSeries.setLineWidth(2);
        lineSeries.setSymbolType(PlotSymbolType.NONE);
        lineSeries.setYSeries(values);
        lineSeries.setLineColor(color);
        lineSeries.setAntialias(SWT.ON);
        return lineSeries;
    }

    public IBarSeries addDateBarSeries(Date[] dates, double[] values, String label)
    {
        IBarSeries barSeries = (IBarSeries) getSeriesSet().createSeries(SeriesType.BAR, label);
        barSeries.setXDateSeries(dates);
        barSeries.setYSeries(values);
        barSeries.setBarColor(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY));
        barSeries.setBarPadding(100);

        return barSeries;
    }

    public TimelineChartToolTip getToolTip()
    {
        return toolTip;
    }

    private void paintTimeGrid(PaintEvent e)
    {
        IAxis xAxis = getAxisSet().getXAxis(0);
        Range range = xAxis.getRange();
        Date upper = new Date((long) range.upper);

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date((long) range.lower));
        cal.add(Calendar.YEAR, 1);
        cal.set(Calendar.MONTH, Calendar.JANUARY);
        cal.set(Calendar.DAY_OF_MONTH, 1);

        while (cal.getTimeInMillis() < upper.getTime())
        {
            int y = xAxis.getPixelCoordinate((double) cal.getTimeInMillis());
            e.gc.drawLine(y, 0, y, e.height);
            e.gc.drawText(String.valueOf(cal.get(Calendar.YEAR)), y + 5, 5);

            cal.add(Calendar.YEAR, 1);
        }
    }

    private void paintMarkerLines(PaintEvent e)
    {
        if (markerLines.isEmpty())
            return;

        IAxis xAxis = getAxisSet().getXAxis(0);

        int labelExtentX = 0;
        int labelStackY = 0;

        for (MarkerLine marker : markerLines)
        {
            int x = xAxis.getPixelCoordinate((double) marker.date.getTime());

            Point textExtent = e.gc.textExtent(marker.label);
            boolean flip = x + 5 + textExtent.x > e.width;
            int textX = flip ? x - 5 - textExtent.x : x + 5;
            labelStackY = labelExtentX > textX ? labelStackY + textExtent.y : 0;
            labelExtentX = x + 5 + textExtent.x;

            e.gc.setLineStyle(SWT.LINE_SOLID);
            e.gc.setForeground(resources.createColor(marker.color));
            e.gc.setLineWidth(2);
            e.gc.drawLine(x, 0, x, e.height);
            e.gc.drawText(marker.label, textX, e.height - 20 - labelStackY, true);
        }
    }
}
