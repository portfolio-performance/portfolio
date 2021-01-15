package name.abuchen.portfolio.ui.util.viewers;

import java.util.function.Function;

import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.jface.viewers.ViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;

import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.util.Colors;

public final class NumberColorLabelProvider<N extends Number> extends OwnerDrawLabelProvider
{
    private final Function<N, String> format;
    private final Function<Object, N> label;
    private final Function<Object, String> tooltip;

    private ColumnViewer viewer;
    private TextLayout cachedTextLayout;

    public NumberColorLabelProvider(Function<N, String> format, Function<Object, N> label)
    {
        this(format, label, null);
    }

    public NumberColorLabelProvider(Function<N, String> format, Function<Object, N> label, Function<Object, String> tooltip)
    {
        this.format = format;
        this.label = label;
        this.tooltip = tooltip;
    }

    public Color getForeground(Object element)
    {
        Number value = label.apply(element);
        if (value == null)
            return null;

        return value.doubleValue() >= 0 ? Colors.theme().greenForeground() : Colors.theme().redForeground();
    }

    public Image getImage(Object element)
    {
        Number value = label.apply(element);
        if (value == null)
            return null;

        return value.doubleValue() >= 0 ? Images.GREEN_ARROW.image() : Images.RED_ARROW.image();
    }

    public String getText(Object element)
    {
        N value = label.apply(element);
        if (value == null)
            return null;

        return format.apply(value);
    }

    @Override
    public String getToolTipText(Object element)
    {
        return tooltip != null ? tooltip.apply(element) : null;
    }

    @Override
    protected void initialize(ColumnViewer viewer, ViewerColumn column)
    {
        this.viewer = viewer;
        super.initialize(viewer, column);
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

    private Rectangle getImageBounds(Widget widget, int index)
    {
        if (widget instanceof TableItem)
            return ((TableItem) widget).getImageBounds(index);
        else if (widget instanceof TreeItem)
            return ((TreeItem) widget).getImageBounds(index);
        else
            throw new IllegalArgumentException();
    }

    private Rectangle getTextBounds(Widget widget, int index)
    {
        if (widget instanceof TableItem)
            return ((TableItem) widget).getTextBounds(index);
        else if (widget instanceof TreeItem)
            return ((TreeItem) widget).getTextBounds(index);
        else
            throw new IllegalArgumentException();
    }

    private Rectangle getSize(Event event, String text)
    {
        StringBuilder builder = new StringBuilder(text);
        builder.append(' ');

        TextLayout textLayout = getSharedTextLayout(event.display);
        textLayout.setText(builder.toString());

        return textLayout.getBounds();
    }

    @Override
    protected void measure(Event event, Object element)
    {
        String text = getText(element);
        Rectangle textSize = text == null ? new Rectangle(0, 0, 0, 0) : getSize(event, text);

        Image image = getImage(element);
        Rectangle imageSize = image == null ? new Rectangle(0, 0, 0, 0) : image.getBounds();

        int width = imageSize.width + textSize.width;
        int height = Math.max(textSize.height, imageSize.height);
        event.setBounds(new Rectangle(event.x, event.y, width, height));
    }

    @Override
    public void paint(Event event, Object element)
    {
        Rectangle tableItem = getBounds(event.item, event.index);

        Color oldForeground = null;
        Color newForeground = getForeground(element);
        if (newForeground != null)
        {
            oldForeground = event.gc.getForeground();
            event.gc.setForeground(newForeground);
        }

        Rectangle imageBounds = getImageBounds(event.item, event.index);
        Image image = getImage(element);
        if (image != null)
        {
            Rectangle bounds = image.getBounds();
            int x = imageBounds.x + Math.max(0, (imageBounds.width - bounds.height) / 2);
            int y = imageBounds.y + Math.max(0, (imageBounds.height - bounds.height) / 2);
            event.gc.drawImage(image, x, y);
        }

        Rectangle textBounds = getTextBounds(event.item, event.index);
        String text = getText(element);
        if (text != null)
        {
            Rectangle size = getSize(event, text);
            TextLayout textLayout = getSharedTextLayout(event.display);
            textLayout.setText(text);

            Rectangle layoutBounds = textLayout.getBounds();
            int afterImageX = image == null ? textBounds.x : (textBounds.x + image.getBounds().width);
            int raggedRightX = event.x + tableItem.width - size.width;
            int x = Math.max(afterImageX, raggedRightX);
            int y = textBounds.y + Math.max(0, (textBounds.height - layoutBounds.height) / 2);

            textLayout.draw(event.gc, x, y);
        }

        if (oldForeground != null)
            event.gc.setForeground(oldForeground);
    }

    @Override
    protected void erase(Event event, Object element)
    {
        // use os-specific background
    }
}
