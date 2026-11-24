package name.abuchen.portfolio.snapshot.security;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MutableMoney;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.security.BaseSecurityPerformanceRecord.Periodicity;
import name.abuchen.portfolio.util.Dates;

/* package */class DividendCalculation extends Calculation
{
    public record DividendCalculationResult(Money sum, LocalDate lastDividendPayment, int numOfEvents,
                    Periodicity periodicity, double rateOfReturnPerYear)
    {
    }

    /**
     * A dividend payment.
     */
    private static class Payment
    {
        /**
         * Amount of the payment.
         */
        public final Money amount;
        /**
         * Date of the payment.
         */
        public final LocalDate date;
        /**
         * Year of the payment.
         */
        public final int year;
        /**
         * Rate of return for this payment based on the moving average cost.
         */
        public final double rateOfReturn;
        /**
         * Market value of the shares of this payment at the payment date (in
         * term currency) if the rate of return is based on the quote, else 0.
         */
        public final long marketValue;
        /**
         * Account (owner) of the payment.
         */
        public final Object owner;

        /**
         * Constructs an instance.
         *
         * @param converter
         *            currency converter
         * @param t
         *            {@link DividendTransaction}
         * @param security
         *            {@link Security}
         */
        public Payment(CurrencyConverter converter, CalculationLineItem.DividendPayment t, Security security)
        {
            this.amount = t.getGrossValue().with(converter.at(t.getDateTime()));
            LocalDateTime time = t.getDateTime();
            this.year = time.getYear();
            this.date = time.toLocalDate();
            this.owner = t.getOwner();

            // try to set rate of return, default is NaN
            double rr = Double.NaN;
            long value = 0;
            if (security != null)
            {
                // calculate the rate of return, but do NOT use the method on
                // the DividendPayment class. Why? The DividendPayment looks
                // only at the payment, but the payment might only be for a
                // partial position (for example if the security is held in
                // multiple accounts). The moving average cost is always the
                // total costs.

                Money movingAverageCost = t.getMovingAverageCost();
                if (movingAverageCost != null && !movingAverageCost.isZero())
                {
                    // Use converted amount (in term currency) instead of raw amount (in transaction currency)
                    // to ensure both values are in the same currency for correct calculation
                    rr = this.amount.getAmount() / (double) movingAverageCost.getAmount();
                }

                // check if it is valid (non 0). There is no moving average
                // cost if the position was sold before the payment (for
                // example between ex-date and payment date)
                if (rr == 0 || Double.isNaN(rr))
                {
                    value = marketValueAtPayment(converter, t, security);
                    rr = Double.NaN;
                }
            }
            this.rateOfReturn = rr;
            this.marketValue = value;
        }

        /**
         * Calculates the market value of the shares of the payment at the date
         * of the payment. It is converted into the term currency because the
         * dividend might be booked in another currency than the currency of the
         * security.
         */
        private long marketValueAtPayment(CurrencyConverter converter, CalculationLineItem.DividendPayment t,
                        Security security)
        {
            // getSecurityPrice may return an empty price value, so check that
            long price = security.getSecurityPrice(date).getValue();
            long shares = t.getTransaction().map(Transaction::getShares).orElse(0L);
            if (price == 0 || shares == 0)
                return 0;

            long marketValue = BigDecimal.valueOf(shares) //
                            .movePointLeft(Values.Share.precision())
                            .multiply(BigDecimal.valueOf(price), Values.MC)
                            .movePointLeft(Values.Quote.precisionDeltaToMoney()) //
                            .setScale(0, RoundingMode.HALF_UP).longValue();

            var currencyCode = security.getCurrencyCode() != null ? security.getCurrencyCode()
                            : amount.getCurrencyCode();
            return Money.of(currencyCode, marketValue).with(converter.at(t.getDateTime())).getAmount();
        }
    }

    private final List<Payment> payments = new ArrayList<>();
    private Periodicity periodicity;
    private MutableMoney sum;
    private double rateOfReturnPerYear;

    @Override
    public void finish(CurrencyConverter converter, List<CalculationLineItem> lineItems)
    {
        // no payments result in no periodicity
        if (payments.isEmpty())
        {
            this.periodicity = Periodicity.NONE;
            return;
        }

        // default is unknown periodicity
        this.periodicity = Periodicity.UNKNOWN;

        // first sort
        Collections.sort(payments, (r, l) -> r.date.compareTo(l.date));

        // get first and last payment
        LocalDate firstPayment = payments.get(0).date;
        LocalDate lastPayment = payments.get(payments.size() - 1).date;

        int significantCount = 0;
        int insignificantYears = 0;
        double sumRateOfReturn = 0;

        // payments based on the quote are grouped by payment date: a payment
        // split across several accounts must give the same rate of return as
        // a single payment. The market value is counted once per account
        // (the largest of the day), because several distributions on the
        // same day into the same account (e.g. a regular and a special
        // dividend) relate to the same shares.
        Map<LocalDate, Long> amountBasedOnQuote = new HashMap<>();
        Map<LocalDate, Map<Object, Long>> valueBasedOnQuote = new HashMap<>();

        // first calc total sum of all payments
        for (Payment p : payments)
        {
            // add to total sum
            sum.add(p.amount);

            if (p.marketValue != 0)
            {
                amountBasedOnQuote.merge(p.date, p.amount.getAmount(), Long::sum);
                valueBasedOnQuote.computeIfAbsent(p.date, d -> new HashMap<>()).merge(p.owner, p.marketValue,
                                Math::max);
            }
            // a payment without cost and without quote has no rate of
            // return and must not turn the result into NaN
            else if (!Double.isNaN(p.rateOfReturn))
            {
                sumRateOfReturn += p.rateOfReturn;
            }
        }

        for (var entry : amountBasedOnQuote.entrySet())
        {
            long value = valueBasedOnQuote.get(entry.getKey()).values().stream().mapToLong(Long::longValue).sum();
            sumRateOfReturn += entry.getValue() / (double) value;
        }

        int years = 0;

        // now walk through individual years
        for (int year = firstPayment.getYear(); year <= lastPayment.getYear(); year++)
        {
            years++;
            int countPerYear = 0;
            long sumPerYear = 0;
            LocalDate lastDate = null;

            // first calc sum only for this year
            for (Payment p : payments)
            {
                if (p.year == year)
                {
                    countPerYear++;
                    sumPerYear += p.amount.getAmount();
                }
            }

            // skip years with no dividend payments
            if (countPerYear == 0)
            {
                insignificantYears++;
                continue;
            }

            // calc expected amount for this year
            double expectedAmount = sumPerYear / (double) countPerYear;

            // then calc significance
            for (Payment p : payments)
            {
                if (p.year == year)
                {
                    // check if dividend contributes the expected amount (if
                    // it is not a very small extraordinary payment below 30% of
                    // the expected one)
                    double significance = p.amount.getAmount() / expectedAmount;
                    if (significance > 0.3)
                    {
                        // check, if dividends were recorded for multiple
                        // accounts at the same date
                        if (lastDate == null || !p.date.equals(lastDate))
                        {
                            significantCount++;
                        }
                    }
                    lastDate = p.date;
                }
            }
        }

        this.rateOfReturnPerYear = sumRateOfReturn / years;

        // determine periodicity?
        if (significantCount > 0)
        {
            // days in current time range
            int days = Dates.daysBetween(firstPayment, lastPayment) - (insignificantYears * 365);
            long daysBetweenPayments = Math.round(days / (double) (significantCount - 1));

            // just check payments inbetween one year
            if (daysBetweenPayments < 430)
            {
                if (daysBetweenPayments > 270)
                {
                    this.periodicity = Periodicity.ANNUAL;
                }
                else if (daysBetweenPayments > 130)
                {
                    this.periodicity = Periodicity.SEMIANNUAL;
                }
                else if (daysBetweenPayments > 60)
                {
                    this.periodicity = Periodicity.QUARTERLY;
                }
                else if (daysBetweenPayments > 20)
                {
                    this.periodicity = Periodicity.MONTHLY;
                }
            }
        }
    }

    public DividendCalculationResult getResult()
    {
        return new DividendCalculationResult(sum.toMoney(), getLastDividendPayment(), payments.size(), periodicity,
                        rateOfReturnPerYear);
    }

    public LocalDate getLastDividendPayment()
    {
        return payments.isEmpty() ? null : payments.get(payments.size() - 1).date;
    }

    public int getNumOfEvents()
    {
        return payments.size();
    }

    public List<Payment> getPayments()
    {
        return payments;
    }

    public Periodicity getPeriodicity()
    {
        return periodicity;
    }

    public double getRateOfReturnPerYear()
    {
        return rateOfReturnPerYear;
    }

    public Money getSum()
    {
        return sum.toMoney();
    }

    @Override
    public void setTermCurrency(String termCurrency)
    {
        super.setTermCurrency(termCurrency);
        this.sum = MutableMoney.of(termCurrency);
    }

    @Override
    public void visit(CurrencyConverter converter, CalculationLineItem.DividendPayment t)
    {
        // construct new payment and add it to the list
        payments.add(new Payment(converter, t, getSecurity()));
    }
}
