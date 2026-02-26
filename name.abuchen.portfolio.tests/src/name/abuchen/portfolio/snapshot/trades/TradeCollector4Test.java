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
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class TradeCollector4Test
{
    private static Client client;

    @BeforeClass
    public static void prepare() throws IOException
    {
        client = ClientFactory.load(TradeCollector4Test.class.getResourceAsStream("trade_test_case4.xml"));
    }

    @Test
    public void testFIFOandMovingAverageCostWithBitcoinTrade() throws TradeCollectorException
    {
        TradeCollector collector = new TradeCollector(client, new TestCurrencyConverter());

        Security bitcoin = client.getSecurities().stream().filter(s -> "Bitcoin".equals(s.getName())).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        List<Trade> trades = collector.collect(bitcoin);

        assertThat(trades.size(), is(4));

        Trade firstTrade = trades.get(0);

        assertThat(firstTrade.getStart(), is(LocalDateTime.parse("2020-01-25T00:00")));
        assertThat(firstTrade.getEnd().orElseThrow(IllegalArgumentException::new),
                        is(LocalDateTime.parse("2020-11-21T00:00")));

        // Purchase A : 0.001301 for 10 EUR
        // Purchase B : 0.012824 for 100 EUR
        // Owned : 0.014125
        // Sale S : 0.00636567

        // FIFO cost

        // A: 10 EUR
        // B: 100 EUR * 0,3949368372
        // because (sale − purchase A) / (owned − purchase A)
        // (0.00636567 − 0.001301 ) / (0.014125 − 0.001301 )

        assertThat(firstTrade.getEntryValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Money.factorize(10 + 39.49368372))));

        // moving average

        // 110 * (sale / owned)
        // 110 * (0.00636567 / 0.014125) = 49.573359292

        assertThat(firstTrade.getEntryValueMovingAverage(),
                        is(Money.of(CurrencyUnit.EUR, Values.Money.factorize(49.573359292))));

        // Note: FIFO and moving average cost differ because each transaction as
        // a different share price even though both purchase transactions were
        // on the same day
    }
}
