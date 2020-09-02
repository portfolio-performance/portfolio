package name.abuchen.portfolio.util;

import org.eclipse.swt.graphics.RGB;

public class ColorConversion
{
    private ColorConversion()
    {
    }

    public static int[] toRGB(String hex)
    {
        try
        {
            Integer intval = Integer.decode(hex);
            int i = intval.intValue();
            return new int[] { (i >> 16) & 0xFF, (i >> 8) & 0xFF, i & 0xFF };
        }
        catch (NumberFormatException ignore)
        {
            return new int[] { 0, 0, 0 };
        }
    }

    public static RGB hex2RGB(String hex)
    {
        int[] rgb = toRGB(hex);
        return new RGB(rgb[0], rgb[1], rgb[2]);
    }

    public static float[] toHSB(String hex)
    {
        return hex2RGB(hex).getHSB();
    }

    public static String toHex(RGB rgb)
    {
        return toHex(rgb.red, rgb.green, rgb.blue);
    }

    public static String toHex(int red, int green, int blue)
    {
        return String.format("#%02x%02x%02x", red, green, blue); //$NON-NLS-1$
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
