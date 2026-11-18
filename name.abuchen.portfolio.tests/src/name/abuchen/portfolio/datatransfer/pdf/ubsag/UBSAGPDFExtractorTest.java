package name.abuchen.portfolio.datatransfer.pdf.ubsag;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.check;
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

import org.hamcrest.number.IsCloseTo;
import org.junit.Test;

import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.UBSAGBankingAGPDFExtractor;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;

@SuppressWarnings("nls")
public class UBSAGPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new UBSAGBankingAGPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0950674175"), hasWkn("21966836"), hasTicker(null), //
                        hasName("UBS (Lux) Fund Solutions - MSCI Emerging Markets UCITS ETF"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-03-08T15:02:13"), hasShares(450.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 4919.95), hasGrossValue("USD", 4890.60), //
                        hasTaxes("USD", 0.00), hasFees("USD", 7.34 + 22.01))));
    }

    @Test
    public void testWertpapierKauf01WithSecurityInCHF()
    {
        var security = new Security("UBS (Lux) Fund Solutions - MSCI Emerging Markets UCITS ETF", "CHF");
        security.setIsin("LU0950674175");
        security.setWkn("21966836");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new UBSAGBankingAGPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

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
                        hasDate("2022-03-08T15:02:13"), hasShares(450.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 4919.95), hasGrossValue("USD", 4890.60), //
                        hasForexGrossValue("CHF", 4532.12), //
                        hasTaxes("USD", 0.00), hasFees("USD", 7.34 + 22.01), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var extractor = new UBSAGBankingAGPDFExtractor(new Client());

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
                        hasIsin("CH0012530207"), hasWkn("1253020"), hasTicker(null), //
                        hasName("N-Akt -B- Bachem Holding AG"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-12-27"), hasShares(15.00), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Auftrags-Nr. 90586827"), //
                        hasAmount("CHF", 2213.15), hasGrossValue("CHF", 2190.00), //
                        hasTaxes("CHF", 1.65), hasFees("CHF", 20.00 + 1.50))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        var extractor = new UBSAGBankingAGPDFExtractor(new Client());

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
                        hasIsin("US17275R1023"), hasWkn("918546"), hasTicker(null), //
                        hasName("N-Akt Cisco Systems Inc."), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-12-26"), hasShares(100.00), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Auftrags-Nr. 90586823"), //
                        hasAmount("CHF", 3881.75), hasGrossValue("CHF", 3846.00), //
                        hasForexGrossValue("USD", 3851.00), //
                        hasTaxes("CHF", 5.75), hasFees("CHF", 36.55 - 6.55), //
                        check(tx -> assertThat(tx.getUnit(Unit.Type.GROSS_VALUE) //
                                        .orElseThrow(IllegalArgumentException::new) //
                                        .getExchangeRate().doubleValue(), //
                                        IsCloseTo.closeTo(0.9987, 0.000001))))));
    }

    @Test
    public void testWertpapierKauf03WithSecurityInUSD()
    {
        var security = new Security("N-Akt Cisco Systems Inc.", "USD");
        security.setIsin("US17275R1023");
        security.setWkn("918546");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new UBSAGBankingAGPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf03.txt"), errors);

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
                        hasDate("2017-12-26"), hasShares(100.00), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Auftrags-Nr. 90586823"), //
                        hasAmount("CHF", 3881.75), hasGrossValue("CHF", 3846.00), //
                        hasForexGrossValue("USD", 3851.00), //
                        hasTaxes("CHF", 5.75), hasFees("CHF", 36.55 - 6.55), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var extractor = new UBSAGBankingAGPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BX7RRJ27"), hasWkn("29134652"), hasTicker(null), //
                        hasName("UBS (Irl) ETF Plc - Factor MSCI USA Quality UCITS ETF"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-11-29T14:19:04"), hasShares(265.00), //
                        hasSource("Verkauf01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 11050.04), hasGrossValue("USD", 11116.75), //
                        hasTaxes("USD", 0.00), hasFees("USD", 16.68 + 50.03))));
    }

    @Test
    public void testWertpapierVerkauf01WithSecurityInCHF()
    {
        var security = new Security("UBS (Irl) ETF Plc - Factor MSCI USA Quality UCITS ETF", "CHF");
        security.setIsin("IE00BX7RRJ27");
        security.setWkn("29134652");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new UBSAGBankingAGPDFExtractor(client);

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
        new AssertImportActions().check(results, "USD");

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-11-29T14:19:04"), hasShares(265.00), //
                        hasSource("Verkauf01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 11050.04), hasGrossValue("USD", 11116.75), //
                        hasForexGrossValue("CHF", 10286.33), //
                        hasTaxes("USD", 0.00), hasFees("USD", 16.68 + 50.03), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        var extractor = new UBSAGBankingAGPDFExtractor(new Client());

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
                        hasIsin("CH0012032048"), hasWkn("1203204"), hasTicker("ROG"), //
                        hasName("Genussscheine Roche Holding AG"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-12-23"), hasShares(10.00), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Auftrags-Nr. 155032892"), //
                        hasAmount("CHF", 3720.18), hasGrossValue("CHF", 3763.00), //
                        hasTaxes("CHF", 2.82), hasFees("CHF", 45.15 - 5.15))));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        var extractor = new UBSAGBankingAGPDFExtractor(new Client());

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
                        hasIsin("US92343V1044"), hasWkn("1095642"), hasTicker("VZ"), //
                        hasName("N-Akt Verizon Communications Inc."), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-08-19"), hasShares(39.00), //
                        hasSource("Verkauf03.txt"), //
                        hasNote("Auftrags-Nr. 149557039"), //
                        hasAmount("CHF", 1883.08), hasGrossValue("CHF", 1946.01), //
                        hasForexGrossValue("USD", 2156.70), //
                        hasTaxes("CHF", 2.92), hasFees("CHF", 60.00 + (0.01 / 0.90231)))));
    }

    @Test
    public void testWertpapierVerkauf03WithSecurityInUSD()
    {
        var security = new Security("N-Akt Verizon Communications Inc.", "USD");
        security.setIsin("US92343V1044");
        security.setWkn("1095642");
        security.setTickerSymbol("VZ");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new UBSAGBankingAGPDFExtractor(client);

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
                        hasDate("2021-08-19"), hasShares(39.00), //
                        hasSource("Verkauf03.txt"), //
                        hasNote("Auftrags-Nr. 149557039"), //
                        hasAmount("CHF", 1883.08), hasGrossValue("CHF", 1946.01), //
                        hasForexGrossValue("USD", 2156.70), //
                        hasTaxes("CHF", 2.92), hasFees("CHF", 60.00 + (0.01 / 0.90231)), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierVerkauf04()
    {
        var extractor = new UBSAGBankingAGPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0010570767"), hasWkn("1057076"), hasTicker("LISP"), //
                        hasName("CHOCOLADEFABRIKEN LINDT & SPRUENGLI AG PARTIZIPATIONSSCHEINE"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-05-10"), hasShares(1.00), //
                        hasSource("Verkauf04.txt"), //
                        hasNote("Auftrags-Nr. 6820800"), //
                        hasAmount("CHF", 36.90), hasGrossValue("CHF", 36.90), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf05()
    {
        var extractor = new UBSAGBankingAGPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0267291224"), hasWkn("26729122"), hasTicker(null), //
                        hasName("N-AKT SUNRISE COMMUNICATIONS GROUP AG"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-04-12"), hasShares(73.00), //
                        hasSource("Verkauf05.txt"), //
                        hasNote("Auftrags-Nr. 8583497"), //
                        hasAmount("CHF", 8030.00), hasGrossValue("CHF", 8030.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf06()
    {
        var extractor = new UBSAGBankingAGPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US92556V1061"), hasWkn("58198423"), hasTicker("VTRSV"), //
                        hasName("N-AKT VIATRIS INC"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-01-04"), hasShares(0.167), //
                        hasSource("Verkauf06.txt"), //
                        hasNote("Auftrags-Nr. 6615705"), //
                        hasAmount("CHF", 2.63), hasGrossValue("CHF", 2.63), //
                        hasForexGrossValue("USD", 3.04), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf06WithSecurityInCHF()
    {
        var security = new Security("N-AKT VIATRIS INC", "CHF");
        security.setIsin("US92556V1061");
        security.setWkn("58198423");
        security.setTickerSymbol("VTRSV");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new UBSAGBankingAGPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf06.txt"), errors);

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
                        hasDate("2020-01-04"), hasShares(0.167), //
                        hasSource("Verkauf06.txt"), //
                        hasNote("Auftrags-Nr. 6615705"), //
                        hasAmount("CHF", 2.63), hasGrossValue("CHF", 2.63), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new UBSAGBankingAGPDFExtractor(new Client());

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
                        hasIsin("US6541061031"), hasWkn("957150"), hasTicker("NKE"), //
                        hasName("AKT -B- NIKE INC."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-12-28T00:00"), hasExDate("2021-12-03T00:00"), //
                        hasShares(20.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Auftrags-Nr. 3256654"), //
                        hasAmount("CHF", 3.85), hasGrossValue("CHF", 5.50), //
                        hasForexGrossValue("USD", 6.10), //
                        hasTaxes("CHF", 1.65), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende01WithSecurityInCHF()
    {
        var security = new Security("AKT -B- NIKE INC.", "CHF");
        security.setIsin("US6541061031");
        security.setWkn("957150");
        security.setTickerSymbol("NKE");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new UBSAGBankingAGPDFExtractor(client);

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
                        hasDate("2021-12-28T00:00"), hasExDate("2021-12-03T00:00"), //
                        hasShares(20.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Auftrags-Nr. 3256654"), //
                        hasAmount("CHF", 3.85), hasGrossValue("CHF", 5.50), //
                        hasTaxes("CHF", 1.65), hasFees("CHF", 0.00), //
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
        var extractor = new UBSAGBankingAGPDFExtractor(new Client());

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
                        hasIsin("GB00BP6MXD84"), hasWkn("115606002"), hasTicker("SHEL"), //
                        hasName("N-AKT SHELL PLC"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-12-20T00:00"), hasExDate("2023-11-16T00:00"), //
                        hasShares(546.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Auftrags-Nr. 4083256 | FX-Marge: 2.62 CHF"), //
                        hasAmount("CHF", 155.64), hasGrossValue("CHF", 155.64), //
                        hasForexGrossValue("EUR", 167.62), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende02WithSecurityInCHF()
    {
        var security = new Security("N-AKT SHELL PLC", "CHF");
        security.setIsin("GB00BP6MXD84");
        security.setWkn("115606002");
        security.setTickerSymbol("SHEL");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new UBSAGBankingAGPDFExtractor(client);

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
                        hasDate("2023-12-20T00:00"), hasExDate("2023-11-16T00:00"), //
                        hasShares(546.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Auftrags-Nr. 4083256 | FX-Marge: 2.62 CHF"), //
                        hasAmount("CHF", 155.64), hasGrossValue("CHF", 155.64), //
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
    public void testDepotAccountFee01()
    {
        var extractor = new UBSAGBankingAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotgebuehren01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2021-06-30"), //
                        hasSource("Depotgebuehren01.txt"), //
                        hasNote("Abrechnung 01.04.2021 - 30.06.2021"), //
                        hasAmount("CHF", 101.82), hasGrossValue("CHF", 101.82), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }
}
