package name.abuchen.portfolio.ui.util.swt;

import org.eclipse.swt.graphics.Color;

import name.abuchen.portfolio.ui.util.ColorGradient;
import name.abuchen.portfolio.ui.util.Colors;

public enum CorrelationColorSchema
{
    SUBTLE_RED_WHITE_GREEN;

    private static final ColorGradient SUBTLE_GRADIENT = new ColorGradient(Colors.getColor(255, 205, 205), // light
                                                                                                           // red
                    Colors.theme().defaultBackground(), Colors.getColor(205, 245, 205) // light
                                                                                       // green
    );

    public Color getColor(double correlation)
    {
        if (Double.isNaN(correlation))
            return Colors.theme().grayForeground();

        if (Math.abs(correlation) < 0.5d)
            return Colors.theme().defaultBackground();

        return SUBTLE_GRADIENT.getColorAt(normalize(correlation));
    }

    private float normalize(double correlation)
    {
        correlation = Math.max(-1d, Math.min(1d, correlation));

        // invertiert: +1 = rot
        float p = (float) ((1d - correlation) / 2d);

        // 4 Stufen
        int steps = 4;
        int bucket = (int) Math.floor(p * steps);

        // Korrektur für p = 1.0
        if (bucket == steps)
            bucket = steps - 1;

        // zurück auf 0–1 mappen (Stufenmittelpunkt)
        return (bucket + 0.5f) / steps;
    }

}
