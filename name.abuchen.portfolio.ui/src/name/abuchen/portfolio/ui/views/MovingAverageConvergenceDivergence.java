package name.abuchen.portfolio.ui.views;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import com.google.common.primitives.Doubles;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.util.chart.TimelineChart;
import name.abuchen.portfolio.ui.views.SecuritiesChart.ChartInterval;

public class MovingAverageConvergenceDivergence
{

    private static final int MACD_MINUEND_PERIOD = 12;
    private static final int MACD_SUBTRAHEND_PERIOD = 26;
    private static final int SIGNAL_LINE_PERIOD = 9;
    private double macdMinuendSmoothingFactor;
    private double macdSubtrahendSmoothingFactor;
    private double signalLineSmoothingFactor;
    private Security security;
    private ChartInterval interval;
    private ChartLineSeriesAxes macd;
    private ChartLineSeriesAxes signalLine;

    public MovingAverageConvergenceDivergence(Security security, ChartInterval interval)
    {
        this.security = security;
        this.interval = Objects.requireNonNull(interval);
        this.macdMinuendSmoothingFactor = 2.0 / (MACD_MINUEND_PERIOD + 1);
        this.macdSubtrahendSmoothingFactor = 2.0 / (MACD_SUBTRAHEND_PERIOD + 1);
        this.signalLineSmoothingFactor = 2.0 / (SIGNAL_LINE_PERIOD + 1);
        this.macd = new ChartLineSeriesAxes();
        this.signalLine = new ChartLineSeriesAxes();

        calculateMacd();
    }

    public ChartLineSeriesAxes getMacdLine()
    {
        return macd;
    }

    public ChartLineSeriesAxes getSignalLine()
    {
        return signalLine;
    }

    private void calculateMacd()
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

        List<LocalDate> datesMacd = new ArrayList<>();
        List<Double> valuesMacd = new ArrayList<>();
        List<Double> valuesSignal = new ArrayList<>();

        // seed the running EMAs with the first value
        double minuendEma = prices.get(index).getValue() / Values.Quote.divider();
        double subtrahendEma = minuendEma;
        double signalEma = 0;

        for (SecurityPrice price : prices) // NOSONAR
        {
            LocalDate date = price.getDate();
            if (date.isAfter(interval.getEnd()))
                break;

            long value = price.getValue();

            minuendEma = (value / Values.Quote.divider() * macdMinuendSmoothingFactor)
                            + (minuendEma * (1 - macdMinuendSmoothingFactor));
            subtrahendEma = (value / Values.Quote.divider() * macdSubtrahendSmoothingFactor)
                            + (subtrahendEma * (1 - macdSubtrahendSmoothingFactor));
            double macdValue = minuendEma - subtrahendEma;
            signalEma = (macdValue * signalLineSmoothingFactor) + (signalEma * (1 - signalLineSmoothingFactor));

            if (date.isBefore(interval.getStart()))
                continue;

            valuesMacd.add(macdValue);
            valuesSignal.add(signalEma);
            datesMacd.add(date);
        }

        Date[] timelineDates = TimelineChart.toJavaUtilDate(datesMacd.toArray(new LocalDate[0]));
        macd.setDates(timelineDates);
        signalLine.setDates(timelineDates);
        macd.setValues(Doubles.toArray(valuesMacd));
        signalLine.setValues(Doubles.toArray(valuesSignal));
    }

}
