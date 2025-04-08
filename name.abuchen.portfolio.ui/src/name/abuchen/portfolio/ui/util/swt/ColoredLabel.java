package name.abuchen.portfolio.ui.util.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.ACC;
import org.eclipse.swt.accessibility.AccessibleControlAdapter;
import org.eclipse.swt.accessibility.AccessibleControlEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

import name.abuchen.portfolio.ui.util.Colors;

/**
 * Displays a label with given colors. The widget is not styled by CSS and
 * therefore retains the colors (which are typically based on a calculation). If
 * neither text nor background color is given, the default colors are used. If
 * only the backdrop color is given, a readable text color is picked. If only
 * the text color is given, the default background is used.
 */
public class ColoredLabel extends Canvas // NOSONAR
{
    private static final int MARGIN_HORIZONTAL = 2;
    private static final int MARGIN_VERTICAL = 1;

    private String text; // $NON-NLS-1$
    private int textStyle = SWT.LEFT;

    private Color textColor;
    private Color backdropColor;

    public ColoredLabel(Composite parent, int style)
    {
        super(parent, SWT.NONE);

        this.textStyle = style;

        initAccessibility();

        addListener(SWT.Paint, this::handlePaint);
    }

    public void setText(String text)
    {
        checkWidget();
        this.text = text;

        redraw();
    }

    public void setTextColor(Color color)
    {
        this.textColor = color;
    }

    public void setBackdropColor(Color color)
    {
        this.backdropColor = color;
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

        Color background = backdropColor != null ? backdropColor : getBackground();

        e.gc.setBackground(background);
        e.gc.fillRectangle(bounds.x, bounds.y, bounds.width, bounds.height);

        if (text != null)
        {
            e.gc.setFont(getFont());

            Color foreground = getForeground();
            if (textColor != null)
                foreground = textColor;
            else if (backdropColor != null)
                foreground = Colors.getTextColor(backdropColor);

            e.gc.setForeground(foreground);
            Point extent = e.gc.stringExtent(text);

            int offsetX = MARGIN_HORIZONTAL;
            if ((this.textStyle & SWT.RIGHT) == SWT.RIGHT)
                offsetX = bounds.width - extent.x - MARGIN_HORIZONTAL;
            else if ((this.textStyle & SWT.CENTER) == SWT.CENTER)
                offsetX = (bounds.width - extent.x) / 2;

            int offsetY = (bounds.height - extent.y) / 2;

            e.gc.drawText(text, offsetX, offsetY, true);
        }

        e.type = SWT.None;
    }

    private void initAccessibility()
    {
        getAccessible().addAccessibleControlListener(new AccessibleControlAdapter()
        {
            @Override
            public void getRole(AccessibleControlEvent e)
            {
                e.detail = ACC.ROLE_LABEL;
            }

            @Override
            public void getValue(AccessibleControlEvent e)
            {
                e.result = text;
            }
        });
    }
}
