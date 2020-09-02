package name.abuchen.portfolio.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.eclipse.swt.graphics.RGB;
import org.junit.Test;

@SuppressWarnings("nls")
public class ColorConversionTest
{

    @Test
    public void testInvalidRGB()
    {
        String hex = "#K10203";
        int[] rgb = ColorConversion.toRGB(hex);
        assertEquals(rgb[0], 0);
        assertEquals(rgb[1], 0);
        assertEquals(rgb[2], 0);
    }

    @Test
    public void testToRGB()
    {
        String hex = "#010203";
        int[] rgb = ColorConversion.toRGB(hex);
        assertEquals(rgb[0], 1);
        assertEquals(rgb[1], 2);
        assertEquals(rgb[2], 3);
    }

    @Test
    public void testHex2RGB()
    {
        String hex = "#010203";
        RGB colour = new RGB(1, 2, 3);
        RGB colour2 = ColorConversion.hex2RGB(hex);
        assertEquals(colour, colour2);
    }

    @Test
    public void testToHSB()
    {
        String hex = "#123456";
        float[] hsb = ColorConversion.toHSB(hex);
        float delta = (float) 0.0001;
        assertEquals(hsb[0], 210.0, delta);
        assertEquals(hsb[1], 0.7906977, delta);
        assertEquals(hsb[2], 0.3372549, delta);
    }

    @Test
    public void testToHexRGB()
    {
        RGB rgb = new RGB(1, 2, 3);
        String hex = "#010203";
        String colour2 = ColorConversion.toHex(rgb);
        assertEquals(hex, colour2);
    }

    @Test
    public void testToHexIntIntInt()
    {
        String hex = "#010203";
        String colour2 = ColorConversion.toHex(1, 2, 3);
        assertEquals(hex, colour2);
    }

    @Test
    public void testToHexFloatFloatFloat()
    {
        String hex = "#123354";
        String colour2 = ColorConversion.toHex((float) 210.0, (float) 0.79, (float) 0.33);
        assertEquals(hex, colour2);
    }

    @Test
    public void testBrighterRGB()
    {
        RGB rgb = new RGB(1, 2, 3);
        RGB colour2 = ColorConversion.brighter(rgb);
        float[] brightness1 = rgb.getHSB();
        float[] brightness2 = colour2.getHSB();
        assertTrue(brightness2[2] > brightness1[2]);
    }

    @Test
    public void testBrighterString()
    {
        String hex = "#123456";
        RGB rgb = new RGB(18, 52, 86);
        String hex2 = ColorConversion.brighter(hex);
        RGB rgb2 = new RGB(56, 97, 137);

        assertEquals(rgb, ColorConversion.hex2RGB(hex));
        assertEquals(rgb2, ColorConversion.hex2RGB(hex2));

        float[] brightness1 = rgb.getHSB();
        float[] brightness2 = rgb2.getHSB();
        assertTrue(brightness2[2] > brightness1[2]);
    }
}
