package name.abuchen.portfolio.ui.views.dividends;

import org.swtchart.IBarSeries;
import org.swtchart.ISeries.SeriesType;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;

public class DividendsChartTab extends AbstractChartTab
{
    @Override
    public String getLabel()
    {
        return Messages.LabelDividendsPerMonth;
    }

    protected void createSeries()
    {
        for (int index = 0; index < model.getNoOfMonths(); index += 12)
        {
            int year = model.getStartYear() + (index / 12);

            IBarSeries barSeries = (IBarSeries) getChart().getSeriesSet().createSeries(SeriesType.BAR,
                            String.valueOf(year));

            double[] series = new double[Math.min(12, model.getNoOfMonths() - index)];
            for (int ii = 0; ii < series.length; ii++)
                series[ii] = model.getSum().getValue(index + ii) / Values.Amount.divider();
            barSeries.setYSeries(series);

            barSeries.setBarColor(getColor(year));
            barSeries.setBarPadding(25);
        }
    }
}
