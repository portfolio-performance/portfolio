package name.abuchen.portfolio.ui.views.earnings;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.eclipse.jface.preference.IPreferenceStore;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.ui.util.ClientFilterMenu;
import name.abuchen.portfolio.util.Interval;

public class EarningsViewModel
{
    public enum Mode
    {
        DIVIDENDS(AccountTransaction.Type.DIVIDENDS), //
        INTEREST(AccountTransaction.Type.INTEREST, AccountTransaction.Type.INTEREST_CHARGE), //
        ALL(AccountTransaction.Type.DIVIDENDS, AccountTransaction.Type.INTEREST,
                        AccountTransaction.Type.INTEREST_CHARGE);

        private Set<AccountTransaction.Type> types;

        private Mode(AccountTransaction.Type first, AccountTransaction.Type... rest)
        {
            this.types = EnumSet.of(first, rest);
        }

        public boolean isIncluded(AccountTransaction transaction)
        {
            return this.types.contains(transaction.getType());
        }
    }

    @FunctionalInterface
    public interface UpdateListener
    {
        void onUpdate();
    }

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

        public int getNoOfMonths()
        {
            return values.length;
        }
    }

    private List<UpdateListener> listeners = new ArrayList<>();

    private final CurrencyConverter converter;
    private final Client client;

    private final ClientFilterMenu clientFilter;

    private int startYear;
    private int noOfmonths;
    private List<Line> lines;
    private Line sum;
    private List<TransactionPair<AccountTransaction>> transactions;

    private Mode mode = Mode.ALL;
    private boolean useGrossValue = true;

    public EarningsViewModel(IPreferenceStore preferences, CurrencyConverter converter, Client client)
    {
        this.converter = converter;
        this.client = client;

        this.clientFilter = new ClientFilterMenu(client, preferences, filter -> recalculate());

        String selection = preferences
                        .getString(EarningsViewModel.class.getSimpleName() + ClientFilterMenu.PREF_KEY_POSTFIX);
        if (selection != null)
            this.clientFilter.getAllItems().filter(item -> item.getUUIDs().equals(selection)).findAny()
                            .ifPresent(this.clientFilter::select);

        this.clientFilter.addListener(filter -> preferences.putValue(
                        EarningsViewModel.class.getSimpleName() + ClientFilterMenu.PREF_KEY_POSTFIX,
                        this.clientFilter.getSelectedItem().getUUIDs()));
    }

    public void configure(int startYear, Mode mode, boolean useGrossValue)
    {
        this.startYear = startYear;
        this.mode = mode;
        this.useGrossValue = useGrossValue;

        recalculate();
    }

    /* package */Client getClient()
    {
        return client;
    }

    public ClientFilterMenu getClientFilterMenu()
    {
        return clientFilter;
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

    public Mode getMode()
    {
        return mode;
    }

    public void setMode(Mode mode)
    {
        this.mode = mode;
        recalculate();
    }

    public boolean usesGrossValue()
    {
        return useGrossValue;
    }

    public void setUseGrossValue(boolean useGrossValue)
    {
        this.useGrossValue = useGrossValue;
        recalculate();
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
        this.startYear = year;
        recalculate();
    }

    public void recalculate()
    {
        calculate();
        fireUpdateChange();
    }

    private void calculate()
    {
        // determine the number of full months within period
        LocalDate now = LocalDate.now();
        if (startYear > now.getYear())
            throw new IllegalArgumentException();
        this.noOfmonths = (now.getYear() - startYear) * 12 + now.getMonthValue();

        Interval interval = Interval.of(LocalDate.of(startYear - 1, Month.DECEMBER, 31), now);
        Predicate<Transaction> predicate = t -> interval.contains(t.getDateTime());

        Map<InvestmentVehicle, Line> vehicle2line = new HashMap<>();

        this.sum = new Line(null, this.noOfmonths);
        this.transactions = new ArrayList<>();

        Client filteredClient = clientFilter.getSelectedFilter().filter(client);

        for (Account account : filteredClient.getAccounts())
        {
            for (AccountTransaction t : account.getTransactions()) // NOSONAR
            {
                if (!mode.isIncluded(t))
                    continue;

                if (!predicate.test(t))
                    continue;

                transactions.add(new TransactionPair<>(account, t));

                Money dividendValue = useGrossValue ? t.getGrossValue() : t.getMonetaryAmount();
                long value = dividendValue.with(converter.at(t.getDateTime())).getAmount();
                if (t.getType().isDebit())
                    value *= -1;

                int index = (t.getDateTime().getYear() - startYear) * 12 + t.getDateTime().getMonthValue() - 1;

                InvestmentVehicle vehicle = t.getSecurity() != null ? t.getSecurity() : account;
                Line line = vehicle2line.computeIfAbsent(vehicle, s -> new Line(s, noOfmonths));
                line.values[index] += value;
                line.sum += value;

                sum.values[index] += value;
                sum.sum += value;
            }
        }

        this.lines = new ArrayList<>(vehicle2line.values());
    }

    public void addUpdateListener(UpdateListener listener)
    {
        this.listeners.add(listener);
    }

    protected void fireUpdateChange()
    {
        this.listeners.stream().forEach(UpdateListener::onUpdate);
    }
}
