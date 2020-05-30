package name.abuchen.portfolio.ui.util.viewers;

import java.util.function.Function;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.util.Colors;

public final class NumberColorLabelProvider<N extends Number> extends ColumnLabelProvider
{
    private final Values<N> format;
    private final Function<Object, N> label;
    private final Function<Object, String> tooltip;

    public NumberColorLabelProvider(Values<N> format, Function<Object, N> label)
    {
        this(format, label, null);
    }

    public NumberColorLabelProvider(Values<N> format, Function<Object, N> label, Function<Object, String> tooltip)
    {
        this.format = format;
        this.label = label;
        this.tooltip = tooltip;
    }

    @Override
    public Color getForeground(Object element)
    {
        Number value = label.apply(element);
        if (value == null)
            return null;

        return value.doubleValue() >= 0 ? Colors.DARK_GREEN : Colors.DARK_RED;
    }

    @Override
    public Image getImage(Object element)
    {
        Number value = label.apply(element);
        if (value == null)
            return null;

        return value.doubleValue() >= 0 ? Images.GREEN_ARROW.image() : Images.RED_ARROW.image();
    }

    @Override
    public String getText(Object element)
    {
        N value = label.apply(element);
        if (value == null)
            return null;

        return format.format(value);
    }

    @Override
    public String getToolTipText(Object element)
    {
        return tooltip != null ? tooltip.apply(element) : null;
    }

}
