package name.abuchen.portfolio.ui.views;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.util.chart.TimelineChart;

public class SimpleMovingAverage
{
    /**
     * Calculates the Simple Moving Average for the given range of days from the
     * given startDate on The method returns an object containing the X and Y
     * Axes of the generated SMA
     * 
     * @return The ChartLineSeriesAxes contains the X and Y Axes of the
     *         generated SMA
     */
    public static ChartLineSeriesAxes getSMA(Integer RangeSMA, Security security, LocalDate startDate)
    {
        if (security == null)
            return null;

        int index;
        LocalDate[] dates;
        double[] values;
        double sum = 0;
        ChartLineSeriesAxes lineSeries = new ChartLineSeriesAxes();

        List<SecurityPrice> prices = security.getPricesIncludingLatest();
        if (prices == null || prices.size() < RangeSMA)
            return null;

        if (startDate == null)
        {
            index = RangeSMA - 1;
            dates = new LocalDate[prices.size() - index];
            values = new double[prices.size() - index];
        }
        else
        {
            index = Math.abs(Collections.binarySearch(prices, new SecurityPrice(startDate, 0),
                            new SecurityPrice.ByDate()));

            if (index >= prices.size())
            {
                // no data available
                return null;
            }

            if (index < RangeSMA)
            {
                index = RangeSMA;
            }

            dates = new LocalDate[prices.size() - index];
            values = new double[prices.size() - index];
        }

        for (int ii = 0; index < prices.size(); index++, ii++)
        {
            List<SecurityPrice> filteredPrices = null;
            int start = index - (RangeSMA - 1);
            sum = 0;
            if (start >= 0)
            {
                LocalDate indexDatePlusOneDay = prices.get(index).getTime().plusDays(1);
                LocalDate indexDateMinusSMA = indexDatePlusOneDay.minusDays(RangeSMA).minusDays(1);
                filteredPrices = prices.stream()
                                .filter(p -> p.getTime().isAfter(indexDateMinusSMA) && p.getTime().isBefore(indexDatePlusOneDay))
                                .collect(Collectors.toList());

                for (SecurityPrice price : filteredPrices)
                {
                    sum += price.getValue();
                }
            }

            dates[ii] = prices.get(index).getTime();
            values[ii] = sum / Values.Quote.divider() / filteredPrices.size();

        }

        lineSeries.setDates(TimelineChart.toJavaUtilDate(dates));
        lineSeries.setValues(values);

        return lineSeries;
    }
}
