package name.abuchen.portfolio.ui.util.chart;

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

        // SWT keeps one image handle per zoom level. GC.copyArea() fills the handle
        // of the GC's zoom level, but Image.getImageData() reads the handle of zoom
        // 100. At display scaling above 100 % there is no handle at zoom 100, so the
        // provider is called again and the untouched - black - buffer is returned.
        // Remember the zoom the provider is asked for (this is exactly the zoom
        // copyArea uses) and read the image data back from that handle.
        AtomicInteger usedZoom = new AtomicInteger(100);

        ImageDataProvider chartImageDataProvider = zoom -> {
            usedZoom.set(zoom);

            // the size of the returned ImageData becomes the size of the image
            // handle in pixels, and GC.copyArea() copies exactly that area out of
            // the control. The size must therefore match SWT's own point-to-pixel
            // conversion, which rounds: truncating allocates a buffer one pixel too
            // small and drops the last column/row of the chart.
            int width = Math.max(1, Math.round(chartSize.x * zoom / 100f));
            int height = Math.max(1, Math.round(chartSize.y * zoom / 100f));
            PaletteData palette = new PaletteData(0xFF, 0xFF00, 0xFF0000);
            return new ImageData(width, height, 32, palette);
        };

        Image image = null;
        GC gc = null;
        try
        {
            image = new Image(chart.getDisplay(), chartImageDataProvider);
            gc = new GC(chart);
            gc.copyArea(image, 0, 0);

            // read back at the zoom whose handle copyArea actually filled
            return image.getImageData(usedZoom.get());
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
