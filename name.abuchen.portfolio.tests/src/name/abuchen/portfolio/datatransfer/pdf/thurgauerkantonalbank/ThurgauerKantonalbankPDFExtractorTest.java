package name.abuchen.portfolio.datatransfer.pdf.thurgauerkantonalbank;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFees;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasForexGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasIsin;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasName;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasShares;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTaxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTicker;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasWkn;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransactions;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransfers;
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
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.ThurgauerKantonalbankPDFExtractor;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class ThurgauerKantonalbankPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new ThurgauerKantonalbankPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("AU3CB0319416"), hasWkn("142923838"), hasTicker(null), //
                        hasName("4.55% Obl Nestle Capital Corp 2025-13.03.30"), //
                        hasCurrencyCode("AUD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-07-20T09:33:00"), hasShares(400.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Referenznummer 1234567890 | Marchzinsen (131 Tage): 648.00 AUD"), //
                        hasAmount("CHF", 23185.11), hasGrossValue("CHF", 23053.23), //
                        hasForexGrossValue("AUD", 40085.60), //
                        hasTaxes("CHF", 34.58), hasFees("CHF", (92.21 + 5.09)))));
    }

    @Test
    public void testWertpapierKauf01WithSecurityInCHF()
    {
        var security = new Security("4.55% Obl Nestle Capital Corp 2025-13.03.30", "CHF");
        security.setIsin("AU3CB0319416");
        security.setWkn("142923838");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ThurgauerKantonalbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-07-20T09:33:00"), hasShares(400.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Referenznummer 1234567890 | Marchzinsen (131 Tage): 648.00 AUD"), //
                        hasAmount("CHF", 23185.11), hasGrossValue("CHF", 23053.23), //
                        hasTaxes("CHF", 34.58), hasFees("CHF", (92.21 + 5.09)))));
    }
}
