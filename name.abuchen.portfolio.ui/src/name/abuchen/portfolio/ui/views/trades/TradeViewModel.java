package name.abuchen.portfolio.ui.views.trades;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.eclipse.jface.preference.IPreferenceStore;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.snapshot.trades.Trade;
import name.abuchen.portfolio.snapshot.trades.TradeCollector;
import name.abuchen.portfolio.snapshot.trades.TradeCollectorException;
import name.abuchen.portfolio.ui.util.ClientFilterMenu;
import name.abuchen.portfolio.util.Interval;

public class TradeViewModel
{
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
    private List<Trade> tradesClosed = new ArrayList<>();
    private List<Trade> tradesOpen = new ArrayList<>();

    public TradeViewModel(IPreferenceStore preferences, CurrencyConverter converter, Client client)
    {
        this.converter = converter;
        this.client = client;

        this.clientFilter = new ClientFilterMenu(client, preferences, filter -> recalculate());

        String selection = preferences
                        .getString(TradeViewModel.class.getSimpleName() + ClientFilterMenu.PREF_KEY_POSTFIX);
        if (selection != null)
            this.clientFilter.getAllItems().filter(item -> item.getUUIDs().equals(selection)).findAny()
                            .ifPresent(this.clientFilter::select);

        this.clientFilter.addListener(filter -> preferences.putValue(
                        TradeViewModel.class.getSimpleName() + ClientFilterMenu.PREF_KEY_POSTFIX,
                        this.clientFilter.getSelectedItem().getUUIDs()));
    }

    public void configure(int startYear)
    {
        this.startYear = startYear;

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

    public List<Trade> getTradesClosed()
    {
        return tradesClosed;
    }

    public List<Trade> getTradesOpen()
    {
        return tradesOpen;
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
        Predicate<Trade> checkIsInInterval = t -> interval.contains(t.getEnd().get());

        Map<InvestmentVehicle, Line> vehicle2line = new HashMap<>();

        this.sum = new Line(null, this.noOfmonths);
        this.tradesOpen = new ArrayList<>();
        this.tradesClosed = new ArrayList<>();

        Client filteredClient = clientFilter.getSelectedFilter().filter(client);

        List<Trade> tradesFilteredClient = collectTrade(filteredClient);
        if (!tradesFilteredClient.isEmpty())
        {
            for (Trade trade : tradesFilteredClient)
            {
                if (!trade.getEnd().isPresent())
                {
                    tradesOpen.add(trade);
                    continue;
                }
                if (!checkIsInInterval.test(trade))
                    continue;

                long value = 0;
                value = trade.getProfitLoss().getAmount();

                if (value != 0)
                {

                    tradesClosed.add(trade);
                    int index = (trade.getEnd().get().getYear() - startYear) * 12 + trade.getEnd().get().getMonthValue()
                                    - 1;
                    Line line = vehicle2line.computeIfAbsent(trade.getSecurity(), s -> new Line(s, noOfmonths));

                    line.values[index] += value;
                    line.sum += value;

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

    public List<Trade> collectTrade(Client filteredClient)
    {
        TradeCollector collector = new TradeCollector(filteredClient, converter);
        List<Trade> trades = new ArrayList<>();
        List<TradeCollectorException> errors = new ArrayList<>();
        getClient().getSecurities().forEach(s -> {
            try
            {
                trades.addAll(collector.collect(s));
            }
            catch (TradeCollectorException e)
            {
                errors.add(e);
            }
        });
        return trades;
    }

}
