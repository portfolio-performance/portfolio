package name.abuchen.portfolio.ui.util.chart;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.ICircularSeries;

import name.abuchen.portfolio.ui.UIConstants;

public class PieChart extends Chart // NOSONAR
{
    private PieChartToolTip tooltip;
    
    public PieChart(Composite parent)
    {
        super(parent, SWT.NONE);

        setData(UIConstants.CSS.CLASS_NAME, "chart"); //$NON-NLS-1$ 

        addListener(SWT.Paint, new Listener()
        {
            @Override
            public void handleEvent(Event event)
            {
                // Set color in root node to background color
                if (getSeriesSet().getSeries().length > 0) {
                    ICircularSeries<?> cs = (ICircularSeries<?>) getSeriesSet().getSeries()[0];
                    cs.getRootNode().setColor(getPlotArea().getBackground());
                }
            }
        });

        getLegend().setVisible(true);
        tooltip = new PieChartToolTip(this);
    }

    public PieChartToolTip getToolTip()
    {
        return tooltip;
    }
}
