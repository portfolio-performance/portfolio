package name.abuchen.portfolio.ui.views;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
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
    private List<SecurityPrice> prices;
    private List<LocalDate> datesSMA;
    private List<Double> valuesSMA;

    public SimpleMovingAverage(int rangeSMA, Security security, LocalDate startDate)
    {
        this.rangeSMA = rangeSMA;
        this.security = security;
        this.startDate = startDate;
        this.SMA = new ChartLineSeriesAxes();
        this.datesSMA = new ArrayList<>();
        this.valuesSMA = new ArrayList<>();
        this.calculateSMA();
    }

    /**
     * Returns the calculated Simple Moving Average
     * 
     * @return The ChartLineSeriesAxes contains the X and Y Axes of the
     *         generated SMA
     */
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
     */
    private void calculateSMA()
    {
        if (security == null)
            return;

        this.prices = security.getPricesIncludingLatest();
        int index;

        SecurityPrice startPrice = null;

        if (prices == null || prices.size() < rangeSMA + 3)
            return;

        if (startDate == null)
        {
            index = 0;
        }
        else
        {
            index = Math.abs(
                            Collections.binarySearch(prices, new SecurityPrice(startDate, 0), new SecurityPrice.ByDate()));
            if (index == -1)
            {
                index = 0;
            } else {
                index = Math.abs(index);
            }
            if (index >= prices.size())
                return;
        }

        for (; index < prices.size(); index++)
        {
            if (index < rangeSMA) continue; 
            LocalDate nextDate = prices.get(index).getDate();
            LocalDate isBefore = nextDate.plusDays(1);
            LocalDate isAfter = prices.get(index - rangeSMA + 2).getDate();
            List<SecurityPrice> filteredPrices = this.getFilteredList(isBefore, isAfter);

            double sum = filteredPrices.stream().mapToLong(SecurityPrice::getValue).sum();

            valuesSMA.add(sum / Values.Quote.divider() / filteredPrices.size());
            datesSMA.add(prices.get(index).getDate());
        }
        LocalDate[] tmpDates = datesSMA.toArray(new LocalDate[0]);
        Double[] tmpPrices = valuesSMA.toArray(new Double[0]);

        this.SMA.setDates(TimelineChart.toJavaUtilDate(tmpDates));
        this.SMA.setValues(ArrayUtils.toPrimitive(tmpPrices));
    }

    private List<SecurityPrice> getFilteredList(LocalDate isBefore, LocalDate isAfter)
    {
        return prices.stream().filter(p -> p.getDate().isAfter(isAfter) && p.getDate().isBefore(isBefore))
                        .collect(Collectors.toList());
    }
}
