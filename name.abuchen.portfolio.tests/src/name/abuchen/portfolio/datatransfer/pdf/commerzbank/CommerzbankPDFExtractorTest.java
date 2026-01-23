package name.abuchen.portfolio.datatransfer.pdf.commerzbank;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.check;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxRefund;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxes;
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
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.pdf.CommerzbankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class CommerzbankPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

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
                        hasIsin(null), hasWkn("A0RPWH"), hasTicker(null), //
                        hasName("i S h s I I I - C o r e MSCI W o r l d U . E T F R e g i s t e r e d S h s USD ( A c c ) o . N ."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-04-18T00:00"), hasShares(0.572), //
                        hasSource("Kauf01.txt"), //
                        hasNote("R.-Nr.: 419794916798D1C2"), //
                        hasAmount("EUR", 24.96), hasGrossValue("EUR", 24.96), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

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
                        hasIsin(null), hasWkn("A0M16S"), hasTicker(null), //
                        hasName("V e r m ö g e n s M a n a g e m e n t B a l a n c e I n h a b e r - A n t e i l e A ( E U R ) o . N ."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-17T00:00"), hasShares(0.345), //
                        hasSource("Kauf02.txt"), //
                        hasNote("R.-Nr.: 540678415889D422"), //
                        hasAmount("EUR", 49.92), hasGrossValue("EUR", 48.73), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.19))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

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
                        hasIsin(null), hasWkn("723610"), hasTicker(null), //
                        hasName("S i e m e n s AG N a m e n s - A k t i e n o . N ."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-01-31T16:59"), hasShares(200.00), //
                        hasSource("Kauf03.txt"), //
                        hasNote("R.-Nr.: 111000000123DAF2"), //
                        hasAmount("EUR", 19907.13), hasGrossValue("EUR", 19852.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 49.63 + 4.90 + 0.60))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

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
                        hasIsin(null), hasWkn("840400"), hasTicker(null), //
                        hasName("A l l i a n z SE v i n k . N a m e n s - A k t i e n o . N ."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-07-12T11:46"), hasShares(250.00), //
                        hasSource("Kauf04.txt"), //
                        hasNote("R.-Nr.: 445789927068DF92"), //
                        hasAmount("EUR", 47878.37), hasGrossValue("EUR", 47752.16), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 119.38 + 2.90 + 1.20 + 2.73))));
    }

    @Test
    public void testWertpapierKauf05()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf05.txt"), errors);

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
                        hasIsin(null), hasWkn("A2DWBY"), hasTicker(null), //
                        hasName("i S h s I I I - M S C I W l d S m . C a . U C I . E T F R e g i s t e r e d S h a r e s U S D ( A c c ) o . N ."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-10-15T00:00"), hasShares(192.60), //
                        hasSource("Kauf05.txt"), //
                        hasNote("R.-Nr.: [persönlichjeDaten}"), //
                        hasAmount("EUR", 1250.00), hasGrossValue("EUR", 1244.39), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.61))));
    }

    @Test
    public void testWertpapierKauf06()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("851995"), hasTicker(null), //
                        hasName("P e p s i C o I n c . R e g i s t e r e d S h a r e s DL - , 0 1 6 6"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-03-13T19:40"), hasShares(70.00), //
                        hasSource("Kauf06.txt"), //
                        hasNote("R.-Nr.: 662692715010DF62"), //
                        hasAmount("USD", 10447.04), hasGrossValue("USD", 10414.04), //
                        hasTaxes("USD", 0.00), hasFees("USD", 26.04 + 5.31 + 1.65))));
    }

    @Test
    public void testSteuerbehandlungVonKauf06()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonKauf06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US7134481081"), hasWkn("851995"), hasTicker(null), //
                        hasName("PEPSICO INC. DL-,0166"), //
                        hasCurrencyCode("USD"))));

        // check taxes transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2025-03-13T00:00"), hasShares(70.00), //
                                        hasSource("SteuerbehandlungVonKauf06.txt"), //
                                        hasNote("Ref.-Nr.: 486s8957oOs3154R"), //
                                        hasAmount("USD", 0.00), hasGrossValue("USD", 0.00), //
                                        hasTaxes("USD", 0.00), hasFees("USD", 0.00)))));
    }

    @Test
    public void testSteuerbehandlungVonKauf06WithSecurityInEUR()
    {
        var security = new Security("PEPSICO INC. DL-,0166", "EUR");
        security.setIsin("US7134481081");
        security.setWkn("851995");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new CommerzbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonKauf06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "USD");

        // check taxes transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2025-03-13T00:00"), hasShares(70.00), //
                                        hasSource("SteuerbehandlungVonKauf06.txt"), //
                                        hasNote("Ref.-Nr.: 486s8957oOs3154R"), //
                                        hasAmount("USD", 0.00), hasGrossValue("USD", 0.00), //
                                        hasForexGrossValue("EUR", 0.00), //
                                        hasTaxes("USD", 0.00), hasFees("USD", 0.00), //
                                        check(tx -> {
                                            var c = new CheckCurrenciesAction();
                                            var account = new Account();
                                            account.setCurrencyCode("USD");
                                            var s = c.process((AccountTransaction) tx, account);
                                            assertThat(s, is(Status.OK_STATUS));
                                        })))));
    }

    @Test
    public void testWertpapierKauf06MitSteuerbehandlungVonKauf06()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Kauf06.txt", "SteuerbehandlungVonKauf06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("851995"), hasTicker(null), //
                        hasName("P e p s i C o I n c . R e g i s t e r e d S h a r e s DL - , 0 1 6 6"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-03-13T19:40"), hasShares(70.00), //
                        hasSource("Kauf06.txt; SteuerbehandlungVonKauf06.txt"), //
                        hasNote("R.-Nr.: 662692715010DF62 | Ref.-Nr.: 486s8957oOs3154R"), //
                        hasAmount("USD", 10447.04), hasGrossValue("USD", 10414.04), //
                        hasTaxes("USD", 0.00), hasFees("USD", 26.04 + 5.31 + 1.65))));
    }

    @Test
    public void testWertpapierKauf06MitSteuerbehandlungVonKauf06WithSecurityInEUR()
    {
        var security = new Security("P e p s i C o I n c . R e g i s t e r e d S h a r e s DL - , 0 1 6 6", "EUR");
        security.setWkn("851995");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new CommerzbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Kauf06.txt", "SteuerbehandlungVonKauf06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "USD");

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-03-13T19:40"), hasShares(70.00), //
                        hasSource("Kauf06.txt; SteuerbehandlungVonKauf06.txt"), //
                        hasNote("R.-Nr.: 662692715010DF62 | Ref.-Nr.: 486s8957oOs3154R"), //
                        hasAmount("USD", 10447.04), hasGrossValue("USD", 10414.04), //
                        hasForexGrossValue("EUR", 9617.69), //
                        hasTaxes("USD", 0.00), hasFees("USD", 26.04 + 5.31 + 1.65))));
    }

    @Test
    public void testWertpapierKauf06MitSteuerbehandlungVonKauf06_SourceFilesReversed()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonKauf06.txt", "Kauf06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US7134481081"), hasWkn("851995"), hasTicker(null), //
                        hasName("PEPSICO INC. DL-,0166"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-03-13T19:40"), hasShares(70.00), //
                        hasSource("Kauf06.txt; SteuerbehandlungVonKauf06.txt"), //
                        hasNote("R.-Nr.: 662692715010DF62 | Ref.-Nr.: 486s8957oOs3154R"), //
                        hasAmount("USD", 10447.04), hasGrossValue("USD", 10414.04), //
                        hasTaxes("USD", 0.00), hasFees("USD", 26.04 + 5.31 + 1.65))));
    }

    @Test
    public void testWertpapierKauf06MitSteuerbehandlungVonKauf06WithSecurityInEUR_SourceFilesReversed()
    {
        var security = new Security("P e p s i C o I n c . R e g i s t e r e d S h a r e s DL - , 0 1 6 6", "EUR");
        security.setWkn("851995");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new CommerzbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonKauf06.txt", "Kauf06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "USD");

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-03-13T19:40"), hasShares(70.00), //
                        hasSource("Kauf06.txt; SteuerbehandlungVonKauf06.txt"), //
                        hasNote("R.-Nr.: 662692715010DF62 | Ref.-Nr.: 486s8957oOs3154R"), //
                        hasAmount("USD", 10447.04), hasGrossValue("USD", 10414.04), //
                        hasForexGrossValue("EUR", 9617.69), //
                        hasTaxes("USD", 0.00), hasFees("USD", 26.04 + 5.31 + 1.65))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

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
                        hasIsin(null), hasWkn("A0M16S"), hasTicker(null), //
                        hasName("V e r m ö g e n s M a n a g e m e n t B a l a n c e I n h a b e r - A n t e i l e A ( E U R ) o . N ."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-02-17T19:44"), hasShares(10.195), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("R.-Nr.: 5406785154111111"), //
                        hasAmount("EUR", 1439.13), hasGrossValue("EUR", 1439.13), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSteuerbehandlungVonVerkauf01()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonVerkauf01.txt"),
                        errors);

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
                        hasIsin("LU0321021155"), hasWkn("A0M16S"), hasTicker(null), //
                        hasName("VERMOEGENSMA.BALANCE A EO"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2021-02-17T00:00"), hasShares(10.195), //
                        hasSource("SteuerbehandlungVonVerkauf01.txt"), //
                        hasNote("Ref.-Nr.: 0W7U3RJX11111111"), //
                        hasAmount("EUR", 27.31), hasGrossValue("EUR", 27.31), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf01MitSteuerbehandlungVonVerkauf01()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt", "SteuerbehandlungVonVerkauf01.txt"),
                        errors);

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
                        hasIsin(null), hasWkn("A0M16S"), hasTicker(null), //
                        hasName("V e r m ö g e n s M a n a g e m e n t B a l a n c e I n h a b e r - A n t e i l e A ( E U R ) o . N ."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-02-17T19:44"), hasShares(10.195), //
                        hasSource("Verkauf01.txt; SteuerbehandlungVonVerkauf01.txt"), //
                        hasNote("R.-Nr.: 5406785154111111 | Ref.-Nr.: 0W7U3RJX11111111"), //
                        hasAmount("EUR", 1411.82), hasGrossValue("EUR", 1439.13), //
                        hasTaxes("EUR", 27.31), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf01MitSteuerbehandlungVonVerkauf01_SourceFilesReversed()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonVerkauf01.txt", "Verkauf01.txt"),
                        errors);

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
                        hasIsin("LU0321021155"), hasWkn("A0M16S"), hasTicker(null), //
                        hasName("VERMOEGENSMA.BALANCE A EO"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-02-17T19:44"), hasShares(10.195), //
                        hasSource("Verkauf01.txt; SteuerbehandlungVonVerkauf01.txt"), //
                        hasNote("R.-Nr.: 5406785154111111 | Ref.-Nr.: 0W7U3RJX11111111"), //
                        hasAmount("EUR", 1411.82), hasGrossValue("EUR", 1439.13), //
                        hasTaxes("EUR", 27.31), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

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
                        hasIsin(null), hasWkn("A0M16S"), hasTicker(null), //
                        hasName("V e r m ö g e n s M a n a g e m e n t B a l a n c e I n h a b e r - A n t e i l e A ( E U R ) o . N ."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-02-15T19:26"), hasShares(4.00), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("R.-Nr.: 540504199522DA72"), //
                        hasAmount("EUR", 562.92), hasGrossValue("EUR", 562.92), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSteuerbehandlungVonVerkauf02()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonVerkauf02.txt"),
                        errors);

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
                        hasIsin("LU0321021155"), hasWkn("A0M16S"), hasTicker(null), //
                        hasName("VERMOEGENSMA.BALANCE A EO"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2021-02-15T00:00"), hasShares(4.00), //
                        hasSource("SteuerbehandlungVonVerkauf02.txt"), //
                        hasNote("Ref.-Nr.: 2K7U3MQX11111111"), //
                        hasAmount("EUR", 11.59), hasGrossValue("EUR", 11.59), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf02MitSteuerbehandlungVonVerkauf02()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Verkauf02.txt", "SteuerbehandlungVonVerkauf02.txt"),
                        errors);

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
                        hasIsin(null), hasWkn("A0M16S"), hasTicker(null), //
                        hasName("V e r m ö g e n s M a n a g e m e n t B a l a n c e I n h a b e r - A n t e i l e A ( E U R ) o . N ."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-02-15T19:26"), hasShares(4.00), //
                        hasSource("Verkauf02.txt; SteuerbehandlungVonVerkauf02.txt"), //
                        hasNote("R.-Nr.: 540504199522DA72 | Ref.-Nr.: 2K7U3MQX11111111"), //
                        hasAmount("EUR", 551.33), hasGrossValue("EUR", 562.92), //
                        hasTaxes("EUR", 11.59), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf02MitSteuerbehandlungVonVerkauf02_SourceFilesReversed()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonVerkauf02.txt", "Verkauf02.txt"),
                        errors);

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
                        hasIsin("LU0321021155"), hasWkn("A0M16S"), hasTicker(null), //
                        hasName("VERMOEGENSMA.BALANCE A EO"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-02-15T19:26"), hasShares(4.00), //
                        hasSource("Verkauf02.txt; SteuerbehandlungVonVerkauf02.txt"), //
                        hasNote("R.-Nr.: 540504199522DA72 | Ref.-Nr.: 2K7U3MQX11111111"), //
                        hasAmount("EUR", 551.33), hasGrossValue("EUR", 562.92), //
                        hasTaxes("EUR", 11.59), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf03.txt"), errors);

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
                        hasIsin(null), hasWkn("840400"), hasTicker(null), //
                        hasName("A l l i a n z SE v i n k . N a m e n s - A k t i e n o . N ."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-01-31T13:10"), hasShares(200.00), //
                        hasSource("Verkauf03.txt"), //
                        hasNote("R.-Nr.: 498786858483DBB2"), //
                        hasAmount("EUR", 40205.45), hasGrossValue("EUR", 40340.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 100.85 + 4.90 + 24.19 + 4.61))));
    }

    @Test
    public void testWertpapierVerkauf04()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf04.txt"), errors);

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
                        hasIsin(null), hasWkn("840400"), hasTicker(null), //
                        hasName("A l l i a n z SE v i n k . N a m e n s - A k t i e n o . N ."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2018-03-02T12:06"), hasShares(250.00), //
                        hasSource("Verkauf04.txt"), //
                        hasNote("R.-Nr.: 445789927068DF92"), //
                        hasAmount("EUR", 45918.97), hasGrossValue("EUR", 46039.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 115.10 + 2.90 + 2.63))));
    }

    @Test
    public void testWertpapierVerkauf05()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf05.txt"), errors);

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
                        hasIsin(null), hasWkn("A3E2FV"), hasTicker(null), //
                        hasName("C a n o p y G r o w t h C o r p . R e g i s t e r e d S h a r e s o . N ."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-09-27T13:02"), hasShares(4.00), //
                        hasSource("Verkauf05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.13), hasGrossValue("EUR", 16.26), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 9.90 + 3.25 + 1.98))));
    }

    @Test
    public void testWertpapierVerkauf06()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("852523"), hasTicker(null), //
                        hasName("S o u t h e r n C o . , T h e R e g i s t e r e d S h a r e s DL 5"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-03-06T18:44"), hasShares(150.00), //
                        hasSource("Verkauf06.txt"), //
                        hasNote("R.-Nr.: 628882782605D292"), //
                        hasAmount("USD", 13217.24), hasGrossValue("USD", 13257.31), //
                        hasTaxes("USD", 0.00), hasFees("USD", 33.14 + 5.28 + 1.65))));
    }

    @Test
    public void testSteuerbehandlungVonVerkauf06()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonVerkauf06.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US8425871071"), hasWkn("852523"), hasTicker(null), //
                        hasName("THE SOUTHERN CO. DL 5"), //
                        hasCurrencyCode("USD"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2025-03-06T00:00"), hasShares(150.00), //
                        hasSource("SteuerbehandlungVonVerkauf06.txt"), //
                        hasNote("Ref.-Nr.: 8S7g9fWBki19769c"), //
                        hasAmount("USD", 1834.41), hasGrossValue("USD", 1834.41), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));
    }

    @Test
    public void testSteuerbehandlungVonVerkauf06WithSecurityInEUR()
    {
        var security = new Security("THE SOUTHERN CO. DL 5", "EUR");
        security.setIsin("US8425871071");
        security.setWkn("852523");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new CommerzbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonVerkauf06.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "USD");

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2025-03-06T00:00"), hasShares(150.00), //
                        hasSource("SteuerbehandlungVonVerkauf06.txt"), //
                        hasNote("Ref.-Nr.: 8S7g9fWBki19769c"), //
                        hasAmount("USD", 1834.41), hasGrossValue("USD", 1834.41), //
                        hasForexGrossValue("EUR", 1693.82), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("USD");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierVerkauf06MitSteuerbehandlungVonVerkauf06()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Verkauf06.txt", "SteuerbehandlungVonVerkauf06.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("852523"), hasTicker(null), //
                        hasName("S o u t h e r n C o . , T h e R e g i s t e r e d S h a r e s DL 5"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-03-06T18:44"), hasShares(150.00), //
                        hasSource("Verkauf06.txt; SteuerbehandlungVonVerkauf06.txt"), //
                        hasNote("R.-Nr.: 628882782605D292 | Ref.-Nr.: 8S7g9fWBki19769c"), //
                        hasAmount("USD", 11382.83), hasGrossValue("USD", 13257.31), //
                        hasTaxes("USD", 1834.41), hasFees("USD", 33.14 + 5.28 + 1.65))));
    }

    @Test
    public void testWertpapierVerkauf06MitSteuerbehandlungVonVerkauf06WithSecurityInEUR()
    {
        var security = new Security("S o u t h e r n C o . , T h e R e g i s t e r e d S h a r e s DL 5", "EUR");
        security.setWkn("852523");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new CommerzbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Verkauf06.txt", "SteuerbehandlungVonVerkauf06.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "USD");

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-03-06T18:44"), hasShares(150.00), //
                        hasSource("Verkauf06.txt; SteuerbehandlungVonVerkauf06.txt"), //
                        hasNote("R.-Nr.: 628882782605D292 | Ref.-Nr.: 8S7g9fWBki19769c"), //
                        hasAmount("USD", 11382.83), hasGrossValue("USD", 13257.31), //
                        hasForexGrossValue("EUR", 12241.28), //
                        hasTaxes("USD", 1834.41), hasFees("USD", 33.14 + 5.28 + 1.65))));
    }

    @Test
    public void testWertpapierVerkauf06MitSteuerbehandlungVonVerkauf06_SourceFilesReversed()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonVerkauf06.txt", "Verkauf06.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US8425871071"), hasWkn("852523"), hasTicker(null), //
                        hasName("THE SOUTHERN CO. DL 5"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-03-06T18:44"), hasShares(150.00), //
                        hasSource("Verkauf06.txt; SteuerbehandlungVonVerkauf06.txt"), //
                        hasNote("R.-Nr.: 628882782605D292 | Ref.-Nr.: 8S7g9fWBki19769c"), //
                        hasAmount("USD", 11382.83), hasGrossValue("USD", 13257.31), //
                        hasTaxes("USD", 1834.41), hasFees("USD", 33.14 + 5.28 + 1.65))));
    }

    @Test
    public void testWertpapierVerkauf06MitSteuerbehandlungVonVerkauf06WithSecurityInEUR_SourceFilesReversed()
    {
        var security = new Security("S o u t h e r n C o . , T h e R e g i s t e r e d S h a r e s DL 5", "EUR");
        security.setWkn("852523");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new CommerzbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonVerkauf06.txt", "Verkauf06.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "USD");

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-03-06T18:44"), hasShares(150.00), //
                        hasSource("Verkauf06.txt; SteuerbehandlungVonVerkauf06.txt"), //
                        hasNote("R.-Nr.: 628882782605D292 | Ref.-Nr.: 8S7g9fWBki19769c"), //
                        hasAmount("USD", 11382.83), hasGrossValue("USD", 13257.31), //
                        hasForexGrossValue("EUR", 12241.28), //
                        hasTaxes("USD", 1834.41), hasFees("USD", 33.14 + 5.28 + 1.65))));
    }

    @Test
    public void testSteuermitteilungOhneVerkauf01()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SteuermitteilungOhneVerkauf01.txt"),
                        errors);

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
                        hasIsin("DE0008404005"), hasWkn("840400"), hasTicker(null), //
                        hasName("ALLIANZ SE NA O.N."), //
                        hasCurrencyCode("EUR"))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2018-03-02T00:00"), hasShares(250.00), //
                        hasSource("SteuermitteilungOhneVerkauf01.txt"), //
                        hasNote("Ref.-Nr.: 0U7QASH3SPP000RP"), //
                        hasAmount("EUR", 548.51), hasGrossValue("EUR", 548.51), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A0J2060"), hasWkn("A0J206"), hasTicker(null), //
                        hasName("iShs-MSCI N . America UCITS ETF Bearer Shares ( D t . Z e r t . ) o . N ."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2015-06-18T00:00"), hasShares(123.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Ref.-Nr.: 3345AO12BC3D4445E"), //
                        hasAmount("EUR", 123.45), hasGrossValue("EUR", 123.45), //
                        hasForexGrossValue("USD", 141.07), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende01WithSecurityInEUR()
    {
        var security = new Security("iShs-MSCI N . America UCITS ETF Bearer Shares ( D t . Z e r t . ) o . N .", "EUR");
        security.setIsin("DE000A0J2060");
        security.setWkn("A0J206");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new CommerzbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2015-06-18T00:00"), hasShares(123.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Ref.-Nr.: 3345AO12BC3D4445E"), //
                        hasAmount("EUR", 123.45), hasGrossValue("EUR", 123.45), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende02()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A0DPMW9"), hasWkn("A0DPMW"), hasTicker(null), //
                        hasName("iShares-MSCI Japan UETF DIS Bearer Shares ( D t . Z e r t . ) o . N ."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2015-07-16T00:00"), hasShares(1234.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Ref.-Nr.: 1A2BCDEFGH1234I"), //
                        hasAmount("EUR", 1045.67), hasGrossValue("EUR", 1045.67), //
                        hasForexGrossValue("USD", 1141.55), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende02WithSecurityInEUR()
    {
        var security = new Security("iShares-MSCI Japan UETF DIS Bearer Shares ( D t . Z e r t . ) o . N .", "EUR");
        security.setIsin("DE000A0DPMW9");
        security.setWkn("A0DPMW");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new CommerzbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2015-07-16T00:00"), hasShares(1234.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Ref.-Nr.: 1A2BCDEFGH1234I"), //
                        hasAmount("EUR", 1045.67), hasGrossValue("EUR", 1045.67), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende03()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

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
                        hasIsin("US7960502018"), hasWkn("881823"), hasTicker(null), //
                        hasName("Samsung E l e c t r o n i c s Co. L t d . R.Shs(NV)Pf(GDR144A)/25 SW 100"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-05-26T00:00"), hasShares(12.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote("Ref.-Nr.: 1234567890ABCDEF"), //
                        hasAmount("EUR", 61.30), hasGrossValue("EUR", 61.52), //
                        hasForexGrossValue("USD", 67.57), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.22))));
    }

    @Test
    public void testDividende03WithSecurityInEUR()
    {
        var security = new Security("Samsung E l e c t r o n i c s Co. L t d . R.Shs(NV)Pf(GDR144A)/25 SW 100", "EUR");
        security.setIsin("US7960502018");
        security.setWkn("881823");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new CommerzbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-05-26T00:00"), hasShares(12.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote("Ref.-Nr.: 1234567890ABCDEF"), //
                        hasAmount("EUR", 61.30), hasGrossValue("EUR", 61.52), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.22), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende04()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende04.txt"), errors);

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
                        hasIsin("DE0007236101"), hasWkn("723610"), hasTicker(null), //
                        hasName("Siemens AG Namens-Aktien o . N ."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-02-10T00:00"), hasShares(500.00), //
                        hasSource("Dividende04.txt"), //
                        hasNote("Ref.-Nr.: 1X1XX1XXXXX00111"), //
                        hasAmount("EUR", 1950.00), hasGrossValue("EUR", 1950.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende05()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende05.txt"), errors);

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
                        hasIsin("DE0008475096"), hasWkn("847509"), hasTicker(null), //
                        hasName("A l l i a n z R o h s t o f f f o n d s I n h a b e r - A n t e i l e A ( E U R )"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-05T00:00"), hasShares(329.817), //
                        hasSource("Dividende05.txt"), //
                        hasNote("Ref.-Nr.: 1U7Z8J3RQTF000SP"), //
                        hasAmount("EUR", 452.51), hasGrossValue("EUR", 452.51), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSteuerbehandlungVonDividende05()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende05.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0008475096"), hasWkn("847509"), hasTicker(null), //
                        hasName("ALLIANZ ROHSTOFFFONDS A"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2025-03-05T00:00"), hasShares(329.817), //
                                        hasSource("SteuerbehandlungVonDividende05.txt"), //
                                        hasNote("Ref.-Nr.: 0u2B2W6NsfL386NS"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testDividende05MitSteuerbehandlungVonDividende05()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende05.txt", "SteuerbehandlungVonDividende05.txt"),
                        errors);

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
                        hasIsin("DE0008475096"), hasWkn("847509"), hasTicker(null), //
                        hasName("A l l i a n z R o h s t o f f f o n d s I n h a b e r - A n t e i l e A ( E U R )"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-05T00:00"), hasShares(329.817), //
                        hasSource("Dividende05.txt; SteuerbehandlungVonDividende05.txt"), //
                        hasNote("Ref.-Nr.: 1U7Z8J3RQTF000SP | Ref.-Nr.: 0u2B2W6NsfL386NS"), //
                        hasAmount("EUR", 452.51), hasGrossValue("EUR", 452.51), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende05MitSteuerbehandlungVonDividende05_SourceFilesReversed()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende05.txt", "Dividende05.txt"),
                        errors);

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
                        hasIsin("DE0008475096"), hasWkn("847509"), hasTicker(null), //
                        hasName("ALLIANZ ROHSTOFFFONDS A"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-05T00:00"), hasShares(329.817), //
                        hasSource("Dividende05.txt; SteuerbehandlungVonDividende05.txt"), //
                        hasNote("Ref.-Nr.: 1U7Z8J3RQTF000SP | Ref.-Nr.: 0u2B2W6NsfL386NS"), //
                        hasAmount("EUR", 452.51), hasGrossValue("EUR", 452.51), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende06()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende06.txt"), errors);

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
                        hasIsin("US8725901040"), hasWkn("A1T7LU"), hasTicker(null), //
                        hasName("T - M o b i l e U S I n c . R e g i s t e r e d S h a r e s D L - , 0 0 0 0 1"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-13T00:00"), hasShares(120.00), //
                        hasSource("Dividende06.txt"), //
                        hasNote("Ref.-Nr.: 6R3y9052SfF124A7"), //
                        hasAmount("EUR", 96.99), hasGrossValue("EUR", 96.99), //
                        hasForexGrossValue("USD", 105.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende06WithSecurityInEUR()
    {
        var security = new Security("T - M o b i l e U S I n c . R e g i s t e r e d S h a r e s D L - , 0 0 0 0 1",
                        "EUR");
        security.setIsin("US8725901040");
        security.setWkn("A1T7LU");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new CommerzbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-13T00:00"), hasShares(120.00), //
                        hasSource("Dividende06.txt"), //
                        hasNote("Ref.-Nr.: 6R3y9052SfF124A7"), //
                        hasAmount("EUR", 96.99), hasGrossValue("EUR", 96.99), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende07()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende07.txt"), errors);

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
                        hasIsin("DE0008475112"), hasWkn("847511"), hasTicker(null), //
                        hasName("A l l i a n z F o n d s J a p a n I n h a b e r - A n t e i l e A ( E U R )"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-05T00:00"), hasShares(500.00), //
                        hasSource("Dividende07.txt"), //
                        hasNote("Ref.-Nr.: 386S4l4q7U3103w0"), //
                        hasAmount("EUR", 713.00), hasGrossValue("EUR", 713.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSteuerbehandlungVonDividende07()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende07.txt"),
                        errors);

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
                        hasIsin("DE0008475112"), hasWkn("847511"), hasTicker(null), //
                        hasName("ALLIANZ FDS JAPAN A (EUR)"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2025-03-05T00:00"), hasShares(500.00), //
                        hasSource("SteuerbehandlungVonDividende07.txt"), //
                        hasNote("Ref.-Nr.: 884A8H8Q6c0848W9"), //
                        hasAmount("EUR", 131.64), hasGrossValue("EUR", 131.64), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende07MitSteuerbehandlungVonDividende07()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende07.txt", "SteuerbehandlungVonDividende07.txt"),
                        errors);

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
                        hasIsin("DE0008475112"), hasWkn("847511"), hasTicker(null), //
                        hasName("A l l i a n z F o n d s J a p a n I n h a b e r - A n t e i l e A ( E U R )"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-05T00:00"), hasShares(500.00), //
                        hasSource("Dividende07.txt; SteuerbehandlungVonDividende07.txt"), //
                        hasNote("Ref.-Nr.: 386S4l4q7U3103w0 | Ref.-Nr.: 884A8H8Q6c0848W9"), //
                        hasAmount("EUR", 581.36), hasGrossValue("EUR", 713.00), //
                        hasTaxes("EUR", 131.64), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende07MitSteuerbehandlungVonDividende07_SourceFilesReversed()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende07.txt", "Dividende07.txt"),
                        errors);

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
                        hasIsin("DE0008475112"), hasWkn("847511"), hasTicker(null), //
                        hasName("ALLIANZ FDS JAPAN A (EUR)"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-05T00:00"), hasShares(500.00), //
                        hasSource("Dividende07.txt; SteuerbehandlungVonDividende07.txt"), //
                        hasNote("Ref.-Nr.: 386S4l4q7U3103w0 | Ref.-Nr.: 884A8H8Q6c0848W9"), //
                        hasAmount("EUR", 581.36), hasGrossValue("EUR", 713.00), //
                        hasTaxes("EUR", 131.64), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende08()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende08.txt"), errors);

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
                        hasIsin("US8725901040"), hasWkn("A1T7LU"), hasTicker(null), //
                        hasName("T - M o b i l e U S I n c . R e g i s t e r e d S h a r e s D L - , 0 0 0 0 1"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-13T00:00"), hasShares(120.00), //
                        hasSource("Dividende08.txt"), //
                        hasNote("Ref.-Nr.: 7i8v1949Lqg792W1"), //
                        hasAmount("EUR", 96.99), hasGrossValue("EUR", 96.99), //
                        hasForexGrossValue("USD", 105.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende08WithSecurityInEUR()
    {
        var security = new Security("T - M o b i l e U S I n c . R e g i s t e r e d S h a r e s D L - , 0 0 0 0 1",
                        "EUR");
        security.setIsin("US8725901040");
        security.setWkn("A1T7LU");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new CommerzbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-13T00:00"), hasShares(120.00), //
                        hasSource("Dividende08.txt"), //
                        hasNote("Ref.-Nr.: 7i8v1949Lqg792W1"), //
                        hasAmount("EUR", 96.99), hasGrossValue("EUR", 96.99), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testSteuerbehandlungVonDividende08()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende08.txt"),
                        errors);

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
                        hasIsin("US8725901040"), hasWkn("A1T7LU"), hasTicker(null), //
                        hasName("T-MOBILE US INC.DL,-00001"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2025-03-13T00:00"), hasShares(120.00), //
                        hasSource("SteuerbehandlungVonDividende08.txt"), //
                        hasNote("Ref.-Nr.: 6j2R5436dOM884a3"), //
                        hasAmount("EUR", 14.55 + 10.23), hasGrossValue("EUR", 14.55 + 10.23), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende08MitSteuerbehandlungVonDividende08()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende08.txt", "SteuerbehandlungVonDividende08.txt"),
                        errors);

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
                        hasIsin("US8725901040"), hasWkn("A1T7LU"), hasTicker(null), //
                        hasName("T - M o b i l e U S I n c . R e g i s t e r e d S h a r e s D L - , 0 0 0 0 1"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-13T00:00"), hasShares(120.00), //
                        hasSource("Dividende08.txt; SteuerbehandlungVonDividende08.txt"), //
                        hasNote("Ref.-Nr.: 7i8v1949Lqg792W1 | Ref.-Nr.: 6j2R5436dOM884a3"), //
                        hasAmount("EUR", 72.21), hasGrossValue("EUR", 96.99), //
                        hasForexGrossValue("USD", 105.60), //
                        hasTaxes("EUR", 14.55 + 10.23), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende08MitSteuerbehandlungVonDividende08WithSecurityInEUR()
    {
        var security = new Security("T - M o b i l e U S I n c . R e g i s t e r e d S h a r e s D L - , 0 0 0 0 1",
                        "EUR");
        security.setIsin("US8725901040");
        security.setWkn("A1T7LU");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new CommerzbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende08.txt", "SteuerbehandlungVonDividende08.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-13T00:00"), hasShares(120.00), //
                        hasSource("Dividende08.txt; SteuerbehandlungVonDividende08.txt"), //
                        hasNote("Ref.-Nr.: 7i8v1949Lqg792W1 | Ref.-Nr.: 6j2R5436dOM884a3"), //
                        hasAmount("EUR", 72.21), hasGrossValue("EUR", 96.99), //
                        hasTaxes("EUR", 14.55 + 10.23), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende08MitSteuerbehandlungVonDividende08_SourceFilesReversed()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende08.txt", "Dividende08.txt"),
                        errors);

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
                        hasIsin("US8725901040"), hasWkn("A1T7LU"), hasTicker(null), //
                        hasName("T-MOBILE US INC.DL,-00001"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-13T00:00"), hasShares(120.00), //
                        hasSource("Dividende08.txt; SteuerbehandlungVonDividende08.txt"), //
                        hasNote("Ref.-Nr.: 7i8v1949Lqg792W1 | Ref.-Nr.: 6j2R5436dOM884a3"), //
                        hasAmount("EUR", 72.21), hasGrossValue("EUR", 96.99), //
                        hasTaxes("EUR", 14.55 + 10.23), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende08MitSteuerbehandlungVonDividende08WithSecurityInEUR_SourceFilesReversed()
    {
        var security = new Security("T-MOBILE US INC.DL,-00001", "EUR");
        security.setIsin("US8725901040");
        security.setWkn("A1T7LU");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new CommerzbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende08.txt", "Dividende08.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-13T00:00"), hasShares(120.00), //
                        hasSource("Dividende08.txt; SteuerbehandlungVonDividende08.txt"), //
                        hasNote("Ref.-Nr.: 7i8v1949Lqg792W1 | Ref.-Nr.: 6j2R5436dOM884a3"), //
                        hasAmount("EUR", 72.21), hasGrossValue("EUR", 96.99), //
                        hasTaxes("EUR", 14.55 + 10.23), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende09()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende09.txt"), errors);

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
                        hasIsin("CH0012005267"), hasWkn("904278"), hasTicker(null), //
                        hasName("N o v a r t i s A G N a m e n s - A k t i e n S F 0 , 4 9"), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-13T00:00"), hasShares(1500.00), //
                        hasSource("Dividende09.txt"), //
                        hasNote("Ref.-Nr.: 0S3m1013fhl684d2"), //
                        hasAmount("EUR", 5465.91), hasGrossValue("EUR", 5465.91), //
                        hasForexGrossValue("CHF", 5250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende09WithSecurityInEUR()
    {
        var security = new Security("N o v a r t i s A G N a m e n s - A k t i e n S F 0 , 4 9", "EUR");
        security.setIsin("CH0012005267");
        security.setWkn("904278");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new CommerzbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-13T00:00"), hasShares(1500.00), //
                        hasSource("Dividende09.txt"), //
                        hasNote("Ref.-Nr.: 0S3m1013fhl684d2"), //
                        hasAmount("EUR", 5465.91), hasGrossValue("EUR", 5465.91), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testSteuerbehandlungVonDividende09()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende09.txt"),
                        errors);

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
                        hasIsin("CH0012005267"), hasWkn("904278"), hasTicker(null), //
                        hasName("NOVARTIS NAM. SF 0,49"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2025-03-13T00:00"), hasShares(1500.00), //
                        hasSource("SteuerbehandlungVonDividende09.txt"), //
                        hasNote("Ref.-Nr.: 0I5t9813ZeI916u2"), //
                        hasAmount("EUR", 2489.71), hasGrossValue("EUR", 2489.71), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende09MitSteuerbehandlungVonDividende09()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende09.txt", "SteuerbehandlungVonDividende09.txt"),
                        errors);

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
                        hasIsin("CH0012005267"), hasWkn("904278"), hasTicker(null), //
                        hasName("N o v a r t i s A G N a m e n s - A k t i e n S F 0 , 4 9"), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-13T00:00"), hasShares(1500.00), //
                        hasSource("Dividende09.txt; SteuerbehandlungVonDividende09.txt"), //
                        hasNote("Ref.-Nr.: 0S3m1013fhl684d2 | Ref.-Nr.: 0I5t9813ZeI916u2"), //
                        hasAmount("EUR", 2976.19), hasGrossValue("EUR", 5465.90), //
                        hasForexGrossValue("CHF", 5250.00), //
                        hasTaxes("EUR", 2489.71), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende09MitSteuerbehandlungVonDividende09WithSecurityInEUR()
    {
        var security = new Security("N o v a r t i s A G N a m e n s - A k t i e n S F 0 , 4 9", "EUR");
        security.setIsin("CH0012005267");
        security.setWkn("904278");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new CommerzbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende09.txt", "SteuerbehandlungVonDividende09.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-13T00:00"), hasShares(1500.00), //
                        hasSource("Dividende09.txt; SteuerbehandlungVonDividende09.txt"), //
                        hasNote("Ref.-Nr.: 0S3m1013fhl684d2 | Ref.-Nr.: 0I5t9813ZeI916u2"), //
                        hasAmount("EUR", 2976.19), hasGrossValue("EUR", 5465.90), //
                        hasTaxes("EUR", 2489.71), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende09MitSteuerbehandlungVonDividende09_SourceFilesReversed()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende09.txt", "Dividende09.txt"),
                        errors);

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
                        hasIsin("CH0012005267"), hasWkn("904278"), hasTicker(null), //
                        hasName("NOVARTIS NAM. SF 0,49"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-13T00:00"), hasShares(1500.00), //
                        hasSource("Dividende09.txt; SteuerbehandlungVonDividende09.txt"), //
                        hasNote("Ref.-Nr.: 0S3m1013fhl684d2 | Ref.-Nr.: 0I5t9813ZeI916u2"), //
                        hasAmount("EUR", 2976.19), hasGrossValue("EUR", 5465.90), //
                        hasTaxes("EUR", 2489.71), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende09MitSteuerbehandlungVonDividende09WithSecurityInEUR_SourceFilesReversed()
    {
        var security = new Security("NOVARTIS NAM. SF 0,49", "EUR");
        security.setIsin("CH0012005267");
        security.setWkn("904278");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new CommerzbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende09.txt", "Dividende09.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-13T00:00"), hasShares(1500.00), //
                        hasSource("Dividende09.txt; SteuerbehandlungVonDividende09.txt"), //
                        hasNote("Ref.-Nr.: 0S3m1013fhl684d2 | Ref.-Nr.: 0I5t9813ZeI916u2"), //
                        hasAmount("EUR", 2976.19), hasGrossValue("EUR", 5465.90), //
                        hasTaxes("EUR", 2489.71), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende10()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende10.txt"), errors);

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
                        hasIsin("US8545021011"), hasWkn("A1CTQA"), hasTicker(null), //
                        hasName("S t a n l e y B l a c k & D e c k e r I n c . R e g i s t e r e d S h a r e s D L 2 , 5 0"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-18T00:00"), hasShares(100.00), //
                        hasSource("Dividende10.txt"), //
                        hasNote("Ref.-Nr.: 2x6f3IVQBDu6967C"), //
                        hasAmount("EUR", 74.92), hasGrossValue("EUR", 74.92), //
                        hasForexGrossValue("USD", 82.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende10WithSecurityInEUR()
    {
        var security = new Security(
                        "S t a n l e y B l a c k & D e c k e r I n c . R e g i s t e r e d S h a r e s D L 2 , 5 0",
                        "EUR");
        security.setIsin("US8545021011");
        security.setWkn("A1CTQA");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new CommerzbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende10.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-18T00:00"), hasShares(100.00), //
                        hasSource("Dividende10.txt"), //
                        hasNote("Ref.-Nr.: 2x6f3IVQBDu6967C"), //
                        hasAmount("EUR", 74.92), hasGrossValue("EUR", 74.92), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testSteuerbehandlungVonDividende10()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende10.txt"),
                        errors);

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
                        hasIsin("US8545021011"), hasWkn("A1CTQA"), hasTicker(null), //
                        hasName("STANLEY BL. + DECK.DL2,50"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2025-03-18T00:00"), hasShares(100.00), //
                        hasSource("SteuerbehandlungVonDividende10.txt"), //
                        hasNote("Ref.-Nr.: 2I7Z9FWTCFT0000R"), //
                        hasAmount("EUR", 19.13), hasGrossValue("EUR", 19.13), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende10MitSteuerbehandlungVonDividende10()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende10.txt", "SteuerbehandlungVonDividende10.txt"),
                        errors);

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
                        hasIsin("US8545021011"), hasWkn("A1CTQA"), hasTicker(null), //
                        hasName("S t a n l e y B l a c k & D e c k e r I n c . R e g i s t e r e d S h a r e s D L 2 , 5 0"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-18T00:00"), hasShares(100.00), //
                        hasSource("Dividende10.txt; SteuerbehandlungVonDividende10.txt"), //
                        hasNote("Ref.-Nr.: 2x6f3IVQBDu6967C | Ref.-Nr.: 2I7Z9FWTCFT0000R"), //
                        hasAmount("EUR", 55.78), hasGrossValue("EUR", 74.91), //
                        hasForexGrossValue("USD", 82.00), //
                        hasTaxes("EUR", 19.13), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende10MitSteuerbehandlungVonDividende10WithSecurityInEUR()
    {
        var security = new Security(
                        "S t a n l e y B l a c k & D e c k e r I n c . R e g i s t e r e d S h a r e s D L 2 , 5 0",
                        "EUR");
        security.setIsin("US8545021011");
        security.setWkn("A1CTQA");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new CommerzbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende10.txt", "SteuerbehandlungVonDividende10.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-18T00:00"), hasShares(100.00), //
                        hasSource("Dividende10.txt; SteuerbehandlungVonDividende10.txt"), //
                        hasNote("Ref.-Nr.: 2x6f3IVQBDu6967C | Ref.-Nr.: 2I7Z9FWTCFT0000R"), //
                        hasAmount("EUR", 55.78), hasGrossValue("EUR", 74.91), //
                        hasTaxes("EUR", 19.13), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende10MitSteuerbehandlungVonDividende10_SourceFilesReversed()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende10.txt", "Dividende10.txt"),
                        errors);

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
                        hasIsin("US8545021011"), hasWkn("A1CTQA"), hasTicker(null), //
                        hasName("STANLEY BL. + DECK.DL2,50"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-18T00:00"), hasShares(100.00), //
                        hasSource("Dividende10.txt; SteuerbehandlungVonDividende10.txt"), //
                        hasNote("Ref.-Nr.: 2x6f3IVQBDu6967C | Ref.-Nr.: 2I7Z9FWTCFT0000R"), //
                        hasAmount("EUR", 55.78), hasGrossValue("EUR", 74.91), //
                        hasTaxes("EUR", 19.13), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende10MitSteuerbehandlungVonDividende10WithSecurityInEUR_SourceFilesReversed()
    {
        var security = new Security("STANLEY BL. + DECK.DL2,50", "EUR");
        security.setIsin("US8545021011");
        security.setWkn("A1CTQA");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new CommerzbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende10.txt", "Dividende10.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-18T00:00"), hasShares(100.00), //
                        hasSource("Dividende10.txt; SteuerbehandlungVonDividende10.txt"), //
                        hasNote("Ref.-Nr.: 2x6f3IVQBDu6967C | Ref.-Nr.: 2I7Z9FWTCFT0000R"), //
                        hasAmount("EUR", 55.78), hasGrossValue("EUR", 74.91), //
                        hasTaxes("EUR", 19.13), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende11()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende11.txt"), errors);

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
                        hasIsin("CH0038863350"), hasWkn("A0Q4DC"), hasTicker(null), //
                        hasName("N e s t l é S . A . N a m e n s - A k t i e n S F - , 1 0"), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-04-24T00:00"), hasShares(126.00), //
                        hasSource("Dividende11.txt"), //
                        hasNote("Ref.-Nr.: 1C7ZBW0NK88007D4"), //
                        hasAmount("EUR", 407.92), hasGrossValue("EUR", 407.92), //
                        hasForexGrossValue("CHF", 384.30), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende11WithSecurityInEUR()
    {
        var security = new Security("N e s t l é S . A . N a m e n s - A k t i e n S F - , 1 0", "EUR");
        security.setIsin("CH0038863350");
        security.setWkn("A0Q4DC");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new CommerzbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende11.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-04-24T00:00"), hasShares(126.00), //
                        hasSource("Dividende11.txt"), //
                        hasNote("Ref.-Nr.: 1C7ZBW0NK88007D4"), //
                        hasAmount("EUR", 407.92), hasGrossValue("EUR", 407.92), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testSteuerbehandlungVonDividende11()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende11.txt"),
                        errors);

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
                        hasIsin("CH0038863350"), hasWkn("A0Q4DC"), hasTicker(null), //
                        hasName("NESTLE NAM. SF-,10"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2025-04-24T00:00"), hasShares(126.00), //
                        hasSource("SteuerbehandlungVonDividende11.txt"), //
                        hasNote("Ref.-Nr.: 1C7ZBW0NQ14714E9"), //
                        hasAmount("EUR", 185.81), hasGrossValue("EUR", 185.81), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende11MitSteuerbehandlungVonDividende11()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende11.txt", "SteuerbehandlungVonDividende11.txt"),
                        errors);

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
                        hasIsin("CH0038863350"), hasWkn("A0Q4DC"), hasTicker(null), //
                        hasName("N e s t l é S . A . N a m e n s - A k t i e n S F - , 1 0"), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-04-24T00:00"), hasShares(126.00), //
                        hasSource("Dividende11.txt; SteuerbehandlungVonDividende11.txt"), //
                        hasNote("Ref.-Nr.: 1C7ZBW0NK88007D4 | Ref.-Nr.: 1C7ZBW0NQ14714E9"), //
                        hasAmount("EUR", 222.11), hasGrossValue("EUR", 407.92), //
                        hasForexGrossValue("CHF", 384.30), //
                        hasTaxes("EUR", 185.81), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende11MitSteuerbehandlungVonDividende11WithSecurityInEUR()
    {
        var security = new Security("N e s t l é S . A . N a m e n s - A k t i e n S F - , 1 0", "EUR");
        security.setIsin("CH0038863350");
        security.setWkn("A0Q4DC");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new CommerzbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende11.txt", "SteuerbehandlungVonDividende11.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-04-24T00:00"), hasShares(126.00), //
                        hasSource("Dividende11.txt; SteuerbehandlungVonDividende11.txt"), //
                        hasNote("Ref.-Nr.: 1C7ZBW0NK88007D4 | Ref.-Nr.: 1C7ZBW0NQ14714E9"), //
                        hasAmount("EUR", 222.11), hasGrossValue("EUR", 407.92), //
                        hasTaxes("EUR", 185.81), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende11MitSteuerbehandlungVonDividende11_SourceFilesReversed()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende11.txt", "Dividende11.txt"),
                        errors);

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
                        hasIsin("CH0038863350"), hasWkn("A0Q4DC"), hasTicker(null), //
                        hasName("NESTLE NAM. SF-,10"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-04-24T00:00"), hasShares(126.00), //
                        hasSource("Dividende11.txt; SteuerbehandlungVonDividende11.txt"), //
                        hasNote("Ref.-Nr.: 1C7ZBW0NK88007D4 | Ref.-Nr.: 1C7ZBW0NQ14714E9"), //
                        hasAmount("EUR", 222.11), hasGrossValue("EUR", 407.92), //
                        hasTaxes("EUR", 185.81), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende11MitSteuerbehandlungVonDividende11WithSecurityInEUR_SourceFilesReversed()
    {
        var security = new Security("NESTLE NAM. SF-,10", "EUR");
        security.setIsin("CH0038863350");
        security.setWkn("A0Q4DC");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new CommerzbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende11.txt", "Dividende11.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-04-24T00:00"), hasShares(126.00), //
                        hasSource("Dividende11.txt; SteuerbehandlungVonDividende11.txt"), //
                        hasNote("Ref.-Nr.: 1C7ZBW0NK88007D4 | Ref.-Nr.: 1C7ZBW0NQ14714E9"), //
                        hasAmount("EUR", 222.11), hasGrossValue("EUR", 407.92), //
                        hasTaxes("EUR", 185.81), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testSteuermitteilungOhneDividende01()
    {
        var extractor = new CommerzbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SteuermitteilungOhneDividende01.txt"),
                        errors);

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
                        hasIsin("US7960502018"), hasWkn("881823"), hasTicker(null), //
                        hasName("SAMSUNG EL./25 GDRS NV PF"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2020-05-26T00:00"), hasShares(12.00), //
                        hasSource("SteuermitteilungOhneDividende01.txt"), //
                        hasNote("Ref.-Nr.: 12345ABCDE12345X"), //
                        hasAmount("EUR", 25.89), hasGrossValue("EUR", 25.89), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }
}
