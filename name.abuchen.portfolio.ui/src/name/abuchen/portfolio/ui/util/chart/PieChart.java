package name.abuchen.portfolio.ui.util.chart;

import java.util.List;

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.IAxis;
import org.eclipse.swtchart.ICircularSeries;
import org.eclipse.swtchart.ICustomPaintListener;
import org.eclipse.swtchart.ISeries;
import org.eclipse.swtchart.model.Node;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.Colors;

public class PieChart extends Chart // NOSONAR
{
    private PieChartToolTip tooltip;
    
    public PieChart(Composite parent)
    {
        super(parent, SWT.NONE);

        setData(UIConstants.CSS.CLASS_NAME, "chart"); //$NON-NLS-1$ 

        addListener(SWT.Paint, new Listener()
        {
            @Override
            public void handleEvent(Event event)
            {
                // Set color in root node to background color
                if (getSeriesSet().getSeries().length > 0) {
                    ICircularSeries<?> cs = (ICircularSeries<?>) getSeriesSet().getSeries()[0];
                    cs.getRootNode().setColor(getPlotArea().getBackground());
                }
            }
        });

        getPlotArea().addCustomPaintListener(new ICustomPaintListener()
        {
            @Override
            public void paintControl(PaintEvent e)
            {
                renderLabels(e);
            }
        });

        getLegend().setVisible(true);
        tooltip = new PieChartToolTip(this);
    }

    public PieChartToolTip getToolTip()
    {
        return tooltip;
    }

    protected void renderLabels(PaintEvent e)
    {
        for(ISeries<?> series : getSeriesSet().getSeries()) {
            if(series instanceof ICircularSeries) {
                IAxis xAxis = (IAxis)getAxisSet().getXAxis(series.getXAxisId());
                IAxis yAxis = (IAxis)getAxisSet().getYAxis(series.getYAxisId());

                List<Node> nodes = ((ICircularSeries<?>)series).getRootNode().getChildren();
                if (!nodes.isEmpty()) {
                    for(Node node : nodes) {
                        renderNodeLabel(node, e.gc, xAxis, yAxis);
                    }
                }
            }
        }
        
    }

    protected void renderNodeLabel(Node node, GC gc, IAxis xAxis, IAxis yAxis)
    {
        int level = 2; // fixed for first doughnut level

        Font oldFont = gc.getFont();
        gc.setForeground(Colors.WHITE);

        int angleStart = node.getAngleBounds().x,
            angleWidth = (int) (node.getAngleBounds().y * 0.5);

        Point outerEnd = calcPixelCoord(xAxis, yAxis, level * 0.85, angleStart+angleWidth);

        FontDescriptor boldDescriptor = FontDescriptor.createFrom(gc.getFont()).setHeight(9);
        gc.setFont(boldDescriptor.createFont(getDisplay()));

        double percent =  ((double)node.getAngleBounds().y / 360);
        if (percent > 0.025) {
            String percentString = Values.Percent2.format(percent);
            Point textSize = gc.textExtent(percentString);
            gc.drawString(percentString, outerEnd.x - (textSize.x/2) , outerEnd.y - (textSize.y/2), true);
        }

        gc.setFont(oldFont);
    }

    private Point calcPixelCoord(IAxis xAxis, IAxis yAxis, double level, int angle)
    {
        double xCoordinate = level * Math.cos(Math.toRadians(angle));
        double yCoordinate = level * Math.sin(Math.toRadians(angle));
        int xPixelCoordinate = xAxis.getPixelCoordinate(xCoordinate);
        int yPixelCoordinate = yAxis.getPixelCoordinate(yCoordinate);
        return new Point(xPixelCoordinate, yPixelCoordinate);
    }
}
