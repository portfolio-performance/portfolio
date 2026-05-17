package name.abuchen.portfolio.datatransfer.pdf.bwbank;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFees;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasIsin;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasName;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasShares;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTaxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.inboundDelivery;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.outboundDelivery;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransfers;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransactions;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countItemsWithFailureMessage;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSecurities;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSkippedItems;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.BwBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class BwBankPDFExtractorTest
{
    @Test
    public void testKauf01()
    {
        var extractor = new BwBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        assertThat(results, hasItem(security( //
                        hasIsin("DE0007100000"), //
                        hasName("Daimler AG Namens-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2020-06-24T17:26"), hasShares(10.00), //
                        hasSource("Kauf01.txt"), hasNote("Ordernummer 726V907"), //
                        hasAmount("EUR", 353.35), hasGrossValue("EUR", 348.05), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.30))));
    }

    @Test
    public void testDepotUmbuchung01()
    {
        var extractor = new BwBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DepotUmbuchung01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        assertThat(results, hasItem(security( //
                        hasIsin("GB00B03MLX29"), //
                        hasName("SHELL PLC A EO-07"), //
                        hasCurrencyCode("EUR"))));
        assertThat(results, hasItem(outboundDelivery( //
                        hasDate("2022-02-03T00:00"), hasShares(26.00), //
                        hasSource("DepotUmbuchung01.txt"), hasNote("Depot-Umbuchung"), //
                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(security( //
                        hasIsin("GB00BP6MXD84"), //
                        hasName("SHELL PLC EO-07"), //
                        hasCurrencyCode("EUR"))));
        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2022-02-03T00:00"), hasShares(26.00), //
                        hasSource("DepotUmbuchung01.txt"), hasNote("Depot-Umbuchung"), //
                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }
}
