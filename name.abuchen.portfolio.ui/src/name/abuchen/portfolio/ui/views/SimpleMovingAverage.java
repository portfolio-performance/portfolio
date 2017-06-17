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

    public static final int MIN_AVERAGE_PRICES_PER_WEEK = 2;

    /**
     * Calculates the Simple Moving Average for the given range of days from the
     * given startDate on The method returns an object containing the X and Y
     * Axes of the generated SMA
     * 
     * @return The ChartLineSeriesAxes contains the X and Y Axes of the
     *         generated SMA
     */
    public static ChartLineSeriesAxes getSMA(int rangeSMA, Security security, LocalDate startDate)
    {
        if (security == null)
            return null;

        int index;
        int minDays = SimpleMovingAverage.getMinimumDaysForSMA(rangeSMA);
        LocalDate[] dates;
        double[] values;

        SecurityPrice startPrice = null;
        ChartLineSeriesAxes lineSeries = new ChartLineSeriesAxes();
        List<SecurityPrice> prices = security.getPricesIncludingLatest();

        if (prices == null || prices.size() < minDays)
            return null;

        if (startDate == null)
        {
            startPrice = SimpleMovingAverage.getStartPrice(rangeSMA, prices);
            // in case no valid start date could be determined, return null
            if (startPrice == null) { return null; }
            index = prices.indexOf(startPrice);
            if (index >= prices.size()) { return null; }
            dates = new LocalDate[prices.size() - index];
            values = new double[prices.size() - index];
        }
        else
        {
            startPrice = SimpleMovingAverage.getStartPrice(rangeSMA, prices, startDate);
            // in case no valid start date could be determined, return null
            if (startPrice == null) { return null; }
            index = prices.indexOf(startPrice);
            if (index >= prices.size()) { return null; }
            dates = new LocalDate[prices.size() - index];
            values = new double[prices.size() - index];
        }

        for (int ii = 0; index < prices.size(); index++, ii++)
        {
            List<SecurityPrice> filteredPrices = null;
            double sum = 0;
            LocalDate nextDate = prices.get(index).getTime();
            LocalDate isBefore = nextDate.plusDays(1);
            LocalDate isAfter = isBefore.minusDays(rangeSMA + 1);
            filteredPrices = SimpleMovingAverage.getFilteredList(prices, isBefore, isAfter);

            if (filteredPrices.size() < minDays) { return null; }

            for (SecurityPrice price : filteredPrices)
            {
                sum += price.getValue();
            }

            dates[ii] = prices.get(index).getTime();
            values[ii] = sum / Values.Quote.divider() / filteredPrices.size();

        }

        lineSeries.setDates(TimelineChart.toJavaUtilDate(dates));
        lineSeries.setValues(values);

        return lineSeries;
    }

    public static int getMinimumDaysForSMA(int rangeSMA)
    {
        int weeks = rangeSMA / 7;
        int minDays = weeks * MIN_AVERAGE_PRICES_PER_WEEK;
        return minDays > 0 ? minDays : 1;

    }

    public static SecurityPrice getStartPrice(int rangeSMA, List<SecurityPrice> prices, LocalDate startDate)
    {
        // get Date of first possible SMA calculation beginning from startDate
        int index = Math.abs(
                        Collections.binarySearch(prices, new SecurityPrice(startDate, 0), new SecurityPrice.ByDate()));

        if (index >= prices.size()) { return null; }
        return determineStartPrice(rangeSMA, prices, startDate);
    }

    public static SecurityPrice getStartPrice(int rangeSMA, List<SecurityPrice> prices)
    {
        // get Date of first possible SMA calculation
        LocalDate smaPeriodEnd = prices.get(0).getTime().plusDays(rangeSMA - 1);
        int index = Math.abs(Collections.binarySearch(prices, new SecurityPrice(smaPeriodEnd, 0),
                        new SecurityPrice.ByDate()));
        if (index >= prices.size()) { return null; }

        return determineStartPrice(rangeSMA, prices, smaPeriodEnd);
    }

    private static SecurityPrice determineStartPrice(int rangeSMA, List<SecurityPrice> prices, LocalDate smaPeriodEnd)
    {
        // check if an SMA can be calculated for this Date
        List<SecurityPrice> filteredPrices = null;
        LocalDate isBefore = smaPeriodEnd.plusDays(1);
        LocalDate isAfter = smaPeriodEnd.minusDays(rangeSMA);
        LocalDate lastDate = prices.get(prices.size() - 1).getTime();
        filteredPrices = SimpleMovingAverage.getFilteredList(prices, isBefore, isAfter);
        int i = 1;
        while (!SimpleMovingAverage.checkListIsValidForSMA(filteredPrices, rangeSMA))
        {
            if (isBefore.plusDays(i).isAfter(lastDate) || isAfter.plusDays(i).isAfter(lastDate))
                return null;

            filteredPrices = SimpleMovingAverage.getFilteredList(prices, isBefore.plusDays(i), isAfter.plusDays(i));
            i++;
        }

        return filteredPrices.get(filteredPrices.size() - 1);
    }

    private static boolean checkListIsValidForSMA(List<SecurityPrice> filteredPrices, int rangeSMA)
    {

        return filteredPrices.size() < SimpleMovingAverage.getMinimumDaysForSMA(rangeSMA) ? false : true;

    }

    private static List<SecurityPrice> getFilteredList(List<SecurityPrice> prices, LocalDate isBefore,
                    LocalDate isAfter)
    {
        return prices.stream().filter(p -> p.getTime().isAfter(isAfter) && p.getTime().isBefore(isBefore))
                        .collect(Collectors.toList());

    }
}
