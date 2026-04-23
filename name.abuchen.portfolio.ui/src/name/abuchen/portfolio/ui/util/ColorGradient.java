package name.abuchen.portfolio.ui.util;

import java.util.function.Supplier;

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

    public static final ColorGradient RED_TO_GREEN = new ColorGradient(
                    () -> ColorGradientDefinitions.redToGreen().getGradient().getColors());
    public static final ColorGradient ORANGE_TO_BLUE = new ColorGradient(
                    () -> ColorGradientDefinitions.orangeToBlue().getGradient().getColors());
    public static final ColorGradient GREEN_YELLOW_RED = new ColorGradient(
                    () -> ColorGradientDefinitions.greenYellowRed().getGradient().getColors());
    public static final ColorGradient GREEN_WHITE_RED = new ColorGradient(
                    () -> ColorGradientDefinitions.greenWhiteRed().getGradient().getColors());
    public static final ColorGradient YELLOW_WHITE_BLACK = new ColorGradient(
                    () -> ColorGradientDefinitions.yellowWhiteBlack().getGradient().getColors());

    private final ColorPoint[] colors;
    private final Supplier<ColorPoint[]> dynamicColorsSupplier;

    public ColorGradient(ColorPoint... colors)
    {
        this.colors = colors;
        this.dynamicColorsSupplier = null;
    }

    public ColorGradient(Color... colors)
    {
        this.colors = new ColorPoint[colors.length];
        for (int ii = 0; ii < colors.length; ii++)
            this.colors[ii] = new ColorPoint(colors[ii], ii / (float) (colors.length - 1));
        this.dynamicColorsSupplier = null;
    }

    private ColorGradient(Supplier<ColorPoint[]> dynamicColorsSupplier)
    {
        this.colors = null;
        this.dynamicColorsSupplier = dynamicColorsSupplier;
    }

    private ColorPoint[] getColors()
    {
        return dynamicColorsSupplier != null ? dynamicColorsSupplier.get() : colors;
    }

    public Color getColorAt(float value)
    {
        var effectiveColors = getColors();

        if (value <= 0)
            return effectiveColors[0].color;

        if (value >= 1)
            return effectiveColors[effectiveColors.length - 1].color;

        for (int ii = 0; ii < effectiveColors.length; ii++)
        {
            var current = effectiveColors[ii];
            if (value <= current.position)
            {
                var previous = effectiveColors[Math.max(0, ii - 1)];
                var diff = current.position - previous.position;
                if (diff == 0)
                    return current.color;
                else
                    return new Color(Colors.interpolate(previous.color.getRGB(), current.color.getRGB(),
                                    (value - previous.position) / diff));
            }
        }

        return effectiveColors[effectiveColors.length - 1].color;
    }
}