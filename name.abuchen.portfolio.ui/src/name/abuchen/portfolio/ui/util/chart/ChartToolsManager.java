package name.abuchen.portfolio.ui.util.chart;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;

import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.util.Pair;

public class ChartToolsManager
{
    interface ChartTool
    {
        void onMouseDown(Event e);

        void onMouseMove(Event e);

        void onMouseUp(Event e);

        void paintControl(PaintEvent e);
    }

    public enum ChartTools
    {
        NONE("", (ch, co) -> null, null, null), // //$NON-NLS-1$
        CROSSHAIR(Messages.LabelCrosshair, CrosshairTool::new, Images.CROSSHAIR_ON, Images.CROSSHAIR_OFF), //
        MEASUREMENT(Messages.LabelMeasureDistance, MeasurementTool::new, Images.MEASUREMENT_ON, Images.MEASUREMENT_OFF); //

        private String actionText;
        private BiFunction<TimelineChart, Color, ChartTool> createToolFactory;
        private Images imageON;
        private Images imageOFF;

        private ChartTools(String actionText, BiFunction<TimelineChart, Color, ChartTool> createToolFactory,
                        Images imageON, Images imageOFF)
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

        ChartTool createTool(TimelineChart chart, Color color)
        {
            return createToolFactory.apply(chart, color);
        }
    }

    record Spot(int time, LocalDate date, double xCoordinate, double valueYLeftAxis, double valueYRightAxis)
    {
        public static Spot from(Event e, TimelineChart chart)
        {
            double xCoordinate = chart.getAxisSet().getXAxis(0).getDataCoordinate(e.x);
            LocalDate date = LocalDate.ofEpochDay((long) xCoordinate);
            double valueYLeftAxis = chart.getAxisSet().getYAxis(2).getDataCoordinate(e.y);
            double valueYRightAxis = chart.getAxisSet().getYAxis(0).getDataCoordinate(e.y);

            return new Spot(e.time, date, xCoordinate, valueYLeftAxis, valueYRightAxis);
        }

        public Point toPoint(TimelineChart chart)
        {
            return new Point(chart.getAxisSet().getXAxis(0).getPixelCoordinate(xCoordinate),
                            chart.getAxisSet().getYAxis(0).getPixelCoordinate(valueYRightAxis));
        }
    }

    public static final int PADDING = 5;
    private ChartTools activeTool = ChartTools.NONE;
    private ChartTool currentTool = null;
    private Color color = Colors.BLACK;
    private TimelineChart chart;
    private ArrayList<Pair<ChartTools, IAction>> buttons = new ArrayList<>();

    public ChartToolsManager(TimelineChart chart)
    {
        this.chart = chart;
        chart.getPlotArea().getControl().addListener(SWT.MouseDown, this::onMouseDown);
        chart.getPlotArea().getControl().addListener(SWT.MouseMove, this::onMouseMove);
        chart.getPlotArea().getControl().addListener(SWT.MouseUp, this::onMouseUp);

        chart.getPlotArea().getControl().addPaintListener(this::paintControl);
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
                            chart.setCursor(this.activeTool == ChartTools.NONE ? null
                                            : new Cursor(this.chart.getDisplay(), SWT.CURSOR_CROSS));
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
}
