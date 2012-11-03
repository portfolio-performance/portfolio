package name.abuchen.portfolio.ui.views;

import java.util.List;

import name.abuchen.portfolio.ui.util.Colors;

import org.eclipse.jface.resource.ColorDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Control;

/* package */class ColorWheel
{
    public class Segment
    {
        private float[] hsb;

        /* package */Segment(float[] hsb)
        {
            this.hsb = new float[hsb.length];
            System.arraycopy(hsb, 0, this.hsb, 0, hsb.length);
        }

        /* package */Segment(RGB rgb)
        {
            this.hsb = rgb.getHSB();
        }

        public Color getColor()
        {
            RGB rgb = new RGB(hsb[0], hsb[1], hsb[2]);
            return (Color) resources.createColor(ColorDescriptor.createFrom(rgb));
        }

        public Color getBrigherColor()
        {
            RGB rgb = new RGB(hsb[0], hsb[1], Math.min(1.0f, hsb[2] + 0.05f));
            return (Color) resources.createColor(ColorDescriptor.createFrom(rgb));
        }

        public Color getDarkerColor()
        {
            RGB rgb = new RGB(hsb[0], hsb[1], Math.max(0f, hsb[2] - 0.05f));
            return (Color) resources.createColor(ColorDescriptor.createFrom(rgb));
        }

        public Segment getShade(int segment)
        {
            return new Segment(new float[] { hsb[0], hsb[1], Math.max(0f, hsb[2] - (0.07f * (segment % 4))) });
        }
    }

    private static final float HUE = 262.3f;
    private static final float SATURATION = 0.464f;
    private static final float BRIGHTNESS = 0.886f;

    private LocalResourceManager resources;
    private float[][] hsbColors;

    private ColorWheel(Control owner)
    {
        resources = new LocalResourceManager(JFaceResources.getResources(), owner);
    }

    public ColorWheel(Control owner, int size)
    {
        this(owner);

        hsbColors = new float[size][];
        float step = (360.0f / (float) size);
        for (int ii = 0; ii < size; ii++)
            hsbColors[ii] = new float[] { (HUE + (step * ii)) % 360f, SATURATION, BRIGHTNESS };
    }

    public ColorWheel(Control owner, List<Colors> colors)
    {
        this(owner);

        hsbColors = new float[colors.size()][];
        for (int ii = 0; ii < hsbColors.length; ii++)
            hsbColors[ii] = colors.get(ii).swt().getHSB();
    }

    public Segment getSegment(int segment)
    {
        return new Segment(hsbColors[segment % hsbColors.length]);
    }
}
