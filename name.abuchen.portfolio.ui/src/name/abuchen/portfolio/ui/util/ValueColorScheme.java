package name.abuchen.portfolio.ui.util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageDataProvider;
import org.eclipse.swt.graphics.PaletteData;
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

        var custom = new ValueColorScheme(CUSTOM_SCHEME, Messages.ValueColorSchemeCustom);
        custom.setPositiveForeground(ColorConversion.hex2RGBA("#007A00")); //$NON-NLS-1$
        custom.setNegativeForeground(ColorConversion.hex2RGBA("#C62828")); //$NON-NLS-1$
        custom.defaultPositiveForeground = custom.positiveForeground;
        custom.defaultNegativeForeground = custom.negativeForeground;

        schemes = List.of(standard, blueOrange, asia, vivid, custom);
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
            upArrow = createArrowImage(positiveForeground, true);
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
            downArrow = createArrowImage(negativeForeground, false);
    }

    public Image upArrow()
    {
        if (upArrow == null && CUSTOM_SCHEME.equals(identifier) && positiveForeground != null)
            upArrow = createArrowImage(positiveForeground, true);
        return upArrow;
    }

    public void setUpArrow(String icon)
    {
        this.upArrow = Images.resolve(icon, false);
    }

    public Image downArrow()
    {
        if (downArrow == null && CUSTOM_SCHEME.equals(identifier) && negativeForeground != null)
            downArrow = createArrowImage(negativeForeground, false);
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
        for (var scheme : schemes)
        {
            if (CUSTOM_SCHEME.equals(scheme.getIdentifier()))
                return scheme;
        }
        return null;
    }

    public static boolean setCustomPositiveColor(String hex)
    {
        var custom = getCustomScheme();
        if (custom == null)
            return false;

        if (hex != null && !hex.isBlank())
        {
            custom.hasCustomPositive = true;
            custom.setPositiveForeground(ColorConversion.hex2RGBA(hex));
        }
        else
        {
            custom.hasCustomPositive = false;
        }
        return true;
    }

    public static boolean setCustomNegativeColor(String hex)
    {
        var custom = getCustomScheme();
        if (custom == null)
            return false;

        if (hex != null && !hex.isBlank())
        {
            custom.hasCustomNegative = true;
            custom.setNegativeForeground(ColorConversion.hex2RGBA(hex));
        }
        else
        {
            custom.hasCustomNegative = false;
        }
        return true;
    }

    public static Image createArrowImage(Color color, boolean up)
    {
        var display = Display.getCurrent() != null ? Display.getCurrent() : Display.getDefault();
        if (display == null || color == null || color.isDisposed())
            return null;

        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        return new Image(display, (ImageDataProvider) zoom -> {
            int size = Math.max(1, Math.round(16f * zoom / 100f));
            var bImg = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            var g2 = bImg.createGraphics();
            try
            {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new java.awt.Color(r, g, b));
                int pTop = Math.round(3f * zoom / 100f);
                int pBottom = Math.round(12f * zoom / 100f);
                int pMid = Math.round(8f * zoom / 100f);
                int pLeft = Math.round(2f * zoom / 100f);
                int pRight = Math.round(14f * zoom / 100f);
                if (up)
                    g2.fillPolygon(new int[] { pMid, pRight, pLeft }, new int[] { pTop, pBottom, pBottom }, 3);
                else
                    g2.fillPolygon(new int[] { pMid, pRight, pLeft }, new int[] { pBottom, pTop, pTop }, 3);
            }
            finally
            {
                g2.dispose();
            }

            var palette = new PaletteData(0x00FF0000, 0x0000FF00, 0x000000FF);
            var data = new ImageData(size, size, 32, palette);
            data.alphaData = new byte[size * size];
            for (int y = 0; y < size; y++)
            {
                for (int x = 0; x < size; x++)
                {
                    int argb = bImg.getRGB(x, y);
                    data.setPixel(x, y, argb & 0x00FFFFFF);
                    data.alphaData[y * size + x] = (byte) ((argb >> 24) & 0xFF);
                }
            }
            return data;
        });
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
