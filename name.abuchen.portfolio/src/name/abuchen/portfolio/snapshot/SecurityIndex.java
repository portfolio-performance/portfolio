package name.abuchen.portfolio.snapshot;

import java.time.LocalDate;
import java.util.List;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.util.Dates;
import name.abuchen.portfolio.util.Interval;

/* package */class SecurityIndex extends PerformanceIndex
{
    /* package */SecurityIndex(Client client, CurrencyConverter converter, ReportingPeriod reportInterval)
    {
        super(client, converter, reportInterval);
    }

    /* package */void calculate(PerformanceIndex clientIndex, Security security)
    {
        List<SecurityPrice> prices = security.getPrices();
        if (prices.isEmpty())
        {
            initEmpty(clientIndex);
            return;
        }

        Interval actualInterval = clientIndex.getActualInterval();

        LocalDate firstPricePoint = prices.get(0).getTime();
        if (firstPricePoint.isAfter(actualInterval.getEnd()))
        {
            initEmpty(clientIndex);
            return;
        }

        LocalDate startDate = clientIndex.getFirstDataPoint().orElse(actualInterval.getEnd());
        if (firstPricePoint.isAfter(startDate))
            startDate = firstPricePoint;

        LocalDate endDate = actualInterval.getEnd();
        LocalDate lastPricePoint = prices.get(prices.size() - 1).getTime();

        if (lastPricePoint.isBefore(endDate))
            endDate = lastPricePoint;

        int size = Dates.daysBetween(startDate, endDate) + 1;
        if (size <= 0)
        {
            initEmpty(clientIndex);
            return;
        }

        dates = new LocalDate[size];
        delta = new double[size];
        accumulated = new double[size];
        transferals = new long[size];
        totals = new long[size];

        final double adjustment = clientIndex.getAccumulatedPercentage()[Dates.daysBetween(actualInterval.getStart(),
                        startDate)];

        // first value = reference value
        dates[0] = startDate;
        delta[0] = 0;
        accumulated[0] = adjustment;
        long valuation = totals[0] = security.getSecurityPrice(startDate).getValue();

        // calculate series
        int index = 1;
        LocalDate date = startDate.plusDays(1);
        while (date.compareTo(endDate) <= 0)
        {
            dates[index] = date;

            long thisValuation = totals[index] = security.getSecurityPrice(date).getValue();
            long thisDelta = thisValuation - valuation;

            delta[index] = (double) thisDelta / (double) valuation;
            accumulated[index] = ((accumulated[index - 1] + 1 - adjustment) * (delta[index] + 1)) - 1 + adjustment;

            date = date.plusDays(1);
            valuation = thisValuation;
            index++;
        }
    }

    private void initEmpty(PerformanceIndex clientIndex)
    {
        LocalDate startDate = clientIndex.getFirstDataPoint().orElse(clientIndex.getActualInterval().getStart());

        dates = new LocalDate[] { startDate };
        delta = new double[] { 0d };
        accumulated = new double[] { 0d };
        transferals = new long[] { 0 };
        totals = new long[] { 0 };
    }
}
