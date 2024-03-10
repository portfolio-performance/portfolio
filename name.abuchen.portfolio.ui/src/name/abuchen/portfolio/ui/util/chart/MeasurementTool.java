package name.abuchen.portfolio.ui.util.chart;

import java.text.DecimalFormat;
import java.time.temporal.ChronoUnit;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.chart.ChartToolsManager.ChartTool;
import name.abuchen.portfolio.ui.util.chart.ChartToolsManager.Spot;


class MeasurementTool implements ChartTool
{

    public static final int OFFSET = 10;
    public static final int PADDING = ChartToolsManager.PADDING;

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
    public void onMouseDown(Event e)
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
    public void onMouseMove(Event e)
    {
        if (!redrawOnMove)
            return;

        end = Spot.from(e, chart);
        chart.redraw();
    }

    @Override
    public void onMouseUp(Event e)
    {
        if (start == null || e.button != 1)
            return;

        // if enough time has elapsed, assume it was click'n'drag
        // mode (otherwise continue in click'n'click mode)
        if (e.time - start.time() > 300)
            redrawOnMove = false;

        end = Spot.from(e, chart);
        chart.redraw();
    }

    @Override
    public void paintControl(PaintEvent e)
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
            buffer.append(chart.getToolTip().getDefaultValueFormat()
                            .format(end.valueYRightAxis() - start.valueYRightAxis()));
            if (showRelativeChange)
            {
                buffer.append(" | "); //$NON-NLS-1$
                buffer.append(Values.PercentWithSign.format(end.valueYRightAxis() / start.valueYRightAxis() - 1));
            }
            var text = buffer.toString();

            Point txtExtend = e.gc.textExtent(text);
            Rectangle plotArea = ((Composite) chart.getPlotArea().getControl()).getClientArea();

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
                e.gc.drawText(text, p2.x - OFFSET - PADDING - txtExtend.x, p2.y - txtExtend.y / 2,
                                true);
            }
        }
        finally
        {
            e.gc.setAntialias(antiAlias);
        }
    }
}
