package name.abuchen.portfolio.snapshot;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.util.Dates;
import name.abuchen.portfolio.util.Interval;

/* package */class SecurityIndex extends PerformanceIndex
{
    /* package */ SecurityIndex(Client client, CurrencyConverter converter, ReportingPeriod reportInterval)
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

        // prices only include historical quotes, not the latest quote. Merge
        // the latest quote into the list if necessary
        prices = security.getPricesIncludingLatest();

        Interval actualInterval = clientIndex.getActualInterval();

        LocalDate firstPricePoint = prices.get(0).getDate();
        if (firstPricePoint.isAfter(actualInterval.getEnd()))
        {
            initEmpty(clientIndex);
            return;
        }

        LocalDate startDate = clientIndex.getFirstDataPoint().orElse(actualInterval.getEnd());
        if (firstPricePoint.isAfter(startDate))
            startDate = firstPricePoint;

        LocalDate endDate = actualInterval.getEnd();
        LocalDate lastPricePoint = prices.get(prices.size() - 1).getDate();

        if (lastPricePoint.isBefore(endDate))
            endDate = lastPricePoint;

        int size = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (size <= 0)
        {
            initEmpty(clientIndex);
            return;
        }

        // needs currency conversion if
        // a) the currency of the security is not null
        // (otherwise it is an index)
        // b) the term currency differs from the currency of the security

        CurrencyConverter converter = security.getCurrencyCode() != null
                        && !security.getCurrencyCode().equals(clientIndex.getCurrencyConverter().getTermCurrency())
                                        ? clientIndex.getCurrencyConverter() : null;

        dates = new LocalDate[size];
        delta = new double[size];
        accumulated = new double[size];
        inboundTransferals = new long[size];
        outboundTransferals = new long[size];
        totals = new long[size];

        final double adjustment = clientIndex.getAccumulatedPercentage()[Dates.daysBetween(actualInterval.getStart(),
                        startDate)];

        // first value = reference value
        dates[0] = startDate;
        delta[0] = 0;
        accumulated[0] = adjustment;
        long valuation = totals[0] = convert(converter, security, startDate);

        // calculate series
        int index = 1;
        LocalDate date = startDate.plusDays(1);
        while (date.compareTo(endDate) <= 0)
        {
            dates[index] = date;

            long thisValuation = totals[index] = convert(converter, security, date);
            long thisDelta = thisValuation - valuation;

            delta[index] = (double) thisDelta / (double) valuation;
            accumulated[index] = ((accumulated[index - 1] + 1 - adjustment) * (delta[index] + 1)) - 1 + adjustment;

            date = date.plusDays(1);
            valuation = thisValuation;
            index++;
        }
    }

    private long convert(CurrencyConverter converter, Security security, LocalDate date)
    {
        SecurityPrice price = security.getSecurityPrice(date);
        if (converter == null)
            return price.getValue();

        // use the picked date for currency conversion, not the date of the
        // quote. This could differ for example on weekends.
        return converter.convert(date, Money.of(security.getCurrencyCode(), price.getValue())).getAmount();
    }

    private void initEmpty(PerformanceIndex clientIndex)
    {
        LocalDate startDate = clientIndex.getFirstDataPoint().orElse(clientIndex.getActualInterval().getStart());

        dates = new LocalDate[] { startDate };
        delta = new double[] { 0d };
        accumulated = new double[] { 0d };
        inboundTransferals = new long[] { 0 };
        outboundTransferals = new long[] { 0 };
        totals = new long[] { 0 };
    }
}
