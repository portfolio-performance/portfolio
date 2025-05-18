package name.abuchen.portfolio.snapshot.trades;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class TradeCollector5Test
{
    private static Client client;

    @BeforeClass
    public static void prepare() throws IOException
    {
        client = ClientFactory.load(TradeCollector5Test.class.getResourceAsStream("trade_test_case5.xml"));
    }

    @Test
    public void testFIFOandMovingAverageCostWithCurrencyOtherThanEUR() throws TradeCollectorException
    {
        TradeCollector collector = new TradeCollector(client, new TestCurrencyConverter("CAD"));

        Security telus = client.getSecurities().stream().filter(s -> "TELUS CORPORATION".equals(s.getName())).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        List<Trade> trades = collector.collect(telus);

        assertThat(trades.size(), is(1));

        Trade firstTrade = trades.get(0);

        assertThat(firstTrade.getStart(), is(LocalDateTime.parse("2023-08-23T00:00")));
        assertThat(firstTrade.getEnd().orElseThrow(IllegalArgumentException::new),
                        is(LocalDateTime.parse("2023-10-16T00:00")));

        // In this case, FIFO and moving average result in the same entry value.
        // This test case tests other currencies than EUR.
        assertThat(firstTrade.getEntryValue(), is(Money.of("CAD", Values.Money.factorize(9994.44))));
        assertThat(firstTrade.getEntryValueMovingAverage(), is(firstTrade.getEntryValue()));
    }
}
