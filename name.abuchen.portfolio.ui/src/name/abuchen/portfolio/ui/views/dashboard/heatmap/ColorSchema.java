package name.abuchen.portfolio.ui.views.dashboard.heatmap;

import java.util.function.DoubleFunction;

import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.ColorGradient;
import name.abuchen.portfolio.ui.util.ColorGradient.ColorPoint;
import name.abuchen.portfolio.ui.util.Colors;

enum ColorSchema
{
    GREEN_YELLOW_RED(Messages.LabelGreenYellowRed), //
    GREEN_WHITE_RED(Messages.LabelGreenWhiteRed), //
    GREEN_GRAY_RED(Messages.LabelGreenGrayRed), //
    BLUE_GRAY_ORANGE(Messages.LabelBlueGrayOrange), //
    YELLOW_WHITE_BLACK(Messages.LabelYellowWhiteBlack);

    private static final float MAX_PERFORMANCE = 0.07f;

    private String label;

    private ColorSchema(String label)
    {
        this.label = label;
    }

    DoubleFunction<Color> buildColorFunction(ResourceManager resourceManager)
    {
        return switch (this)
        {
            case GREEN_YELLOW_RED -> performance -> {
                // Normalize performance between -0.07 and +0.07 and map it to a
                // hue-like scale between 0 (red) and 60 (yellow) or 120
                // (green).
                var p = normalizePerformance(performance);
                var hue = p * 120f;

                return resourceManager.createColor(new RGB(hue, 0.9f, 1f));
            };

            case GREEN_WHITE_RED -> performance -> {
                var p = normalizePerformance(performance);

                return new ColorGradient(//
                                Colors.RED, //
                                Colors.theme().defaultBackground(), //
                                Colors.HEATMAP_DARK_GREEN //
                ).getColorAt(p);
            };

            case GREEN_GRAY_RED -> performance -> {
                var p = normalizePerformance(performance);
                return ColorGradient.RED_TO_GREEN.getColorAt(p);
            };

            case BLUE_GRAY_ORANGE -> performance -> {
                var p = normalizePerformance(performance);
                return ColorGradient.ORANGE_TO_BLUE.getColorAt(p);
            };

            case YELLOW_WHITE_BLACK -> performance -> {
                var p = normalizePerformance(performance);

                // cutover from yellow to black at +0.05 performance
                var cutover = (MAX_PERFORMANCE + 0.05f) / (2f * MAX_PERFORMANCE);

                return new ColorGradient(//
                                new ColorPoint(Colors.theme().defaultBackground(), 0), //
                                new ColorPoint(Colors.YELLOW, cutover), //
                                new ColorPoint(Colors.getColor(91, 91, 0), cutover), //
                                new ColorPoint(Colors.BLACK, 1) //
                ).getColorAt(p);
            };

            default -> throw new IllegalArgumentException("Unsupported color schema: " + this); //$NON-NLS-1$
        };
    }

    /**
     * Normalizes the performance value between -MAX_PERFORMANCE and
     * +MAX_PERFORMANCE, and scales it to a 0 to 1 range for color
     * interpolation.
     *
     * @param performance
     *            the input performance value
     * @return a normalized performance value between 0 and 1
     */
    private float normalizePerformance(double performance)
    {
        performance = Math.max(-MAX_PERFORMANCE, performance);
        performance = Math.min(MAX_PERFORMANCE, performance);
        return (float) ((performance + MAX_PERFORMANCE) / (2 * MAX_PERFORMANCE));
    }

    @Override
    public String toString()
    {
        return label;
    }
}
