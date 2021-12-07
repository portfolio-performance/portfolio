package name.abuchen.portfolio.snapshot.trades;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import name.abuchen.portfolio.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class TradeCollectorTest
{
    private static Client client;

    @BeforeClass
    public static void prepare() throws IOException
    {
        client = ClientFactory.load(TradeCollectorTest.class.getResourceAsStream("trade_test_case.xml"));
    }

    @Test
    public void testPartialOutboundDeliveryIncludingTransfers() throws TradeCollectorException
    {
        TradeCollector collector = new TradeCollector(client, new TestCurrencyConverter());

        Security allianz = client.getSecurities().stream().filter(s -> "Allianz SE".equals(s.getName())).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        List<Trade> trades = collector.collect(allianz);

        assertThat(trades.size(), is(2));

        Trade firstTrade = trades.get(0);

        assertThat(firstTrade.getStart(), is(LocalDateTime.parse("2012-01-02T00:00")));
        assertThat(firstTrade.getEnd().orElseThrow(IllegalArgumentException::new),
                        is(LocalDateTime.parse("2012-01-13T00:00")));
        assertThat(firstTrade.getHoldingPeriod(), is(11L));
        assertThat(firstTrade.getProfitLoss(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1605 - 737.81))));

        Trade secondTrade = trades.get(1);

        assertThat(secondTrade.getStart(), is(LocalDateTime.parse("2012-01-02T00:00")));
        assertThat(secondTrade.getEnd().isPresent(), is(false));
    }

    @Test
    public void testCompletedTrade() throws TradeCollectorException
    {
        TradeCollector collector = new TradeCollector(client, new TestCurrencyConverter());

        Security commerzbank = client.getSecurities().stream().filter(s -> "Commerzbank AG".equals(s.getName()))
                        .findAny().orElseThrow(IllegalArgumentException::new);

        List<Trade> trades = collector.collect(commerzbank);

        assertThat(trades.size(), is(1));

        Trade firstTrade = trades.get(0);

        assertThat(firstTrade.getStart(), is(LocalDateTime.parse("2012-01-01T00:00")));
        assertThat(firstTrade.getEnd().orElseThrow(IllegalArgumentException::new),
                        is(LocalDateTime.parse("2012-01-10T00:00")));
        assertThat(firstTrade.getHoldingPeriod(), is(9L));
        assertThat(firstTrade.getProfitLoss(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(688.36 - 1019.80))));
    }

    @Test
    public void testPartialTransferOutOfMultiplePurchases() throws TradeCollectorException
    {
        // Linde has 2 open trades because there are 2 holdings each in a
        // separate securities account

        TradeCollector collector = new TradeCollector(client, new TestCurrencyConverter());

        Security linde = client.getSecurities().stream().filter(s -> "Linde AG".equals(s.getName())).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        List<Trade> trades = collector.collect(linde);
        Collections.sort(trades, (r, l) -> r.getStart().compareTo(l.getStart()));

        assertThat(trades.size(), is(2));

        Trade firstTrade = trades.get(0);

        assertThat(firstTrade.getStart(), is(LocalDateTime.parse("2012-01-01T00:00")));
        assertThat(firstTrade.getEnd().isPresent(), is(false));
        assertThat(firstTrade.getShares(), is(Values.Share.factorize(40)));

        Trade secondTrade = trades.get(1);

        assertThat(secondTrade.getStart(), is(LocalDateTime.parse("2012-01-28T00:00")));
        assertThat(secondTrade.getEnd().isPresent(), is(false));
        assertThat(secondTrade.getShares(), is(Values.Share.factorize(10)));
    }

    @Test
    public void testPurchaseWithMultipleSells() throws TradeCollectorException
    {
        TradeCollector collector = new TradeCollector(client, new TestCurrencyConverter());

        Security man = client.getSecurities().stream().filter(s -> "MAN SE".equals(s.getName())).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        List<Trade> trades = collector.collect(man);
        Collections.sort(trades, (r, l) -> r.getStart().compareTo(l.getStart()));

        assertThat(trades.size(), is(3));

        Trade firstTrade = trades.get(0);

        assertThat(firstTrade.getShares(), is(Values.Share.factorize(5)));

        Trade secondTrade = trades.get(1);

        assertThat(secondTrade.getShares(), is(Values.Share.factorize(4)));

        Trade thirdTrade = trades.get(2);

        assertThat(thirdTrade.getShares(), is(Values.Share.factorize(1)));
        assertThat(thirdTrade.getEnd().isPresent(), is(false));
    }

}
