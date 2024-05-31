package name.abuchen.portfolio.datatransfer.pdf.bison;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFeed;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFeedProperty;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFees;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasName;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasShares;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTaxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTicker;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.inboundDelivery;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.BisonPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.TestCoinSearchProvider;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.online.SecuritySearchProvider;
import name.abuchen.portfolio.online.impl.CoinGeckoQuoteFeed;

@SuppressWarnings("nls")
public class BisonPDFExtractorTest
{
    BisonPDFExtractor extractor = new BisonPDFExtractor(new Client())
    {
        @Override
        protected List<SecuritySearchProvider> lookupCryptoProvider()
        {
            return TestCoinSearchProvider.cryptoProvider();
        }
    };

    @Test
    public void testInfoReport01()
    {
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "InfoReport01.txt"), errors);

        if (!errors.isEmpty())
            errors.get(0).printStackTrace();

        assertThat(errors, empty());
        // 13 transactions + 2 securities
        assertThat(results.size(), is(15));
        new AssertImportActions().check(results, "EUR");

        // check crypto currencies
        assertThat(results,
                        hasItem(security(hasTicker("BTC"), hasName("Bitcoin"), hasCurrencyCode("EUR"),
                                        hasFeed(CoinGeckoQuoteFeed.ID),
                                        hasFeedProperty(CoinGeckoQuoteFeed.COINGECKO_COIN_ID, "bitcoin"))));
        assertThat(results,
                        hasItem(security(hasTicker("ETH"), hasName("Ethereum"), hasCurrencyCode("EUR"),
                                        hasFeed(CoinGeckoQuoteFeed.ID),
                                        hasFeedProperty(CoinGeckoQuoteFeed.COINGECKO_COIN_ID, "ethereum"))));

        // check transactions
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-12-28T09:00"), hasShares(0.00028505), //
                        hasSource("InfoReport01.txt"), hasNote(null), //
                        hasAmount("EUR", 12.50), hasGrossValue("EUR", 12.50), //
                        hasTaxes("EUR", 0), hasFees("EUR", 0))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2021-11-30T09:01"), hasShares(0.00318593), //
                        hasSource("InfoReport01.txt"), hasNote(null), //
                        hasAmount("EUR", 12.50), hasGrossValue("EUR", 12.50), //
                        hasTaxes("EUR", 0), hasFees("EUR", 0))));

        assertThat(results, hasItem(deposit( //
                        hasDate("2021-11-29T08:49"), //
                        hasSource("InfoReport01.txt"), hasNote(null), //
                        hasAmount("EUR", 250), hasGrossValue("EUR", 250), //
                        hasTaxes("EUR", 0), hasFees("EUR", 0))));
    }

    @Test
    public void testInfoReport02()
    {
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "InfoReport02.txt"), errors);

        if (!errors.isEmpty())
            errors.get(0).printStackTrace();

        assertThat(errors, empty());
        // 2 transactions + 2 securities
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check crypto currencies
        assertThat(results,
                        hasItem(security(hasTicker("BTC"), hasName("Bitcoin"), hasCurrencyCode("EUR"),
                                        hasFeed(CoinGeckoQuoteFeed.ID),
                                        hasFeedProperty(CoinGeckoQuoteFeed.COINGECKO_COIN_ID, "bitcoin"))));
        assertThat(results,
                        hasItem(security(hasTicker("ETH"), hasName("Ethereum"), hasCurrencyCode("EUR"),
                                        hasFeed(CoinGeckoQuoteFeed.ID),
                                        hasFeedProperty(CoinGeckoQuoteFeed.COINGECKO_COIN_ID, "ethereum"))));

        // check transactions
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-12T21:19"), hasShares(0.00180296), //
                        hasSource("InfoReport02.txt"), hasNote(null), //
                        hasAmount("EUR", 47.62), hasGrossValue("EUR", 47.62), //
                        hasTaxes("EUR", 0), hasFees("EUR", 0))));

        assertThat(results, hasItem(sale( //
                        hasDate("2022-06-12T21:19"), hasShares(0.03396843), //
                        hasSource("InfoReport02.txt"), hasNote(null), //
                        hasAmount("EUR", 47.62), hasGrossValue("EUR", 47.62), //
                        hasTaxes("EUR", 0), hasFees("EUR", 0))));
    }

    @Test
    public void testInfoReport03()
    {
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "InfoReport03.txt"), errors);

        if (!errors.isEmpty())
            errors.get(0).printStackTrace();

        assertThat(errors, empty());
        // 5 transactions + 1 security
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // check crypto currencies
        assertThat(results,
                        hasItem(security(hasTicker("BTC"), hasName("Bitcoin"), hasCurrencyCode("EUR"),
                                        hasFeed(CoinGeckoQuoteFeed.ID),
                                        hasFeedProperty(CoinGeckoQuoteFeed.COINGECKO_COIN_ID, "bitcoin"))));

        // check transactions

        assertThat(results, hasItem(removal( //
                        hasDate("2020-11-22T09:51"), //
                        hasSource("InfoReport03.txt"), hasNote(null), //
                        hasAmount("EUR", 100), hasGrossValue("EUR", 100), //
                        hasTaxes("EUR", 0), hasFees("EUR", 0))));

        assertThat(results, hasItem(sale( //
                        hasDate("2020-11-21T19:45"), hasShares(0.00636567), //
                        hasSource("InfoReport03.txt"), hasNote(null), //
                        hasAmount("EUR", 100), hasGrossValue("EUR", 100), //
                        hasTaxes("EUR", 0), hasFees("EUR", 0))));

        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2020-01-16T11:19"), hasShares(0.00130130), //
                        hasSource("InfoReport03.txt"), hasNote(null), //
                        hasAmount("EUR", 10), hasGrossValue("EUR", 10), //
                        hasTaxes("EUR", 0), hasFees("EUR", 0))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2020-01-16T11:19"), hasShares(0.01282436), //
                        hasSource("InfoReport03.txt"), hasNote(null), //
                        hasAmount("EUR", 100), hasGrossValue("EUR", 100), //
                        hasTaxes("EUR", 0), hasFees("EUR", 0))));

        assertThat(results, hasItem(deposit( //
                        hasDate("2020-01-16T10:30"), //
                        hasSource("InfoReport03.txt"), hasNote(null), //
                        hasAmount("EUR", 100), hasGrossValue("EUR", 100), //
                        hasTaxes("EUR", 0), hasFees("EUR", 0))));

    }

}
