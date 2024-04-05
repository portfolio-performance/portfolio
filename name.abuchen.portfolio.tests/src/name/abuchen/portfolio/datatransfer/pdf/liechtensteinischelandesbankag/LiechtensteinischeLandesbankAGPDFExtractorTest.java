package name.abuchen.portfolio.datatransfer.pdf.liechtensteinischelandesbankag;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.check;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
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
import name.abuchen.portfolio.datatransfer.pdf.LiechtensteinischeLandesbankAGPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;

@SuppressWarnings("nls")
public class LiechtensteinischeLandesbankAGPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        LiechtensteinischeLandesbankAGPDFExtractor extractor = new LiechtensteinischeLandesbankAGPDFExtractor(new Client());

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
                        hasIsin("LI0290349492"), hasWkn("29034949"), hasTicker(null), //
                        hasName("Ant Plenum CAT Bond Fund Klasse -P CHF-"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-11-22T00:00"), hasShares(1.394011), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Auftragsnummer XXXXXXXXX"), //
                        hasAmount("CHF", 145.56), hasGrossValue("CHF", 145.56), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        LiechtensteinischeLandesbankAGPDFExtractor extractor = new LiechtensteinischeLandesbankAGPDFExtractor(new Client());

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
                        hasIsin("LU2498533301"), hasWkn("122593168"), hasTicker(null), //
                        hasName("Ant Schroder Inter Selec Fund SICAV BlueOrchard Emerg Mark Imp Bo Cap C CHF Hedg"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-11-23T00:00"), hasShares(.561522), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Auftragsnummer XXXXXXXXX"), //
                        hasAmount("CHF", 52.21), hasGrossValue("CHF", 52.13), //
                        hasTaxes("CHF", 0.08), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        LiechtensteinischeLandesbankAGPDFExtractor extractor = new LiechtensteinischeLandesbankAGPDFExtractor(new Client());

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
                        hasIsin("CH0012032048"), hasWkn("1203204"), hasTicker(null), //
                        hasName("GS Roche Holding AG"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-12-04T00:00"), hasShares(0.203319), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Auftragsnummer XXXXXXXXX"), //
                        hasAmount("CHF", 48.12), hasGrossValue("CHF", 48.08), //
                        hasTaxes("CHF", 0.04), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        LiechtensteinischeLandesbankAGPDFExtractor extractor = new LiechtensteinischeLandesbankAGPDFExtractor(new Client());

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
                        hasIsin("ES0125220311"), hasWkn("978954"), hasTicker(null), //
                        hasName("Shs Acciona SA Bearer"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-10-09T00:00"), hasShares(0.4282), //
                        hasSource("Kauf04.txt"), //
                        hasNote("Auftragsnummer XXXXXXXXX"), //
                        hasAmount("CHF", 46.90), hasGrossValue("CHF", 46.73), //
                        hasForexGrossValue("EUR", 48.47), //
                        hasTaxes("CHF", 0.17), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierKauf04WithSecurityInCHF()
    {
        Security security = new Security("Shs Acciona SA Bearer", "CHF");
        security.setIsin("ES0125220311");
        security.setWkn("978954");

        Client client = new Client();
        client.addSecurity(security);

        LiechtensteinischeLandesbankAGPDFExtractor extractor = new LiechtensteinischeLandesbankAGPDFExtractor(client);

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
                        hasDate("2023-10-09T00:00"), hasShares(0.4282), //
                        hasSource("Kauf04.txt"), //
                        hasNote("Auftragsnummer XXXXXXXXX"), //
                        hasAmount("CHF", 46.90), hasGrossValue("CHF", 46.73), //
                        hasTaxes("CHF", 0.17), hasFees("CHF", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Status s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        LiechtensteinischeLandesbankAGPDFExtractor extractor = new LiechtensteinischeLandesbankAGPDFExtractor(new Client());

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
                        hasIsin("LU1313769793"), hasWkn("30270619"), hasTicker(null), //
                        hasName("Ant CANDRIAM SUSTAINABLE SICAV - Bond Euro Cap -I-"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-11-07T00:00"), hasShares(0.062), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Auftragsnummer XXXXXXXXX"), //
                        hasAmount("CHF", 55.10), hasGrossValue("CHF", 55.10), //
                        hasForexGrossValue("EUR", 57.20), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf01WithSecurityInCHF()
    {
        Security security = new Security("Ant CANDRIAM SUSTAINABLE SICAV - Bond Euro Cap -I-", "CHF");
        security.setIsin("LU1313769793");
        security.setWkn("30270619");

        Client client = new Client();
        client.addSecurity(security);

        LiechtensteinischeLandesbankAGPDFExtractor extractor = new LiechtensteinischeLandesbankAGPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-11-07T00:00"), hasShares(0.062), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Auftragsnummer XXXXXXXXX"), //
                        hasAmount("CHF", 55.10), hasGrossValue("CHF", 55.10), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Status s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        LiechtensteinischeLandesbankAGPDFExtractor extractor = new LiechtensteinischeLandesbankAGPDFExtractor(new Client());

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
                        hasIsin("CH1243598427"), hasWkn("124359842"), hasTicker(null), //
                        hasName("N Akt Sandoz Grp AG"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-10-09T00:00"), hasShares(0.36666), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Auftragsnummer XXXXXXXXX"), //
                        hasAmount("CHF", 9.12), hasGrossValue("CHF", 9.13), //
                        hasTaxes("CHF", 0.01), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        LiechtensteinischeLandesbankAGPDFExtractor extractor = new LiechtensteinischeLandesbankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US0311621009"), hasWkn("907582"), hasTicker(null), //
                        hasName("Reg Shs Amgen Inc"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-10-06T00:00"), hasShares(0.164426), //
                        hasSource("Verkauf03.txt"), //
                        hasNote("Auftragsnummer 616526383"), //
                        hasAmount("CHF", 39.81), hasGrossValue("CHF", 39.87), //
                        hasForexGrossValue("USD", 43.57), //
                        hasTaxes("CHF", 0.06), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf03WithSecurityInCHF()
    {
        Security security = new Security("Ant CANDRIAM SUSTAINABLE SICAV - Bond Euro Cap -I-", "CHF");
        security.setIsin("LU1313769793");
        security.setWkn("30270619");

        Client client = new Client();
        client.addSecurity(security);

        LiechtensteinischeLandesbankAGPDFExtractor extractor = new LiechtensteinischeLandesbankAGPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-11-07T00:00"), hasShares(0.062), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Auftragsnummer XXXXXXXXX"), //
                        hasAmount("CHF", 55.10), hasGrossValue("CHF", 55.10), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Status s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende01()
    {
        LiechtensteinischeLandesbankAGPDFExtractor extractor = new LiechtensteinischeLandesbankAGPDFExtractor(new Client());

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
                        hasIsin("US42250P1030"), hasWkn("50880191"), hasTicker(null), //
                        hasName("Reg Shs Healthpeak Pptys Inc"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-11-20T00:00"), hasShares(25.114744), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Auftragsnummer 623950393"), //
                        hasAmount("CHF", 5.65), hasGrossValue("CHF", 6.65), //
                        hasForexGrossValue("USD", 7.53), //
                        hasTaxes("CHF", (1.13 * 0.882477)), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende01WithSecurityInCHF()
    {
        Security security = new Security("Reg Shs Healthpeak Pptys Inc", "CHF");
        security.setIsin("US42250P1030");
        security.setWkn("50880191");

        Client client = new Client();
        client.addSecurity(security);

        LiechtensteinischeLandesbankAGPDFExtractor extractor = new LiechtensteinischeLandesbankAGPDFExtractor(client);

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
                        hasDate("2023-11-20T00:00"), hasShares(25.114744), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Auftragsnummer 623950393"), //
                        hasAmount("CHF", 5.65), hasGrossValue("CHF", 6.65), //
                        hasTaxes("CHF", (1.13 * 0.882477)), hasFees("CHF", 0.00), //
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
        LiechtensteinischeLandesbankAGPDFExtractor extractor = new LiechtensteinischeLandesbankAGPDFExtractor(new Client());

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
                        hasIsin("GB0006776081"), hasWkn("400018"), hasTicker(null), //
                        hasName("Reg Shs Pearson PLC"), //
                        hasCurrencyCode("GBP"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-09-18T00:00"), hasShares(17.943232), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Auftragsnummer XXXXXXXXX"), //
                        hasAmount("CHF", 1.39), hasGrossValue("CHF", 1.39), //
                        hasForexGrossValue("GBP", 1.26), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende02WithSecurityInCHF()
    {
        Security security = new Security("Reg Shs Pearson PLC", "CHF");
        security.setIsin("GB0006776081");
        security.setWkn("400018");

        Client client = new Client();
        client.addSecurity(security);

        LiechtensteinischeLandesbankAGPDFExtractor extractor = new LiechtensteinischeLandesbankAGPDFExtractor(client);

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
                        hasDate("2023-09-18T00:00"), hasShares(17.943232), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Auftragsnummer XXXXXXXXX"), //
                        hasAmount("CHF", 1.39), hasGrossValue("CHF", 1.39), //
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
    public void testKontoauzug01()
    {
        LiechtensteinischeLandesbankAGPDFExtractor extractor = new LiechtensteinischeLandesbankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-12-31"), hasAmount("EUR", 456.60), //
                        hasSource("Kontoauszug01.txt"), hasNote("30.09.2023 - 31.12.2023"))));
    }

    @Test
    public void testDepotauszug01()
    {
        LiechtensteinischeLandesbankAGPDFExtractor extractor = new LiechtensteinischeLandesbankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-02-01"), hasAmount("CHF", 465.86), //
                        hasSource("Depotauszug01.txt"), hasNote("Auftragsnummer: XXXXXXXXX"))));
    }
}
