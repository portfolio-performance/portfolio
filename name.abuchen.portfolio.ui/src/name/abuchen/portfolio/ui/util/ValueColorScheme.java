package name.abuchen.portfolio.ui.util;

import java.util.List;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGBA;

import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.util.ColorConversion;

/**
 * ValueColorScheme provides color schemes for positive/negative value
 * indicators. Users can choose between different schemes (e.g., green/red or
 * blue/orange) for improved accessibility.
 */
public final class ValueColorScheme
{
    public static final String STANDARD_SCHEME = "standard"; //$NON-NLS-1$

    private static final List<ValueColorScheme> schemes;
    private static ValueColorScheme currentScheme;

    static
    {
        // initialize value color scheme with values in case CSS cannot be read

        var standard = new ValueColorScheme(STANDARD_SCHEME);
        standard.setPositiveForeground(Colors.DARK_GREEN.getRGBA());
        standard.setNegativeForeground(Colors.DARK_RED.getRGBA());
        standard.setUpArrow("scheme/standard/light/up_arrow.svg"); //$NON-NLS-1$
        standard.setDownArrow("scheme/standard/light/down_arrow.svg"); //$NON-NLS-1$

        var blueOrange = new ValueColorScheme("blueorange"); //$NON-NLS-1$
        blueOrange.setPositiveForeground(ColorConversion.hex2RGBA("#0066CC")); //$NON-NLS-1$
        blueOrange.setNegativeForeground(ColorConversion.hex2RGBA("#B36E3A")); //$NON-NLS-1$
        blueOrange.setUpArrow("scheme/blueorange/light/up_arrow.svg"); //$NON-NLS-1$
        blueOrange.setDownArrow("scheme/blueorange/light/down_arrow.svg"); //$NON-NLS-1$

        var asia = new ValueColorScheme("asia"); //$NON-NLS-1$
        asia.setPositiveForeground(Colors.DARK_RED.getRGBA());
        asia.setNegativeForeground(Colors.DARK_GREEN.getRGBA());
        asia.setUpArrow("scheme/asia/light/up_arrow.svg"); //$NON-NLS-1$
        asia.setDownArrow("scheme/asia/light/down_arrow.svg"); //$NON-NLS-1$

        schemes = List.of(standard, blueOrange, asia);
        currentScheme = standard;
    }

    private final String identifier;

    private Color positiveForeground;
    private Color negativeForeground;
    private Image upArrow;
    private Image downArrow;

    private ValueColorScheme(String identifier)
    {
        this.identifier = identifier;
    }

    public String getIdentifier()
    {
        return identifier;
    }

    public Color positiveForeground()
    {
        return positiveForeground;
    }

    public void setPositiveForeground(RGBA color)
    {
        this.positiveForeground = Colors.getColor(color.rgb);
    }

    public Color negativeForeground()
    {
        return negativeForeground;
    }

    public void setNegativeForeground(RGBA color)
    {
        this.negativeForeground = Colors.getColor(color.rgb);
    }

    public Image upArrow()
    {
        return upArrow;
    }

    public void setUpArrow(String icon)
    {
        this.upArrow = Images.resolve(icon, false);
    }

    public Image downArrow()
    {
        return downArrow;
    }

    public void setDownArrow(String icon)
    {
        this.downArrow = Images.resolve(icon, false);
    }

    public static ValueColorScheme current()
    {
        return currentScheme;
    }

    public static List<ValueColorScheme> getAvailableSchemes()
    {
        return schemes;
    }

    public static void initialize(String schemeId)
    {
        if (schemeId == null)
            return;

        for (var scheme : schemes)
        {
            if (schemeId.equals(scheme.getIdentifier()))
                currentScheme = scheme;
        }
    }
}
