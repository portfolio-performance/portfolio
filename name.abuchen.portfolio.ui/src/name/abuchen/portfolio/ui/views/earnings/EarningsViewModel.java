package name.abuchen.portfolio.ui.views.earnings;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.eclipse.jface.preference.IPreferenceStore;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.snapshot.trades.Trade;
import name.abuchen.portfolio.snapshot.trades.TradeCollector;
import name.abuchen.portfolio.snapshot.trades.TradeCollectorException;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.ClientFilterMenu;
import name.abuchen.portfolio.util.Interval;

public class EarningsViewModel
{
    public enum Mode
    {
        DIVIDENDS(Messages.LabelDividends, AccountTransaction.Type.DIVIDENDS), //
        INTEREST(Messages.LabelInterest, AccountTransaction.Type.INTEREST, AccountTransaction.Type.INTEREST_CHARGE), //
        EARNINGS(Messages.LabelEarnings, AccountTransaction.Type.DIVIDENDS, AccountTransaction.Type.INTEREST,
                        AccountTransaction.Type.INTEREST_CHARGE), //
        TAXES(Messages.ColumnTaxes, AccountTransaction.Type.DIVIDENDS, AccountTransaction.Type.INTEREST,
                        AccountTransaction.Type.INTEREST_CHARGE, AccountTransaction.Type.TAXES,
                        AccountTransaction.Type.TAX_REFUND), //
        FEES(Messages.ColumnFees, AccountTransaction.Type.DIVIDENDS, AccountTransaction.Type.INTEREST,
                        AccountTransaction.Type.INTEREST_CHARGE, AccountTransaction.Type.FEES,
                        AccountTransaction.Type.FEES_REFUND), //
        TRADES(Messages.LabelEarningsTradeProfitLoss, null), //
        ALL("\u2211", AccountTransaction.Type.DIVIDENDS, AccountTransaction.Type.INTEREST, //$NON-NLS-1$
                        AccountTransaction.Type.INTEREST_CHARGE, AccountTransaction.Type.TAXES,
                        AccountTransaction.Type.TAX_REFUND, AccountTransaction.Type.FEES,
                        AccountTransaction.Type.FEES_REFUND);

        private String label;
        private Set<AccountTransaction.Type> types;

        private Mode(String label, AccountTransaction.Type first, AccountTransaction.Type... rest)
        {
            this.label = label;
            this.types = first == null ? EnumSet.noneOf(AccountTransaction.Type.class) : EnumSet.of(first, rest);
        }

        public String getLabel()
        {
            return label;
        }

        public boolean isAccountTxIncluded(AccountTransaction transaction)
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
        private boolean consolidateRetired;
        private long[] values;
        private long sum;

        public Line(InvestmentVehicle vehicle, boolean consolidateRetired, int length)
        {
            this.vehicle = vehicle;
            this.consolidateRetired = consolidateRetired;
            this.values = new long[length];
        }

        public InvestmentVehicle getVehicle()
        {
            return vehicle;
        }

        public boolean getConsolidatedRetired()
        {
            return consolidateRetired;
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

    private CurrencyConverter converter;
    private final Client client;

    private final ClientFilterMenu clientFilter;

    private int startYear;
    private int noOfmonths;
    private List<Line> lines;
    private Line sum;
    private Line sumRetired;
    private List<TransactionPair<?>> transactions = new ArrayList<>();

    private Mode mode = Mode.ALL;
    private boolean useGrossValue = true;
    private boolean useConsolidateRetired = true;

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

    public void configure(int startYear, Mode mode, boolean useGrossValue, boolean useConsolidateRetired)
    {
        this.startYear = startYear;
        this.mode = mode;
        this.useGrossValue = useGrossValue;
        this.useConsolidateRetired = useConsolidateRetired;

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

    public Line getSumRetired()
    {
        return sumRetired;
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

    public boolean usesConsolidateRetired()
    {
        return useConsolidateRetired;
    }

    public void setUseConsolidateRetired(boolean useConsolidateRetired)
    {
        this.useConsolidateRetired = useConsolidateRetired;
        recalculate();
    }

    /**
     * Returns all lines including the sum line
     */
    public List<Line> getAllLines()
    {
        List<Line> answer = new ArrayList<>();
        answer.addAll(lines);
        if (useConsolidateRetired)
            answer.add(sumRetired);
        answer.add(sum);
        return answer;
    }

    public List<TransactionPair<?>> getTransactions()
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
        // the base currency might have changed
        this.converter = this.converter.with(client.getBaseCurrency());

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
        Predicate<Transaction> checkIsInInterval = t -> interval.contains(t.getDateTime());

        Map<InvestmentVehicle, Line> vehicle2line = new HashMap<>();

        this.sum = new Line(null, false, this.noOfmonths);
        this.sumRetired = new Line(null, useConsolidateRetired, this.noOfmonths);
        this.transactions = new ArrayList<>();

        Client filteredClient = clientFilter.getSelectedFilter().filter(client);

        EnumSet<Mode> processGainTx = EnumSet.of(Mode.TRADES, Mode.ALL);
        if (processGainTx.contains(mode))
        {
            List<Trade> trades = collectTrades(filteredClient);

            for (Trade trade : trades)
            {
                final Optional<LocalDateTime> end = trade.getEnd();
                if (!end.isPresent() || !interval.contains(end.get()))
                    continue;

                long value = 0;
                value = trade.getGrossProfitLoss().getAmount();

                if (value != 0)
                {
                    int index = (end.get().getYear() - startYear) * 12 + end.get().getMonthValue() - 1;
                    InvestmentVehicle vehicle = trade.getSecurity();
                    if (useConsolidateRetired && vehicle.isRetired())
                    {
                        sumRetired.values[index] += value;
                        sumRetired.sum += value;
                    }
                    else
                    {
                        Line line = vehicle2line.computeIfAbsent(vehicle, s -> new Line(s, false, noOfmonths));
                        line.values[index] += value;
                        line.sum += value;
                    }

                    sum.values[index] += value;
                    sum.sum += value;
                }
            }
        }

        EnumSet<Mode> processPorfolioTx = EnumSet.of(Mode.TAXES, Mode.FEES, Mode.ALL);
        if (processPorfolioTx.contains(mode))
        {
            for (Portfolio portfolio : filteredClient.getPortfolios())
            {
                for (PortfolioTransaction transaction : portfolio.getTransactions())
                {
                    if (!checkIsInInterval.test(transaction))
                        continue;

                    long value = 0;
                    switch (mode)
                    {
                        case TAXES:
                            value -= transaction.getUnitSum(Unit.Type.TAX).with(converter.at(transaction.getDateTime()))
                                            .getAmount();
                            break;
                        case FEES:
                            value -= transaction.getUnitSum(Unit.Type.FEE).with(converter.at(transaction.getDateTime()))
                                            .getAmount();
                            break;
                        case ALL:
                            value -= transaction.getUnitSum(Unit.Type.TAX).with(converter.at(transaction.getDateTime()))
                                            .getAmount();
                            value -= transaction.getUnitSum(Unit.Type.FEE).with(converter.at(transaction.getDateTime()))
                                            .getAmount();
                            break;

                        default:
                    }

                    if (value != 0)
                    {
                        transactions.add(new TransactionPair<>(portfolio, transaction));

                        int index = (transaction.getDateTime().getYear() - startYear) * 12
                                        + transaction.getDateTime().getMonthValue() - 1;

                        InvestmentVehicle vehicle = transaction.getSecurity();
                        if (useConsolidateRetired && vehicle.isRetired())
                        {
                            sumRetired.values[index] += value;
                            sumRetired.sum += value;
                        }
                        else
                        {
                            Line line = vehicle2line.computeIfAbsent(vehicle, s -> new Line(s, false, noOfmonths));
                            line.values[index] += value;
                            line.sum += value;
                        }
                        sum.values[index] += value;
                        sum.sum += value;
                    }
                }
            }
        }

        for (Account account : filteredClient.getAccounts())
        {
            for (AccountTransaction transaction : account.getTransactions()) // NOSONAR
            {
                if (!mode.isAccountTxIncluded(transaction))
                    continue;

                if (!checkIsInInterval.test(transaction))
                    continue;

                long value = 0;
                switch (mode)
                {
                    case TAXES:
                        if (transaction.getType() == AccountTransaction.Type.TAXES
                                        || transaction.getType() == AccountTransaction.Type.TAX_REFUND)
                        {
                            value = transaction.getMonetaryAmount().with(converter.at(transaction.getDateTime()))
                                            .getAmount();
                            if (transaction.getType().isDebit())
                                value *= -1;
                        }
                        else
                        {
                            value -= transaction.getUnitSum(Unit.Type.TAX).with(converter.at(transaction.getDateTime()))
                                            .getAmount();
                        }
                        break;
                    case FEES:
                        if (transaction.getType() == AccountTransaction.Type.FEES
                                        || transaction.getType() == AccountTransaction.Type.FEES_REFUND)
                        {
                            value = transaction.getMonetaryAmount().with(converter.at(transaction.getDateTime()))
                                            .getAmount();
                            if (transaction.getType().isDebit())
                                value *= -100;
                        }
                        else
                        {
                            value -= transaction.getUnitSum(Unit.Type.FEE).with(converter.at(transaction.getDateTime()))
                                            .getAmount();
                        }
                        break;
                    case TRADES:
                        break;
                    case ALL:
                        value = transaction.getMonetaryAmount().with(converter.at(transaction.getDateTime()))
                                        .getAmount();
                        if (transaction.getType().isDebit())
                            value *= -1;
                        break;
                    default:
                        value = (useGrossValue ? transaction.getGrossValue() : transaction.getMonetaryAmount())
                                        .with(converter.at(transaction.getDateTime())).getAmount();
                        if (transaction.getType().isDebit())
                            value *= -1;
                }

                if (value != 0)
                {
                    transactions.add(new TransactionPair<>(account, transaction));

                    int index = (transaction.getDateTime().getYear() - startYear) * 12
                                    + transaction.getDateTime().getMonthValue() - 1;

                    InvestmentVehicle vehicle = transaction.getSecurity() != null ? transaction.getSecurity() : account;
                    if (useConsolidateRetired && vehicle.isRetired())
                    {
                        sumRetired.values[index] += value;
                        sumRetired.sum += value;
                    }
                    else
                    {
                        Line line = vehicle2line.computeIfAbsent(vehicle, s -> new Line(s, false, noOfmonths));
                        line.values[index] += value;
                        line.sum += value;
                    }

                    sum.values[index] += value;
                    sum.sum += value;
                }
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

    public List<Trade> collectTrades(Client filteredClient)
    {
        TradeCollector collector = new TradeCollector(filteredClient, converter);
        List<Trade> trades = new ArrayList<>();
        getClient().getSecurities().forEach(s -> {
            try
            {
                trades.addAll(collector.collect(s));
            }
            catch (TradeCollectorException e)
            {
                PortfolioPlugin.log(e);
            }
        });
        return trades;
    }
}
