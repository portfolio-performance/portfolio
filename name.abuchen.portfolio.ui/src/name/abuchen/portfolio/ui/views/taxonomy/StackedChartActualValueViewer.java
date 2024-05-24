package name.abuchen.portfolio.ui.views.taxonomy;

import jakarta.inject.Inject;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.editor.PortfolioPart;
import name.abuchen.portfolio.ui.util.chart.StackedTimelineChart;
import name.abuchen.portfolio.ui.util.format.AmountNumberFormat;
import name.abuchen.portfolio.ui.util.format.ThousandsNumberFormat;

public class StackedChartActualValueViewer extends AbstractStackedChartViewer
{
    @Inject
    public StackedChartActualValueViewer(PortfolioPart part, TaxonomyModel model, TaxonomyNodeRenderer renderer)
    {
        super(part, model, renderer);
    }

    @Override
    protected void configureChart(StackedTimelineChart chart)
    {
        chart.getAxisSet().getYAxis(0).getTick().setFormat(new ThousandsNumberFormat());
        chart.getToolTip().setDefaultValueFormat(new AmountNumberFormat());
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
                answer[ii] = values[ii] / Values.Amount.divider();
        }
        return answer;
    }

    @Override
    protected void adjustRange(StackedTimelineChart chart)
    {
        chart.getAxisSet().adjustRange();
    }
}
