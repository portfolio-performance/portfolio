package name.abuchen.portfolio.ui.util.viewers;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;

import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.jface.viewers.ViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;

import name.abuchen.portfolio.money.Values;

public abstract class SharesLabelProvider extends OwnerDrawLabelProvider
{
    private static final char POINT = new DecimalFormatSymbols().getDecimalSeparator();

    private final NumberFormat format = new DecimalFormat("#,##0.###"); //$NON-NLS-1$

    private ColumnViewer viewer;
    private TextLayout cachedTextLayout;

    public Color getForeground(Object element)
    {
        return null;
    }

    public Color getBackground(Object element)
    {
        return null;
    }

    public abstract Long getValue(Object element);

    @Override
    public String getToolTipText(Object element)
    {
        Long value = getValue(element);
        return value != null ? Values.Share.format(value) : null;
    }

    private TextLayout getSharedTextLayout(Display display)
    {
        if (this.cachedTextLayout == null)
        {
            int orientation = this.viewer.getControl().getStyle() & (SWT.LEFT_TO_RIGHT | SWT.RIGHT_TO_LEFT);
            this.cachedTextLayout = new TextLayout(display);
            this.cachedTextLayout.setOrientation(orientation);
        }
        return this.cachedTextLayout;
    }

    @Override
    protected void initialize(ColumnViewer viewer, ViewerColumn column)
    {
        this.viewer = viewer;
        super.initialize(viewer, column);
    }

    private Rectangle getSize(Event event, String text)
    {
        String s = text;
        int p = s.indexOf(POINT);
        if (p >= 0)
            s = s.substring(0, p);

        TextLayout textLayout = getSharedTextLayout(event.display);
        textLayout.setText(s + ",000"); //$NON-NLS-1$

        return textLayout.getBounds();
    }

    private Rectangle getBounds(Widget widget, int index)
    {
        if (widget instanceof TableItem)
            return ((TableItem) widget).getBounds(index);
        else if (widget instanceof TreeItem)
            return ((TreeItem) widget).getBounds(index);
        else
            throw new IllegalArgumentException();
    }

    @Override
    protected void measure(Event event, Object element)
    {
        Long value = getValue(element);
        if (value != null)
        {
            String text = format.format(value / Values.Share.divider());
            Rectangle size = getSize(event, text);

            Rectangle tableItem = getBounds(event.item, event.index);

            int width = Math.min(size.width, tableItem.width);

            event.setBounds(new Rectangle(event.x + tableItem.width - width, event.y, //
                            width, event.height));
        }
    }

    @Override
    protected void paint(Event event, Object element)
    {
        Rectangle tableItem = getBounds(event.item, event.index);
        boolean isSelected = (event.detail & SWT.SELECTED) != 0 || (event.detail & SWT.HOT) != 0;

        if (!isSelected)
            fillBackground(event, element, tableItem);

        Long value = getValue(element);
        if (value == null)
            return;

        Color oldForeground = null;
        Color newForeground = isSelected ? null : getForeground(element);
        if (newForeground != null)
        {
            oldForeground = event.gc.getForeground();
            event.gc.setForeground(newForeground);
        }

        String text = format.format(value / Values.Share.divider());
        Rectangle size = getSize(event, text);

        TextLayout textLayout = getSharedTextLayout(event.display);
        textLayout.setText(text);

        Rectangle layoutBounds = textLayout.getBounds();
        int x = event.x + tableItem.width - Math.min(size.width, tableItem.width);
        int y = event.y + Math.max(0, (tableItem.height - layoutBounds.height) / 2);

        textLayout.draw(event.gc, x, y);

        if (oldForeground != null)
            event.gc.setForeground(oldForeground);
    }

    private void fillBackground(Event event, Object element, Rectangle bounds)
    {
        Color newBackground = getBackground(element);
        if (newBackground != null)
        {
            Color oldBackground = event.gc.getBackground();
            event.gc.setBackground(newBackground);
            event.gc.fillRectangle(bounds.x, bounds.y, bounds.width, bounds.height);
            event.gc.setBackground(oldBackground);
        }
    }

    @Override
    protected void erase(Event event, Object element)
    {
        // use os-specific background
    }
}
