package name.abuchen.portfolio.ui.util.chart;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.IAxis;
import org.eclipse.swtchart.IAxis.Position;
import org.eclipse.swtchart.ICustomPaintListener;
import org.eclipse.swtchart.ILineSeries;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.swtchart.internal.PlotArea;
import org.eclipse.swtchart.LineStyle;

import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.Colors;

public class ScatterChart extends Chart // NOSONAR
{
    private ChartContextMenu contextMenu;
    private Color highlightColor = Colors.BLACK;

    @SuppressWarnings("restriction")
    public ScatterChart(Composite parent)
    {
        super(parent, SWT.NONE, null);

        // we must use the secondary constructor that is not creating the
        // PlotArea because the default constructor adds a mouse move listener
        // that is redrawing the chart on every mouse move. That leads to janky
        // UI when the tooltip is shown.
        new PlotArea(this, SWT.NONE);

        setData(UIConstants.CSS.CLASS_NAME, "chart"); //$NON-NLS-1$

        getLegend().setVisible(false);

        // y axis
        IAxis yAxis = getAxisSet().getYAxis(0);
        yAxis.setPosition(Position.Secondary);

        getPlotArea().addCustomPaintListener(new ICustomPaintListener()
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
        getPlotArea().getControl().addTraverseListener(event -> event.doit = true);

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

    public ILineSeries<?> addScatterSeries(String id, double[] xSeries, double[] ySeries, String label)
    {
        var scatterSeries = (ILineSeries<?>) getSeriesSet().createSeries(SeriesType.LINE, id);
        scatterSeries.setDescription(label);
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

    @Override
    public boolean setFocus()
    {
        return getPlotArea().getControl().setFocus();
    }
}
