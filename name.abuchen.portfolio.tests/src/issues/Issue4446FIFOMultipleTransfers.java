package issues;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.trades.Trade;
import name.abuchen.portfolio.snapshot.trades.TradeCollector;
import name.abuchen.portfolio.snapshot.trades.TradeCollectorException;
import name.abuchen.portfolio.util.Interval;

public class Issue4446FIFOMultipleTransfers
{
    @Test
    public void testDefaultSnapshot() throws IOException, TradeCollectorException
    {
        Client client = ClientFactory.load(
                        Issue4446FIFOMultipleTransfers.class.getResourceAsStream("Issue4446FIFOMultipleTransfers.xml")); //$NON-NLS-1$

        Security security = client.getSecurities().get(0);
        assertThat(security.getName(), is("ADIDAS AG NA O.N.")); //$NON-NLS-1$

        CurrencyConverter converter = new TestCurrencyConverter();
        TradeCollector collector = new TradeCollector(client, converter);

        List<Trade> trades = collector.collect(security);

        Trade t = trades.getFirst();

        assertThat(t.getLastTransaction().getTransaction().getNote(), is("buy-in should be 1000")); //$NON-NLS-1$
        assertThat(t.getEntryValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000))));
    }

    @Test
    public void testFIFOWithTransferAndSameDayPurchase() throws IOException, TradeCollectorException
    {
        // scenario: first a purchase (with a time in the transaction) and then
        // an outbound transfer (without time stamp) on the same day

        Client client = ClientFactory.load(Issue4446FIFOMultipleTransfers.class
                        .getResourceAsStream("Issue4446FIFOTransferWithSameDayPurchase.xml")); //$NON-NLS-1$

        Security security = client.getSecurities().get(0);
        assertThat(security.getName(), is("WisdomTree Physical Swiss Gold acc (ETC)")); //$NON-NLS-1$

        CurrencyConverter converter = new TestCurrencyConverter();
        TradeCollector collector = new TradeCollector(client, converter);

        List<Trade> trades = collector.collect(security);

        // check open trade
        var trade = trades.stream().filter(t -> t.getEnd().isEmpty()).findFirst().orElseThrow();

        assertThat(trade.getEnd().isEmpty(), is(true));
        assertThat(trade.getEntryValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3916.56))));

        // check FIFO via CostCalculation class

        var snapshot = LazySecurityPerformanceSnapshot.create(client, converter,
                        Interval.of(LocalDate.of(2024, 1, 1), LocalDate.of(2025, 4, 18)));

        var snapshotRecord = snapshot.getRecord(security).orElseThrow();

        assertThat(snapshotRecord.getFifoCost().get(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3916.56))));
    }

}
