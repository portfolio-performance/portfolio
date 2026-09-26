package name.abuchen.portfolio.datatransfer.pdf.upvest;

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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTicker;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasWkn;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
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
import name.abuchen.portfolio.datatransfer.pdf.UpvestPDFExtractor;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class UpvestPDFExtractorTest
{

    @Test
    public void testKauf01()
    {
        var extractor = new UpvestPDFExtractor(new Client());

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


        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0290358497"), //
                        hasWkn(null), //
                        hasTicker(null), //
                        hasName("XTR.II EUR OV.RATE SW. 1C"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-30T15:35:01"), //
                        hasShares(2.60301), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Abr.-Nr.: 47ca082b-7818-45d4-9dda-8cb2d1174086"), //
                        hasAmount("EUR", 386.00), //
                        hasGrossValue("EUR", 386.00), //
                        hasTaxes("EUR", 0.00 + 0.00 + 0.00), //
                        hasFees("EUR", 0.00))));
    }

    @Test
    public void testVerkauf01()
    {
        var extractor = new UpvestPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0290358497"), //
                        hasWkn(null), //
                        hasTicker(null), //
                        hasName("XTR.II EUR OV.RATE SW. 1C"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2026-02-02T15:05:46"), //
                        hasShares(2.60301), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Abr.-Nr.: 33c092d9-3c6e-4a0d-ad35-b44c1e1efe81"), //
                        hasAmount("EUR", 386.04), //
                        hasGrossValue("EUR", 386.05), //
                        hasTaxes("EUR", 0.01 + 0.00 + 0.00), //
                        hasFees("EUR", 0.00))));
    }

    @Test
    public void testVerkauf02()
    {
        var extractor = new UpvestPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US92343V1044"), hasWkn(null), hasTicker(null), //
                        hasName("VERIZON COMM. INC. DL-,10"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2026-05-07T09:02:34"), hasShares(30.00), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Abr.-Nr.: 210fba8e-917f-401e-918f-b3dda7887a2e"), //
                        hasAmount("EUR", 1205.11), hasGrossValue("EUR", 1209.15), //
                        hasTaxes("EUR", 3.83 + 0.21 + 0.00), hasFees("EUR", 0.00))));
    }
}
