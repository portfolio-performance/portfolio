package name.abuchen.portfolio.ui.util.chart;

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

        ImageDataProvider chartImageDataProvider = zoom -> {
            if (zoom != 100)
                return null;
            PaletteData palette = new PaletteData(0xFF, 0xFF00, 0xFF0000);
            return new ImageData(chartSize.x, chartSize.y, 32, palette);
        };

        Image image = null;
        GC gc = null;
        try
        {
            image = new Image(chart.getDisplay(), chartImageDataProvider);
            gc = new GC(chart);
            gc.copyArea(image, 0, 0);
            return image.getImageData();
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
