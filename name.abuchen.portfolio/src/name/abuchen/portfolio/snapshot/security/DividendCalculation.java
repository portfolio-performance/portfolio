package name.abuchen.portfolio.snapshot.security;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
     *
     * @author SB
     */
    private static class DividendPayment implements Comparable<DividendPayment>
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

        @Override
        public int compareTo(DividendPayment o)
        {
            return this.date.compareTo(o.date);
        }
    }

    private LocalDate firstPayment;
    private LocalDate lastPayment;
    private final List<DividendPayment> payments = new ArrayList<DividendPayment>();
    private Periodicity periodicity;
    private MutableMoney sum;

    @Override
    public void finish()
    {
        // first sort
        Collections.sort(payments);
        // default is unknown periodicity
        this.periodicity = Periodicity.UNKNOWN;
        // check payments?
        if (!payments.isEmpty())
        {
            // get first and last payment
            firstPayment = payments.get(0).date;
            lastPayment = payments.get(payments.size() - 1).date;

            HashMap<Integer, MutableMoney> hmSumPerYear = new HashMap<Integer, MutableMoney>();
            // sum up dividends
            for (DividendPayment p : payments)
            {
                // add total sum
                sum.add(p.amount);
                // add sum per year
                MutableMoney m = hmSumPerYear.get(p.year);
                if (m == null)
                {
                    // construct if missing
                    m = MutableMoney.of(sum.getCurrencyCode());
                    hmSumPerYear.put(p.year, m);
                }
                m.add(p.amount);
            }

            int significantCount = 0;
            // now walk through individual years
            for (int year = firstPayment.getYear(); year <= lastPayment.getYear(); year++)
            {
                int count = 0;
                // first calc sum for year
                MutableMoney m = MutableMoney.of(sum.getCurrencyCode());
                for (DividendPayment p : payments)
                {
                    if (p.year == year)
                    {
                        m.add(p.amount);
                        count++;
                    }
                }
                double sum = m.getAmount();
                // expected amount
                double expectedAmount = sum / count;
                // then calc significance
                for (DividendPayment p : payments)
                {
                    if (p.year == year)
                    {
                        // check if dividend contributes the expected amount (if
                        // it is not a very small extraordinary payment)
                        double significance = (p.amount.getAmount()) / expectedAmount;
                        if (significance > 0.3)
                        {
                            significantCount++;
                        }
                    }
                }
            }
            // determine periodicity?
            if (significantCount > 0)
            {
                // days in current time range
                int days = Dates.daysBetween(firstPayment, lastPayment);
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
        else
        {
            // no payments
            this.periodicity = Periodicity.NONE;
        }
    }

    public LocalDate getLastDividendPayment()
    {
        return lastPayment;
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
