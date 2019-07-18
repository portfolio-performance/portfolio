package name.abuchen.portfolio.ui.views.earnings;

import org.eclipse.swt.SWT;
import org.swtchart.ILineSeries;
import org.swtchart.ILineSeries.PlotSymbolType;
import org.swtchart.ISeries.SeriesType;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;

public class EarningsAccumulatedChartTab extends AbstractChartTab
{
    @Override
    public String getLabel()
    {
        return Messages.LabelAccumulatedEarnings;
    }

    @Override
    protected void createSeries()
    {
        for (int index = 0; index < model.getNoOfMonths(); index += 12)
        {
            int year = model.getStartYear() + (index / 12);

            ILineSeries lineSeries = (ILineSeries) getChart().getSeriesSet().createSeries(SeriesType.LINE,
                            String.valueOf(year));

            double[] series = new double[Math.min(12, model.getNoOfMonths() - index)];

            long value = 0;
            for (int ii = 0; ii < series.length; ii++)
            {
                value += model.getSum().getValue(index + ii);
                series[ii] = value / Values.Amount.divider();
            }
            lineSeries.setYSeries(series);

            lineSeries.setLineColor(getColor(year));
            lineSeries.setLineWidth(2);
            lineSeries.setSymbolType(PlotSymbolType.NONE);
            lineSeries.setAntialias(SWT.ON);
        }
    }
}
