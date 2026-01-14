package name.abuchen.portfolio.datatransfer.pdf.targobank;

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
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.TargobankPDFExtractor;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class TargobankPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new TargobankPDFExtractor(new Client());

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
                        hasIsin("DE0000ABC123"), hasWkn("ABC123"), hasTicker(null), //
                        hasName("FanCy shaRe. nAmE X0-X0"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-01-02T13:01:00"), hasShares(987.654), //
                        hasSource("Kauf01.txt"), //
                        hasNote("R.-Nr.: BOE-2020-0223620085-0000068 | Ref.-Nr.: 555666777888"), //
                        hasAmount("EUR", 1008.91), hasGrossValue("EUR", 1000.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 8.90))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var extractor = new TargobankPDFExtractor(new Client());

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
                        hasIsin("DE0000ABC123"), hasWkn("ABC123"), hasTicker(null), //
                        hasName("Muster AG"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-09-21T19:27:00"), hasShares(1710.00), //
                        hasSource("Kauf02.txt"), //
                        hasNote("R.-Nr.: NUMMER | Ref.-Nr.: NUMMER"), //
                        hasAmount("EUR", 1187.94), hasGrossValue("EUR", 1187.94), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var extractor = new TargobankPDFExtractor(new Client());

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
                        hasIsin("LU0000ZYX987"), hasWkn("ZYX987"), hasTicker(null), //
                        hasName("an0tHer vERy FNcY NaMe"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-01-10T00:00"), hasShares(10.00), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("R.-Nr.: BGH-2020-0223620085-0000068 | Ref.-Nr.: 0291235DH0293422"), //
                        hasAmount("EUR", 1239.00), hasGrossValue("EUR", 1239.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        var extractor = new TargobankPDFExtractor(new Client());

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
                        hasIsin("SE0006425815"), hasWkn("A14TK6"), hasTicker(null), //
                        hasName("PowerCell Sweden AB (publ) - Namn-Aktier SK-,022"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-05-26T20:32:00"), hasShares(300.00), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("R.-Nr.: BOE-2020-0223620168-0003824 | Ref.-Nr.: 2005262032215246"), //
                        hasAmount("EUR", 7439.35), hasGrossValue("EUR", 7458.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 18.65))));
    }

    @Test
    public void testSteuerbehandlungVonVerkauf02()
    {
        var extractor = new TargobankPDFExtractor(new Client());

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
                        hasIsin("SE0006425815"), hasWkn("A14TK6"), hasTicker(null), //
                        hasName("PowerCell Sweden AB (publ) - Namn-Aktier SK-,022"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2020-05-26T20:32:00"), hasShares(300.00), //
                        hasSource("SteuerbehandlungVonVerkauf02.txt"), //
                        hasNote("Tr.-Nr.: TBK14720B024746O001"), //
                        hasAmount("EUR", 823.76), hasGrossValue("EUR", 823.76), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testVerkauf02MitSteuerbehandlungVonVerkauf02()
    {
        var extractor = new TargobankPDFExtractor(new Client());

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
                        hasIsin("SE0006425815"), hasWkn("A14TK6"), hasTicker(null), //
                        hasName("PowerCell Sweden AB (publ) - Namn-Aktier SK-,022"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-05-26T20:32:00"), hasShares(300.00), //
                        hasSource("Verkauf02.txt; SteuerbehandlungVonVerkauf02.txt"), //
                        hasNote("R.-Nr.: BOE-2020-0223620168-0003824 | Ref.-Nr.: 2005262032215246 | Tr.-Nr.: TBK14720B024746O001"), //
                        hasAmount("EUR", 6615.59), hasGrossValue("EUR", 7458.00), //
                        hasTaxes("EUR", 823.76), hasFees("EUR", 18.65))));
    }

    @Test
    public void testVerkauf02MitSteuerbehandlungVonVerkauf02_SourceFilesReversed()
    {
        var extractor = new TargobankPDFExtractor(new Client());

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
                        hasIsin("SE0006425815"), hasWkn("A14TK6"), hasTicker(null), //
                        hasName("PowerCell Sweden AB (publ) - Namn-Aktier SK-,022"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-05-26T20:32:00"), hasShares(300.00), //
                        hasSource("Verkauf02.txt; SteuerbehandlungVonVerkauf02.txt"), //
                        hasNote("R.-Nr.: BOE-2020-0223620168-0003824 | Ref.-Nr.: 2005262032215246 | Tr.-Nr.: TBK14720B024746O001"), //
                        hasAmount("EUR", 6615.59), hasGrossValue("EUR", 7458.00), //
                        hasTaxes("EUR", 823.76), hasFees("EUR", 18.65))));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        var extractor = new TargobankPDFExtractor(new Client());

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
                        hasIsin("DE000UH3GTU5"), hasWkn("UH3GTU"), hasTicker(null), //
                        hasName("UBS AG (London Branch) - FaktS O.End DJIA 34446,1281"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-01-03T12:04:02"), hasShares(4000.00), //
                        hasSource("Verkauf03.txt"), //
                        hasNote("R.-Nr.: BOE-2023-0223620153-0000031 | Ref.-Nr.: 9301031204028497"), //
                        hasAmount("EUR", 0.01), hasGrossValue("EUR", 4.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.99))));
    }

    @Test
    public void testSteuerbehandlungVonVerkauf03()
    {
        var extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonVerkauf03.txt"),
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
                        hasIsin("DE000UH3GTU5"), hasWkn("UH3GTU"), hasTicker(null), //
                        hasName("UBS AG (London Branch) - FaktS O.End DJIA 34446,1281"), //
                        hasCurrencyCode("EUR"))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2023-01-03T12:04:02"), hasShares(4000.00), //
                                        hasSource("SteuerbehandlungVonVerkauf03.txt"), //
                                        hasNote("Tr.-Nr.: TBK00323B007292O001"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testVerkauf03MitSteuerbehandlungVonVerkauf03()
    {
        var extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Verkauf03.txt", "SteuerbehandlungVonVerkauf03.txt"),
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
                        hasIsin("DE000UH3GTU5"), hasWkn("UH3GTU"), hasTicker(null), //
                        hasName("UBS AG (London Branch) - FaktS O.End DJIA 34446,1281"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-01-03T12:04:02"), hasShares(4000.00), //
                        hasSource("Verkauf03.txt; SteuerbehandlungVonVerkauf03.txt"), //
                        hasNote("R.-Nr.: BOE-2023-0223620153-0000031 | Ref.-Nr.: 9301031204028497 | Tr.-Nr.: TBK00323B007292O001"), //
                        hasAmount("EUR", 0.01), hasGrossValue("EUR", 4.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.99))));
    }

    @Test
    public void testVerkauf03MitSteuerbehandlungVonVerkauf03_SourceFilesReversed()
    {
        var extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonVerkauf03.txt", "Verkauf03.txt"),
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
                        hasIsin("DE000UH3GTU5"), hasWkn("UH3GTU"), hasTicker(null), //
                        hasName("UBS AG (London Branch) - FaktS O.End DJIA 34446,1281"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-01-03T12:04:02"), hasShares(4000.00), //
                        hasSource("Verkauf03.txt; SteuerbehandlungVonVerkauf03.txt"), //
                        hasNote("R.-Nr.: BOE-2023-0223620153-0000031 | Ref.-Nr.: 9301031204028497 | Tr.-Nr.: TBK00323B007292O001"), //
                        hasAmount("EUR", 0.01), hasGrossValue("EUR", 4.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.99))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new TargobankPDFExtractor(new Client());

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
                        hasIsin("IE00BKX55T58"), hasWkn("A12CX1"), hasTicker(null), //
                        hasName("Vang.FTSE Develop.World U.ETF - Registered Shares USD Dis.oN"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-24T00:00"), hasShares(81.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote("R.-Nr.: CPS-2020-0123456789-0001234"), //
                        hasAmount("EUR", 21.18), hasGrossValue("EUR", 21.18), //
                        hasForexGrossValue("USD", 23.77), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende01WithSecurityInEUR()
    {
        var security = new Security("Vang.FTSE Develop.World U.ETF - Registered Shares USD Dis.oN", "EUR");
        security.setIsin("IE00BKX55T58");
        security.setWkn("A12CX1");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TargobankPDFExtractor(client);

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
                        hasDate("2020-06-24T00:00"), hasShares(81.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote("R.-Nr.: CPS-2020-0123456789-0001234"), //
                        hasAmount("EUR", 21.18), hasGrossValue("EUR", 21.18), //
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
    public void testSteuerbehandlungVonDividende01()
    {
        var extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende01.txt"),
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
                        hasIsin("IE00BKX55T58"), hasWkn("A12CX1"), hasTicker(null), //
                        hasName("Vang.FTSE Develop.World U.ETF - Registered Shares USD Dis.oN"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2020-06-24T00:00"), hasShares(81.00), //
                        hasSource("SteuerbehandlungVonDividende01.txt"), //
                        hasNote("Tr.-Nr.: INDTBK1234567890"), //
                        hasAmount("EUR", 5.59), hasGrossValue("EUR", 5.59), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende01MitSteuerbehandlungVonDividende01()
    {
        var extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende01.txt", "SteuerbehandlungVonDividende01.txt"),
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
                        hasIsin("IE00BKX55T58"), hasWkn("A12CX1"), hasTicker(null), //
                        hasName("Vang.FTSE Develop.World U.ETF - Registered Shares USD Dis.oN"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-24T00:00"), hasShares(81.00), //
                        hasSource("Dividende01.txt; SteuerbehandlungVonDividende01.txt"), //
                        hasNote("R.-Nr.: CPS-2020-0123456789-0001234 | Tr.-Nr.: INDTBK1234567890"), //
                        hasAmount("EUR", 15.59), hasGrossValue("EUR", 21.18), //
                        hasForexGrossValue("USD", 23.77), //
                        hasTaxes("EUR", 5.59), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende01MitSteuerbehandlungVonDividende01WithSecurityInEUR()
    {
        var security = new Security("Vang.FTSE Develop.World U.ETF - Registered Shares USD Dis.oN", "EUR");
        security.setIsin("IE00BKX55T58");
        security.setWkn("A12CX1");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TargobankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende01.txt", "Dividende01.txt"),
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
                        hasDate("2020-06-24T00:00"), hasShares(81.00), //
                        hasSource("Dividende01.txt; SteuerbehandlungVonDividende01.txt"), //
                        hasNote("R.-Nr.: CPS-2020-0123456789-0001234 | Tr.-Nr.: INDTBK1234567890"), //
                        hasAmount("EUR", 15.59), hasGrossValue("EUR", 21.18), //
                        hasTaxes("EUR", 5.59), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende01MitSteuerbehandlungVonDividende01_SourceFilesReversed()
    {
        var extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende01.txt", "Dividende01.txt"),
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
                        hasIsin("IE00BKX55T58"), hasWkn("A12CX1"), hasTicker(null), //
                        hasName("Vang.FTSE Develop.World U.ETF - Registered Shares USD Dis.oN"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-24T00:00"), hasShares(81.00), //
                        hasSource("Dividende01.txt; SteuerbehandlungVonDividende01.txt"), //
                        hasNote("R.-Nr.: CPS-2020-0123456789-0001234 | Tr.-Nr.: INDTBK1234567890"), //
                        hasAmount("EUR", 15.59), hasGrossValue("EUR", 21.18), //
                        hasTaxes("EUR", 5.59), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende01MitSteuerbehandlungVonDividende01WithSecurityInEUR_SourceFilesReversed()
    {
        var security = new Security("Vang.FTSE Develop.World U.ETF - Registered Shares USD Dis.oN", "EUR");
        security.setIsin("IE00BKX55T58");
        security.setWkn("A12CX1");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TargobankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende01.txt", "SteuerbehandlungVonDividende01.txt"),
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
                        hasDate("2020-06-24T00:00"), hasShares(81.00), //
                        hasSource("Dividende01.txt; SteuerbehandlungVonDividende01.txt"), //
                        hasNote("R.-Nr.: CPS-2020-0123456789-0001234 | Tr.-Nr.: INDTBK1234567890"), //
                        hasAmount("EUR", 15.59), hasGrossValue("EUR", 21.18), //
                        hasTaxes("EUR", 5.59), hasFees("EUR", 0.00), //
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
        var extractor = new TargobankPDFExtractor(new Client());

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
                        hasIsin("IE00BKX55S42"), hasWkn("A12CXZ"), hasTicker(null), //
                        hasName("Vang.FTSE Dev.Eur.ex UK U.ETF - Registered Shares EUR Dis. o"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-24T00:00"), hasShares(61.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote("R.-Nr.: CPS-2020-0123456789-0001234"), //
                        hasAmount("EUR", 15.29), hasGrossValue("EUR", 15.29), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSteuerbehandlungVonDividende02()
    {
        var extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende02.txt"),
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
                        hasIsin("IE00BKX55S42"), hasWkn("A12CXZ"), hasTicker(null), //
                        hasName("Vang.FTSE Dev.Eur.ex UK U.ETF - Registered Shares EUR Dis. o"), //
                        hasCurrencyCode("EUR"))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2020-06-11T00:00"), hasShares(61.00), //
                                        hasSource("SteuerbehandlungVonDividende02.txt"), //
                                        hasNote("Tr.-Nr.: INDTBK1234567890"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testDividende02MitSteuerbehandlungVonDividende02()
    {
        var extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende02.txt", "SteuerbehandlungVonDividende02.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BKX55S42"), hasWkn("A12CXZ"), hasTicker(null), //
                        hasName("Vang.FTSE Dev.Eur.ex UK U.ETF - Registered Shares EUR Dis. o"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-24T00:00"), hasShares(61.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote("R.-Nr.: CPS-2020-0123456789-0001234"), //
                        hasAmount("EUR", 15.29), hasGrossValue("EUR", 15.29), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2020-06-11T00:00"), hasShares(61.00), //
                                        hasSource("SteuerbehandlungVonDividende02.txt"), //
                                        hasNote("Tr.-Nr.: INDTBK1234567890"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testDividende02MitSteuerbehandlungVonDividende02_SourceFilesReversed()
    {
        var extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende02.txt", "Dividende02.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BKX55S42"), hasWkn("A12CXZ"), hasTicker(null), //
                        hasName("Vang.FTSE Dev.Eur.ex UK U.ETF - Registered Shares EUR Dis. o"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-24T00:00"), hasShares(61.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote("R.-Nr.: CPS-2020-0123456789-0001234"), //
                        hasAmount("EUR", 15.29), hasGrossValue("EUR", 15.29), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2020-06-11T00:00"), hasShares(61.00), //
                                        hasSource("SteuerbehandlungVonDividende02.txt"), //
                                        hasNote("Tr.-Nr.: INDTBK1234567890"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testDividende03()
    {
        var extractor = new TargobankPDFExtractor(new Client());

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
                        hasIsin("LU0875160326"), hasWkn("DBX0NK"), hasTicker(null), //
                        hasName("Xtrackers Harvest CSI300 - Inhaber-Anteile 1D o.N."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-04-27T00:00"), hasShares(1700.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote("R.-Nr.: CPS-2020-0223620168-0001148"), //
                        hasAmount("EUR", 279.64), hasGrossValue("EUR", 279.64), //
                        hasForexGrossValue("USD", 304.81), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende03WithSecurityInEUR()
    {
        var security = new Security("Xtrackers Harvest CSI300 - Inhaber-Anteile 1D o.N.", "EUR");
        security.setIsin("LU0875160326");
        security.setWkn("DBX0NK");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TargobankPDFExtractor(client);

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
                        hasDate("2020-04-27T00:00"), hasShares(1700.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote("R.-Nr.: CPS-2020-0223620168-0001148"), //
                        hasAmount("EUR", 279.64), hasGrossValue("EUR", 279.64), //
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
    public void testSteuerbehandlungVonDividende03()
    {
        var extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende03.txt"),
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
                        hasIsin("LU0875160326"), hasWkn("DBX0NK"), hasTicker(null), //
                        hasName("Xtrackers Harvest CSI300 - Inhaber-Anteile 1D o.N."), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2020-04-27T00:00"), hasShares(1700.00), //
                        hasSource("SteuerbehandlungVonDividende03.txt"), //
                        hasNote("Tr.-Nr.: INDTBK12120CG000130O00"), //
                        hasAmount("EUR", 51.63), hasGrossValue("EUR", 51.63), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende03MitSteuerbehandlungVonDividende03()
    {
        var extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende03.txt", "SteuerbehandlungVonDividende03.txt"),
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
                        hasIsin("LU0875160326"), hasWkn("DBX0NK"), hasTicker(null), //
                        hasName("Xtrackers Harvest CSI300 - Inhaber-Anteile 1D o.N."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-04-27T00:00"), hasShares(1700.00), //
                        hasSource("Dividende03.txt; SteuerbehandlungVonDividende03.txt"), //
                        hasNote("R.-Nr.: CPS-2020-0223620168-0001148 | Tr.-Nr.: INDTBK12120CG000130O00"), //
                        hasAmount("EUR", 228.01), hasGrossValue("EUR", 279.64), //
                        hasForexGrossValue("USD", 304.81), //
                        hasTaxes("EUR", 51.63), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende03MitSteuerbehandlungVonDividende03WithSecurityInEUR()
    {
        var security = new Security("Xtrackers Harvest CSI300 - Inhaber-Anteile 1D o.N.", "EUR");
        security.setIsin("LU0875160326");
        security.setWkn("DBX0NK");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TargobankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende03.txt", "Dividende03.txt"),
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
                        hasDate("2020-04-27T00:00"), hasShares(1700.00), //
                        hasSource("Dividende03.txt; SteuerbehandlungVonDividende03.txt"), //
                        hasNote("R.-Nr.: CPS-2020-0223620168-0001148 | Tr.-Nr.: INDTBK12120CG000130O00"), //
                        hasAmount("EUR", 228.01), hasGrossValue("EUR", 279.64), //
                        hasTaxes("EUR", 51.63), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende03MitSteuerbehandlungVonDividende03_SourceFilesReversed()
    {
        var extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende03.txt", "Dividende03.txt"),
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
                        hasIsin("LU0875160326"), hasWkn("DBX0NK"), hasTicker(null), //
                        hasName("Xtrackers Harvest CSI300 - Inhaber-Anteile 1D o.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-04-27T00:00"), hasShares(1700.00), //
                        hasSource("Dividende03.txt; SteuerbehandlungVonDividende03.txt"), //
                        hasNote("R.-Nr.: CPS-2020-0223620168-0001148 | Tr.-Nr.: INDTBK12120CG000130O00"), //
                        hasAmount("EUR", 228.01), hasGrossValue("EUR", 279.64), //
                        hasTaxes("EUR", 51.63), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende03MitSteuerbehandlungVonDividende03WithSecurityInEUR_SourceFilesReversed()
    {
        var security = new Security("Xtrackers Harvest CSI300 - Inhaber-Anteile 1D o.N.", "EUR");
        security.setIsin("LU0875160326");
        security.setWkn("DBX0NK");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TargobankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende03.txt", "SteuerbehandlungVonDividende03.txt"),
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
                        hasDate("2020-04-27T00:00"), hasShares(1700.00), //
                        hasSource("Dividende03.txt; SteuerbehandlungVonDividende03.txt"), //
                        hasNote("R.-Nr.: CPS-2020-0223620168-0001148 | Tr.-Nr.: INDTBK12120CG000130O00"), //
                        hasAmount("EUR", 228.01), hasGrossValue("EUR", 279.64), //
                        hasTaxes("EUR", 51.63), hasFees("EUR", 0.00), //
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
        var extractor = new TargobankPDFExtractor(new Client());

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
                        hasIsin("DE0123456789"), hasWkn("ABC0DE"), hasTicker(null), //
                        hasName("Aktiengesellschaft AG"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-07-10T00:00"), hasShares(1790.00), //
                        hasSource("Dividende04.txt"), //
                        hasNote("R.-Nr.: NUMMER"), //
                        hasAmount("EUR", 17.90), hasGrossValue("EUR", 17.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSteuerbehandlungVonDividende04()
    {
        var extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende04.txt"),
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
                        hasIsin("DE0123456789"), hasWkn("ABC0DE"), hasTicker(null), //
                        hasName("Aktiengesellschaft AG"), //
                        hasCurrencyCode("EUR"))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2020-07-08T00:00"), hasShares(1790.00), //
                                        hasSource("SteuerbehandlungVonDividende04.txt"), //
                                        hasNote("Tr.-Nr.: NUMMER"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testDividende04MitSteuerbehandlungVonDividende04()
    {
        var extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende04.txt", "SteuerbehandlungVonDividende04.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0123456789"), hasWkn("ABC0DE"), hasTicker(null), //
                        hasName("Aktiengesellschaft AG"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-07-10T00:00"), hasShares(1790.00), //
                        hasSource("Dividende04.txt"), //
                        hasNote("R.-Nr.: NUMMER"), //
                        hasAmount("EUR", 17.90), hasGrossValue("EUR", 17.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2020-07-08T00:00"), hasShares(1790.00), //
                                        hasSource("SteuerbehandlungVonDividende04.txt"), //
                                        hasNote("Tr.-Nr.: NUMMER"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testDividende04MitSteuerbehandlungVonDividende04_SourceFilesReversed()
    {
        var extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende04.txt", "Dividende04.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0123456789"), hasWkn("ABC0DE"), hasTicker(null), //
                        hasName("Aktiengesellschaft AG"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-07-10T00:00"), hasShares(1790.00), //
                        hasSource("Dividende04.txt"), //
                        hasNote("R.-Nr.: NUMMER"), //
                        hasAmount("EUR", 17.90), hasGrossValue("EUR", 17.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2020-07-08T00:00"), hasShares(1790.00), //
                                        hasSource("SteuerbehandlungVonDividende04.txt"), //
                                        hasNote("Tr.-Nr.: NUMMER"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testDividende05()
    {
        var extractor = new TargobankPDFExtractor(new Client());

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
                        hasIsin("DE0123456789"), hasWkn("ABC0DE"), hasTicker(null), //
                        hasName("Aktiengesellschaft AG"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-08-31T00:00"), hasShares(235.00), //
                        hasSource("Dividende05.txt"), //
                        hasNote("R.-Nr.: NUMMER"), //
                        hasAmount("EUR", 20.82 + 3.67), hasGrossValue("EUR", 20.82 + 3.67), //
                        hasForexGrossValue("USD", 29.41), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende05WithSecurityInEUR()
    {
        var security = new Security("Aktiengesellschaft AG", "EUR");
        security.setIsin("DE0123456789");
        security.setWkn("ABC0DE");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TargobankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende05.txt"), errors);

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
                        hasDate("2020-08-31T00:00"), hasShares(235.00), //
                        hasSource("Dividende05.txt"), //
                        hasNote("R.-Nr.: NUMMER"), //
                        hasAmount("EUR", 20.82 + 3.67), hasGrossValue("EUR", 20.82 + 3.67), //
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
    public void testSteuerbehandlungVonDividende05()
    {
        var extractor = new TargobankPDFExtractor(new Client());

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
                        hasIsin("DE0123456789"), hasWkn("ABC0DE"), hasTicker(null), //
                        hasName("Aktiengesellschaft AG"), //
                        hasCurrencyCode("EUR"))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2020-08-21T00:00"), hasShares(235.00), //
                                        hasSource("SteuerbehandlungVonDividende05.txt"), //
                                        hasNote("Tr.-Nr.: NUMMER"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testDividende05MitSteuerbehandlungVonDividende05()
    {
        var extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende05.txt", "SteuerbehandlungVonDividende05.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0123456789"), hasWkn("ABC0DE"), hasTicker(null), //
                        hasName("Aktiengesellschaft AG"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-08-31T00:00"), hasShares(235.00), //
                        hasSource("Dividende05.txt"), //
                        hasNote("R.-Nr.: NUMMER"), //
                        hasAmount("EUR", 20.82 + 3.67), hasGrossValue("EUR", 20.82 + 3.67), //
                        hasForexGrossValue("USD", 29.41), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2020-08-21T00:00"), hasShares(235.00), //
                                        hasSource("SteuerbehandlungVonDividende05.txt"), //
                                        hasNote("Tr.-Nr.: NUMMER"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testDividende05MitSteuerbehandlungVonDividende05WithSecurityInEUR()
    {
        var security = new Security("Aktiengesellschaft AG", "EUR");
        security.setIsin("DE0123456789");
        security.setWkn("ABC0DE");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TargobankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende05.txt", "Dividende05.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-08-31T00:00"), hasShares(235.00), //
                        hasSource("Dividende05.txt"), //
                        hasNote("R.-Nr.: NUMMER"), //
                        hasAmount("EUR", 20.82 + 3.67), hasGrossValue("EUR", 20.82 + 3.67), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2020-08-21T00:00"), hasShares(235.00), //
                                        hasSource("SteuerbehandlungVonDividende05.txt"), //
                                        hasNote("Tr.-Nr.: NUMMER"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                                        check(tx -> {
                                            var c = new CheckCurrenciesAction();
                                            var account = new Account();
                                            account.setCurrencyCode("EUR");
                                            var s = c.process((AccountTransaction) tx, account);
                                            assertThat(s, is(Status.OK_STATUS));
                                        })))));
    }

    @Test
    public void testDividende05MitSteuerbehandlungVonDividende05_SourceFilesReversed()
    {
        var extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende05.txt", "Dividende05.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0123456789"), hasWkn("ABC0DE"), hasTicker(null), //
                        hasName("Aktiengesellschaft AG"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-08-31T00:00"), hasShares(235.00), //
                        hasSource("Dividende05.txt"), //
                        hasNote("R.-Nr.: NUMMER"), //
                        hasAmount("EUR", 20.82 + 3.67), hasGrossValue("EUR", 20.82 + 3.67), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2020-08-21T00:00"), hasShares(235.00), //
                                        hasSource("SteuerbehandlungVonDividende05.txt"), //
                                        hasNote("Tr.-Nr.: NUMMER"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testDividende05MitSteuerbehandlungVonDividende05WithSecurityInEUR_SourceFilesReversed()
    {
        var security = new Security("Aktiengesellschaft AG", "EUR");
        security.setIsin("DE0123456789");
        security.setWkn("ABC0DE");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TargobankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende05.txt", "SteuerbehandlungVonDividende05.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-08-31T00:00"), hasShares(235.00), //
                        hasSource("Dividende05.txt"), //
                        hasNote("R.-Nr.: NUMMER"), //
                        hasAmount("EUR", 20.82 + 3.67), hasGrossValue("EUR", 20.82 + 3.67), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2020-08-21T00:00"), hasShares(235.00), //
                                        hasSource("SteuerbehandlungVonDividende05.txt"), //
                                        hasNote("Tr.-Nr.: NUMMER"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                                        check(tx -> {
                                            var c = new CheckCurrenciesAction();
                                            var account = new Account();
                                            account.setCurrencyCode("EUR");
                                            var s = c.process((AccountTransaction) tx, account);
                                            assertThat(s, is(Status.OK_STATUS));
                                        })))));
    }

    @Test
    public void testDividende06()
    {
        var extractor = new TargobankPDFExtractor(new Client());

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
                        hasIsin("DE0002635307"), hasWkn("263530"), hasTicker(null), //
                        hasName("iSh.STOXX Europe 600 U.ETF DE - Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-12-15T00:00"), hasShares(1227.00), //
                        hasSource("Dividende06.txt"), //
                        hasNote("R.-Nr.: CPS-2022-0223620024-0002215"), //
                        hasAmount("EUR", 217.69), hasGrossValue("EUR", 217.69), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSteuerbehandlungVonDividende06()
    {
        var extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende06.txt"),
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
                        hasIsin("DE0002635307"), hasWkn("263530"), hasTicker(null), //
                        hasName("iSh.STOXX Europe 600 U.ETF DE - Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2022-12-15T00:00"), hasShares(1227.00), //
                        hasSource("SteuerbehandlungVonDividende06.txt"), //
                        hasNote("Tr.-Nr.: INDTBK34822CG020886O00"), //
                        hasAmount("EUR", 42.65), hasGrossValue("EUR", 42.65), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende06MitSteuerbehandlungVonDividende06()
    {
        var extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende06.txt", "SteuerbehandlungVonDividende06.txt"),
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
                        hasIsin("DE0002635307"), hasWkn("263530"), hasTicker(null), //
                        hasName("iSh.STOXX Europe 600 U.ETF DE - Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-12-15T00:00"), hasShares(1227.00), //
                        hasSource("Dividende06.txt; SteuerbehandlungVonDividende06.txt"), //
                        hasNote("R.-Nr.: CPS-2022-0223620024-0002215 | Tr.-Nr.: INDTBK34822CG020886O00"), //
                        hasAmount("EUR", 175.04), hasGrossValue("EUR", 217.69), //
                        hasTaxes("EUR", 42.65), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende06MitSteuerbehandlungVonDividende06_SourceFilesReversed()
    {
        var extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende06.txt", "Dividende06.txt"),
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
                        hasIsin("DE0002635307"), hasWkn("263530"), hasTicker(null), //
                        hasName("iSh.STOXX Europe 600 U.ETF DE - Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-12-15T00:00"), hasShares(1227.00), //
                        hasSource("Dividende06.txt; SteuerbehandlungVonDividende06.txt"), //
                        hasNote("R.-Nr.: CPS-2022-0223620024-0002215 | Tr.-Nr.: INDTBK34822CG020886O00"), //
                        hasAmount("EUR", 175.04), hasGrossValue("EUR", 217.69), //
                        hasTaxes("EUR", 42.65), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende07()
    {
        var extractor = new TargobankPDFExtractor(new Client());

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
                        hasIsin("US6668071029"), hasWkn("851915"), hasTicker(null), //
                        hasName("Northrop Grumman Corp. - Registered Shares DL 1"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-12-18T00:00"), hasShares(4.00), //
                        hasSource("Dividende07.txt"), //
                        hasNote("R.-Nr.: CPS-2024-0223620171-0003969"), //
                        hasAmount("EUR", 6.71 + 1.18), hasGrossValue("EUR", 6.71 + 1.18), //
                        hasForexGrossValue("USD", 8.24), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende07WithSecurityInEUR()
    {
        var security = new Security("Northrop Grumman Corp. - Registered Shares DL 1", "EUR");
        security.setIsin("US6668071029");
        security.setWkn("851915");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TargobankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende07.txt"), errors);

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
                        hasDate("2024-12-18T00:00"), hasShares(4.00), //
                        hasSource("Dividende07.txt"), //
                        hasNote("R.-Nr.: CPS-2024-0223620171-0003969"), //
                        hasAmount("EUR", 6.71 + 1.18), hasGrossValue("EUR", 6.71 + 1.18), //
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
    public void testSteuerbehandlungVonDividende07()
    {
        var extractor = new TargobankPDFExtractor(new Client());

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
                        hasIsin("US6668071029"), hasWkn("851915"), hasTicker(null), //
                        hasName("Northrop Grumman Corp. - Registered Shares DL 1"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2024-12-18T00:00"), hasShares(4.00), //
                        hasSource("SteuerbehandlungVonDividende07.txt"), //
                        hasNote("Tr.-Nr.: INDTBK35424CG007898O00"), //
                        hasAmount("EUR", 0.83 + 1.18), hasGrossValue("EUR", 0.83 + 1.18), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende07MitSteuerbehandlungVonDividende07()
    {
        var extractor = new TargobankPDFExtractor(new Client());

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
                        hasIsin("US6668071029"), hasWkn("851915"), hasTicker(null), //
                        hasName("Northrop Grumman Corp. - Registered Shares DL 1"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-12-18T00:00"), hasShares(4.00), //
                        hasSource("Dividende07.txt; SteuerbehandlungVonDividende07.txt"), //
                        hasNote("R.-Nr.: CPS-2024-0223620171-0003969 | Tr.-Nr.: INDTBK35424CG007898O00"), //
                        hasAmount("EUR", 7.06 - 1.18), hasGrossValue("EUR", 7.89), //
                        hasForexGrossValue("USD", 8.24), //
                        hasTaxes("EUR", 0.83 + 1.18), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende07MitSteuerbehandlungVonDividende03WithSecurityInEUR()
    {
        var security = new Security("Northrop Grumman Corp. - Registered Shares DL 1", "EUR");
        security.setIsin("US6668071029");
        security.setWkn("851915");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TargobankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende07.txt", "Dividende07.txt"),
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
                        hasDate("2024-12-18T00:00"), hasShares(4.00), //
                        hasSource("Dividende07.txt; SteuerbehandlungVonDividende07.txt"), //
                        hasNote("R.-Nr.: CPS-2024-0223620171-0003969 | Tr.-Nr.: INDTBK35424CG007898O00"), //
                        hasAmount("EUR", 7.06 - 1.18), hasGrossValue("EUR", 7.89), //
                        hasTaxes("EUR", 0.83 + 1.18), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende07MitSteuerbehandlungVonDividende07_SourceFilesReversed()
    {
        var extractor = new TargobankPDFExtractor(new Client());

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
                        hasIsin("US6668071029"), hasWkn("851915"), hasTicker(null), //
                        hasName("Northrop Grumman Corp. - Registered Shares DL 1"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-12-18T00:00"), hasShares(4.00), //
                        hasSource("Dividende07.txt; SteuerbehandlungVonDividende07.txt"), //
                        hasNote("R.-Nr.: CPS-2024-0223620171-0003969 | Tr.-Nr.: INDTBK35424CG007898O00"), //
                        hasAmount("EUR", 7.06 - 1.18), hasGrossValue("EUR", 7.89), //
                        hasTaxes("EUR", 0.83 + 1.18), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende07MitSteuerbehandlungVonDividende07WithSecurityInEUR_SourceFilesReversed()
    {
        var security = new Security("Northrop Grumman Corp. - Registered Shares DL 1", "EUR");
        security.setIsin("US6668071029");
        security.setWkn("851915");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TargobankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende07.txt", "SteuerbehandlungVonDividende07.txt"),
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
                        hasDate("2024-12-18T00:00"), hasShares(4.00), //
                        hasSource("Dividende07.txt; SteuerbehandlungVonDividende07.txt"), //
                        hasNote("R.-Nr.: CPS-2024-0223620171-0003969 | Tr.-Nr.: INDTBK35424CG007898O00"), //
                        hasAmount("EUR", 7.06 - 1.18), hasGrossValue("EUR", 7.89), //
                        hasTaxes("EUR", 0.83 + 1.18), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende08()
    {
        var extractor = new TargobankPDFExtractor(new Client());

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
                        hasIsin("US1912161007"), hasWkn("850663"), hasTicker(null), //
                        hasName("Coca-Cola Co., The - Registered Shares DL -,25"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-10-01T00:00"), hasShares(127.00), //
                        hasSource("Dividende08.txt"), //
                        hasNote("R.-Nr.: CPS-2020-0223111111-0001111"), //
                        hasAmount("EUR", 37.70 + 6.65), hasGrossValue("EUR", 37.70 + 6.65), //
                        hasForexGrossValue("USD", 52.07), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende08WithSecurityInEUR()
    {
        var security = new Security("Coca-Cola Co., The - Registered Shares DL -,25", "EUR");
        security.setIsin("US1912161007");
        security.setWkn("850663");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TargobankPDFExtractor(client);

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
                        hasDate("2020-10-01T00:00"), hasShares(127.00), //
                        hasSource("Dividende08.txt"), //
                        hasNote("R.-Nr.: CPS-2020-0223111111-0001111"), //
                        hasAmount("EUR", 37.70 + 6.65), hasGrossValue("EUR", 37.70 + 6.65), //
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
        var extractor = new TargobankPDFExtractor(new Client());

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
                        hasIsin("US1912161007"), hasWkn("850663"), hasTicker(null), //
                        hasName("Coca-Cola Co., The - Registered Shares DL -,25"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2020-10-01T00:00"), hasShares(127.00), //
                        hasSource("SteuerbehandlungVonDividende08.txt"), //
                        hasNote("Tr.-Nr.: INDTBK27620CG00000"), //
                        hasAmount("EUR", 4.68 + 6.65), hasGrossValue("EUR", 4.68 + 6.65), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende08MitSteuerbehandlungVonDividende08()
    {
        var extractor = new TargobankPDFExtractor(new Client());

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
                        hasIsin("US1912161007"), hasWkn("850663"), hasTicker(null), //
                        hasName("Coca-Cola Co., The - Registered Shares DL -,25"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-10-01T00:00"), hasShares(127.00), //
                        hasSource("Dividende08.txt; SteuerbehandlungVonDividende08.txt"), //
                        hasNote("R.-Nr.: CPS-2020-0223111111-0001111 | Tr.-Nr.: INDTBK27620CG00000"), //
                        hasAmount("EUR", 33.02), hasGrossValue("EUR", 44.35), //
                        hasForexGrossValue("USD", 52.07), //
                        hasTaxes("EUR", 4.68 + 6.65), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende08MitSteuerbehandlungVonDividende08WithSecurityInEUR()
    {
        var security = new Security("Coca-Cola Co., The - Registered Shares DL -,25", "EUR");
        security.setIsin("US1912161007");
        security.setWkn("850663");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TargobankPDFExtractor(client);

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
                        hasDate("2020-10-01T00:00"), hasShares(127.00), //
                        hasSource("Dividende08.txt; SteuerbehandlungVonDividende08.txt"), //
                        hasNote("R.-Nr.: CPS-2020-0223111111-0001111 | Tr.-Nr.: INDTBK27620CG00000"), //
                        hasAmount("EUR", 39.67 - 6.65), hasGrossValue("EUR", 44.35), //
                        hasTaxes("EUR", 4.68 + 6.65), hasFees("EUR", 0.00), //
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
        var extractor = new TargobankPDFExtractor(new Client());

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
                        hasIsin("US1912161007"), hasWkn("850663"), hasTicker(null), //
                        hasName("Coca-Cola Co., The - Registered Shares DL -,25"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-10-01T00:00"), hasShares(127.00), //
                        hasSource("Dividende08.txt; SteuerbehandlungVonDividende08.txt"), //
                        hasNote("R.-Nr.: CPS-2020-0223111111-0001111 | Tr.-Nr.: INDTBK27620CG00000"), //
                        hasAmount("EUR", 39.67 - 6.65), hasGrossValue("EUR", 44.35), //
                        hasTaxes("EUR", 4.68 + 6.65), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende08MitSteuerbehandlungVonDividende08WithSecurityInEUR_SourceFilesReversed()
    {
        var security = new Security("Coca-Cola Co., The - Registered Shares DL -,25", "EUR");
        security.setIsin("US1912161007");
        security.setWkn("850663");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TargobankPDFExtractor(client);

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
                        hasDate("2020-10-01T00:00"), hasShares(127.00), //
                        hasSource("Dividende08.txt; SteuerbehandlungVonDividende08.txt"), //
                        hasNote("R.-Nr.: CPS-2020-0223111111-0001111 | Tr.-Nr.: INDTBK27620CG00000"), //
                        hasAmount("EUR", 39.67 - 6.65), hasGrossValue("EUR", 44.35), //
                        hasTaxes("EUR", 4.68 + 6.65), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }
}
