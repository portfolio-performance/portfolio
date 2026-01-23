package name.abuchen.portfolio.datatransfer.pdf.bsdex;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFeed;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFeedProperty;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFees;
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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
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

import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.BSDEXPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.TestCoinSearchProvider;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.online.SecuritySearchProvider;
import name.abuchen.portfolio.online.impl.CoinGeckoQuoteFeed;

@SuppressWarnings("nls")
public class BSDEXPDFExtractorTest
{
    BSDEXPDFExtractor extractor = new BSDEXPDFExtractor(new Client())
    {
        @Override
        protected List<SecuritySearchProvider> lookupCryptoProvider()
        {
            return TestCoinSearchProvider.cryptoProvider();
        }
    };

    @Test
    public void testTransaktionshistorie01()
    {
        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionshistorie01.txt"), errors);

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
                        hasIsin(null), hasWkn(null), hasTicker("BTC"), //
                        hasName("Bitcoin"), //
                        hasCurrencyCode("EUR"), //
                        hasFeed(CoinGeckoQuoteFeed.ID), //
                        hasFeedProperty(CoinGeckoQuoteFeed.COINGECKO_COIN_ID, "bitcoin"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-12-18T04:14:42"), hasShares(0.001), //
                        hasSource("Transaktionshistorie01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 99.20), hasGrossValue("EUR", 99.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.20))));
    }

    @Test
    public void testTransaktionshistorie02()
    {
        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionshistorie02.txt"), errors);

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
                        hasIsin(null), hasWkn(null), hasTicker("BTC"), //
                        hasName("Bitcoin"), //
                        hasCurrencyCode("EUR"), //
                        hasFeed(CoinGeckoQuoteFeed.ID), //
                        hasFeedProperty(CoinGeckoQuoteFeed.COINGECKO_COIN_ID, "bitcoin"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-04-30T19:16:59"), hasShares(0.0001283), //
                        hasSource("Transaktionshistorie02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 7.19), hasGrossValue("EUR", 7.18), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.01))));
    }

    @Test
    public void testTransaktionshistorie03()
    {
        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionshistorie03.txt"), errors);

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
                        hasIsin(null), hasWkn(null), hasTicker("XRP"), //
                        hasName("XRP"), //
                        hasCurrencyCode("EUR"), //
                        hasFeed(CoinGeckoQuoteFeed.ID), //
                        hasFeedProperty(CoinGeckoQuoteFeed.COINGECKO_COIN_ID, "xrp"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-12-17T18:07:05"), hasShares(15.00), //
                        hasSource("Transaktionshistorie03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 37.72), hasGrossValue("EUR", 37.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.08))));
    }

    @Test
    public void testTransaktionshistorie04()
    {
        var extractor = new BSDEXPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionshistorie04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-06-14T06:00:26"), hasAmount("EUR", 25.50), //
                        hasSource("Transaktionshistorie04.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-17T06:30:23"), hasAmount("EUR", 113.66), //
                        hasSource("Transaktionshistorie04.txt"), hasNote(null))));
    }

    @Test
    public void testTransaktionshistorie05()
    {
        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionshistorie05.txt"), errors);

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
                        hasIsin(null), hasWkn(null), hasTicker("BTC"), //
                        hasName("Bitcoin"), //
                        hasCurrencyCode("EUR"), //
                        hasFeed(CoinGeckoQuoteFeed.ID), //
                        hasFeedProperty(CoinGeckoQuoteFeed.COINGECKO_COIN_ID, "bitcoin"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("ETH"), //
                        hasName("Ethereum"), //
                        hasCurrencyCode("EUR"), //
                        hasFeed(CoinGeckoQuoteFeed.ID), //
                        hasFeedProperty(CoinGeckoQuoteFeed.COINGECKO_COIN_ID, "ethereum"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-08-02T01:25:55"), hasShares(0.00553256), //
                        hasSource("Transaktionshistorie05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 151.18), hasGrossValue("EUR", 150.65), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.53))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-02-16T23:47:54"), hasShares(0.032057491), //
                        hasSource("Transaktionshistorie05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 49.01), hasGrossValue("EUR", 49.18), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.17))));
    }
}
