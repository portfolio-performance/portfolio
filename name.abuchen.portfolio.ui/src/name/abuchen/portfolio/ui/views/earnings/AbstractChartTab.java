package name.abuchen.portfolio.ui.views.earnings;

import java.text.DateFormatSymbols;
import java.util.Arrays;

import javax.inject.Inject;

import org.eclipse.jface.resource.ColorDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.swtchart.Chart;
import org.swtchart.IAxis;
import org.swtchart.IAxis.Position;
import org.swtchart.ICustomPaintListener;
import org.swtchart.IPlotArea;
import org.swtchart.ISeries;
import org.swtchart.Range;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.chart.TimelineChartToolTip;
import name.abuchen.portfolio.ui.util.chart.TimelineChart.ThousandsNumberFormat;

public abstract class AbstractChartTab implements EarningsTab
{
    private class PaintBehindListener implements ICustomPaintListener
    {
        @Override
        public void paintControl(PaintEvent e)
        {
            int y = chart.getAxisSet().getYAxis(0).getPixelCoordinate(0);
            e.gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
            e.gc.setLineStyle(SWT.LINE_SOLID);
            e.gc.drawLine(0, y, e.width, y);
        }

        @Override
        public boolean drawBehindSeries()
        {
            return true;
        }
    }

    private static final int[][] COLORS = new int[][] { //
                    new int[] { 140, 86, 75 }, //
                    new int[] { 227, 119, 194 }, //
                    new int[] { 127, 127, 127 }, //
                    new int[] { 188, 189, 34 }, //
                    new int[] { 23, 190, 207 }, //
                    new int[] { 114, 124, 201 }, //
                    new int[] { 250, 115, 92 }, //
                    new int[] { 253, 182, 103 }, //
                    new int[] { 143, 207, 112 }, //
                    new int[] { 87, 207, 253 }, //
                    new int[] { 31, 119, 180 }, //
                    new int[] { 255, 127, 14 }, //
                    new int[] { 44, 160, 44 }, //
                    new int[] { 214, 39, 40 }, //
                    new int[] { 148, 103, 189 } }; //

    @Inject
    protected EarningsViewModel model;

    private LocalResourceManager resources;
    private Chart chart;

    protected abstract void createSeries();

    protected Chart getChart()
    {
        return chart;
    }

    protected LocalResourceManager getResources()
    {
        return resources;
    }

    @Override
    public Control createControl(Composite parent)
    {
        resources = new LocalResourceManager(JFaceResources.getResources(), parent);

        chart = new Chart(parent, SWT.NONE);
        chart.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        chart.getTitle().setVisible(false);
        chart.getLegend().setPosition(SWT.BOTTOM);
        chart.getPlotArea().addPaintListener(new PaintBehindListener());

        IAxis xAxis = chart.getAxisSet().getXAxis(0);
        xAxis.getTitle().setVisible(false);
        xAxis.getTick().setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
        xAxis.getTitle().setText(Messages.ColumnMonth);

        IAxis yAxis = chart.getAxisSet().getYAxis(0);
        yAxis.getTitle().setVisible(false);
        yAxis.getTick().setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
        yAxis.setPosition(Position.Secondary);
        
        xAxis.enableCategory(true);

        // add paint listeners
        IPlotArea plotArea = (IPlotArea) chart.getPlotArea();
        plotArea.addCustomPaintListener(new PaintBehindListener());

        // format symbols returns 13 values as some calendars have 13 months
        xAxis.setCategorySeries(Arrays.copyOfRange(new DateFormatSymbols().getMonths(), 0, 12));

        createSeries();

        chart.getAxisSet().adjustRange();

        // if max/min value of range is more than 1000, formatting is #.#k
        Range r = yAxis.getRange();
        if (r.lower < -1000.0 || r.upper > 1000.0)
        {
            yAxis.getTick().setFormat(new ThousandsNumberFormat());
        }

        attachTooltipTo(chart);

        model.addUpdateListener(this::updateChart);

        return chart;
    }

    protected void attachTooltipTo(Chart chart)
    {
        TimelineChartToolTip toolTip = new TimelineChartToolTip(chart);
        toolTip.enableCategory(true);
    }

    protected Color getColor(int year)
    {
        RGB rgb = new RGB(COLORS[year % COLORS.length][0], //
                        COLORS[year % COLORS.length][1], //
                        COLORS[year % COLORS.length][2]);
        return resources.createColor(ColorDescriptor.createFrom(rgb));
    }

    private void updateChart()
    {
        try
        {
            chart.suspendUpdate(true);
            for (ISeries s : chart.getSeriesSet().getSeries())
                chart.getSeriesSet().deleteSeries(s.getId());

            createSeries();

            chart.getAxisSet().adjustRange();
        }
        finally
        {
            chart.suspendUpdate(false);
        }
        chart.redraw();
    }

}
