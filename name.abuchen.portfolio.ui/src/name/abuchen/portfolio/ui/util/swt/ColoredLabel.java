package name.abuchen.portfolio.ui.util.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

import name.abuchen.portfolio.ui.util.Colors;

/**
 * Displays a label with the given background color and a readable text color
 * calculated based on the brightness of the background color. Most importantly,
 * the widget is not styled via CSS and therefore retains the colors.
 */
public class ColoredLabel extends Canvas // NOSONAR
{
    private static final int MARGIN_HORIZONTAL = 2;
    private static final int MARGIN_VERTICAL = 1;

    private String text; // $NON-NLS-1$
    private Color color;

    public ColoredLabel(Composite parent, int style)
    {
        super(parent, style);

        addListener(SWT.Paint, this::handlePaint);
    }

    public void setText(String text)
    {
        checkWidget();
        this.text = text;

        redraw();
    }

    public void setHightlightColor(Color color)
    {
        this.color = color;
    }

    @Override
    public Point computeSize(int wHint, int hHint, boolean changed)
    {
        if (text == null)
            return new Point(0, 0);

        GC gc = new GC(this);
        gc.setFont(getFont());
        Point extent = gc.stringExtent(text);
        gc.dispose();

        return new Point(wHint == SWT.DEFAULT ? extent.x + 2 * MARGIN_HORIZONTAL : wHint,
                        hHint == SWT.DEFAULT ? extent.y + 2 * MARGIN_VERTICAL : hHint);
    }

    private void handlePaint(Event e)
    {
        Rectangle bounds = getClientArea();

        int style = getStyle();

        if (color != null)
        {
            e.gc.setBackground(color);
            e.gc.fillRectangle(bounds.x, bounds.y, bounds.width, bounds.height);
        }

        if (text != null)
        {
            e.gc.setFont(getFont());
            e.gc.setForeground(Colors.getTextColor(color));

            int offset = MARGIN_HORIZONTAL;
            if ((style & SWT.RIGHT) == SWT.RIGHT || (style & SWT.CENTER) == SWT.CENTER)
            {
                int width = e.gc.stringExtent(text).x;
                if ((style & SWT.RIGHT) == SWT.RIGHT)
                    offset = bounds.width - width - MARGIN_HORIZONTAL;
                else if ((style & SWT.CENTER) == SWT.CENTER)
                    offset = (bounds.width - width) / 2 + MARGIN_HORIZONTAL;
            }

            e.gc.drawText(text, offset, MARGIN_VERTICAL, true);
        }

        e.type = SWT.None;
    }
}
