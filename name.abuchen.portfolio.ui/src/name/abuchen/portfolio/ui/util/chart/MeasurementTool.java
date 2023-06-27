package name.abuchen.portfolio.ui.util.chart;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.SimpleAction;

public class MeasurementTool
{
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

    public static final int OFFSET = 10;
    public static final int PADDING = 5;

    private final TimelineChart chart;
    private ArrayList<IAction> buttons = new ArrayList<>();

    private boolean isActive = false;

    private Color color = Colors.BLACK;

    private boolean redrawOnMove = false;
    private Spot start;
    private Spot end;

    /* package */ MeasurementTool(TimelineChart chart)
    {
        this.chart = chart;

        chart.getPlotArea().addListener(SWT.MouseDown, this::onMouseDown);
        chart.getPlotArea().addListener(SWT.MouseMove, this::onMouseMove);
        chart.getPlotArea().addListener(SWT.MouseUp, this::onMouseUp);

        chart.getPlotArea().addPaintListener(this::paintControl);
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
        if (!PortfolioPlugin.getDefault().getPreferenceStore()
                        .getBoolean(UIConstants.Preferences.ENABLE_EXPERIMENTAL_FEATURES))
            return;

        var action = createAction();
        // store buttons to update their image on context menu action
        buttons.add(action);
        toolBar.add(action);

    }

    public void addContextMenu(IMenuManager manager)
    {
        if (PortfolioPlugin.getDefault().getPreferenceStore()
                        .getBoolean(UIConstants.Preferences.ENABLE_EXPERIMENTAL_FEATURES))
            manager.add(createAction());
    }

    private SimpleAction createAction()
    {
        return new SimpleAction(Messages.LabelMeasureDistance, //
                        isActive ? Images.MEASUREMENT_ON : Images.MEASUREMENT_OFF, //
                        a -> {
                            isActive = !isActive;

                            // update images of tool bar buttons
                            ImageDescriptor image = isActive ? Images.MEASUREMENT_ON.descriptor()
                                            : Images.MEASUREMENT_OFF.descriptor();
                            buttons.forEach(button -> button.setImageDescriptor(image));

                            chart.getToolTip().setActive(!isActive);

                            if (!isActive)
                            {
                                start = end = null;
                                redrawOnMove = false;
                                chart.redraw();
                            }
                        });
    }

    private void onMouseDown(Event e)
    {
        if (!isActive || e.button != 1)
            return;

        if (redrawOnMove) // click'n'click mode
            end = Spot.from(e, chart);
        else // new line
            start = end = Spot.from(e, chart);

        redrawOnMove = true;
        chart.redraw();
    }

    private void onMouseMove(Event e)
    {
        if (!isActive || !redrawOnMove)
            return;

        end = Spot.from(e, chart);
        chart.redraw();
    }

    private void onMouseUp(Event e)
    {
        if (!isActive || start == null || e.button != 1)
            return;

        // if enough time has elapsed, assume it was click'n'drag
        // mode (otherwise continue in click'n'click mode)
        if (e.time - start.time > 300)
            redrawOnMove = false;

        end = Spot.from(e, chart);
        chart.redraw();
    }

    private void paintControl(PaintEvent e)
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
            e.gc.setForeground(this.color);
            e.gc.setBackground(this.color);
            e.gc.setAntialias(SWT.ON);
            e.gc.drawLine(p1.x, p1.y, p2.x, p2.y);

            e.gc.fillOval(p1.x - 2, p1.y - 2, 5, 5);
            e.gc.fillOval(p2.x - 2, p2.y - 2, 5, 5);

            String text = String.valueOf(ChronoUnit.DAYS.between(start.date(), end.date())) + " | " //$NON-NLS-1$
                            + chart.getToolTip().getDefaultValueFormat().format(end.value() - start.value()) + " | " //$NON-NLS-1$
                            + Values.PercentWithSign.format(end.value() / start.value() - 1);

            Point txtExtend = e.gc.textExtent(text);
            Rectangle plotArea = chart.getPlotArea().getClientArea();

            e.gc.setBackground(Colors.brighter(this.color));
            e.gc.setForeground(Colors.getTextColor(this.color));

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
