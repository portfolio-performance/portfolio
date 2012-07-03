package name.abuchen.portfolio.ui.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
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
    private final LocalResourceManager resources;

    public TimelineChart(Composite parent)
    {
        super(parent, SWT.NONE);

        resources = new LocalResourceManager(JFaceResources.getResources(), this);

        setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        getTitle().setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
        getLegend().setVisible(false);

        // x axis
        final IAxis xAxis = getAxisSet().getXAxis(0);
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
                if (markerLines.isEmpty())
                    return;

                for (MarkerLine marker : markerLines)
                {
                    int y = xAxis.getPixelCoordinate((double) marker.date.getTime());
                    e.gc.setLineStyle(SWT.LINE_SOLID);
                    e.gc.setForeground(resources.createColor(marker.color));
                    e.gc.setLineWidth(2);
                    e.gc.drawLine(y, 0, y, e.height);
                    e.gc.drawText(marker.label, y + 5, e.height - 20, true);
                }
            }
        });

    }

    public void addMarkerLine(Date date, RGB color, String label)
    {
        this.markerLines.add(new MarkerLine(date, color, label));
    }

    public void clearMarkerLines()
    {
        this.markerLines.clear();
    }

    public void addDateSeries(Date[] dates, double[] values, Colors color)
    {
        addDateSeries(dates, values, color, false, color.name());
    }

    public void addDateSeries(Date[] dates, double[] values, Colors color, String label)
    {
        addDateSeries(dates, values, color, false, label);
    }

    public void addDateSeries(Date[] dates, double[] values, Colors color, boolean showArea)
    {
        addDateSeries(dates, values, color, showArea, color.name());
    }

    public void addDateSeries(Date[] dates, double[] values, Colors color, boolean showArea, String label)
    {
        ILineSeries lineSeries = (ILineSeries) getSeriesSet().createSeries(SeriesType.LINE, label);
        lineSeries.setXDateSeries(dates);
        lineSeries.enableArea(showArea);
        lineSeries.setLineWidth(2);
        lineSeries.setSymbolType(PlotSymbolType.NONE);
        lineSeries.setYSeries(values);
        lineSeries.setLineColor(resources.createColor(color.swt()));
        lineSeries.setAntialias(SWT.ON);
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

}
