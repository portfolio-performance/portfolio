package name.abuchen.portfolio.ui.views.earnings;

import java.time.LocalDate;
import java.time.Month;

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
        LocalDate now = LocalDate.now();
        boolean isJanuary = now.getMonth() == Month.JANUARY;

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

            // if January is the only data point, then no line is drawn (there
            // is no February value to draw to). Therefore we make sure that at
            // least a symbol is drawn for the January data point
            if (isJanuary && year == now.getYear())
            {
                lineSeries.setSymbolSize(1);
                lineSeries.setSymbolType(PlotSymbolType.SQUARE);
            }

        }
    }
}
