package name.abuchen.portfolio.ui.util.chart;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Display;
import org.swtchart.Chart;
import org.swtchart.IAxis;
import org.swtchart.Range;

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
        Image image = new Image(Display.getDefault(), chart.getBounds());
        GC gc = new GC(chart);
        gc.copyArea(image, 0, 0);
        gc.dispose();
        ImageData imageData = image.getImageData();
        ImageLoader loader = new ImageLoader();
        loader.data = new ImageData[] { imageData };
        loader.save(filename, format);
        image.dispose();
    }
}
