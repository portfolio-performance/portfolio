package name.abuchen.portfolio.ui.util.chart;

import java.time.LocalDate;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swtchart.IAxis;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.chart.ChartToolsManager.ChartTool;
import name.abuchen.portfolio.ui.util.chart.ChartToolsManager.Spot;

class CrosshairTool implements ChartTool
{
    private final TimelineChart chart;
    private Spot spot;
    private Color color;

    CrosshairTool(TimelineChart chart, Color color)
    {
        this.chart = chart;
        this.color = color;
    }

    @Override
    public void onMouseDown(Event e)
    {
        if (e.button != 1)
            return;
        spot = Spot.from(e, chart);

        chart.redraw();
    }

    @Override
    public void onMouseMove(Event e)
    {
        // no need to react on mouse moving
    }

    @Override
    public void onMouseUp(Event e)
    {
        // no need to react on mouse up
    }

    @Override
    public void paintControl(PaintEvent e)
    {
        if (spot == null)
            return;

        drawCrosshair(e, spot);
    }

    private void drawCrosshairDateTextbox(PaintEvent e, Spot spot, Point p)
    {
        LocalDate date = spot.date();
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

        e.gc.fillRoundRectangle(rectPoint.x, rectPoint.y, txtXExtend.x, txtXExtend.y, ChartToolsManager.PADDING,
                        ChartToolsManager.PADDING);
        e.gc.drawText(xText, textPoint.x, textPoint.y, true);
    }

    private void drawCrosshairValueTextbox(PaintEvent e, Spot spot, Point p)
    {
        String yText = chart.getToolTip().getDefaultValueFormat().format(spot.valueYRightAxis());

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

        e.gc.fillRoundRectangle(rectPoint.x, rectPoint.y, txtYExtend.x, txtYExtend.y, ChartToolsManager.PADDING,
                        ChartToolsManager.PADDING);
        e.gc.drawText(yText, textPoint.x, textPoint.y, true);
    }

    private void drawCrosshairValueSecondAxisTextbox(PaintEvent e, Spot spot, Point p)
    {
        IAxis axis = chart.getAxisSet().getYAxis(2);
        if (!axis.getTick().isVisible())
            return;

        var axisFormat = axis.getTick().getFormat();
        String yText = ""; //$NON-NLS-1$
        if (axisFormat != null)
        {
            yText = axisFormat.format(spot.valueYLeftAxis());
        }

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

        e.gc.fillRoundRectangle(rectPoint.x, rectPoint.y, txtYExtend.x, txtYExtend.y, ChartToolsManager.PADDING,
                        ChartToolsManager.PADDING);
        e.gc.drawText(yText, textPoint.x, textPoint.y, true);
    }

    private void drawCrosshair(PaintEvent e, Spot spot)
    {
        e.gc.setLineWidth(1);
        e.gc.setLineStyle(SWT.LINE_SOLID);
        e.gc.setForeground(color);
        e.gc.setBackground(color);
        e.gc.setAntialias(SWT.ON);

        Point point = spot.toPoint(chart);

        // draw crosshair
        e.gc.drawLine(point.x, 0, point.x, e.height);
        e.gc.drawLine(0, point.y, e.width, point.y);

        // set textbox style
        e.gc.setForeground(Colors.theme().defaultForeground());
        e.gc.setBackground(Colors.theme().defaultBackground());
        e.gc.setAlpha(220);

        // value for horizontal axis
        drawCrosshairDateTextbox(e, spot, point);

        drawCrosshairValueTextbox(e, spot, point);
        drawCrosshairValueSecondAxisTextbox(e, spot, point);
    }

}
