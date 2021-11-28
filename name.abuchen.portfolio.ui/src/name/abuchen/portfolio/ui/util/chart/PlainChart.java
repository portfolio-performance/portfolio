package name.abuchen.portfolio.ui.util.chart;

import org.eclipse.swt.widgets.Composite;
import org.swtchart.Chart;

public class PlainChart extends Chart // NOSONAR
{
    public PlainChart(Composite parent, int style)
    {
        super(parent, style);
    }

    @Override
    public boolean setFocus()
    {
        // for whatever reason, when the CTabFolder (used for the part stack)
        // tries to fix the focus, the chart legend forces a selection event if
        // the first tab is currently not selected. This results in "jumpy"
        // behavior when selecting tabs (the user clicks on the third one, but
        // the first is activated)

        // fix: never force focus on the legend but focus the plot area instead

        return getPlotArea().setFocus();
    }
}
