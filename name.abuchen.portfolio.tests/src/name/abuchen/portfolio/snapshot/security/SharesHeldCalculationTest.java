package name.abuchen.portfolio.snapshot.security;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.PortfolioBuilder;
import name.abuchen.portfolio.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.Interval;

public class SharesHeldCalculationTest
{
    @Test
    public void testSharesHeldIfMultiplePortfolioContainSameSecurity()
    {
        Client client = new Client();
        Security security = new Security();
        client.addSecurity(security);

        new PortfolioBuilder() //
                        .buy(security, "2018-01-01", Values.Share.factorize(1), 100) //$NON-NLS-1$
                        .addTo(client);

        new PortfolioBuilder() //
                        .buy(security, "2018-01-01", Values.Share.factorize(1), 100) //$NON-NLS-1$
                        .addTo(client);

        SecurityPerformanceSnapshot snapshot = SecurityPerformanceSnapshot.create(client, new TestCurrencyConverter(),
                        Interval.of(LocalDate.parse("2018-02-01"), LocalDate.parse("2018-02-02"))); //$NON-NLS-1$ //$NON-NLS-2$

        assertThat(snapshot.getRecords().size(), is(1));

        assertThat(snapshot.getRecords().get(0).getSharesHeld(), is(Values.Share.factorize(2)));
    }

}
