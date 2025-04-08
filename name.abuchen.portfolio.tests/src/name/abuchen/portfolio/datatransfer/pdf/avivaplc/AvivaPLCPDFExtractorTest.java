package name.abuchen.portfolio.datatransfer.pdf.avivaplc;

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
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSecurities;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.AvivaPLCPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class AvivaPLCPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        AvivaPLCPDFExtractor extractor = new AvivaPLCPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "GBP");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("GB00B5B74S01"), hasWkn("B5B74S0"), hasTicker("FPD4"), //
                        hasName("Vanguard US Equity Index I£"), //
                        hasCurrencyCode("GBP"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-11-02T21:00"), hasShares(1.5661), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Order reference: ############"), //
                        hasAmount("GBP", 999.99), hasGrossValue("GBP", 999.99), //
                        hasTaxes("GBP", 0.00), hasFees("GBP", 0.00))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        AvivaPLCPDFExtractor extractor = new AvivaPLCPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "GBP");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("GB00B5B74S01"), hasWkn("B5B74S0"), hasTicker("FPD4"), //
                        hasName("Vanguard US Equity Index I£"), //
                        hasCurrencyCode("GBP"))));

        assertThat(results, hasItem(security( //
                        hasIsin("GB00BMJJJF91"), hasWkn("BMJJJF9"), hasTicker("KLDQ"), //
                        hasName("HSBC FTSE All World Index C"), //
                        hasCurrencyCode("GBP"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-08-30T21:00"), hasShares(1.5399), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Order reference: ###########"), //
                        hasAmount("GBP", 999.99), hasGrossValue("GBP", 999.99), //
                        hasTaxes("GBP", 0.00), hasFees("GBP", 0.00))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-08-30T12:00"), hasShares(131.3900), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Order reference: #########"), //
                        hasAmount("GBP", 333.34), hasGrossValue("GBP", 333.34), //
                        hasTaxes("GBP", 0.00), hasFees("GBP", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        AvivaPLCPDFExtractor extractor = new AvivaPLCPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "GBP");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("Av MyM My Future Growth"), //
                        hasCurrencyCode("GBP"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-03-07T23:59"), hasShares(792.2496), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Order reference: #########"), //
                        hasAmount("GBP", 1777.41), hasGrossValue("GBP", 1777.41), //
                        hasTaxes("GBP", 0.00), hasFees("GBP", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        AvivaPLCPDFExtractor extractor = new AvivaPLCPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "GBP");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("GB00BMJJJF91"), hasWkn("BMJJJF9"), hasTicker("KLDQ"), //
                        hasName("HSBC FTSE All World Index C"), //
                        hasCurrencyCode("GBP"))));

        assertThat(results, hasItem(security( //
                        hasIsin("GB00B5B71Q71"), hasWkn("B5B71Q7"), hasTicker("FPD3"), //
                        hasName("Vanguard US Equity Index A£"), //
                        hasCurrencyCode("GBP"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-03-27T12:00"), hasShares(92.3400), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Order reference: ############"), //
                        hasAmount("GBP", 218.75), hasGrossValue("GBP", 218.75), //
                        hasTaxes("GBP", 0.00), hasFees("GBP", 0.00))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-03-24T21:00"), hasShares(0.0071), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Order reference: ###########"), //
                        hasAmount("GBP", 5.06), hasGrossValue("GBP", 5.06), //
                        hasTaxes("GBP", 0.00), hasFees("GBP", 0.00))));
    }
}
