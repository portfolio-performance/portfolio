package issues;

import java.io.IOException;
import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshotTest;
import name.abuchen.portfolio.snapshot.filter.ClientClassificationFilter;
import name.abuchen.portfolio.snapshot.filter.ClientFilter;
import name.abuchen.portfolio.util.Interval;

public class Issue1817CapitalGainsOnFilteredClientIfSecurityIsTransferredTest
{
    @Test
    public void testPurchaseValueOfSecurityPositionWithTransfers() throws IOException
    {
        Client client = ClientFactory.load(Issue371PurchaseValueWithTransfersTest.class
                        .getResourceAsStream("Issue1817CapitalGainsOnFilteredClientIfSecurityIsTransferred.xml")); //$NON-NLS-1$

        CurrencyConverter converter = new TestCurrencyConverter();
        Interval period = Interval.of(LocalDate.parse("2018-01-01"), //$NON-NLS-1$
                        LocalDate.parse("2020-01-01")); //$NON-NLS-1$

        Taxonomy taxonomy = client.getTaxonomy("assetclasses"); //$NON-NLS-1$

        Classification classification = taxonomy.getClassificationById("EQUITY"); //$NON-NLS-1$

        ClientFilter filter = new ClientClassificationFilter(classification);

        Client filteredClient = filter.filter(client);

        ClientPerformanceSnapshot clientSnapshot = new ClientPerformanceSnapshot(filteredClient, converter, period);

        ClientPerformanceSnapshotTest.assertThatCalculationWorksOut(clientSnapshot, converter);
    }
}
