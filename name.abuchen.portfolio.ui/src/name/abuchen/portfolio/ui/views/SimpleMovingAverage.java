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
    private int rangeSMA;
    private Security security;
    private LocalDate startDate;
    private ChartLineSeriesAxes SMA;
    private int calculatedMinimumDays;
    private List<SecurityPrice> prices;

    public SimpleMovingAverage(int rangeSMA, Security security, LocalDate startDate)
    {
        this.rangeSMA = rangeSMA;
        this.security = security;
        this.startDate = startDate;
        this.SMA = new ChartLineSeriesAxes();
        this.calculatedMinimumDays = getMinimumDaysForSMA();
        this.calculateSMA();
    }

    public ChartLineSeriesAxes getSMA()
    {
        return this.SMA;
    }

    public SimpleMovingAverage calculateSMA(int rangeSMA, Security security, LocalDate startDate)
    {
        return new SimpleMovingAverage(rangeSMA, security, startDate);
    }

    /**
     * Calculates the Simple Moving Average for the given range of days from the
     * given startDate on The method returns an object containing the X and Y
     * Axes of the generated SMA
     * 
     * @return The ChartLineSeriesAxes contains the X and Y Axes of the
     *         generated SMA
     */
    private void calculateSMA()
    {
        if (security == null)
            return;

        this.prices = security.getPricesIncludingLatest();
        int index;
        LocalDate[] dates;
        double[] values;

        SecurityPrice startPrice = null;

        if (prices == null || prices.size() < calculatedMinimumDays)
            return;

        if (startDate == null)
        {
            startPrice = this.getStartPrice();
            // in case no valid start date could be determined, return null
            if (startPrice == null)
                return;
            index = prices.indexOf(startPrice);
            if (index >= prices.size())
                return;
            dates = new LocalDate[prices.size() - index];
            values = new double[prices.size() - index];
        }
        else
        {
            startPrice = this.getStartPriceFromStartDate();
            // in case no valid start date could be determined, return null
            if (startPrice == null)
                return;
            index = prices.indexOf(startPrice);
            if (index >= prices.size())
                return;
            dates = new LocalDate[prices.size() - index];
            values = new double[prices.size() - index];
        }

        for (int ii = 0; index < prices.size(); index++, ii++)
        {
            LocalDate nextDate = prices.get(index).getTime();
            LocalDate isBefore = nextDate.plusDays(1);
            LocalDate isAfter = isBefore.minusDays(rangeSMA + 1L);
            List<SecurityPrice> filteredPrices = this.getFilteredList(isBefore, isAfter);

            if (filteredPrices.size() < calculatedMinimumDays)
                return;

            double sum = filteredPrices.stream().mapToLong(SecurityPrice::getValue).sum();

            dates[ii] = prices.get(index).getTime();
            values[ii] = sum / Values.Quote.divider() / filteredPrices.size();

        }

        this.SMA.setDates(TimelineChart.toJavaUtilDate(dates));
        this.SMA.setValues(values);
    }

    public int getMinimumDaysForSMA()
    {
        int weeks = rangeSMA / 7;
        int minDays = weeks * MIN_AVERAGE_PRICES_PER_WEEK;
        return minDays > 0 ? minDays : 1;
    }

    public SecurityPrice getStartPriceFromStartDate()
    {
        // get Date of first possible SMA calculation beginning from startDate
        int index = Math.abs(
                        Collections.binarySearch(prices, new SecurityPrice(startDate, 0), new SecurityPrice.ByDate()));

        if (index >= prices.size())
            return null;
        return determineStartPrice(startDate);
    }

    public SecurityPrice getStartPrice()
    {
        // get Date of first possible SMA calculation
        LocalDate smaPeriodEnd = prices.get(0).getTime().plusDays(rangeSMA - 1L);
        int index = Math.abs(Collections.binarySearch(prices, new SecurityPrice(smaPeriodEnd, 0),
                        new SecurityPrice.ByDate()));
        if (index >= prices.size())
            return null;

        return determineStartPrice(smaPeriodEnd);
    }

    private SecurityPrice determineStartPrice(LocalDate smaPeriodEnd)
    {
        // check if an SMA can be calculated for this Date
        List<SecurityPrice> filteredPrices = null;
        LocalDate isBefore = smaPeriodEnd.plusDays(1);
        LocalDate isAfter = smaPeriodEnd.minusDays(rangeSMA);
        LocalDate lastDate = prices.get(prices.size() - 1).getTime();
        filteredPrices = this.getFilteredList(isBefore, isAfter);
        
        int i = 1;
        while (!this.checkListIsValidForSMA(filteredPrices))
        {
            if (isBefore.plusDays(i).isAfter(lastDate) || isAfter.plusDays(i).isAfter(lastDate))
                return null;

            filteredPrices = this.getFilteredList(isBefore.plusDays(i), isAfter.plusDays(i));
            i++;
        }

        return filteredPrices.get(filteredPrices.size() - 1);
    }

    private boolean checkListIsValidForSMA(List<SecurityPrice> filteredPrices)
    {
        return filteredPrices.size() < calculatedMinimumDays ? false : true;
    }

    private List<SecurityPrice> getFilteredList(LocalDate isBefore, LocalDate isAfter)
    {
        return prices.stream().filter(p -> p.getTime().isAfter(isAfter) && p.getTime().isBefore(isBefore))
                        .collect(Collectors.toList());
    }
}
