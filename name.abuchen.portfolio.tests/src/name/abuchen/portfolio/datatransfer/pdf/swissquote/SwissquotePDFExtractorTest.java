package name.abuchen.portfolio.datatransfer.pdf.swissquote;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.check;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.fee;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasExDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFeed;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFeedProperty;
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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interestCharge;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.outboundDelivery;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
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
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.SwissquotePDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.TestCoinSearchProvider;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.SecuritySearchProvider;
import name.abuchen.portfolio.online.impl.CoinGeckoQuoteFeed;

@SuppressWarnings("nls")
public class SwissquotePDFExtractorTest
{
    SwissquotePDFExtractor extractor = new SwissquotePDFExtractor(new Client())
    {
        @Override
        protected List<SecuritySearchProvider> lookupCryptoProvider()
        {
            return TestCoinSearchProvider.cryptoProvider();
        }
    };

    @Test
    public void testWertpapierKauf01()
    {
        var client = new Client();

        var extractor = new SwissquotePDFExtractor(client);

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
                        hasIsin("US0378331005"), hasWkn(null), hasTicker(null), //
                        hasName("APPLE ORD"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-08-05T00:00"), hasShares(15.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Referenz: 32484929"), //
                        hasAmount("USD", 2900.60), hasGrossValue("USD", 2895.00), //
                        hasTaxes("USD", 4.75), hasFees("USD", 0.85))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var client = new Client();

        var extractor = new SwissquotePDFExtractor(client);

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
                        hasIsin("CH0001752309"), hasWkn(null), hasTicker(null), //
                        hasName("FISCHER N"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-05-13T00:00"), hasShares(3.00), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Referenz: 32484929"), //
                        hasAmount("CHF", 2747.40), hasGrossValue("CHF", 2713.50), //
                        hasTaxes("CHF", 2.05), hasFees("CHF", 31.85))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        var client = new Client();

        var extractor = new SwissquotePDFExtractor(client);

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
                        hasIsin("DK0010268606"), hasWkn(null), hasTicker(null), //
                        hasName("VESTAS WIND SYSTEMS ORD"), //
                        hasCurrencyCode("DKK"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-07-12T00:00"), hasShares(61.00), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Referenz: 32484929"), //
                        hasAmount("CHF", 5650.15), hasGrossValue("CHF", 5602.65), //
                        hasForexGrossValue("DKK", 37301.50), //
                        hasTaxes("CHF", 8.40), hasFees("CHF", 39.10))));
    }

    @Test
    public void testWertpapierKauf03WithSecurityInCHF()
    {
        var security = new Security("VESTAS WIND SYSTEMS ORD", "CHF");
        security.setIsin("DK0010268606");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new SwissquotePDFExtractor(client);

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
                        hasDate("2017-07-12T00:00"), hasShares(61.00), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Referenz: 32484929"), //
                        hasAmount("CHF", 5650.15), hasGrossValue("CHF", 5602.65), //
                        hasTaxes("CHF", 8.40), hasFees("CHF", 39.10), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        var client = new Client();

        var extractor = new SwissquotePDFExtractor(client);

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
                        hasIsin("IE00B3RBWM25"), hasWkn(null), hasTicker(null), //
                        hasName("Vanguard All World ETF Dist"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-10-28T00:00"), hasShares(25.00), //
                        hasSource("Kauf04.txt"), //
                        hasNote("Referenz: 206871550"), //
                        hasAmount("CHF", 2102.25), hasGrossValue("CHF", 2087.25), //
                        hasTaxes("CHF", 3.15), hasFees("CHF", 9.85 + 2.00))));
    }

    @Test
    public void testWertpapierKauf05()
    {
        var client = new Client();

        var extractor = new SwissquotePDFExtractor(client);

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0210483332"), hasWkn(null), hasTicker(null), //
                        hasName("RICHEMONT N"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-10-31T00:00"), hasShares(1.0269), //
                        hasSource("Kauf05.txt"), //
                        hasNote("Referenz: 312345678"), //
                        hasAmount("CHF", 100.10), hasGrossValue("CHF", 99.00), //
                        hasTaxes("CHF", 0.10), hasFees("CHF", 1.00))));
    }

    @Test
    public void testWertpapierKauf06()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

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
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("QQQ MAY24 460C"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-03-25T00:00"), hasShares(500.00), //
                        hasSource("Kauf06.txt"), //
                        hasNote("Referenz: 539850276"), //
                        hasAmount("USD", 3144.75), hasGrossValue("USD", 3135.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 2.25 + 7.50))));
    }

    @Test
    public void testWertpapierKauf07()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

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
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("SPY JUL24 527C"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-05-29T00:00"), hasShares(300.00), //
                        hasSource("Kauf07.txt"), //
                        hasNote("Referenz: 581899025"), //
                        hasAmount("USD", 3579.35), hasGrossValue("USD", 3573.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 1.35 + 5.00))));
    }

    @Test
    public void testWertpapierKauf08()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

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
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("SPY JUL24 530C"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-05-23T00:00"), hasShares(500.00), //
                        hasSource("Kauf08.txt"), //
                        hasNote("Referenz: 578365281"), //
                        hasAmount("USD", 4769.75), hasGrossValue("USD", 4760.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 2.25 + 7.50))));
    }

    @Test
    public void testWertpapierKauf09()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

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
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US0231351067"), hasWkn(null), hasTicker(null), //
                        hasName("AMAZON COM ORD"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-11-15T00:00"), hasShares(0.4683), //
                        hasSource("Kauf09.txt"), //
                        hasNote("Referenz: 699111111"), //
                        hasAmount("USD", 95.00), hasGrossValue("USD", 94.86), //
                        hasTaxes("USD", 0.14), hasFees("USD", 0.00))));
    }

    @Test
    public void testWertpapierKauf10()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

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
                        hasIsin("IE00BM8R0J59"), hasWkn(null), hasTicker(null), //
                        hasName("GLOBAL X NASDAQ 100 COVERED CALL ETF"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-12-05T16:38:31"), hasShares(700.00), //
                        hasSource("Kauf10.txt"), //
                        hasNote("Referenz: 987654321"), //
                        hasAmount("EUR", 9926.95), hasGrossValue("EUR", 9912.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 14.95))));
    }

    @Test
    public void testSecurityBuy01()
    {
        var client = new Client();

        var extractor = new SwissquotePDFExtractor(client);

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B3RBWM25"), hasWkn(null), hasTicker(null), //
                        hasName("Vanguard FTSE All-World UCITS ETF USD"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-02-06T00:00"), hasShares(3.00), //
                        hasSource("Buy01.txt"), //
                        hasNote("Ref.-Nr.: 123456789"), //
                        hasAmount("CHF", 390.45), hasGrossValue("CHF", 387.85), //
                        hasTaxes("CHF", 0.60), hasFees("CHF", 2.00))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var client = new Client();

        var extractor = new SwissquotePDFExtractor(client);

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
                        hasIsin("CH0363463438"), hasWkn(null), hasTicker(null), //
                        hasName("IDORSIA N"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2018-02-05T00:00"), hasShares(322.00), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Referenz: 32484929"), //
                        hasAmount("CHF", 8198.70), hasGrossValue("CHF", 8236.75), //
                        hasTaxes("CHF", 6.20), hasFees("CHF", 31.85))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        var client = new Client();

        var extractor = new SwissquotePDFExtractor(client);

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
                        hasIsin("DK0010268606"), hasWkn(null), hasTicker(null), //
                        hasName("VESTAS WIND SYSTEMS ORD"), //
                        hasCurrencyCode("DKK"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-03-08T00:00"), hasShares(61.00), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Referenz: 32474929"), //
                        hasAmount("CHF", 5267.80), hasGrossValue("CHF", 5305.45), //
                        hasForexGrossValue("DKK", 35410.50), //
                        hasTaxes("CHF", 5.80), hasFees("CHF", 31.85))));
    }

    @Test
    public void testWertpapierVerkauf02WithSecurityInCHF()
    {
        var security = new Security("VESTAS WIND SYSTEMS ORD", "CHF");
        security.setIsin("DK0010268606");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new SwissquotePDFExtractor(client);

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
        new AssertImportActions().check(results, "CHF");

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-03-08T00:00"), hasShares(61.00), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Referenz: 32474929"), //
                        hasAmount("CHF", 5267.80), hasGrossValue("CHF", 5305.45), //
                        hasTaxes("CHF", 5.80), hasFees("CHF", 31.85), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

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
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("SPY JUL24 527C"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-06-07T00:00"), hasShares(100.00), //
                        hasSource("Verkauf03.txt"), //
                        hasNote("Referenz: XXXXXX"), //
                        hasAmount("USD", 1159.55), hasGrossValue("USD", 1160.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.45))));
    }

    @Test
    public void testWertpapierVerkauf04()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

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
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("SPY JUL24 530C"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-05-24T00:00"), hasShares(200.00), //
                        hasSource("Verkauf04.txt"), //
                        hasNote("Referenz: 579076719"), //
                        hasAmount("USD", 2348.10), hasGrossValue("USD", 2350.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.90 + 1.00))));
    }

    @Test
    public void testWertpapierVerkauf05()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

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
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("SPY JUL24 527C"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-05-31T00:00"), hasShares(300.00), //
                        hasSource("Verkauf05.txt"), //
                        hasNote("Referenz: 583488434"), //
                        hasAmount("USD", 3461.65), hasGrossValue("USD", 3468.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 1.35 + 5.00))));
    }

    @Test
    public void testWertpapierVerkauf06()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

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
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("NVDA AUG24 1,130P"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-06-04T00:00"), hasShares(100.00), //
                        hasSource("Verkauf06.txt"), //
                        hasNote("Referenz: 585697505"), //
                        hasAmount("USD", 7044.55), hasGrossValue("USD", 7050.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.45 + 5.00))));
    }

    @Test
    public void testSecuritySell01()
    {
        var client = new Client();

        var extractor = new SwissquotePDFExtractor(client);

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0183135976"), hasWkn(null), hasTicker(null), //
                        hasName("Swisscanto Silver ETF-A CHF"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-10-21T00:00"), hasShares(5.0721), //
                        hasSource("Sell01.txt"), //
                        hasNote("Ref.-Nr.: 533430837"), //
                        hasAmount("CHF", 548.20), hasGrossValue("CHF", 548.60), //
                        hasTaxes("CHF", 0.40), hasFees("CHF", 0.00))));
    }

    @Test
    public void testExpiryOption01()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "OptionVerfall01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("QQQ APR24 447C"), //
                        hasCurrencyCode("USD"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        outboundDelivery( //
                                        hasDate("2024-04-08T00:00"), hasShares(100.00), //
                                        hasSource("OptionVerfall01.txt"), //
                                        hasNote("Referenz: 549183576"), //
                                        hasAmount("USD", 0.00), hasGrossValue("USD", 0.00), //
                                        hasTaxes("USD", 0.00), hasFees("USD", 0.00)))));
    }

    @Test
    public void testTitelumbuchung01()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Titelumbuchung01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(2L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0012032048"), hasWkn("1203204"), hasTicker(null), //
                        hasName("ROCHE GS"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("CH1499059983"), hasWkn(null), hasTicker(null), //
                        hasName("ROCHE PS"), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        outboundDelivery( //
                                        hasDate("2026-03-17T00:00"), hasShares(8.493), //
                                        hasSource("Titelumbuchung01.txt"), //
                                        hasNote("Referenz: 9876543210"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        inboundDelivery( //
                                        hasDate("2026-03-17T00:00"), hasShares(8.493), //
                                        hasSource("Titelumbuchung01.txt"), //
                                        hasNote("Referenz: 9876543210"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testCryptoKauf01()
    {
        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "CryptoKauf01.txt"), errors);

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
                        hasIsin(null), hasWkn(null), hasTicker("BTC"), //
                        hasName("Bitcoin"), //
                        hasCurrencyCode("USD"), //
                        hasFeed(CoinGeckoQuoteFeed.ID), //
                        hasFeedProperty(CoinGeckoQuoteFeed.COINGECKO_COIN_ID, "bitcoin"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-03-19T00:00"), hasShares(0.02), //
                        hasSource("CryptoKauf01.txt"), //
                        hasNote("Referenz: 535993271"), //
                        hasAmount("USD", 1285.61), hasGrossValue("USD", 1272.88), //
                        hasTaxes("USD", 0.00), hasFees("USD", 12.73))));
    }

    @Test
    public void testDividende01()
    {
        var client = new Client();

        var extractor = new SwissquotePDFExtractor(client);

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
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US41753F1093"), hasWkn(null), hasTicker(null), //
                        hasName("HARVEST CAPITAL CREDIT ORD"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-06-27T00:00"), hasExDate(null), //
                        hasShares(350.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Referenz: 32484929"), //
                        hasAmount("USD", 19.60), hasGrossValue("USD", 28.00), //
                        hasTaxes("USD", 8.40), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende01WithSecurityInCHF()
    {
        var security = new Security("HARVEST CAPITAL CREDIT ORD", "CHF");
        security.setIsin("US41753F1093");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new SwissquotePDFExtractor(client);

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
        new AssertImportActions().check(results, "USD");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-06-27T00:00"), hasExDate(null), //
                        hasShares(350.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Referenz: 32484929"), //
                        hasAmount("USD", 19.60), hasGrossValue("USD", 28.00), //
                        hasForexGrossValue("CHF", 27.37), //
                        hasTaxes("USD", 8.40), hasFees("USD", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("USD");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende02()
    {
        var client = new Client();

        var extractor = new SwissquotePDFExtractor(client);

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
                        hasIsin("CH0025751329"), hasWkn(null), hasTicker(null), //
                        hasName("LOGITECH N"), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-09-20T00:00"), hasExDate(null), //
                        hasShares(250.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Referenz: 32484929"), //
                        hasAmount("CHF", 118.62), hasGrossValue("CHF", 182.50), //
                        hasTaxes("CHF", 63.88), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende03()
    {
        var client = new Client();

        var extractor = new SwissquotePDFExtractor(client);

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
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B3RBWM25"), hasWkn(null), hasTicker(null), //
                        hasName("Vanguard All World ETF Dist"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-03-31T00:00"), hasExDate(null), //
                        hasShares(40.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote("Referenz: 222443221"), //
                        hasAmount("USD", 13.52), hasGrossValue("USD", 13.52), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende03WithSecurityInCHF()
    {
        var security = new Security("Vanguard All World ETF Dist", "CHF");
        security.setIsin("IE00B3RBWM25");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "USD");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-03-31T00:00"), hasExDate(null), //
                        hasShares(40.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote("Referenz: 222443221"), //
                        hasAmount("USD", 13.52), hasGrossValue("USD", 13.52), //
                        hasForexGrossValue("CHF", 12.74), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("USD");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende04()
    {
        var client = new Client();

        var extractor = new SwissquotePDFExtractor(client);

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
                        hasIsin("CH0371153492"), hasWkn(null), hasTicker(null), //
                        hasName("Landis+Gyr N"), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-07-01T00:00"), hasExDate(null), //
                        hasShares(34.00), //
                        hasSource("Dividende04.txt"), //
                        hasNote("Referenz: 32484929"), //
                        hasAmount("CHF", 107.10), hasGrossValue("CHF", 107.10), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende05()
    {
        var client = new Client();

        var extractor = new SwissquotePDFExtractor(client);

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0012032048"), hasWkn(null), hasTicker(null), //
                        hasName("ROCHE GS"), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-03-20T00:00"), hasExDate(null), //
                        hasShares(4.0542), //
                        hasSource("Dividende05.txt"), //
                        hasNote("Referenz: 312345678"), //
                        hasAmount("CHF", 25.03), hasGrossValue("CHF", 38.51), //
                        hasTaxes("CHF", 13.48), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende06()
    {
        var client = new Client();

        var extractor = new SwissquotePDFExtractor(client);

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
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B8GKDB10"), hasWkn(null), hasTicker(null), //
                        hasName("Vanguard AllWrld Div ETF Dist"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-03-29T00:00"), hasExDate(null), //
                        hasShares(3.553), //
                        hasSource("Dividende06.txt"), //
                        hasNote("Referenz: 123456789"), //
                        hasAmount("USD", 1.46), hasGrossValue("USD", 1.46), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende06WithSecurityInCHF()
    {
        var security = new Security("Vanguard AllWrld Div ETF Dist", "CHF");
        security.setIsin("IE00B8GKDB10");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "USD");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-03-29T00:00"), hasExDate(null), //
                        hasShares(3.553), //
                        hasSource("Dividende06.txt"), //
                        hasNote("Referenz: 123456789"), //
                        hasAmount("USD", 1.46), hasGrossValue("USD", 1.46), //
                        hasForexGrossValue("CHF", 1.34), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("USD");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende07()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

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
                        hasIsin("NL0009690239"), hasWkn(null), hasTicker(null), //
                        hasName("VanEck Global Real Estate ETF"), //
                        hasCurrencyCode("EUR"))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-12-13T00:00"), hasExDate(null), //
                        hasShares(420.00), //
                        hasSource("Dividende07.txt"), //
                        hasNote("Referenz: 548895976"), //
                        hasAmount("EUR", 114.24), hasGrossValue("EUR", 134.40), //
                        hasTaxes("EUR", 20.16), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende07WithSecurityInCHF()
    {
        var security = new Security("VanEck Global Real Estate ETF", "CHF");
        security.setIsin("NL0009690239");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende07.txt"), errors);

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
                        hasDate("2023-12-13T00:00"), hasExDate(null), //
                        hasShares(420.00), //
                        hasSource("Dividende07.txt"), //
                        hasNote("Referenz: 548895976"), //
                        hasAmount("EUR", 114.24), hasGrossValue("EUR", 134.40), //
                        hasForexGrossValue("CHF", 127.03), //
                        hasTaxes("EUR", 20.16), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende08()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

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
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US82889N6739"), hasWkn("122171132"), hasTicker(null), //
                        hasName("SIMPLIFY BITCOIN STGY INC ETF"), //
                        hasCurrencyCode("USD"))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-02-28T00:00"), hasExDate(null), //
                        hasShares(281.00), //
                        hasSource("Dividende08.txt"), //
                        hasNote("Referenz: 795604930"), //
                        hasAmount("USD", 23.88), hasGrossValue("USD", 28.10), //
                        hasTaxes("USD", 4.22), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende08WithSecurityInCHF()
    {
        var security = new Security("SIMPLIFY BITCOIN STGY INC ETF", "CHF");
        security.setIsin("US82889N6739");
        security.setWkn("122171132");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "USD");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-02-28T00:00"), hasExDate(null), //
                        hasShares(281.00), //
                        hasSource("Dividende08.txt"), //
                        hasNote("Referenz: 795604930"), //
                        hasAmount("USD", 23.88), hasGrossValue("USD", 28.10), //
                        hasForexGrossValue("CHF", 25.28), //
                        hasTaxes("USD", 4.22), hasFees("USD", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("USD");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende09()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

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
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US37954Y4594"), hasWkn("47537082"), hasTicker(null), //
                        hasName("GLOBAL X RUSSELL 2000 CVRED CALL ET F"), //
                        hasCurrencyCode("USD"))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-03T00:00"), hasExDate(null), //
                        hasShares(300.00), //
                        hasSource("Dividende09.txt"), //
                        hasNote("Referenz: 532146499"), //
                        hasAmount("USD", 41.49), hasGrossValue("USD", 48.81), //
                        hasTaxes("USD", 7.32), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende09WithSecurityInCHF()
    {
        var security = new Security("GLOBAL X RUSSELL 2000 CVRED CALL ET F", "CHF");
        security.setIsin("US37954Y4594");
        security.setWkn("47537082");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "USD");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-03T00:00"), hasExDate(null), //
                        hasShares(300.00), //
                        hasSource("Dividende09.txt"), //
                        hasNote("Referenz: 532146499"), //
                        hasAmount("USD", 41.49), hasGrossValue("USD", 48.81), //
                        hasForexGrossValue("CHF", 44.05), //
                        hasTaxes("USD", 7.32), hasFees("USD", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("USD");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende10()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

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
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US87612E1064"), hasWkn("1036943"), hasTicker(null), //
                        hasName("TARGET ORD"), //
                        hasCurrencyCode("USD"))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-03T00:00"), hasExDate(null), //
                        hasShares(8.00), //
                        hasSource("Dividende10.txt"), //
                        hasNote("Referenz: 055857024"), //
                        hasAmount("USD", 6.28), hasGrossValue("USD", 8.96), //
                        hasTaxes("USD", 2.68), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende10WithSecurityInCHF()
    {
        var security = new Security("TARGET ORD", "CHF");
        security.setIsin("US87612E1064");
        security.setWkn("1036943");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new SwissquotePDFExtractor(client);

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
        new AssertImportActions().check(results, "USD");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-03T00:00"), hasExDate(null), //
                        hasShares(8.00), //
                        hasSource("Dividende10.txt"), //
                        hasNote("Referenz: 055857024"), //
                        hasAmount("USD", 6.28), hasGrossValue("USD", 8.96), //
                        hasForexGrossValue("CHF", 8.09), //
                        hasTaxes("USD", 2.68), hasFees("USD", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("USD");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende11()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

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
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US30303M1027"), hasWkn("14917609"), hasTicker(null), //
                        hasName("META PLATFORMS CL A ORD"), //
                        hasCurrencyCode("USD"))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-26T00:00"), hasExDate(null), //
                        hasShares(29.00), //
                        hasSource("Dividende11.txt"), //
                        hasNote("Referenz: 819926315"), //
                        hasAmount("USD", 10.67), hasGrossValue("USD", 15.23), //
                        hasTaxes("USD", 2.28 + 2.28), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende11WithSecurityInCHF()
    {
        var security = new Security("META PLATFORMS CL A ORD", "CHF");
        security.setIsin("US30303M1027");
        security.setWkn("14917609");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende11.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "USD");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-26T00:00"), hasExDate(null), //
                        hasShares(29.00), //
                        hasSource("Dividende11.txt"), //
                        hasNote("Referenz: 819926315"), //
                        hasForexGrossValue("CHF", 13.46), //
                        hasTaxes("USD", 2.28 + 2.28), hasFees("USD", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("USD");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende12()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("SE0015811955"), hasWkn("111465893"), hasTicker(null), //
                        hasName("INVESTOR ORD"), //
                        hasCurrencyCode("SEK"))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-05-14T00:00"), hasExDate(null), //
                        hasShares(175.00), //
                        hasSource("Dividende12.txt"), //
                        hasNote("Referenz: 847092627"), //
                        hasAmount("CHF", 38.87), hasGrossValue("CHF", 55.53), //
                        hasForexGrossValue("SEK", 656.25), //
                        hasTaxes("CHF", 16.66), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende12WithSecurityInCHF()
    {
        var security = new Security("INVESTOR ORD", "CHF");
        security.setIsin("SE0015811955");
        security.setWkn("111465893");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende12.txt"), errors);

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
                        hasDate("2025-05-14T00:00"), hasExDate(null), //
                        hasShares(175.00), //
                        hasSource("Dividende12.txt"), //
                        hasNote("Referenz: 847092627"), //
                        hasAmount("CHF", 38.87), hasGrossValue("CHF", 55.53), //
                        hasTaxes("CHF", 16.66), hasFees("CHF", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("CHF");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende13()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

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
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B5SSQT16"), hasWkn(null), hasTicker(null), //
                        hasName("HSBC MSCI EMERGING MARKETS UCI"), //
                        hasCurrencyCode("USD"))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-05-13T00:00"), hasExDate(null), //
                        hasShares(1000.00), //
                        hasSource("Dividende13.txt"), //
                        hasNote("Referenz: 249975908"), //
                        hasAmount("USD", 39.70), hasGrossValue("USD", 39.70), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende13WithSecurityInCHF()
    {
        var security = new Security("HSBC MSCI EMERGING MARKETS UCI", "CHF");
        security.setIsin("IE00B5SSQT16");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende13.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "USD");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-05-13T00:00"), hasExDate(null), //
                        hasShares(1000.00), //
                        hasSource("Dividende13.txt"), //
                        hasNote("Referenz: 249975908"), //
                        hasAmount("USD", 39.70), hasGrossValue("USD", 39.70), //
                        hasForexGrossValue("CHF", 39.81), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende14()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

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
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US26923G8226"), hasWkn("13983992"), hasTicker(null), //
                        hasName("VIRTUS INFRCP US PRERD STCK ETF"), //
                        hasCurrencyCode("USD"))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2026-01-27T00:00"), hasExDate(null), //
                        hasShares(900.00), //
                        hasSource("Dividende14.txt"), //
                        hasNote("Referenz: 2011421839"), //
                        hasAmount("USD", 131.96), hasGrossValue("USD", 155.26), //
                        hasTaxes("USD", 23.30), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende14WithSecurityInCHF()
    {
        var security = new Security("VIRTUS INFRCP US PRERD STCK ETF", "CHF");
        security.setIsin("US26923G8226");
        security.setWkn("13983992");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende14.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "USD");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2026-01-27T00:00"), hasExDate(null), //
                        hasShares(900.00), //
                        hasSource("Dividende14.txt"), //
                        hasNote("Referenz: 2011421839"), //
                        hasAmount("USD", 131.96), hasGrossValue("USD", 155.26), //
                        hasForexGrossValue("CHF", 120.69), //
                        hasTaxes("USD", 23.30), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende15()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

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
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("XS2852999775"), hasWkn("136957026"), hasTicker(null), //
                        hasName("INCOMESHARES GOLD YIELD ETP"), //
                        hasCurrencyCode("USD"))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2026-05-12T00:00"), hasExDate(null), //
                        hasShares(581.00), //
                        hasSource("Dividende15.txt"), //
                        hasNote("Referenz: 1097680988"), //
                        hasAmount("USD", 83.70), hasGrossValue("USD", 83.70), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende15WithSecurityInCHF()
    {
        var security = new Security("INCOMESHARES GOLD YIELD ETP", "CHF");
        security.setIsin("XS2852999775");
        security.setWkn("136957026");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende15.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "USD");

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2026-05-12T00:00"), hasExDate(null), //
                        hasShares(581.00), //
                        hasSource("Dividende15.txt"), //
                        hasNote("Referenz: 1097680988"), //
                        hasAmount("USD", 83.70), hasGrossValue("USD", 83.70), //
                        hasForexGrossValue("CHF", 65.45), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));
    }

    @Test
    public void testZahlungsverkehr01()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zahlungsverkehr01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2022-10-27"), hasAmount("EUR", 1000.00), //
                        hasSource("Zahlungsverkehr01.txt"), hasNote("Referenz: 312345678"))));
    }

    @Test
    public void testZahlungsverkehr02()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zahlungsverkehr02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2022-12-27"), hasAmount("EUR", 250.00), //
                        hasSource("Zahlungsverkehr02.txt"), hasNote("Referenz: 123456789"))));
    }

    @Test
    public void testZahlungsverkehr03()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zahlungsverkehr03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2022-12-04"), hasAmount("EUR", 11.00), //
                        hasSource("Zahlungsverkehr03.txt"), hasNote("Referenz: 123456789"))));
    }

    @Test
    public void testZahlungsverkehr04()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zahlungsverkehr04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2023-01-04"), hasAmount("EUR", 605.66), //
                        hasSource("Zahlungsverkehr04.txt"), hasNote("Referenz: 567891234"))));
    }

    @Test
    public void testZahlungsverkehr05()
    {
        var client = new Client();

        var extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zahlungsverkehr05.txt"), errors);

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
                        hasDate("2019-09-30"), //
                        hasSource("Zahlungsverkehr05.txt"), //
                        hasNote("Referenz: 32484929 | Depotgebühren"), //
                        hasAmount("CHF", 28.55), hasGrossValue("CHF", 28.55), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testZinsabrechnung01()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zinsabrechnung01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "CHF", "USD", "EUR");

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2022-12-30"), //
                        hasSource("Zinsabrechnung01.txt"), //
                        hasNote("Zinsabrechnung 05.09.2022 - 31.12.2022"), //
                        hasAmount("CHF", 1.36), hasGrossValue("CHF", 1.36), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2022-12-30"), //
                        hasSource("Zinsabrechnung01.txt"), //
                        hasNote("Zinsabrechnung 05.09.2022 - 31.12.2022"), //
                        hasAmount("USD", 0.08), hasGrossValue("USD", 0.08), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2022-12-30"), //
                        hasSource("Zinsabrechnung01.txt"), //
                        hasNote("Zinsabrechnung 05.09.2022 - 31.12.2022"), //
                        hasAmount("EUR", 0.60), hasGrossValue("EUR", 0.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug01()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(7L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, "CHF", "USD", "EUR");

        // assert transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2023-03-31"), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote("Depotgebühren"), //
                        hasAmount("CHF", 20.00), hasGrossValue("CHF", 20.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // assert transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2023-06-30"), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote("Depotgebühren"), //
                        hasAmount("CHF", 20.00), hasGrossValue("CHF", 20.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // assert transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2023-09-29"), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote("Depotgebühren"), //
                        hasAmount("CHF", 20.00), hasGrossValue("CHF", 20.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // assert transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2023-12-29"), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote("Depotgebühren"), //
                        hasAmount("CHF", 20.00), hasGrossValue("CHF", 20.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // assert transaction
        assertThat(results, hasItem(interestCharge( //
                        hasDate("2023-12-31"), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote("Sollzinsen"), //
                        hasAmount("CHF", 127.85), hasGrossValue("CHF", 127.85), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // assert transaction
        assertThat(results, hasItem(interestCharge( //
                        hasDate("2023-12-31"), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote("Sollzinsen"), //
                        hasAmount("EUR", 18.56), hasGrossValue("EUR", 18.56), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(interestCharge( //
                        hasDate("2023-12-31"), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote("Sollzinsen"), //
                        hasAmount("USD", 41.39), hasGrossValue("USD", 41.39), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));
    }

    @Test
    public void testKontoauszug02()
    {
        var extractor = new SwissquotePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug02.txt"), errors);

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
        assertThat(results, hasItem(deposit(hasDate("2026-04-08"), hasAmount("CHF", 40000.00), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));
    }
}
