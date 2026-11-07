package name.abuchen.portfolio.ui.util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.RGBA;

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
    private static ValueColorScheme currentScheme;
    private static ValueColorScheme customScheme;

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

        var custom = new ValueColorScheme(CUSTOM_SCHEME, Messages.ValueColorSchemeCustom);
        custom.setPositiveForeground(ColorConversion.hex2RGBA("#00E000")); //$NON-NLS-1$
        custom.setNegativeForeground(ColorConversion.hex2RGBA("#FF6B6B")); //$NON-NLS-1$
        custom.setUpArrowColor(ColorConversion.hex2RGB("#00E000")); //$NON-NLS-1$
        custom.setDownArrowColor(ColorConversion.hex2RGB("#FF6B6B")); //$NON-NLS-1$
        custom.setUpArrow("scheme/standard/light/up_arrow.svg"); //$NON-NLS-1$
        custom.setDownArrow("scheme/standard/light/down_arrow.svg"); //$NON-NLS-1$
        customScheme = custom;

        schemes = List.of(standard, blueOrange, asia, vivid, custom);
        currentScheme = standard;
    }

    private static final int MAX_CACHED_ARROW_IMAGES = 32;
    private static final Map<String, Image> ARROW_CACHE = new LinkedHashMap<>(MAX_CACHED_ARROW_IMAGES, 0.75f, true);

    private final String identifier;
    private final String name;

    private Color positiveForeground;
    private Color negativeForeground;
    private RGB upArrowColor;
    private RGB downArrowColor;
    private Image upArrow;
    private Image downArrow;

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

    public Color positiveForeground()
    {
        return positiveForeground;
    }

    public void setPositiveForeground(RGBA color)
    {
        this.positiveForeground = Colors.getColor(color.rgb);
    }

    public void setPositiveForeground(RGB color)
    {
        this.positiveForeground = Colors.getColor(color);
    }

    public Color negativeForeground()
    {
        return negativeForeground;
    }

    public void setNegativeForeground(RGBA color)
    {
        this.negativeForeground = Colors.getColor(color.rgb);
    }

    public void setNegativeForeground(RGB color)
    {
        this.negativeForeground = Colors.getColor(color);
    }

    public boolean isCustom()
    {
        return CUSTOM_SCHEME.equals(identifier);
    }

    public static ValueColorScheme getCustomScheme()
    {
        return customScheme;
    }

    public Image upArrow()
    {
        if (isCustom() && upArrowColor != null)
        {
            Image customArrow = getArrowImage(upArrowColor, true);
            if (customArrow != null)
                return customArrow;
        }
        return upArrow;
    }

    public void setUpArrow(String icon)
    {
        this.upArrow = Images.resolve(icon, false);
    }

    public Image downArrow()
    {
        if (isCustom() && downArrowColor != null)
        {
            Image customArrow = getArrowImage(downArrowColor, false);
            if (customArrow != null)
                return customArrow;
        }
        return downArrow;
    }

    public void setDownArrow(String icon)
    {
        this.downArrow = Images.resolve(icon, false);
    }

    public RGB getUpArrowColor()
    {
        return upArrowColor;
    }

    public void setUpArrowColor(RGB rgb)
    {
        this.upArrowColor = rgb;
    }

    public RGB getDownArrowColor()
    {
        return downArrowColor;
    }

    public void setDownArrowColor(RGB rgb)
    {
        this.downArrowColor = rgb;
    }

    public static Image getArrowImage(RGB rgb, boolean up)
    {
        if (rgb == null)
            return null;

        String hex = ColorConversion.toHex(rgb);
        String key = (up ? "up_" : "down_") + hex; //$NON-NLS-1$ //$NON-NLS-2$

        Image image = ARROW_CACHE.get(key);
        if (image != null && !image.isDisposed())
            return image;

        try
        {
            String pathData = up ? "M12 3l12 18h-24z" : "M12 21l-12-18h24z"; //$NON-NLS-1$ //$NON-NLS-2$
            String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"-6 -6 36 36\" width=\"16\" height=\"16\" fill=\"" //$NON-NLS-1$
                            + hex + "\"><path d=\"" + pathData + "\"/></svg>"; //$NON-NLS-1$ //$NON-NLS-2$

            Path tempFile = Files.createTempFile("pp_arrow_" + (up ? "up_" : "down_"), ".svg"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            Files.writeString(tempFile, svg, StandardCharsets.UTF_8);
            tempFile.toFile().deleteOnExit();

            ImageDescriptor descriptor = ImageDescriptor.createFromURL(tempFile.toUri().toURL());
            image = descriptor.createImage();
            if (image != null)
                cacheArrowImage(key, image);
            return image;
        }
        catch (Exception e)
        {
            return up ? Images.resolve("scheme/standard/light/up_arrow.svg", false) //$NON-NLS-1$
                            : Images.resolve("scheme/standard/light/down_arrow.svg", false); //$NON-NLS-1$
        }
    }

    private static void cacheArrowImage(String key, Image image)
    {
        ARROW_CACHE.put(key, image);

        Iterator<Map.Entry<String, Image>> iterator = ARROW_CACHE.entrySet().iterator();
        while (ARROW_CACHE.size() > MAX_CACHED_ARROW_IMAGES && iterator.hasNext())
        {
            Map.Entry<String, Image> entry = iterator.next();
            if (!isCurrentCustomArrow(entry.getKey()))
            {
                iterator.remove();
                if (!entry.getValue().isDisposed())
                    entry.getValue().dispose();
            }
        }
    }

    private static boolean isCurrentCustomArrow(String key)
    {
        RGB upColor = customScheme.getUpArrowColor();
        RGB downColor = customScheme.getDownArrowColor();
        return (upColor != null && key.equals("up_" + ColorConversion.toHex(upColor))) //$NON-NLS-1$
                        || (downColor != null && key.equals("down_" + ColorConversion.toHex(downColor))); //$NON-NLS-1$
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
