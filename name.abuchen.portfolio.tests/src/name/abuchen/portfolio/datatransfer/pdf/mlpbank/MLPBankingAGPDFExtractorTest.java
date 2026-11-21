package name.abuchen.portfolio.datatransfer.pdf.mlpbank;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.check;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.fee;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.feeRefund;
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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxRefund;
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
import name.abuchen.portfolio.datatransfer.pdf.MLPBankingAGPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class MLPBankingAGPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new MLPBankingAGPDFExtractor(new Client());

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
                        hasIsin("LU0106280836"), hasWkn("930920"), hasTicker(null), //
                        hasName("SAUREN GLOBAL BALANCED INHABER-ANTEILE A O.N"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-01-14T00:00"), hasShares(4.929), //
                        hasSource("Kauf01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 100.01), hasGrossValue("EUR", 100.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var extractor = new MLPBankingAGPDFExtractor(new Client());

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
                        hasIsin("DE000A0MUWS7"), hasWkn("A0MUWS"), hasTicker(null), //
                        hasName("ZINSPLUS OP INHABER-ANTEILE"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2013-10-15T00:00"), hasShares(0.826), //
                        hasSource("Kauf02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 45.12), hasGrossValue("EUR", 45.12), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        var extractor = new MLPBankingAGPDFExtractor(new Client());

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
                        hasIsin("DE000A0NEKK1"), hasWkn("A0NEKK"), hasTicker(null), //
                        hasName("HANSAGOLD INHABER-ANTEILE USD-KLASSE A"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-04T00:00"), hasShares(0.639), //
                        hasSource("Kauf03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 50.00), //
                        hasForexGrossValue("USD", 54.59), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf03WithSecurityInEUR()
    {
        var security = new Security("HANSAGOLD INHABER-ANTEILE USD-KLASSE A", "EUR");
        security.setIsin("DE000A0NEKK1");
        security.setWkn("A0NEKK");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new MLPBankingAGPDFExtractor(client);

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
        new AssertImportActions().check(results, "EUR");

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-04T00:00"), hasShares(0.639), //
                        hasSource("Kauf03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 50.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        var extractor = new MLPBankingAGPDFExtractor(new Client());

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
                        hasIsin("DE0009848119"), hasWkn("984811"), hasTicker(null), //
                        hasName("DWS TOP DIVIDENDE INHABER-ANTEILE LD"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-03-20T00:00"), hasShares(5.563), //
                        hasSource("Kauf04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 658.38), hasGrossValue("EUR", 627.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 31.37))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var extractor = new MLPBankingAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0087412390"), hasWkn("974515"), hasTicker(null), //
                        hasName("DWS CON.DJE ALPHA RENT.GLOBAL INHABER-ANTEILE LC O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-03-01T00:00"), hasShares(8.104), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Veräußerungsverlust 3,00- EUR"), //
                        hasAmount("EUR", 1100.04), hasGrossValue("EUR", 1100.04), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2022-03-03"), //
                        hasShares(8.104), //
                        hasSource("Verkauf01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.79), hasGrossValue("EUR", 0.79), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        var extractor = new MLPBankingAGPDFExtractor(new Client());

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
                        hasIsin("LU0079474960"), hasWkn("986838"), hasTicker(null), //
                        hasName("AB SICAV I-AMERICAN GROWTH PTF ACTIONS NOM. A O.N."), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-02-16T00:00"), hasShares(163.269), //
                        hasSource("Verkauf02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 23272.81), hasGrossValue("EUR", 24013.85), //
                        hasForexGrossValue("USD", 27399.80), //
                        hasTaxes("EUR", 702.41 + 38.63), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf02WithSecurityInEUR()
    {
        var security = new Security("AB SICAV I-AMERICAN GROWTH PTF ACTIONS NOM. A O.N.", "EUR");
        security.setIsin("LU0079474960");
        security.setWkn("986838");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new MLPBankingAGPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf02.txt"), errors);

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
                        hasDate("2022-02-16T00:00"), hasShares(163.269), //
                        hasSource("Verkauf02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 23272.81), hasGrossValue("EUR", 24013.85), //
                        hasTaxes("EUR", 702.41 + 38.63), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new MLPBankingAGPDFExtractor(new Client());

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
                        hasIsin("LU0244125554"), hasWkn("A0JJUS"), hasTicker(null), //
                        hasName("MULTICOOP.-GAM COMMODITY ACTIONS AU PORTEUR A O.N."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-11-22T00:00"), hasExDate("2019-11-12T00:00"), //
                        hasShares(34.942), //
                        hasSource("Dividende01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 7.55), hasGrossValue("EUR", 7.55), //
                        hasForexGrossValue("USD", 8.39), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende01WithSecurityInEUR()
    {
        var security = new Security("MULTICOOP.-GAM COMMODITY ACTIONS AU PORTEUR A O.N.", "EUR");
        security.setIsin("LU0244125554");
        security.setWkn("A0JJUS");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new MLPBankingAGPDFExtractor(client);

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
                        hasDate("2019-11-22T00:00"), hasExDate("2019-11-12T00:00"), //
                        hasShares(34.942), //
                        hasSource("Dividende01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 7.55), hasGrossValue("EUR", 7.55), //
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
        var extractor = new MLPBankingAGPDFExtractor(new Client());

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
                        hasIsin("DE000A0MUWS7"), hasWkn("A0MUWS"), hasTicker(null), //
                        hasName("ZINSPLUS OP INHABER-ANTEILE"), //
                        hasCurrencyCode("EUR"))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2017-04-18"), hasExDate("2017-04-03"), //
                        hasShares(80.043), //
                        hasSource("Dividende02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 13.61), hasGrossValue("EUR", 13.61), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDepotauszug01()
    {
        var extractor = new MLPBankingAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(5L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, "EUR");

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2022-01-03"), hasAmount("EUR", 500.00), //
                        hasSource("Depotauszug01.txt"), hasNote("Lastschrifteinr."))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2022-02-01"), hasAmount("EUR", 500.00), //
                        hasSource("Depotauszug01.txt"), hasNote("Lastschrifteinr."))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2022-03-01"), hasAmount("EUR", 500.00), //
                        hasSource("Depotauszug01.txt"), hasNote("Lastschrifteinr."))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2022-01-05"), //
                        hasSource("Depotauszug01.txt"), //
                        hasNote("Depotpreis Q4/2021"), //
                        hasAmount("EUR", 11.90), hasGrossValue("EUR", 11.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2022-01-05"), //
                        hasSource("Depotauszug01.txt"), //
                        hasNote("Verwaltungsentgelt Q4/2021"), //
                        hasAmount("EUR", 73.78), hasGrossValue("EUR", 73.78), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDepotauszug02()
    {
        var extractor = new MLPBankingAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(5L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, "EUR");

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2016-07-15"), hasAmount("EUR", 50.00), //
                        hasSource("Depotauszug02.txt"), hasNote("Lastschrifteinr."))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2016-08-15"), hasAmount("EUR", 50.00), //
                        hasSource("Depotauszug02.txt"), hasNote("Lastschrifteinr."))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2016-09-15"), hasAmount("EUR", 50.00), //
                        hasSource("Depotauszug02.txt"), hasNote("Lastschrifteinr."))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2016-07-04"), //
                        hasSource("Depotauszug02.txt"), //
                        hasNote("Vermögensdepot Q2/2016"), //
                        hasAmount("EUR", 2.27), hasGrossValue("EUR", 2.27), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2016-07-04"), //
                        hasSource("Depotauszug02.txt"), //
                        hasNote("Vermögensdepot Q2/2016"), //
                        hasAmount("EUR", 3.57), hasGrossValue("EUR", 3.57), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDepotauszug03()
    {
        var extractor = new MLPBankingAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2012-01-16"), hasAmount("EUR", 50.00), //
                        hasSource("Depotauszug03.txt"), hasNote("Lastschrifteinr."))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2012-02-15"), hasAmount("EUR", 50.00), //
                        hasSource("Depotauszug03.txt"), hasNote("Lastschrifteinr."))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2012-03-15"), hasAmount("EUR", 50.00), //
                        hasSource("Depotauszug03.txt"), hasNote("Lastschrifteinr."))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2012-01-05"), //
                        hasSource("Depotauszug03.txt"), //
                        hasNote("Depotpreis Q4/2011"), //
                        hasAmount("EUR", 3.57), hasGrossValue("EUR", 3.57), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDepotauszug04()
    {
        var extractor = new MLPBankingAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(5L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, "EUR");

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2022-07-01"), hasAmount("EUR", 300.00), //
                        hasSource("Depotauszug04.txt"), hasNote("Lastschrifteinr."))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2022-08-01"), hasAmount("EUR", 300.00), //
                        hasSource("Depotauszug04.txt"), hasNote("Lastschrifteinr."))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2022-09-01"), hasAmount("EUR", 300.00), //
                        hasSource("Depotauszug04.txt"), hasNote("Lastschrifteinr."))));

        // check fee refund transaction
        assertThat(results, hasItem(feeRefund( //
                        hasDate("2022-07-04"), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Erstattung Vertriebsfolgeprovision Q2/2022"), //
                        hasAmount("EUR", 13.11), hasGrossValue("EUR", 13.11), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2022-07-04"), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Depotentgelt Q2/2022"), //
                        hasAmount("EUR", 37.25), hasGrossValue("EUR", 37.25), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }
}
