package name.abuchen.portfolio.ui.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGBA;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.util.ColorConversion;

/**
 * ValueColorScheme provides color schemes for positive/negative value
 * indicators. Users can choose between different schemes (e.g., green/red or
 * blue/orange) for improved accessibility.
 */
public final class ValueColorScheme
{
    public static final String STANDARD_SCHEME = "standard"; //$NON-NLS-1$
    public static final String CUSTOM_SCHEME = "custom"; //$NON-NLS-1$

    private static final List<ValueColorScheme> schemes;
    private static final ValueColorScheme customScheme;
    private static ValueColorScheme currentScheme;

    static
    {
        // initialize value color scheme with values in case CSS cannot be read

        var standard = new ValueColorScheme(STANDARD_SCHEME, Messages.ValueColorSchemeStandard);
        standard.setPositiveForeground(Colors.DARK_GREEN.getRGBA());
        standard.setNegativeForeground(Colors.DARK_RED.getRGBA());
        standard.setUpArrow("scheme/standard/light/up_arrow.svg"); //$NON-NLS-1$
        standard.setDownArrow("scheme/standard/light/down_arrow.svg"); //$NON-NLS-1$

        var blueOrange = new ValueColorScheme("blueorange", Messages.ValueColorSchemeBlueOrange); //$NON-NLS-1$
        blueOrange.setPositiveForeground(ColorConversion.hex2RGBA("#0066CC")); //$NON-NLS-1$
        blueOrange.setNegativeForeground(ColorConversion.hex2RGBA("#B36E3A")); //$NON-NLS-1$
        blueOrange.setUpArrow("scheme/blueorange/light/up_arrow.svg"); //$NON-NLS-1$
        blueOrange.setDownArrow("scheme/blueorange/light/down_arrow.svg"); //$NON-NLS-1$

        var asia = new ValueColorScheme("asia", Messages.ValueColorSchemeAsia); //$NON-NLS-1$
        asia.setPositiveForeground(Colors.DARK_RED.getRGBA());
        asia.setNegativeForeground(Colors.DARK_GREEN.getRGBA());
        asia.setUpArrow("scheme/asia/light/up_arrow.svg"); //$NON-NLS-1$
        asia.setDownArrow("scheme/asia/light/down_arrow.svg"); //$NON-NLS-1$

        var vivid = new ValueColorScheme("vivid", Messages.ValueColorSchemeVividContrast); //$NON-NLS-1$
        vivid.setPositiveForeground(ColorConversion.hex2RGBA("#00E000")); //$NON-NLS-1$
        vivid.setNegativeForeground(ColorConversion.hex2RGBA("#FE2B05")); //$NON-NLS-1$
        vivid.setUpArrow("scheme/vivid/light/up_arrow.svg"); //$NON-NLS-1$
        vivid.setDownArrow("scheme/vivid/light/down_arrow.svg"); //$NON-NLS-1$

        customScheme = new ValueColorScheme(CUSTOM_SCHEME, Messages.ValueColorSchemeCustom);
        customScheme.setPositiveForeground(ColorConversion.hex2RGBA("#007A00")); //$NON-NLS-1$
        customScheme.setNegativeForeground(ColorConversion.hex2RGBA("#C62828")); //$NON-NLS-1$
        customScheme.defaultPositiveForeground = customScheme.positiveForeground;
        customScheme.defaultNegativeForeground = customScheme.negativeForeground;

        schemes = List.of(standard, blueOrange, asia, vivid, customScheme);
        currentScheme = standard;
    }

    private final String identifier;
    private final String name;

    private Color positiveForeground;
    private Color negativeForeground;
    private Color defaultPositiveForeground;
    private Color defaultNegativeForeground;
    private Image upArrow;
    private Image downArrow;
    private boolean hasCustomPositive;
    private boolean hasCustomNegative;

    private ValueColorScheme(String identifier, String name)
    {
        this.identifier = identifier;
        this.name = name;
    }

    public String getIdentifier()
    {
        return identifier;
    }

    public String getName()
    {
        return name;
    }

    public boolean hasCustomPositive()
    {
        return hasCustomPositive;
    }

    public boolean hasCustomNegative()
    {
        return hasCustomNegative;
    }

    public Color positiveForeground()
    {
        return positiveForeground;
    }

    public Color defaultPositiveForeground()
    {
        return defaultPositiveForeground != null ? defaultPositiveForeground : positiveForeground;
    }

    public void setDefaultPositiveForeground(RGBA color)
    {
        this.defaultPositiveForeground = Colors.getColor(color.rgb);
    }

    public void setPositiveForeground(RGBA color)
    {
        this.positiveForeground = Colors.getColor(color.rgb);
        if (CUSTOM_SCHEME.equals(identifier))
        {
            var oldArrow = this.upArrow;
            this.upArrow = createArrowImage(positiveForeground, true);
            disposeLater(oldArrow);
        }
    }

    public Color negativeForeground()
    {
        return negativeForeground;
    }

    public Color defaultNegativeForeground()
    {
        return defaultNegativeForeground != null ? defaultNegativeForeground : negativeForeground;
    }

    public void setDefaultNegativeForeground(RGBA color)
    {
        this.defaultNegativeForeground = Colors.getColor(color.rgb);
    }

    public void setNegativeForeground(RGBA color)
    {
        this.negativeForeground = Colors.getColor(color.rgb);
        if (CUSTOM_SCHEME.equals(identifier))
        {
            var oldArrow = this.downArrow;
            this.downArrow = createArrowImage(negativeForeground, false);
            disposeLater(oldArrow);
        }
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

    public static ValueColorScheme getCustomScheme()
    {
        return customScheme;
    }

    public static void setCustomPositiveColor(String hex)
    {
        if (isValidHexColor(hex))
        {
            customScheme.hasCustomPositive = true;
            customScheme.setPositiveForeground(ColorConversion.hex2RGBA(hex));
        }
        else
        {
            customScheme.hasCustomPositive = false;
            if (customScheme.defaultPositiveForeground != null && !customScheme.defaultPositiveForeground.isDisposed())
                customScheme.setPositiveForeground(customScheme.defaultPositiveForeground.getRGBA());
        }
    }

    public static void setCustomNegativeColor(String hex)
    {
        if (isValidHexColor(hex))
        {
            customScheme.hasCustomNegative = true;
            customScheme.setNegativeForeground(ColorConversion.hex2RGBA(hex));
        }
        else
        {
            customScheme.hasCustomNegative = false;
            if (customScheme.defaultNegativeForeground != null && !customScheme.defaultNegativeForeground.isDisposed())
                customScheme.setNegativeForeground(customScheme.defaultNegativeForeground.getRGBA());
        }
    }

    private static boolean isValidHexColor(String hex)
    {
        return hex != null && hex.matches("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?"); //$NON-NLS-1$
    }

    private static void disposeLater(Image image)
    {
        if (image == null || image.isDisposed())
            return;

        var display = Display.getDefault();
        if (display != null && !display.isDisposed())
            display.asyncExec(() -> {
                if (!image.isDisposed())
                    image.dispose();
            });
        else
            image.dispose();
    }

    public static Image createArrowImage(Color color, boolean up)
    {
        var display = Display.getCurrent() != null ? Display.getCurrent() : Display.getDefault();
        if (display == null || color == null || color.isDisposed())
            return null;

        String hex = ColorConversion.toHex(color.getRGB());
        String path = up ? "M12 3l12 18h-24z" : "M12 21l-12-18h24z"; //$NON-NLS-1$ //$NON-NLS-2$
        String svg = String.format(
                        "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"-6 -6 36 36\" width=\"16\" height=\"16\" fill=\"%s\"><path d=\"%s\"/></svg>", //$NON-NLS-1$
                        hex, path);

        try (var in = new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)))
        {
            return new Image(display, in);
        }
        catch (IOException e)
        {
            return null;
        }
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
