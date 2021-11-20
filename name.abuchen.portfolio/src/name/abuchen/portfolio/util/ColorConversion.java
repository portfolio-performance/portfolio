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

    public static RGB brighter(RGB rgb)
    {
        float[] hsb = rgb.getHSB();
        float saturation = Math.max(0f, hsb[1] - 0.2f);
        float brightness = Math.min(1f, hsb[2] + 0.2f);
        return new RGB(hsb[0], saturation, brightness);
    }

    public static String brighter(String hex)
    {
        return toHex(brighter(hex2RGB(hex)));
    }
}
