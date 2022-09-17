package name.abuchen.portfolio.ui.util.chart;

import java.util.List;

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.IAxis;
import org.eclipse.swtchart.ICircularSeries;
import org.eclipse.swtchart.ISeries;
import org.eclipse.swtchart.model.Node;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.views.IPieChart;

public class PieChart extends Chart
{
    private enum Orientation
    {
        X_AXIS, Y_AXIS
    }

    private static final int ID_PRIMARY_X_AXIS = 0;
    private static final int ID_PRIMARY_Y_AXIS = 0;

    private PieChartToolTip tooltip;
    private IPieChart.ChartType chartType;
    private ILabelProvider labelProvider;

    public PieChart(Composite parent, IPieChart.ChartType chartType)
    {
        this(parent, chartType, new DefaultLabelProvider());
    }

    public PieChart(Composite parent, IPieChart.ChartType chartType, ILabelProvider labelProvider)
    {
        super(parent, SWT.NONE);
        this.chartType = chartType;
        this.labelProvider = labelProvider;

        setData(UIConstants.CSS.CLASS_NAME, "chart"); //$NON-NLS-1$

        if (IPieChart.ChartType.DONUT == chartType)
        {
            addListener(SWT.Paint, event -> {
                // Set color in root node to background color
                if (getSeriesSet().getSeries().length > 0)
                {
                    ICircularSeries<?> cs = (ICircularSeries<?>) getSeriesSet().getSeries()[0];
                    cs.getRootNode().setColor(getPlotArea().getBackground());
                }
            });
        }

        getPlotArea().addCustomPaintListener(this::renderLabels);

        getLegend().setVisible(true);
        tooltip = new PieChartToolTip(this);
    }

    public PieChartToolTip getToolTip()
    {
        return tooltip;
    }

    /**
     * Allow to override pie slide label
     */
    public void setLabelProvider(ILabelProvider labelProvider)
    {
        if (labelProvider != null)
            this.labelProvider = labelProvider;
    }

    /**
     * Find the node at position
     */
    public Node getNodeAt(int x, int y)
    {
        Node node = null;
        for (ISeries<?> series : getSeriesSet().getSeries())
        {
            if (!(series instanceof ICircularSeries))
            {
                continue;
            }
            ICircularSeries<?> circularSeries = (ICircularSeries<?>) series;

            double primaryValueX = getSelectedPrimaryAxisValue(x, PieChart.Orientation.X_AXIS);
            double primaryValueY = getSelectedPrimaryAxisValue(y, PieChart.Orientation.Y_AXIS);

            node = circularSeries.getPieSliceFromPosition(primaryValueX, primaryValueY);
            circularSeries.setHighlightedNode(node);

        }
        return node;
    }

    private double getSelectedPrimaryAxisValue(int position, Orientation orientation)
    {
        double primaryValue;
        double start;
        double stop;
        int length;

        if (Orientation.X_AXIS == orientation)
        {
            IAxis axis = getAxisSet().getXAxis(ID_PRIMARY_X_AXIS);
            start = axis.getRange().lower;
            stop = axis.getRange().upper;
            length = getPlotArea().getSize().x;
        }
        else
        {
            IAxis axis = getAxisSet().getYAxis(ID_PRIMARY_Y_AXIS);
            start = axis.getRange().lower;
            stop = axis.getRange().upper;
            length = getPlotArea().getSize().y;
        }

        if (position <= 0)
        {
            primaryValue = start;
        }
        else if (position > length)
        {
            primaryValue = stop;
        }
        else
        {
            double delta = stop - start;
            double percentage;
            if (Orientation.X_AXIS == orientation)
            {
                percentage = ((100.0d / length) * position) / 100.0d;
            }
            else
            {
                percentage = (100.0d - ((100.0d / length) * position)) / 100.0d;
            }
            primaryValue = start + delta * percentage;
        }
        return primaryValue;
    }

    protected void renderLabels(PaintEvent e)
    {
        for (ISeries<?> series : getSeriesSet().getSeries())
        {
            if (series instanceof ICircularSeries)
            {
                IAxis xAxis = getAxisSet().getXAxis(series.getXAxisId());
                IAxis yAxis = getAxisSet().getYAxis(series.getYAxisId());

                List<Node> nodes = ((ICircularSeries<?>) series).getRootNode().getChildren();
                if (!nodes.isEmpty())
                {
                    for (Node node : nodes)
                    {
                        renderNodeLabel(node, (ICircularSeries<?>) series, e.gc, xAxis, yAxis);
                    }
                }
            }
        }

    }

    protected void renderNodeLabel(Node node, ICircularSeries<?> series, GC gc, IAxis xAxis, IAxis yAxis)
    {
        // children drawn first as parent overrides it's section of drawing
        if (!node.getChildren().isEmpty())
        {
            for (Node childNode : node.getChildren())
            {
                renderNodeLabel(childNode, series, gc, xAxis, yAxis);
            }
        }
        if (!node.isVisible())
            return;

        int level = node.getLevel() - series.getRootNode().getLevel() + (chartType == IPieChart.ChartType.PIE ? 0 : 1);

        Font oldFont = gc.getFont();
        gc.setForeground(Colors.WHITE);

        int angleStart = node.getAngleBounds().x;
        int angleWidth = (int) (node.getAngleBounds().y * 0.5);

        Point outerEnd = calcPixelCoord(xAxis, yAxis, level * 0.85, angleStart + angleWidth);

        FontDescriptor boldDescriptor = FontDescriptor.createFrom(gc.getFont()).setHeight(9);
        gc.setFont(boldDescriptor.createFont(getDisplay()));

        String label = labelProvider.getLabel(node);
        if (label != null)
        {
            Point textSize = gc.textExtent(label);
            gc.drawString(label, outerEnd.x - (textSize.x / 2), outerEnd.y - (textSize.y / 2), true);
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

    public interface ILabelProvider
    {
        public String getLabel(Node node);
    }

    private static class DefaultLabelProvider implements ILabelProvider
    {
        @Override
        public String getLabel(Node node)
        {
            String percentString = null;
            double percent = ((double) node.getAngleBounds().y / 360);
            if (percent > 0.025)
            {
                percentString = Values.Percent2.format(percent);
            }
            return percentString;
        }
    }

    public static final class PieColors
    {
        private static final int SIZE = 11;
        private static final float STEP = 360.0f / SIZE;

        private static final float HUE = 262.3f;
        private static final float SATURATION = 0.464f;
        private static final float BRIGHTNESS = 0.886f;

        private int nextSlice = 0;

        public Color next()
        {
            float brightness = Math.min(1.0f, BRIGHTNESS + (0.05f * (nextSlice / (float) SIZE)));
            return Colors.getColor(new RGB((HUE + (STEP * nextSlice++)) % 360f, SATURATION, brightness));

        }
    }
}
