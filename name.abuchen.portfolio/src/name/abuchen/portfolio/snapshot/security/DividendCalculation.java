package name.abuchen.portfolio.snapshot.security;

import java.time.LocalDate;

import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MutableMoney;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceRecord.Periodicity;
import name.abuchen.portfolio.util.Dates;

/* package */class DividendCalculation extends Calculation
{
    private MutableMoney sum;
    private int numOfEvents;

    private LocalDate firstPayment;
    private LocalDate lastPayment;

    private int regularEvents;

    @Override
    public void setTermCurrency(String termCurrency)
    {
        super.setTermCurrency(termCurrency);
        this.sum = MutableMoney.of(termCurrency);
    }

    @Override
    public void visit(CurrencyConverter converter, DividendTransaction t)
    {
        sum.add(t.getGrossValue().with(converter.at(t.getDateTime())));
        numOfEvents++;

        if (t.getShares() > 0
                        && (lastPayment == null || Dates.daysBetween(lastPayment, t.getDateTime().toLocalDate()) > 30))
        {
            regularEvents++;
        }

        if (firstPayment == null)
            firstPayment = t.getDateTime().toLocalDate();
        lastPayment = t.getDateTime().toLocalDate();
    }

    public Money getSum()
    {
        return sum.toMoney();
    }

    public int getNumOfEvents()
    {
        return numOfEvents;
    }

    public LocalDate getLastDividendPayment()
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
