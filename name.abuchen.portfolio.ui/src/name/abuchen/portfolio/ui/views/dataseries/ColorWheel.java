package name.abuchen.portfolio.ui.views.dataseries;

import org.eclipse.swt.graphics.RGB;

/* package */class ColorWheel
{
    private static final float HUE = 262.3f;
    private static final float SATURATION = 0.464f;
    private static final float BRIGHTNESS = 0.886f;

    private float[][] hsbColors;

    /* package */ ColorWheel(int size)
    {
        hsbColors = new float[size][];
        float step = 360.0f / (float) size;
        for (int ii = 0; ii < size; ii++)
            hsbColors[ii] = new float[] { (HUE + (step * ii)) % 360f, SATURATION, BRIGHTNESS };
    }

    /* package */ RGB getRGB(int segment)
    {
        float[] hsb = hsbColors[segment % hsbColors.length];
        return new RGB(hsb[0], hsb[1], hsb[2]);
    }
}
