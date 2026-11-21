package name.abuchen.portfolio.datatransfer.pdf.oberbankag;

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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.withFailureMessage;
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

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.OberbankAGPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class OberbankAGPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new OberbankAGPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CA09228F1036"), hasWkn(null), hasTicker(null), //
                        hasName("BlackBerry Ltd. Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-01-28T09:16:17"), hasShares(14.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Auftrags-Nr. 999999 | Abrechnungs-Nr. 888888"), //
                        hasAmount("EUR", 274.62), hasGrossValue("EUR", 267.37), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 7.25))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var extractor = new OberbankAGPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("AT000B127337"), hasWkn(null), hasTicker(null), //
                        hasName("Oberbank AG Nachr. Anleihe 2023-2031"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-08-07T20:09:48"), hasShares(80.00), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Abrechnungs-Nr. 888888 | Stückzinsen f. 166 Tage: 165,55 EUR"), //
                        hasAmount("EUR", 8068.87), hasGrossValue("EUR", 8029.55), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 39.32))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        var extractor = new OberbankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("AT0000784830"), hasWkn(null), hasTicker(null), //
                        hasName("3 Banken Aktien-Dachfonds (T) Miteigentumsanteile - Thesaurierend"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-03-09T00:00:01"), hasShares(9.60), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Auftrags-Nr. 999999 | Abrechnungs-Nr. 888888"), //
                        hasAmount("EUR", 239.93), hasGrossValue("EUR", 239.33), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.60))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        var extractor = new OberbankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("AT0000A06NX7"), hasWkn(null), hasTicker(null), //
                        hasName("Oberbank Vermögensmanagement (T) Miteigentumsanteile - Thesaurierend"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-02-05T00:00:01"), hasShares(0.58), //
                        hasSource("Kauf04.txt"), //
                        hasNote("Auftrags-Nr. 999999 | Abrechnungs-Nr. 888888"), //
                        hasAmount("EUR", 79.60), hasGrossValue("EUR", 79.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.20))));
    }

    @Test
    public void testWertpapierKaufStorno01()
    {
        var extractor = new OberbankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KaufStorno01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US79466L3024"), hasWkn(null), hasTicker(null), //
                        hasName("salesforce.com Inc. Registered Shares DL -,001"), //
                        hasCurrencyCode("EUR"))));

        // check cancellation (Storno) transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        purchase( //
                                        hasDate("2021-04-12T09:09:43"), hasShares(9.00), //
                                        hasSource("KaufStorno01.txt"), //
                                        hasNote("Auftrags-Nr. 999999 | Abrechnungs-Nr. 888888"), //
                                        hasAmount("EUR", 1753.25), hasGrossValue("EUR", 1746.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 7.25)))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var extractor = new OberbankAGPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("AT0000730007"), hasWkn(null), hasTicker(null), //
                        hasName("ANDRITZ AG AKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-04-29T09:40:30"), hasShares(95.00), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Auftrags-Nr. 999999 | Abrechnungs-Nr. 888888"), //
                        hasAmount("EUR", 4177.42), hasGrossValue("EUR", 4265.50), //
                        hasTaxes("EUR", 73.15), hasFees("EUR", 14.93))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        var extractor = new OberbankAGPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("AT0000A06NX7"), hasWkn(null), hasTicker(null), //
                        hasName("Oberbank Vermögensmanagement (T) Miteigentumsanteile - Thesaurierend"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-01-22T00:00:01"), hasShares(6.60), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Auftrags-Nr. 999999 | Abrechnungs-Nr. 888888"), //
                        hasAmount("EUR", 909.89), hasGrossValue("EUR", 923.74), //
                        hasTaxes("EUR", 13.85), hasFees("EUR", 0.00))));
    }
}
