package name.abuchen.portfolio.snapshot.security;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MutableMoney;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceRecord.Periodicity;
import name.abuchen.portfolio.util.Dates;

/* package */class DividendCalculation extends Calculation
{
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
         * Rate of return for this payment.
         */
        public final double rateOfReturn;

        public final Money fifoCost;

        /**
         * Constructs an instance.
         *
         * @param converter
         *            currency converter
         * @param diviPayment
         *            {@link DividendTransaction}
         * @param security
         *            {@link Security}
         */
        public Payment(CurrencyConverter converter, CalculationLineItem.DividendPayment diviPayment, Security security)
        {
            this.amount = diviPayment.getGrossValue().with(converter.at(diviPayment.getDateTime()));
            LocalDateTime time = diviPayment.getDateTime();
            this.year = time.getYear();
            this.date = time.toLocalDate();
            fifoCost = diviPayment.getFifoCost();

            // try to set rate of return, default is NaN
            double rr = Double.NaN;
            if (security != null)
            {
                // calculate the rate of return, but do NOT use the method on
                // the DividendPayment class. Why? The DividendPayment looks
                // only at the payment, but the payment might only be for a
                // partial position (for example if the security is held in
                // multiple accounts). The moving average cost is always the
                // total costs.

                Money movingAverageCost = diviPayment.getMovingAverageCost();
                if (movingAverageCost != null && !movingAverageCost.isZero())
                    rr = diviPayment.getGrossValueAmount() / (double) movingAverageCost.getAmount();

                // check if it is valid (non 0)
                if (rr == 0)
                {
                    // else use the security price at that date
                    SecurityPrice p = security.getSecurityPrice(date);
                    // getSecurityPrice may return an empty price value, so
                    // check that
                    long pValue = p.getValue();
                    if (pValue != 0)
                    {
                        double sharePriceAmount = ((double) pValue) / Values.Quote.factor()
                                        * Values.AmountFraction.factor();
                        rr = diviPayment.getDividendPerShare() / sharePriceAmount;
                    }
                }
            }
            this.rateOfReturn = rr;
        }
    }

    private final List<Payment> payments = new ArrayList<>();
    private Periodicity periodicity;
    private MutableMoney sum;
    private double rateOfReturnPerYear;
    public double yieldOnCostPerYear;

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

        // first calc total sum of all payments
        for (Payment p : payments)
        {
            // add to total sum
            sum.add(p.amount);
            sumRateOfReturn += p.rateOfReturn;
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
        yieldOnCostPerYear = calculateYieldOnCost(years, LocalDate.now());

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

    public double getYieldOnCost()
    {
        return yieldOnCostPerYear;
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

    /**
     * calculates the dividend yield on cost for one year. yield is based on the
     * last dividends and not on an average dividend payment over time for this
     * security.
     * 
     * @param years
     *            between first and last dividend payments
     * @return yield from 0.0 to 1.0
     */
    double calculateYieldOnCost(int years, LocalDate now)
    {
        long fifoCostAmount = 0L;
        for (int i = payments.size() - 1; i >= 0; i--)
        {
            Payment p = payments.get(i);
            if (p.fifoCost != null && p.fifoCost.isPositive())
            {
                fifoCostAmount = p.fifoCost.getAmount();
                break;
            }
        }
        if (fifoCostAmount > 0)
        {
            double fifoCosts = fifoCostAmount * Values.AmountFraction.factor();

            LocalDate firstDividendPaymentAccepted = now.minusYears(1);
            double diviSum = years > 1 ? payments.stream() //
                            .filter(p -> p.date.isAfter(firstDividendPaymentAccepted)) //
                            .mapToDouble(p -> p.amount.getAmount() * Values.AmountFraction.factor()) //
                            .sum() //
                            : sum.getAmount() * Values.AmountFraction.factor() / years;

            return diviSum / fifoCosts;
        }
        return 0.0;
    }

}
