package name.abuchen.portfolio.ui.util.chart;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageDataProvider;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.IAxis;
import org.eclipse.swtchart.Range;

public final class ChartUtil
{
    private ChartUtil()
    {
    }

    public static void addMargins(Chart chart, double zoomRatio)
    {
        for (IAxis axis : chart.getAxisSet().getAxes())
            addMargin(axis, zoomRatio);
    }

    public static void addYMargins(Chart chart, double zoomRatio)
    {
        for (IAxis axis : chart.getAxisSet().getYAxes())
            addMargin(axis, zoomRatio);
    }

    public static void addMargin(IAxis axis, double zoomRatio)
    {
        Range range = axis.getRange();
        double midPoint = ((range.upper - range.lower) / 2) + range.lower;
        double lower = (range.lower - 2 * zoomRatio * midPoint) / (1 - 2 * zoomRatio);
        double upper = (range.upper - 2 * zoomRatio * midPoint) / (1 - 2 * zoomRatio);
        axis.setRange(new Range(lower, upper));
    }

    public static void save(Chart chart, String filename, int format)
    {
        // based on https://github.com/eclipse/swtchart/issues/86
        ImageLoader imageLoader = new ImageLoader();
        imageLoader.data = new ImageData[] { getImageData(chart) };
        imageLoader.save(filename, format);
    }

    private static ImageData getImageData(Chart chart)
    {
        chart.redraw();
        chart.update();

        Point chartSize = chart.getSize();

        // GC.copyArea() fills the image of the GC's zoom only, while getImageData()
        // reads the zoom it is asked for - above 100 % an untouched, black buffer.
        // Windows asks the provider while copying, cocoa and GTK while the image is
        // set up. As SWT keeps asking other zooms once it gets null, only the first
        // request of a phase is the zoom whose buffer copyArea fills.
        AtomicBoolean copying = new AtomicBoolean(false);
        AtomicInteger setupZoom = new AtomicInteger(0);
        AtomicInteger copyZoom = new AtomicInteger(0);

        ImageDataProvider chartImageDataProvider = zoom -> {
            if (copying.get())
                copyZoom.compareAndSet(0, zoom);
            else
                setupZoom.compareAndSet(0, zoom);

            // supply the 100 % image and let SWT scale it, using its own rounding.
            // A buffer of its own would stay alive as an image representation and
            // cocoa would hand that out instead of the copied pixels.
            if (zoom != 100)
                return null;

            PaletteData palette = new PaletteData(0xFF, 0xFF00, 0xFF0000);
            return new ImageData(Math.max(1, chartSize.x), Math.max(1, chartSize.y), 32, palette);
        };

        Image image = null;
        GC gc = null;
        try
        {
            image = new Image(chart.getDisplay(), chartImageDataProvider);
            gc = new GC(chart);

            copying.set(true);
            try
            {
                gc.copyArea(image, 0, 0);
            }
            finally
            {
                copying.set(false);
            }

            // Windows knows the GC's zoom by now, cocoa and GTK stay at their setup zoom
            int zoom = copyZoom.get() > 0 ? copyZoom.get() : setupZoom.get();
            return image.getImageData(zoom > 0 ? zoom : 100);
        }
        finally
        {
            if (gc != null && !gc.isDisposed())
                gc.dispose();
            if (image != null && !image.isDisposed())
                image.dispose();
        }
    }
}
