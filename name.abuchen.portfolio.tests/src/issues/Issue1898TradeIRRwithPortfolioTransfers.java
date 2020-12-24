package issues;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.hamcrest.number.IsCloseTo;
import org.junit.Test;

import name.abuchen.portfolio.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.snapshot.trades.Trade;
import name.abuchen.portfolio.snapshot.trades.TradeCollector;
import name.abuchen.portfolio.snapshot.trades.TradeCollectorException;

public class Issue1898TradeIRRwithPortfolioTransfers
{
    @Test
    public void testPurchaseValueOfSecurityPositionWithTransfers() throws IOException, TradeCollectorException
    {
        Client client = ClientFactory.load(Issue1898TradeIRRwithPortfolioTransfers.class
                        .getResourceAsStream("Issue1898TradeIRRwithPortfolioTransfers.xml")); //$NON-NLS-1$

        CurrencyConverter converter = new TestCurrencyConverter();

        TradeCollector collector = new TradeCollector(client, converter);

        Security security = client.getSecurities().get(0);
        assertThat(security.getName(), is("MASCH. BERTH. HERMLE AG Inhaber")); //$NON-NLS-1$

        List<Trade> trades = collector.collect(security);

        assertThat(trades.size(), is(2));

        Trade closedTrade = trades.stream().filter(t -> t.getEnd().isPresent()).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        assertThat(closedTrade.getStart(), is(LocalDateTime.parse("2013-07-10T00:00"))); //$NON-NLS-1$
        assertThat(closedTrade.getEnd().orElseThrow(IllegalArgumentException::new),
                        is(LocalDateTime.parse("2020-09-21T00:00"))); //$NON-NLS-1$
        assertThat(closedTrade.getIRR(), IsCloseTo.closeTo(0.0963, 0.0001));
        assertThat(closedTrade.getTransactions().size(), is(2));

        Trade openTrade = trades.stream().filter(t -> !t.getEnd().isPresent()).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        assertThat(openTrade.getStart(), is(LocalDateTime.parse("2020-04-29T00:00"))); //$NON-NLS-1$
        assertThat(openTrade.getTransactions().size(), is(1));
    }
}
