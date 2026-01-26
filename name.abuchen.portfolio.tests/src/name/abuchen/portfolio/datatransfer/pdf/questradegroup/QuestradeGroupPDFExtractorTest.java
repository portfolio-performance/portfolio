package name.abuchen.portfolio.datatransfer.pdf.questradegroup;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
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
import name.abuchen.portfolio.datatransfer.pdf.QuestradeGroupPDFExtractor;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class QuestradeGroupPDFExtractorTest
{
    @Test
    public void testSecurityBuy01()
    {
        var extractor = new QuestradeGroupPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CAD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("VEQT"), //
                        hasName("VANGUARD ALL-EQUITY ETF"), //
                        hasCurrencyCode("CAD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-10T00:00"), hasShares(50.00), //
                        hasSource("Buy01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 2046.50), hasGrossValue("CAD", 2046.50), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));
    }

    @Test
    public void testSecurityBuy02()
    {
        var extractor = new QuestradeGroupPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CAD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("VEQT"), //
                        hasName("VANGUARD ALL-EQUITY ETF"), //
                        hasCurrencyCode("CAD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-01-16T00:00"), hasShares(29.00), //
                        hasSource("Buy02.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 974.50), hasGrossValue("CAD", 974.40), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.10))));
    }

    @Test
    public void testSecurityBuy03()
    {
        var extractor = new QuestradeGroupPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CAD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("XEQT"), //
                        hasName("ISHARES CORE EQUITY ETF"), //
                        hasCurrencyCode("CAD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-09-22T00:00"), hasShares(76.00), //
                        hasSource("Buy03.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 1973.96), hasGrossValue("CAD", 1973.96), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));
    }

    @Test
    public void testSecurityBuy04()
    {
        var extractor = new QuestradeGroupPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CAD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("XEQT"), //
                        hasName("ISHARES CORE EQUITY ETF"), //
                        hasCurrencyCode("CAD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-09-22T00:00"), hasShares(76.00), //
                        hasSource("Buy04.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 1973.23), hasGrossValue("CAD", 1972.96), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.27))));
    }

    @Test
    public void testSecurityBuy05()
    {
        var extractor = new QuestradeGroupPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CAD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("XEQT"), //
                        hasName(null), //
                        hasCurrencyCode("CAD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-01-17T00:00"), hasShares(19.00), //
                        hasSource("Buy05.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 481.08), hasGrossValue("CAD", 481.08), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));
    }

    @Test
    public void testSecurityBuy06()
    {
        var extractor = new QuestradeGroupPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CAD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("XEQT"), //
                        hasName("ISHARES CORE EQUITY ETF"), //
                        hasCurrencyCode("CAD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-02-24T00:00"), hasShares(30.00), //
                        hasSource("Buy06.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 756.40), hasGrossValue("CAD", 756.40), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));
    }

    @Test
    public void testDividend01()
    {
        var extractor = new QuestradeGroupPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividend01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CAD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("VEQT"), //
                        hasName(null), //
                        hasCurrencyCode("CAD"))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-01-07T00:00"), hasShares(29.00), //
                        hasSource("Dividend01.txt"), //
                        hasNote("REC 12/30/24"), //
                        hasAmount("CAD", 20.69), hasGrossValue("CAD", 20.69), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));
    }

    @Test
    public void testDividend02()
    {
        var extractor = new QuestradeGroupPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividend02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CAD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("XEQT"), //
                        hasName(null), //
                        hasCurrencyCode("CAD"))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-09-29T00:00"), hasShares(95.00), //
                        hasSource("Dividend02.txt"), //
                        hasNote("REC 09/26/23"), //
                        hasAmount("CAD", 23.55), hasGrossValue("CAD", 23.55), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));
    }

    @Test
    public void testDividend03()
    {
        var extractor = new QuestradeGroupPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividend03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CAD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("XEQT"), //
                        hasName(null), //
                        hasCurrencyCode("CAD"))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-03-31T00:00"), hasShares(19.00), 
                        hasSource("Dividend03.txt"), //
                        hasNote("REC 03/23/23"), //
                        hasAmount("CAD", 1.67), hasGrossValue("CAD", 1.67), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));
    }

    @Test
    public void testAccountTransaction01()
    {
        var extractor = new QuestradeGroupPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatement01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CAD");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-04-09T00:00"), hasAmount("CAD", 10000.00),
                        hasSource("AccountStatement01.txt"), hasNote(null))));
    }
}
