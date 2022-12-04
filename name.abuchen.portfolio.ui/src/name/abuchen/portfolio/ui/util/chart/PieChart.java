package name.abuchen.portfolio.ui.util.chart;

import java.util.List;
import java.util.Optional;

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.IAxis;
import org.eclipse.swtchart.ICircularSeries;
import org.eclipse.swtchart.ICustomPaintListener;
import org.eclipse.swtchart.ISeries;
import org.eclipse.swtchart.model.Node;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.views.IPieChart;

public class PieChart extends Chart
{
    public interface ILabelProvider
    {
        public String getLabel(Node node);
    }

    public abstract static class LabelPainter implements ICustomPaintListener
    {
        protected final PieChart pieChart;
        protected Font labelFont;

        protected LabelPainter(PieChart pieChart)
        {
            this.pieChart = pieChart;
        }

        @Override
        public final void paintControl(PaintEvent e)
        {
            if (labelFont == null)
            {
                // lazily create font _after_ CSS has been applied

                FontDescriptor fd = FontDescriptor.createFrom(e.gc.getFont());
                labelFont = fd.increaseHeight(-1).createFont(e.gc.getDevice());

                pieChart.addDisposeListener(de -> labelFont.dispose());
            }

            for (ISeries<?> series : pieChart.getSeriesSet().getSeries())
            {
                if (series instanceof ICircularSeries)
                {
                    IAxis xAxis = pieChart.getAxisSet().getXAxis(series.getXAxisId());
                    IAxis yAxis = pieChart.getAxisSet().getYAxis(series.getYAxisId());

                    List<Node> nodes = ((ICircularSeries<?>) series).getRootNode().getChildren();
                    if (!nodes.isEmpty())
                    {
                        for (Node node : nodes)
                            renderLabel(node, (ICircularSeries<?>) series, e.gc, xAxis, yAxis);
                    }
                }
            }
        }

        protected abstract void renderLabel(Node node, ICircularSeries<?> series, GC gc, IAxis xAxis, IAxis yAxis);

        protected final Point getPixelCoordinate(IAxis xAxis, IAxis yAxis, double pieLevel, int angle)
        {
            double xCoordinate = pieLevel * Math.cos(Math.toRadians(angle));
            double yCoordinate = pieLevel * Math.sin(Math.toRadians(angle));
            return new Point(xAxis.getPixelCoordinate(xCoordinate), yAxis.getPixelCoordinate(yCoordinate));
        }
    }

    public static class RenderLabelsAlongAngle extends LabelPainter
    {
        public RenderLabelsAlongAngle(PieChart pieChart)
        {
            super(pieChart);
        }

        @Override
        protected void renderLabel(Node node, ICircularSeries<?> series, GC gc, IAxis xAxis, IAxis yAxis)
        {
            for (Node childNode : node.getChildren())
                renderLabel(childNode, series, gc, xAxis, yAxis);

            String label = pieChart.labelProvider.getLabel(node);
            if (!node.isVisible() || label == null)
                return;

            int level = node.getLevel() - series.getRootNode().getLevel()
                            + (pieChart.chartType == IPieChart.ChartType.PIE ? 0 : 1);

            Point angleBounds = node.getAngleBounds();

            // check if pie chart (inner bound) is big enough to draw label

            gc.setFont(labelFont);
            Point textSize = gc.textExtent(label);

            Point start = getPixelCoordinate(xAxis, yAxis, (level - 1), angleBounds.x);
            Point end = getPixelCoordinate(xAxis, yAxis, (level - 1), angleBounds.x + angleBounds.y);
            int size = (int) Math.sqrt(Math.pow((end.x - start.x), 2) + Math.pow((end.y - start.y), 2));
            if (size < textSize.y - 4)
                return;

            // calculate text angle

            int angle = angleBounds.x + angleBounds.y / 2;
            Point innerBound = getPixelCoordinate(xAxis, yAxis, (level - 1), angle);
            Point outerBound = getPixelCoordinate(xAxis, yAxis, level, angle);

            Transform t = new Transform(gc.getDevice());

            Point textPosition;

            if (angle >= 90 && angle <= 270)
            {
                textPosition = outerBound;
                t.translate(textPosition.x, textPosition.y);
                t.rotate((-angle + 180));
            }
            else
            {
                textPosition = innerBound;
                t.translate(textPosition.x, textPosition.y);
                t.rotate(-angle);
            }

            gc.setTransform(t);
            gc.setForeground(Colors.getTextColor(node.getColor()));

            int length = (int) Math.sqrt(
                            Math.pow((outerBound.x - innerBound.x), 2) + Math.pow((outerBound.y - innerBound.y), 2));
            gc.setClipping(0, 0 - (textSize.y / 2), length - 3, textSize.y);

            gc.drawString(label, 2, 0 - (textSize.y / 2), true);

            gc.setClipping((Rectangle) null);
            gc.setTransform(null);
        }

    }

    public static class RenderLabelsCenteredInPie extends LabelPainter
    {
        private final ILabelProvider labelProvider;

        public RenderLabelsCenteredInPie(PieChart pieChart)
        {
            this(pieChart, pieChart.labelProvider);
        }

        public RenderLabelsCenteredInPie(PieChart pieChart, ILabelProvider labelProvider)
        {
            super(pieChart);
            this.labelProvider = labelProvider;
        }

        @Override
        protected void renderLabel(Node node, ICircularSeries<?> series, GC gc, IAxis xAxis, IAxis yAxis)
        {
            for (Node childNode : node.getChildren())
                renderLabel(childNode, series, gc, xAxis, yAxis);

            String label = labelProvider.getLabel(node);
            if (!node.isVisible() || label == null)
                return;

            int level = node.getLevel() - series.getRootNode().getLevel()
                            + (pieChart.chartType == IPieChart.ChartType.PIE ? 0 : 1);

            Point angleBounds = node.getAngleBounds();

            // check if pie chart (inner bound) is big enough to draw label

            Point textSize = gc.textExtent(label);

            Point start = getPixelCoordinate(xAxis, yAxis, (level - 1), angleBounds.x);
            Point end = getPixelCoordinate(xAxis, yAxis, (level - 1), angleBounds.x + angleBounds.y);
            int size = (int) Math.sqrt(Math.pow((end.x - start.x), 2) + Math.pow((end.y - start.y), 2));
            if (size < textSize.y - 6)
                return;

            Font oldFont = gc.getFont();
            gc.setForeground(Colors.getTextColor(node.getColor()));
            gc.setFont(labelFont);

            int angleStart = node.getAngleBounds().x;
            int angleWidth = (int) (node.getAngleBounds().y * 0.5);

            Point outerEnd = getPixelCoordinate(xAxis, yAxis, level * 0.85, angleStart + angleWidth);
            gc.drawString(label, outerEnd.x - (textSize.x / 2), outerEnd.y - (textSize.y / 2), true);

            gc.setFont(oldFont);
        }
    }

    public static class RenderLabelsOutsidePie extends LabelPainter
    {
        private final ILabelProvider labelProvider;

        public RenderLabelsOutsidePie(PieChart pieChart, ILabelProvider labelProvider)
        {
            super(pieChart);
            this.labelProvider = labelProvider;
        }

        @Override
        protected void renderLabel(Node node, ICircularSeries<?> series, GC gc, IAxis xAxis, IAxis yAxis)
        {
            for (Node childNode : node.getChildren())
                renderLabel(childNode, series, gc, xAxis, yAxis);

            String label = labelProvider.getLabel(node);
            if (!node.isVisible() || label == null)
                return;

            int level = node.getLevel() - series.getRootNode().getLevel()
                            + (pieChart.chartType == IPieChart.ChartType.PIE ? 0 : 1);

            Point angleBounds = node.getAngleBounds();
            int angle = angleBounds.x + angleBounds.y / 2;

            Point textSize = gc.textExtent(label);

            // some heuristic to check if there is enough space to render a
            // label

            Point start = getPixelCoordinate(xAxis, yAxis, (level - 1), angleBounds.x);
            Point end = getPixelCoordinate(xAxis, yAxis, (level - 1), angleBounds.x + angleBounds.y);
            if (Math.abs(start.y - end.y) < 4)
                return;

            Point point = getPixelCoordinate(xAxis, yAxis, level * 1.03, angle);
            int x = angle >= 90 && angle <= 270 ? point.x - textSize.x : point.x;

            Font oldFont = gc.getFont();
            gc.setForeground(Colors.theme().defaultForeground());
            gc.setFont(labelFont);

            gc.drawString(label, x, point.y - (textSize.y / 2), true);

            gc.setFont(oldFont);
        }

    }

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
        this(parent, chartType, node -> Values.Percent2.format(node.getValue() / node.getParent().getValue()));
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

        getLegend().setVisible(false);
        getTitle().setVisible(false);

        tooltip = new PieChartToolTip(this);
    }

    public void addLabelPainter(LabelPainter labelPainter)
    {
        getPlotArea().addCustomPaintListener(labelPainter);
    }

    public PieChartToolTip getToolTip()
    {
        return tooltip;
    }

    /**
     * Find the node at position
     */
    public Optional<Node> getNodeAt(int x, int y)
    {
        Node node = null;
        for (ISeries<?> series : getSeriesSet().getSeries())
        {
            if (!(series instanceof ICircularSeries))
                continue;

            ICircularSeries<?> circularSeries = (ICircularSeries<?>) series;

            double primaryValueX = getSelectedPrimaryAxisValue(x, PieChart.Orientation.X_AXIS);
            double primaryValueY = getSelectedPrimaryAxisValue(y, PieChart.Orientation.Y_AXIS);

            node = circularSeries.getPieSliceFromPosition(primaryValueX, primaryValueY);
            return Optional.ofNullable(node);
        }

        return Optional.empty();
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
