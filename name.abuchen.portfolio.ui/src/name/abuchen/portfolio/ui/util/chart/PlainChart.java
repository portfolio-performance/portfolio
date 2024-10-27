package name.abuchen.portfolio.ui.util.chart;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.internal.PlotArea;

public class PlainChart extends Chart // NOSONAR
{
    @SuppressWarnings("restriction")
    public PlainChart(Composite parent, int style)
    {
        super(parent, style, null);

        // we must use the secondary constructor that is not creating the
        // PlotArea because the default constructor adds a mouse move listener
        // that is redrawing the chart on every mouse move. That leads to janky
        // UI when the tooltip is shown.
        new PlotArea(this, SWT.NONE);
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

        return getPlotArea().getControl().setFocus();
    }
}
