package name.abuchen.portfolio.ui.util.chart;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.swtchart.Chart;
import org.swtchart.IAxis;
import org.swtchart.IAxis.Position;
import org.swtchart.ICustomPaintListener;
import org.swtchart.ILineSeries;
import org.swtchart.ILineSeries.PlotSymbolType;
import org.swtchart.IPlotArea;
import org.swtchart.ISeries.SeriesType;
import org.swtchart.LineStyle;
import org.swtchart.Range;

import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.util.Dates;

public class StackedTimelineChart extends Chart // NOSONAR
{
    private TimelineChartToolTip toolTip;

    private List<LocalDate> dates;

    public StackedTimelineChart(Composite parent, List<LocalDate> dates)
    {
        super(parent, SWT.NONE);

        setData(UIConstants.CSS.CLASS_NAME, "chart"); //$NON-NLS-1$

        this.dates = dates;

        getLegend().setVisible(false);

        // x axis
        IAxis xAxis = getAxisSet().getXAxis(0);
        xAxis.getTitle().setVisible(false);
        xAxis.getTick().setVisible(false);
        xAxis.getGrid().setStyle(LineStyle.NONE);

        String[] categories = new String[dates.size()];
        for (int ii = 0; ii < categories.length; ii++)
            categories[ii] = dates.get(ii).toString();
        xAxis.setCategorySeries(categories);
        xAxis.enableCategory(true);

        // y axis
        IAxis yAxis = getAxisSet().getYAxis(0);
        yAxis.getTitle().setVisible(false);
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

        toolTip = new TimelineChartToolTip(this);
        toolTip.enableCategory(true);
        toolTip.reverseLabels(true);
        toolTip.setValueFormat(new DecimalFormat("#0.0%")); //$NON-NLS-1$

        new ChartContextMenu(this);
    }

    public ILineSeries addSeries(String label, double[] values, Color color)
    {
        ILineSeries series = (ILineSeries) getSeriesSet().createSeries(SeriesType.LINE, label);
        series.setYSeries(values);

        series.setLineWidth(2);
        series.setSymbolType(PlotSymbolType.NONE);
        series.setAntialias(SWT.ON);

        series.enableStack(true);
        series.enableArea(true);
        series.setLineColor(color);

        return series;
    }

    public TimelineChartToolTip getToolTip()
    {
        return toolTip;
    }

    private void paintTimeGrid(PaintEvent e)
    {
        IAxis xAxis = getAxisSet().getXAxis(0);
        Range range = xAxis.getRange();

        final LocalDate start = dates.get(0);
        final LocalDate end = dates.get(dates.size() - 1);

        int totalDays = Dates.daysBetween(start, end) + 1;

        e.gc.setForeground(getAxisSet().getAxes()[0].getGrid().getForeground());

        LocalDate current = start.plusYears(1).withDayOfYear(1);
        while (current.isBefore(end))
        {
            int days = Dates.daysBetween(start, current);
            int y = xAxis.getPixelCoordinate((double) days * range.upper / (double) totalDays);
            e.gc.drawLine(y, 0, y, e.height);
            e.gc.drawText(String.valueOf(current.getYear()), y + 5, 5);

            current = current.plusYears(1);
        }
    }

    @Override
    public void save(String filename, int format)
    {
        ChartUtil.save(this, filename, format);
    }
}
