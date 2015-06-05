package name.abuchen.portfolio.ui.util.chart;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.swtchart.Chart;
import org.swtchart.IAxis;
import org.swtchart.IAxis.Position;
import org.swtchart.ILineSeries;
import org.swtchart.ISeries.SeriesType;
import org.swtchart.LineStyle;
import org.swtchart.Range;

public class ScatterChart extends Chart
{
    private static final double ZOOM_RATIO = 0.1;
    private ChartContextMenu contextMenu;

    public ScatterChart(Composite parent)
    {
        super(parent, SWT.NONE);

        Color backColor = Display.getDefault().getSystemColor(SWT.COLOR_BLACK);

        setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        getTitle().setForeground(backColor);
        getLegend().setVisible(false);

        // x axis
        IAxis xAxis = getAxisSet().getXAxis(0);
        xAxis.getTitle().setForeground(backColor);
        xAxis.getTick().setForeground(backColor);

        // y axis
        IAxis yAxis = getAxisSet().getYAxis(0);
        yAxis.getTitle().setForeground(backColor);
        yAxis.getTick().setForeground(backColor);
        yAxis.setPosition(Position.Secondary);

        new ScatterChartToolTip(this);

        ZoomMouseWheelListener.attachTo(this);
        MovePlotKeyListener.attachTo(this);
        ZoomInAreaListener.attachTo(this);

        this.contextMenu = new ChartContextMenu(this);
    }

    public ILineSeries addScatterSeries(double[] xSeries, double[] ySeries, String label)
    {
        ILineSeries scatterSeries = (ILineSeries) getSeriesSet().createSeries(SeriesType.LINE, label);
        scatterSeries.setLineStyle(LineStyle.NONE);
        scatterSeries.setXSeries(xSeries);
        scatterSeries.setYSeries(ySeries);
        scatterSeries.setAntialias(SWT.ON);
        scatterSeries.setSymbolSize(10);
        return scatterSeries;
    }

    public void adjustRange()
    {
        try
        {
            setRedraw(false);

            getAxisSet().adjustRange();
            for (IAxis axis : getAxisSet().getXAxes())
                addMargin(axis);
            for (IAxis axis : getAxisSet().getYAxes())
                addMargin(axis);
        }
        finally
        {
            setRedraw(true);
        }
    }

    private void addMargin(IAxis axis)
    {
        Range range = axis.getRange();
        double midPoint = ((range.upper - range.lower) / 2) + range.lower;
        double lower = (range.lower - 2 * ZOOM_RATIO * midPoint) / (1 - 2 * ZOOM_RATIO);
        double upper = (range.upper - 2 * ZOOM_RATIO * midPoint) / (1 - 2 * ZOOM_RATIO);
        axis.setRange(new Range(lower, upper));
    }

    public void exportMenuAboutToShow(IMenuManager manager, String label)
    {
        this.contextMenu.exportMenuAboutToShow(manager, label);
    }
}
