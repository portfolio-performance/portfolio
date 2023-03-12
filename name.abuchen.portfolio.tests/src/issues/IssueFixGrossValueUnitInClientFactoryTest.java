package issues;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;

import org.junit.Test;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;

public class IssueFixGrossValueUnitInClientFactoryTest
{
    @Test
    public void testMigrationOfClassificationKeys() throws IOException
    {
        Client client = ClientFactory.load(Issue1498FifoCrossPortfolioTest.class
                        .getResourceAsStream("IssueFixGrossValueUnitInClientFactory.xml")); //$NON-NLS-1$

        assertThat(client.getFileVersionAfterRead(), is(55));

        // check account transaction (dividend)
        AccountTransaction txa = client.getAccounts().get(0).getTransactions().get(0);
        assertThat(txa.getUnit(Unit.Type.GROSS_VALUE).orElseThrow().getAmount(), is(txa.getGrossValue()));

        // check portfolio transaction (inbound delivery)
        PortfolioTransaction txp = client.getPortfolios().get(0).getTransactions().get(0);
        assertThat(txp.getUnit(Unit.Type.GROSS_VALUE).orElseThrow().getAmount(), is(txp.getGrossValue()));

        // check CHF portfolio transaction (inbound delivery) - handle negative
        // exchange rates
        txp = client.getPortfolios().get(0).getTransactions().get(1);
        final Unit unit = txp.getUnit(Unit.Type.GROSS_VALUE).orElseThrow();
        assertThat(unit.getAmount(), is(txp.getGrossValue()));
        assertThat(unit.getForex().getCurrencyCode(), is("CHF")); //$NON-NLS-1$
        assertThat(unit.getExchangeRate().signum(), is(-1));

    }
}
