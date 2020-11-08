package name.abuchen.portfolio.ui.util;

import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.RGBA;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.util.ColorConversion;

public final class Colors
{
    /**
     * Theme holds the colors that a themed via CSS. Because Eclipse 4.16
     * disposes (still) disposes colors upon theme change, we just inject the
     * RGB values and cache the colors here.
     */
    public static class Theme
    {
        private Color defaultForeground = Colors.BLACK;
        private Color defaultBackground = Colors.WHITE;
        private Color warningBackground = getColor(254, 223, 107); // FEDF6B
        private Color redBackground = Colors.GREEN;
        private Color greenBackground = Colors.RED;
        private Color redForeground = Colors.DARK_RED;
        private Color greenForeground = Colors.DARK_GREEN;
        private Color hyperlink = Display.getDefault().getSystemColor(SWT.COLOR_LINK_FOREGROUND);

        public Color defaultForeground()
        {
            return defaultForeground;
        }

        public void setDefaultForeground(RGBA color)
        {
            this.defaultForeground = getColor(color.rgb);
        }

        public Color defaultBackground()
        {
            return defaultBackground;
        }

        public void setDefaultBackground(RGBA color)
        {
            this.defaultBackground = getColor(color.rgb);
        }

        public Color warningBackground()
        {
            return warningBackground;
        }

        public void setWarningBackground(RGBA color)
        {
            this.warningBackground = getColor(color.rgb);
        }

        public Color redBackground()
        {
            return redBackground;
        }

        public void setRedBackground(RGBA color)
        {
            this.redBackground = getColor(color.rgb);
        }

        public Color greenBackground()
        {
            return greenBackground;
        }

        public void setGreenBackground(RGBA color)
        {
            this.greenBackground = getColor(color.rgb);
        }

        public Color redForeground()
        {
            return redForeground;
        }

        public void setRedForeground(RGBA color)
        {
            this.redForeground = getColor(color.rgb);
        }

        public Color greenForeground()
        {
            return greenForeground;
        }

        public void setGreenForeground(RGBA color)
        {
            this.greenForeground = getColor(color.rgb);
        }

        public Color hyperlink()
        {
            return hyperlink;
        }

        public void setHyperlink(RGBA color)
        {
            this.hyperlink = getColor(color.rgb);
        }
    }

    public static final Color GRAY = Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
    public static final Color WHITE = Display.getDefault().getSystemColor(SWT.COLOR_WHITE);
    public static final Color DARK_GRAY = Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
    public static final Color DARK_RED = Display.getDefault().getSystemColor(SWT.COLOR_DARK_RED);
    public static final Color DARK_GREEN = Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN);
    public static final Color BLACK = Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
    public static final Color RED = Display.getDefault().getSystemColor(SWT.COLOR_RED);
    public static final Color GREEN = Display.getDefault().getSystemColor(SWT.COLOR_GREEN);

    private static final ColorRegistry REGISTRY = new ColorRegistry();

    public static final Color ICON_ORANGE = getColor(241, 143, 1); // F18F01
    public static final Color ICON_BLUE = getColor(14, 110, 142);
    public static final Color ICON_GREEN = getColor(154, 193, 85); // 9AC155

    public static final Color TOTALS = getColor(0, 0, 0);

    public static final Color CASH = getColor(196, 55, 194);
    public static final Color EQUITY = getColor(87, 87, 255);

    public static final Color CPI = getColor(120, 120, 120);
    public static final Color IRR = getColor(0, 0, 0);

    public static final Color DARK_BLUE = getColor(149, 165, 180); // 95A5B4

    public static final Color HEADINGS = getColor(57, 62, 66); // 393E42
    public static final Color OTHER_CATEGORY = getColor(180, 180, 180);
    public static final Color INFO_TOOLTIP_BACKGROUND = getColor(236, 235, 236);

    public static final Color SIDEBAR_TEXT = getColor(57, 62, 66); // 393E42
    public static final Color SIDEBAR_BACKGROUND = getColor(249, 250, 250); // F9FAFA
    public static final Color SIDEBAR_BACKGROUND_SELECTED = getColor(228, 230, 233); // E4E6E9
    public static final Color SIDEBAR_BORDER = getColor(244, 245, 245); // F4F5F5

    private static final Theme theme = new Theme();

    private Colors()
    {
    }

    public static Theme theme()
    {
        return theme;
    }

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
        return luminance < 0.4 ? BLACK : WHITE;
    }

    public static Color brighter(Color base)
    {
        return getColor(ColorConversion.brighter(base.getRGB()));
    }

    public static RGB interpolate(RGB first, RGB second, float factor)
    {
        int red = Math.round(first.red + factor * (second.red - first.red));
        int green = Math.round(first.green + factor * (second.green - first.green));
        int blue = Math.round(first.blue + factor * (second.blue - first.blue));

        return new RGB(red, green, blue);
    }
}
