package name.abuchen.portfolio.datatransfer.pdf.erstebank;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.check;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxes;
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
import name.abuchen.portfolio.datatransfer.pdf.ErsteBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class ErsteBankPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

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
                        hasIsin("AT0000937503"), hasWkn(null), hasTicker(null), //
                        hasName("VOESTALPINE AG AKT. O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2015-09-23T09:02:20"), hasShares(105.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Ref.-Nr.: 12-12345678"), //
                        hasAmount("EUR", 3217.22), hasGrossValue("EUR", 3207.23), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 9.99))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

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
                        hasIsin("DE0005190003"), hasWkn(null), hasTicker(null), //
                        hasName("BAY.MOTOREN WERKE AG ST"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2009-10-05T12:39:27"), hasShares(50.00), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Ref.-Nr.: 12-1234567-1"), //
                        hasAmount("EUR", 1584.19), hasGrossValue("EUR", 1574.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 9.99))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

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
                        hasIsin("DE0009769794"), hasWkn(null), hasTicker(null), //
                        hasName("DWS TOP 50 WELT"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2014-07-21T18:00:00"), hasShares(1.363), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Ref.-Nr.: 12-12345678"), //
                        hasAmount("EUR", 99.96), hasGrossValue("EUR", 98.19), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.77))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

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
                        hasIsin("DE0009769794"), hasWkn(null), hasTicker(null), //
                        hasName("DWS TOP 50 WELT"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2009-11-23T18:00:00"), hasShares(2.004), //
                        hasSource("Kauf04.txt"), //
                        hasNote("Ref.-Nr.: 12-1234567-1"), //
                        hasAmount("EUR", 99.97), hasGrossValue("EUR", 98.21), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.76))));
    }

    @Test
    public void testWertpapierKauf05()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

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
                        hasIsin("AT0000741053"), hasWkn(null), hasTicker(null), //
                        hasName("EVN STAMMAKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2009-05-28T15:41:42"), hasShares(90.00), //
                        hasSource("Kauf05.txt"), //
                        hasNote("Ref.-Nr.: 12-1234567-1"), //
                        hasAmount("EUR", 1142.19), hasGrossValue("EUR", 1132.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 9.99))));
    }

    @Test
    public void testWertpapierKauf06()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("AT0000720008"), hasWkn(null), hasTicker(null), //
                        hasName("TELEKOM AUSTRIA AKT. O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2009-11-20T16:28:24"), hasShares(90.00), //
                        hasSource("Kauf06.txt"), //
                        hasNote("Ref.-Nr.: 12-1234567-2"), //
                        hasAmount("EUR", 1049.40), hasGrossValue("EUR", 1049.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf07()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf07.txt"), errors);

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
                        hasIsin("AT0000697750"), hasWkn(null), hasTicker(null), //
                        hasName("CONWERT IMMO. AKT. O.N"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2012-03-06T16:57:08"), hasShares(50.00), //
                        hasSource("Kauf07.txt"), //
                        hasNote("Ref.-Nr.: 12-12345678"), //
                        hasAmount("EUR", 397.50), hasGrossValue("EUR", 397.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf08()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf08.txt"), errors);

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
                        hasIsin("AT0000688676"), hasWkn(null), hasTicker(null), //
                        hasName("RAIFF.ENERGIE-AKTIEN T"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2009-12-23T16:18:07"), hasShares(0.671), //
                        hasSource("Kauf08.txt"), //
                        hasNote("Ref.-Nr.: 12-1234567-1"), //
                        hasAmount("EUR", 99.82), hasGrossValue("EUR", 97.63), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.19))));
    }

    @Test
    public void testWertpapierKauf09()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf09.txt"), errors);

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
                        hasIsin("US1491231015"), hasWkn(null), hasTicker(null), //
                        hasName("CATERPILLAR INC. DL 1"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2009-12-03T16:42:22"), hasShares(35.00), //
                        hasSource("Kauf09.txt"), //
                        hasNote("Ref.-Nr.: 12-1234567-1"), //
                        hasAmount("EUR", 1397.90), hasGrossValue("EUR", 1377.36), //
                        hasForexGrossValue("USD", 2069.89), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 20.54))));
    }

    @Test
    public void testWertpapierKauf09WithSecurityInEUR()
    {
        var security = new Security("CATERPILLAR INC. DL 1", "EUR");
        security.setIsin("US1491231015");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ErsteBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf09.txt"), errors);

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
                        hasDate("2009-12-03T16:42:22"), hasShares(35.00), //
                        hasSource("Kauf09.txt"), //
                        hasNote("Ref.-Nr.: 12-1234567-1"), //
                        hasAmount("EUR", 1397.90), hasGrossValue("EUR", 1377.36), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 20.54), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf10()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf10.txt"), errors);

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
                        hasIsin("LU0243955886"), hasWkn(null), hasTicker(null), //
                        hasName("INVESCO ASIAN INF. A ACC."), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2014-12-22T18:00:00"), hasShares(12.536), //
                        hasSource("Kauf10.txt"), //
                        hasNote("Ref.-Nr.: 12-12345678"), //
                        hasAmount("EUR", 149.94), hasGrossValue("EUR", 146.44), //
                        hasForexGrossValue("USD", 178.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.26 / 1.2169))));
    }

    @Test
    public void testWertpapierKauf10WithSecurityInEUR()
    {
        var security = new Security("INVESCO ASIAN INF. A", "EUR");
        security.setIsin("LU0243955886");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ErsteBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf10.txt"), errors);

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
                        hasDate("2014-12-22T18:00:00"), hasShares(12.536), //
                        hasSource("Kauf10.txt"), //
                        hasNote("Ref.-Nr.: 12-12345678"), //
                        hasAmount("EUR", 149.94), hasGrossValue("EUR", 146.44), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.26 / 1.2169), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf11()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf11.txt"), errors);

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
                        hasIsin("LU0243955886"), hasWkn(null), hasTicker(null), //
                        hasName("INVESCO ASIAN INF. A ACC."), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2015-08-20T18:00:00"), hasShares(11.787), //
                        hasSource("Kauf11.txt"), //
                        hasNote("Ref.-Nr.: 12-12345678"), //
                        hasAmount("EUR", 149.87), hasGrossValue("EUR", 146.09), //
                        hasForexGrossValue("USD", 163.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.24 / 1.1229))));
    }

    @Test
    public void testWertpapierKauf11WithSecurityInEUR()
    {
        var security = new Security("INVESCO ASIAN INF. A", "EUR");
        security.setIsin("LU0243955886");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ErsteBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf11.txt"), errors);

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
                        hasDate("2015-08-20T18:00:00"), hasShares(11.787), //
                        hasSource("Kauf11.txt"), //
                        hasNote("Ref.-Nr.: 12-12345678"), //
                        hasAmount("EUR", 149.87), hasGrossValue("EUR", 146.09), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.24 / 1.1229), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf12()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf12.txt"), errors);

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
                        hasIsin("AT0000A0CNC5"), hasWkn(null), hasTicker(null), //
                        hasName("ERSTE GROUP BANK AG INDEX-ZERT.2009/OPEN END CRUDE"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-03-25T09:15:13"), hasShares(45.00), //
                        hasSource("Kauf12.txt"), //
                        hasNote("Ref.-Nr.: XXXXXXXX | Limit: Bestens"), //
                        hasAmount("EUR", 866.05), hasGrossValue("EUR", 841.05), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 25.00))));
    }

    @Test
    public void testWertpapierKauf13()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf13.txt"), errors);

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
                        hasIsin("DE0008404005"), hasWkn(null), hasTicker(null), //
                        hasName("ALLIANZ SE VINK.NAMENS-AKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-09-08T14:10:34"), hasShares(10.00), //
                        hasSource("Kauf13.txt"), //
                        hasNote("Ref.-Nr.: 12003580 | Limit: Bestens"), //
                        hasAmount("EUR", 1717.38), hasGrossValue("EUR", 1668.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 35.00 + 5.00 + 8.78))));
    }

    @Test
    public void testWertpapierKauf14()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf14.txt"), errors);

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
                        hasIsin("US00287Y1091"), hasWkn(null), hasTicker(null), //
                        hasName("ABBVIE INC. REGISTERED SHARES DL -,01"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-09-08T18:32:23"), hasShares(5.00), //
                        hasSource("Kauf14.txt"), //
                        hasNote("Ref.-Nr.: 12003586 | Limit: Bestens"), //
                        hasAmount("EUR", 750.95), hasGrossValue("EUR", 700.42), //
                        hasForexGrossValue("USD", 697.27), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 35.00 + 3.54 + 5.00 + 6.99))));
    }

    @Test
    public void testWertpapierKauf14WithSecurityInEUR()
    {
        var security = new Security("ABBVIE INC. REGISTERED SHARES DL -,01", "EUR");
        security.setIsin("US00287Y1091");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ErsteBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf14.txt"), errors);

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
                        hasDate("2022-09-08T18:32:23"), hasShares(5.00), //
                        hasSource("Kauf14.txt"), //
                        hasNote("Ref.-Nr.: 12003586 | Limit: Bestens"), //
                        hasAmount("EUR", 750.95), hasGrossValue("EUR", 700.42), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 35.00 + 3.54 + 5.00 + 6.99), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf15()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf15.txt"), errors);

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
                        hasIsin("IE00BHZRQZ17"), hasWkn(null), hasTicker(null), //
                        hasName("FT ICAV-FR.FTSE INDIA U.ETF REG. SHARES USD ACC. O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-01-16T09:31:02"), hasShares(500.00), //
                        hasSource("Kauf15.txt"), //
                        hasNote("Ref.-Nr.: 13001070 | Limit: EUR 30,15"), //
                        hasAmount("EUR", 15114.70), hasGrossValue("EUR", 15075.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 39.70))));
    }

    @Test
    public void testWertpapierKauf16()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf16.txt"), errors);

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
                        hasIsin("DE0009769794"), hasWkn(null), hasTicker(null), //
                        hasName("DWS TOP 50 WELT"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2009-07-21T18:00"), hasShares(2.214), //
                        hasSource("Kauf16.txt"), //
                        hasNote("Ref.-Nr.: 12-1234567-1"), //
                        hasAmount("EUR", 99.96), hasGrossValue("EUR", 98.43), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.53))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2009-07-21T18:00"), hasShares(2.214), //
                        hasSource("Kauf16.txt"), //
                        hasNote("Ref.-Nr.: 12-1234567-1"), //
                        hasAmount("EUR", 0.02), hasGrossValue("EUR", 0.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf17()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf17.txt"), errors);

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
                        hasIsin("DE0009769794"), hasWkn(null), hasTicker(null), //
                        hasName("DWS TOP 50 WELT"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2009-11-23T18:00"), hasShares(2.004), //
                        hasSource("Kauf17.txt"), //
                        hasNote("Ref.-Nr.: 12-1234567-1"), //
                        hasAmount("EUR", 99.97), hasGrossValue("EUR", 98.21), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.76))));
    }

    @Test
    public void testWertpapierKauf18()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf18.txt"), errors);

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
                        hasIsin("DE0009769794"), hasWkn(null), hasTicker(null), //
                        hasName("DWS TOP 50 WELT"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2014-07-21T18:00"), hasShares(1.363), //
                        hasSource("Kauf18.txt"), //
                        hasNote("Ref.-Nr.: 12-12345678"), //
                        hasAmount("EUR", 99.96), hasGrossValue("EUR", 98.19), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.77))));
    }

    @Test
    public void testWertpapierKauf19()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf19.txt"), errors);

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
                        hasIsin("AT0000828637"), hasWkn(null), hasTicker(null), //
                        hasName("CORE DYNAMIC T MITEIGENTUMSANTEILE - THESAURIEREND"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-04-13T13:43:45"), hasShares(12.00), //
                        hasSource("Kauf19.txt"), //
                        hasNote("Ref.-Nr.: 35082457"), //
                        hasAmount("EUR", 2148.54), hasGrossValue("EUR", 2075.88), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 72.66))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

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
                        hasIsin("AT0000937503"), hasWkn(null), hasTicker(null), //
                        hasName("VOESTALPINE AG AKT. O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2015-10-23T16:46:47"), hasShares(10.00), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Ref.-Nr.: 12-12345678"), //
                        hasAmount("EUR", 343.85), hasGrossValue("EUR", 346.85), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.00))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

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
                        hasIsin("DE0005190003"), hasWkn(null), hasTicker(null), //
                        hasName("BAY.MOTOREN WERKE AG ST"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2010-04-09T16:27:05"), hasShares(50.00), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Ref.-Nr.: 12-1234567-1"), //
                        hasAmount("EUR", 1732.31), hasGrossValue("EUR", 1742.30), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 9.99))));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

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
                        hasIsin("AT0000741053"), hasWkn(null), hasTicker(null), //
                        hasName("EVN STAMMAKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2010-04-16T15:27:19"), hasShares(90.00), //
                        hasSource("Verkauf03.txt"), //
                        hasNote("Ref.-Nr.: 12-1234567-1"), //
                        hasAmount("EUR", 1154.61), hasGrossValue("EUR", 1164.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 9.99))));
    }

    @Test
    public void testWertpapierVerkauf04()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

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
                        hasIsin("DE0005200000"), hasWkn(null), hasTicker(null), //
                        hasName("BEIERSDORF AG O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2012-04-10T09:00:20"), hasShares(29.00), //
                        hasSource("Verkauf04.txt"), //
                        hasNote("Ref.-Nr.: 12-12345678"), //
                        hasAmount("EUR", 1394.90), hasGrossValue("EUR", 1394.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf05()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("AT0000652011"), hasWkn(null), hasTicker(null), //
                        hasName("ERSTE GROUP BK ST.AKT.ON"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2012-12-06T16:52:18"), hasShares(50.00), //
                        hasSource("Verkauf05.txt"), //
                        hasNote("Ref.-Nr.: 12-12345678"), //
                        hasAmount("EUR", 1121.25), hasGrossValue("EUR", 1121.25), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf06()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("AT0000743059"), hasWkn(null), hasTicker(null), //
                        hasName("OMV AG AKT. O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2013-04-30T14:32:40"), hasShares(10.00), //
                        hasSource("Verkauf06.txt"), //
                        hasNote("Ref.-Nr.: 12-12345678"), //
                        hasAmount("EUR", 358.90), hasGrossValue("EUR", 358.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf07()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf07.txt"), errors);

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
                        hasIsin("US6174464486"), hasWkn(null), hasTicker(null), //
                        hasName("MORGAN ST., DEAN W. DL-01"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2010-12-03T15:30:14"), hasShares(60.00), //
                        hasSource("Verkauf07.txt"), //
                        hasNote("Ref.-Nr.: 12-1234567-1"), //
                        hasAmount("EUR", 1116.78), hasGrossValue("EUR", 1136.50), //
                        hasForexGrossValue("USD", 1522.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 19.72))));
    }

    @Test
    public void testWertpapierVerkauf07WithSecurityInEUR()
    {
        var security = new Security("MORGAN ST., DEAN W. DL-01", "EUR");
        security.setIsin("US6174464486");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ErsteBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf07.txt"), errors);

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
                        hasDate("2010-12-03T15:30:14"), hasShares(60.00), //
                        hasSource("Verkauf07.txt"), //
                        hasNote("Ref.-Nr.: 12-1234567-1"), //
                        hasAmount("EUR", 1116.78), hasGrossValue("EUR", 1136.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 19.72), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierVerkauf08()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf08.txt"), errors);

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
                        hasIsin("AT0000809058"), hasWkn(null), hasTicker(null), //
                        hasName("IMMOFINANZ AG INHABERAKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2017-06-08T19:30"), hasShares(0.40), //
                        hasSource("Verkauf08.txt"), //
                        hasNote("Ref.-Nr.: 89628241"), //
                        hasAmount("EUR", 0.80), hasGrossValue("EUR", 0.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf09()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf09.txt"), errors);

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
                        hasIsin("CA0679011084"), hasWkn(null), hasTicker(null), //
                        hasName("BARRICK GOLD CORP. SHARES O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2016-06-13T17:10:18"), hasShares(35.00), //
                        hasSource("Verkauf09.txt"), //
                        hasNote("Ref.-Nr.: 27302059 | Limit: Bestens"), //
                        hasAmount("EUR", 613.10), hasGrossValue("EUR", 625.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.75 + 8.95))));
    }

    @Test
    public void testWertpapierVerkauf10()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf10.txt"), errors);

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
                        hasIsin("AT0000677901"), hasWkn(null), hasTicker(null), //
                        hasName("RAIFF.-NACHHALTIGKEITSF.-AKT.(R) A MITEIGENTUMSANTEILE - AUSSCHUETTEND"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2016-12-14T18:38:03"), hasShares(15.00), //
                        hasSource("Verkauf10.txt"), //
                        hasNote("Ref.-Nr.: 28170733"), //
                        hasAmount("EUR", 1706.55), hasGrossValue("EUR", 1706.55), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf11()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf11.txt"), errors);

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
                        hasIsin("LU0303816705"), hasWkn(null), hasTicker(null), //
                        hasName("FIDELITY FDS-EM.EU,MID.EAST.A. REG. SHARES A-ACC. EUR O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2016-11-23T20:54:28"), hasShares(331.00), //
                        hasSource("Verkauf11.txt"), //
                        hasNote("Ref.-Nr.: 12345678"), //
                        hasAmount("EUR", 5149.14), hasGrossValue("EUR", 5216.56), //
                        hasTaxes("EUR", 67.42), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf12()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf12.txt"), errors);

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
                        hasIsin("AT0000606306"), hasWkn(null), hasTicker(null), //
                        hasName("RAIFFEISEN BANK INTERNATIONAL AG INHABERAKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2016-11-23T17:24:00"), hasShares(158.00), //
                        hasSource("Verkauf12.txt"), //
                        hasNote("Ref.-Nr.: 12345678 | Limit: Bestens"), //
                        hasAmount("EUR", 2606.82), hasGrossValue("EUR", 2674.15), //
                        hasTaxes("EUR", 58.38), hasFees("EUR", 8.95))));
    }

    @Test
    public void testWertpapierVerkauf13()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf13.txt"), errors);

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
                        hasIsin("AT0000606306"), hasWkn(null), hasTicker(null), //
                        hasName("RAIFFEISEN BANK INTERNATIONAL AG INHABERAKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2016-11-23T17:24:00"), hasShares(89.00), //
                        hasSource("Verkauf13.txt"), //
                        hasNote("Ref.-Nr.: 12345678 | Limit: Bestens"), //
                        hasAmount("EUR", 1471.94), hasGrossValue("EUR", 1506.33), //
                        hasTaxes("EUR", 32.88), hasFees("EUR", 1.51))));
    }

    @Test
    public void testWertpapierVerkauf14()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf14.txt"), errors);

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
                        hasIsin("FR0014008VX5"), hasWkn(null), hasTicker(null), //
                        hasName("EUROAPI SAS ACTIONS NOM. EO 1"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-06-29T19:30"), hasShares(0.609), //
                        hasSource("Verkauf14.txt"), //
                        hasNote("Ref.-Nr.: 82004481"), //
                        hasAmount("EUR", 8.95), hasGrossValue("EUR", 9.46), //
                        hasTaxes("EUR", 0.51), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSammelabrechnungKaufVerkauf01()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SammelabrechnungKaufVerkauf01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("AT0000A1QA79"), hasWkn(null), hasTicker(null), //
                        hasName("ERSTE R ST DIV EUR R01 T"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("AT0000705660"), hasWkn(null), hasTicker(null), //
                        hasName("ERSTE WWF ST ENV EU R A"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-03-26T00:00"), hasShares(23.008), //
                        hasSource("SammelabrechnungKaufVerkauf01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2099.97), hasGrossValue("EUR", 2027.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 72.97))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-03-26T00:00"), hasShares(14.603), //
                        hasSource("SammelabrechnungKaufVerkauf01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2099.87), hasGrossValue("EUR", 2026.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 72.97))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

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
                        hasIsin("CA0679011084"), hasWkn(null), hasTicker(null), //
                        hasName("BARRICK GOLD CORP."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2015-09-15T00:00"), hasExDate("2015-08-27T00:00"), //
                        hasShares(35.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Ref.-Nr.: 12-123456789"), //
                        hasAmount("EUR", 0.40), hasGrossValue("EUR", 0.62), //
                        hasForexGrossValue("USD", 0.70), //
                        hasTaxes("EUR", (0.18 + 0.07) * 0.888889), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende01WithSecurityInEUR()
    {
        var security = new Security("BARRICK GOLD CORP.", "EUR");
        security.setIsin("CA0679011084");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ErsteBankPDFExtractor(client);

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
                        hasDate("2015-09-15T00:00"), hasExDate("2015-08-27T00:00"), //
                        hasShares(35.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Ref.-Nr.: 12-123456789"), //
                        hasAmount("EUR", 0.40), hasGrossValue("EUR", 0.62), //
                        hasTaxes("EUR", (0.18 + 0.07) * 0.888889), hasFees("EUR", 0.00), //
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
        var extractor = new ErsteBankPDFExtractor(new Client());

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
                        hasIsin("DE0008430026"), hasWkn(null), hasTicker(null), //
                        hasName("MUENCH.RUECKVERS.VNA O.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2010-04-29T00:00"), hasExDate("2010-04-29T00:00"), //
                        hasShares(35.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Ref.-Nr.: 12-12345"), //
                        hasAmount("EUR", 127.54), hasGrossValue("EUR", 201.25), //
                        hasTaxes("EUR", 73.21), hasFees("EUR", 0.50))));
    }

    @Test
    public void testDividende03()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

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
                        hasIsin("AT0000741053"), hasWkn(null), hasTicker(null), //
                        hasName("EVN STAMMAKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2010-01-29T00:00"), hasExDate("2010-01-26T00:00"), //
                        hasShares(90.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote("Ref.-Nr.: 12-12345"), //
                        hasAmount("EUR", 24.97), hasGrossValue("EUR", 33.30), //
                        hasTaxes("EUR", 8.33), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende04()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

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
                        hasIsin("US6174464486"), hasWkn(null), hasTicker(null), //
                        hasName("MORGAN ST., DEAN W. DL-01"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2010-08-13T00:00"), hasExDate("2010-07-28T00:00"), //
                        hasShares(60.00), //
                        hasSource("Dividende04.txt"), //
                        hasNote("Ref.-Nr.: 12-123456"), //
                        hasAmount("EUR", 1.73), hasGrossValue("EUR", 2.32), //
                        hasForexGrossValue("USD", 3.00), //
                        hasTaxes("EUR", 0.58), hasFees("EUR", 0.01))));
    }

    @Test
    public void testDividende04WithSecurityInEUR()
    {
        var security = new Security("MORGAN ST., DEAN W. DL-01", "EUR");
        security.setIsin("US6174464486");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ErsteBankPDFExtractor(client);

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
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2010-08-13T00:00"), hasExDate("2010-07-28T00:00"), //
                        hasShares(60.00), //
                        hasSource("Dividende04.txt"), //
                        hasNote("Ref.-Nr.: 12-123456"), //
                        hasAmount("EUR", 1.73), hasGrossValue("EUR", 2.32), //
                        hasTaxes("EUR", 0.58), hasFees("EUR", 0.01), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende05()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

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
                        hasIsin("DE0009769794"), hasWkn(null), hasTicker(null), //
                        hasName("DWS TOP 50 WELT"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2010-11-26T00:00"), hasExDate("2010-11-26T00:00"), //
                        hasShares(35.275), //
                        hasSource("Dividende05.txt"), //
                        hasNote("Ref.-Nr.: 12-123456"), //
                        hasAmount("EUR", 3.84), hasGrossValue("EUR", 5.64), //
                        hasTaxes("EUR", 0.39 + 1.41), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende06()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

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
                        hasIsin("AT0000677901"), hasWkn(null), hasTicker(null), //
                        hasName("RAIFF.ETHIK-AKTIEN (R)"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2011-06-15T00:00"), hasExDate("2011-06-15T00:00"), //
                        hasShares(20.286), //
                        hasSource("Dividende06.txt"), //
                        hasNote("Ref.-Nr.: 12-123456"), //
                        hasAmount("EUR", 17.85), hasGrossValue("EUR", 19.07), //
                        hasTaxes("EUR", 1.22), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende07()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

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
                        hasIsin("DE0009769794"), hasWkn(null), hasTicker(null), //
                        hasName("DWS TOP 50 WELT"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2012-11-23T00:00"), hasExDate("2012-11-23T00:00"), //
                        hasShares(77.638), //
                        hasSource("Dividende07.txt"), //
                        hasNote("Ref.-Nr.: 12-123456789"), //
                        hasAmount("EUR", 13.81), hasGrossValue("EUR", 16.30), //
                        hasTaxes("EUR", 0.52 + 1.97), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende08()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

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
                        hasIsin("DE0009769794"), hasWkn(null), hasTicker(null), //
                        hasName("DWS TOP 50 WELT"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2013-11-22T00:00"), hasExDate("2013-11-22T00:00"), //
                        hasShares(96.324), //
                        hasSource("Dividende08.txt"), //
                        hasNote("Ref.-Nr.: 12-123456789"), //
                        hasAmount("EUR", 25.70), hasGrossValue("EUR", 29.86), //
                        hasTaxes("EUR", 0.88 + 3.28), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende09()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende09.txt"), errors);

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
                        hasIsin("AT0000677901"), hasWkn(null), hasTicker(null), //
                        hasName("RAIFF.-NACHHALTIGKEITSF.-AKT.(R)A MITEIGENTUMSANTEILE - AUSSCHUETTEND"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2016-06-16T00:00"), hasExDate("2016-06-15T00:00"), //
                        hasShares(62.00), //
                        hasSource("Dividende09.txt"), //
                        hasNote("Ausschüttung"), //
                        hasAmount("EUR", 52.19), hasGrossValue("EUR", 63.24), //
                        hasTaxes("EUR", 4.81 + 0.11 + 6.13), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende10()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende10.txt"), errors);

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
                        hasIsin("CA0679011084"), hasWkn(null), hasTicker(null), //
                        hasName("BARRICKGOLDCORP. SHARES O.N."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2016-03-16T00:00"), hasExDate("2016-02-25T00:00"), //
                        hasShares(35.00), //
                        hasSource("Dividende10.txt"), //
                        hasNote("Quartalsdividende"), //
                        hasAmount("EUR", 0.01), hasGrossValue("EUR", 0.63), //
                        hasForexGrossValue("USD", 0.70), //
                        hasTaxes("EUR", 0.16 + 0.08), hasFees("EUR", 0.38))));
    }

    @Test
    public void testDividende10WithSecurityInEUR()
    {
        var security = new Security("BARRICKGOLDCORP. SHARES O.N.", "EUR");
        security.setIsin("CA0679011084");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ErsteBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende10.txt"), errors);

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
                        hasDate("2016-03-16T00:00"), hasExDate("2016-02-25T00:00"), //
                        hasShares(35.00), //
                        hasSource("Dividende10.txt"), //
                        hasNote("Quartalsdividende"), //
                        hasAmount("EUR", 0.01), hasGrossValue("EUR", 0.63), //
                        hasTaxes("EUR", 0.16 + 0.08), hasFees("EUR", 0.38), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende11()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende11.txt"), errors);

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
                        hasIsin("AT0000746409"), hasWkn(null), hasTicker(null), //
                        hasName("VERBUNDAG INHABERAKTIEN KAT. A O.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2016-05-04T00:00"), hasExDate("2016-04-20T00:00"), //
                        hasShares(75.00), //
                        hasSource("Dividende11.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 19.03), hasGrossValue("EUR", 26.25), //
                        hasTaxes("EUR", 7.22), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende12()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende12.txt"), errors);

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
                        hasIsin("DE0008404005"), hasWkn(null), hasTicker(null), //
                        hasName("ALLIANZSE VINK.NAMENS-AKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2016-05-06T00:00"), hasExDate("2016-05-05T00:00"), //
                        hasShares(60.00), //
                        hasSource("Dividende12.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 264.10), hasGrossValue("EUR", 438.00), //
                        hasTaxes("EUR", 115.52 + 54.75), hasFees("EUR", 3.63))));
    }

    @Test
    public void testDividende13()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende13.txt"), errors);

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
                        hasIsin("AT0000809058"), hasWkn(null), hasTicker(null), //
                        hasName("IMMOFINANZAG INHABERAKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2017-06-08T00:00"), hasExDate("2017-06-02T00:00"), //
                        hasShares(1270.4), //
                        hasSource("Dividende13.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 76.22), hasGrossValue("EUR", 76.22), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende14()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende14.txt"), errors);

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
                        hasIsin("AT0000705660"), hasWkn(null), hasTicker(null), //
                        hasName("ERSTE WWF STOCK ENV EUR R01 MITEIGENTUMSANTEILE - AUSSCHUETTEND"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-08-01T00:00"), hasExDate("2022-07-28T00:00"), //
                        hasShares(25.621), //
                        hasSource("Dividende14.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 52.53), hasGrossValue("EUR", 153.73), //
                        hasTaxes("EUR", 101.20), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende15()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende15.txt"), errors);

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
                        hasIsin("DE000BASF111"), hasWkn(null), hasTicker(null), //
                        hasName("BASF SE NAMENS-AKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-05-05T00:00"), hasExDate("2022-05-02T00:00"), //
                        hasShares(60.00), //
                        hasSource("Dividende15.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 124.69), hasGrossValue("EUR", 204.00), //
                        hasTaxes("EUR", 53.81 + 25.50), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende16()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende16.txt"), errors);

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
                        hasIsin("DE0008430026"), hasWkn(null), hasTicker(null), //
                        hasName("MUENCH.RUECKVERS.VNA O.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2009-04-23T00:00"), hasExDate("2009-04-23T00:00"), //
                        hasShares(35.00), //
                        hasSource("Dividende16.txt"), //
                        hasNote("Ref.-Nr.: 12-12345"), //
                        hasAmount("EUR", 122.00), hasGrossValue("EUR", 192.50), //
                        hasTaxes("EUR", 50.77 + 19.25), hasFees("EUR", 0.48))));
    }

    @Test
    public void testDividende17()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende17.txt"), errors);

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
                        hasIsin("US6778621044"), hasWkn(null), hasTicker(null), //
                        hasName("LUKOIL HLDG SP.ADR4//RR25"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2011-08-19T00:00"), hasExDate("2011-05-04T00:00"), //
                        hasShares(50.00), //
                        hasSource("Dividende17.txt"), //
                        hasNote("Ref.-Nr.: 12-123456"), //
                        hasAmount("EUR", 52.47), hasGrossValue("EUR", 70.24), //
                        hasForexGrossValue("USD", 101.97), //
                        hasTaxes("EUR", (15.30 + 10.20) / 1.4490000), hasFees("EUR", 0.17))));
    }

    @Test
    public void testDividende17WithSecurityInEUR()
    {
        var security = new Security("LUKOIL HLDG SP.ADR4//RR25", "EUR");
        security.setIsin("US6778621044");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ErsteBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende17.txt"), errors);

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
                        hasDate("2011-08-19T00:00"), hasExDate("2011-05-04T00:00"), //
                        hasShares(50.00), //
                        hasSource("Dividende17.txt"), //
                        hasNote("Ref.-Nr.: 12-123456"), //
                        hasAmount("EUR", 52.47), hasGrossValue("EUR", 70.24), //
                        hasTaxes("EUR", (15.30 + 10.20) / 1.4490000), hasFees("EUR", 0.17), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende18()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende18.txt"), errors);

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
                        hasIsin("US6778621044"), hasWkn(null), hasTicker(null), //
                        hasName("LUKOIL N.K.SP.ADR RL-025"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2012-08-24T00:00"), hasExDate("2012-05-09T00:00"), //
                        hasShares(50.00), //
                        hasSource("Dividende18.txt"), //
                        hasNote("Ref.-Nr.: 12-123456789"), //
                        hasAmount("EUR", 68.66), hasGrossValue("EUR", 116.78 * 0.788923), //
                        hasForexGrossValue("USD", 116.78), //
                        hasTaxes("EUR", (17.51 + 11.67) * 0.788923), hasFees("EUR", (0.35 + 0.22) * 0.788923))));
    }

    @Test
    public void testDividende18WithSecurityInEUR()
    {
        var security = new Security("LUKOIL N.K.SP.ADR RL-025", "EUR");
        security.setIsin("US6778621044");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ErsteBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende18.txt"), errors);

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
                        hasDate("2012-08-24T00:00"), hasExDate("2012-05-09T00:00"), //
                        hasShares(50.00), //
                        hasSource("Dividende18.txt"), //
                        hasNote("Ref.-Nr.: 12-123456789"), //
                        hasAmount("EUR", 68.66), hasGrossValue("EUR", 116.78 * 0.788923), //
                        hasTaxes("EUR", (17.51 + 11.67) * 0.788923), hasFees("EUR", (0.35 + 0.22) * 0.788923), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende19()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende19.txt"), errors);

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
                        hasIsin("FR0000044448"), hasWkn(null), hasTicker(null), //
                        hasName("NEXANS INH.EO 1"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2012-05-23T00:00"), hasExDate("2012-05-18T00:00"), //
                        hasShares(33.00), //
                        hasSource("Dividende19.txt"), //
                        hasNote("Ref.-Nr.: 12-123456789"), //
                        hasAmount("EUR", 21.69), hasGrossValue("EUR", 36.30), //
                        hasTaxes("EUR", 10.89 + 3.63), hasFees("EUR", 0.09))));
    }

    @Test
    public void testDividende20()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende20.txt"), errors);

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
                        hasIsin("AT0000677901"), hasWkn(null), hasTicker(null), //
                        hasName("RAIFF.ETHIK-AKTIEN A"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2010-06-15T00:00"), hasExDate("2010-06-15T00:00"), //
                        hasShares(7.054), //
                        hasSource("Dividende20.txt"), //
                        hasNote("Ref.-Nr.: 12-12345"), //
                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.14), //
                        hasTaxes("EUR", 0.14), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende21()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende21.txt"), errors);

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
                        hasIsin("AT0000APOST4"), hasWkn(null), hasTicker(null), //
                        hasName("OESTERREICHISCHE POST AG"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2009-08-20T00:00"), hasExDate("2009-08-20T00:00"), //
                        hasShares(90.000), //
                        hasSource("Dividende21.txt"), //
                        hasNote("Ref.-Nr.: 12-12345"), //
                        hasAmount("EUR", 75.94), hasGrossValue("EUR", 75.94), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende22()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende22.txt"), errors);

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
                        hasIsin("AT0000809058"), hasWkn(null), hasTicker(null), //
                        hasName("IMMOFINANZ IMMOBILIEN ANLAGEN AG"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2011-10-05T00:00"), hasExDate("2011-09-30T00:00"), //
                        hasShares(1750.000), //
                        hasSource("Dividende22.txt"), //
                        hasNote("Ref.-Nr.: 12-123456"), //
                        hasAmount("EUR", 175.00), hasGrossValue("EUR", 175.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende23()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende23.txt"), errors);

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
                        hasIsin("AT0000809058"), hasWkn(null), hasTicker(null), //
                        hasName("IMMOFINANZ IMMOBILIEN ANLAGEN AG"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2012-10-15T00:00"), hasExDate("2012-10-08T00:00"), //
                        hasShares(1750.000), //
                        hasSource("Dividende23.txt"), //
                        hasNote("Ref.-Nr.: 12-123456789"), //
                        hasAmount("EUR", 262.50), hasGrossValue("EUR", 262.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende24()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende24.txt"), errors);

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
                        hasName("ALPHABET INC. REG. SHS CL. A DL-,001"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-06-18T00:00"), hasExDate("2024-06-10T00:00"), //
                        hasShares(700.00), //
                        hasSource("Dividende24.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 88.12), hasGrossValue("EUR", 130.69), //
                        hasForexGrossValue("USD", 140.00), //
                        hasTaxes("EUR", 19.60 + 16.34), hasFees("EUR", 6.63))));
    }

    @Test
    public void testDividende24WithSecurityInEUR()
    {
        var security = new Security("ALPHABET INC. REG. SHS CL. A DL-,001", "EUR");
        security.setIsin("US02079K3059");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ErsteBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende24.txt"), errors);

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
                        hasDate("2024-06-18T00:00"), hasExDate("2024-06-10T00:00"), //
                        hasShares(700.00), //
                        hasSource("Dividende24.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 88.12), hasGrossValue("EUR", 130.69), //
                        hasTaxes("EUR", 19.60 + 16.34), hasFees("EUR", 6.63), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende25()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende25.txt"), errors);

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
                        hasIsin("DE0008474214"), hasWkn(null), hasTicker(null), //
                        hasName("DWS GLOBAL COMMUNICATIONS INHABER-ANTEILE ND"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2024-12-09T00:00"), hasShares(5.00), //
                        hasSource("Dividende25.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 16.38), hasGrossValue("EUR", 16.38), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende26()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende26.txt"), errors);

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
                        hasIsin("AT0000A08SH5"), hasWkn(null), hasTicker(null), //
                        hasName("ERSTEIMMOFDEURRT MITEIGENTUMSANTEILE - THESAURIEREND"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-07-15T00:00"), hasExDate("2021-07-13T00:00"), //
                        hasShares(12.00), //
                        hasSource("Dividende26.txt"), //
                        hasNote("Ausschüttung"), //
                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 5.32), //
                        hasTaxes("EUR", 5.32), hasFees("EUR", 0.00))));
    }

    @Test
    public void testVerlustbescheinigung01()
    {
        var extractor = new ErsteBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verlustbescheinigung01.txt"), errors);

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
        assertThat(results, hasItem(taxes(hasDate("2013-04-28"), hasAmount("EUR", 198.21), //
                        hasSource("Verlustbescheinigung01.txt"), hasNote("01.04.2012 bis 31.12.2012"))));
    }
}
