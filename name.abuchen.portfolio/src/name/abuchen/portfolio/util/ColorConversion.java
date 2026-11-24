package name.abuchen.portfolio.util;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.RGBA;

public class ColorConversion
{
    private ColorConversion()
    {
    }

    public static RGB hex2RGB(String hex)
    {
        return hex2RGBA(hex).rgb;
    }

    public static RGBA hex2RGBA(String hex)
    {
        try
        {
            long i = Long.decode(hex).longValue();
            return new RGBA((int) ((i >> 16) & 0xFF), // R
                            (int) ((i >> 8) & 0xFF), // G
                            (int) (i & 0xFF), // B
                            (int) ((i >> 24) & 0xFF) // A
            );
        }
        catch (NumberFormatException ignore)
        {
            return new RGBA(0, 0, 0, 0);
        }
    }

    public static float[] toHSB(String hex)
    {
        return hex2RGB(hex).getHSB();
    }

    public static String toHex(RGB rgb)
    {
        return toHex(rgb.red, rgb.green, rgb.blue);
    }

    public static String toHex(RGBA rgba)
    {
        return toHex(rgba.alpha, rgba.rgb.red, rgba.rgb.green, rgba.rgb.blue);
    }

    public static String toHex(int red, int green, int blue)
    {
        return String.format("#%02x%02x%02x", red, green, blue); //$NON-NLS-1$
    }

    public static String toHex(int alpha, int red, int green, int blue)
    {
        return String.format("#%02x%02x%02x%02x", alpha, red, green, blue); //$NON-NLS-1$
    }

    public static String toHex(float hue, float saturation, float brightness)
    {
        return toHex(new RGB(hue, saturation, brightness));
    }

    /**
     * Returns a color that stays visible against a light and dark background.
     * Unsaturated bright colors (close to white) and colors close to black get
     * a minimum saturation and a brightness in the middle range. Colors that
     * are already visible keep their hue and saturation.
     */
    public static RGB ensureVisible(RGB rgb)
    {
        float[] hsb = rgb.getHSB();
        float hue = hsb[0], saturation = hsb[1], brightness = hsb[2];

        // close to white: invisible on light backgrounds (only for unsaturated
        // colors, saturated bright colors remain visible)
        if (brightness > 0.75f && saturation < 0.3f)
            return new RGB(hue, 0.35f, 0.8f);

        // close to black: invisible on dark backgrounds, regardless of the
        // saturation of the color
        if (brightness < 0.3f)
            return new RGB(hue, Math.max(0.35f, saturation), 0.45f);

        return rgb;
    }

    public static RGB brighter(RGB rgb)
    {
        float[] hsb = rgb.getHSB();
        // keep the result visible on light backgrounds (issue #2829)
        float saturation = Math.max(0.2f, hsb[1] - 0.2f);
        float brightness = Math.min(0.9f, hsb[2] + 0.2f);
        return new RGB(hsb[0], saturation, brightness);
    }

    public static String brighter(String hex)
    {
        return toHex(brighter(hex2RGB(hex)));
    }

    public static RGB darker(RGB rgb)
    {
        float[] hsb = rgb.getHSB();
        float saturation = Math.min(1f, hsb[1] + 0.2f);
        float brightness = Math.max(0.25f, hsb[2] - 0.2f);
        return new RGB(hsb[0], saturation, brightness);
    }

    public static String darker(String hex)
    {
        return toHex(darker(hex2RGB(hex)));
    }
}
