package name.abuchen.portfolio.ui.util;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGBA;

public final class ColorGradientDefinitions
{
    public static final class Definition
    {
        private final String cssClass;
        private final Color[] colors;

        private Definition(String cssClass, Color... defaultColors)
        {
            this.cssClass = cssClass;
            this.colors = defaultColors;
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
                    Colors.getColor(201, 46, 37), // C92E25
                    Colors.getColor(253, 127, 118), // FD7F76
                    Colors.getColor(253, 154, 147), // FD9A93
                    Colors.getColor(252, 187, 183), // FCBBB7
                    Colors.getColor(232, 232, 232), // E8E8E8
                    Colors.getColor(187, 228, 145), // BBE491
                    Colors.getColor(161, 215, 113), // A1D771
                    Colors.getColor(128, 194, 94), // 80C25E
                    Colors.getColor(57, 123, 39) // 397B27
    );

    private static final Definition ORANGE_TO_BLUE = new Definition("orange-to-blue", //$NON-NLS-1$
                    Colors.getColor(179, 110, 58), // B36E3A
                    Colors.getColor(253, 156, 82), // FD9C52
                    Colors.getColor(255, 206, 170), // FFCEAA
                    Colors.getColor(221, 221, 221), // DDDDDD
                    Colors.getColor(158, 203, 236), // 9ECBEC
                    Colors.getColor(60, 151, 218), // 3C97DA
                    Colors.getColor(42, 105, 153) // 2A6999
    );

    private static final Definition GREEN_YELLOW_RED = new Definition("green-yellow-red", //$NON-NLS-1$
                    Colors.getColor(255, 43, 48), // FF2B30
                    Colors.getColor(255, 213, 98), // FFD562
                    Colors.getColor(2, 245, 0) // 02F500
    );

    private static final Definition GREEN_WHITE_RED = new Definition("green-white-red", //$NON-NLS-1$
                    Colors.getColor(255, 43, 48), // FF2B30
                    Colors.getColor(238, 238, 238), // EEEEEE
                    Colors.getColor(2, 245, 0) // 02F500
    );

    private static final Definition YELLOW_WHITE_BLACK = new Definition("yellow-white-black", //$NON-NLS-1$
                    Colors.getColor(238, 238, 238), // EEEEEE
                    Colors.getColor(255, 213, 98), // FFD562
                    Colors.getColor(128, 128, 0), // 808000
                    Colors.getColor(0, 0, 0) // 000000
    );

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
