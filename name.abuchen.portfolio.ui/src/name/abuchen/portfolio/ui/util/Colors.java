package name.abuchen.portfolio.ui.util;

import org.eclipse.swt.graphics.RGB;

public enum Colors
{
    TOTALS(0, 0, 0), //

    CASH(196, 55, 194), //
    BOND(220, 161, 34), //
    STOCK(87, 87, 255), //
    REAL_ESTATE(253, 106, 14), //
    COMMODITY(87, 159, 87), //

    CPI(39, 196, 39), //
    IRR(0, 0, 0);

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
}
