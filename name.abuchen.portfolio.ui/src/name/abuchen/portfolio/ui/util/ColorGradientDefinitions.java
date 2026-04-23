package name.abuchen.portfolio.ui.util;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGBA;

public final class ColorGradientDefinitions
{
    public static final class Definition
    {
        private final String cssClass;
        private final Color[] colors;

        private Definition(String cssClass, Color... colors)
        {
            this.cssClass = cssClass;
            this.colors = colors;
        }

        public String getCssClass()
        {
            return cssClass;
        }

        public ColorGradient getGradient()
        {
            return new ColorGradient(colors);
        }

        public void setColor(int index, RGBA color)
        {
            colors[index] = Colors.getColor(color.rgb);
        }
    }

    private static final Definition RED_TO_GREEN = new Definition("red-to-green", //$NON-NLS-1$
                    Colors.getColor(201, 46, 37), //
                    Colors.getColor(253, 127, 118), //
                    Colors.getColor(253, 154, 147), //
                    Colors.getColor(252, 187, 183), //
                    Colors.getColor(232, 232, 232), //
                    Colors.getColor(187, 228, 145), //
                    Colors.getColor(161, 215, 113), //
                    Colors.getColor(128, 194, 94), //
                    Colors.getColor(57, 123, 39));

    private static final Definition ORANGE_TO_BLUE = new Definition("orange-to-blue", //$NON-NLS-1$
                    Colors.getColor(179, 110, 58), //
                    Colors.getColor(253, 156, 82), //
                    Colors.getColor(255, 206, 170), //
                    Colors.getColor(221, 221, 221), //
                    Colors.getColor(158, 203, 236), //
                    Colors.getColor(60, 151, 218), //
                    Colors.getColor(42, 105, 153));

    private static final Definition GREEN_YELLOW_RED = new Definition("green-yellow-red", //$NON-NLS-1$
                    Colors.getColor(255, 0, 0), //
                    Colors.getColor(255, 255, 0), //
                    Colors.getColor(0, 255, 0));

    private static final Definition GREEN_WHITE_RED = new Definition("green-white-red", //$NON-NLS-1$
                    Colors.getColor(255, 0, 0), //
                    Colors.getColor(255, 255, 255), //
                    Colors.getColor(104, 229, 23));

    private static final Definition YELLOW_WHITE_BLACK = new Definition("yellow-white-black", //$NON-NLS-1$
                    Colors.getColor(255, 255, 255), //
                    Colors.getColor(255, 255, 0), //
                    Colors.getColor(91, 91, 0), //
                    Colors.getColor(0, 0, 0));

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