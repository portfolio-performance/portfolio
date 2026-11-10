package name.abuchen.portfolio.online.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.online.QuoteFeedException;
import name.abuchen.portfolio.online.impl.SiftingQuoteFeed.AssetClass;

@SuppressWarnings("nls")
public class SiftingQuoteFeedTest
{
    private static final String RESPONSE = """
                    {
                      "data": [
                        {
                          "t": 1788998400000,
                          "o": 78300.65,
                          "h": 78554.35,
                          "l": 76469.95,
                          "c": 76574.15,
                          "v": 8200.997520470006
                        },
                        {
                          "t": 1789084800000,
                          "o": 76574.15,
                          "h": 79893.15,
                          "l": 76070.15,
                          "c": 77225.705,
                          "v": 8013.5065939599945
                        }
                      ],
                      "meta": {
                        "as_of": "2026-09-12T13:09:38Z",
                        "next_cursor": "U0ZUQ1IxfDE1NzgxODI0MDAwMDA",
                        "symbol": "BTCUSD",
                        "interval": "1d"
                      }
                    }
                    """;

    @Test
    public void testExtractBars()
    {
        QuoteFeedData data = new QuoteFeedData();

        new SiftingQuoteFeed().extract(RESPONSE, data);

        assertTrue(data.getErrors().isEmpty()); // NOSONAR
        assertThat(data.getLatestPrices().size(), is(2));

        LatestSecurityPrice price = data.getLatestPrices().get(1);
        assertThat(price.getDate(), is(LocalDate.of(2026, 9, 11)));
        assertThat(price.getValue(), is(Values.Quote.factorize(77225.705)));
        assertThat(price.getHigh(), is(Values.Quote.factorize(79893.15)));
        assertThat(price.getLow(), is(Values.Quote.factorize(76070.15)));
        assertThat(price.getVolume(), is(8013L));
    }

    @Test
    public void testExtractSkipsBarsWithoutClose()
    {
        String response = """
                        {
                          "data": [
                            { "t": 1789084800000, "o": 1.0, "h": 1.0, "l": 1.0, "v": 1.0 },
                            { "t": 1789171200000, "o": 1.0, "h": 1.1, "l": 0.9, "c": 1.05, "v": 2.0 }
                          ],
                          "meta": { "symbol": "EURUSD", "interval": "1d" }
                        }
                        """;

        QuoteFeedData data = new QuoteFeedData();

        new SiftingQuoteFeed().extract(response, data);

        assertThat(data.getLatestPrices().size(), is(1));
        assertThat(data.getLatestPrices().get(0).getValue(), is(Values.Quote.factorize(1.05)));
    }

    @Test
    public void testExtractEmptyResult()
    {
        QuoteFeedData data = new QuoteFeedData();

        new SiftingQuoteFeed().extract("""
                        { "data": [], "meta": { "symbol": "EURUSD", "interval": "1d" } }
                        """, data);

        assertTrue(data.getLatestPrices().isEmpty()); // NOSONAR
        assertTrue(data.getErrors().isEmpty()); // NOSONAR
    }

    @Test
    public void testPagingInformation()
    {
        SiftingQuoteFeed feed = new SiftingQuoteFeed();

        assertThat(feed.countDataRows(RESPONSE), is(2));
        assertThat(feed.extractLastDate(RESPONSE), is(LocalDate.of(2026, 9, 11)));
    }

    @Test
    public void testPagingInformationOfEmptyAnswer()
    {
        String response = """
                        {
                          "data": [],
                          "meta": { "as_of": "2026-09-12T14:48:20Z", "symbol": "BTCUSD", "interval": "1d" }
                        }
                        """;

        SiftingQuoteFeed feed = new SiftingQuoteFeed();

        assertThat(feed.countDataRows(response), is(0));
        assertThat(feed.extractLastDate(response), is(nullValue()));
    }

    @Test
    public void testPagingCountsBarsWithoutClosingPrice()
    {
        // the bar without a closing price is not converted into a price, but it
        // still counts towards the page size
        String response = """
                        {
                          "data": [
                            { "t": 1789084800000, "o": 1.0, "h": 1.0, "l": 1.0, "v": 1.0 },
                            { "t": 1789171200000, "o": 1.0, "h": 1.1, "l": 0.9, "c": 1.05, "v": 2.0 }
                          ],
                          "meta": { "symbol": "EURUSD", "interval": "1d" }
                        }
                        """;

        SiftingQuoteFeed feed = new SiftingQuoteFeed();

        assertThat(feed.countDataRows(response), is(2));
        assertThat(feed.extractLastDate(response), is(LocalDate.of(2026, 9, 12)));
    }

    @Test
    public void testAssetClassFallsBackToEquities()
    {
        assertThat(AssetClass.of(null), is(AssetClass.STOCKS));
        assertThat(AssetClass.of(""), is(AssetClass.STOCKS));
        assertThat(AssetClass.of("no-such-asset-class"), is(AssetClass.STOCKS));

        assertThat(AssetClass.of("forex"), is(AssetClass.FOREX));
        assertThat(AssetClass.of("FOREX"), is(AssetClass.FOREX));
        assertThat(AssetClass.of("crypto"), is(AssetClass.CRYPTO));
        assertThat(AssetClass.of("commodities"), is(AssetClass.COMMODITIES));

        assertThat(AssetClass.STOCKS.getPathSegment(), is("stocks"));
    }

    @Test
    public void testNoApiKey() throws QuoteFeedException
    {
        Security security = new Security("Bitcoin", "USD");
        security.setTickerSymbol("BTCUSD");

        QuoteFeedData data = new SiftingQuoteFeed().getHistoricalQuotes(security, false);

        assertTrue(data.getLatestPrices().isEmpty()); // NOSONAR
        assertFalse(data.getErrors().isEmpty());
    }

    @Test
    public void testNoTickerSymbol() throws QuoteFeedException
    {
        Security security = new Security("Bitcoin", "USD");

        SiftingQuoteFeed feed = new SiftingQuoteFeed();
        feed.setApiKey("api-key");

        QuoteFeedData data = feed.getHistoricalQuotes(security, false);

        assertTrue(data.getLatestPrices().isEmpty()); // NOSONAR
        assertFalse(data.getErrors().isEmpty());
    }
}
