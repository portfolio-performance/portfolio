package name.abuchen.portfolio.datatransfer.pdf.boursedirect;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.fee;
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
import name.abuchen.portfolio.datatransfer.pdf.BourseDirectPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class BourseDirectPDFExtractorTest
{
    @Test
    public void testReleveDeCompte01()
    {
        var extractor = new BourseDirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ReleveDeCompte01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(3L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0011550185"), hasWkn(null), hasTicker(null), //
                        hasName("BNPP S&P500EUR ETF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0011550193"), hasWkn(null), hasTicker(null), //
                        hasName("BNPETF STOXX 600"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0013412020"), hasWkn(null), hasTicker(null), //
                        hasName("AM.PEA MS.EM M.ACC"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-12-10T09:04:28"), hasShares(173.00), //
                        hasSource("ReleveDeCompte01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 4978.30), hasGrossValue("EUR", 4973.82), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.48))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-12-10T09:04:18"), hasShares(250.00), //
                        hasSource("ReleveDeCompte01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 4090.80), hasGrossValue("EUR", 4087.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.80))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-12-10T09:04:11"), hasShares(37.00), //
                        hasSource("ReleveDeCompte01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 886.98), hasGrossValue("EUR", 885.08), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.90))));
    }

    @Test
    public void testReleveDeCompte02()
    {
        var extractor = new BourseDirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ReleveDeCompte02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(5L));
        assertThat(countBuySell(results), is(5L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(10));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0000120271"), hasWkn(null), hasTicker(null), //
                        hasName("TOTALENERGIES SE"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B4BNMY34"), hasWkn(null), hasTicker(null), //
                        hasName("ACCENTURE CL.A"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1681047236"), hasWkn(null), hasTicker(null), //
                        hasName("A.E.ST.50 U.EUR C"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("NL0000395903"), hasWkn(null), hasTicker(null), //
                        hasName("WOLTERS KLUWER"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("NL00150001Q9"), hasWkn(null), hasTicker(null), //
                        hasName("STELLANTIS"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-08-05T16:35:33"), hasShares(7.00), //
                        hasSource("ReleveDeCompte02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 363.59), hasGrossValue("EUR", 361.15), //
                        hasTaxes("EUR", 1.45), hasFees("EUR", 0.99))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-08-05T16:29:06"), hasShares(1.00), //
                        hasSource("ReleveDeCompte02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 216.93), hasGrossValue("EUR", 215.85), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.08))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-08-05T16:31:21"), hasShares(2.00), //
                        hasSource("ReleveDeCompte02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 275.59), hasGrossValue("EUR", 274.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.99))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-08-05T16:32:51"), hasShares(2.00), //
                        hasSource("ReleveDeCompte02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 265.79), hasGrossValue("EUR", 264.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.99))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-08-05T16:37:38"), hasShares(23.00), //
                        hasSource("ReleveDeCompte02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 174.18), hasGrossValue("EUR", 173.31), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.87))));
    }

    @Test
    public void testReleveDeCompte03()
    {
        var extractor = new BourseDirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ReleveDeCompte03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("NL0010273215"), hasWkn(null), hasTicker(null), //
                        hasName("ASML HOLDING"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-08-06T00:00"), hasShares(3.00), //
                        hasSource("ReleveDeCompte03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 4.08), hasGrossValue("EUR", 4.80), //
                        hasTaxes("EUR", 0.72), hasFees("EUR", 0.00))));
    }

    @Test
    public void testReleveDeCompte04()
    {
        var extractor = new BourseDirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ReleveDeCompte04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-02-24"), hasAmount("EUR", 2400.00), //
                        hasSource("ReleveDeCompte04.txt"), hasNote(null))));
    }

    @Test
    public void testReleveDeCompte05()
    {
        var extractor = new BourseDirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ReleveDeCompte05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US0995021062"), hasWkn(null), hasTicker(null), //
                        hasName("BOOZ ALLEN CL.A"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US5024311095"), hasWkn(null), hasTicker(null), //
                        hasName("L3HARRIS TECHN."), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-03-16T19:50:11"), hasShares(45.00), //
                        hasSource("ReleveDeCompte05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2977.53), hasGrossValue("EUR", 2986.04), //
                        hasForexGrossValue("USD", 3564.00), //
                        hasTaxes("EUR", 0.01), hasFees("EUR", 8.50))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-03-16T19:45:03"), hasShares(20.00), //
                        hasSource("ReleveDeCompte05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3178.78), hasGrossValue("EUR", 3187.29), //
                        hasForexGrossValue("USD", 3804.20), //
                        hasTaxes("EUR", 0.01), hasFees("EUR", 8.50))));
    }

    @Test
    public void testDividende02WithSecurityInEUR()
    {
        var security1 = new Security("BOOZ ALLEN CL.A", "EUR");
        security1.setIsin("US0995021062");

        var security2 = new Security("L3HARRIS TECHN.", "EUR");
        security2.setIsin("US5024311095");

        var client = new Client();
        client.addSecurity(security1);
        client.addSecurity(security2);

        var extractor = new BourseDirectPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ReleveDeCompte05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-03-16T19:50:11"), hasShares(45.00), //
                        hasSource("ReleveDeCompte05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2977.53), hasGrossValue("EUR", 2986.04), //
                        hasTaxes("EUR", 0.01), hasFees("EUR", 8.50))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-03-16T19:45:03"), hasShares(20.00), //
                        hasSource("ReleveDeCompte05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3178.78), hasGrossValue("EUR", 3187.29), //
                        hasTaxes("EUR", 0.01), hasFees("EUR", 8.50))));
    }

    @Test
    public void testReleveDeCompte06()
    {
        var extractor = new BourseDirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ReleveDeCompte06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US5024311095"), hasWkn(null), hasTicker(null), //
                        hasName("L3HARRIS TECHN."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-03-30T00:00"), hasShares(20.00), //
                        hasSource("ReleveDeCompte06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 9.55), hasGrossValue("EUR", 17.34), //
                        hasTaxes("EUR", 7.79), hasFees("EUR", 0.00))));
    }

    @Test
    public void testReleveDeCompte07()
    {
        var extractor = new BourseDirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ReleveDeCompte07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2021-07-06"), hasAmount("EUR", 0.81), //
                        hasSource("ReleveDeCompte07.txt"), hasNote("DDG ETG 2T21"))));
    }
}
