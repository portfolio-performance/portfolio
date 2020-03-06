package name.abuchen.portfolio.ui.views;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.google.common.primitives.Doubles;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.util.chart.TimelineChart;
import name.abuchen.portfolio.ui.views.SecuritiesChart.ChartInterval;

public class ExponentialMovingAverage
{
    private int rangeEMA;
    private double smoothingFactor;
    private Security security;
    private ChartInterval interval;
    private ChartLineSeriesAxes result;

    public ExponentialMovingAverage(int rangeEMA, Security security, ChartInterval interval)
    {
        this.rangeEMA = rangeEMA;
        this.smoothingFactor = 2.0 / (this.rangeEMA + 1);
        this.security = security;
        this.interval = Objects.requireNonNull(interval);

        this.result = new ChartLineSeriesAxes();

        calculateEMA();
    }

    /**
     * Returns the calculated Exponential Moving Average
     *
     * @return The ChartLineSeriesAxes contains the X and Y Axes of the
     *         generated EMA
     */
    public ChartLineSeriesAxes getEMA()
    {
        return this.result;
    }

    /**
     * Calculates the Exponential Moving Average for the given range of days
     * from the given startDate
     * <p/>
     * Compared to the Simple Moving Average, the Exponential Moving Average
     * puts more emphasis on recent values and discounts older values faster.
     * <p/>
     * To calculate the EMA we also only need the previous EMA and a smoothing
     * factor which id derived from the number of days in the range.
     * 
     * <pre>
     *      smoothingFactor = 2 / ( range + 1 )
     *      EMA = ( value(t) * smoothingFactor ) + ( EMA(t-1) * ( 1 - smoothingFactor ) )
     * </pre>
     * 
     * Where range is the number of days to average and value(t) is the current
     * value, for example, the daily closing price
     * <p/>
     * The method returns an object containing the X and Y Axes of the generated
     * EMA
     */
    private void calculateEMA()
    {
        if (security == null)
            return;

        List<SecurityPrice> prices = security.getPricesIncludingLatest();
        if (prices == null)
            return;

        int index = Collections.binarySearch(prices, new SecurityPrice(interval.getStart(), 0),
                        new SecurityPrice.ByDate());

        if (index < 0)
            index = -index - 1;

        if (index >= prices.size())
            return;

        List<LocalDate> datesEMA = new ArrayList<>();
        List<Double> valuesEMA = new ArrayList<>();

        // see the running EMA with the first value
        double ema = prices.get(index).getValue() / Values.Quote.divider();

        for (; index < prices.size(); index++) // NOSONAR
        {
            LocalDate date = prices.get(index).getDate();
            if (date.isAfter(interval.getEnd()))
                break;

            ema = (prices.get(index).getValue() / Values.Quote.divider() * smoothingFactor)
                            + (ema * (1 - smoothingFactor));

            valuesEMA.add(ema);
            datesEMA.add(date);
        }

        result.setDates(TimelineChart.toJavaUtilDate(datesEMA.toArray(new LocalDate[0])));
        result.setValues(Doubles.toArray(valuesEMA));
    }
}
