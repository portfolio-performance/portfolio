package name.abuchen.portfolio.snapshot;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.math.IRR;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * Calculates the money-weighted return for every point of a
 * {@link PerformanceIndex}. Each value represents the IRR from the beginning
 * of the reporting period through the corresponding date.
 */
public final class IRRSeries
{
    private IRRSeries()
    {
    }

    public static double[] calculate(PerformanceIndex index)
    {
        if (index instanceof SecurityIndex securityIndex)
            return calculateBenchmark(securityIndex);

        return calculateInvestment(index);
    }

    public static LocalDate[] getDates(PerformanceIndex index)
    {
        var dates = index.getDates().clone();

        if (index instanceof SecurityIndex && dates.length > 0)
        {
            var end = index.getReportInterval().getEnd();
            if (dates[dates.length - 1].isBefore(end))
                dates[dates.length - 1] = end;
        }

        return dates;
    }

    private static double[] calculateInvestment(PerformanceIndex index)
    {
        LocalDate[] indexDates = index.getDates();
        long[] totals = index.getTotals();
        long[] inboundTransferals = index.getInboundTransferals();
        long[] outboundTransferals = index.getOutboundTransferals();

        double[] answer = new double[indexDates.length];

        if (indexDates.length == 0)
            return answer;

        List<LocalDate> dates = new ArrayList<>();
        List<Double> values = new ArrayList<>();

        // The reporting interval is half-open: (start, end]. The valuation at
        // the start therefore represents the initial investment while external
        // cash flows on the start date itself are not part of the interval.
        if (totals[0] != 0)
        {
            dates.add(indexDates[0]);
            values.add(-toValue(totals[0]));
        }

        for (int ii = 0; ii < indexDates.length; ii++)
        {
            if (ii > 0)
            {
                double externalCashFlow = toValue(outboundTransferals[ii]) - toValue(inboundTransferals[ii]);
                if (externalCashFlow != 0d)
                {
                    dates.add(indexDates[ii]);
                    values.add(externalCashFlow);
                }
            }

            boolean hasFinalValuation = totals[ii] != 0;
            if (hasFinalValuation)
            {
                dates.add(indexDates[ii]);
                values.add(toValue(totals[ii]));
            }

            answer[ii] = values.isEmpty() ? 0d : IRR.calculate(dates, values);

            // The valuation is the terminal cash flow for this point only. The
            // next point must continue with external cash flows accumulated so
            // far, not with the previous day's valuation.
            if (hasFinalValuation)
            {
                dates.remove(dates.size() - 1);
                values.remove(values.size() - 1);
            }
        }

        return answer;
    }

    private static double[] calculateBenchmark(SecurityIndex index)
    {
        LocalDate[] indexDates = getDates(index);
        double[] answer = new double[indexDates.length];

        Security security = index.getSecurity();
        List<SecurityPrice> prices = security.getPricesIncludingLatest();
        if (prices.isEmpty())
            return answer;

        LocalDate start = index.getReportInterval().getStart();
        if (prices.get(0).getDate().isAfter(start))
            start = prices.get(0).getDate();

        String currency = security.getCurrencyCode() == null ? index.getClient().getBaseCurrency()
                        : security.getCurrencyCode();

        double startValue = index.getCurrencyConverter()
                        .convert(start, Money.of(currency, security.getSecurityPrice(start).getValue())).getAmount()
                        / Values.Amount.divider();

        for (int ii = 0; ii < indexDates.length; ii++)
        {
            LocalDate end = indexDates[ii];
            if (end.isBefore(start))
                continue;

            double endValue = index.getCurrencyConverter()
                            .convert(end, Money.of(currency, security.getSecurityPrice(end).getValue())).getAmount()
                            / Values.Amount.divider();

            answer[ii] = IRR.calculate(List.of(start, end), List.of(-startValue, endValue));
        }

        return answer;
    }

    private static double toValue(long value)
    {
        return value / Values.Amount.divider();
    }
}
