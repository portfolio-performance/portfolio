package name.abuchen.portfolio.ui.util.viewers;

import java.util.function.Function;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Images;

public final class NumberColorLabelProvider<N extends Number> extends ColumnLabelProvider
{
    private final Values<N> format;
    private final Function<Object, N> provider;

    public NumberColorLabelProvider(Values<N> format, Function<Object, N> provider)
    {
        this.format = format;
        this.provider = provider;
    }

    @Override
    public Color getForeground(Object element)
    {
        Number value = provider.apply(element);
        if (value == null)
            return null;

        return Display.getCurrent()
                        .getSystemColor(value.doubleValue() >= 0 ? SWT.COLOR_DARK_GREEN : SWT.COLOR_DARK_RED);
    }

    @Override
    public Image getImage(Object element)
    {
        Number value = provider.apply(element);
        if (value == null)
            return null;

        return value.doubleValue() >= 0 ? Images.GREEN_ARROW.image() : Images.RED_ARROW.image();
    }

    @Override
    public String getText(Object element)
    {
        N value = provider.apply(element);
        if (value == null)
            return null;

        return format.format(value);
    }
}