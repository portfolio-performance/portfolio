package name.abuchen.portfolio.datatransfer.pdf.boursedirect;

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
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSecurities;
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
                        hasAmount("EUR", 363.59), hasGrossValue("EUR", 362.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.99))));

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
}
