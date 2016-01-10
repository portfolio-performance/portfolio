package name.abuchen.portfolio.ui.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.util.ColorConversion;

public enum Colors
{
    TOTALS(0, 0, 0), //

    CASH(196, 55, 194), //
    DEBT(220, 161, 34), //
    EQUITY(87, 87, 255), //
    REAL_ESTATE(253, 106, 14), //
    COMMODITY(87, 159, 87), //

    CPI(120, 120, 120), //
    IRR(0, 0, 0), //

    HEADINGS(149, 165, 180), // 95A5B4
    OTHER_CATEGORY(180, 180, 180), //
    INFO_TOOLTIP_BACKGROUND(236, 235, 236),

    WARNING(254, 223, 107);

    private final int red;
    private final int green;
    private final int blue;

    private Colors(int red, int green, int blue)
    {
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    public int red()
    {
        return this.red;
    }

    public int green()
    {
        return this.green;
    }

    public int blue()
    {
        return this.blue;
    }

    public RGB swt()
    {
        return new RGB(red, green, blue);
    }

    public String asHex()
    {
        return toHex(swt());
    }

    public static String toHex(RGB rgb)
    {
        return ColorConversion.toHex(rgb);
    }

    public static RGB toRGB(String hex)
    {
        int rgb[] = ColorConversion.toRGB(hex);
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

        if (luminance < 0.2)
            return Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
        else
            return Display.getDefault().getSystemColor(SWT.COLOR_WHITE);
    }

}
