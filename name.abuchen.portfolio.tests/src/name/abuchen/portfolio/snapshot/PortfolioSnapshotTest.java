package name.abuchen.portfolio.snapshot;

import static org.junit.Assert.assertTrue;

import java.time.LocalDate;

import name.abuchen.portfolio.PortfolioBuilder;
import name.abuchen.portfolio.SecurityBuilder;
import name.abuchen.portfolio.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;

import org.junit.Test;

@SuppressWarnings("nls")
public class PortfolioSnapshotTest
{

    @Test
    public void testBuyAndSellLeavesNoEntryInSnapshot()
    {
        Client client = new Client();

        Security a = new SecurityBuilder() //
                        .addPrice("2010-01-01", 1000) //
                        .addTo(client);

        Portfolio portfolio = new PortfolioBuilder() //
                        .buy(a, "2010-01-01", 1000000, 10000) //
                        .sell(a, "2010-01-02", 700000, 12000) //
                        .sell(a, "2010-01-03", 300000, 12000) //
                        .addTo(client);

        LocalDate date = LocalDate.parse("2010-01-31");
        PortfolioSnapshot snapshot = PortfolioSnapshot.create(portfolio, new TestCurrencyConverter(), date);

        assertTrue(snapshot.getPositions().isEmpty());
    }
}
