package issues;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceSnapshot;
import name.abuchen.portfolio.util.Dates;

import org.junit.Test;

public class Issue371PurchaseValueWithTransfers
{
    @Test
    public void testPurchaseValueOfSecurityPositionWithTransfers() throws IOException
    {
        Client client = ClientFactory.load(Issue371PurchaseValueWithTransfers.class
                        .getResourceAsStream("Issue371PurchaseValueWithTransfers.xml")); //$NON-NLS-1$

        Security adidas = client.getSecurities().get(0);
        assertThat(adidas.getName(), is("Adidas AG")); //$NON-NLS-1$

        ReportingPeriod period = new ReportingPeriod.FromXtoY(Dates.date("2010-11-20"), Dates.date("2015-11-20")); //$NON-NLS-1$ //$NON-NLS-2$

        // make sure that the transfer entry exists
        assertThat(client.getPortfolios().size(), is(2));
        assertThat(client.getPortfolios().stream().flatMap(p -> p.getTransactions().stream())
                        .filter(t -> t.getSecurity() == adidas)
                        .filter(t -> t.getCrossEntry() instanceof PortfolioTransferEntry)
                        .filter(t -> t.getType() == PortfolioTransaction.Type.TRANSFER_IN).findAny().isPresent(),
                        is(true));

        ClientSnapshot snapshot = ClientSnapshot.create(client, period.getEndDate());
        SecurityPosition securityPosition = snapshot.getPositionsByVehicle().get(adidas).getPosition();

        SecurityPerformanceSnapshot securitySnapshot = SecurityPerformanceSnapshot.create(client, period);
        SecurityPerformanceRecord securityRecord = securitySnapshot.getRecords().get(0);
        assertThat(securityRecord.getSecurity(), is(adidas));

        assertThat(securityPosition.getFIFOPurchaseValue(), is(securityRecord.getFifoCost()));
    }
}
