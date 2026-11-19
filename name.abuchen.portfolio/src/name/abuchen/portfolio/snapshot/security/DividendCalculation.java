package name.abuchen.portfolio.snapshot.security;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
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
         * Rate of return for this payment.
         */
        public final double rateOfReturn;
        /**
         * Accrued interest (in term currency) offset against this payment.
         */
        private long accruedInterest;

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

                Money movingAverageCost = t.getMovingAverageCost();
                if (movingAverageCost != null && !movingAverageCost.isZero())
                {
                    // Use converted amount (in term currency) instead of raw amount (in transaction currency)
                    // to ensure both values are in the same currency for correct calculation
                    rr = this.amount.getAmount() / (double) movingAverageCost.getAmount();
                }

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
                        rr = t.getDividendPerShare() / sharePriceAmount;
                    }
                }
            }
            this.rateOfReturn = rr;
        }

        /**
         * Rate of return including the accrued interest offset against this
         * payment. The rate of return is the payment divided by the cost (or
         * the price), hence it is scaled by the adjusted amount.
         */
        public double getRateOfReturnInclAccruedInterest()
        {
            if (accruedInterest == 0)
                return rateOfReturn;
            return rateOfReturn * (amount.getAmount() + accruedInterest) / amount.getAmount();
        }
    }

    /**
     * Accrued interest of a purchase (negative) or sale (positive).
     */
    private record AccruedInterest(LocalDate date, long amount)
    {
    }

    private final List<Payment> payments = new ArrayList<>();
    private final List<AccruedInterest> accruedInterestItems = new ArrayList<>();
    private Periodicity periodicity;
    private MutableMoney sum;

    /**
     * Accrued interest paid with purchases (negative) and received with sales
     * (positive). It is part of the interest income of the security, but not
     * a payment (it does not change periodicity and number of payments).
     */
    private MutableMoney accruedInterest;
    private double rateOfReturnPerYear;

    @Override
    public void finish(CurrencyConverter converter, List<CalculationLineItem> lineItems)
    {
        sum.add(accruedInterest.toMoney());

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

        offsetAccruedInterest();

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
            sumRateOfReturn += p.getRateOfReturnInclAccruedInterest();
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

    /**
     * Adjusts the rate of return of the payments by the accrued interest: the
     * accrued interest paid with a purchase reduces the first payment on or
     * after the purchase, the accrued interest received with a sale increases
     * the last payment on or before the sale. Accrued interest without such a
     * payment does not change the rate of return. The amount of the payments
     * (and therefore periodicity and significance) is not changed.
     */
    private void offsetAccruedInterest()
    {
        for (AccruedInterest item : accruedInterestItems)
        {
            Payment target = null;

            if (item.amount() < 0)
            {
                for (Payment p : payments)
                {
                    if (!p.date.isBefore(item.date()) && !p.amount.isZero())
                    {
                        target = p;
                        break;
                    }
                }
            }
            else
            {
                for (Payment p : payments)
                {
                    if (!p.date.isAfter(item.date()) && !p.amount.isZero())
                        target = p;
                }
            }

            if (target != null)
                target.accruedInterest += item.amount();
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
        this.accruedInterest = MutableMoney.of(termCurrency);
    }

    @Override
    public void visit(CurrencyConverter converter, CalculationLineItem.DividendPayment t)
    {
        // construct new payment and add it to the list
        payments.add(new Payment(converter, t, getSecurity()));
    }

    @Override
    public void visit(CurrencyConverter converter, CalculationLineItem.TransactionItem item, PortfolioTransaction t)
    {
        Money interest = t.getAccruedInterest(converter);
        if (interest.isZero())
            return;

        if (t.getType().isPurchase())
            accruedInterest.subtract(interest);
        else
            accruedInterest.add(interest);

        long signed = t.getType().isPurchase() ? -interest.getAmount() : interest.getAmount();
        accruedInterestItems.add(new AccruedInterest(t.getDateTime().toLocalDate(), signed));
    }
}
