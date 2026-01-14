package name.abuchen.portfolio.datatransfer.pdf.directasim;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.check;
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
import name.abuchen.portfolio.datatransfer.pdf.DirectaSimPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class DirectaSimPDFExtractorTest
{
    @Test
    public void testSecurityBuy01()
    {
        var extractor = new DirectaSimPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy01.txt"), errors);

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
                        hasIsin("IE00BK5BQT80"), hasWkn(null), hasTicker(null), //
                        hasName("VANGUARD FTSE ALL-WORLD UCITS"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-01-05T14:02:36"), hasShares(29.00), //
                        hasSource("Buy01.txt"), //
                        hasNote("Ordine T1673620593440"), //
                        hasAmount("EUR", 3079.29), hasGrossValue("EUR", 3074.29), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.00))));
    }

    @Test
    public void testSecurityBuy02()
    {
        var extractor = new DirectaSimPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy02.txt"), errors);

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
                        hasIsin("LU000FXZOR04"), hasWkn(null), hasTicker(null), //
                        hasName("ISHARES EUR ULTRALONG BOND"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-01-09T11:57:34"), hasShares(2900.00), //
                        hasSource("Buy02.txt"), //
                        hasNote("Ordine X4171246514720"), //
                        hasAmount("EUR", 1511.58), hasGrossValue("EUR", 1502.08), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 9.50))));
    }

    @Test
    public void testSecurityBuy03()
    {
        var extractor = new DirectaSimPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy03.txt"), errors);

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
                        hasIsin("IE00BK5BQT80"), hasWkn(null), hasTicker(null), //
                        hasName("VANGUARD FTSE ALL-WORLD UCITS"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-04-12T09:04:21"), hasShares(7.00), //
                        hasSource("Buy03.txt"), //
                        hasNote("Ordine P9417565891845"), //
                        hasAmount("EUR", 829.15), hasGrossValue("EUR", 829.15), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

    }

    @Test
    public void testSecurityBuy04()
    {
        var extractor = new DirectaSimPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy04.txt"), errors);

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
                        hasIsin("US2270461096"), hasWkn(null), hasTicker(null), //
                        hasName("CROCS INC"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-09-25T15:30:14"), hasShares(9.00), //
                        hasSource("Buy04.txt"), //
                        hasNote("Ordine c6954896449059"), //
                        hasAmount("EUR", 731.44), hasGrossValue("EUR", 722.99), //
                        hasForexGrossValue("USD", 769.77), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 8.45))));

    }

    @Test
    public void testSecurityBuy04WithSecurityInEUR()
    {
        var security = new Security("CROCS INC", "EUR");
        security.setIsin("US2270461096");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DirectaSimPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy04.txt"), errors);

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
                        hasDate("2023-09-25T15:30:14"), hasShares(9.00), //
                        hasSource("Buy04.txt"), //
                        hasNote("Ordine c6954896449059"), //
                        hasAmount("EUR", 731.44), hasGrossValue("EUR", 722.99), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 8.45), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testSecuritySell01()
    {
        var extractor = new DirectaSimPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sell01.txt"), errors);

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
                        hasIsin("IE000RHYOR04"), hasWkn(null), hasTicker(null), //
                        hasName("ISHARES EUR ULTRASHORT BOND"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-11-18T10:10:45"), hasShares(2000.00), //
                        hasSource("Sell01.txt"), //
                        hasNote("Ordine B6023301689031"), //
                        hasAmount("EUR", 10910.19), hasGrossValue("EUR", 11021.20), //
                        hasTaxes("EUR", 101.51), hasFees("EUR", 9.50))));
    }
}
