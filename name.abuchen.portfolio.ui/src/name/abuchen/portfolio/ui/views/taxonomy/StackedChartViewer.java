package name.abuchen.portfolio.ui.views.taxonomy;

import java.text.DecimalFormat;

import jakarta.inject.Inject;

import org.eclipse.swtchart.Range;

import name.abuchen.portfolio.ui.editor.PortfolioPart;
import name.abuchen.portfolio.ui.util.chart.StackedTimelineChart;
import name.abuchen.portfolio.ui.util.format.AxisTickPercentNumberFormat;

public class StackedChartViewer extends AbstractStackedChartViewer
{
    @Inject
    public StackedChartViewer(PortfolioPart part, TaxonomyModel model, TaxonomyNodeRenderer renderer)
    {
        super(part, model, renderer);
    }

    @Override
    protected void configureChart(StackedTimelineChart chart)
    {
        chart.getAxisSet().getYAxis(0).getTick().setFormat(new AxisTickPercentNumberFormat("#0.0%")); //$NON-NLS-1$
        chart.getToolTip().setDefaultValueFormat(new DecimalFormat("#0.0%")); //$NON-NLS-1$
    }

    @Override
    protected double[] getValues(SeriesBuilder series, long[] totals)
    {
        var values = series.getValues();

        double[] answer = new double[values.length];
        for (int ii = 0; ii < answer.length; ii++)
        {
            if (totals[ii] == 0)
                answer[ii] = 0d;
            else
                answer[ii] = values[ii] / (double) totals[ii];
        }
        return answer;
    }

    @Override
    protected void adjustRange(StackedTimelineChart chart)
    {
        chart.getAxisSet().adjustRange();
        chart.getAxisSet().getYAxis(0).setRange(new Range(-0.025, 1.025));
    }
}
