package name.abuchen.portfolio.ui.util;

import org.eclipse.swt.graphics.Color;

/**
 * Inspired by
 * http://www.andrewnoske.com/wiki/Code_-_heatmaps_and_color_gradients
 */
public class ColorGradient
{
    public static class ColorPoint
    {
        public final Color color;
        public final float position;

        public ColorPoint(Color color, float position)
        {
            this.color = color;
            this.position = position;
        }
    }

    public static final ColorGradient RED_TO_GREEN = new ColorGradient( //
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

    public static final ColorGradient ORANGE_TO_BLUE = new ColorGradient( //
                    Colors.getColor(179, 110, 58), // B36E3A
                    Colors.getColor(253, 156, 82), // FD9C52
                    Colors.getColor(255, 206, 170), // FFCEAA
                    Colors.getColor(221, 221, 221), // DDDDDD
                    Colors.getColor(158, 203, 236), // 9ECBEC
                    Colors.getColor(60, 151, 218), // 3C97DA
                    Colors.getColor(42, 105, 153) // 2A6999
    );

    private final ColorPoint[] colors;

    public ColorGradient(ColorPoint... colors)
    {
        this.colors = colors;
    }

    public ColorGradient(Color... colors)
    {
        this.colors = new ColorPoint[colors.length];
        for (int ii = 0; ii < colors.length; ii++)
            this.colors[ii] = new ColorPoint(colors[ii], ii / (float) (colors.length - 1));
    }

    public Color getColorAt(float value)
    {
        if (value <= 0)
            return colors[0].color;

        if (value >= 1)
            return colors[colors.length - 1].color;

        for (int ii = 0; ii < colors.length; ii++)
        {
            var current = colors[ii];
            if (value <= current.position)
            {
                var previous = colors[Math.max(0, ii - 1)];
                var diff = current.position - previous.position;
                if (diff == 0)
                    return current.color;
                else
                    return new Color(Colors.interpolate(previous.color.getRGB(), current.color.getRGB(),
                                    (value - previous.position) / diff));
            }
        }

        return colors[colors.length - 1].color;
    }
}
