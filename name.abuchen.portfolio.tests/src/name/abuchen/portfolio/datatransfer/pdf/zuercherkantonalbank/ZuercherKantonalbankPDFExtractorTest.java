package name.abuchen.portfolio.datatransfer.pdf.zuercherkantonalbank;

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
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.ZuercherKantonalbankPDFExtractor;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class ZuercherKantonalbankPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        ZuercherKantonalbankPDFExtractor extractor = new ZuercherKantonalbankPDFExtractor(new Client());

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
                        hasIsin("JE00B4T3BW64"), hasWkn("12964057"), hasTicker(null), //
                        hasName("Registered Shs Glencore PLC"), //
                        hasCurrencyCode("GBP"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-10-04T00:00"), hasShares(1000), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Abwicklungs-Nr. 592473551 | Auftrags-Nr. ONBA-0005128022772021"), //
                        hasAmount("CHF", 4469.94), hasGrossValue("CHF", 4462.37), //
                        hasForexGrossValue("GBP", 3545.00), //
                        hasTaxes("CHF", 6.68), hasFees("CHF", 0.89))));
    }

    @Test
    public void testWertpapierKauf01WithSecurityInCHF()
    {
        Security security = new Security("Registered Shs Glencore PLC", "CHF");
        security.setIsin("JE00B4T3BW64");
        security.setWkn("12964057");

        Client client = new Client();
        client.addSecurity(security);

        ZuercherKantonalbankPDFExtractor extractor = new ZuercherKantonalbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-10-04T00:00"), hasShares(1000), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Abwicklungs-Nr. 592473551 | Auftrags-Nr. ONBA-0005128022772021"), //
                        hasAmount("CHF", 4469.94), hasGrossValue("CHF", 4462.37), //
                        hasTaxes("CHF", 6.68), hasFees("CHF", 0.89), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Status s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        ZuercherKantonalbankPDFExtractor extractor = new ZuercherKantonalbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("GB0009697037"), hasWkn("1142141"), hasTicker(null), //
                        hasName("Registered Shs Babcock International Group PLC"), //
                        hasCurrencyCode("GBP"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-06-29T00:00"), hasShares(2000), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Abwicklungs-Nr. 715281248 | Auftrags-Nr. ONBA-0005066961792023"), //
                        hasAmount("USD", 7515.04), hasGrossValue("USD", 7461.23), //
                        hasForexGrossValue("GBP", 5900.00), //
                        hasTaxes("USD", 48.50), hasFees("USD", 5.31))));
    }

    @Test
    public void testWertpapierKauf02WithSecurityInUSD()
    {
        Security security = new Security("Registered Shs Babcock International Group PLC", "USD");
        security.setIsin("GB0009697037");
        security.setWkn("1142141");

        Client client = new Client();
        client.addSecurity(security);

        ZuercherKantonalbankPDFExtractor extractor = new ZuercherKantonalbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "USD");

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-06-29T00:00"), hasShares(2000), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Abwicklungs-Nr. 715281248 | Auftrags-Nr. ONBA-0005066961792023"), //
                        hasAmount("USD", 7515.04), hasGrossValue("USD", 7461.23), //
                        hasTaxes("USD", 48.50), hasFees("USD", 5.31), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Status s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        ZuercherKantonalbankPDFExtractor extractor = new ZuercherKantonalbankPDFExtractor(new Client());

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
                        hasIsin("CH0012214059"), hasWkn("1221405"), hasTicker(null), //
                        hasName("Namen-Akt Holcim AG"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-08T00:00"), hasShares(150), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Abwicklungs-Nr. 640981320 | Auftrags-Nr. ONBA-0005082331592022"), //
                        hasAmount("CHF", 7294.30), hasGrossValue("CHF", 7287.33), //
                        hasTaxes("CHF", 5.47), hasFees("CHF", 1.50))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        ZuercherKantonalbankPDFExtractor extractor = new ZuercherKantonalbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0000121485"), hasWkn("21591"), hasTicker(null), //
                        hasName("Act Kering SA"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-03-28T00:00"), hasShares(10), //
                        hasSource("Kauf04.txt"), //
                        hasNote("Abwicklungs-Nr. 626674106 | Auftrags-Nr. ONBA-0005089660872022"), //
                        hasAmount("CHF", 6078.35), hasGrossValue("CHF", 6046.52), //
                        hasForexGrossValue("EUR", 5891.00), //
                        hasTaxes("CHF", 27.21), hasFees("CHF", 4.62))));
    }

    @Test
    public void testWertpapierKauf04WithSecurityInCHF()
    {
        Security security = new Security("Act Kering SA", "CHF");
        security.setIsin("FR0000121485");
        security.setWkn("21591");

        Client client = new Client();
        client.addSecurity(security);

        ZuercherKantonalbankPDFExtractor extractor = new ZuercherKantonalbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-03-28T00:00"), hasShares(10), //
                        hasSource("Kauf04.txt"), //
                        hasNote("Abwicklungs-Nr. 626674106 | Auftrags-Nr. ONBA-0005089660872022"), //
                        hasAmount("CHF", 6078.35), hasGrossValue("CHF", 6046.52), //
                        hasTaxes("CHF", 27.21), hasFees("CHF", 4.62), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Status s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf05()
    {
        ZuercherKantonalbankPDFExtractor extractor = new ZuercherKantonalbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0512157782"), hasWkn("51215778"), hasTicker(null), //
                        hasName("Swisscanto (CH) IPF III (IPF III)"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-12-28T00:00"), hasShares(0.645), //
                        hasSource("Kauf05.txt"), //
                        hasNote("Abwicklungs-Nr. 123 | Auftrags-Nr. 123"), //
                        hasAmount("CHF", 99.96), hasGrossValue("CHF", 99.96), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierKauf06()
    {
        ZuercherKantonalbankPDFExtractor extractor = new ZuercherKantonalbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("51215778"), hasTicker(null), //
                        hasName("SWC (CH) IPF III VF 95 Passiv NT"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-28T00:00"), hasShares(0.633), //
                        hasSource("Kauf06.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 99.98), hasGrossValue("CHF", 99.98), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        ZuercherKantonalbankPDFExtractor extractor = new ZuercherKantonalbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0012214059"), hasWkn("1221405"), hasTicker(null), //
                        hasName("Namen-Akt Holcim AG"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-02-24T00:00"), hasShares(150), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Abwicklungs-Nr. 691980481 | Auftrags-Nr. ONBA-0005093310552023"), //
                        hasAmount("CHF", 8512.11), hasGrossValue("CHF", 8520.00), //
                        hasTaxes("CHF", 6.39), hasFees("CHF", 1.50))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        ZuercherKantonalbankPDFExtractor extractor = new ZuercherKantonalbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0000121485"), hasWkn("21591"), hasTicker(null), //
                        hasName("Act Kering SA"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-02-17T00:00"), hasShares(10), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Abwicklungs-Nr. 690706592 | Auftrags-Nr. ONBA-0005146670482023"), //
                        hasAmount("CHF", 5831.90), hasGrossValue("CHF", 5845.13), //
                        hasForexGrossValue("EUR", 5900.00), //
                        hasTaxes("CHF", 8.77), hasFees("CHF", 4.46))));
    }

    @Test
    public void testWertpapierVerkauf02WithSecurityInCHF()
    {
        Security security = new Security("Act Kering SA", "CHF");
        security.setIsin("FR0000121485");
        security.setWkn("21591");

        Client client = new Client();
        client.addSecurity(security);

        ZuercherKantonalbankPDFExtractor extractor = new ZuercherKantonalbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-02-17T00:00"), hasShares(10), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Abwicklungs-Nr. 690706592 | Auftrags-Nr. ONBA-0005146670482023"), //
                        hasAmount("CHF", 5831.90), hasGrossValue("CHF", 5845.13), //
                        hasTaxes("CHF", 8.77), hasFees("CHF", 4.46), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Status s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende01()
    {
        ZuercherKantonalbankPDFExtractor extractor = new ZuercherKantonalbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("GB0009697037"), hasWkn("1142141"), hasTicker(null), //
                        hasName("Registered Shs Babcock International Group PLC"), //
                        hasCurrencyCode("GBP"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-01-19T00:00"), hasShares(2000), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Abwicklungs-Nr. 744484581"), //
                        hasAmount("CHF", 37.25), hasGrossValue("CHF", 37.250), //
                        hasForexGrossValue("GBP", 34.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende01WithSecurityInCHF()
    {
        Security security = new Security("Registered Shs Babcock International Group PLC", "CHF");
        security.setIsin("GB0009697037");
        security.setWkn("1142141");

        Client client = new Client();
        client.addSecurity(security);

        ZuercherKantonalbankPDFExtractor extractor = new ZuercherKantonalbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-01-19T00:00"), hasShares(2000), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Abwicklungs-Nr. 744484581"), //
                        hasAmount("CHF", 37.25), hasGrossValue("CHF", 37.250), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode("CHF");
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende02()
    {
        ZuercherKantonalbankPDFExtractor extractor = new ZuercherKantonalbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US46641Q3323"), hasWkn("52708803"), hasTicker(null), //
                        hasName("Shs J.P. Morgan Exchange-Traded Fund Trust JPMorgan Equity Premium"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-01-04T00:00"), hasShares(200), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Abwicklungs-Nr. 680426625"), //
                        hasAmount("CHF", 74.01), hasGrossValue("CHF", 105.73), //
                        hasForexGrossValue("USD", 114.58), //
                        hasTaxes("CHF", 31.72), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende02WithSecurityInUSD()
    {
        Security security = new Security("Shs J.P. Morgan Exchange-Traded Fund Trust JPMorgan Equity Premium", "CHF");
        security.setIsin("US46641Q3323");
        security.setWkn("52708803");

        Client client = new Client();
        client.addSecurity(security);

        ZuercherKantonalbankPDFExtractor extractor = new ZuercherKantonalbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-01-04T00:00"), hasShares(200), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Abwicklungs-Nr. 680426625"), //
                        hasAmount("CHF", 74.01), hasGrossValue("CHF", 105.73), //
                        hasTaxes("CHF", 31.72), hasFees("CHF", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode("CHF");
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende03()
    {
        ZuercherKantonalbankPDFExtractor extractor = new ZuercherKantonalbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US46641Q3323"), hasWkn("52708803"), hasTicker(null), //
                        hasName("Shs J.P. Morgan Exchange-Traded Fund Trust JPMorgan Equity Premium"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-02-06T00:00"), hasShares(200), //
                        hasSource("Dividende03.txt"), //
                        hasNote("Abwicklungs-Nr. 759703889"), //
                        hasAmount("USD", 42.08), hasGrossValue("USD", 60.12), //
                        hasTaxes("USD", 18.04), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende03WithSecurityInUSD()
    {
        Security security = new Security("Shs J.P. Morgan Exchange-Traded Fund Trust JPMorgan Equity Premium", "CHF");
        security.setIsin("US46641Q3323");
        security.setWkn("52708803");

        Client client = new Client();
        client.addSecurity(security);

        ZuercherKantonalbankPDFExtractor extractor = new ZuercherKantonalbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "USD");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-02-06T00:00"), hasShares(200), //
                        hasSource("Dividende03.txt"), //
                        hasNote("Abwicklungs-Nr. 759703889"), //
                        hasAmount("USD", 42.08), hasGrossValue("USD", 60.12), //
                        hasForexGrossValue("CHF", 51.44), //
                        hasTaxes("USD", 18.04), hasFees("USD", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode("USD");
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende04()
    {
        ZuercherKantonalbankPDFExtractor extractor = new ZuercherKantonalbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B02KXH56"), hasWkn("1965564"), hasTicker(null), //
                        hasName("Shs USD iShares PLC - iShares MSCI Japan UCITS ETF USD (Dist)"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-01-24T00:00"), hasShares(930), //
                        hasSource("Dividende04.txt"), //
                        hasNote("Abwicklungs-Nr. 754899061"), //
                        hasAmount("USD", 99.14), hasGrossValue("USD", 99.14), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));
    }
}
