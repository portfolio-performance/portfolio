package name.abuchen.portfolio.ui.views.dividends;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.snapshot.ReportingPeriod;

public class DividendsViewModel
{
    public static class Line
    {
        private InvestmentVehicle vehicle;
        private long[] values;
        private long sum;

        public Line(InvestmentVehicle vehicle, int length)
        {
            this.vehicle = vehicle;
            this.values = new long[length];
        }

        public InvestmentVehicle getVehicle()
        {
            return vehicle;
        }

        public long getValue(int index)
        {
            return values[index];
        }

        public long getSum()
        {
            return sum;
        }
    }

    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    private final CurrencyConverter converter;
    private final Client client;

    private int startYear = 2016;

    private int noOfmonths;
    private List<Line> lines;
    private Line sum;
    private List<TransactionPair<AccountTransaction>> transactions;

    public DividendsViewModel(CurrencyConverter converter, Client client)
    {
        this.converter = converter;
        this.client = client;
    }

    public int getStartYear()
    {
        return startYear;
    }

    public int getNoOfMonths()
    {
        return noOfmonths;
    }

    public List<Line> getLines()
    {
        return lines;
    }

    public Line getSum()
    {
        return sum;
    }

    /**
     * Returns all lines including the sum line
     */
    public List<Line> getAllLines()
    {
        List<Line> answer = new ArrayList<>();
        answer.addAll(lines);
        answer.add(sum);
        return answer;
    }

    public List<TransactionPair<AccountTransaction>> getTransactions()
    {
        return transactions;
    }

    public void updateWith(int year)
    {
        int oldStartYear = this.startYear;
        this.startYear = year;

        calculate();

        firePropertyChange("startYear", oldStartYear, this.startYear); //$NON-NLS-1$
    }

    private void calculate()
    {
        // determine the number of full months within period
        LocalDate now = LocalDate.now();
        if (startYear > now.getYear())
            throw new IllegalArgumentException();
        this.noOfmonths = (now.getYear() - startYear) * 12 + now.getMonthValue();

        Predicate<Transaction> predicate = new ReportingPeriod.FromXtoY(LocalDate.of(startYear - 1, Month.DECEMBER, 31),
                        now).containsTransaction();

        Map<InvestmentVehicle, Line> vehicle2line = new HashMap<>();

        this.sum = new Line(null, this.noOfmonths);
        this.transactions = new ArrayList<>();

        for (Account account : client.getAccounts())
        {
            for (AccountTransaction t : account.getTransactions())
            {
                if (t.getType() != AccountTransaction.Type.DIVIDENDS)
                    continue;

                if (!predicate.test(t))
                    continue;

                transactions.add(new TransactionPair<AccountTransaction>(account, t));

                long value = converter.at(t.getDate()).apply(t.getMonetaryAmount()).getAmount();
                int index = (t.getDate().getYear() - startYear) * 12 + t.getDate().getMonthValue() - 1;

                Line line = vehicle2line.computeIfAbsent(t.getSecurity(), s -> new Line(s, noOfmonths));
                line.values[index] += value;
                line.sum += value;

                sum.values[index] += value;
                sum.sum += value;
            }
        }

        this.lines = new ArrayList<>(vehicle2line.values());
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener)
    {
        propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
    }

    protected void firePropertyChange(String attribute, Object oldValue, Object newValue)
    {
        propertyChangeSupport.firePropertyChange(attribute, oldValue, newValue);
    }
}
