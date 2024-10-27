package name.abuchen.portfolio.ui.util.chart;

import java.util.Arrays;
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
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.swtchart.internal.PlotArea;
import org.eclipse.swtchart.model.Node;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.Colors;

public class CircularChart extends Chart
{
    public interface ILabelProvider
    {
        public String getLabel(Node node);
    }

    public abstract static class LabelPainter implements ICustomPaintListener
    {
        protected final CircularChart pieChart;
        protected Font labelFont;

        protected LabelPainter(CircularChart pieChart)
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
        public RenderLabelsAlongAngle(CircularChart pieChart)
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
                            + (pieChart.chartType == SeriesType.PIE ? 0 : 1);

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
            gc.setForeground(Colors.getTextColor(node.getSliceColor()));

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

        public RenderLabelsCenteredInPie(CircularChart pieChart)
        {
            this(pieChart, pieChart.labelProvider);
        }

        public RenderLabelsCenteredInPie(CircularChart pieChart, ILabelProvider labelProvider)
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
                            + (pieChart.chartType == SeriesType.PIE ? 0 : 1);

            Point angleBounds = node.getAngleBounds();

            // check if pie chart (inner bound) is big enough to draw label

            Point textSize = gc.textExtent(label);

            Point start = getPixelCoordinate(xAxis, yAxis, (level - 1), angleBounds.x);
            Point end = getPixelCoordinate(xAxis, yAxis, (level - 1), angleBounds.x + angleBounds.y);
            int size = (int) Math.sqrt(Math.pow((end.x - start.x), 2) + Math.pow((end.y - start.y), 2));

            if ((size < textSize.y - 6) && (angleBounds.y < 350))
                return;

            Font oldFont = gc.getFont();
            gc.setForeground(Colors.getTextColor(node.getSliceColor()));
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

        public RenderLabelsOutsidePie(CircularChart pieChart, ILabelProvider labelProvider)
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
                            + (pieChart.chartType == SeriesType.PIE ? 0 : 1);

            Point angleBounds = node.getAngleBounds();
            int angle = angleBounds.x + angleBounds.y / 2;

            Point textSize = gc.textExtent(label);

            // some heuristic to check if there is enough space to render a
            // label

            Point start = getPixelCoordinate(xAxis, yAxis, (level - 1), angleBounds.x);
            Point end = getPixelCoordinate(xAxis, yAxis, (level - 1), angleBounds.x + angleBounds.y);

            if ((Math.abs(start.y - end.y) < 4) && (angleBounds.y < 350))
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

    private CircularChartToolTip tooltip;
    private SeriesType chartType;
    private ILabelProvider labelProvider;

    public CircularChart(Composite parent, SeriesType chartType)
    {
        this(parent, chartType, node -> Values.Percent2.format(node.getValue() / node.getParent().getValue()));
    }

    @SuppressWarnings("restriction")
    public CircularChart(Composite parent, SeriesType chartType, ILabelProvider labelProvider)
    {
        super(parent, SWT.NONE, null);

        // we must use the secondary constructor that is not creating the
        // PlotArea because the default constructor adds a mouse move listener
        // that is redrawing the chart on every mouse move. That leads to janky
        // UI when the tooltip is shown.
        new PlotArea(this, SWT.NONE);

        this.chartType = chartType;
        this.labelProvider = labelProvider;

        setData(UIConstants.CSS.CLASS_NAME, "chart"); //$NON-NLS-1$

        if (SeriesType.DOUGHNUT == chartType)
        {
            addListener(SWT.Paint, event -> {
                // Set color in root node to background color
                if (getSeriesSet().getSeries().length > 0)
                {
                    ICircularSeries<?> cs = (ICircularSeries<?>) getSeriesSet().getSeries()[0];
                    cs.getRootNode().setSliceColor(getPlotArea().getBackground());
                }
            });
        }

        getLegend().setVisible(false);
        getTitle().setVisible(false);

        tooltip = new CircularChartToolTip(this);
    }

    public void addLabelPainter(LabelPainter labelPainter)
    {
        getPlotArea().addCustomPaintListener(labelPainter);
    }

    public CircularChartToolTip getToolTip()
    {
        return tooltip;
    }

    /**
     * Find the node at position
     */
    public Optional<Node> getNodeAt(int x, int y)
    {
        for (ISeries<?> series : getSeriesSet().getSeries())
        {
            if (!(series instanceof ICircularSeries))
                continue;

            ICircularSeries<?> circularSeries = (ICircularSeries<?>) series;
            return Optional.ofNullable(circularSeries.getPieSliceFromPosition(x, y));
        }

        return Optional.empty();
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

    public void updateAngleBounds()
    {
        for (ISeries<?> series : getSeriesSet().getSeries())
        {
            if (series instanceof ICircularSeries<?>)
                updateAngleBounds(((ICircularSeries<?>) series).getRootNode());
        }
    }

    private void updateAngleBounds(Node parent) // NOSONAR
    {
        if (parent.getChildren() == null || parent.getChildren().isEmpty())
            return;

        Point angleBounds = parent.getAngleBounds();

        List<Node> children = parent.getChildren();
        int size = children.size();

        // exact length of the arc of one slice
        double[] arcs = new double[size];
        // length of an arc rounded to int
        int[] intArcs = new int[size];
        // diff checks for rounding errors
        int diff = angleBounds.y;

        for (int ii = 0; ii < size; ii++)
        {
            Node child = children.get(ii);
            double fraction = child.getValue() / parent.getValue();
            arcs[ii] = fraction * angleBounds.y;
            intArcs[ii] = (int) (arcs[ii] + 0.5);
            diff -= intArcs[ii];
        }

        // if diff < 0 then we assigned more than the available arc length
        while (diff < 0 && diff >= -10)
        {
            double delta = 0d;
            int candidate = 0;
            for (int ii = 0; ii < size; ii++)
            {
                double d = intArcs[ii] - arcs[ii];

                // skip nodes which would be reduced to zero
                if (d > delta && intArcs[ii] > 1)
                {
                    delta = d;
                    candidate = ii;
                }
            }
            intArcs[candidate]--;
            diff++;
        }

        // if diff > 0 then we assigned less than the available arc length
        while (diff > 0 && diff < 10)
        {
            double delta = 0d;
            int candidate = 0;
            for (int ii = 0; ii < size; ii++)
            {
                double d = intArcs[ii] - arcs[ii];

                // prefer slices with zero
                if (intArcs[ii] == 0)
                    d -= 1;

                if (d < delta)
                {
                    delta = d;
                    candidate = ii;
                }
            }
            intArcs[candidate]++;
            diff--;
        }

        long zeros = Arrays.stream(intArcs).filter(i -> i == 0).count();
        boolean[] gaps = new boolean[size];
        if (zeros > 0)
        {
            // if available, assign available degrees from the gap to the zero
            while (diff > 0 && zeros > 0)
            {
                for (int ii = 0; ii < size; ii++)
                {
                    if (intArcs[ii] == 0)
                    {
                        intArcs[ii] = 1;
                        zeros--;
                        diff--;
                        break;
                    }
                }
            }

            // now insert gaps into the pie to indicate that there is at
            // least one slice which cannot be shown

            int gapsNeeded = 0;

            for (int ii = 0; ii < size; ii++)
            {
                if (intArcs[ii] == 0 && (ii + 1 == size || intArcs[ii + 1] > 0))
                {
                    gaps[ii] = true;
                    gapsNeeded++;
                }
            }

            while (gapsNeeded > 0 && gapsNeeded < 50)
            {
                double delta = 0d;
                int candidate = 0;
                for (int ii = 0; ii < size; ii++)
                {
                    double d = intArcs[ii] - arcs[ii];
                    if (d > delta && intArcs[ii] > 1)
                    {
                        delta = d;
                        candidate = ii;
                    }
                }
                intArcs[candidate]--;
                gapsNeeded--;
            }
        }

        // assign calculated angles to nodes
        int nextAngle = angleBounds.x;
        for (int ii = 0; ii < children.size(); ii++)
        {
            Node child = children.get(ii);
            child.setAngleBounds(new Point(nextAngle, intArcs[ii]));

            if (gaps[ii])
                nextAngle++; // add the gap

            updateAngleBounds(child);

            nextAngle += intArcs[ii];
        }
    }
}
