package name.abuchen.portfolio.ui.util.chart;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.swtchart.IAxis;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.util.Pair;

public class ChartToolsManager
{
    private abstract static class ChartToolBase
    {

        void onMouseDown(Event e)
        {
        }

        void onMouseMove(Event e)
        {
        }

        void onMouseUp(Event e)
        {
        }

        void paintControl(PaintEvent e)
        {
        }

    }

    public enum ChartTools
    {
        NONE("", c -> null, null, null), // //$NON-NLS-1$
        CROSSHAIR(Messages.BtnLabelCrosshair, p -> new CrosshairTool(p.getLeft(), p.getRight()), Images.CROSSHAIR_ON, 
                        Images.CROSSHAIR_OFF), //
        MEASUREMENT(Messages.BtnLabelMeasureDistance, p -> new MeasurementTool(p.getLeft(), p.getRight()),
                        Images.MEASUREMENT_ON, Images.MEASUREMENT_OFF);

        private String actionText;
        private Function<Pair<TimelineChart, Color>, ChartToolBase> createToolFactory;
        private Images imageON;
        private Images imageOFF;

        private ChartTools(String actionText,
                        Function<Pair<TimelineChart, Color>, ChartToolBase> createToolFactory,
                        Images imageON,
                        Images imageOFF)
        {
            this.actionText = actionText;
            this.createToolFactory = createToolFactory;
            this.imageON = imageON;
            this.imageOFF = imageOFF;
        }

        String getActionText()
        {
            return actionText;
        }

        Images getImageOFF()
        {
            return imageOFF;
        }

        Images getImageON()
        {
            return imageON;
        }

        ChartToolBase createTool(TimelineChart chart, Color color)
        {
            return createToolFactory.apply(new Pair<>(chart, color));
        }
    }

    private static record Spot(int time, LocalDate date, double xCoordinate, double value)
    {
        public static Spot from(Event e, TimelineChart chart)
        {
            double xCoordinate = chart.getAxisSet().getXAxis(0).getDataCoordinate(e.x);
            LocalDate date = Instant.ofEpochMilli((long) xCoordinate).atZone(ZoneId.systemDefault()).toLocalDate();
            double value = chart.getAxisSet().getYAxis(0).getDataCoordinate(e.y);

            return new Spot(e.time, date, xCoordinate, value);
        }

        public Point toPoint(TimelineChart chart)
        {
            return new Point(chart.getAxisSet().getXAxis(0).getPixelCoordinate(xCoordinate),
                            chart.getAxisSet().getYAxis(0).getPixelCoordinate(value));
        }
    }

    public static final int PADDING = 5;
    private ChartTools activeTool = ChartTools.NONE;
    private ChartToolBase currentTool = null;
    private Color color = Colors.BLACK;
    private TimelineChart chart;
    private ArrayList<Pair<ChartTools, IAction>> buttons = new ArrayList<>();

    public ChartToolsManager(TimelineChart chart)
    {
        this.chart = chart;
        chart.getPlotArea().addListener(SWT.MouseDown, this::onMouseDown);
        chart.getPlotArea().addListener(SWT.MouseMove, this::onMouseMove);
        chart.getPlotArea().addListener(SWT.MouseUp, this::onMouseUp);

        chart.getPlotArea().addPaintListener(this::paintControl);
    }

    private void onMouseDown(Event e)
    {
        if (currentTool == null)
            return;

        currentTool.onMouseDown(e);
    }

    private void onMouseMove(Event e)
    {
        if (currentTool == null)
            return;

        currentTool.onMouseMove(e);
    }

    private void onMouseUp(Event e)
    {
        if (currentTool == null)
            return;

        currentTool.onMouseUp(e);
    }

    private void paintControl(PaintEvent e)
    {
        if (currentTool == null)
            return;

        currentTool.paintControl(e);
    }

    public Color getColor()
    {
        return color;
    }

    public void setColor(Color color)
    {
        this.color = color;
    }

    public void addButtons(ToolBarManager toolBar)
    {
        createActions().forEach(p -> {
            toolBar.add(p.getRight());
            buttons.add(p);
        });
    }

    private List<Pair<ChartTools, IAction>> createActions()
    {
        return List.of(createAction(ChartTools.CROSSHAIR), createAction(ChartTools.MEASUREMENT));
    }

    private Pair<ChartTools, IAction> createAction(ChartTools tool)
    {
        var action = new SimpleAction(tool.getActionText(), //
                        this.activeTool == tool ? tool.getImageON() : tool.getImageOFF(), //
                        a -> {
                            if (this.activeTool == tool)
                                this.activeTool = ChartTools.NONE;
                            else
                                this.activeTool = tool;

                            currentTool = this.activeTool.createTool(chart, color);

                            // update images of tool bar buttons
                            updateButtonImages(this.activeTool);

                            chart.getToolTip().setActive(this.activeTool == ChartTools.NONE);
                            chart.redraw();
                        });

        return new Pair<>(tool, action);
    }

    private void updateButtonImages(ChartTools activeTool)
    {
        buttons.forEach(p -> p.getRight().setImageDescriptor(
                        (p.getLeft() == activeTool ? p.getLeft().getImageON() : p.getLeft().getImageOFF())
                                        .descriptor()));
    }

    public void addContextMenu(IMenuManager manager)
    {
        createActions().forEach(p -> manager.add(p.getRight()));
    }

    private static class CrosshairTool extends ChartToolBase
    {
        private final TimelineChart chart;
        private Point p1;
        private Color color;

        private CrosshairTool(TimelineChart chart, Color color)
        {
            this.chart = chart;
            this.color = color;
        }

        @Override
        void onMouseDown(Event e)
        {
            if (e.button != 1)
                return;
            p1 = new Point(e.x, e.y);

            chart.redraw();
        }

        @Override
        void paintControl(PaintEvent e)
        {
            if (p1 == null)
                return;

            drawCrosshair(e, p1);
        }

        private LocalDate getDateTime(Point p)
        {
            return Instant.ofEpochMilli((long) chart.getAxisSet().getXAxis(0).getDataCoordinate(p.x))
                            .atZone(ZoneId.systemDefault()).toLocalDate();
        }

        private void drawCrosshairDateTextbox(PaintEvent e, Point p)
        {
            LocalDate date = getDateTime(p);
            String xText = Values.Date.format(date);

            // Add margin to text
            Point txtXExtend = e.gc.textExtent(xText);
            txtXExtend.x += 5;
            txtXExtend.y += 5;

            Point rectPoint = new Point(0, e.height - txtXExtend.y - 2);
            Point textPoint = new Point(0, e.height - txtXExtend.y + 1);

            // Visual shift vertical of the container
            if (p.x <= e.width / 2)
            {
                rectPoint.x = p.x + 5;
                textPoint.x = p.x + 8;
            }
            else
            {
                rectPoint.x = p.x - txtXExtend.x - 5;
                textPoint.x = p.x - txtXExtend.x - 1;
            }

            e.gc.setBackground(Colors.brighter(color));
            e.gc.setForeground(Colors.getTextColor(color));

            e.gc.fillRoundRectangle(rectPoint.x, rectPoint.y, txtXExtend.x, txtXExtend.y, PADDING, PADDING);
            e.gc.drawText(xText, textPoint.x, textPoint.y, true);
        }

        private double getYValue(Point p)
        {
            return chart.getAxisSet().getYAxis(0).getDataCoordinate(p.y);
        }

        private void drawCrosshairValueTextbox(PaintEvent e, Point p)
        {
            double value = getYValue(p);

            String yText = new DecimalFormat(Values.Quote.pattern()).format(value);

            // Add margin to text
            Point txtYExtend = e.gc.textExtent(yText);
            txtYExtend.x += 5;
            txtYExtend.y += 5;

            Point rectPoint = new Point(e.width - txtYExtend.x - 2, 0);
            Point textPoint = new Point(e.width - txtYExtend.x + 1, 0);

            // Visual shift horizontally of the container
            if (p.y <= e.height / 2)
            {
                rectPoint.y = p.y + 4;
                textPoint.y = p.y + 7;
            }
            else
            {
                rectPoint.y = p.y - txtYExtend.y - 4;
                textPoint.y = p.y - txtYExtend.y - 1;
            }

            e.gc.setBackground(Colors.brighter(color));
            e.gc.setForeground(Colors.getTextColor(color));

            e.gc.fillRoundRectangle(rectPoint.x, rectPoint.y, txtYExtend.x, txtYExtend.y, PADDING, PADDING);
            e.gc.drawText(yText, textPoint.x, textPoint.y, true);
        }

        private void drawCrosshairValueSecondAxisTextbox(PaintEvent e, Point p)
        {
            IAxis axis = chart.getAxisSet().getYAxis(2);
            if (!axis.getTick().isVisible())
                return;

            double value = axis.getDataCoordinate(p.y);

            String yText = Values.PercentWithSign.format(value);

            // Add margin to text
            Point txtYExtend = e.gc.textExtent(yText);
            txtYExtend.x += 5;
            txtYExtend.y += 5;

            Point rectPoint = new Point(1, 0);
            Point textPoint = new Point(3, 0);

            // Visual shift horizontally of the container
            if (p.y <= e.height / 2)
            {
                rectPoint.y = p.y + 4;
                textPoint.y = p.y + 7;
            }
            else
            {
                rectPoint.y = p.y - txtYExtend.y - 4;
                textPoint.y = p.y - txtYExtend.y - 1;
            }

            e.gc.setBackground(Colors.brighter(color));
            e.gc.setForeground(Colors.getTextColor(color));

            e.gc.fillRoundRectangle(rectPoint.x, rectPoint.y, txtYExtend.x, txtYExtend.y, PADDING, PADDING);
            e.gc.drawText(yText, textPoint.x, textPoint.y, true);
        }

        private void drawCrosshair(PaintEvent e, Point p)
        {
            e.gc.setLineWidth(1);
            e.gc.setLineStyle(SWT.LINE_SOLID);
            e.gc.setForeground(color);
            e.gc.setBackground(color);
            e.gc.setAntialias(SWT.ON);

            // draw crosshair
            e.gc.drawLine(p.x, 0, p.x, e.height);
            e.gc.drawLine(0, p.y, e.width, p.y);

            // set textbox style
            e.gc.setForeground(Colors.theme().defaultForeground());
            e.gc.setBackground(Colors.theme().defaultBackground());
            e.gc.setAlpha(220);

            // value for horizontal axis
            drawCrosshairDateTextbox(e, p);

            drawCrosshairValueTextbox(e, p);
            drawCrosshairValueSecondAxisTextbox(e, p);
        }
    }

    private static class MeasurementTool extends ChartToolBase
    {

        public static final int OFFSET = 10;

        private final TimelineChart chart;

        private boolean showRelativeChange = true;

        private boolean redrawOnMove = false;
        private Spot start;
        private Spot end;

        private Color color;

        MeasurementTool(TimelineChart chart, Color color)
        {
            this.chart = chart;
            this.color = color;

            // do not show relative change for chart show
            // percentages as it does not make sense once
            // negative values are included
            this.showRelativeChange = !(chart.getToolTip().getDefaultValueFormat() instanceof DecimalFormat fmt)
                            || fmt.toPattern().indexOf('%') < 0;
        }

        @Override
        void onMouseDown(Event e)
        {
            if (e.button != 1)
                return;

            if (redrawOnMove) // click'n'click mode
                end = Spot.from(e, chart);
            else // new line
                start = end = Spot.from(e, chart);

            redrawOnMove = true;
            chart.redraw();
        }

        @Override
        void onMouseMove(Event e)
        {
            if (!redrawOnMove)
                return;

            end = Spot.from(e, chart);
            chart.redraw();
        }

        @Override
        void onMouseUp(Event e)
        {
            if (start == null || e.button != 1)
                return;

            // if enough time has elapsed, assume it was click'n'drag
            // mode (otherwise continue in click'n'click mode)
            if (e.time - start.time > 300)
                redrawOnMove = false;

            end = Spot.from(e, chart);
            chart.redraw();
        }

        @Override
        void paintControl(PaintEvent e)
        {
            if (start == null || end == null)
                return;

            Point p1 = start.toPoint(chart);
            Point p2 = end.toPoint(chart);

            int antiAlias = e.gc.getAntialias();
            try
            {
                e.gc.setLineWidth(1);
                e.gc.setLineStyle(SWT.LINE_SOLID);
                e.gc.setForeground(color);
                e.gc.setBackground(color);
                e.gc.setAntialias(SWT.ON);
                e.gc.drawLine(p1.x, p1.y, p2.x, p2.y);

                e.gc.fillOval(p1.x - 2, p1.y - 2, 5, 5);
                e.gc.fillOval(p2.x - 2, p2.y - 2, 5, 5);

                var buffer = new StringBuilder();
                buffer.append(String.valueOf(ChronoUnit.DAYS.between(start.date(), end.date())));
                buffer.append(" | "); //$NON-NLS-1$
                buffer.append(chart.getToolTip().getDefaultValueFormat().format(end.value() - start.value()));
                if (showRelativeChange)
                {
                    buffer.append(" | "); //$NON-NLS-1$
                    buffer.append(Values.PercentWithSign.format(end.value() / start.value() - 1));
                }
                var text = buffer.toString();

                Point txtExtend = e.gc.textExtent(text);
                Rectangle plotArea = chart.getPlotArea().getClientArea();

                e.gc.setBackground(Colors.brighter(color));
                e.gc.setForeground(Colors.getTextColor(color));

                if (p2.x < plotArea.width / 2)
                {
                    // draw to the right
                    e.gc.fillRoundRectangle(p2.x + OFFSET, //
                                    p2.y - txtExtend.y / 2 - PADDING, //
                                    txtExtend.x + 2 * PADDING, //
                                    txtExtend.y + 2 * PADDING, PADDING, PADDING);
                    e.gc.drawText(text, p2.x + OFFSET + PADDING, p2.y - txtExtend.y / 2, true);
                }
                else
                {
                    // draw to the left
                    e.gc.fillRoundRectangle(p2.x - OFFSET - 2 * PADDING - txtExtend.x, //
                                    p2.y - txtExtend.y / 2 - PADDING, //
                                    txtExtend.x + 2 * PADDING, //
                                    txtExtend.y + 2 * PADDING, PADDING, PADDING);
                    e.gc.drawText(text, p2.x - OFFSET - PADDING - txtExtend.x, p2.y - txtExtend.y / 2, true);
                }
            }
            finally
            {
                e.gc.setAntialias(antiAlias);
            }
        }
    }

}
