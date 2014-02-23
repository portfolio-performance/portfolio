package name.abuchen.portfolio.snapshot.security;

import java.util.Date;

import name.abuchen.portfolio.snapshot.security.SecurityPerformanceRecord.Periodicity;
import name.abuchen.portfolio.util.Dates;

/* package */class DividendCalculation extends Calculation
{
    private long sum;
    private int numOfEvents;

    private Date firstPayment;
    private Date lastPayment;

    private int regularEvents;

    @Override
    public void visit(DividendTransaction t)
    {
        sum += t.getAmount();
        numOfEvents++;

        if (t.getShares() > 0 && (lastPayment == null || Dates.daysBetween(lastPayment, t.getDate()) > 30))
        {
            regularEvents++;
        }

        if (firstPayment == null)
            firstPayment = t.getDate();
        lastPayment = t.getDate();
    }

    public long getSum()
    {
        return sum;
    }

    public int getNumOfEvents()
    {
        return numOfEvents;
    }

    public Date getLastDividendPayment()
    {
        return lastPayment;
    }

    public Periodicity getPeriodicity()
    {
        if (firstPayment == null)
            return Periodicity.NONE;

        if (regularEvents == 0)
            return Periodicity.NONE;

        if (regularEvents == 1)
            return Periodicity.UNKNOWN;

        int days = Dates.daysBetween(firstPayment, lastPayment);

        long daysBetweenPayments = Math.round(days / (double) (regularEvents - 1));

        if (daysBetweenPayments > 400)
            return Periodicity.UNKNOWN;
        else if (daysBetweenPayments > 300)
            return Periodicity.ANNUAL;
        else if (daysBetweenPayments > 150)
            return Periodicity.SEMIANNUAL;
        else if (daysBetweenPayments > 75)
            return Periodicity.QUARTERLY;
        else
            return Periodicity.UNKNOWN;
    }
}
