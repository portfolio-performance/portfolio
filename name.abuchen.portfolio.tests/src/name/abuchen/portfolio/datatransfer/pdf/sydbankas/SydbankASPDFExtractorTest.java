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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
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
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.SydbankASPDFExtractor;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class SydbankASPDFExtractorTest
{
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
                        hasNote("Ref.-Nr.: 1195520/E"), //
                        hasAmount("DKK", 26036.89), hasGrossValue("DKK", 26076.00), //
                        hasTaxes("DKK", 0.00), hasFees("DKK", 39.11))));
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
                        hasNote("Ref.-Nr.: 1195520/E"), //
                        hasAmount("DKK", 104.50), hasGrossValue("DKK", 104.50), //
                        hasTaxes("DKK", 0.00), hasFees("DKK", 0.00))));
    }
}
