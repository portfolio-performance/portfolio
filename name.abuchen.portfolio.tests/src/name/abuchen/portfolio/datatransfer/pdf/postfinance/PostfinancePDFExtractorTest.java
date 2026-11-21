package name.abuchen.portfolio.datatransfer.pdf.postfinance;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.check;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.fee;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFeed;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFeedProperty;
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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
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
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.PostfinancePDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.TestCoinSearchProvider;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.SecuritySearchProvider;
import name.abuchen.portfolio.online.impl.CoinGeckoQuoteFeed;

@SuppressWarnings("nls")
public class PostfinancePDFExtractorTest
{
    PostfinancePDFExtractor extractor = new PostfinancePDFExtractor(new Client())
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

        var extractor = new PostfinancePDFExtractor(client);

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
                        hasIsin("NL0000009355"), hasWkn(null), hasTicker(null), //
                        hasName("UNILEVER DUTCH CERT"), //
                        hasCurrencyCode("EUR"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-09-25T00:00"), hasShares(60.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Ref.-Nr.: 153557048"), //
                        hasAmount("EUR", 2850.24), hasGrossValue("EUR", 2837.40), //
                        hasTaxes("EUR", 4.26), hasFees("EUR", 8.58))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var client = new Client();

        var extractor = new PostfinancePDFExtractor(client);

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
                        hasIsin("CH0025751329"), hasWkn(null), hasTicker(null), //
                        hasName("LOGITECH N"), //
                        hasCurrencyCode("CHF"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-10-25T00:00"), hasShares(87.00), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Ref.-Nr.: 155571206"), //
                        hasAmount("CHF", 2998.95), hasGrossValue("CHF", 2970.20), //
                        hasTaxes("CHF", 2.25), hasFees("CHF", (25.00 + 1.50)))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        var client = new Client();

        var extractor = new PostfinancePDFExtractor(client);

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
                        hasIsin("DE000PAH0038"), hasWkn(null), hasTicker(null), //
                        hasName("PORSCHE AUTOMOBIL HOLDING PRF"), //
                        hasCurrencyCode("EUR"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-03-27T00:00"), hasShares(55.00), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Ref.-Nr.: 122007844"), //
                        hasAmount("CHF", 2968.50), hasGrossValue("CHF", 2963.40), //
                        hasForexGrossValue("EUR", 2736.80), //
                        hasTaxes("CHF", 4.45), hasFees("CHF", 0.60 * 1.08279))));
    }

    @Test
    public void testWertpapierKauf03WithSecurityInCHF()
    {
        var security = new Security("PORSCHE AUTOMOBIL HOLDING PRF", "CHF");
        security.setIsin("DE000PAH0038");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new PostfinancePDFExtractor(client);

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

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-03-27T00:00"), hasShares(55.00), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Ref.-Nr.: 122007844"), //
                        hasAmount("CHF", 2968.50), hasGrossValue("CHF", 2963.40), //
                        hasTaxes("CHF", 4.45), hasFees("CHF", 0.60 * 1.08279), //
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

        var extractor = new PostfinancePDFExtractor(client);

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
                        hasIsin("LU0188802960"), hasWkn(null), hasTicker(null), //
                        hasName("Pictet - Japan Index - I"), //
                        hasCurrencyCode("JPY"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-12-20T00:00"), hasShares(1.441), //
                        hasSource("Kauf04.txt"), //
                        hasNote("Auftrag 10111111"), //
                        hasAmount("CHF", 280.91), hasGrossValue("CHF", 280.49), //
                        hasForexGrossValue("JPY", 34019.00), //
                        hasTaxes("CHF", 0.42), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierKauf04WithSecurityInCHF()
    {
        var security = new Security("Pictet - Japan Index - I", "CHF");
        security.setIsin("LU0188802960");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new PostfinancePDFExtractor(client);

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

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-12-20T00:00"), hasShares(1.441), //
                        hasSource("Kauf04.txt"), //
                        hasNote("Auftrag 10111111"), //
                        hasAmount("CHF", 280.91), hasGrossValue("CHF", 280.49), //
                        hasTaxes("CHF", 0.42), hasFees("CHF", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf05()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

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
                        hasIsin("CH0014933193"), hasWkn(null), hasTicker(null), //
                        hasName("PF - Global Fund A"), //
                        hasCurrencyCode("CHF"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-07-31T00:00"), hasShares(1.216), //
                        hasSource("Kauf05.txt"), //
                        hasNote("Auftrag 15186945"), //
                        hasAmount("CHF", 200.00), hasGrossValue("CHF", 198.02), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.98))));
    }

    @Test
    public void testWertpapierKauf06()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0105994401"), hasWkn(null), hasTicker(null), //
                        hasName("UBS ETF - SXI Real Estate Funds"), //
                        hasCurrencyCode("CHF"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-01-14T00:00"), hasShares(14.000), //
                        hasSource("Kauf06.txt"), //
                        hasNote("Auftrag 19888922"), //
                        hasAmount("CHF", 141.28), hasGrossValue("CHF", 141.17), //
                        hasTaxes("CHF", 0.11), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var client = new Client();

        var extractor = new PostfinancePDFExtractor(client);

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
                        hasIsin("IE00BD4TYL27"), hasWkn(null), hasTicker(null), //
                        hasName("UBSETF MSCI USA hdg to CHF"), //
                        hasCurrencyCode("CHF"))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2018-09-20T00:00"), hasShares(308.00), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Ref.-Nr.: 153327044"), //
                        hasAmount("CHF", 7467.50), hasGrossValue("CHF", 7481.30), //
                        hasTaxes("CHF", 11.20), hasFees("CHF", 2.60))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

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
                        hasIsin("CH0482006191"), hasWkn(null), hasTicker(null), //
                        hasName("UBS Index Fund - Bonds"), //
                        hasCurrencyCode("CHF"))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-04-16T00:00"), hasShares(0.074), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Auftrag 21187458"), //
                        hasAmount("CHF", 71.14), hasGrossValue("CHF", 71.14), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

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
                        hasIsin("LU0309035367"), hasWkn(null), hasTicker(null), //
                        hasName("Pictet - Short-Term Money Market"), //
                        hasCurrencyCode("JPY"))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-06-18T00:00"), hasShares(0.553), //
                        hasSource("Verkauf03.txt"), //
                        hasNote("Auftrag 21796086"), //
                        hasAmount("CHF", 310.79), hasGrossValue("CHF", 310.79), //
                        hasForexGrossValue("JPY", 55384.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testtestWertpapierVerkauf03WithSecurityInCHF()
    {
        var security = new Security("Pictet - Short-Term Money Market", "CHF");
        security.setIsin("LU0309035367");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new PostfinancePDFExtractor(client);

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

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-06-18T00:00"), hasShares(0.553), //
                        hasSource("Verkauf03.txt"), //
                        hasNote("Auftrag 21796086"), //
                        hasAmount("CHF", 310.79), hasGrossValue("CHF", 310.79), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf04()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

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
                        hasIsin("CH0352765355"), hasWkn(null), hasTicker(null), //
                        hasName("Credit Suisse Index Fund (CH) II - Gold Blue - QB"), //
                        hasCurrencyCode("USD"))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-03-13T00:00"), hasShares(0.211), //
                        hasSource("Verkauf04.txt"), //
                        hasNote("Auftrag 16621393"), //
                        hasAmount("CHF", 328.93), hasGrossValue("CHF", 328.93), //
                        hasForexGrossValue("USD", 376.83), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testtestWertpapierVerkauf04WithSecurityInCHF()
    {
        var security = new Security("Credit Suisse Index Fund (CH) II - Gold Blue - QB", "CHF");
        security.setIsin("CH0352765355");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new PostfinancePDFExtractor(client);

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
        new AssertImportActions().check(results, "CHF");

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-03-13T00:00"), hasShares(0.211), //
                        hasSource("Verkauf04.txt"), //
                        hasNote("Auftrag 16621393"), //
                        hasAmount("CHF", 328.93), hasGrossValue("CHF", 328.93), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("BTC"), //
                        hasName("Bitcoin"), //
                        hasCurrencyCode("USD"), //
                        hasFeed(CoinGeckoQuoteFeed.ID), //
                        hasFeedProperty(CoinGeckoQuoteFeed.COINGECKO_COIN_ID, "bitcoin"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-12-01T00:00"), hasShares(0.006124), //
                        hasSource("CryptoKauf01.txt"), //
                        hasNote("Auftrag 37463178"), //
                        hasAmount("CHF", 538.73), hasGrossValue("CHF", 533.66), //
                        hasForexGrossValue("USD", 596.33), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 5.07))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

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
                        hasIsin("NL0000009355"), hasWkn(null), hasTicker(null), //
                        hasName("UNILEVER DUTCH CERT"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-06-05"), hasExDate("2019-05-02T00:00"), //
                        hasShares(60.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Ref.-Nr.: 169933304"), //
                        hasAmount("EUR", 20.93), hasGrossValue("EUR", 24.62), //
                        hasTaxes("EUR", 3.69), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende02()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

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
                        hasIsin("CH0032912732"), hasWkn(null), hasTicker(null), //
                        hasName("UBS ETF CH - SLI CHF A"), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2017-09-08"), hasExDate("2017-09-06T00:00"), //
                        hasShares(34.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Ref.-Nr.: 130516418"), //
                        hasAmount("CHF", 36.69), hasGrossValue("CHF", 56.44), //
                        hasTaxes("CHF", 19.75), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende03()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

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
                        hasIsin("CH0019396990"), hasWkn(null), hasTicker(null), //
                        hasName("YPSOMED HLDG"), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2018-07-05"), hasExDate("2018-07-03T00:00"), //
                        hasShares(19.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote("Ref.-Nr.: 149619136"), //
                        hasAmount("CHF", 26.60), hasGrossValue("CHF", 26.60), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende04()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

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
                        hasIsin("IE00B9M04V95"), hasWkn(null), hasTicker(null), //
                        hasName("iShares J.P. Morgan USD EM Bond CHF Hedged ETF"), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-10-25T00:00"), hasExDate("2023-10-12T00:00"), hasShares(275.00), //
                        hasSource("Dividende04.txt"), //
                        hasNote("Ref.-Nr.: 7790541"), //
                        hasAmount("CHF", 4.07), hasGrossValue("CHF", 4.07), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende05()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

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
                        hasIsin("CH0105994401"), hasWkn(null), hasTicker(null), //
                        hasName("UBS ETF - SXI Real Estate Funds"), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-09-13T00:00"), hasExDate("2023-09-08T00:00"), hasShares(42.00), //
                        hasSource("Dividende05.txt"), //
                        hasNote("Ref.-Nr.: 7600589"), //
                        hasAmount("CHF", 3.28), hasGrossValue("CHF", 5.04), //
                        hasTaxes("CHF", 1.76), hasFees("CHF", 0.00))));
    }

    @Test
    public void testZahlungsverkehr01()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2023-05-02"), hasAmount("CHF", 1200.00), //
                        hasSource("Zahlungsverkehr01.txt"), hasNote("Ref.-Nr.: 391377700"))));
    }

    @Test
    public void testZahlungsverkehr02()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2017-01-31"), hasAmount("CHF", 5675.50), //
                        hasSource("Zahlungsverkehr02.txt"), hasNote("Ref.-Nr.: 119387423"))));
    }

    @Test
    public void testJahresgebuehr01()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Jahresgebuehr01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check fees transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2019-01-03"), //
                        hasSource("Jahresgebuehr01.txt"), //
                        hasNote("Jahresgebühr | Ref.-Nr.: 161333839"), //
                        hasAmount("CHF", 90.00), hasGrossValue("CHF", 90.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDepotgebuehr01()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotgebuehr01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check fees transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2024-01-01"), //
                        hasSource("Depotgebuehr01.txt"), //
                        hasNote("Depotgebühr | Ref.-Nr.: 464973467"), //
                        hasAmount("CHF", 18.00), hasGrossValue("CHF", 18.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDepotgebuehr02()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotgebuehr02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check fees transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2023-12-30"), //
                        hasSource("Depotgebuehr02.txt"), //
                        hasNote("R-Nr.: 2705944 | 01.10.2023 - 31.12.2023"), //
                        hasAmount("CHF", 16.12), hasGrossValue("CHF", 16.12), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testZinsabschluss01()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zinsabschluss01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check 1st interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2019-10-31"), //
                        hasSource("Zinsabschluss01.txt"), //
                        hasNote("01.01.2019 – 31.10.2019"), //
                        hasAmount("CHF", 4.17), hasGrossValue("CHF", 4.17), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check 2nd interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2019-12-31"), //
                        hasSource("Zinsabschluss01.txt"), //
                        hasNote("01.11.2019 – 31.12.2019"), //
                        hasAmount("CHF", 0.42), hasGrossValue("CHF", 0.42), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testZinsabschluss02()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zinsabschluss02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "CHF");

        // check 1st interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2019-10-31"), //
                        hasSource("Zinsabschluss02.txt"), //
                        hasNote("01.01.2019 – 31.10.2019"), //
                        hasAmount("CHF", 83.33), hasGrossValue("CHF", 83.33), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check 2nd interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2019-12-31"), //
                        hasSource("Zinsabschluss02.txt"), //
                        hasNote("01.11.2019 – 31.12.2019"), //
                        hasAmount("CHF", 33.33), hasGrossValue("CHF", 33.33), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2019-12-31"), //
                        hasSource("Zinsabschluss02.txt"), //
                        hasNote("Verrechnungssteuer 01.01.2019 - 31.12.2019"), //
                        hasAmount("CHF", 40.83), hasGrossValue("CHF", 40.83), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testZinsabschluss03()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zinsabschluss03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2015-12-31"), //
                        hasSource("Zinsabschluss03.txt"), //
                        hasNote("01.01.2015 - 31.12.2015"), //
                        hasAmount("CHF", 1.00), hasGrossValue("CHF", 1.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check fees transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2015-12-31"), //
                        hasSource("Zinsabschluss03.txt"), //
                        hasNote("Gebührenausweis 01.01.2015 - 31.12.2015"), //
                        hasAmount("CHF", 60.00), hasGrossValue("CHF", 60.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testZinsabschluss04()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zinsabschluss04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "CHF");

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2017-12-31"), //
                        hasSource("Zinsabschluss04.txt"), //
                        hasNote("01.01.2017 - 31.12.2017"), //
                        hasAmount("CHF", 400.00), hasGrossValue("CHF", 400.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2017-12-31"), //
                        hasSource("Zinsabschluss04.txt"), //
                        hasNote("Verrechnungssteuer 01.01.2017 - 31.12.2017"), //
                        hasAmount("CHF", 140.00), hasGrossValue("CHF", 140.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check fees transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2017-12-31"), //
                        hasSource("Zinsabschluss04.txt"), //
                        hasNote("Gebührenausweis 01.01.2017 - 31.12.2017"), //
                        hasAmount("CHF", 60.00), hasGrossValue("CHF", 60.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testKontoauszug01()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(77L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(77));
        new AssertImportActions().check(results, "CHF");

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-01"), hasAmount("CHF", 100.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Auftrag CH-DD-Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-01"), hasAmount("CHF", 150.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Auftrag CH-DD-Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-01"), hasAmount("CHF", 250.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Auftrag CH-DD-Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-02"), hasAmount("CHF", 250.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Lastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-02"), hasAmount("CHF", 150.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Lastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-02"), hasAmount("CHF", 100.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Lastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-02"), hasAmount("CHF", 250.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Oranger Einzahlungsschein"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-02"), hasAmount("CHF", 150.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Oranger Einzahlungsschein"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-02"), hasAmount("CHF", 100.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Oranger Einzahlungsschein"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-03"), hasAmount("CHF", 25.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Kauf/Dienstleistung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-03"), hasAmount("CHF", 15.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Kauf/Dienstleistung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-03"), hasAmount("CHF", 10.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Kauf/Dienstleistung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-04"), hasAmount("CHF", 2000.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Übertrag auf Konto"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-04"), hasAmount("CHF", 1000.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Übertrag auf Konto"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-04"), hasAmount("CHF", 500.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Übertrag auf Konto"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-05"), hasAmount("CHF", 1300.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Giro Post"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-05"), hasAmount("CHF", 150.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Giro Post"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-05"), hasAmount("CHF", 200.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Giro Post"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-06"), hasAmount("CHF", 1000.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Giro Bank"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-06"), hasAmount("CHF", 100.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Giro Bank"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-06"), hasAmount("CHF", 200.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Giro Bank"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-07"), hasAmount("CHF", 10.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Kauf/Online Shopping"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-07"), hasAmount("CHF", 15.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Kauf/Online Shopping"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-07"), hasAmount("CHF", 25.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Kauf/Online Shopping"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-07"), hasAmount("CHF", 150.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Bargeldbezug vom 07.04.2018"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-07"), hasAmount("CHF", 100.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Bargeldbezug vom 07.04.2018"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-07"), hasAmount("CHF", 100.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Bargeldbezug vom 07.04.2018"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-08"), hasAmount("CHF", 100.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Bargeldbezug"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-08"), hasAmount("CHF", 150.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Bargeldbezug"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-08"), hasAmount("CHF", 250.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Bargeldbezug"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-08"), hasAmount("CHF", 100.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Kauf/Dienstleistung vom 08.04.2018"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-08"), hasAmount("CHF", 150.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Kauf/Dienstleistung vom 08.04.2018"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-08"), hasAmount("CHF", 250.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Kauf/Dienstleistung vom 08.04.2018"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-08"), hasAmount("CHF", 100.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Kauf/Dienstleistung vom 08.04.2018"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-08"), hasAmount("CHF", 150.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Kauf/Dienstleistung vom 08.04.2018"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-08"), hasAmount("CHF", 250.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Kauf/Dienstleistung vom 08.04.2018"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-08"), hasAmount("CHF", 5.25), //
                        hasSource("Kontoauszug01.txt"), hasNote("Kauf/Dienstleistung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-09"), hasAmount("CHF", 10.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Kauf/Online Shopping vom 09.04.2018"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-09"), hasAmount("CHF", 15.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Kauf/Online Shopping vom 09.04.2018"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-09"), hasAmount("CHF", 25.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Kauf/Online Shopping vom 09.04.2018"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-09"), hasAmount("CHF", 10.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Kauf/Online Shopping vom 09.04.2018"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-09"), hasAmount("CHF", 15.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Kauf/Online Shopping vom 09.04.2018"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-09"), hasAmount("CHF", 25.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Kauf/Online Shopping vom 09.04.2018"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-10"), hasAmount("CHF", 10.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("TWINT Geld senden"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-10"), hasAmount("CHF", 15.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("TWINT Geld senden"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-10"), hasAmount("CHF", 25.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("TWINT Geld senden"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-10"), hasAmount("CHF", 10.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("TWINT Geld senden"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-10"), hasAmount("CHF", 15.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("TWINT Geld senden"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-10"), hasAmount("CHF", 25.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("TWINT Geld senden"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-10"), hasAmount("CHF", 10.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("TWINT Kauf/Dienstleistung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-10"), hasAmount("CHF", 15.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("TWINT Kauf/Dienstleistung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-10"), hasAmount("CHF", 25.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("TWINT Kauf/Dienstleistung"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2018-04-10"), hasAmount("CHF", 10.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("TWINT Geld empfangen"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2018-04-10"), hasAmount("CHF", 15.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("TWINT Geld empfangen"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2018-04-10"), hasAmount("CHF", 25.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("TWINT Geld empfangen"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2018-04-11"), hasAmount("CHF", 11.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("TWINT Geld empfangen"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2018-04-11"), hasAmount("CHF", 16.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("TWINT Geld empfangen"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2018-04-11"), hasAmount("CHF", 26.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("TWINT Geld empfangen"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2018-04-20"), hasAmount("CHF", 10000.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Gutschrift von Fremdbank"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2018-04-20"), hasAmount("CHF", 600.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Gutschrift von Fremdbank"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2018-04-20"), hasAmount("CHF", 500.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Gutschrift von Fremdbank"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2018-04-21"), hasAmount("CHF", 50.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Einzahlschein/QR-Zahlteil"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2018-04-21"), hasAmount("CHF", 150.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Einzahlschein/QR-Zahlteil"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2018-04-21"), hasAmount("CHF", 250.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Einzahlschein/QR-Zahlteil"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2018-04-22"), hasAmount("CHF", 50.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Gutschrift Online Shopping"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2018-04-22"), hasAmount("CHF", 150.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Gutschrift Online Shopping"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2018-04-22"), hasAmount("CHF", 250.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Gutschrift Online Shopping"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2018-04-23"), hasAmount("CHF", 10.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Gutschrift"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2018-04-23"), hasAmount("CHF", 10.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Gutschrift"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2018-04-23"), hasAmount("CHF", 10.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Gutschrift"))));

        // check fees transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2018-04-30"), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote(""), //
                        hasAmount("CHF", 11.00), hasGrossValue("CHF", 11.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check fees transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2018-04-30"), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote(""), //
                        hasAmount("CHF", 10.00), hasGrossValue("CHF", 10.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check fees transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2018-04-30"), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote(""), //
                        hasAmount("CHF", 9.00), hasGrossValue("CHF", 9.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check fees transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2018-04-30"), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote("Kontoführungsgebühr"), //
                        hasAmount("CHF", 5.00), hasGrossValue("CHF", 5.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check fees transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2018-04-30"), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote("Einzahlungen am Schalter"), //
                        hasAmount("CHF", 2.40), hasGrossValue("CHF", 2.40), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check fees transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2018-04-30"), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote("Guthabengebühr für 03.2018"), //
                        hasAmount("CHF", 15.00), hasGrossValue("CHF", 15.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2018-04-30"), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote("Zinsabschluss 01.01.2018 - 30.04.2018"), //
                        hasAmount("CHF", 1.00), hasGrossValue("CHF", 1.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testKontoauszug02()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(6L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "CHF");

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-09"), hasAmount("CHF", 20000.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Übertrag auf Konto"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-09"), hasAmount("CHF", 20000.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Übertrag auf Konto"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-12"), hasAmount("CHF", 40000.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Übertrag auf Konto"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2018-04-10"), hasAmount("CHF", 25000.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Übertrag aus Konto"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2018-04-11"), hasAmount("CHF", 25000.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Übertrag aus Konto"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2018-04-13"), hasAmount("CHF", 10000.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Übertrag aus Konto"))));
    }

    @Test
    public void testKontoauszug03()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(54L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(54));
        new AssertImportActions().check(results, "CHF");

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2015-04-01"), hasAmount("CHF", 250.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("E-FINANCE 01-22222-5"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2015-04-02"), hasAmount("CHF", 1000.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("E-FINANCE 01-22222-5"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2015-04-02"), hasAmount("CHF", 350.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("E-FINANCE 01-22222-5"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2015-04-03"), hasAmount("CHF", 250.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Auftrag DEBIT DIRECT"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2015-04-04"), hasAmount("CHF", 250.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Auftrag DEBIT DIRECT"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2015-04-04"), hasAmount("CHF", 1150.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Auftrag DEBIT DIRECT"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2015-04-05"), hasAmount("CHF", 200.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Kauf/Online Shopping"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2015-04-06"), hasAmount("CHF", 100.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Kauf/Online Shopping"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2015-04-06"), hasAmount("CHF", 150.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Kauf/Online Shopping"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2015-04-07"), hasAmount("CHF", 150.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Kauf/Online Shopping"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2015-04-08"), hasAmount("CHF", 100.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Kauf/Online Shopping"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2015-04-08"), hasAmount("CHF", 200.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Kauf/Online Shopping"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2015-04-09"), hasAmount("CHF", 50.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Bargeldbezug"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2015-04-10"), hasAmount("CHF", 100.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Bargeldbezug"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2015-04-10"), hasAmount("CHF", 150.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Bargeldbezug"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2015-04-11"), hasAmount("CHF", 50.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Online-Shopping"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2015-04-12"), hasAmount("CHF", 75.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Online-Shopping"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2015-04-12"), hasAmount("CHF", 25.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Online-Shopping"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2015-04-13"), hasAmount("CHF", 100.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Kauf/Dienstleistung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2015-04-14"), hasAmount("CHF", 50.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Kauf/Dienstleistung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2015-04-14"), hasAmount("CHF", 100.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Kauf/Dienstleistung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2015-04-15"), hasAmount("CHF", 100.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Auftrag CH-DD-Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2015-04-16"), hasAmount("CHF", 200.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Auftrag CH-DD-Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2015-04-16"), hasAmount("CHF", 300.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Auftrag CH-DD-Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2015-04-17"), hasAmount("CHF", 100.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Giro Internation (SEPA)"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2015-04-18"), hasAmount("CHF", 150.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Giro Internation (SEPA)"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2015-04-18"), hasAmount("CHF", 250.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Giro Internation (SEPA)"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2015-04-27"), hasAmount("CHF", 100.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Übertrag auf Konto"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2015-04-27"), hasAmount("CHF", 250.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Übertrag auf Konto"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2015-04-22"), hasAmount("CHF", 100.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Giro Ausland"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2015-04-22"), hasAmount("CHF", 150.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Giro Ausland"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2015-04-22"), hasAmount("CHF", 250.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Giro Ausland"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2015-04-22"), hasAmount("CHF", 1300.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Giro aus Online-SIC"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2015-04-22"), hasAmount("CHF", 2000.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Giro aus Online-SIC"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2015-04-22"), hasAmount("CHF", 250.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Giro aus Online-SIC"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2015-04-23"), hasAmount("CHF", 1000.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Giro aus Konto"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2015-04-23"), hasAmount("CHF", 500.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Giro aus Konto"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2015-04-23"), hasAmount("CHF", 250.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Giro aus Konto"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2015-04-24"), hasAmount("CHF", 100.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Gutschrift Online Shopping"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2015-04-24"), hasAmount("CHF", 500.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Gutschrift Online Shopping"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2015-04-24"), hasAmount("CHF", 250.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Gutschrift Online Shopping"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2015-04-25"), hasAmount("CHF", 100.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Gutschrift von Fremdbank"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2015-04-25"), hasAmount("CHF", 500.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Gutschrift von Fremdbank"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2015-04-25"), hasAmount("CHF", 1250.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Gutschrift von Fremdbank"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2015-04-26"), hasAmount("CHF", 100.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Gutschrift von Fremdbank"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2015-04-26"), hasAmount("CHF", 500.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Gutschrift von Fremdbank"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2015-04-26"), hasAmount("CHF", 250.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Gutschrift von Fremdbank"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2015-04-27"), hasAmount("CHF", 150.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Übertrag aus Konto"))));

        // check fees transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2015-04-30"), //
                        hasSource("Kontoauszug03.txt"), //
                        hasNote("Überweisungsgebühr (SEPA)"), //
                        hasAmount("CHF", 1.00), hasGrossValue("CHF", 1.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check fees transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2015-04-30"), //
                        hasSource("Kontoauszug03.txt"), //
                        hasNote("Kontoführungsgebühr"), //
                        hasAmount("CHF", 2.00), hasGrossValue("CHF", 2.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check fees transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2015-04-30"), //
                        hasSource("Kontoauszug03.txt"), //
                        hasNote("Kontoführungsgebühr"), //
                        hasAmount("CHF", 5.00), hasGrossValue("CHF", 5.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check fees transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2015-04-30"), //
                        hasSource("Kontoauszug03.txt"), //
                        hasNote("Jahrespreis Login"), //
                        hasAmount("CHF", 4.00), hasGrossValue("CHF", 4.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check fees transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2015-04-30"), //
                        hasSource("Kontoauszug03.txt"), //
                        hasNote("Kontoführungsgebühr (Papier)"), //
                        hasAmount("CHF", 24.00), hasGrossValue("CHF", 24.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2015-04-30"), //
                        hasSource("Kontoauszug03.txt"), //
                        hasNote("Zinsabschluss 01.04.2015 - 30.04.2015"), //
                        hasAmount("CHF", 1.00), hasGrossValue("CHF", 1.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testKontoauszug04()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2021-12-01"), hasAmount("CHF", 111.00), //
                        hasSource("Kontoauszug04.txt"), hasNote("Überweisung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2021-12-05"), hasAmount("CHF", 11.00), //
                        hasSource("Kontoauszug04.txt"), hasNote("Überweisung"))));
    }

    @Test
    public void testKontoauszug05()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug05MitTwintKauf.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(27L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(27));
        new AssertImportActions().check(results, "CHF");

        // check removal transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-11-27"), //
                        hasAmount("CHF", 13.90), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Kauf/Dienstleistung vom 27.11.2024"))));

        // check removal transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-02"), //
                        hasAmount("CHF", 24.00), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Kauf/Dienstleistung vom 02.12.2024"))));

        // check removal transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-08"), //
                        hasAmount("CHF", 3.20), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Kauf/Dienstleistung vom 08.12.2024"))));

        // check removal transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-09"), //
                        hasAmount("CHF", 3.20), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Kauf/Dienstleistung vom 09.12.2024"))));

        // check removal transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-09"), //
                        hasAmount("CHF", 20.00), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Kauf/Dienstleistung vom 09.12.2024"))));

        // check removal transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-09"), //
                        hasAmount("CHF", 5.00), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Kauf/Online Shopping"))));

        // check removal transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-10"), //
                        hasAmount("CHF", 13.80), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Kauf/Dienstleistung vom 10.12.2024"))));

        // check removal transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-11"), //
                        hasAmount("CHF", 16.50), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Kauf/Dienstleistung vom 11.12.2024"))));

        // check removal transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-11"), //
                        hasAmount("CHF", 18.00), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Kauf/Dienstleistung vom 11.12.2024"))));

        // check removal transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-12"), //
                        hasAmount("CHF", 25.00), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Kauf/Dienstleistung vom 12.12.2024"))));

        // check removal transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-13"), //
                        hasAmount("CHF", 8.20), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Kauf/Dienstleistung vom 13.12.2024"))));

        // check removal transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-13"), //
                        hasAmount("CHF", 20.00), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Kauf/Dienstleistung vom 13.12.2024"))));

        // check removal transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-14"), //
                        hasAmount("CHF", 28.85), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Kauf/Dienstleistung vom 14.12.2024"))));

        // check removal transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-12"), //
                        hasAmount("CHF", 8.80), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Kauf/Dienstleistung vom 12.12.2024"))));

        // check removal transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-15"), //
                        hasAmount("CHF", 21.00), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("TWINT Kauf/Dienstleistung"))));

        // check removal transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-16"), //
                        hasAmount("CHF", 26.00), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Kauf/Dienstleistung vom 16.12.2024"))));

        // check removal transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-16"), //
                        hasAmount("CHF", 5.70), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Kauf/Dienstleistung vom 16.12.2024"))));

        // check removal transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-17"), //
                        hasAmount("CHF", 25.00), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Kauf/Dienstleistung vom 17.12.2024"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit( //
                        hasDate("2024-12-20"), //
                        hasAmount("CHF", 6465.80), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Gutschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-24"), //
                        hasAmount("CHF", 2000.00), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Lastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-27"), //
                        hasAmount("CHF", 5056.00), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Lastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-27"), //
                        hasAmount("CHF", 2000.00), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Lastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-27"), //
                        hasAmount("CHF", 1720.00), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Lastschrift"))));

        // assert transaction
        // NOTE: This is an 'inboundDelivery' from the savings account instead
        // of a 'deposit', but finding the accompanying 'outboundDelivery' in
        // the savings accounts bank statement might be impossible
        assertThat(results, hasItem(deposit( //
                        hasDate("2024-12-27"), //
                        hasAmount("CHF", 2500.00), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Übertrag aus Konto CH0000000000000000000"))));

        // check removal transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-28"), //
                        hasAmount("CHF", 12.31), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Kauf/Dienstleistung vom 28.12.2024"))));

        // check fees transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2024-12-30"), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Depotgebühr"), //
                        hasAmount("CHF", 4.72), hasGrossValue("CHF", 4.72), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check fees transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2024-12-31"), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote(""), //
                        hasAmount("CHF", 5.00), hasGrossValue("CHF", 5.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testKontoauszug06()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check removal transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2025-01-04"), hasAmount("CHF", 70.30), //
                        hasSource("Kontoauszug06.txt"), hasNote("Kauf/Dienstleistung"))));
    }

    @Test
    public void testKontoauszug07()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug7MitPFPay.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "CHF");

        // check removal transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2026-03-01"), //
                        hasAmount("CHF", 30.00), //
                        hasSource("Kontoauszug7MitPFPay.txt"), //
                        hasNote("PF Pay Kauf/Online Shopping"))));

        // check removal transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2026-03-02"), //
                        hasAmount("CHF", 20.00), //
                        hasSource("Kontoauszug7MitPFPay.txt"), //
                        hasNote("PF Pay Kauf/Online Shopping"))));

        // check removal transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2026-03-03"), //
                        hasAmount("CHF", 10.00), //
                        hasSource("Kontoauszug7MitPFPay.txt"), //
                        hasNote("PF Pay Kauf/Online Shopping vom 03.03.2026"))));
    }
}
