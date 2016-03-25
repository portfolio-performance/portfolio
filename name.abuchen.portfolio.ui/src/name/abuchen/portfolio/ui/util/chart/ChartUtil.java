package name.abuchen.portfolio.ui.util.chart;

import org.swtchart.Chart;
import org.swtchart.IAxis;
import org.swtchart.Range;

public final class ChartUtil
{
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
}
