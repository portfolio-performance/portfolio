package name.abuchen.portfolio.snapshot.security;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MutableMoney;
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
        public final long costBaseAmount;

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
        public Payment(CurrencyConverter converter, CalculationLineItem.DividendPayment t)
        {
            this.amount = t.getGrossValue().with(converter.at(t.getDateTime()));

            LocalDateTime time = t.getDateTime();
            this.year = time.getYear();
            this.date = time.toLocalDate();

            Money movingAverageCost = t.getMovingAverageCost();
            long costBase = 0;

            if (movingAverageCost != null && !movingAverageCost.isZero())
            {
                costBase = movingAverageCost.getAmount();

                if (t.getTransaction().isPresent() //
                                && t.getTransaction().get().getShares() > 0 //
                                && t.getTotalShares() > 0 //
                                && t.getTransaction().get().getShares() < t.getTotalShares())
                {
                    costBase = Math.round(movingAverageCost.getAmount()
                                    * (t.getTransaction().get().getShares() / (double) t.getTotalShares()));
                }
            }

            this.costBaseAmount = costBase;
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

        // first calc total sum of all payments
        for (Payment p : payments)
        {
            // add to total sum
            sum.add(p.amount);
        }


        // now walk through individual years
        for (int year = firstPayment.getYear(); year <= lastPayment.getYear(); year++)
        {
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

        this.rateOfReturnPerYear = calculateRateOfReturnPerYear(firstPayment.getYear(), lastPayment.getYear());

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

    private double calculateRateOfReturnPerYear(int firstYear, int lastYear)
    {
        double sumYearlyRateOfReturn = 0;
        int yearsWithValidRateOfReturn = 0;

        for (int year = firstYear; year <= lastYear; year++)
        {
            double yearlyRateOfReturn = 0;
            boolean hasValidRateOfReturn = false;

            LocalDate currentDate = null;
            long dividendAmountOnDate = 0;
            long costBaseAmountOnDate = 0;

            for (Payment p : payments)
            {
                if (p.year != year)
                    continue;

                if (currentDate == null)
                {
                    currentDate = p.date;
                }
                else if (!currentDate.equals(p.date))
                {
                    if (dividendAmountOnDate > 0 && costBaseAmountOnDate > 0)
                    {
                        yearlyRateOfReturn += dividendAmountOnDate / (double) costBaseAmountOnDate;
                        hasValidRateOfReturn = true;
                    }

                    currentDate = p.date;
                    dividendAmountOnDate = 0;
                    costBaseAmountOnDate = 0;
                }

                dividendAmountOnDate += p.amount.getAmount();

                if (p.costBaseAmount > 0)
                    costBaseAmountOnDate += p.costBaseAmount;
            }

            if (dividendAmountOnDate > 0 && costBaseAmountOnDate > 0)
            {
                yearlyRateOfReturn += dividendAmountOnDate / (double) costBaseAmountOnDate;
                hasValidRateOfReturn = true;
            }

            if (hasValidRateOfReturn)
            {
                sumYearlyRateOfReturn += yearlyRateOfReturn;
                yearsWithValidRateOfReturn++;
            }
        }

        return yearsWithValidRateOfReturn > 0 ? sumYearlyRateOfReturn / yearsWithValidRateOfReturn : Double.NaN;
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
        payments.add(new Payment(converter, t));
    }
}
