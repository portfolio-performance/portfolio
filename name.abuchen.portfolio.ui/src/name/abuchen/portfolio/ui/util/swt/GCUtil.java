package name.abuchen.portfolio.ui.util.swt;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

public final class GCUtil
{
    private GCUtil()
    {
    }

    /**
     * Draw a right triangle covering the top right half of a Rectangle r. The
     * color of the triangle has to be set before on GC gc.
     */
    public static final void drawTopRightTriangleOverlay(GC gc, Rectangle r)
    {
        gc.fillPolygon(new int[] { r.x, r.y, r.width, r.y, r.width, r.height });
    }

    /**
     * Draw a right triangle covering the top left half of a Rectangle r. The
     * color of the triangle has to be set before on GC gc.
     */
    public static final void drawTopLeftTriangleOverlay(GC gc, Rectangle r)
    {
        gc.fillPolygon(new int[] { r.x, r.y, r.width, r.y, r.x, r.height });
    }

    /**
     * Draw a right triangle covering the bottom right half of a Rectangle r.
     * The color of the triangle has to be set before on GC gc.
     */
    public static final void drawBottomRightTriangleOverlay(GC gc, Rectangle r)
    {
        gc.fillPolygon(new int[] { r.x, r.height, r.width, r.y, r.width, r.height });
    }

    /**
     * Draw a right triangle covering the bottom left half of a Rectangle r. The
     * color of the triangle has to be set before on GC gc.
     */
    public static final void drawBottomLeftTriangleOverlay(GC gc, Rectangle r)
    {
        gc.fillPolygon(new int[] { r.x, r.height, r.x, r.y, r.width, r.height });
    }

}
