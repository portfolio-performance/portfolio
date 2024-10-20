package name.abuchen.portfolio.ui.views.dashboard.heatmap;

import java.util.function.DoubleFunction;

import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.Colors;

enum ColorSchema
{
    GREEN_YELLOW_RED(Messages.LabelGreenYellowRed), //
    GREEN_WHITE_RED(Messages.LabelGreenWhiteRed), //
    GREEN_GRAY_RED(Messages.LabelGreenGrayRed), //
    BLUE_GRAY_ORANGE(Messages.LabelBlueGrayOrange), //
    YELLOW_WHITE_BLACK(Messages.LabelYellowWhiteBlack);

    private static final double MAX_PERFORMANCE = 0.07f;

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
                double p = normalizePerformance(performance);
                float hue = (float) p * 120f;

                return resourceManager.createColor(new RGB(hue, 0.9f, 1f));
            };

            case GREEN_WHITE_RED -> performance -> {
                // Performance is normalized and interpolated between green
                // (positive) and the background (neutral) or red (negative)
                double p = Math.min(MAX_PERFORMANCE, Math.abs(performance)) / MAX_PERFORMANCE;

                RGB color = performance > 0f
                                ? Colors.interpolate(Colors.theme().defaultBackground().getRGB(),
                                                Colors.HEATMAP_DARK_GREEN.getRGB(), (float) p)
                                : Colors.interpolate(Colors.theme().defaultBackground().getRGB(), Colors.RED.getRGB(),
                                                (float) p);

                return resourceManager.createColor(color);
            };

            case GREEN_GRAY_RED -> performance -> {
                // Performance interpolates between green (positive) and gray
                // (neutral) or red (negative)
                double p = normalizePerformance(performance);

                RGB color = performance > 0
                                ? Colors.interpolate(Colors.GRAY.getRGB(), Colors.HEATMAP_DARK_GREEN.getRGB(),
                                                (float) p)
                                : Colors.interpolate(Colors.GRAY.getRGB(), Colors.RED.getRGB(), (float) p);

                return resourceManager.createColor(color);
            };

            case BLUE_GRAY_ORANGE -> performance -> {
                // Performance interpolates between blue(stable) and gray
                // (neutral) or orange (volatile)
                double p = normalizePerformance(performance);

                RGB color = performance > 0 ? Colors.interpolate(Colors.GRAY.getRGB(), Colors.BLUE.getRGB(), (float) p)
                                : Colors.interpolate(Colors.GRAY.getRGB(), Colors.HEATMAP_ORANGE.getRGB(), (float) p);

                return resourceManager.createColor(color);
            };

            case YELLOW_WHITE_BLACK -> performance -> {
                // Performance interpolates between yellow (moderate) and white
                // (neutral) or black (extreme)
                double p = normalizePerformance(performance);

                RGB color = performance > 0.05
                                ? Colors.interpolate(Colors.YELLOW.getRGB(), Colors.BLACK.getRGB(),
                                                (float) ((p - 0.05) / 0.95))
                                : Colors.interpolate(Colors.WHITE.getRGB(), Colors.YELLOW.getRGB(), (float) p);

                return resourceManager.createColor(color);
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
    private double normalizePerformance(double performance)
    {
        performance = Math.max(-MAX_PERFORMANCE, performance);
        performance = Math.min(MAX_PERFORMANCE, performance);
        return (performance + MAX_PERFORMANCE) / (2 * MAX_PERFORMANCE);
    }

    @Override
    public String toString()
    {
        return label;
    }
}
