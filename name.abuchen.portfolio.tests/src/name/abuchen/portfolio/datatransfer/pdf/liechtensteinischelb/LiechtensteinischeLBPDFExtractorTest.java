package name.abuchen.portfolio.datatransfer.pdf.liechtensteinischelb;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.check;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.fee;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasExDate;
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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.inboundDelivery;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.skippedItem;
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
import name.abuchen.portfolio.datatransfer.pdf.LiechtensteinischeLBPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class LiechtensteinischeLBPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new LiechtensteinischeLBPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LI0290349492"), hasWkn("29034949"), hasTicker(null), //
                        hasName("Ant Plenum CAT Bond Fund Klasse -P CHF-"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-11-17T00:00"), hasShares(1.394011), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Auftragsnummer XXXXXXXXX"), //
                        hasAmount("CHF", 145.56), hasGrossValue("CHF", 145.56), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var extractor = new LiechtensteinischeLBPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU2498533301"), hasWkn("122593168"), hasTicker(null), //
                        hasName("Ant Schroder Inter Selec Fund SICAV BlueOrchard Emerg Mark Imp Bo Cap C CHF Hedg"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-11-20T00:00"), hasShares(.561522), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Auftragsnummer XXXXXXXXX"), //
                        hasAmount("CHF", 52.21), hasGrossValue("CHF", 52.13), //
                        hasTaxes("CHF", 0.08), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        var extractor = new LiechtensteinischeLBPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0012032048"), hasWkn("1203204"), hasTicker(null), //
                        hasName("GS Roche Holding AG"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-11-30T09:00:19"), hasShares(0.203319), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Auftragsnummer XXXXXXXXX"), //
                        hasAmount("CHF", 48.12), hasGrossValue("CHF", 48.08), //
                        hasTaxes("CHF", 0.04), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        var extractor = new LiechtensteinischeLBPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("ES0125220311"), hasWkn("978954"), hasTicker(null), //
                        hasName("Shs Acciona SA Bearer"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-10-05T09:00:16"), hasShares(0.4282), //
                        hasSource("Kauf04.txt"), //
                        hasNote("Auftragsnummer XXXXXXXXX"), //
                        hasAmount("CHF", 46.90), hasGrossValue("CHF", 46.73), //
                        hasForexGrossValue("EUR", 48.47), //
                        hasTaxes("CHF", 0.17), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierKauf04WithSecurityInCHF()
    {
        var security = new Security("Shs Acciona SA Bearer", "CHF");
        security.setIsin("ES0125220311");
        security.setWkn("978954");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new LiechtensteinischeLBPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-10-05T09:00:16"), hasShares(0.4282), //
                        hasSource("Kauf04.txt"), //
                        hasNote("Auftragsnummer XXXXXXXXX"), //
                        hasAmount("CHF", 46.90), hasGrossValue("CHF", 46.73), //
                        hasTaxes("CHF", 0.17), hasFees("CHF", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf05()
    {
        var extractor = new LiechtensteinischeLBPDFExtractor(new Client());

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
                        hasIsin("DE000BASF111"), hasWkn("11450563"), hasTicker(null), //
                        hasName("N Akt BASF SE"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-11-09T10:15:38"), hasShares(25.00), //
                        hasSource("Kauf05.txt"), //
                        hasNote("Auftragsnummer 468729311"), //
                        hasAmount("EUR", 1219.48), hasGrossValue("EUR", 1191.63), //
                        hasTaxes("EUR", 1.79), hasFees("EUR", 25.22 + 0.84))));
    }

    @Test
    public void testWertpapierKauf06()
    {
        var extractor = new LiechtensteinischeLBPDFExtractor(new Client());

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
                        hasIsin("US91282CGX39"), hasWkn("126441977"), hasTicker(null), //
                        hasName("3.875% Treasury Nts United States 2023-30.04.25"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-11-22T18:16:07"), hasShares(75.00), //
                        hasSource("Kauf06.txt"), //
                        hasNote("Auftragsnummer 343253140 | Marchzinsen 25 Tage: 20.07 USD"), //
                        hasAmount("USD", 7512.58), hasGrossValue("USD", 7501.32), //
                        hasTaxes("USD", 11.26), hasFees("USD", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var extractor = new LiechtensteinischeLBPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1313769793"), hasWkn("30270619"), hasTicker(null), //
                        hasName("Ant CANDRIAM SUSTAINABLE SICAV - Bond Euro Cap -I-"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-11-02T00:00"), hasShares(0.062), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Auftragsnummer XXXXXXXXX"), //
                        hasAmount("CHF", 55.10), hasGrossValue("CHF", 55.10), //
                        hasForexGrossValue("EUR", 57.20), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf01WithSecurityInCHF()
    {
        var security = new Security("Ant CANDRIAM SUSTAINABLE SICAV - Bond Euro Cap -I-", "CHF");
        security.setIsin("LU1313769793");
        security.setWkn("30270619");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new LiechtensteinischeLBPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-11-02T00:00"), hasShares(0.062), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Auftragsnummer XXXXXXXXX"), //
                        hasAmount("CHF", 55.10), hasGrossValue("CHF", 55.10), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        var extractor = new LiechtensteinischeLBPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH1243598427"), hasWkn("124359842"), hasTicker(null), //
                        hasName("N Akt Sandoz Grp AG"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-10-05T09:20:31"), hasShares(0.36666), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Auftragsnummer XXXXXXXXX"), //
                        hasAmount("CHF", 9.12), hasGrossValue("CHF", 9.13), //
                        hasTaxes("CHF", 0.01), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        var extractor = new LiechtensteinischeLBPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US0311621009"), hasWkn("907582"), hasTicker(null), //
                        hasName("Reg Shs Amgen Inc"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-10-04T20:45:56"), hasShares(0.164426), //
                        hasSource("Verkauf03.txt"), //
                        hasNote("Auftragsnummer 616526383"), //
                        hasAmount("CHF", 39.81), hasGrossValue("CHF", 39.87), //
                        hasForexGrossValue("USD", 43.57), //
                        hasTaxes("CHF", 0.06), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf03WithSecurityInCHF()
    {
        var security = new Security("Reg Shs Amgen Inc", "CHF");
        security.setIsin("US0311621009");
        security.setWkn("907582");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new LiechtensteinischeLBPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-10-04T20:45:56"), hasShares(0.164426), //
                        hasSource("Verkauf03.txt"), //
                        hasNote("Auftragsnummer 616526383"), //
                        hasAmount("CHF", 39.81), hasGrossValue("CHF", 39.87), //
                        hasTaxes("CHF", 0.06), hasFees("CHF", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierVerkauf04()
    {
        var extractor = new LiechtensteinischeLBPDFExtractor(new Client());

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
                        hasIsin("ZAE000015889"), hasWkn("104977"), hasTicker(null), //
                        hasName("Reg Shs Naspers Ltd -N-"), //
                        hasCurrencyCode("ZAR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-01-23T08:00:50"), hasShares(10.00), //
                        hasSource("Verkauf04.txt"), //
                        hasNote("Auftragsnummer 570384475"), //
                        hasAmount("EUR", 1773.47), hasGrossValue("EUR", 1776.93), //
                        hasForexGrossValue("ZAR", 34080.00), //
                        hasTaxes("EUR", (51.10 / 19.179094)), hasFees("EUR", (15.41 / 19.179094)))));
    }

    @Test
    public void testWertpapierVerkauf04WithSecurityInEUR()
    {
        var security = new Security("Reg Shs Naspers Ltd -N-", "EUR");
        security.setIsin("ZAE000015889");
        security.setWkn("104977");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new LiechtensteinischeLBPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-01-23T08:00:50"), hasShares(10.00), //
                        hasSource("Verkauf04.txt"), //
                        hasNote("Auftragsnummer 570384475"), //
                        hasAmount("EUR", 1773.47), hasGrossValue("EUR", 1776.93), //
                        hasTaxes("EUR", (51.10 / 19.179094)), hasFees("EUR", (15.41 / 19.179094)), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierVerkaufStorno01()
    {
        var extractor = new LiechtensteinischeLBPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "VerkaufStorno01.txt"), errors);

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
                        hasIsin("HU0000403340"), hasWkn("36909223"), hasTicker(null), //
                        hasName("2.75% Bonds Hungary 2017-22.12.26 Series D"), //
                        hasCurrencyCode("HUF"))));

        // check cancellation (Storno) transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        sale( //
                                        hasDate("2025-12-10T15:25:31"), hasShares(54000.00), //
                                        hasSource("VerkaufStorno01.txt"), //
                                        hasNote("Auftragsnummer 159818771 | Marchzinsen 355 Tage: 144'434.00 HUF"), //
                                        hasAmount("EUR", 13684.70), hasGrossValue("EUR", 13705.26), //
                                        hasForexGrossValue("HUF", 5224851.00), //
                                        hasTaxes("EUR", (8053.27 / 391.768348)), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new LiechtensteinischeLBPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US42250P1030"), hasWkn("50880191"), hasTicker(null), //
                        hasName("Reg Shs Healthpeak Pptys Inc"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-11-20T00:00"), hasExDate("2023-11-06T00:00"), //
                        hasShares(25.114744), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Auftragsnummer 623950393"), //
                        hasAmount("CHF", 5.65), hasGrossValue("CHF", 6.65), //
                        hasForexGrossValue("USD", 7.53), //
                        hasTaxes("CHF", (1.13 * 0.882477)), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende01WithSecurityInCHF()
    {
        var security = new Security("Reg Shs Healthpeak Pptys Inc", "CHF");
        security.setIsin("US42250P1030");
        security.setWkn("50880191");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new LiechtensteinischeLBPDFExtractor(client);

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
        new AssertImportActions().check(results, "CHF");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-11-20T00:00"), hasExDate("2023-11-06T00:00"), //
                        hasShares(25.114744), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Auftragsnummer 623950393"), //
                        hasAmount("CHF", 5.65), hasGrossValue("CHF", 6.65), //
                        hasTaxes("CHF", (1.13 * 0.882477)), hasFees("CHF", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("CHF");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende02()
    {
        var extractor = new LiechtensteinischeLBPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("GB0006776081"), hasWkn("400018"), hasTicker(null), //
                        hasName("Reg Shs Pearson PLC"), //
                        hasCurrencyCode("GBP"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-09-18T00:00"), hasExDate("2023-08-10T00:00"), //
                        hasShares(17.943232), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Auftragsnummer XXXXXXXXX"), //
                        hasAmount("CHF", 1.39), hasGrossValue("CHF", 1.39), //
                        hasForexGrossValue("GBP", 1.26), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende02WithSecurityInCHF()
    {
        var security = new Security("Reg Shs Pearson PLC", "CHF");
        security.setIsin("GB0006776081");
        security.setWkn("400018");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new LiechtensteinischeLBPDFExtractor(client);

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
        new AssertImportActions().check(results, "CHF");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-09-18T00:00"), hasExDate("2023-08-10T00:00"), //
                        hasShares(17.943232), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Auftragsnummer XXXXXXXXX"), //
                        hasAmount("CHF", 1.39), hasGrossValue("CHF", 1.39), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("CHF");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende03()
    {
        var extractor = new LiechtensteinischeLBPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0012214059"), hasWkn("1221405"), hasTicker(null), //
                        hasName("N Akt Holcim AG"), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-05-22T00:00"), hasExDate("2025-05-19T00:00"), //
                        hasShares(75.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote("Auftragsnummer 736150083"), //
                        hasAmount("CHF", 232.50), hasGrossValue("CHF", 232.50), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende04()
    {
        var extractor = new LiechtensteinischeLBPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("NO0010096985"), hasWkn("1245893"), hasTicker(null), //
                        hasName("N Akt Equinor ASA"), //
                        hasCurrencyCode("NOK"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-08-29T00:00"), hasExDate("2025-08-18T00:00"), //
                        hasShares(225.00), //
                        hasSource("Dividende04.txt"), //
                        hasNote("Auftragsnummer 757826517"), //
                        hasAmount("CHF", 49.78), hasGrossValue("CHF", 66.37), //
                        hasForexGrossValue("NOK", 849.15), //
                        hasTaxes("CHF", (212.29 * 0.07815708)), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende04WithSecurityInCHF()
    {
        var security = new Security("N Akt Equinor ASA", "CHF");
        security.setIsin("NO0010096985");
        security.setWkn("1245893");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new LiechtensteinischeLBPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-08-29T00:00"), hasExDate("2025-08-18T00:00"), //
                        hasShares(225.00), //
                        hasSource("Dividende04.txt"), //
                        hasNote("Auftragsnummer 757826517"), //
                        hasAmount("CHF", 49.78), hasGrossValue("CHF", 66.37), //
                        hasTaxes("CHF", (212.29 * 0.07815708)), hasFees("CHF", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("CHF");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDepoteingang01()
    {
        var extractor = new LiechtensteinischeLBPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depoteingang01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "ZAR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("ZAE000015889"), hasWkn("104977"), hasTicker(null), //
                        hasName("Reg Shs Naspers Ltd -N-"), //
                        hasCurrencyCode("ZAR"))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        inboundDelivery( //
                                        hasDate("2022-11-04T00:00"), hasShares(50.00), //
                                        hasSource("Depoteingang01.txt"), //
                                        hasNote("Auftragsnummer 556171599"), //
                                        hasAmount("ZAR", 0.00), hasGrossValue("ZAR", 0.00), //
                                        hasTaxes("ZAR", 0.00), hasFees("ZAR", 0.00)))));
    }

    @Test
    public void testFestgeld01()
    {
        var extractor = new LiechtensteinischeLBPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Festgeld01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "HUF");

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        removal(hasDate("2023-06-06T00:00"), hasAmount("HUF", 3770000.00), //
                                        hasSource("Festgeld01.txt"), hasNote("Auftragsnummer 305856191")))));
    }

    @Test
    public void testKontoauzug01()
    {
        var extractor = new LiechtensteinischeLBPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-12-31"), hasAmount("EUR", 456.60), //
                        hasSource("Kontoauszug01.txt"), hasNote("30.09.2023 - 31.12.2023"))));
    }

    @Test
    public void testKontoauzug02()
    {
        var extractor = new LiechtensteinischeLBPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2024-06-30"), //
                        hasSource("Kontoauszug02.txt"), //
                        hasNote("Auftragsnummer: 668110724 | All-in-Gebühr"), //
                        hasAmount("CHF", 215.09), hasGrossValue("CHF", 215.09), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2024-06-28"), //
                        hasSource("Kontoauszug02.txt"), //
                        hasNote("Auftragsnummer: 669550491 | Zinszahlung Callgeld"), //
                        hasAmount("CHF", 70.70), hasGrossValue("CHF", 70.70), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testKontoauzug03()
    {
        var extractor = new LiechtensteinischeLBPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2026-06-08"), hasAmount("EUR", 1364.73), //
                        hasSource("Kontoauszug03.txt"), //
                        hasNote("Auftragsnummer: 824427279 | E-Banking Kontoübertrag"))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2026-06-30"), //
                        hasSource("Kontoauszug03.txt"), //
                        hasNote("Auftragsnummer: 827183171 | Anlagegebühr"), //
                        hasAmount("EUR", 447.50), hasGrossValue("EUR", 447.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDepotauszug01()
    {
        var extractor = new LiechtensteinischeLBPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-02-01"), hasAmount("CHF", 465.86), //
                        hasSource("Depotauszug01.txt"), hasNote("Auftragsnummer: XXXXXXXXX"))));
    }
}
