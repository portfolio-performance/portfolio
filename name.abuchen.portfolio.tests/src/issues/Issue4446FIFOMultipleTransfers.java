package issues;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
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
import name.abuchen.portfolio.snapshot.trades.Trade;
import name.abuchen.portfolio.snapshot.trades.TradeCollector;
import name.abuchen.portfolio.snapshot.trades.TradeCollectorException;

public class Issue4446FIFOMultipleTransfers
{
    @Test
    public void testDefaultSnapshot() throws IOException, TradeCollectorException
    {
        Client client = ClientFactory.load(Issue4446FIFOMultipleTransfers.class
                        .getResourceAsStream("Issue4446FIFOMultipleTransfers.xml")); //$NON-NLS-1$

        Security security = client.getSecurities().get(0);
        assertThat(security.getName(), is("ADIDAS AG NA O.N.")); //$NON-NLS-1$

        CurrencyConverter converter = new TestCurrencyConverter();
        TradeCollector collector = new TradeCollector(client, converter);

        List<Trade> trades = collector.collect(security);

        Trade t = trades.getFirst();

        assertThat(t.getLastTransaction().getTransaction().getNote(), is("buy-in should be 1000")); //$NON-NLS-1$
        assertThat(t.getEntryValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000))));
    }
}
