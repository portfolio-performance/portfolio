package name.abuchen.portfolio.ui.views.payments;

import java.text.DateFormatSymbols;
import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.IAxis;
import org.eclipse.swtchart.ILineSeries;
import org.eclipse.swtchart.ILineSeries.PlotSymbolType;
import org.eclipse.swtchart.ISeries.SeriesType;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.TabularDataSource;
import name.abuchen.portfolio.ui.util.chart.TimelineChartToolTip;
import name.abuchen.portfolio.ui.util.format.AmountNumberFormat;

public class PaymentsAccumulatedChartBuilder implements PaymentsChartBuilder
{

    @Override
    public String getLabel()
    {
        return Messages.LabelPaymentsPerMonthAccumulated;
    }

    @Override
    public int getTabIndex()
    {
        return 6;
    }

    @Override
    public void configure(Chart chart, Consumer<TabularDataSource> selectionListener)
    {
        IAxis xAxis = chart.getAxisSet().getXAxis(0);
        xAxis.enableCategory(true);
        // format symbols returns 13 values as some calendars have 13 months
        xAxis.setCategorySeries(Arrays.copyOfRange(new DateFormatSymbols().getMonths(), 0, 12));

        TimelineChartToolTip toolTip = new TimelineChartToolTip(chart);
        toolTip.enableCategory(true);
        toolTip.setDefaultValueFormat(new AmountNumberFormat());
    }

    @Override
    public void createSeries(Chart chart, PaymentsViewModel model)
    {
        LocalDate now = LocalDate.now();
        boolean isJanuary = now.getMonth() == Month.JANUARY;

        for (int index = 0; index < model.getNoOfMonths(); index += 12)
        {
            int year = model.getStartYear() + (index / 12);

            var lineSeries = (ILineSeries<?>) chart.getSeriesSet().createSeries(SeriesType.LINE, String.valueOf(year));
            lineSeries.setDescription(lineSeries.getId());

            double[] series = new double[Math.min(12, model.getNoOfMonths() - index)];

            long value = 0;
            for (int ii = 0; ii < series.length; ii++)
            {
                value += model.getSum().getValue(index + ii);
                series[ii] = value / Values.Amount.divider();
            }
            lineSeries.setYSeries(series);

            lineSeries.setLineColor(PaymentsColors.getColor(year));
            lineSeries.setLineWidth(2);
            lineSeries.setSymbolType(PlotSymbolType.NONE);
            lineSeries.setAntialias(SWT.ON);

            // if January is the only data point, then no line is drawn (there
            // is no February value to draw to). Therefore we make sure that at
            // least a symbol is drawn for the January data point
            if (isJanuary && year == now.getYear())
            {
                lineSeries.setSymbolSize(2);
                lineSeries.setSymbolType(PlotSymbolType.SQUARE);
            }

        }
    }
}
