package name.abuchen.portfolio.ui.util;

import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.util.ColorConversion;

public final class Colors
{

    public static final Color GRAY = Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
    public static final Color WHITE = Display.getDefault().getSystemColor(SWT.COLOR_WHITE);
    public static final Color DARK_GRAY = Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
    public static final Color DARK_RED = Display.getDefault().getSystemColor(SWT.COLOR_DARK_RED);
    public static final Color DARK_GREEN = Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN);
    public static final Color BLACK = Display.getDefault().getSystemColor(SWT.COLOR_BLACK);

    private static final ColorRegistry REGISTRY = new ColorRegistry();

    public static final Color TOTALS = getColor(0, 0, 0);

    public static final Color CASH = getColor(196, 55, 194);
    public static final Color EQUITY = getColor(87, 87, 255);

    public static final Color CPI = getColor(120, 120, 120);
    public static final Color IRR = getColor(0, 0, 0);

    public static final Color DARK_BLUE = getColor(149, 165, 180); // 95A5B4

    public static final Color HEADINGS = getColor(57, 62, 66); // 95A5B4
    public static final Color OTHER_CATEGORY = getColor(180, 180, 180);
    public static final Color INFO_TOOLTIP_BACKGROUND = getColor(236, 235, 236);

    public static final Color WARNING = getColor(254, 223, 107);

    public static final Color SIDEBAR_TEXT = getColor(57, 62, 66);
    public static final Color SIDEBAR_BACKGROUND = getColor(249, 250, 250);
    public static final Color SIDEBAR_BACKGROUND_SELECTED = getColor(228, 230, 233);
    public static final Color SIDEBAR_BORDER = getColor(244, 245, 245);

    private Colors()
    {}

    public static Color getColor(RGB rgb)
    {
        return getColor(rgb.red, rgb.green, rgb.blue);
    }

    /**
     * Constructs a color instance with the given red, green and blue values.
     *
     * @param red
     *            the red component of the new instance
     * @param green
     *            the green component of the new instance
     * @param blue
     *            the blue component of the new instance
     * @exception IllegalArgumentException
     *                if the red, green or blue argument is not between 0 and
     *                255
     */
    public static Color getColor(int red, int green, int blue)
    {
        String key = getColorKey(red, green, blue);
        if (REGISTRY.hasValueFor(key))
        {
            return REGISTRY.get(key);
        }
        else
        {
            REGISTRY.put(key, new RGB(red, green, blue));
            return getColor(key);
        }
    }

    private static Color getColor(String key)
    {
        return REGISTRY.get(key);
    }

    private static String getColorKey(int red, int green, int blue)
    {
        return red + "_" + green + "_" + blue; //$NON-NLS-1$ //$NON-NLS-2$
    }

    public static String toHex(Color color)
    {
        return ColorConversion.toHex(color.getRed(), color.getGreen(), color.getBlue());
    }

    public static String toHex(RGB rgb)
    {
        return ColorConversion.toHex(rgb.red, rgb.green, rgb.blue);
    }

    public static RGB toRGB(String hex)
    {
        int[] rgb = ColorConversion.toRGB(hex);
        return new RGB(rgb[0], rgb[1], rgb[2]);
    }

    /**
     * Returns an appropriate text color (black or white) for the given
     * background color.
     */
    public static Color getTextColor(Color color)
    {
        // http://stackoverflow.com/questions/596216/formula-to-determine-brightness-of-rgb-color

        double luminance = 1 - (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue()) / 255;
        return luminance < 0.2 ? BLACK : WHITE;
    }

}
