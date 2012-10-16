package name.abuchen.portfolio.ui.views;

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

        private Segment(float[] hsb)
        {
            this.hsb = new float[hsb.length];
            System.arraycopy(hsb, 0, this.hsb, 0, hsb.length);
        }

        Segment(RGB rgb)
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
    }

    private static final float HUE = 262.3f;
    private static final float SATURATION = 0.464f;
    private static final float BRIGHTNESS = 0.886f;
    private final float step;

    private LocalResourceManager resources;

    public ColorWheel(Control owner)
    {
        this(owner, 10);
    }

    public ColorWheel(Control owner, int size)
    {
        resources = new LocalResourceManager(JFaceResources.getResources(), owner);
        step = (360.0f / (float) size);
    }

    public Segment getSegment(int segment)
    {
        return new Segment(new float[] { (HUE + (step * segment)) % 360f, SATURATION, BRIGHTNESS });
    }
}
