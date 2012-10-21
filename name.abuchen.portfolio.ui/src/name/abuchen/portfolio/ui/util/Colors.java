package name.abuchen.portfolio.ui.util;

import org.eclipse.swt.graphics.RGB;

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
}
