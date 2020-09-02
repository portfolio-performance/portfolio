package issues;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceSnapshot;
import name.abuchen.portfolio.util.Interval;

public class Issue371PurchaseValueWithTransfers
{
    @Test
    public void testPurchaseValueOfSecurityPositionWithTransfers() throws IOException
    {
        Client client = ClientFactory.load(Issue371PurchaseValueWithTransfers.class
                        .getResourceAsStream("Issue371PurchaseValueWithTransfers.xml")); //$NON-NLS-1$

        Security adidas = client.getSecurities().get(0);
        assertThat(adidas.getName(), is("Adidas AG")); //$NON-NLS-1$

        Interval period = Interval.of(LocalDate.parse("2010-11-20"), //$NON-NLS-1$
                        LocalDate.parse("2015-11-20")); //$NON-NLS-1$

        // make sure that the transfer entry exists
        assertThat(client.getPortfolios().size(), is(2));
        assertThat(client.getPortfolios().stream().flatMap(p -> p.getTransactions().stream())
                        .filter(t -> t.getSecurity() == adidas)
                        .filter(t -> t.getCrossEntry() instanceof PortfolioTransferEntry)
                        .anyMatch(t -> t.getType() == PortfolioTransaction.Type.TRANSFER_IN),
                        is(true));

        CurrencyConverter converter = new TestCurrencyConverter();
        ClientSnapshot snapshot = ClientSnapshot.create(client, converter, period.getEnd());
        SecurityPosition securityPosition = snapshot.getPositionsByVehicle().get(adidas).getPosition();

        SecurityPerformanceSnapshot securitySnapshot = SecurityPerformanceSnapshot.create(client, converter, period);
        SecurityPerformanceRecord securityRecord = securitySnapshot.getRecords().get(0);
        assertThat(securityRecord.getSecurity(), is(adidas));

        assertThat(securityPosition.getFIFOPurchaseValue(), is(securityRecord.getFifoCost()));
    }
}
