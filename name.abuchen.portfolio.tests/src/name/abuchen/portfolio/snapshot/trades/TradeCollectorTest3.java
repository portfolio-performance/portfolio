package name.abuchen.portfolio.snapshot.trades;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.PortfolioBuilder;
import name.abuchen.portfolio.SecurityBuilder;
import name.abuchen.portfolio.TestCurrencyConverter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class TradeCollectorTest3
{
    @Test
    public void testTradesThatAreOpenedAndClosedOnTheSameDay() throws TradeCollectorException
    {
        Client client = new Client();

        Security security = new SecurityBuilder().addTo(client);

        // first insert the outbound delivery, then the inbound delivery in
        // order to test that the sorting takes the types into account

        new PortfolioBuilder(new Account("one"))
                        .outbound_delivery(security, "2022-01-01", Values.Share.factorize(5),
                                        Values.Amount.factorize(500), 0, 0)
                        .inbound_delivery(security, "2022-01-01", Values.Share.factorize(10),
                                        Values.Amount.factorize(1000))
                        .addTo(client);

        TradeCollector collector = new TradeCollector(client, new TestCurrencyConverter());

        List<Trade> trades = collector.collect(security);

        assertThat(trades.size(), is(2));

        Trade firstTrade = trades.get(0);

        assertThat(firstTrade.getStart(), is(LocalDateTime.parse("2022-01-01T00:00")));
        assertThat(firstTrade.getEnd().orElseThrow(IllegalArgumentException::new),
                        is(LocalDateTime.parse("2022-01-01T00:00")));
        assertThat(firstTrade.getHoldingPeriod(), is(0L));
        assertThat(firstTrade.getProfitLoss(), is(Money.of(CurrencyUnit.EUR, 0L)));
    }

}
