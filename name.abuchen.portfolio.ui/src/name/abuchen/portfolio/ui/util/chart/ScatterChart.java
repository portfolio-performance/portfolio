package name.abuchen.portfolio.ui.util.chart;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.swtchart.Chart;
import org.swtchart.IAxis;
import org.swtchart.IAxis.Position;
import org.swtchart.ICustomPaintListener;
import org.swtchart.ILineSeries;
import org.swtchart.IPlotArea;
import org.swtchart.ISeries.SeriesType;
import org.swtchart.LineStyle;

import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.Colors;

public class ScatterChart extends Chart // NOSONAR
{
    private ChartContextMenu contextMenu;
    private Color highlightColor = Colors.BLACK;

    public ScatterChart(Composite parent)
    {
        super(parent, SWT.NONE);

        setData(UIConstants.CSS.CLASS_NAME, "chart"); //$NON-NLS-1$

        getLegend().setVisible(false);

        // y axis
        IAxis yAxis = getAxisSet().getYAxis(0);
        yAxis.setPosition(Position.Secondary);

        ((IPlotArea) getPlotArea()).addCustomPaintListener(new ICustomPaintListener()
        {
            @Override
            public void paintControl(PaintEvent e)
            {
                Color oldForebround = e.gc.getForeground();
                e.gc.setForeground(highlightColor);

                IAxis xAxis = getAxisSet().getXAxes()[0];
                int y = xAxis.getPixelCoordinate(0);
                e.gc.drawLine(y, 0, y, e.height);

                IAxis yAxis = getAxisSet().getYAxes()[0];
                int x = yAxis.getPixelCoordinate(0);
                e.gc.drawLine(0, x, e.width, x);

                e.gc.setForeground(oldForebround);
            }

            @Override
            public boolean drawBehindSeries()
            {
                return true;
            }
        });

        new ScatterChartToolTip(this);

        ZoomMouseWheelListener.attachTo(this);
        MovePlotKeyListener.attachTo(this);
        ZoomInAreaListener.attachTo(this);
        getPlotArea().addTraverseListener(event -> event.doit = true);

        this.contextMenu = new ChartContextMenu(this);
    }

    public Color getHighlightColor()
    {
        return highlightColor;
    }

    public void setHighlightColor(Color color)
    {
        this.highlightColor = color;
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
            ChartUtil.addMargins(this, 0.1);
        }
        finally
        {
            setRedraw(true);
        }
    }

    public void exportMenuAboutToShow(IMenuManager manager, String label)
    {
        this.contextMenu.exportMenuAboutToShow(manager, label);
    }

    @Override
    public void save(String filename, int format)
    {
        ChartUtil.save(this, filename, format);
    }
}
