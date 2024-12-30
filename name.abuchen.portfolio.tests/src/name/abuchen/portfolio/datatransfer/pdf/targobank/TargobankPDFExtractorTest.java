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
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSecurities;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.TargobankPDFExtractor;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;

@SuppressWarnings("nls")
public class TargobankPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonVerkauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Verkauf02.txt", "SteuerbehandlungVonVerkauf02.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonVerkauf02.txt", "Verkauf02.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonVerkauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Verkauf03.txt", "SteuerbehandlungVonVerkauf03.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonVerkauf03.txt", "Verkauf03.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        Security security = new Security("Vang.FTSE Develop.World U.ETF - Registered Shares USD Dis.oN",
                        CurrencyUnit.EUR);
        security.setIsin("IE00BKX55T58");
        security.setWkn("A12CX1");

        Client client = new Client();
        client.addSecurity(security);

        TargobankPDFExtractor extractor = new TargobankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-24T00:00"), hasShares(81.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote("R.-Nr.: CPS-2020-0123456789-0001234"), //
                        hasAmount("EUR", 21.18), hasGrossValue("EUR", 21.18), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testSteuerbehandlungVonDividende01()
    {
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende01.txt", "SteuerbehandlungVonDividende01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        Security security = new Security("Vang.FTSE Develop.World U.ETF - Registered Shares USD Dis.oN",
                        CurrencyUnit.EUR);
        security.setIsin("IE00BKX55T58");
        security.setWkn("A12CX1");

        Client client = new Client();
        client.addSecurity(security);

        TargobankPDFExtractor extractor = new TargobankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende01.txt", "Dividende01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-24T00:00"), hasShares(81.00), //
                        hasSource("Dividende01.txt; SteuerbehandlungVonDividende01.txt"), //
                        hasNote("R.-Nr.: CPS-2020-0123456789-0001234 | Tr.-Nr.: INDTBK1234567890"), //
                        hasAmount("EUR", 15.59), hasGrossValue("EUR", 21.18), //
                        hasTaxes("EUR", 5.59), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende01MitSteuerbehandlungVonDividende01_SourceFilesReversed()
    {
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende01.txt", "Dividende01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        Security security = new Security("Vang.FTSE Develop.World U.ETF - Registered Shares USD Dis.oN",
                        CurrencyUnit.EUR);
        security.setIsin("IE00BKX55T58");
        security.setWkn("A12CX1");

        Client client = new Client();
        client.addSecurity(security);

        TargobankPDFExtractor extractor = new TargobankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende01.txt", "SteuerbehandlungVonDividende01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-24T00:00"), hasShares(81.00), //
                        hasSource("Dividende01.txt; SteuerbehandlungVonDividende01.txt"), //
                        hasNote("R.-Nr.: CPS-2020-0123456789-0001234 | Tr.-Nr.: INDTBK1234567890"), //
                        hasAmount("EUR", 15.59), hasGrossValue("EUR", 21.18), //
                        hasTaxes("EUR", 5.59), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende02()
    {
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende02.txt", "SteuerbehandlungVonDividende02.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende02.txt", "Dividende02.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        Security security = new Security("Xtrackers Harvest CSI300 - Inhaber-Anteile 1D o.N.", CurrencyUnit.EUR);
        security.setIsin("LU0875160326");
        security.setWkn("DBX0NK");

        Client client = new Client();
        client.addSecurity(security);

        TargobankPDFExtractor extractor = new TargobankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-04-27T00:00"), hasShares(1700.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote("R.-Nr.: CPS-2020-0223620168-0001148"), //
                        hasAmount("EUR", 279.64), hasGrossValue("EUR", 279.64), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testSteuerbehandlungVonDividende03()
    {
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende03.txt", "SteuerbehandlungVonDividende03.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        Security security = new Security("Xtrackers Harvest CSI300 - Inhaber-Anteile 1D o.N.", CurrencyUnit.EUR);
        security.setIsin("LU0875160326");
        security.setWkn("DBX0NK");

        Client client = new Client();
        client.addSecurity(security);

        TargobankPDFExtractor extractor = new TargobankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende03.txt", "Dividende03.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-04-27T00:00"), hasShares(1700.00), //
                        hasSource("Dividende03.txt; SteuerbehandlungVonDividende03.txt"), //
                        hasNote("R.-Nr.: CPS-2020-0223620168-0001148 | Tr.-Nr.: INDTBK12120CG000130O00"), //
                        hasAmount("EUR", 228.01), hasGrossValue("EUR", 279.64), //
                        hasTaxes("EUR", 51.63), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende03MitSteuerbehandlungVonDividende03_SourceFilesReversed()
    {
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende03.txt", "Dividende03.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        Security security = new Security("Xtrackers Harvest CSI300 - Inhaber-Anteile 1D o.N.", CurrencyUnit.EUR);
        security.setIsin("LU0875160326");
        security.setWkn("DBX0NK");

        Client client = new Client();
        client.addSecurity(security);

        TargobankPDFExtractor extractor = new TargobankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende03.txt", "SteuerbehandlungVonDividende03.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-04-27T00:00"), hasShares(1700.00), //
                        hasSource("Dividende03.txt; SteuerbehandlungVonDividende03.txt"), //
                        hasNote("R.-Nr.: CPS-2020-0223620168-0001148 | Tr.-Nr.: INDTBK12120CG000130O00"), //
                        hasAmount("EUR", 228.01), hasGrossValue("EUR", 279.64), //
                        hasTaxes("EUR", 51.63), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende04()
    {
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende04.txt", "SteuerbehandlungVonDividende04.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende04.txt", "Dividende04.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
                        hasAmount("EUR", 20.82), hasGrossValue("EUR", 20.82), //
                        hasForexGrossValue("USD", 25.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende05WithSecurityInEUR()
    {
        Security security = new Security("Aktiengesellschaft AG", CurrencyUnit.EUR);
        security.setIsin("DE0123456789");
        security.setWkn("ABC0DE");

        Client client = new Client();
        client.addSecurity(security);

        TargobankPDFExtractor extractor = new TargobankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-08-31T00:00"), hasShares(235.00), //
                        hasSource("Dividende05.txt"), //
                        hasNote("R.-Nr.: NUMMER"), //
                        hasAmount("EUR", 20.82), hasGrossValue("EUR", 20.82), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testSteuerbehandlungVonDividende05()
    {
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende05.txt", "SteuerbehandlungVonDividende05.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
                        hasAmount("EUR", 20.82), hasGrossValue("EUR", 20.82), //
                        hasForexGrossValue("USD", 25.00), //
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
        Security security = new Security("Aktiengesellschaft AG", CurrencyUnit.EUR);
        security.setIsin("DE0123456789");
        security.setWkn("ABC0DE");

        Client client = new Client();
        client.addSecurity(security);

        TargobankPDFExtractor extractor = new TargobankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende05.txt", "Dividende05.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-08-31T00:00"), hasShares(235.00), //
                        hasSource("Dividende05.txt"), //
                        hasNote("R.-Nr.: NUMMER"), //
                        hasAmount("EUR", 20.82), hasGrossValue("EUR", 20.82), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
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
                                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                                            Account account = new Account();
                                            account.setCurrencyCode(CurrencyUnit.EUR);
                                            Status s = c.process((AccountTransaction) tx, account);
                                            assertThat(s, is(Status.OK_STATUS));
                                        })))));
    }

    @Test
    public void testDividende05MitSteuerbehandlungVonDividende05_SourceFilesReversed()
    {
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende05.txt", "Dividende05.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
                        hasAmount("EUR", 20.82), hasGrossValue("EUR", 20.82), //
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
        Security security = new Security("Aktiengesellschaft AG", CurrencyUnit.EUR);
        security.setIsin("DE0123456789");
        security.setWkn("ABC0DE");

        Client client = new Client();
        client.addSecurity(security);

        TargobankPDFExtractor extractor = new TargobankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende05.txt", "SteuerbehandlungVonDividende05.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-08-31T00:00"), hasShares(235.00), //
                        hasSource("Dividende05.txt"), //
                        hasNote("R.-Nr.: NUMMER"), //
                        hasAmount("EUR", 20.82), hasGrossValue("EUR", 20.82), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
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
                                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                                            Account account = new Account();
                                            account.setCurrencyCode(CurrencyUnit.EUR);
                                            Status s = c.process((AccountTransaction) tx, account);
                                            assertThat(s, is(Status.OK_STATUS));
                                        })))));
    }

    @Test
    public void testDividende06()
    {
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende06.txt", "SteuerbehandlungVonDividende06.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende06.txt", "Dividende06.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
                        hasAmount("EUR", 6.71), hasGrossValue("EUR", 6.71), //
                        hasForexGrossValue("USD", 7.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende07WithSecurityInEUR()
    {
        Security security = new Security("Northrop Grumman Corp. - Registered Shares DL 1", CurrencyUnit.EUR);
        security.setIsin("US6668071029");
        security.setWkn("851915");

        Client client = new Client();
        client.addSecurity(security);

        TargobankPDFExtractor extractor = new TargobankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-12-18T00:00"), hasShares(4.00), //
                        hasSource("Dividende07.txt"), //
                        hasNote("R.-Nr.: CPS-2024-0223620171-0003969"), //
                        hasAmount("EUR", 6.71), hasGrossValue("EUR", 6.71), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testSteuerbehandlungVonDividende07()
    {
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
                        hasAmount("EUR", 0.83), hasGrossValue("EUR", 0.83), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende07MitSteuerbehandlungVonDividende07()
    {
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende07.txt", "SteuerbehandlungVonDividende07.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
                        hasAmount("EUR", 7.06), hasGrossValue("EUR", 7.89), //
                        hasForexGrossValue("USD", 8.24), //
                        hasTaxes("EUR", 0.83), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende07MitSteuerbehandlungVonDividende03WithSecurityInEUR()
    {
        Security security = new Security("Northrop Grumman Corp. - Registered Shares DL 1", CurrencyUnit.EUR);
        security.setIsin("US6668071029");
        security.setWkn("851915");

        Client client = new Client();
        client.addSecurity(security);

        TargobankPDFExtractor extractor = new TargobankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende07.txt", "Dividende07.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-12-18T00:00"), hasShares(4.00), //
                        hasSource("Dividende07.txt; SteuerbehandlungVonDividende07.txt"), //
                        hasNote("R.-Nr.: CPS-2024-0223620171-0003969 | Tr.-Nr.: INDTBK35424CG007898O00"), //
                        hasAmount("EUR", 7.06), hasGrossValue("EUR", 7.89), //
                        hasTaxes("EUR", 0.83), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende07MitSteuerbehandlungVonDividende07_SourceFilesReversed()
    {
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende07.txt", "Dividende07.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
                        hasAmount("EUR", 7.06), hasGrossValue("EUR", 7.89), //
                        hasTaxes("EUR", 0.83), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende07MitSteuerbehandlungVonDividende07WithSecurityInEUR_SourceFilesReversed()
    {
        Security security = new Security("Northrop Grumman Corp. - Registered Shares DL 1", CurrencyUnit.EUR);
        security.setIsin("US6668071029");
        security.setWkn("851915");

        Client client = new Client();
        client.addSecurity(security);

        TargobankPDFExtractor extractor = new TargobankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende07.txt", "SteuerbehandlungVonDividende07.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-12-18T00:00"), hasShares(4.00), //
                        hasSource("Dividende07.txt; SteuerbehandlungVonDividende07.txt"), //
                        hasNote("R.-Nr.: CPS-2024-0223620171-0003969 | Tr.-Nr.: INDTBK35424CG007898O00"), //
                        hasAmount("EUR", 7.06), hasGrossValue("EUR", 7.89), //
                        hasTaxes("EUR", 0.83), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }
}
