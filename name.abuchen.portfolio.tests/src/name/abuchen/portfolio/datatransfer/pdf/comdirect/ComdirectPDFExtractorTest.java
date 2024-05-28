package name.abuchen.portfolio.datatransfer.pdf.comdirect;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.check;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.fee;
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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interestCharge;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxRefund;
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
import name.abuchen.portfolio.datatransfer.pdf.ComdirectPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;

@SuppressWarnings("nls")
public class ComdirectPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

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
                        hasIsin("FR0000121014"), hasWkn("853292"), hasTicker(null), //
                        hasName("LVMH Moët Henn. L. Vuitton SA Actions Port. (C.R.) EO 0,3"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2007-05-19T00:00"), hasShares(100.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Ord.-Nr.: 111111111111 | R.-Nr.: 11111111111DB11"), //
                        hasAmount("EUR", 8627.90), hasGrossValue("EUR", 8600.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 26.40 + 1.50))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

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
                        hasIsin("US0970231058"), hasWkn("850471"), hasTicker(null), //
                        hasName("Boeing Co. Registered Shares DL 5"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2016-07-18T17:02"), hasShares(160.00), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Ord.-Nr.: 072324316214-001 | R.-Nr.: 395955075438D1F5"), //
                        hasAmount("EUR", 19359.18), hasGrossValue("EUR", 19303.52), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 53.16 + 2.50))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0005194062"), hasWkn("519406"), hasTicker(null), //
                        hasName("BayWa AG vink. Namens-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2013-03-14T12:09"), hasShares(1437.00), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Ord.-Nr.: 071077639314-001 | R.-Nr.: 290356708923DCA5"), //
                        hasAmount("EUR", 16312.80), hasGrossValue("EUR", 16265.14), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 45.56 + 1.50 + 0.60))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US0231351067"), hasWkn("906866"), hasTicker(null), //
                        hasName("Amazon.com Inc. Registered Shares DL -,01"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-04-16T18:33"), hasShares(2.00), //
                        hasSource("Kauf04.txt"), //
                        hasNote("Ord.-Nr.: -001 | R.-Nr.: "), //
                        hasAmount("EUR", 4444.15), hasGrossValue("EUR", 4412.36), //
                        hasForexGrossValue("USD", 4768.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 18.93 + (13.90 / 1.080600)))));
    }

    @Test
    public void testWertpapierKauf04WithSecurityInEUR()
    {
        Security security = new Security("Amazon.com Inc. Registered Shares DL -,01", CurrencyUnit.EUR);
        security.setIsin("US0231351067");
        security.setWkn("906866");

        Client client = new Client();
        client.addSecurity(security);

        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-04-16T18:33"), hasShares(2.00), //
                        hasSource("Kauf04.txt"), //
                        hasNote("Ord.-Nr.: -001 | R.-Nr.: "), //
                        hasAmount("EUR", 4444.15), hasGrossValue("EUR", 4412.36), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 18.93 + (13.90 / 1.080600)), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Status s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf05()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US00845V3087"), hasWkn("A0ET5J"), hasTicker(null), //
                        hasName("Agere Systems Inc. Registered Shares DL -,01"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2005-06-06T00:00"), hasShares(0.10), //
                        hasSource("Kauf05.txt"), //
                        hasNote("Ord.-Nr.: 111111111111 | R.-Nr.: 111111111111D111"), //
                        hasAmount("EUR", 0.88), hasGrossValue("EUR", 0.88), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKaufMitSteuerbehandlung01()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KaufMitSteuerbehandlung01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU2114851830"), hasWkn("DWS26Y"), hasTicker(null), //
                        hasName("ARERO Der Weltfonds - ESG Inhaber-Anteile LC o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-01-02T00:00"), hasShares(8.544), //
                        hasSource("KaufMitSteuerbehandlung01.txt"), //
                        hasNote("Ord.-Nr.: 592581219254 | R.-Nr.: 878649826981vsP4"), //
                        hasAmount("EUR", 999.90), hasGrossValue("EUR", 999.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2023-01-02T00:00"), hasShares(8.544), //
                                        hasSource("KaufMitSteuerbehandlung01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testWertpapierKaufMitSteuerbehandlung02()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KaufMitSteuerbehandlung02.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0378437502"), hasWkn("ETF010"), hasTicker(null), //
                        hasName("ComSt.-DJ Industr.Averag.U.ETF Inhaber-Anteile I o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-06-01T00:00"), hasShares(0.10), //
                        hasSource("KaufMitSteuerbehandlung02.txt"), //
                        hasNote("Ord.-Nr.: 150799808720 | R.-Nr.: 454960516206DB75"), //
                        hasAmount("EUR", 24.99), hasGrossValue("EUR", 24.62), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.37))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2018-06-01T00:00"), hasShares(0.10), //
                                        hasSource("KaufMitSteuerbehandlung02.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testWertpapierKaufMitSteuerbehandlung03()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KaufMitSteuerbehandlung03.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1273675402"), hasWkn("A14X2L"), hasTicker(null), //
                        hasName("BSF - BlackRock MIPG Actions Nom.A4 EUR o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-01-08T00:00"), hasShares(0.205), //
                        hasSource("KaufMitSteuerbehandlung03.txt"), //
                        hasNote("Ord.-Nr.: 004786949040 | R.-Nr.: 442604441030D195"), //
                        hasAmount("EUR", 24.97), hasGrossValue("EUR", 24.97), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2018-01-08T00:00"), hasShares(0.205), //
                                        hasSource("KaufMitSteuerbehandlung03.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testWertpapierKaufMitSteuerbehandlung04()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KaufMitSteuerbehandlung04.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000BASF111"), hasWkn("BASF11"), hasTicker(null), //
                        hasName("BASF SE Namens-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-11-01T00:00"), hasShares(2.17), //
                        hasSource("KaufMitSteuerbehandlung04.txt"), //
                        hasNote("Ord.-Nr.: 303796885690 | R.-Nr.: 468179348267DC35"), //
                        hasAmount("EUR", 149.95), hasGrossValue("EUR", 146.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.20 + 0.95))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2018-11-01T00:00"), hasShares(2.17), //
                                        hasSource("KaufMitSteuerbehandlung04.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testWertpapierKaufMitSteuerbehandlung05()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KaufMitSteuerbehandlung05.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0055631609"), hasWkn("974119"), hasTicker(null), //
                        hasName("BGF - World Gold Fund Act. Nom. A2RF USD o.N."), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-06-07T00:00"), hasShares(0.843), //
                        hasSource("KaufMitSteuerbehandlung05.txt"), //
                        hasNote("Ord.-Nr.: 153797450590 | R.-Nr.: 424029016868D1B5"), //
                        hasAmount("EUR", 24.97), hasGrossValue("EUR", 24.97), //
                        hasForexGrossValue("USD", 28.05), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2017-06-07T00:00"), hasShares(0.843), //
                                        hasSource("KaufMitSteuerbehandlung05.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testWertpapierKaufMitSteuerbehandlung05WithSecurityInEUR()
    {
        Security security = new Security("BGF - World Gold Fund Act. Nom. A2RF USD o.N.", CurrencyUnit.EUR);
        security.setIsin("LU0055631609");
        security.setWkn("974119");

        Client client = new Client();
        client.addSecurity(security);

        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KaufMitSteuerbehandlung05.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-06-07T00:00"), hasShares(0.843), //
                        hasSource("KaufMitSteuerbehandlung05.txt"), //
                        hasNote("Ord.-Nr.: 153797450590 | R.-Nr.: 424029016868D1B5"), //
                        hasAmount("EUR", 24.97), hasGrossValue("EUR", 24.97), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Status s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2017-06-07T00:00"), hasShares(0.843), //
                                        hasSource("KaufMitSteuerbehandlung05.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testWertpapierKaufMitSteuerbehandlung06()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KaufMitSteuerbehandlung06.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BTN1Y115"), hasWkn("A14M2J"), hasTicker(null), //
                        hasName("Medtronic PLC Registered Shares DL -,0001"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2016-11-22T00:00"), hasShares(20.00), //
                        hasSource("KaufMitSteuerbehandlung06.txt"), //
                        hasNote("Ord.-Nr.: 072450450919 | R.-Nr.: 406909971273D0D5"), //
                        hasAmount("EUR", 1431.40), hasGrossValue("EUR", 1420.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 9.90 + 1.50))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2016-11-22T00:00"), hasShares(20.00), //
                                        hasSource("KaufMitSteuerbehandlung06.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testWertpapierKaufMitSteuerbehandlung07()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KaufMitSteuerbehandlung07.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("NL0000687663"), hasWkn("A0LFB3"), hasTicker(null), //
                        hasName("AerCap Holdings N.V. Aandelen op naam EO -,01"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-03-11T00:00"), hasShares(720.00), //
                        hasSource("KaufMitSteuerbehandlung07.txt"), //
                        hasNote("Ord.-Nr.: 000121111681 | R.-Nr.: 511138111156DA65"), //
                        hasAmount("EUR", 25847.07), hasGrossValue("EUR", 25759.28), //
                        hasForexGrossValue("USD", 28870.99), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 62.90 + (27.90 / 1.120800)))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2020-03-11T00:00"), hasShares(720.00), //
                                        hasSource("KaufMitSteuerbehandlung07.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testWertpapierKaufMitSteuerbehandlung07WithSecurityInEUR()
    {
        Security security = new Security("AerCap Holdings N.V. Aandelen op naam EO -,01", CurrencyUnit.EUR);
        security.setIsin("NL0000687663");
        security.setWkn("A0LFB3");

        Client client = new Client();
        client.addSecurity(security);

        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KaufMitSteuerbehandlung07.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-03-11T00:00"), hasShares(720.00), //
                        hasSource("KaufMitSteuerbehandlung07.txt"), //
                        hasNote("Ord.-Nr.: 000121111681 | R.-Nr.: 511138111156DA65"), //
                        hasAmount("EUR", 25847.07), hasGrossValue("EUR", 25759.28), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 62.90 + (27.90 / 1.120800)), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Status s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2020-03-11T00:00"), hasShares(720.00), //
                                        hasSource("KaufMitSteuerbehandlung07.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testWertpapierKaufMitSteuerbehandlung08()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KaufMitSteuerbehandlung08.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("GB00B63H8491"), hasWkn("A1H81L"), hasTicker(null), //
                        hasName("Rolls Royce Holdings PLC Registered Shares LS 0.20"), //
                        hasCurrencyCode("GBP"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-12-02T00:00"), hasShares(6000.00), //
                        hasSource("KaufMitSteuerbehandlung08.txt"), //
                        hasNote("Ord.-Nr.: 111111111111 | R.-Nr.: 111111111111DB11"), //
                        hasAmount("EUR", 50569.94), hasGrossValue("EUR", 50238.13), //
                        hasForexGrossValue("GBP", 42720.00), //
                        hasTaxes("EUR", (213.60 / 0.850350)), hasFees("EUR", 62.90 + (15.07 / 0.850350)))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2019-12-02T00:00"), hasShares(6000.00), //
                                        hasSource("KaufMitSteuerbehandlung08.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testWertpapierKaufMitSteuerbehandlung08WithSecurityInEUR()
    {
        Security security = new Security("Rolls Royce Holdings PLC Registered Shares LS 0.20", CurrencyUnit.EUR);
        security.setIsin("GB00B63H8491");
        security.setWkn("A1H81L");

        Client client = new Client();
        client.addSecurity(security);

        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KaufMitSteuerbehandlung08.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-12-02T00:00"), hasShares(6000.00), //
                        hasSource("KaufMitSteuerbehandlung08.txt"), //
                        hasNote("Ord.-Nr.: 111111111111 | R.-Nr.: 111111111111DB11"), //
                        hasAmount("EUR", 50569.94), hasGrossValue("EUR", 50238.13), //
                        hasTaxes("EUR", (213.60 / 0.850350)), hasFees("EUR", 62.90 + (15.07 / 0.850350)), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Status s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2019-12-02T00:00"), hasShares(6000.00), //
                                        hasSource("KaufMitSteuerbehandlung08.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testWertpapierKaufMitSteuerbehandlung09()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KaufMitSteuerbehandlung09.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CA8911021050"), hasWkn("914305"), hasTicker(null), //
                        hasName("Toromont Industries Ltd. Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-02-21T15:43"), hasShares(34.00), //
                        hasSource("KaufMitSteuerbehandlung09.txt"), //
                        hasNote("Ord.-Nr.: 111111111111-111 | R.-Nr.: 2222222222222A22"), //
                        hasAmount("EUR", 1686.80), hasGrossValue("EUR", 1666.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.90 + 9.90 + 5.00 + 3.00))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2020-02-21T00:00"), hasShares(34.00), //
                                        hasSource("KaufMitSteuerbehandlung09.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testWertpapierKaufMitSteuerbehandlung10()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KaufMitSteuerbehandlung10.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US09259E1082"), hasWkn("A2N4AB"), hasTicker(null), //
                        hasName("BlackRock TCP Capital Corp. Registered Shares DL -,001"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-28T16:50"), hasShares(150.00), //
                        hasSource("KaufMitSteuerbehandlung10.txt"), //
                        hasNote("R.-Nr.: "), //
                        hasAmount("EUR", 1430.30), hasGrossValue("EUR", 1417.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.90 + 9.90))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2020-12-28T00:00"), hasShares(150.00), //
                                        hasSource("KaufMitSteuerbehandlung10.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testWertpapierKaufMitSteuerbehandlung11()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KaufMitSteuerbehandlung11.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU2114851830"), hasWkn("DWS26Y"), hasTicker(null), //
                        hasName("ARERO Der Weltfonds - ESG Inhaber-Anteile LC o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-01-16T00:00"), hasShares(8.261), //
                        hasSource("KaufMitSteuerbehandlung11.txt"), //
                        hasNote("Ord.-Nr.: 000000000000 | R.-Nr.: 600000000000DBD5"), //
                        hasAmount("EUR", 999.91), hasGrossValue("EUR", 999.91), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2023-01-16T00:00"), hasShares(8.261), //
                                        hasSource("KaufMitSteuerbehandlung11.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testWertpapierKaufMitSteuerbehandlung12()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KaufMitSteuerbehandlung12.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU2114851830"), hasWkn("DWS26Y"), hasTicker(null), //
                        hasName("ARERO Der Weltfonds - ESG Inhaber-Anteile LC o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-05-02T00:00"), hasShares(2.734), //
                        hasSource("KaufMitSteuerbehandlung12.txt"), //
                        hasNote("Ord.-Nr.: 302789004599 | R.-Nr.: 019579984081kw86"), //
                        hasAmount("EUR", 323.49), hasGrossValue("EUR", 323.49), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2023-05-02T00:00"), hasShares(2.734), //
                                        hasSource("KaufMitSteuerbehandlung12.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testWertpapierKaufMitSteuerbehandlung13()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KaufMitSteuerbehandlung13.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000BASF111"), hasWkn("BASF11"), hasTicker(null), //
                        hasName("BASF SE Namens-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-10-04T00:00"), hasShares(0.565), //
                        hasSource("KaufMitSteuerbehandlung13.txt"), //
                        hasNote("Ord.-Nr.: 272803480270 | R.-Nr.: 591997149596D095"), //
                        hasAmount("EUR", 24.99), hasGrossValue("EUR", 23.68), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.36 + 0.95))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2022-10-04T00:00"), hasShares(0.565), //
                                        hasSource("KaufMitSteuerbehandlung13.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testWertpapierKaufMitSteuerbehandlung14()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KaufMitSteuerbehandlung14.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000BASF111"), hasWkn("BASF11"), hasTicker(null), //
                        hasName("BASF SE Namens-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-10-04T09:02"), hasShares(5.00), //
                        hasSource("KaufMitSteuerbehandlung14.txt"), //
                        hasNote("Ord.-Nr.: 000312226831-001 | R.-Nr.: 591958998217D175"), //
                        hasAmount("EUR", 220.85), hasGrossValue("EUR", 207.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 9.90 + 0.95 + 2.50))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2022-10-04T00:00"), hasShares(5.00), //
                                        hasSource("KaufMitSteuerbehandlung14.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testWertpapierKaufMitSteuerbehandlung15()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KaufMitSteuerbehandlung15.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0048580855"), hasWkn("973265"), hasTicker(null), //
                        hasName("Fidelity Fds-Greater China Fd. Reg.Shares A (Glob.Cert.) o.N."), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-07T00:00"), hasShares(0.165), //
                        hasSource("KaufMitSteuerbehandlung15.txt"), //
                        hasNote("Ord.-Nr.: 1123456789560 | R.-Nr.: 123456784001DF35"), //
                        hasAmount("EUR", 49.98), hasGrossValue("EUR", 49.98), //
                        hasForexGrossValue("USD", 60.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2021-05-07T00:00"), hasShares(0.165), //
                                        hasSource("KaufMitSteuerbehandlung15.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testWertpapierKaufMitSteuerbehandlung15WithSecurityInEUR()
    {
        Security security = new Security("Fidelity Fds-Greater China Fd. Reg.Shares A (Glob.Cert.) o.N.",
                        CurrencyUnit.EUR);
        security.setIsin("LU0048580855");
        security.setWkn("973265");

        Client client = new Client();
        client.addSecurity(security);

        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KaufMitSteuerbehandlung15.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-07T00:00"), hasShares(0.165), //
                        hasSource("KaufMitSteuerbehandlung15.txt"), //
                        hasNote("Ord.-Nr.: 1123456789560 | R.-Nr.: 123456784001DF35"), //
                        hasAmount("EUR", 49.98), hasGrossValue("EUR", 49.98), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Status s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2021-05-07T00:00"), hasShares(0.165), //
                                        hasSource("KaufMitSteuerbehandlung15.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testWertpapierKaufMitSteuerbehandlung16()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KaufMitSteuerbehandlung16.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000SR7YCM4"), hasWkn("SR7YCM"), hasTicker(null), //
                        hasName("14,00000% SG Issuer S.A. EO-MTN 23(24) Tesla"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-11-13T00:00"), hasShares(10.00), //
                        hasSource("KaufMitSteuerbehandlung16.txt"), //
                        hasNote("Ord.-Nr.: 317309783000 | R.-Nr.: 627068187716DB85"), //
                        hasAmount("EUR", 1000.00), hasGrossValue("EUR", 1000.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2023-11-13T00:00"), hasShares(10.00), //
                                        hasSource("KaufMitSteuerbehandlung16.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testWertpapierKaufSteuerbehandlung01()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KaufSteuerbehandlung01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US7427181091"), hasWkn("852062"), hasTicker(null), //
                        hasName("PROCTER GAMBLE"), //
                        hasCurrencyCode("EUR"))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2021-01-04T00:00"), hasShares(0.216), //
                                        hasSource("KaufSteuerbehandlung01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

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
                        hasIsin("DE0009848002"), hasWkn("984800"), hasTicker(null), //
                        hasName("DWS Internet-Aktien Typ O Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2007-02-20T00:00"), hasShares(67.00), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Ord.-Nr.: 007301624026-004 | R.-Nr.: 099094791965DAF2"), //
                        hasAmount("EUR", 1004.33), hasGrossValue("EUR", 1004.33), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

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
                        hasIsin("DE0009503540"), hasWkn("950354"), hasTicker(null), //
                        hasName("Citigroup Global Markets Dt. KOS03/21.12.04 Allianz 80"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2003-12-08T00:00"), hasShares(550.00), //
                        hasSource("Verkauf02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1086.72), hasGrossValue("EUR", 1100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.88 + 9.90 + 2.50))));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

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
                        hasIsin("IE00B0M62Q58"), hasWkn("A0HGV0"), hasTicker(null), //
                        hasName("iShares PLC-MSCI Wo.UC.ETF DIS Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2015-06-08T00:00"), hasShares(16.00), //
                        hasSource("Verkauf03.txt"), //
                        hasNote("Ord.-Nr.: 71871368321 / 001"), //
                        hasAmount("EUR", 535.52), hasGrossValue("EUR", 535.52), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf04()
    {
        // This is a manipulated PDF debug to check if a fee refund works with a
        // security with foreign currency, as well as with fees in foreign
        // currency. If there are problems here, this can also be deleted and
        // replaced with a correct PDF debug.

        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "Verkauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0007472060"), hasWkn("747206"), hasTicker(null), //
                        hasName("Wirecard AG Inhaber-Aktien o.N."), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-08-25T14:34"), hasShares(3.00), //
                        hasSource("Verkauf04.txt"), //
                        hasNote("Ord.-Nr.: 123-123 | R.-Nr.: XXXXXXXXXXXXXXXX"), //
                        hasAmount("EUR", (2.90 / 1.222500)), hasGrossValue("EUR", (2.90 / 1.222500)), //
                        hasForexGrossValue("USD", 2.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2020-08-25T14:34"), hasShares(3.00), //
                        hasSource("Verkauf04.txt"), //
                        hasNote("Ord.-Nr.: 123-123 | R.-Nr.: XXXXXXXXXXXXXXXX"), //
                        hasAmount("EUR", (2.90 / 1.222500) + 8.23), hasGrossValue("EUR", (2.90 / 1.222500) + 8.23), //
                        hasForexGrossValue("USD", 2.90 + (8.23 * 1.222500)), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf04WithSecurityInEUR()
    {
        // This is a manipulated PDF debug to check if a fee refund works with a
        // security with foreign currency, as well as with fees in foreign
        // currency. If there are problems here, this can also be deleted and
        // replaced with a correct PDF debug.

        Security security = new Security("Wirecard AG Inhaber-Aktien o.N.", CurrencyUnit.EUR);
        security.setIsin("DE0007472060");
        security.setWkn("747206");

        Client client = new Client();
        client.addSecurity(security);

        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "Verkauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-08-25T14:34"), hasShares(3.00), //
                        hasSource("Verkauf04.txt"), //
                        hasNote("Ord.-Nr.: 123-123 | R.-Nr.: XXXXXXXXXXXXXXXX"), //
                        hasAmount("EUR", (2.90 / 1.222500)), hasGrossValue("EUR", (2.90 / 1.222500)), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2020-08-25T14:34"), hasShares(3.00), //
                        hasSource("Verkauf04.txt"), //
                        hasNote("Ord.-Nr.: 123-123 | R.-Nr.: XXXXXXXXXXXXXXXX"), //
                        hasAmount("EUR", (2.90 / 1.222500) + 8.23), hasGrossValue("EUR", (2.90 / 1.222500) + 8.23), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkaufMitSteuerbehandlung01()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "VerkaufMitSteuerbehandlung01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000BAY0017"), hasWkn("BAY001"), hasTicker(null), //
                        hasName("Bayer AG Namens-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-02-06T20:06"), hasShares(8.00), //
                        hasSource("VerkaufMitSteuerbehandlung01.txt"), //
                        hasNote("Ord.-Nr.: 000117637940-001 | R.-Nr.: 508104401078D295"), //
                        hasAmount("EUR", 616.18), hasGrossValue("EUR", 626.08), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 9.90))));
    }

    @Test
    public void testWertpapierVerkaufMitSteuerbehandlung02()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "VerkaufMitSteuerbehandlung02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0055631609"), hasWkn("974119"), hasTicker(null), //
                        hasName("BGF - World Gold Fund Act. Nom. A2 USD o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2018-06-05T20:19"), hasShares(1.00), //
                        hasSource("VerkaufMitSteuerbehandlung02.txt"), //
                        hasNote("Ord.-Nr.: 000062293103-001 | R.-Nr.: 455380245510DC35"), //
                        hasAmount("EUR", 10.18), hasGrossValue("EUR", 22.98), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.90 + 9.90))));
    }

    @Test
    public void testWertpapierVerkaufMitSteuerbehandlung03()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "VerkaufMitSteuerbehandlung03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B0M62Q58"), hasWkn("A0HGV0"), hasTicker(null), //
                        hasName("iShares PLC-MSCI Wo.UC.ETF DIS Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2015-07-17T11:28"), hasShares(1.00), //
                        hasSource("VerkaufMitSteuerbehandlung03.txt"), //
                        hasNote("Ord.-Nr.: 071915155121-001 | R.-Nr.: 364505682358DE85"), //
                        hasAmount("EUR", 23.34), hasGrossValue("EUR", 34.74), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 9.90 + 1.50))));
    }

    @Test
    public void testWertpapierVerkaufMitSteuerbehandlung04()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "VerkaufMitSteuerbehandlung04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0005545503"), hasWkn("554550"), hasTicker(null), //
                        hasName("Drillisch AG Inhaber-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2016-09-12T10:28"), hasShares(21.00), //
                        hasSource("VerkaufMitSteuerbehandlung04.txt"), //
                        hasNote("Ord.-Nr.: 072368222219-001 | R.-Nr.: 400775538910DB25"), //
                        hasAmount("EUR", 832.69), hasGrossValue("EUR", 849.66), //
                        hasTaxes("EUR", 4.88 + 0.26 + 0.43), hasFees("EUR", 9.90 + 1.50))));
    }

    @Test
    public void testWertpapierVerkaufMitSteuerbehandlung05()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "VerkaufMitSteuerbehandlung05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0007472060"), hasWkn("747206"), hasTicker(null), //
                        hasName("Wirecard AG Inhaber-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-08-25T14:34"), hasShares(3.00), //
                        hasSource("VerkaufMitSteuerbehandlung05.txt"), //
                        hasNote("Ord.-Nr.: 123-123 | R.-Nr.: XXXXXXXXXXXXXXXX"), //
                        hasAmount("EUR", 3.54), hasGrossValue("EUR", 3.54), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2020-08-25T14:34"), hasShares(3.00), //
                        hasSource("VerkaufMitSteuerbehandlung05.txt"), //
                        hasNote("Ord.-Nr.: 123-123 | R.-Nr.: XXXXXXXXXXXXXXXX"), //
                        hasAmount("EUR", 9.90 + 2.50), hasGrossValue("EUR", 9.90 + 2.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkaufMitSteuerbehandlung06()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "VerkaufMitSteuerbehandlung06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0007472060"), hasWkn("747206"), hasTicker(null), //
                        hasName("Wirecard AG Inhaber-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-08-25T14:34"), hasShares(3.00), //
                        hasSource("VerkaufMitSteuerbehandlung06.txt"), //
                        hasNote("Ord.-Nr.: 123-123 | R.-Nr.: XXXXXXXXXXXXXXXX"), //
                        hasAmount("EUR", 3.54), hasGrossValue("EUR", 3.54), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2020-08-25T14:34"), hasShares(3.00), //
                        hasSource("VerkaufMitSteuerbehandlung06.txt"), //
                        hasNote("Ord.-Nr.: 123-123 | R.-Nr.: XXXXXXXXXXXXXXXX"), //
                        hasAmount("EUR", 9.90 + 2.50 + 0.75), hasGrossValue("EUR", 9.90 + 2.50 + 0.75), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkaufMitSteuerbehandlung07()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "VerkaufMitSteuerbehandlung07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US86771W1053"), hasWkn("A14V1T"), hasTicker(null), //
                        hasName("Sunrun Inc. Registered Shares DL -,0001"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-12-15T20:37"), hasShares(25.00), //
                        hasSource("VerkaufMitSteuerbehandlung07.txt"), //
                        hasNote("Ord.-Nr.: 000184824005-001 | R.-Nr.: 535214798006DA55"), //
                        hasAmount("EUR", 1263.05), hasGrossValue("EUR", 1287.32), //
                        hasForexGrossValue("USD", 1573.75), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 12.90 + (13.90 / 1.222500)))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2020-12-15T00:00"), hasShares(25.00), //
                        hasSource("VerkaufMitSteuerbehandlung07.txt"), //
                        hasNote("Ref.-Nr.: 2GIG7N0VBSQ00112"), //
                        hasAmount("EUR", 9.47 + 0.52 + 0.85), hasGrossValue("EUR", 9.47 + 0.52 + 0.85), //
                        hasForexGrossValue("USD", 13.25), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkaufMitSteuerbehandlung07WithSecurityInEUR()
    {
        Security security = new Security("Sunrun Inc. Registered Shares DL -,0001", CurrencyUnit.EUR);
        security.setIsin("US86771W1053");
        security.setWkn("A14V1T");

        Client client = new Client();
        client.addSecurity(security);

        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "VerkaufMitSteuerbehandlung07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-12-15T20:37"), hasShares(25.00), //
                        hasSource("VerkaufMitSteuerbehandlung07.txt"), //
                        hasNote("Ord.-Nr.: 000184824005-001 | R.-Nr.: 535214798006DA55"), //
                        hasAmount("EUR", 1263.05), hasGrossValue("EUR", 1287.32), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 12.90 + (13.90 / 1.222500)))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2020-12-15T00:00"), hasShares(25.00), //
                        hasSource("VerkaufMitSteuerbehandlung07.txt"), //
                        hasNote("Ref.-Nr.: 2GIG7N0VBSQ00112"), //
                        hasAmount("EUR", 9.47 + 0.52 + 0.85), hasGrossValue("EUR", 9.47 + 0.52 + 0.85), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkaufMitSteuerbehandlung08()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "VerkaufMitSteuerbehandlung08.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US46267X1081"), hasWkn("A2JGN8"), hasTicker(null), //
                        hasName("Iqiyi Inc. Reg.Shs (Sp.ADRs) /7 DL-,00001"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-03-10T00:00"), hasShares(400.00), //
                        hasSource("VerkaufMitSteuerbehandlung08.txt"), //
                        hasNote("Ord.-Nr.: 000209283981 | R.-Nr.: 542587765895DFA5"), //
                        hasAmount("USD", 9903.02), hasGrossValue("USD", 10912.00), //
                        hasTaxes("USD", 958.34), hasFees("USD", 36.74 + 13.90))));
    }

    @Test
    public void testWertpapierVerkaufMitSteuerbehandlung09()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "VerkaufMitSteuerbehandlung09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US4405431069"), hasWkn("A0B9UT"), hasTicker(null), //
                        hasName("Hornbeck Offshore Svcs Inc. Registered Shares DL -,01"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2016-06-13T00:00"), hasShares(1300.00), //
                        hasSource("VerkaufMitSteuerbehandlung09.txt"), //
                        hasNote("Ord.-Nr.: 111111111111 | R.-Nr.: 111111111111A011"), //
                        hasAmount("USD", 11958.85), hasGrossValue("USD", 12012.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 38.89 + 14.26))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2016-06-13T00:00"), hasShares(1300.00), //
                        hasSource("VerkaufMitSteuerbehandlung09.txt"), //
                        hasNote("Ref.-Nr.: 11IAAA1A11XA111A"), //
                        hasAmount("USD", 3999.37), hasGrossValue("USD", 3999.37), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));
    }

    @Test
    public void testWertpapierVerkaufMitSteuerbehandlung10()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "VerkaufMitSteuerbehandlung10.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0000121014"), hasWkn("853292"), hasTicker(null), //
                        hasName("LVMH Moët Henn. L. Vuitton SE Actions Port. (C.R.) EO 0,3"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-08-11T17:27"), hasShares(20.00), //
                        hasSource("VerkaufMitSteuerbehandlung10.txt"), //
                        hasNote("Ord.-Nr.: 123-001 | R.-Nr.: 123"), //
                        hasAmount("EUR", 13249.46), hasGrossValue("EUR", 13886.00), //
                        hasTaxes("EUR", 551.67 + 30.34 + 44.13), hasFees("EUR", 7.90 + 2.50))));
    }

    @Test
    public void testWertpapierVerkaufMitSteuerbehandlung11()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "VerkaufMitSteuerbehandlung11.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US0231351067"), hasWkn("906866"), hasTicker(null), //
                        hasName("Amazon.com Inc. Registered Shares DL -,01"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-06-05T00:00"), hasShares(0.788), //
                        hasSource("VerkaufMitSteuerbehandlung11.txt"), //
                        hasNote("Ord.-Nr.: 156003283640 | R.-Nr.: 613071635311DDB5"), //
                        hasAmount("EUR", 83.42), hasGrossValue("EUR", 91.83), //
                        hasTaxes("EUR", 7.98 + 0.43), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkaufMitSteuerbehandlung12()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "VerkaufMitSteuerbehandlung12.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000BAY0017"), hasWkn("BAY001"), hasTicker(null), //
                        hasName("Bayer AG Namens-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-07-12T00:00"), hasShares(0.329), //
                        hasSource("VerkaufMitSteuerbehandlung12.txt"), //
                        hasNote("Ord.-Nr.: 117113463595 | R.-Nr.: 900515157905fwH2"), //
                        hasAmount("EUR", 16.54), hasGrossValue("EUR", 16.54), //
                        hasTaxes("EUR", 0.0), hasFees("EUR", 0.00))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2021-07-12T00:00"), hasShares(0.329), //
                        hasSource("VerkaufMitSteuerbehandlung12.txt"), //
                        hasNote("Ref.-Nr.: 0QIH75Q77DT000PI"), //
                        hasAmount("EUR", 2.03 + 0.11), hasGrossValue("EUR", 2.03 + 0.11), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkaufMitSteuerbehandlung13()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "VerkaufMitSteuerbehandlung13.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US38259P5089"), hasWkn("A0B7FY"), hasTicker(null), //
                        hasName("Google Inc. Reg. Shares Class A DL -,001"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2013-05-15T12:34"), hasShares(2.00), //
                        hasSource("VerkaufMitSteuerbehandlung13.txt"), //
                        hasNote("Ord.-Nr.: 071132136214-001 | R.-Nr.: 295713531330DE85"), //
                        hasAmount("EUR", 1366.60), hasGrossValue("EUR", 1378.00), //
                        hasTaxes("EUR", 0.0), hasFees("EUR", 9.90 + 1.50))));
    }

    @Test
    public void testWertpapierVerkaufMitSteuerbehandlung14()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "VerkaufMitSteuerbehandlung14.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BYVJRP78"), hasWkn("A2AFCZ"), hasTicker(null), //
                        hasName("iShs IV-Sust.MSCI Em.Mkts SRI Registered Shares USD o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-12-27T00:00"), hasShares(0.227), //
                        hasSource("VerkaufMitSteuerbehandlung14.txt"), //
                        hasNote("Ord.-Nr.: 123456789012 | R.-Nr.: XXXXXXXXXXXXXXXX"), //
                        hasAmount("EUR", 1.40), hasGrossValue("EUR", 1.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2023-12-27T00:00"), hasShares(0.227), //
                        hasSource("VerkaufMitSteuerbehandlung14.txt"), //
                        hasNote("Ref.-Nr.: XXXXXXXXXXXXXXXX"), //
                        hasAmount("EUR", 0.03), hasGrossValue("EUR", 0.03), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkaufMitSteuerbehandlung15()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "VerkaufMitSteuerbehandlung15.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009809566"), hasWkn("980956"), hasTicker(null), //
                        hasName("Deka-ImmobilienEuropa Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-02-06T14:25"), hasShares(157), //
                        hasSource("VerkaufMitSteuerbehandlung15.txt"), //
                        hasNote("Ord.-Nr.: 600779868664-001 | R.-Nr.: 901203817830RNX5"), //
                        hasAmount("EUR", 6945.97), hasGrossValue("EUR", 6970.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 22.33 + 2.50))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2024-02-06T00:00"), hasShares(157), //
                        hasSource("VerkaufMitSteuerbehandlung15.txt"), //
                        hasNote("Ref.-Nr.: 2AIKQNCYMSG00012"), //
                        hasAmount("EUR", 23.32), hasGrossValue("EUR", 23.32), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkaufMitSteuerbehandlung16()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "VerkaufMitSteuerbehandlung16.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BYWQWR46"), hasWkn("A2PLDF"), hasTicker(null), //
                        hasName("VanEck Vid eSports UC. ETF Reg. Shares A USD Acc. o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-07-10T17:09"), hasShares(24), //
                        hasSource("VerkaufMitSteuerbehandlung16.txt"), //
                        hasNote("Ord.-Nr.: 047475193414-001 | R.-Nr.: 619811576270VuY2"), //
                        hasAmount("EUR", 741.54), hasGrossValue("EUR", 756.84), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.90 + 9.90 + 2.50))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2023-07-10T00:00"), hasShares(24), //
                        hasSource("VerkaufMitSteuerbehandlung16.txt"), //
                        hasNote("Ref.-Nr.: 5VvxslUgtiM4592Q"), //
                        hasAmount("EUR", 23.19), hasGrossValue("EUR", 23.19), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkaufMitSteuerbehandlung17()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "VerkaufMitSteuerbehandlung17.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000VP7G5K3"), hasWkn("VP7G5K"), hasTicker(null), //
                        hasName("Vontobel Financial Products OEND.FAKTORZ 20(20/unl)3Long"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-02-22T21:26"), hasShares(17.00), //
                        hasSource("VerkaufMitSteuerbehandlung17.txt"), //
                        hasNote("Ord.-Nr.: 000380590129-001 | R.-Nr.: 635722021545D2E5"), //
                        hasAmount("EUR", 1590.93), hasGrossValue("EUR", 1605.14), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 9.90 + 2.50 + 1.81))));
    }

    @Test
    public void testWertpapierVerkaufSteuerbehandlung01()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "VerkaufSteuerbehandlung01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US38259P7069"), hasWkn("A110NH"), hasTicker(null), //
                        hasName("GOOGLE INC.C DL-,001"), //
                        hasCurrencyCode("EUR"))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2015-05-05T00:00"), hasShares(0.049), //
                                        hasSource("VerkaufSteuerbehandlung01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testDividende01()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

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
                        hasIsin("US7427181091"), hasWkn("852062"), hasTicker(null), //
                        hasName("P r oc t e r & G a m b l e C o ., T he R e gi st er ed S ha r e s o .N ."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2018-02-19T00:00"), hasShares(0.316), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Ref.-Nr.: 0SID3MHIVFT000ZN | Quartalsdividende"), //
                        hasAmount("EUR", 0.17), hasGrossValue("EUR", 0.17), //
                        hasForexGrossValue("USD", 0.22), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende01WithSecurityInEUR()
    {
        Security security = new Security("P r oc t e r & G a m b l e C o ., T he R e gi st er ed S ha r e s o .N .",
                        CurrencyUnit.EUR);
        security.setIsin("US7427181091");
        security.setWkn("852062");

        Client client = new Client();
        client.addSecurity(security);

        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);

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
                        hasDate("2018-02-19T00:00"), hasShares(0.316), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Ref.-Nr.: 0SID3MHIVFT000ZN | Quartalsdividende"), //
                        hasAmount("EUR", 0.17), hasGrossValue("EUR", 0.17), //
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
    public void testDividende02()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

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
                        hasIsin("NL0000388619"), hasWkn("A0JMQ9"), hasTicker(null), //
                        hasName("U n i l ev e r N. V . Aa nd e l e n o p n aa m E O -, 16"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-04T00:00"), hasShares(13.944), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Ref.-Nr.: 1SIFUSL13ZS0014W | Quartalsdividende"), //
                        hasAmount("EUR", 4.86 + 0.86), hasGrossValue("EUR", 4.86 + 0.86), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende03()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

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
                        hasIsin("US7960502018"), hasWkn("881823"), hasTicker(null), //
                        hasName("S am su n g El e ct r on i c s Co . Lt d. R. S h s ( N V ) Pf d( GD R 14 4 A) 1 / 2 S W5 0 0 0"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2013-04-24T00:00"), hasShares(4.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote("Ref.-Nr.: 0SI6UK467PE001G6 | Zwischendividende"), //
                        hasAmount("EUR", 10.16), hasGrossValue("EUR", 10.22), //
                        hasForexGrossValue("USD", 13.36), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.06))));
    }

    @Test
    public void testDividende03WithSecurityInEUR()
    {
        Security security = new Security(
                        "S am su n g El e ct r on i c s Co . Lt d. R. S h s ( N V ) Pf d( GD R 14 4 A) 1 / 2 S W5 0 0 0",
                        CurrencyUnit.EUR);
        security.setIsin("US7960502018");
        security.setWkn("881823");

        Client client = new Client();
        client.addSecurity(security);

        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);

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
                        hasDate("2013-04-24T00:00"), hasShares(4.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote("Ref.-Nr.: 0SI6UK467PE001G6 | Zwischendividende"), //
                        hasAmount("EUR", 10.16), hasGrossValue("EUR", 10.22), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.06), //
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
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

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
                        hasIsin("US47215P1066"), hasWkn("A112ST"), hasTicker(null), //
                        hasName("J D. co m I n c. R .S hs C l. A ( S p .A D R s ) /1 DL - , 0 0 0 02"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-05-08T00:00"), hasShares(18.102), //
                        hasSource("Dividende04.txt"), //
                        hasNote("Ref.-Nr.: 0KIJMJQ59DS0008I"), //
                        hasAmount("EUR", 9.79), hasGrossValue("EUR", 10.11), //
                        hasForexGrossValue("USD", 11.22), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.32))));
    }

    @Test
    public void testDividende04WithSecurityInEUR()
    {
        Security security = new Security("J D. co m I n c. R .S hs C l. A ( S p .A D R s ) /1 DL - , 0 0 0 02",
                        CurrencyUnit.EUR);
        security.setIsin("US47215P1066");
        security.setWkn("A112ST");

        Client client = new Client();
        client.addSecurity(security);

        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-05-08T00:00"), hasShares(18.102), //
                        hasSource("Dividende04.txt"), //
                        hasNote("Ref.-Nr.: 0KIJMJQ59DS0008I"), //
                        hasAmount("EUR", 9.79), hasGrossValue("EUR", 10.11), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.32), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende05()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

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
                        hasIsin("GB00B10RZP78"), hasWkn("A0JNE2"), hasTicker(null), //
                        hasName("U ni l e ve r P LC R e gi s t er e d Sh ar e s LS - , 0 31 1 11"), //
                        hasCurrencyCode("GBP"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-03-23T00:00"), hasShares(24.334), //
                        hasSource("Dividende05.txt"), //
                        hasNote("Ref.-Nr.: 1EIJJKJZODD001WS | Schlussdividende"), //
                        hasAmount("EUR", 10.58), hasGrossValue("EUR", 10.58), //
                        hasForexGrossValue("GBP", 9.28), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende05WithSecurityInEUR()
    {
        Security security = new Security("U ni l e ve r P LC R e gi s t er e d Sh ar e s LS - , 0 31 1 11",
                        CurrencyUnit.EUR);
        security.setIsin("GB00B10RZP78");
        security.setWkn("A0JNE2");

        Client client = new Client();
        client.addSecurity(security);

        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);

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
                        hasDate("2023-03-23T00:00"), hasShares(24.334), //
                        hasSource("Dividende05.txt"), //
                        hasNote("Ref.-Nr.: 1EIJJKJZODD001WS | Schlussdividende"), //
                        hasAmount("EUR", 10.58), hasGrossValue("EUR", 10.58), //
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
    public void testDividende06()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

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
                        hasIsin("IE00B0M62Q58"), hasWkn("A0HGV0"), hasTicker(null), //
                        hasName("i S ha r e s P L C - M SC I W o . U C . E T F D IS R eg i s t er e d S ha re s o .N ."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2015-03-23T00:00"), hasShares(11.971), //
                        hasSource("Dividende06.txt"), //
                        hasNote("Ref.-Nr.: 06I9BZMXFEV000MG"), //
                        hasAmount("EUR", 1.05), hasGrossValue("EUR", 1.05), //
                        hasForexGrossValue("USD", 1.13), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende06WithSecurityInEUR()
    {
        Security security = new Security(
                        "i S ha r e s P L C - M SC I W o . U C . E T F D IS R eg i s t er e d S ha re s o .N .",
                        CurrencyUnit.EUR);
        security.setIsin("IE00B0M62Q58");
        security.setWkn("A0HGV0");

        Client client = new Client();
        client.addSecurity(security);

        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2015-03-23T00:00"), hasShares(11.971), //
                        hasSource("Dividende06.txt"), //
                        hasNote("Ref.-Nr.: 06I9BZMXFEV000MG"), //
                        hasAmount("EUR", 1.05), hasGrossValue("EUR", 1.05), //
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
    public void testDividende07()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

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
                        hasIsin("LU0099575291"), hasWkn("921801"), hasTicker(null), //
                        hasName("F i de l it y Fd s- Te l e c o mm u ni c. Fd . Re g .S h ar e s A (G l o b . Ce r t. ) o . N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2006-08-15T00:00"), hasShares(151.00), //
                        hasSource("Dividende07.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3.02), hasGrossValue("EUR", 3.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende08()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A0F5UH1"), hasWkn("A0F5UH"), hasTicker(null), //
                        hasName("iS h. S T . G l . S e l . Di v . 1 0 0 U .E T F D E I n h ab e r- A nt e il e"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-01-16T00:00"), hasShares(322.933), //
                        hasSource("Dividende08.txt"), //
                        hasNote("Ref.-Nr.: 1SIJF5FGYDZ000KM"), //
                        hasAmount("EUR", 115.86), hasGrossValue("EUR", 115.86), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSteuerbehandlungVonDividende08()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A0F5UH1"), hasWkn("A0F5UH"), hasTicker(null), //
                        hasName("IS.S.GL.SE.D.100 U.ETF A."), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2023-01-16T00:00"), hasShares(322.933), //
                        hasSource("SteuerbehandlungVonDividende08.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 20.28 + 1.11), hasGrossValue("EUR", 20.28 + 1.11), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende08MitSteuerbehandlungVonDividende08()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende08.txt", "SteuerbehandlungVonDividende08.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A0F5UH1"), hasWkn("A0F5UH"), hasTicker(null), //
                        hasName("iS h. S T . G l . S e l . Di v . 1 0 0 U .E T F D E I n h ab e r- A nt e il e"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-01-16T00:00"), hasShares(322.933), //
                        hasSource("Dividende08.txt; SteuerbehandlungVonDividende08.txt"), //
                        hasNote("Ref.-Nr.: 1SIJF5FGYDZ000KM"), //
                        hasAmount("EUR", 94.47), hasGrossValue("EUR", 115.86), //
                        hasTaxes("EUR", 20.28 + 1.11), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende08MitSteuerbehandlungVonDividende08_SourceFilesReversed()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende08.txt", "Dividende08.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A0F5UH1"), hasWkn("A0F5UH"), hasTicker(null), //
                        hasName("IS.S.GL.SE.D.100 U.ETF A."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-01-16T00:00"), hasShares(322.933), //
                        hasSource("Dividende08.txt; SteuerbehandlungVonDividende08.txt"), //
                        hasNote("Ref.-Nr.: 1SIJF5FGYDZ000KM"), //
                        hasAmount("EUR", 94.47), hasGrossValue("EUR", 115.86), //
                        hasTaxes("EUR", 20.28 + 1.11), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende09()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A0F5UH1"), hasWkn("A0F5UH"), hasTicker(null), //
                        hasName("i S h . S T .G l .S e l. Di v .1 0 0 U. E TF D E In h a b e r- A n t ei l e"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2018-04-16T00:00"), hasShares(165.933), //
                        hasSource("Dividende09.txt"), //
                        hasNote("Ref.-Nr.: 2IID7EPALHP000IV"), //
                        hasAmount("EUR", 15.63), hasGrossValue("EUR", 15.63), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSteuerbehandlungVonDividende09()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A0F5UH1"), hasWkn("A0F5UH"), hasTicker(null), //
                        hasName("IS.S.GL.SE.D.100 U.ETF A."), //
                        hasCurrencyCode("EUR"))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2018-04-16T00:00"), hasShares(165.933), //
                                        hasSource("SteuerbehandlungVonDividende09.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testDividende09MitSteuerbehandlungVonDividende09()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende09.txt", "SteuerbehandlungVonDividende09.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A0F5UH1"), hasWkn("A0F5UH"), hasTicker(null), //
                        hasName("i S h . S T .G l .S e l. Di v .1 0 0 U. E TF D E In h a b e r- A n t ei l e"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2018-04-16T00:00"), hasShares(165.933), //
                        hasSource("Dividende09.txt; SteuerbehandlungVonDividende09.txt"), //
                        hasNote("Ref.-Nr.: 2IID7EPALHP000IV"), //
                        hasAmount("EUR", 15.63), hasGrossValue("EUR", 15.63), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende09MitSteuerbehandlungVonDividende09_SourceFilesReversed()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende09.txt", "Dividende09.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A0F5UH1"), hasWkn("A0F5UH"), hasTicker(null), //
                        hasName("IS.S.GL.SE.D.100 U.ETF A."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2018-04-16T00:00"), hasShares(165.933), //
                        hasSource("Dividende09.txt; SteuerbehandlungVonDividende09.txt"), //
                        hasNote("Ref.-Nr.: 2IID7EPALHP000IV"), //
                        hasAmount("EUR", 15.63), hasGrossValue("EUR", 15.63), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende10()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende10.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000CN2VKP9"), hasWkn("CN2VKP"), hasTicker(null), //
                        hasName("Co m me r zb a nk A G AA L P R OT E C T 0 6 . 16 P AH 3"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2016-06-23T00:00"), hasShares(10.00), //
                        hasSource("Dividende10.txt"), //
                        hasNote("Ref.-Nr.: 22IAS84ET6700189"), //
                        hasAmount("EUR", 54.08), hasGrossValue("EUR", 54.08), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende11()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende11.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US7427181091"), hasWkn("852062"), hasTicker(null), //
                        hasName("P r oc t e r & G a m b l e C o ., T he R e gi st er ed S ha r e s o .N ."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-05-19T00:00"), hasShares(7.499), //
                        hasSource("Dividende11.txt"), //
                        hasNote("Ref.-Nr.: 0IIFTGGFCJV002JX | Quartalsdividende"), //
                        hasAmount("EUR", 5.47), hasGrossValue("EUR", 5.47), //
                        hasForexGrossValue("USD", 5.93), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende11WithSecurityInEUR()
    {
        Security security = new Security("P r oc t e r & G a m b l e C o ., T he R e gi st er ed S ha r e s o .N .",
                        CurrencyUnit.EUR);
        security.setIsin("US7427181091");
        security.setWkn("852062");

        Client client = new Client();
        client.addSecurity(security);

        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende11.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-05-19T00:00"), hasShares(7.499), //
                        hasSource("Dividende11.txt"), //
                        hasNote("Ref.-Nr.: 0IIFTGGFCJV002JX | Quartalsdividende"), //
                        hasAmount("EUR", 5.47), hasGrossValue("EUR", 5.47), //
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
    public void testSteuerbehandlungVonDividende11()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende11.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US7427181091"), hasWkn("852062"), hasTicker(null), //
                        hasName("PROCTER GAMBLE"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2020-05-19T00:00"), hasShares(7.499), //
                        hasSource("SteuerbehandlungVonDividende11.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", (5.47 - 4.65) + 0.58), hasGrossValue("EUR", (5.47 - 4.65) + 0.58), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende11MitSteuerbehandlungVonDividende11()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende11.txt", "SteuerbehandlungVonDividende11.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US7427181091"), hasWkn("852062"), hasTicker(null), //
                        hasName("P r oc t e r & G a m b l e C o ., T he R e gi st er ed S ha r e s o .N ."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-05-19T00:00"), hasShares(7.499), //
                        hasSource("Dividende11.txt; SteuerbehandlungVonDividende11.txt"), //
                        hasNote("Ref.-Nr.: 0IIFTGGFCJV002JX | Quartalsdividende"), //
                        hasAmount("EUR", 4.07), hasGrossValue("EUR", 5.47), //
                        hasForexGrossValue("USD", 5.93), //
                        hasTaxes("EUR", (5.47 - 4.65) + 0.58), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende11MitSteuerbehandlungVonDividende11WithSecurityInEUR()
    {
        Security security = new Security("P r oc t e r & G a m b l e C o ., T he R e gi st er ed S ha r e s o .N .",
                        CurrencyUnit.EUR);
        security.setIsin("US7427181091");
        security.setWkn("852062");

        Client client = new Client();
        client.addSecurity(security);

        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende11.txt", "SteuerbehandlungVonDividende11.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-05-19T00:00"), hasShares(7.499), //
                        hasSource("Dividende11.txt; SteuerbehandlungVonDividende11.txt"), //
                        hasNote("Ref.-Nr.: 0IIFTGGFCJV002JX | Quartalsdividende"), //
                        hasAmount("EUR", 4.07), hasGrossValue("EUR", 5.47), //
                        hasTaxes("EUR", (5.47 - 4.65) + 0.58), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende11MitSteuerbehandlungVonDividende11_SourceFilesReversed()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende11.txt", "Dividende11.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US7427181091"), hasWkn("852062"), hasTicker(null), //
                        hasName("PROCTER GAMBLE"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-05-19T00:00"), hasShares(7.499), //
                        hasSource("Dividende11.txt; SteuerbehandlungVonDividende11.txt"), //
                        hasNote("Ref.-Nr.: 0IIFTGGFCJV002JX | Quartalsdividende"), //
                        hasAmount("EUR", 4.07), hasGrossValue("EUR", 5.47), //
                        hasTaxes("EUR", (5.47 - 4.65) + 0.58), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende11MitSteuerbehandlungVonDividende11WithSecurityInEUR_SourceFilesReversed()
    {
        Security security = new Security("P r oc t e r & G a m b l e C o ., T he R e gi st er ed S ha r e s o .N .",
                        CurrencyUnit.EUR);
        security.setIsin("US7427181091");
        security.setWkn("852062");

        Client client = new Client();
        client.addSecurity(security);

        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende11.txt", "Dividende11.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-05-19T00:00"), hasShares(7.499), //
                        hasSource("Dividende11.txt; SteuerbehandlungVonDividende11.txt"), //
                        hasNote("Ref.-Nr.: 0IIFTGGFCJV002JX | Quartalsdividende"), //
                        hasAmount("EUR", 4.07), hasGrossValue("EUR", 5.47), //
                        hasTaxes("EUR", (5.47 - 4.65) + 0.58), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende12()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende12.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B3RBWM25"), hasWkn("A1JX52"), hasTicker(null), //
                        hasName("V a n g u a r d F T S E Al l -W o rl d U . E TF R eg i st er e d S h ar e s U S D D is .o N"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-07-02T00:00"), hasShares(84.000), //
                        hasSource("Dividende12.txt"), //
                        hasNote("Ref.-Nr.: 0OIH6DFZDB600A12"), //
                        hasAmount("EUR", 40.96), hasGrossValue("EUR", 40.96), //
                        hasForexGrossValue("USD", 48.82), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende12WithSecurityInEUR()
    {
        Security security = new Security(
                        "V a n g u a r d F T S E Al l -W o rl d U . E TF R eg i st er e d S h ar e s U S D D is .o N",
                        CurrencyUnit.EUR);
        security.setIsin("IE00B3RBWM25");
        security.setWkn("A1JX52");

        Client client = new Client();
        client.addSecurity(security);

        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende12.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-07-02T00:00"), hasShares(84.000), //
                        hasSource("Dividende12.txt"), //
                        hasNote("Ref.-Nr.: 0OIH6DFZDB600A12"), //
                        hasAmount("EUR", 40.96), hasGrossValue("EUR", 40.96), //
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
    public void testSteuerbehandlungVonDividende12()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende12.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B3RBWM25"), hasWkn("A1JX52"), hasTicker(null), //
                        hasName("VANG.FTSE A.-WO.U.ETF DLD"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2021-07-02T00:00"), hasShares(84.000), //
                        hasSource("SteuerbehandlungVonDividende12.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 7.17 + 0.39), hasGrossValue("EUR", 7.17 + 0.39), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende12MitSteuerbehandlungVonDividende12()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende12.txt", "SteuerbehandlungVonDividende12.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B3RBWM25"), hasWkn("A1JX52"), hasTicker(null), //
                        hasName("V a n g u a r d F T S E Al l -W o rl d U . E TF R eg i st er e d S h ar e s U S D D is .o N"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-07-02T00:00"), hasShares(84.000), //
                        hasSource("Dividende12.txt; SteuerbehandlungVonDividende12.txt"), //
                        hasNote("Ref.-Nr.: 0OIH6DFZDB600A12"), //
                        hasAmount("EUR", 33.40), hasGrossValue("EUR", 40.96), //
                        hasForexGrossValue("USD", 48.82), //
                        hasTaxes("EUR", 7.17 + 0.39), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende12MitSteuerbehandlungVonDividende12WithSecurityInEUR()
    {
        Security security = new Security(
                        "V a n g u a r d F T S E Al l -W o rl d U . E TF R eg i st er e d S h ar e s U S D D is .o N",
                        CurrencyUnit.EUR);
        security.setIsin("IE00B3RBWM25");
        security.setWkn("A1JX52");

        Client client = new Client();
        client.addSecurity(security);

        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende12.txt", "SteuerbehandlungVonDividende12.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-07-02T00:00"), hasShares(84.000), //
                        hasSource("Dividende12.txt; SteuerbehandlungVonDividende12.txt"), //
                        hasNote("Ref.-Nr.: 0OIH6DFZDB600A12"), //
                        hasAmount("EUR", 33.40), hasGrossValue("EUR", 40.96), //
                        hasTaxes("EUR", 7.17 + 0.39), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende12MitSteuerbehandlungVonDividende12_SourceFilesReversed()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende12.txt", "Dividende12.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B3RBWM25"), hasWkn("A1JX52"), hasTicker(null), //
                        hasName("VANG.FTSE A.-WO.U.ETF DLD"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-07-02T00:00"), hasShares(84.000), //
                        hasSource("Dividende12.txt; SteuerbehandlungVonDividende12.txt"), //
                        hasNote("Ref.-Nr.: 0OIH6DFZDB600A12"), //
                        hasAmount("EUR", 33.40), hasGrossValue("EUR", 40.96), //
                        hasTaxes("EUR", 7.17 + 0.39), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende12MitSteuerbehandlungVonDividende12WithSecurityInEUR_SourceFilesReversed()
    {
        Security security = new Security(
                        "V a n g u a r d F T S E Al l -W o rl d U . E TF R eg i st er e d S h ar e s U S D D is .o N",
                        CurrencyUnit.EUR);
        security.setIsin("IE00B3RBWM25");
        security.setWkn("A1JX52");

        Client client = new Client();
        client.addSecurity(security);

        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende12.txt", "Dividende12.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-07-02T00:00"), hasShares(84.000), //
                        hasSource("Dividende12.txt; SteuerbehandlungVonDividende12.txt"), //
                        hasNote("Ref.-Nr.: 0OIH6DFZDB600A12"), //
                        hasAmount("EUR", 33.40), hasGrossValue("EUR", 40.96), //
                        hasTaxes("EUR", 7.17 + 0.39), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende13()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende13.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0008404005"), hasWkn("840400"), hasTicker(null), //
                        hasName("Al l i an z S E v i n k . N am en s - Ak t ie n o . N ."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2018-05-14T00:00"), hasShares(16.00), //
                        hasSource("Dividende13.txt"), //
                        hasNote("Ref.-Nr.: 1234567890"), //
                        hasAmount("EUR", 128.00), hasGrossValue("EUR", 128.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSteuerbehandlungVonDividende13()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende13.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0008404005"), hasWkn("840400"), hasTicker(null), //
                        hasName("ALLIANZ SE NA O.N."), //
                        hasCurrencyCode("EUR"))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2018-05-14T00:00"), hasShares(16.00), //
                                        hasSource("SteuerbehandlungVonDividende13.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testDividende13MitSteuerbehandlungVonDividende13()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende13.txt", "SteuerbehandlungVonDividende13.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0008404005"), hasWkn("840400"), hasTicker(null), //
                        hasName("Al l i an z S E v i n k . N am en s - Ak t ie n o . N ."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2018-05-14T00:00"), hasShares(16.00), //
                        hasSource("Dividende13.txt; SteuerbehandlungVonDividende13.txt"), //
                        hasNote("Ref.-Nr.: 1234567890"), //
                        hasAmount("EUR", 128.00), hasGrossValue("EUR", 128.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende13MitSteuerbehandlungVonDividende13_SourceFilesReversed()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende13.txt", "Dividende13.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0008404005"), hasWkn("840400"), hasTicker(null), //
                        hasName("ALLIANZ SE NA O.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2018-05-14T00:00"), hasShares(16.00), //
                        hasSource("Dividende13.txt; SteuerbehandlungVonDividende13.txt"), //
                        hasNote("Ref.-Nr.: 1234567890"), //
                        hasAmount("EUR", 128.00), hasGrossValue("EUR", 128.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende14()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende14.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US1266501006"), hasWkn("859034"), hasTicker(null), //
                        hasName("C VS H e a lt h Co r p. R eg is te r ed S h a re s D L -, 0 1"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-11-04T00:00"), hasShares(32.000), //
                        hasSource("Dividende14.txt"), //
                        hasNote("Ref.-Nr.: XXX1234567899ABC | Quartalsdividende"), //
                        hasAmount("EUR", 13.69), hasGrossValue("EUR", 13.69), //
                        hasForexGrossValue("USD", 16.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende14WithSecurityInEUR()
    {
        Security security = new Security("C VS H e a lt h Co r p. R eg is te r ed S h a re s D L -, 0 1",
                        CurrencyUnit.EUR);
        security.setIsin("US1266501006");
        security.setWkn("859034");

        Client client = new Client();
        client.addSecurity(security);

        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende14.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-11-04T00:00"), hasShares(32.000), //
                        hasSource("Dividende14.txt"), //
                        hasNote("Ref.-Nr.: XXX1234567899ABC | Quartalsdividende"), //
                        hasAmount("EUR", 13.69), hasGrossValue("EUR", 13.69), //
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
    public void testSteuerbehandlungVonDividende14()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende14.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US1266501006"), hasWkn("859034"), hasTicker(null), //
                        hasName("CVS HEALTH CORP. DL-,01"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2020-11-04T00:00"), hasShares(32.000), //
                        hasSource("SteuerbehandlungVonDividende14.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", (13.70 - 11.64) + 1.38 + 0.07),
                        hasGrossValue("EUR", (13.70 - 11.64) + 1.38 + 0.07), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende14MitSteuerbehandlungVonDividende14()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende14.txt", "SteuerbehandlungVonDividende14.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US1266501006"), hasWkn("859034"), hasTicker(null), //
                        hasName("C VS H e a lt h Co r p. R eg is te r ed S h a re s D L -, 0 1"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-11-04T00:00"), hasShares(32.000), //
                        hasSource("Dividende14.txt; SteuerbehandlungVonDividende14.txt"), //
                        hasNote("Ref.-Nr.: XXX1234567899ABC | Quartalsdividende"), //
                        hasAmount("EUR", 10.19), hasGrossValue("EUR", 13.70), //
                        hasForexGrossValue("USD", 16.00), //
                        hasTaxes("EUR", (13.70 - 11.64) + 1.38 + 0.07), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende14MitSteuerbehandlungVonDividende14WithSecurityInEUR()
    {
        Security security = new Security("C VS H e a lt h Co r p. R eg is te r ed S h a re s D L -, 0 1",
                        CurrencyUnit.EUR);
        security.setIsin("US1266501006");
        security.setWkn("859034");

        Client client = new Client();
        client.addSecurity(security);

        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende14.txt", "SteuerbehandlungVonDividende14.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-11-04T00:00"), hasShares(32.000), //
                        hasSource("Dividende14.txt; SteuerbehandlungVonDividende14.txt"), //
                        hasNote("Ref.-Nr.: XXX1234567899ABC | Quartalsdividende"), //
                        hasAmount("EUR", 10.19), hasGrossValue("EUR", 13.70), //
                        hasTaxes("EUR", (13.70 - 11.64) + 1.38 + 0.07), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende14MitSteuerbehandlungVonDividende14_SourceFilesReversed()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende14.txt", "Dividende14.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US1266501006"), hasWkn("859034"), hasTicker(null), //
                        hasName("CVS HEALTH CORP. DL-,01"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-11-04T00:00"), hasShares(32.000), //
                        hasSource("Dividende14.txt; SteuerbehandlungVonDividende14.txt"), //
                        hasNote("Ref.-Nr.: XXX1234567899ABC | Quartalsdividende"), //
                        hasAmount("EUR", 10.19), hasGrossValue("EUR", 13.70), //
                        hasTaxes("EUR", (13.70 - 11.64) + 1.38 + 0.07), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende14MitSteuerbehandlungVonDividende14WithSecurityInEUR_SourceFilesReversed()
    {
        Security security = new Security("C VS H e a lt h Co r p. R eg is te r ed S h a re s D L -, 0 1",
                        CurrencyUnit.EUR);
        security.setIsin("US1266501006");
        security.setWkn("859034");

        Client client = new Client();
        client.addSecurity(security);

        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende14.txt", "Dividende14.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-11-04T00:00"), hasShares(32.000), //
                        hasSource("Dividende14.txt; SteuerbehandlungVonDividende14.txt"), //
                        hasNote("Ref.-Nr.: XXX1234567899ABC | Quartalsdividende"), //
                        hasAmount("EUR", 10.19), hasGrossValue("EUR", 13.70), //
                        hasTaxes("EUR", (13.70 - 11.64) + 1.38 + 0.07), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende15()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende15.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CA8911021050"), hasWkn("914305"), hasTicker(null), //
                        hasName("To r o m on t I n du st r i e s L t d . R eg is te r ed S h ar e s o .N ."), //
                        hasCurrencyCode("CAD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-01-07T00:00"), hasShares(34.000), //
                        hasSource("Dividende15.txt"), //
                        hasNote("Ref.-Nr.: 1XABCDEF0000V | Quartalsdividende"), //
                        hasAmount("EUR", 6.71), hasGrossValue("EUR", 6.71), hasForexGrossValue("CAD", 10.54), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende15WithSecurityInEUR()
    {
        Security security = new Security("To r o m on t I n du st r i e s L t d . R eg is te r ed S h ar e s o .N .",
                        CurrencyUnit.EUR);
        security.setIsin("CA8911021050");
        security.setWkn("914305");

        Client client = new Client();
        client.addSecurity(security);

        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende15.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-01-07T00:00"), hasShares(34.000), //
                        hasSource("Dividende15.txt"), //
                        hasNote("Ref.-Nr.: 1XABCDEF0000V | Quartalsdividende"), //
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
    public void testSteuerbehandlungVonDividende15()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende15.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CA8911021050"), hasWkn("914305"), hasTicker(null), //
                        hasName("TOROMONT INDS LTD."), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2021-01-07T00:00"), hasShares(34.000), //
                        hasSource("SteuerbehandlungVonDividende15.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 6.71 - 5.03), hasGrossValue("EUR", 6.71 - 5.03), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende15MitSteuerbehandlungVonDividende15()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende15.txt", "SteuerbehandlungVonDividende15.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CA8911021050"), hasWkn("914305"), hasTicker(null), //
                        hasName("To r o m on t I n du st r i e s L t d . R eg is te r ed S h ar e s o .N ."), //
                        hasCurrencyCode("CAD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-01-07T00:00"), hasShares(34.000), //
                        hasSource("Dividende15.txt; SteuerbehandlungVonDividende15.txt"), //
                        hasNote("Ref.-Nr.: 1XABCDEF0000V | Quartalsdividende"), //
                        hasAmount("EUR", 5.03), hasGrossValue("EUR", 6.71), hasForexGrossValue("CAD", 10.54), //
                        hasTaxes("EUR", 6.71 - 5.03), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende15MitSteuerbehandlungVonDividende15WithSecurityInEUR()
    {
        Security security = new Security("To r o m on t I n du st r i e s L t d . R eg is te r ed S h ar e s o .N .",
                        CurrencyUnit.EUR);
        security.setIsin("CA8911021050");
        security.setWkn("914305");

        Client client = new Client();
        client.addSecurity(security);

        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende15.txt", "SteuerbehandlungVonDividende15.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-01-07T00:00"), hasShares(34.000), //
                        hasSource("Dividende15.txt; SteuerbehandlungVonDividende15.txt"), //
                        hasNote("Ref.-Nr.: 1XABCDEF0000V | Quartalsdividende"), //
                        hasAmount("EUR", 5.03), hasGrossValue("EUR", 6.71), //
                        hasTaxes("EUR", 6.71 - 5.03), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende15MitSteuerbehandlungVonDividende15_SourceFilesReversed()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende15.txt", "Dividende15.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CA8911021050"), hasWkn("914305"), hasTicker(null), //
                        hasName("TOROMONT INDS LTD."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-01-07T00:00"), hasShares(34.000), //
                        hasSource("Dividende15.txt; SteuerbehandlungVonDividende15.txt"), //
                        hasNote("Ref.-Nr.: 1XABCDEF0000V | Quartalsdividende"), //
                        hasAmount("EUR", 5.03), hasGrossValue("EUR", 6.71), //
                        hasTaxes("EUR", 6.71 - 5.03), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende15MitSteuerbehandlungVonDividende15WithSecurityInEUR_SourceFilesReversed()
    {
        Security security = new Security("To r o m on t I n du st r i e s L t d . R eg is te r ed S h ar e s o .N .",
                        CurrencyUnit.EUR);
        security.setIsin("CA8911021050");
        security.setWkn("914305");

        Client client = new Client();
        client.addSecurity(security);

        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende15.txt", "Dividende15.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-01-07T00:00"), hasShares(34.000), //
                        hasSource("Dividende15.txt; SteuerbehandlungVonDividende15.txt"), //
                        hasNote("Ref.-Nr.: 1XABCDEF0000V | Quartalsdividende"), //
                        hasAmount("EUR", 5.03), hasGrossValue("EUR", 6.71), //
                        hasTaxes("EUR", 6.71 - 5.03), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende16()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende16.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE0032523478"), hasWkn("778928"), hasTicker(null), //
                        hasName("i Sh s - E O C o rp Bd La r . Ca p U . ET F R e g i s te r e d S ha r e s o . N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-12-31T00:00"), hasShares(81.921), //
                        hasSource("Dividende16.txt"), //
                        hasNote("Ref.-Nr.: 1RIHIHNKLPV001JQ"), //
                        hasAmount("EUR", 18.39), hasGrossValue("EUR", 18.39), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSteuerbehandlungVonDividende16()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende16.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE0032523478"), hasWkn("778928"), hasTicker(null), //
                        hasName("ISHS-EO C.BD L.C.U.ETFEOD"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2021-12-31T00:00"), hasShares(81.921), //
                        hasSource("SteuerbehandlungVonDividende16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 4.49 + 0.24 + 0.41), hasGrossValue("EUR", 4.49 + 0.24 + 0.41), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende16MitSteuerbehandlungVonDividende16()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende16.txt", "SteuerbehandlungVonDividende16.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE0032523478"), hasWkn("778928"), hasTicker(null), //
                        hasName("i Sh s - E O C o rp Bd La r . Ca p U . ET F R e g i s te r e d S ha r e s o . N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-12-31T00:00"), hasShares(81.921), //
                        hasSource("Dividende16.txt; SteuerbehandlungVonDividende16.txt"), //
                        hasNote("Ref.-Nr.: 1RIHIHNKLPV001JQ"), //
                        hasAmount("EUR", 13.25), hasGrossValue("EUR", 18.39), //
                        hasTaxes("EUR", 4.49 + 0.24 + 0.41), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende16MitSteuerbehandlungVonDividende16_SourceFilesReversed()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende16.txt", "Dividende16.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE0032523478"), hasWkn("778928"), hasTicker(null), //
                        hasName("ISHS-EO C.BD L.C.U.ETFEOD"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-12-31T00:00"), hasShares(81.921), //
                        hasSource("Dividende16.txt; SteuerbehandlungVonDividende16.txt"), //
                        hasNote("Ref.-Nr.: 1RIHIHNKLPV001JQ"), //
                        hasAmount("EUR", 13.25), hasGrossValue("EUR", 18.39), //
                        hasTaxes("EUR", 4.49 + 0.24 + 0.41), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende17()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende17.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("NL0000009355"), hasWkn("A0JMZB"), hasTicker(null), //
                        hasName("U n il e ve r N . V . C e r t . v .A a n d e l e n E O -, 1 6"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2010-12-15T00:00"), hasShares(1900.00), //
                        hasSource("Dividende17.txt"), //
                        hasNote("Zwischendividende"), //
                        hasAmount("EUR", 335.92 + 59.28), hasGrossValue("EUR", 335.92 + 59.28), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende18MitSteuerbehandlung()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "Dividende18MitSteuerbehandlung.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0008232125"), hasWkn("823212"), hasTicker(null), //
                        hasName("De u t s c he L uf t h a n s a A G v i nk .N a me n s- A kt ie n o . N ."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2009-04-27T00:00"), hasShares(3000.00), //
                        hasSource("Dividende18MitSteuerbehandlung.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1546.13), hasGrossValue("EUR", 2100.00), //
                        hasTaxes("EUR", 525.00 + 28.87), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende19MitSteuerbehandlung()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "Dividende19MitSteuerbehandlung.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0007162000"), hasWkn("716200"), hasTicker(null), //
                        hasName("K + S A k t ie n ge s e l l sc ha f t In h a b e r -A k t i e n o . N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2008-05-15T00:00"), hasShares(80.000), //
                        hasSource("Dividende19MitSteuerbehandlung.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 126.24), hasGrossValue("EUR", 160.00), //
                        hasTaxes("EUR", 32.00 + 1.76), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende20MitSteuerbehandlung()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "Dividende20MitSteuerbehandlung.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US4592001014"), hasWkn("851399"), hasTicker(null), //
                        hasName("I nt l B u s i ne ss M a c hi n e s Co r p . R eg i s te r ed S ha r es DL - ,2 0"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2008-06-12T00:00"), hasShares(100.000), //
                        hasSource("Dividende20MitSteuerbehandlung.txt"), //
                        hasNote("Quartalsdividende"), //
                        hasAmount("EUR", 27.37), hasGrossValue("EUR", 32.20), //
                        hasForexGrossValue("USD", 50.00), //
                        hasTaxes("EUR", (7.50 / 1.552700)), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende20MitSteuerbehandlungWithSecurityInEUR()
    {
        Security security = new Security(
                        "I nt l B u s i ne ss M a c hi n e s Co r p . R eg i s te r ed S ha r es DL - ,2 0",
                        CurrencyUnit.EUR);
        security.setIsin("US4592001014");
        security.setWkn("851399");

        Client client = new Client();
        client.addSecurity(security);

        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "Dividende20MitSteuerbehandlung.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2008-06-12T00:00"), hasShares(100.000), //
                        hasSource("Dividende20MitSteuerbehandlung.txt"), //
                        hasNote("Quartalsdividende"), //
                        hasAmount("EUR", 27.37), hasGrossValue("EUR", 32.20), //
                        hasTaxes("EUR", (7.50 / 1.552700)), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende21()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende21.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US7170811035"), hasWkn("852009"), hasTicker(null), //
                        hasName("P f iz e r I n c. Re g i s te r ed S ha r e s DL - , 0 5"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-09-07T00:00"), hasShares(300.000), //
                        hasSource("Dividende21.txt"), //
                        hasNote("Ref.-Nr.: 22IJUON6JHE000NY | Quartalsdividende"), //
                        hasAmount("EUR", 114.21), hasGrossValue("EUR", 114.21), //
                        hasForexGrossValue("USD", 123.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende21WithSecurityInEUR()
    {
        Security security = new Security("P f iz e r I n c. Re g i s te r ed S ha r e s DL - , 0 5", CurrencyUnit.EUR);
        security.setIsin("US7170811035");
        security.setWkn("852009");

        Client client = new Client();
        client.addSecurity(security);

        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende21.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-09-07T00:00"), hasShares(300.000), //
                        hasSource("Dividende21.txt"), //
                        hasNote("Ref.-Nr.: 22IJUON6JHE000NY | Quartalsdividende"), //
                        hasAmount("EUR", 114.21), hasGrossValue("EUR", 114.21), //
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
    public void testSteuerbehandlungVonDividende21()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende21.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US7170811035"), hasWkn("852009"), hasTicker(null), //
                        hasName("PFIZER INC. DL-,05"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2023-09-07T00:00"), hasShares(300.000), //
                        hasSource("SteuerbehandlungVonDividende21.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", (114.21 - 97.08) + 11.42 + 0.62),
                        hasGrossValue("EUR", (114.21 - 97.08) + 11.42 + 0.62), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende21MitSteuerbehandlungVonDividende21()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende21.txt", "SteuerbehandlungVonDividende21.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US7170811035"), hasWkn("852009"), hasTicker(null), //
                        hasName("P f iz e r I n c. Re g i s te r ed S ha r e s DL - , 0 5"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-09-07T00:00"), hasShares(300.000), //
                        hasSource("Dividende21.txt; SteuerbehandlungVonDividende21.txt"), //
                        hasNote("Ref.-Nr.: 22IJUON6JHE000NY | Quartalsdividende"), //
                        hasAmount("EUR", 85.04), hasGrossValue("EUR", 114.21), //
                        hasForexGrossValue("USD", 123.00), //
                        hasTaxes("EUR", (114.21 - 97.08) + 11.42 + 0.62), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende21MitSteuerbehandlungVonDividende21WithSecurityInEUR()
    {
        Security security = new Security("P f iz e r I n c. Re g i s te r ed S ha r e s DL - , 0 5", CurrencyUnit.EUR);
        security.setIsin("US7170811035");
        security.setWkn("852009");

        Client client = new Client();
        client.addSecurity(security);

        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende21.txt", "SteuerbehandlungVonDividende21.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-09-07T00:00"), hasShares(300.000), //
                        hasSource("Dividende21.txt; SteuerbehandlungVonDividende21.txt"), //
                        hasNote("Ref.-Nr.: 22IJUON6JHE000NY | Quartalsdividende"), //
                        hasAmount("EUR", 85.04), hasGrossValue("EUR", 114.21), //
                        hasTaxes("EUR", (114.21 - 97.08) + 11.42 + 0.62), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende21MitSteuerbehandlungVonDividende21_SourceFilesReversed()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende21.txt", "Dividende21.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US7170811035"), hasWkn("852009"), hasTicker(null), //
                        hasName("PFIZER INC. DL-,05"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-09-07T00:00"), hasShares(300.000), //
                        hasSource("Dividende21.txt; SteuerbehandlungVonDividende21.txt"), //
                        hasNote("Ref.-Nr.: 22IJUON6JHE000NY | Quartalsdividende"), //
                        hasAmount("EUR", 85.04), hasGrossValue("EUR", 114.21), //
                        hasTaxes("EUR", (114.21 - 97.08) + 11.42 + 0.62), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende21MitSteuerbehandlungVonDividende21WithSecurityInEUR_SourceFilesReversed()
    {
        Security security = new Security("P f iz e r I n c. Re g i s te r ed S ha r e s DL - , 0 5", CurrencyUnit.EUR);
        security.setIsin("US7170811035");
        security.setWkn("852009");

        Client client = new Client();
        client.addSecurity(security);

        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende21.txt", "Dividende21.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-09-07T00:00"), hasShares(300.000), //
                        hasSource("Dividende21.txt; SteuerbehandlungVonDividende21.txt"), //
                        hasNote("Ref.-Nr.: 22IJUON6JHE000NY | Quartalsdividende"), //
                        hasAmount("EUR", 85.04), hasGrossValue("EUR", 114.21), //
                        hasTaxes("EUR", (114.21 - 97.08) + 11.42 + 0.62), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende22()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende22.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("BRPETRACNPR6"), hasWkn("899019"), hasTicker(null), //
                        hasName("P et r o le o B r as i l ei ro S . A . R eg . P re fe r r e d Sh ar e s o .N ."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2008-01-29T00:00"), hasShares(300.000), //
                        hasSource("Dividende22.txt"), //
                        hasNote("Zwischendividende"), //
                        hasAmount("EUR", 56.07), hasGrossValue("EUR", 56.07 + 8.41), //
                        hasTaxes("EUR", 8.41), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende23()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende23.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0005933956"), hasWkn("593395"), hasTicker(null), //
                        hasName("iS ha r es D J E U R O S TO X X 5 0 ( D E) I n h a b er -A n t ei l e"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2008-12-15T00:00"), hasShares(240.000), //
                        hasSource("Dividende23.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 49.92), hasGrossValue("EUR", 49.92), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende24()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende24.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0005557508"), hasWkn("555750"), hasTicker(null), //
                        hasName("D eu t sc h e T el e k o m A G N am e n s - A kt i e n o. N ."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2008-05-16T00:00"), hasShares(166.000), //
                        hasSource("Dividende24.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 102.16), hasGrossValue("EUR", 129.48), //
                        hasTaxes("EUR", 25.90 + 1.42), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende25()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende25.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0010405431"), hasWkn("LYX0BF"), hasTicker(null), //
                        hasName("Ly x o r F T S E A T HE X L a . C ap U. ET F A ct i o ns au P o rt e u r o . N ."), //
                        hasCurrencyCode("EUR"))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        dividend( //
                                        hasDate("2017-08-31T00:00"), hasShares(3100.000), //
                                        hasSource("Dividende25.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testSteuerbehandlungVonDividende26()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende26.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0010405431"), hasWkn("LYX0BF"), hasTicker(null), //
                        hasName("LYXOR U.E.FT.ATHEX L.CAP"), //
                        hasCurrencyCode("EUR"))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2016-07-08T00:00"), hasShares(3100.000), //
                                        hasSource("SteuerbehandlungVonDividende26.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testDividende27()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende27.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009848002"), hasWkn("984800"), hasTicker(null), //
                        hasName("D WS In te rn e t - A k t i en T y p O I n ha b er - A nt e il e"), //
                        hasCurrencyCode("EUR"))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        dividend( //
                                        hasDate("2006-10-02T00:00"), hasShares(67.000), //
                                        hasSource("Dividende27.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 1.28), hasGrossValue("EUR", 1.34), //
                                        hasTaxes("EUR", 0.06), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testDividende28()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende28.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US3703341046"), hasWkn("853862"), hasTicker(null), //
                        hasName("Ge n er a l M i l ls I nc . R e g i st er ed Sh a r e s D L - , 10"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-11-03T00:00"), hasShares(0.516), //
                        hasSource("Dividende28.txt"), //
                        hasNote("Ref.-Nr.: 19IJYGE75X60021N | Quartalsdividende"), //
                        hasAmount("EUR", 0.29), hasGrossValue("EUR", 0.29), //
                        hasForexGrossValue("USD", 0.30), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende28WithSecurityInEUR()
    {
        Security security = new Security("Ge n er a l M i l ls I nc . R e g i st er ed Sh a r e s D L - , 10", CurrencyUnit.EUR);
        security.setIsin("US3703341046");
        security.setWkn("853862");

        Client client = new Client();
        client.addSecurity(security);

        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende28.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-11-03T00:00"), hasShares(0.516), //
                        hasSource("Dividende28.txt"), //
                        hasNote("Ref.-Nr.: 19IJYGE75X60021N | Quartalsdividende"), //
                        hasAmount("EUR", 0.29), hasGrossValue("EUR", 0.29), //
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
    public void testSteuerbehandlungVonDividende28()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende28.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US3703341046"), hasWkn("853862"), hasTicker(null), //
                        hasName("GENL MILLS DL -,10"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2023-11-03T00:00"), hasShares(0.516), //
                        hasSource("SteuerbehandlungVonDividende28.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.03 + 0.04), hasGrossValue("EUR", 0.03 + 0.04), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende28MitSteuerbehandlungVonDividende28()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende28.txt", "SteuerbehandlungVonDividende28.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US3703341046"), hasWkn("853862"), hasTicker(null), //
                        hasName("Ge n er a l M i l ls I nc . R e g i st er ed Sh a r e s D L - , 10"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-11-03T00:00"), hasShares(0.516), //
                        hasSource("Dividende28.txt; SteuerbehandlungVonDividende28.txt"), //
                        hasNote("Ref.-Nr.: 19IJYGE75X60021N | Quartalsdividende"), //
                        hasAmount("EUR", 0.21), hasGrossValue("EUR", 0.28), //
                        hasForexGrossValue("USD", 0.30), //
                        hasTaxes("EUR", 0.03 + 0.04), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende28MitSteuerbehandlungVonDividende28WithSecurityInEUR()
    {
        Security security = new Security("GENL MILLS DL -,10", CurrencyUnit.EUR);
        security.setIsin("US3703341046");
        security.setWkn("853862");

        Client client = new Client();
        client.addSecurity(security);

        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende28.txt", "SteuerbehandlungVonDividende28.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-11-03T00:00"), hasShares(0.516), //
                        hasSource("Dividende28.txt; SteuerbehandlungVonDividende28.txt"), //
                        hasNote("Ref.-Nr.: 19IJYGE75X60021N | Quartalsdividende"), //
                        hasAmount("EUR", 0.21), hasGrossValue("EUR", 0.28), //
                        hasTaxes("EUR", 0.03 + 0.04), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende28MitSteuerbehandlungVonDividende28_SourceFilesReversed()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende28.txt", "Dividende28.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US3703341046"), hasWkn("853862"), hasTicker(null), //
                        hasName("GENL MILLS DL -,10"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-11-03T00:00"), hasShares(0.516), //
                        hasSource("Dividende28.txt; SteuerbehandlungVonDividende28.txt"), //
                        hasNote("Ref.-Nr.: 19IJYGE75X60021N | Quartalsdividende"), //
                        hasAmount("EUR", 0.21), hasGrossValue("EUR", 0.28), //
                        hasTaxes("EUR", 0.07), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende28MitSteuerbehandlungVonDividende28WithSecurityInEUR_SourceFilesReversed()
    {
        Security security = new Security("GENL MILLS DL -,10", CurrencyUnit.EUR);
        security.setIsin("US3703341046");
        security.setWkn("853862");

        Client client = new Client();
        client.addSecurity(security);

        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende28.txt", "Dividende28.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-11-03T00:00"), hasShares(0.516), //
                        hasSource("Dividende28.txt; SteuerbehandlungVonDividende28.txt"), //
                        hasNote("Ref.-Nr.: 19IJYGE75X60021N | Quartalsdividende"), //
                        hasAmount("EUR", 0.21), hasGrossValue("EUR", 0.28), //
                        hasTaxes("EUR", 0.03 + 0.04), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende29()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende29.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US8825081040"), hasWkn("852654"), hasTicker(null), //
                        hasName("T ex as I n s t ru m e nt s I n c. R e gi s t e re d S ha r e s DL 1"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-11-16T00:00"), hasShares(10.512), //
                        hasSource("Dividende29.txt"), //
                        hasNote("Ref.-Nr.: 20IJZBCQ1A00002D | Quartalsdividende"), //
                        hasAmount("EUR", 12.72), hasGrossValue("EUR", 12.72), //
                        hasForexGrossValue("USD", 13.67), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende29WithSecurityInEUR()
    {
        Security security = new Security("T ex as I n s t ru m e nt s I n c. R e gi s t e re d S ha r e s DL 1", CurrencyUnit.EUR);
        security.setIsin("US8825081040");
        security.setWkn("852654");

        Client client = new Client();
        client.addSecurity(security);

        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende29.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-11-16T00:00"), hasShares(10.512), //
                        hasSource("Dividende29.txt"), //
                        hasNote("Ref.-Nr.: 20IJZBCQ1A00002D | Quartalsdividende"), //
                        hasAmount("EUR", 12.72), hasGrossValue("EUR", 12.72), //
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
    public void testSteuerbehandlungVonDividende29()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende29.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US8825081040"), hasWkn("852654"), hasTicker(null), //
                        hasName("TEXAS INSTR. DL 1"), //
                        hasCurrencyCode("EUR"))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2023-11-16T00:00"), hasShares(10.512), //
                                        hasSource("SteuerbehandlungVonDividende29.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testDividende29MitSteuerbehandlungVonDividende29()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende29.txt", "SteuerbehandlungVonDividende29.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US8825081040"), hasWkn("852654"), hasTicker(null), //
                        hasName("T ex as I n s t ru m e nt s I n c. R e gi s t e re d S ha r e s DL 1"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-11-16T00:00"), hasShares(10.512), //
                        hasSource("Dividende29.txt; SteuerbehandlungVonDividende29.txt"), //
                        hasNote("Ref.-Nr.: 20IJZBCQ1A00002D | Quartalsdividende"), //
                        hasAmount("EUR", 10.81), hasGrossValue("EUR", 12.72), //
                        hasForexGrossValue("USD", 13.67), //
                        hasTaxes("EUR", 1.91), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende29MitSteuerbehandlungVonDividende29WithSecurityInEUR()
    {
        Security security = new Security("T ex as I n s t ru m e nt s I n c. R e gi s t e re d S ha r e s DL 1", CurrencyUnit.EUR);
        security.setIsin("US8825081040");
        security.setWkn("852654");

        Client client = new Client();
        client.addSecurity(security);

        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende29.txt", "SteuerbehandlungVonDividende29.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-11-16T00:00"), hasShares(10.512), //
                        hasSource("Dividende29.txt; SteuerbehandlungVonDividende29.txt"), //
                        hasNote("Ref.-Nr.: 20IJZBCQ1A00002D | Quartalsdividende"), //
                        hasAmount("EUR", 10.81), hasGrossValue("EUR", 12.72), //
                        hasTaxes("EUR", 1.91), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende29MitSteuerbehandlungVonDividende29_SourceFilesReversed()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende29.txt", "Dividende29.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US8825081040"), hasWkn("852654"), hasTicker(null), //
                        hasName("TEXAS INSTR. DL 1"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-11-16T00:00"), hasShares(10.512), //
                        hasSource("Dividende29.txt; SteuerbehandlungVonDividende29.txt"), //
                        hasNote("Ref.-Nr.: 20IJZBCQ1A00002D | Quartalsdividende"), //
                        hasAmount("EUR", 10.81), hasGrossValue("EUR", 12.72), //
                        hasTaxes("EUR", 1.91), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende29MitSteuerbehandlungVonDividende29WithSecurityInEUR_SourceFilesReversed()
    {
        Security security = new Security("TEXAS INSTR. DL 1", CurrencyUnit.EUR);
        security.setIsin("US8825081040");
        security.setWkn("852654");

        Client client = new Client();
        client.addSecurity(security);

        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende29.txt", "Dividende29.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-11-16T00:00"), hasShares(10.512), //
                        hasSource("Dividende29.txt; SteuerbehandlungVonDividende29.txt"), //
                        hasNote("Ref.-Nr.: 20IJZBCQ1A00002D | Quartalsdividende"), //
                        hasAmount("EUR", 10.81), hasGrossValue("EUR", 12.72), //
                        hasTaxes("EUR", 1.91), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende30()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende30.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("NL0011683594"), hasWkn("A2JAHJ"), hasTicker(null), //
                        hasName("Va n E c k M s t r . D M D i v i d en d . U C . E TF A a n d e l e n oo p to on d e r o. N ."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-09-13T00:00"), hasShares(2.867), //
                        hasSource("Dividende30.txt"), //
                        hasNote("Ref.-Nr.: JGF67MNG9MJ"), //
                        hasAmount("EUR", 1.15), hasGrossValue("EUR", 1.15), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSteuerbehandlungVonDividende30()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende30.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("NL0011683594"), hasWkn("A2JAHJ"), hasTicker(null), //
                        hasName("VANECK MSTR.DM DIV.UC.ETF"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2023-09-13T00:00"), hasShares(2.867), //
                        hasSource("SteuerbehandlungVonDividende30.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende30MitSteuerbehandlungVonDividende30()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende30.txt", "SteuerbehandlungVonDividende30.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("NL0011683594"), hasWkn("A2JAHJ"), hasTicker(null), //
                        hasName("Va n E c k M s t r . D M D i v i d en d . U C . E TF A a n d e l e n oo p to on d e r o. N ."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-09-13T00:00"), hasShares(2.867), //
                        hasSource("Dividende30.txt; SteuerbehandlungVonDividende30.txt"), //
                        hasNote("Ref.-Nr.: JGF67MNG9MJ"), //
                        hasAmount("EUR", 0.98), hasGrossValue("EUR", 1.15), //
                        hasTaxes("EUR", 0.17), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende30MitSteuerbehandlungVonDividende30_SourceFilesReversed()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonDividende30.txt", "Dividende30.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("NL0011683594"), hasWkn("A2JAHJ"), hasTicker(null), //
                        hasName("VANECK MSTR.DM DIV.UC.ETF"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-09-13T00:00"), hasShares(2.867), //
                        hasSource("Dividende30.txt; SteuerbehandlungVonDividende30.txt"), //
                        hasNote("Ref.-Nr.: JGF67MNG9MJ"), //
                        hasAmount("EUR", 0.98), hasGrossValue("EUR", 1.15), //
                        hasTaxes("EUR", 0.17), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende31()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende31.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US0378331005"), hasWkn("865985"), hasTicker(null), //
                        hasName("A p p le In c . R e gi s te r e d S ha r e s o . N ."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-02-19T00:00"), hasShares(35.845), //
                        hasSource("Dividende31.txt"), //
                        hasNote("Ref.-Nr.: 1ZIKR9EMLW100ATE | Quartalsdividende"), //
                        hasAmount("EUR", 7.99), hasGrossValue("EUR", 7.99), //
                        hasForexGrossValue("USD", 8.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende31WithSecurityInEUR()
    {
        Security security = new Security("A p p le In c . R e gi s te r e d S ha r e s o . N .", CurrencyUnit.EUR);
        security.setIsin("US0378331005");
        security.setWkn("865985");

        Client client = new Client();
        client.addSecurity(security);

        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende31.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-02-19T00:00"), hasShares(35.845), //
                        hasSource("Dividende31.txt"), //
                        hasNote("Ref.-Nr.: 1ZIKR9EMLW100ATE | Quartalsdividende"), //
                        hasAmount("EUR", 7.99), hasGrossValue("EUR", 7.99), //
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
    public void testSteuerbehandlungVonEinloesung01()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonEinloesung01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000CB89VM3"), hasWkn("CB89VM"), hasTicker(null), //
                        hasName("COBA CAM.PART.-ANL.09/15"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2015-09-30T00:00"), hasShares(50.00), //
                        hasSource("SteuerbehandlungVonEinloesung01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 338.50 + 18.61 + 30.46), hasGrossValue("EUR", 338.50 + 18.61 + 30.46), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSteuerbehandlungVonEinbuchung01()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungVonEinbuchung01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US38259P7069"), hasWkn("A110NH"), hasTicker(null), //
                        hasName("GOOGLE INC.C DL-,001"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2014-04-03T00:00"), hasShares(5.00), //
                        hasSource("SteuerbehandlungVonEinbuchung01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 312.56 + 17.19), hasGrossValue("EUR", 312.56 + 17.19), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testVorabpauschaleSteuerbehandlung01()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "VorabpauschaleSteuerbehandlung01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BP3QZJ36"), hasWkn("A12ATD"), hasTicker(null), //
                        hasName("ISIV-MSCI FRAN. U.ETF EOA"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2020-01-14T00:00"), hasShares(11.486), //
                        hasSource("VorabpauschaleSteuerbehandlung01.txt"), //
                        hasNote("Vorabpauschale | Ref.-Nr.: 08IFLCBPY1000J7B"), //
                        hasAmount("EUR", 0.09), hasGrossValue("EUR", 0.09), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testVorabpauschaleSteuerbehandlung02()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "VorabpauschaleSteuerbehandlung02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0779800910"), hasWkn("DBX0M2"), hasTicker(null), //
                        hasName("XTR.CSI300 SWAP 1C"), //
                        hasCurrencyCode("EUR"))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2021-01-04T00:00"), hasShares(2432.087), //
                                        hasSource("VorabpauschaleSteuerbehandlung02.txt"), //
                                        hasNote("Vorabpauschale | Ref.-Nr.: 2EIGW2KNYMC00HC8"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2021-01-04T00:00"), hasShares(2432.087), //
                                        hasSource("VorabpauschaleSteuerbehandlung02.txt"), //
                                        hasNote("Vorabpauschale | Ref.-Nr.: 2EIGW2KNYMC00HC8"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testFinanzreport01()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Finanzreport01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(24L));
        assertThat(results.size(), is(24));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2018-09-04"), hasAmount("EUR", 1500.00), //
                        hasSource("Finanzreport01.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2018-09-05"), hasAmount("EUR", 300.00), //
                        hasSource("Finanzreport01.txt"), hasNote("Übertrag"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2017-10-30"), hasAmount("EUR", 50.00), //
                        hasSource("Finanzreport01.txt"), hasNote("Auszahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2018-03-26"), hasAmount("EUR", 9.00), //
                        hasSource("Finanzreport01.txt"), hasNote("Kartenverfügung Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2018-09-17"), hasAmount("EUR", 68.88), //
                        hasSource("Finanzreport01.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2018-09-14"), hasAmount("EUR", 10000.00), //
                        hasSource("Finanzreport01.txt"), hasNote("Übertrag auf Girokonto"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2019-03-26"), hasAmount("EUR", 100.00), //
                        hasSource("Finanzreport01.txt"), hasNote("Übertrag auf Girokonto"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2018-09-17"), hasAmount("EUR", 2.18), //
                        hasSource("Finanzreport01.txt"), hasNote("Visa-Umsatz"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2019-09-28"), hasAmount("EUR", 3.94), //
                        hasSource("Finanzreport01.txt"), hasNote("Visa-Umsatz"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2019-10-24"), hasAmount("EUR", 19.21), //
                        hasSource("Finanzreport01.txt"), hasNote("Barauszahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2018-09-14"), hasAmount("EUR", 10000.00), //
                        hasSource("Finanzreport01.txt"), hasNote("Übertrag auf Girokonto"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2018-09-21"), hasAmount("EUR", 20.00), //
                        hasSource("Finanzreport01.txt"), hasNote("Übertrag"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2018-07-30"), hasAmount("EUR", 2000.00), //
                        hasSource("Finanzreport01.txt"), hasNote("Übertrag auf Tagesgeld PLUS-Konto"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2018-09-05"), hasAmount("EUR", 300.00), //
                        hasSource("Finanzreport01.txt"), hasNote("Übertrag auf Visa-Karte"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2019-07-22"), hasAmount("EUR", 71.93), //
                        hasSource("Finanzreport01.txt"), hasNote("Gutschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-01-01"), hasAmount("EUR", 5432.10), //
                        hasSource("Finanzreport01.txt"), hasNote("Bargeldeinzahlung Karte"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2014-09-29"), hasAmount("EUR", 5.90), //
                        hasSource("Finanzreport01.txt"), hasNote("Gebühr Barauszahlung"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2018-09-05"), hasAmount("EUR", 5.90), //
                        hasSource("Finanzreport01.txt"), hasNote("Entgelte"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2015-10-22"), hasAmount("EUR", 5.90), //
                        hasSource("Finanzreport01.txt"), hasNote("Gebühren/Spesen"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2018-07-27"), hasAmount("EUR", 0.45), //
                        hasSource("Finanzreport01.txt"), hasNote("Auslandsentgelt"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2018-09-28"), hasAmount("EUR", 0.70), //
                        hasSource("Finanzreport01.txt"), hasNote("Versandpauschale"))));

        // assert transaction
        assertThat(results, hasItem(interestCharge(hasDate("2015-12-31"), hasAmount("EUR", 0.07), //
                        hasSource("Finanzreport01.txt"), hasNote("Kontoabschluss Abschluss Zinsen"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2018-09-28"), hasAmount("EUR", 0.14), //
                        hasSource("Finanzreport01.txt"), hasNote("Kontoabschluss Abschluss Zinsen"))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2018-09-28"), hasAmount("EUR", 0.05), //
                        hasSource("Finanzreport01.txt"), hasNote("Kapitalertragsteuer"))));
    }

    @Test
    public void testFinanzreport02()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Finanzreport02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(11L));
        assertThat(results.size(), is(11));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2013-11-18"), hasAmount("EUR", 20.00), //
                        hasSource("Finanzreport02.txt"), hasNote("Übertrag"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2013-11-28"), hasAmount("EUR", 20.00), //
                        hasSource("Finanzreport02.txt"), hasNote("Übertrag"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2013-12-02"), hasAmount("EUR", 500.00), //
                        hasSource("Finanzreport02.txt"), hasNote("Übertrag"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2013-11-30"), hasAmount("EUR", 12.31), //
                        hasSource("Finanzreport02.txt"), hasNote("Visa-Umsatz"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2013-11-26"), hasAmount("EUR", 810.00), //
                        hasSource("Finanzreport02.txt"), hasNote("Übertrag"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2013-11-26"), hasAmount("EUR", 810.00), //
                        hasSource("Finanzreport02.txt"), hasNote("Übertrag"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2013-11-27"), hasAmount("EUR", 200.00), //
                        hasSource("Finanzreport02.txt"), hasNote("Übertrag"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2013-11-18"), hasAmount("EUR", 20.00), //
                        hasSource("Finanzreport02.txt"), hasNote("Übertrag auf Visa-Karte"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2013-11-18"), hasAmount("EUR", 20.00), //
                        hasSource("Finanzreport02.txt"), hasNote("Übertrag auf Visa-Karte"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2013-12-02"), hasAmount("EUR", 500.00), //
                        hasSource("Finanzreport02.txt"), hasNote("Übertrag auf Visa-Karte"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2013-12-08"), hasAmount("EUR", 5.90), //
                        hasSource("Finanzreport02.txt"), hasNote("Gebühren/Spesen"))));
    }

    @Test
    public void testFinanzreport03()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Finanzreport03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(13L));
        assertThat(results.size(), is(13));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2017-06-05"), hasAmount("EUR", 49.66), //
                        hasSource("Finanzreport03.txt"), hasNote("Übertrag"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2017-06-27"), hasAmount("EUR", 22.89), //
                        hasSource("Finanzreport03.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2017-06-28"), hasAmount("EUR", 264.99), //
                        hasSource("Finanzreport03.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2017-06-29"), hasAmount("EUR", 199.00), //
                        hasSource("Finanzreport03.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2017-07-03"), hasAmount("EUR", 29.90), //
                        hasSource("Finanzreport03.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2017-07-03"), hasAmount("EUR", 9.14), //
                        hasSource("Finanzreport03.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2017-07-03"), hasAmount("EUR", 8.65), //
                        hasSource("Finanzreport03.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2017-06-21"), hasAmount("EUR", 8.98), //
                        hasSource("Finanzreport03.txt"), hasNote("Visa-Umsatz"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-06-28"), hasAmount("EUR", 816.00), //
                        hasSource("Finanzreport03.txt"), hasNote("Übertrag"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-06-28"), hasAmount("EUR", 816.00), //
                        hasSource("Finanzreport03.txt"), hasNote("Übertrag"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-06-28"), hasAmount("EUR", 250.00), //
                        hasSource("Finanzreport03.txt"), hasNote("Übertrag"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-06-30"), hasAmount("EUR", 9.14), //
                        hasSource("Finanzreport03.txt"), hasNote("Visa-Kartenabrechnung"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2017-06-22"), hasAmount("EUR", 0.16), //
                        hasSource("Finanzreport03.txt"), hasNote("Auslandsentgelt"))));
    }

    @Test
    public void testFinanzreport04()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Finanzreport04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(7L));
        assertThat(results.size(), is(7));
        // new AssertImportActions().check(results, CurrencyUnit.EUR); <--
        // Multiple currencies

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2017-06-14"), hasAmount("EUR", 501.00), //
                        hasSource("Finanzreport04.txt"), hasNote("Kontoübertrag"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2017-06-13"), hasAmount("EUR", 2450.87), //
                        hasSource("Finanzreport04.txt"), hasNote("Übertrag auf Girokonto"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2017-06-13"), hasAmount("EUR", 2450.87), //
                        hasSource("Finanzreport04.txt"), hasNote("Übertrag auf Girokonto"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-06-13"), hasAmount("EUR", 9.07), //
                        hasSource("Finanzreport04.txt"), hasNote("Gutschrift aus Bonus-Sparen"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-06-09"), hasAmount("EUR", 1200.00), //
                        hasSource("Finanzreport04.txt"), hasNote("Übertrag"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-06-14"), hasAmount("USD", 554.83), //
                        hasSource("Finanzreport04.txt"), hasNote("Kontoübertrag"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2017-06-30"), hasAmount("EUR", 0.02), //
                        hasSource("Finanzreport04.txt"), hasNote("Kontoabschluss Abschluss Zinsen"))));
    }

    @Test
    public void testFinanzreport05()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Finanzreport05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(17L));
        assertThat(results.size(), is(17));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-08-01"), hasAmount("EUR", 894.30), //
                        hasSource("Finanzreport05.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-07-04"), hasAmount("EUR", 0.42), //
                        hasSource("Finanzreport05.txt"), hasNote("Wechselgeld-Sparen"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-07-04"), hasAmount("EUR", 188.58), //
                        hasSource("Finanzreport05.txt"), hasNote("Barauszahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-07-08"), hasAmount("EUR", 0.36), //
                        hasSource("Finanzreport05.txt"), hasNote("Wechselgeld-Sparen"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-07-08"), hasAmount("EUR", 124.64), //
                        hasSource("Finanzreport05.txt"), hasNote("Barauszahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-07-13"), hasAmount("EUR", 0.30), //
                        hasSource("Finanzreport05.txt"), hasNote("Wechselgeld-Sparen"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-07-12"), hasAmount("EUR", 0.30), //
                        hasSource("Finanzreport05.txt"), hasNote("Wechselgeld-Sparen"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-07-13"), hasAmount("EUR", 191.70), //
                        hasSource("Finanzreport05.txt"), hasNote("Barauszahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-07-22"), hasAmount("EUR", 0.89), //
                        hasSource("Finanzreport05.txt"), hasNote("Wechselgeld-Sparen"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-07-22"), hasAmount("EUR", 129.11), //
                        hasSource("Finanzreport05.txt"), hasNote("Barauszahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-07-28"), hasAmount("EUR", 0.70), //
                        hasSource("Finanzreport05.txt"), hasNote("Wechselgeld-Sparen"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-07-28"), hasAmount("EUR", 257.30), //
                        hasSource("Finanzreport05.txt"), hasNote("Barauszahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2016-07-11"), hasAmount("EUR", 1000.00), //
                        hasSource("Finanzreport05.txt"), hasNote("Übertrag"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2016-08-01"), hasAmount("EUR", 2.97), //
                        hasSource("Finanzreport05.txt"), hasNote("Gutschrift Wechselgeld-Sparen"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2016-07-12"), hasAmount("EUR", 191.70), //
                        hasSource("Finanzreport05.txt"), hasNote("Korrektur Barauszahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2016-07-29"), hasAmount("EUR", 894.30), //
                        hasSource("Finanzreport05.txt"), hasNote("Visa-Kartenabrechnung"))));
    }

    @Test
    public void testFinanzreport06()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Finanzreport06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(5L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2018-08-01"), hasAmount("EUR", 30.00), //
                        hasSource("Finanzreport06.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2018-07-23"), hasAmount("EUR", 0.86), //
                        hasSource("Finanzreport06.txt"), hasNote("Wechselgeld-Sparen"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2018-07-23"), hasAmount("EUR", 29.14), //
                        hasSource("Finanzreport06.txt"), hasNote("Visa-Umsatz"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2018-08-01"), hasAmount("EUR", 0.86), //
                        hasSource("Finanzreport06.txt"), hasNote("Gutschrift Wechselgeld-Sparen"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2018-07-31"), hasAmount("EUR", 30.00), //
                        hasSource("Finanzreport06.txt"), hasNote("Visa-Kartenabrechnung"))));
    }

    @Test
    public void testFinanzreport07()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Finanzreport07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2023-01-31"), hasAmount("EUR", 1.90), //
                        hasSource("Finanzreport07.txt"), hasNote("Kontoführungsentgelt"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2023-01-31"), hasAmount("EUR", 4.90), //
                        hasSource("Finanzreport07.txt"), hasNote("Entgelte"))));
    }

    @Test
    public void testWertpapierVerwahrentgelt01()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verwahrentgelt01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("A0S9GB"), hasTicker(null), //
                        hasName("Xetra Gold"), //
                        hasCurrencyCode("EUR"))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2020-01-06T00:00"), hasShares(0), //
                        hasSource("Verwahrentgelt01.txt"), //
                        hasNote("Verwahrentgelt Xetra Gold (0,0298 %)"), //
                        hasAmount("EUR", 0.01), hasGrossValue("EUR", 0.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerwahrentgelt02()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verwahrentgelt02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("A0S9GB"), hasTicker(null), //
                        hasName("Xetra Gold"), //
                        hasCurrencyCode("EUR"))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2019-05-06T00:00"), hasShares(0), //
                        hasSource("Verwahrentgelt02.txt"), //
                        hasNote("Verwahrentgelt Xetra Gold (0,0298 %)"), //
                        hasAmount("EUR", 123.45), hasGrossValue("EUR", 123.45), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSteuerbehandlungOhneDividende01()
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "SteuerbehandlungOhneDividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US7561091049"), hasWkn("899744"), hasTicker(null), //
                        hasName("REALTY INC. CORP. DL 1"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2023-07-18T00:00"), hasShares(70), //
                        hasSource("SteuerbehandlungOhneDividende01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 15.90 - 11.84), hasGrossValue("EUR", 15.90 - 11.84), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testCheckIfSellWithTwoBuyTaxesTransactionsOnTheSameDate()
    {
        // @formatter:off
        // {
        // "2013-05-15T00":00={
        //                 "Google Inc. Reg. Shares Class A DL -",
        //                    001=[
        //                           15.05.2013 Verkauf EUR 1.366, 60 Google Inc. Reg. Shares Class A DL -, 001 VerkaufMitSteuerbehandlung13.txt,
        //                           15.05.2013 Steuern EUR 0,00 Google Inc. Reg. Shares Class A DL -, 001 VerkaufMitSteuerbehandlung13.txt
        //                         ]
        //                      },
        //  "2022-10-04T00":00={
        //                 "BASF SE Namens-Aktien o.N.="[
        //                    04.10.2022 Steuern EUR 0,00 BASF SE Namens-Aktien o.N. KaufMitSteuerbehandlung13.txt,
        //                    04.10.2022 Steuern EUR 0,00 BASF SE Namens-Aktien o.N. KaufMitSteuerbehandlung14.txt
        //                  ]
        // }
        // @formatter:off
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KaufMitSteuerbehandlung13.txt",
                        "KaufMitSteuerbehandlung14.txt", "VerkaufMitSteuerbehandlung13.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(3L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000BASF111"), hasWkn("BASF11"), hasTicker(null), //
                        hasName("BASF SE Namens-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US38259P5089"), hasWkn("A0B7FY"), hasTicker(null), //
                        hasName("Google Inc. Reg. Shares Class A DL -,001"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2013-05-15T12:34"), hasShares(2.00), //
                        hasSource("VerkaufMitSteuerbehandlung13.txt"), //
                        hasNote("Ord.-Nr.: 071132136214-001 | R.-Nr.: 295713531330DE85"), //
                        hasAmount("EUR", 1366.60), hasGrossValue("EUR", 1378.00), //
                        hasTaxes("EUR", 0.0), hasFees("EUR", 9.90 + 1.50))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-10-04T00:00"), hasShares(0.565), //
                        hasSource("KaufMitSteuerbehandlung13.txt"), //
                        hasNote("Ord.-Nr.: 272803480270 | R.-Nr.: 591997149596D095"), //
                        hasAmount("EUR", 24.99), hasGrossValue("EUR", 23.68), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.36 + 0.95))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2022-10-04T00:00"), hasShares(0.565), //
                                        hasSource("KaufMitSteuerbehandlung13.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-10-04T09:02"), hasShares(5.00), //
                        hasSource("KaufMitSteuerbehandlung14.txt"), //
                        hasNote("Ord.-Nr.: 000312226831-001 | R.-Nr.: 591958998217D175"), //
                        hasAmount("EUR", 220.85), hasGrossValue("EUR", 207.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 9.90 + 0.95 + 2.50))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2022-10-04T00:00"), hasShares(5.00), //
                                        hasSource("KaufMitSteuerbehandlung14.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }
}
