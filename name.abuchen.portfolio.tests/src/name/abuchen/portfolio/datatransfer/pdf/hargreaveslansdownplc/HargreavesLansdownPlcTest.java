package name.abuchen.portfolio.datatransfer.pdf.hargreaveslansdownplc;

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
import name.abuchen.portfolio.datatransfer.pdf.HargreavesLansdownPlcExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class HargreavesLansdownPlcTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new HargreavesLansdownPlcExtractor(new Client());

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
        new AssertImportActions().check(results, "GBP");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B4X9L533"), hasWkn(null), hasTicker("HMWO"), //
                        hasName("HSBC ETFs Plc MSCI World ETF GBP"), //
                        hasCurrencyCode("GBP"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-10-15T09:47"), hasShares(2539.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Ref.: B105372223-01000000"), //
                        hasAmount("GBP", 57581.69), hasGrossValue("GBP", 57569.74), //
                        hasTaxes("GBP", 0.00), hasFees("GBP", 11.95))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var extractor = new HargreavesLansdownPlcExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "GBP");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("GB00BG0QPJ30"), hasWkn(null), hasTicker(null), //
                        hasName("Legal & General UK Index Class C - Accumulation (GBP)"), //
                        hasCurrencyCode("GBP"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-04-03T08:00"), hasShares(1043.478), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Ref.: B918390061-01000000"), //
                        hasAmount("GBP", 3000.00), hasGrossValue("GBP", 3000.00), //
                        hasTaxes("GBP", 0.00), hasFees("GBP", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var extractor = new HargreavesLansdownPlcExtractor(new Client());

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
        new AssertImportActions().check(results, "GBP");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("GB00BKX5CN86"), hasWkn(null), hasTicker("JE"), //
                        hasName("Just Eat plc Ordinary Shares 1p"), //
                        hasCurrencyCode("GBP"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-03-27T11:46"), hasShares(132.00), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Ref.: S362136892-01000000"), //
                        hasAmount("GBP", 981.87), hasGrossValue("GBP", 993.82), //
                        hasTaxes("GBP", 0.00), hasFees("GBP", 11.95))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        var extractor = new HargreavesLansdownPlcExtractor(new Client());

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
        new AssertImportActions().check(results, "GBP");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BKX55S42"), hasWkn(null), hasTicker("VERX"), //
                        hasName("Vanguard Funds plc FTSE Developed Europe ex-UK UCITS ETF"), //
                        hasCurrencyCode("GBP"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-01-25T09:35"), hasShares(5700.00), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Ref.: S186039383-01000000"), //
                        hasAmount("GBP", 172287.66), hasGrossValue("GBP", 172299.61), //
                        hasTaxes("GBP", 0.00), hasFees("GBP", 11.95))));
    }
}
