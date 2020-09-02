package name.abuchen.portfolio.snapshot;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.math.IRR;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.Dates;
import name.abuchen.portfolio.util.Interval;

/* package */class SecurityIndex extends PerformanceIndex
{
    private final PerformanceIndex clientIndex;
    private final Security security;

    /* package */ SecurityIndex(PerformanceIndex referenceIndex, Security security)
    {
        super(referenceIndex.getClient(), referenceIndex.getCurrencyConverter(), referenceIndex.getReportInterval());

        this.clientIndex = referenceIndex;
        this.security = security;
    }

    /* package */void calculate()
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
        
        // start date is set to date of first data point minus 1 day to have the
        // same logic as for non-benchmark items to get the first day's delta,
        // total and accumulated calculated correctly
        if (startDate.isAfter(actualInterval.getStart()))
        {
            startDate = startDate.minusDays(1);
        }

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
                                        ? clientIndex.getCurrencyConverter()
                                        : null;

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

    @Override
    public double getPerformanceIRR()
    {
        List<SecurityPrice> prices = security.getPricesIncludingLatest();
        if (prices.isEmpty())
            return 0d;

        LocalDate start = getReportInterval().getStart();
        LocalDate end = getReportInterval().getEnd();

        if (prices.get(0).getDate().isAfter(end))
            return 0d;

        if (prices.get(0).getDate().isAfter(start))
            start = prices.get(0).getDate();

        String currency = security.getCurrencyCode() == null ? getClient().getBaseCurrency()
                        : security.getCurrencyCode();

        List<LocalDate> dates = new ArrayList<>();
        List<Double> values = new ArrayList<>();

        // start value
        dates.add(start);
        values.add(-getCurrencyConverter()
                        .convert(start, Money.of(currency, security.getSecurityPrice(start).getValue())).getAmount()
                        / Values.Amount.divider());

        dates.add(end);
        values.add(getCurrencyConverter().convert(end, Money.of(currency, security.getSecurityPrice(end).getValue()))
                        .getAmount() / Values.Amount.divider());

        return IRR.calculate(dates, values);
    }

}
