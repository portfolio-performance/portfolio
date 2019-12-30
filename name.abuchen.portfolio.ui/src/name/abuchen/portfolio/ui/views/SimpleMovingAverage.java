package name.abuchen.portfolio.ui.views;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.ArrayUtils;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.util.chart.TimelineChart;
import name.abuchen.portfolio.ui.views.SecuritiesChart.ChartInterval;

public class SimpleMovingAverage
{
    public static final int MIN_AVERAGE_PRICES_PER_WEEK = 2;

    private int rangeSMA;
    private Security security;
    private ChartInterval interval;
    private ChartLineSeriesAxes result;

    public SimpleMovingAverage(int rangeSMA, Security security, ChartInterval interval)
    {
        this.rangeSMA = rangeSMA;
        this.security = security;
        this.interval = Objects.requireNonNull(interval);

        this.result = new ChartLineSeriesAxes();

        calculateSMA();
    }

    /**
     * Returns the calculated Simple Moving Average
     * 
     * @return The ChartLineSeriesAxes contains the X and Y Axes of the
     *         generated SMA
     */
    public ChartLineSeriesAxes getSMA()
    {
        return this.result;
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

        List<SecurityPrice> prices = security.getPricesIncludingLatest();
        if (prices == null || prices.size() < rangeSMA)
            return;

        int index = Collections.binarySearch(prices, new SecurityPrice(interval.getStart(), 0),
                        new SecurityPrice.ByDate());

        if (index < 0)
            index = -index - 1;

        if (index >= prices.size())
            return;

        List<LocalDate> datesSMA = new ArrayList<>();
        List<Double> valuesSMA = new ArrayList<>();

        for (; index < prices.size(); index++) // NOSONAR
        {
            if (index < rangeSMA - 1)
                continue;
            
            LocalDate date = prices.get(index).getDate();
            if (date.isAfter(interval.getEnd()))
                break;

            List<SecurityPrice> filteredPrices = prices.subList(index - rangeSMA + 1, index + 1);
            long sum = filteredPrices.stream().mapToLong(SecurityPrice::getValue).sum();

            valuesSMA.add(sum / Values.Quote.divider() / filteredPrices.size());
            datesSMA.add(date);
        }
        
        LocalDate[] tmpDates = datesSMA.toArray(new LocalDate[0]);
        Double[] tmpPrices = valuesSMA.toArray(new Double[0]);

        result.setDates(TimelineChart.toJavaUtilDate(tmpDates));
        result.setValues(ArrayUtils.toPrimitive(tmpPrices));
    }
}
