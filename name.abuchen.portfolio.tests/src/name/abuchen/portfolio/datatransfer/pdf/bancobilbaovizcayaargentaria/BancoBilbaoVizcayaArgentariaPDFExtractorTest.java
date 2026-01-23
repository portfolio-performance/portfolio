package name.abuchen.portfolio.datatransfer.pdf.bancobilbaovizcayaargentaria;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.check;
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

import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.pdf.BancoBilbaoVizcayaArgentariaPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class BancoBilbaoVizcayaArgentariaPDFExtractorTest
{
    @Test
    public void testValorCompra01()
    {
        var extractor = new BancoBilbaoVizcayaArgentariaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Compra01.txt"), errors);

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
                        hasIsin("US4581401001"), hasWkn(null), hasTicker(null), //
                        hasName("ACC.INTEL CORPORATION -USD-"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-20T16:50:47"), hasShares(100), //
                        hasSource("Compra01.txt"), //
                        hasAmount("EUR", 1854.38), hasGrossValue("EUR", 1826.25), //
                        hasForexGrossValue("USD", 2090.69), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (20.00 + 8.13)))));
    }

    @Test
    public void testValorCompra01WithSecurityInEUR()
    {
        var security = new Security("ACC.INTEL CORPORATION -USD-", "EUR");
        security.setIsin("US4581401001");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new BancoBilbaoVizcayaArgentariaPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Compra01.txt"), errors);

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
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-20T16:50:47"), hasShares(100), //
                        hasSource("Compra01.txt"), //
                        hasAmount("EUR", 1854.38), hasGrossValue("EUR", 1826.25), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (20.00 + 8.13)), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testValorCompra02()
    {
        var extractor = new BancoBilbaoVizcayaArgentariaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Compra02.txt"), errors);

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
                        hasIsin("ES0113925038"), hasWkn(null), hasTicker(null), //
                        hasName("BBVA BOLSA IND. USA CUBIERTO FI"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-01-02T00:00"), hasShares(451.5550246), //
                        hasSource("Compra02.txt"), //
                        hasAmount("EUR", 15000.00), hasGrossValue("EUR", 15000.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testValorVenta01()
    {
        var extractor = new BancoBilbaoVizcayaArgentariaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Venta01.txt"), errors);

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
                        hasIsin("US0079031078"), hasWkn(null), hasTicker(null), //
                        hasName("ACC.ADVANCED MICRO DEV."), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(sale( //
                        hasDate("2025-06-17T20:18:37"), hasShares(10.00), //
                        hasSource("Venta01.txt"), //
                        hasAmount("EUR", 1081.36), hasGrossValue("EUR", 1100.71), //
                        hasForexGrossValue("USD", 1270.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (4.95 + 12.00 + 2.40)))));
    }

    @Test
    public void testValorVenta01WithSecurityInEUR()
    {
        var security = new Security("ACC.ADVANCED MICRO DEV.", "EUR");
        security.setIsin("US0079031078");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new BancoBilbaoVizcayaArgentariaPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Venta01.txt"), errors);

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
                        hasDate("2025-06-17T20:18:37"), hasShares(10.00), //
                        hasSource("Venta01.txt"), //
                        hasAmount("EUR", 1081.36), hasGrossValue("EUR", 1100.71), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (4.95 + 12.00 + 2.40)), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testValorVenta02()
    {
        var extractor = new BancoBilbaoVizcayaArgentariaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Venta02.txt"), errors);

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
                        hasIsin("ES0113925038"), hasWkn(null), hasTicker(null), //
                        hasName("BBVA BOLSA IND. USA CUBIERTO FI"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-05-28T00:00"), hasShares(483.2315919), //
                        hasSource("Venta02.txt"), //
                        hasAmount("EUR", 16000.00), hasGrossValue("EUR", 16000.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividendos01()
    {
        var extractor = new BancoBilbaoVizcayaArgentariaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividendos01.txt"), errors);

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
                        hasIsin("US8299331004"), hasWkn(null), hasTicker(null), //
                        hasName("ACC.SIRIUS XM HOLDINGS INC"), //
                        hasCurrencyCode("USD"))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-05-28T00:00"), hasShares(33.00), //
                        hasSource("Dividendos01.txt"), //
                        hasAmount("EUR", 3.57), hasGrossValue("EUR", 7.83), //
                        hasTaxes("EUR", 2.44), hasFees("EUR", 1.82))));
    }

    @Test
    public void testDividendos02()
    {
        var extractor = new BancoBilbaoVizcayaArgentariaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividendos02.txt"), errors);

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
                        hasIsin("US02079K3059"), hasWkn(null), hasTicker(null), //
                        hasName("ACC.ALPHABET INC CLASE-A"), //
                        hasCurrencyCode("USD"))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-06-16T00:00"), hasShares(6.00), //
                        hasSource("Dividendos02.txt"), //
                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 1.08), //
                        hasForexGrossValue("USD", 1.26), //
                        hasTaxes("EUR", 0.33), hasFees("EUR", 0.75))));
    }

    @Test
    public void testDividende02WithSecurityInEUR()
    {
        var security = new Security("ACC.ALPHABET INC CLASE-A", "EUR");
        security.setIsin("US02079K3059");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new BancoBilbaoVizcayaArgentariaPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividendos02.txt"), errors);

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
                        hasDate("2025-06-16T00:00"), hasShares(6.00), //
                        hasSource("Dividendos02.txt"), //
                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 1.08), //
                        hasTaxes("EUR", 0.33), hasFees("EUR", 0.75), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testComisiones01()
    {
        var extractor = new BancoBilbaoVizcayaArgentariaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Comisiones01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(4L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(8));
        new AssertImportActions().check(results, "EUR");

        // check securities
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("ACC.AMAZON -USD-"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("ACC.APPLE INC"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("ACC.ALPHABET INC CLASE-A"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("ACC.SIRIUS XM HOLDINGS INC"), //
                        hasCurrencyCode("USD"))));

        // check fees transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2025-06-30T00:00"), hasShares(0.00), //
                        hasSource("Comisiones01.txt"), //
                        hasAmount("EUR", 20.88 + 4.38), hasGrossValue("EUR", 20.88 + 4.38), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(fee( //
                        hasDate("2025-06-30T00:00"), hasShares(0.00), //
                        hasSource("Comisiones01.txt"), //
                        hasAmount("EUR", 30.00 + 6.30), hasGrossValue("EUR", 30.00 + 6.30), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(fee( //
                        hasDate("2025-06-30T00:00"), hasShares(0.00), //
                        hasSource("Comisiones01.txt"), //
                        hasAmount("EUR", 30.00 + 6.30), hasGrossValue("EUR", 30.00 + 6.30), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(fee( //
                        hasDate("2025-06-30T00:00"), hasShares(0.00), //
                        hasSource("Comisiones01.txt"), //
                        hasAmount("EUR", 30.00 + 6.30), hasGrossValue("EUR", 30.00 + 6.30), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

}
