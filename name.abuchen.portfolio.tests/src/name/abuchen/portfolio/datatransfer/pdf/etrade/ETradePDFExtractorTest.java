package name.abuchen.portfolio.datatransfer.pdf.etrade;

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
import name.abuchen.portfolio.datatransfer.pdf.ETradePDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class ETradePDFExtractorTest
{
    @Test
    public void testSecurityBuy01()
    {
        var extractor = new ETradePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("NXPI"), //
                        hasName("NXP SEMICONDUCTORS N.V."), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-02-28T00:00"), hasShares(5.2350), //
                        hasSource("Buy01.txt"), //
                        hasNote("Taxable Gain $169.30"), //
                        hasAmount("USD", 959.31), hasGrossValue("USD", 959.31), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));
    }

    @Test
    public void testSecuritySell01()
    {
        var extractor = new ETradePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sell01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US4581401001"), hasWkn(null), hasTicker(null), //
                        hasName("INTEL CORP"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-09-18T00:00"), hasShares(796.00), //
                        hasSource("Sell01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 24902.94), hasGrossValue("USD", 24902.94), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));
    }

    @Test
    public void testSecurityRelease01()
    {
        var extractor = new ETradePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Release01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("XCWE"), //
                        hasName("noaQU ppixWWH, INC. - NEW"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-08-10T00:00"), hasShares(34.00), //
                        hasSource("Release01.txt"), //
                        hasNote("Award Number 4414241"), //
                        hasAmount("USD", 2387.31), hasGrossValue("USD", 2387.31), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-08-10T00:00"), hasShares(17.519), //
                        hasSource("Release01.txt"), //
                        hasNote("Award Number 4414241"), //
                        hasAmount("USD", 0.00), hasGrossValue("USD", 1230.10), //
                        hasTaxes("USD", 1230.10), hasFees("USD", 0.00))));
    }
}
