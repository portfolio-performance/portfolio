package name.abuchen.portfolio.datatransfer.pdf.sydbankas;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
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
import name.abuchen.portfolio.datatransfer.pdf.SydbankASPDFExtractor;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class SydbankASPDFExtractorTest
{
    @Test
    public void testSecurityBuy01()
    {
        var extractor = new SydbankASPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "DKK");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DK0062498333"), hasWkn(null), hasTicker(null), //
                        hasName("Novo Nordisk A/S B"), //
                        hasCurrencyCode("DKK"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-08-21T11:24:20"), hasShares(10.00), //
                        hasSource("Buy01.txt"), //
                        hasNote(null), //
                        hasAmount("DKK", 3515.50), hasGrossValue("DKK", 3486.50), //
                        hasTaxes("DKK", 0.00), hasFees("DKK", 29.00))));
    }

    @Test
    public void testSecuritySell01()
    {
        var extractor = new SydbankASPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "DKK");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DK0010068006"), hasWkn(null), hasTicker(null), //
                        hasName("Sparinvest Danske Aktier KL A"), //
                        hasCurrencyCode("DKK"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-02-02T10:53:55"), hasShares(120.00), //
                        hasSource("Sell01.txt"), //
                        hasNote(null), //
                        hasAmount("DKK", 26036.89), hasGrossValue("DKK", 26076.00), //
                        hasTaxes("DKK", 0.00), hasFees("DKK", 39.11))));
    }

    @Test
    public void testSecuritySell02()
    {
        var extractor = new SydbankASPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sell02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "DKK");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DK0015168686"), hasWkn(null), hasTicker(null), //
                        hasName("Nordea Invest Mellemlange Obligationer KL1"), //
                        hasCurrencyCode("DKK"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-08-21T09:54:09"), hasShares(120.00), //
                        hasSource("Sell02.txt"), //
                        hasNote(null), //
                        hasAmount("DKK", 18745.00), hasGrossValue("DKK", 18774.00), //
                        hasTaxes("DKK", 0.00), hasFees("DKK", 29.00))));
    }

    @Test
    public void testSecuritySell03()
    {
        var extractor = new SydbankASPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sell03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "DKK");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DK0060105203"), hasWkn(null), hasTicker(null), //
                        hasName("Sparinvest Korte Obligationer KL A"), //
                        hasCurrencyCode("DKK"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-08-21T09:57:10"), hasShares(209.00), //
                        hasSource("Sell03.txt"), //
                        hasNote(null), //
                        hasAmount("DKK", 24183.65), hasGrossValue("DKK", 24212.65), //
                        hasTaxes("DKK", 0.00), hasFees("DKK", 29.00))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new SydbankASPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "DKK");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DK0060105203"), hasWkn(null), hasTicker(null), //
                        hasName("Sparinvest Korte Obligationer KL A"), //
                        hasCurrencyCode("DKK"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-02-07T00:00"), hasShares(209.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote(null), //
                        hasAmount("DKK", 104.50), hasGrossValue("DKK", 104.50), //
                        hasTaxes("DKK", 0.00), hasFees("DKK", 0.00))));
    }

    @Test
    public void testDividende02()
    {
        var extractor = new SydbankASPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "DKK");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DK0062498333"), hasWkn(null), hasTicker(null), //
                        hasName("Novo Nordisk A/S B"), //
                        hasCurrencyCode("DKK"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-08-19T00:00"), hasShares(2288.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote(null), //
                        hasAmount("DKK", 6263.40), hasGrossValue("DKK", 8580.00), //
                        hasTaxes("DKK", 2316.60), hasFees("DKK", 0.00))));
    }
}
