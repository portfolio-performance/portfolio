package name.abuchen.portfolio.ui.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

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

    HEADINGS(149, 165, 180), //
    OTHER_CATEGORY(180, 180, 180);

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
        return '#' + Integer.toHexString((rgb.red << 16) | (rgb.green << 8) | rgb.blue);
    }

    public static String toHex(float[] hsb)
    {
        return toHex(new RGB(hsb[0], hsb[1], hsb[2]));
    }

    public static RGB toRGB(String hex)
    {
        try
        {
            Integer intval = Integer.decode(hex);
            int i = intval.intValue();
            return new RGB((i >> 16) & 0xFF, (i >> 8) & 0xFF, i & 0xFF);
        }
        catch (NumberFormatException ignore)
        {
            return Display.getDefault().getSystemColor(SWT.COLOR_BLACK).getRGB();
        }
    }

}
