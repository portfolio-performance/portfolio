package name.abuchen.portfolio.ui.views;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.google.common.collect.Iterables;
import com.google.common.primitives.Doubles;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.views.SecuritiesChart.ChartInterval;

public class MovingAverageConvergenceDivergence
{

    private static final int MACD_MINUEND_PERIOD = 12;
    private static final int MACD_SUBTRAHEND_PERIOD = 26;
    private static final int SIGNAL_LINE_PERIOD = 9;
    private static final int MAX_MACD_PERIOD = Math.max(MACD_MINUEND_PERIOD, MACD_SUBTRAHEND_PERIOD);
    private static final int SIGNAL_LINE_START = SIGNAL_LINE_PERIOD + MAX_MACD_PERIOD - 1;
    private Security security;
    private ChartInterval interval;
    private ChartLineSeriesAxes macd;
    private ChartLineSeriesAxes signalLine;

    public MovingAverageConvergenceDivergence(Security security, ChartInterval interval)
    {
        this.security = Objects.requireNonNull(security);
        this.interval = Objects.requireNonNull(interval);

        calculateMacd();
    }

    public Optional<ChartLineSeriesAxes> getMacdLine()
    {
        return Optional.ofNullable(macd);
    }

    public Optional<ChartLineSeriesAxes> getSignalLine()
    {
        return Optional.ofNullable(signalLine);
    }

    private void calculateMacd()
    {
        List<SecurityPrice> prices = security.getPricesIncludingLatest();
        if (prices.isEmpty())
            return;

        if (Iterables.getLast(prices).getDate().isBefore(interval.getStart()))
            return;

        long count = 0L;
        List<LocalDate> datesMacd = new ArrayList<>();
        List<LocalDate> datesSignal = new ArrayList<>();
        List<Double> valuesMacd = new ArrayList<>();
        List<Double> valuesSignal = new ArrayList<>();

        ExponentialMovingAverageCalculator minuendEmaCalc = new ExponentialMovingAverageCalculator(MACD_MINUEND_PERIOD);
        ExponentialMovingAverageCalculator subtrahendEmaCalc = new ExponentialMovingAverageCalculator(
                        MACD_SUBTRAHEND_PERIOD);
        ExponentialMovingAverageCalculator signalEmaCalc = new ExponentialMovingAverageCalculator(SIGNAL_LINE_PERIOD);

        for (SecurityPrice price : prices) // NOSONAR
        {
            LocalDate date = price.getDate();
            if (date.isAfter(interval.getEnd()))
                break;

            double valueDivided = price.getValue() / Values.Quote.divider();
            double minuendEma = minuendEmaCalc.average(valueDivided);
            double subtrahendEma = subtrahendEmaCalc.average(valueDivided);
            double macdValue = minuendEma - subtrahendEma;
            double signalEma = ++count >= MAX_MACD_PERIOD ? signalEmaCalc.average(macdValue) : Double.NaN;

            if (date.isBefore(interval.getStart()))
                continue;

            if (count >= MAX_MACD_PERIOD)
            {
                valuesMacd.add(macdValue);
                datesMacd.add(date);
            }

            if (count >= SIGNAL_LINE_START)
            {
                valuesSignal.add(signalEma);
                datesSignal.add(date);
            }
        }

        if (valuesMacd.size() >= MAX_MACD_PERIOD)
        {
            macd = new ChartLineSeriesAxes();
            macd.setDates(datesMacd.toArray(new LocalDate[0]));
            macd.setValues(Doubles.toArray(valuesMacd));
        }

        if (valuesSignal.size() >= SIGNAL_LINE_START)
        {
            signalLine = new ChartLineSeriesAxes();
            signalLine.setDates(datesSignal.toArray(new LocalDate[0]));
            signalLine.setValues(Doubles.toArray(valuesSignal));
        }
    }

    private static final class ExponentialMovingAverageCalculator
    {
        private final double smoothingFactor;
        private final int period;
        private double previous = Double.NaN;
        private double initialTotal;
        private int initialCount;

        private ExponentialMovingAverageCalculator(int period)
        {
            this.smoothingFactor = 2.0 / (period + 1);
            this.period = period;
        }

        private double average(double value)
        {
            if (initialCount == period)
                previous = previous + smoothingFactor * (value - previous);
            else
            {
                initialTotal += value;
                if (++initialCount == period)
                    // Calculate the first EMA value
                    previous = initialTotal / initialCount;
            }
            return previous;
        }
    }
}
