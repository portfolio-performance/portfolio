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
    GREEN_WHITE_RED(Messages.LabelGreenWhiteRed);

    private String label;

    private ColorSchema(String label)
    {
        this.label = label;
    }

    @Override
    public String toString()
    {
        return label;
    }

    /* package */ DoubleFunction<Color> buildColorFunction(ResourceManager resourceManager)
    {
        switch (this)
        {
            case GREEN_YELLOW_RED:
                return performance -> {
                    // convert to 0 = -0.07 -> 1 = +0.07
                    final double max = 0.07f;

                    double p = performance;
                    p = Math.max(-max, p);
                    p = Math.min(max, p);
                    p = (p + max) * (1 / (2 * max));

                    // 0 = red, 60 = yellow, 120 = red
                    float hue = (float) p * 120f;
                    return resourceManager.createColor(new RGB(hue, 0.9f, 1f));
                };
            case GREEN_WHITE_RED:
                return performance -> {

                    final double max = 0.07f;

                    double p = performance;
                    p = Math.min(max, Math.abs(p));
                    p = p / max;

                    RGB color = performance > 0f ? Colors.GREEN.getRGB() : Colors.RED.getRGB();
                    return resourceManager.createColor(
                                    Colors.interpolate(Colors.theme().defaultBackground().getRGB(), color, (float) p));
                };
            default:
                throw new IllegalArgumentException();
        }
    }

}
