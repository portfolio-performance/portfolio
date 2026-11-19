package name.abuchen.portfolio.ui.util.chart;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.IAxis;
import org.eclipse.swtchart.internal.ChartLayout;
import org.eclipse.swtchart.internal.axis.Axis;

/**
 * Works around a layout bug in SWTChart: if the tick labels of a horizontal
 * axis are hidden, {@link ChartLayout} computes the tick positions of the
 * vertical axes before it reserves the margin of the hidden axis. The grid
 * lines and tick labels of the vertical axes are then laid out for a plot area
 * that is a few pixels taller than the real one and do not line up with the
 * series anymore. The error grows towards the top of the chart.
 * <p>
 * After the regular layout, the ticks of the vertical axes are therefore
 * recomputed with the final height of the plot area.
 */
@SuppressWarnings("restriction")
public class TickAlignedChartLayout extends ChartLayout
{
    @Override
    protected void layout(Composite composite, boolean flushCache)
    {
        super.layout(composite, flushCache);

        if (!(composite instanceof Chart chart) || chart.getPlotArea() == null || chart.getAxisSet() == null)
            return;

        int height = chart.getPlotArea().getSize().y;
        if (height <= 0)
            return;

        IAxis[] verticalAxes = chart.getOrientation() == SWT.HORIZONTAL ? chart.getAxisSet().getYAxes()
                        : chart.getAxisSet().getXAxes();

        for (IAxis axis : verticalAxes)
        {
            if (axis instanceof Axis a)
                a.getTick().updateTick(height);
        }
    }
}
