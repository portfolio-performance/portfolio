package name.abuchen.portfolio.datatransfer.pdf.alpaccapital;

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
import name.abuchen.portfolio.datatransfer.pdf.AlpacCapitalPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class AlpacCapitalPDFExtractorTest
{
    @Test
    public void testSecurityBuy01()
    {
        AlpacCapitalPDFExtractor extractor = new AlpacCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("46436E718"), hasTicker("SGOV"), //
                        hasName(null), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-11T15:45:50"), hasShares(1.079762663), //
                        hasSource("Buy01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 108.46), hasGrossValue("USD", 108.46), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));
    }

    @Test
    public void testSecurityBuy02()
    {
        AlpacCapitalPDFExtractor extractor = new AlpacCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("922908363"), hasTicker("VOO"), //
                        hasName(null), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("46436E718"), hasTicker("SGOV"), //
                        hasName(null), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-17T11:27:43"), hasShares(1.00), //
                        hasSource("Buy02.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 513.94), hasGrossValue("USD", 512.94), //
                        hasTaxes("USD", 0.00), hasFees("USD", 1.00))));


        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-07-17T11:27:42"), hasShares(1.078967788), //
                        hasSource("Buy02.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 108.46), hasGrossValue("USD", 108.46), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-17T15:45:51"), hasShares(16.00), //
                        hasSource("Buy02.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 1608.43), hasGrossValue("USD", 1608.43), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-17T15:45:51"), hasShares(1.722824162), //
                        hasSource("Buy02.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 173.19), hasGrossValue("USD", 173.19), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));
    }
}
