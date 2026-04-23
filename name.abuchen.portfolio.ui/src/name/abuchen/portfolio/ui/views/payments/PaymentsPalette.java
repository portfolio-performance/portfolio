package name.abuchen.portfolio.ui.views.payments;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGBA;

import name.abuchen.portfolio.ui.util.Colors;

public final class PaymentsPalette
{
    private static final int SIZE = 15;

    private static final PaymentsPalette INSTANCE = new PaymentsPalette();

    private final Color[] colors = new Color[SIZE];

    private PaymentsPalette()
    {
    }

    public static PaymentsPalette instance()
    {
        return INSTANCE;
    }

    public int size()
    {
        return SIZE;
    }

    public Color get(int index)
    {
        return requireConfigured(index);
    }

    public Color getCyclic(int index)
    {
        return requireConfigured(index % colors.length);
    }

    private Color requireConfigured(int index)
    {
        if (colors[index] == null)
            throw new IllegalStateException(
                            "CSS payments palette color not configured: ColorPalette.payments.color-" + index); //$NON-NLS-1$

        return colors[index];
    }

    public void setColor(int index, RGBA color)
    {
        colors[index] = Colors.getColor(color.rgb);
    }
}
