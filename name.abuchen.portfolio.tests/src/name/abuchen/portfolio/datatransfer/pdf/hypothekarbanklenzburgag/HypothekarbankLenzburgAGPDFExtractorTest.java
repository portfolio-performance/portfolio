package name.abuchen.portfolio.datatransfer.pdf.hypothekarbanklenzburgag;

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

import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.HypothekarbankLenzburgAGPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class HypothekarbankLenzburgAGPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        HypothekarbankLenzburgAGPDFExtractor extractor = new HypothekarbankLenzburgAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE000716YHJ7"), hasWkn("125615212"), hasTicker(null), //
                        hasName("Inve FTSE All"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-03-14T00:00"), hasShares(720), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Transaktion 61327806-0002"), //
                        hasAmount("CHF", 3974.14), hasGrossValue("CHF", 3948.48), //
                        hasTaxes("CHF", 5.92), hasFees("CHF", 19.74))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        HypothekarbankLenzburgAGPDFExtractor extractor = new HypothekarbankLenzburgAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE000716YHJ7"), hasWkn("125615212"), hasTicker(null), //
                        hasName("Inve FTSE All"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-04-04T00:00"), hasShares(44), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Transaktion 62108127-0002"), //
                        hasAmount("CHF", 251.86), hasGrossValue("CHF", 250.23), //
                        hasTaxes("CHF", 0.38), hasFees("CHF", 1.25))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        HypothekarbankLenzburgAGPDFExtractor extractor = new HypothekarbankLenzburgAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BKS7L097"), hasWkn("51992937"), hasTicker(null), //
                        hasName("Inv S&P 500 ESG"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-04-04T00:00"), hasShares(4), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Transaktion 62108136-0002"), //
                        hasAmount("CHF", 260.10), hasGrossValue("CHF", 258.42), //
                        hasTaxes("CHF", 0.39), hasFees("CHF", 1.29))));
    }
}
