package name.abuchen.portfolio.ui.util;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGBA;

public final class ColorGradientDefinitions
{
    public static final class Definition
    {
        private final String cssClass;
        private final Color[] colors;

        private Definition(String cssClass, int size)
        {
            this.cssClass = cssClass;
            this.colors = new Color[size];
        }

        public String getCssClass()
        {
            return cssClass;
        }

        public ColorGradient getGradient()
        {
            for (Color color : colors)
            {
                if (color == null)
                    throw new IllegalStateException("ColorGradient '" + cssClass + "' is not initialized from CSS"); //$NON-NLS-1$ //$NON-NLS-2$
            }

            return new ColorGradient(colors);
        }

        public void setColor(int index, RGBA color)
        {
            colors[index] = Colors.getColor(color.rgb);
        }
    }

    private static final Definition RED_TO_GREEN = new Definition("red-to-green", 9); //$NON-NLS-1$

    private static final Definition ORANGE_TO_BLUE = new Definition("orange-to-blue", 7); //$NON-NLS-1$

    private static final Definition GREEN_YELLOW_RED = new Definition("green-yellow-red", 3); //$NON-NLS-1$

    private static final Definition GREEN_WHITE_RED = new Definition("green-white-red", 3); //$NON-NLS-1$

    private static final Definition YELLOW_WHITE_BLACK = new Definition("yellow-white-black", 4); //$NON-NLS-1$

    private ColorGradientDefinitions()
    {
    }

    public static Definition redToGreen()
    {
        return RED_TO_GREEN;
    }

    public static Definition orangeToBlue()
    {
        return ORANGE_TO_BLUE;
    }

    public static Definition greenYellowRed()
    {
        return GREEN_YELLOW_RED;
    }

    public static Definition greenWhiteRed()
    {
        return GREEN_WHITE_RED;
    }

    public static Definition yellowWhiteBlack()
    {
        return YELLOW_WHITE_BLACK;
    }
}