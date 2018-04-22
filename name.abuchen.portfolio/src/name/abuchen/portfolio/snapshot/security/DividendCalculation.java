package name.abuchen.portfolio.snapshot.security;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MutableMoney;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceRecord.Periodicity;
import name.abuchen.portfolio.util.Dates;

/* package */class DividendCalculation extends Calculation
{
    /**
     * A dividend payment.
     */
    private static class DividendPayment
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
         * Constructs an instance.
         *
         * @param converter
         *            currency converter
         * @param t
         *            {@link DividendTransaction}
         */
        public DividendPayment(CurrencyConverter converter, DividendTransaction t)
        {
            this.amount = t.getGrossValue().with(converter.at(t.getDateTime()));
            LocalDateTime time = t.getDateTime();
            this.year = time.getYear();
            this.date = time.toLocalDate();
        }
    }

    private final List<DividendPayment> payments = new ArrayList<>();
    private Periodicity periodicity;
    private MutableMoney sum;

    @Override
    public void finish()
    {
        // first sort
        Collections.sort(payments, (r, l) -> r.date.compareTo(l.date));
        // default is unknown periodicity
        this.periodicity = Periodicity.UNKNOWN;

        if (payments.isEmpty())
        {
            this.periodicity = Periodicity.NONE;
            return;
        }

        // get first and last payment
        LocalDate firstPayment = payments.get(0).date;
        LocalDate lastPayment = payments.get(payments.size() - 1).date;

        int significantCount = 0;
        int insignificantYears = 0;

        // first calc total sum
        for (DividendPayment p : payments)
        {
            // add total sum
            sum.add(p.amount);
        }

        // now walk through individual years
        for (int year = firstPayment.getYear(); year <= lastPayment.getYear(); year++)
        {
            int countPerYear = 0;
            long sumPerYear = 0;

            // first calc sum for year
            for (DividendPayment p : payments)
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

            // expected amount
            double expectedAmount = sumPerYear / (double) countPerYear;

            // then calc significance
            for (DividendPayment p : payments)
            {
                if (p.year == year)
                {
                    // check if dividend contributes the expected amount (if
                    // it is not a very small extraordinary payment)
                    double significance = p.amount.getAmount() / expectedAmount;
                    if (significance > 0.3)
                    {
                        significantCount++;
                    }
                }
            }
        }

        if (significantCount == 0)
            return;

        // determine periodicity?
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

    public LocalDate getLastDividendPayment()
    {
        return payments.isEmpty() ? null : payments.get(payments.size() - 1).date;
    }

    public int getNumOfEvents()
    {
        return payments.size();
    }

    public Periodicity getPeriodicity()
    {
        return periodicity;
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
    public void visit(CurrencyConverter converter, DividendTransaction t)
    {
        // construct new payment and add it to the list
        payments.add(new DividendPayment(converter, t));
    }
}
