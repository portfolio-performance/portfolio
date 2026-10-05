package name.abuchen.portfolio.online.impl;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.online.QuoteFeedException;
import name.abuchen.portfolio.online.RateLimitExceededException;
import name.abuchen.portfolio.online.impl.FXMacroDataQuoteFeed.CurrencyPair;
import name.abuchen.portfolio.util.WebAccess;
import name.abuchen.portfolio.util.WebAccess.WebAccessException;

@SuppressWarnings("nls")
public class FXMacroDataQuoteFeedTest
{
    private static final String API_KEY = "test-api-key";

    // example response of the OpenAPI specification
    private static final String RESPONSE = """
                    {
                      "base": "USD",
                      "quote": "JPY",
                      "source": "Official central-bank reference rates",
                      "start_date": "2026-06-17",
                      "end_date": "2026-06-18",
                      "pagination": {
                        "limit": 100,
                        "offset": 0,
                        "returned_count": 2,
                        "total_count": 2,
                        "has_more": false,
                        "next_offset": null
                      },
                      "data": [
                        {
                          "date": "2026-06-18",
                          "val": 161.1288,
                          "observation_datetime": 1781740800,
                          "observation_datetime_iso": "2026-06-18T00:00:00Z",
                          "observation_datetime_precision": "date"
                        },
                        {
                          "date": "2026-06-17",
                          "val": 161.1288,
                          "observation_datetime": 1781654400,
                          "observation_datetime_iso": "2026-06-17T00:00:00Z",
                          "observation_datetime_precision": "date"
                        }
                      ]
                    }
                    """;

    private static final String FIRST_PAGE = """
                    {
                      "base": "USD",
                      "quote": "JPY",
                      "pagination": {
                        "limit": 100,
                        "offset": 0,
                        "returned_count": 2,
                        "total_count": 3,
                        "has_more": true,
                        "next_offset": 100
                      },
                      "data": [
                        { "date": "2026-06-18", "val": 161.1288 },
                        { "date": "2026-06-17", "val": 161.1288 }
                      ]
                    }
                    """;

    private static final String SECOND_PAGE = """
                    {
                      "base": "USD",
                      "quote": "JPY",
                      "pagination": {
                        "limit": 100,
                        "offset": 100,
                        "returned_count": 1,
                        "total_count": 3,
                        "has_more": false,
                        "next_offset": null
                      },
                      "data": [
                        { "date": "2026-06-16", "val": 160.95 }
                      ]
                    }
                    """;

    private static final String UNAUTHORIZED = """
                    {
                      "detail": "API key not recognised. Check your key or omit it for public endpoints.",
                      "code": "invalid_api_key"
                    }
                    """;

    private static Security security(String tickerSymbol)
    {
        var security = new Security("US Dollar / Japanese Yen", "JPY");
        security.setTickerSymbol(tickerSymbol);
        return security;
    }

    private static FXMacroDataQuoteFeed feed()
    {
        var feed = spy(new FXMacroDataQuoteFeed());
        feed.setApiKey(API_KEY);
        return feed;
    }

    @Test
    public void testParseCurrencyPair()
    {
        assertThat(CurrencyPair.parse("EUR/USD").orElseThrow(), is(new CurrencyPair("EUR", "USD")));
        assertThat(CurrencyPair.parse("EURUSD").orElseThrow(), is(new CurrencyPair("EUR", "USD")));
        assertThat(CurrencyPair.parse("eur-usd").orElseThrow(), is(new CurrencyPair("EUR", "USD")));
        assertThat(CurrencyPair.parse("EUR_USD").orElseThrow(), is(new CurrencyPair("EUR", "USD")));
        assertThat(CurrencyPair.parse(" gbp / jpy ").orElseThrow(), is(new CurrencyPair("GBP", "JPY")));
        assertThat(CurrencyPair.parse("AUDUSD=X").orElseThrow(), is(new CurrencyPair("AUD", "USD")));

        assertThat(CurrencyPair.parse("USD/JPY").orElseThrow().path(), is("/v1/forex/USD/JPY"));
    }

    @Test
    public void testParseInvalidCurrencyPair()
    {
        assertTrue(CurrencyPair.parse(null).isEmpty());
        assertTrue(CurrencyPair.parse("").isEmpty());
        assertTrue(CurrencyPair.parse("EUR").isEmpty());
        assertTrue(CurrencyPair.parse("AAPL").isEmpty());
        assertTrue(CurrencyPair.parse("EURUSDX").isEmpty());
        assertTrue(CurrencyPair.parse("EUR/US").isEmpty());
        assertTrue(CurrencyPair.parse("EUR/EUR").isEmpty());
    }

    @Test
    public void testExtractRates()
    {
        var data = new QuoteFeedData();

        int rows = new FXMacroDataQuoteFeed().extract(RESPONSE, data);

        assertThat(rows, is(2));
        assertTrue(data.getErrors().isEmpty()); // NOSONAR
        assertThat(data.getLatestPrices().size(), is(2));

        LatestSecurityPrice price = data.getLatestPrices().get(0);
        assertThat(price.getDate(), is(LocalDate.of(2026, 6, 18)));
        assertThat(price.getValue(), is(Values.Quote.factorize(161.1288)));
        assertThat(price.getHigh(), is(LatestSecurityPrice.NOT_AVAILABLE));
        assertThat(price.getLow(), is(LatestSecurityPrice.NOT_AVAILABLE));
        assertThat(price.getVolume(), is(LatestSecurityPrice.NOT_AVAILABLE));

        assertThat(data.getLatestPrices().get(1).getDate(), is(LocalDate.of(2026, 6, 17)));
    }

    @Test
    public void testExtractSkipsRowsWithoutValue()
    {
        var response = """
                        {
                          "data": [
                            { "date": "2026-06-18", "val": null },
                            { "date": "2026-06-17", "val": 161.1288 },
                            { "val": 161.1288 }
                          ]
                        }
                        """;

        var data = new QuoteFeedData();

        int rows = new FXMacroDataQuoteFeed().extract(response, data);

        assertThat(rows, is(3));
        assertThat(data.getLatestPrices().size(), is(1));
        assertThat(data.getLatestPrices().get(0).getDate(), is(LocalDate.of(2026, 6, 17)));
    }

    @Test
    public void testExtractReportsUnexpectedResponse()
    {
        var data = new QuoteFeedData();

        int rows = new FXMacroDataQuoteFeed().extract("{ \"detail\": \"Not Found\" }", data);

        assertThat(rows, is(0));
        assertThat(data.getErrors().size(), is(1));
    }

    @Test
    public void testExtractNextOffset()
    {
        var feed = new FXMacroDataQuoteFeed();

        assertThat(feed.extractNextOffset(FIRST_PAGE), is(100));
        assertThat(feed.extractNextOffset(SECOND_PAGE), is(-1));
        assertThat(feed.extractNextOffset("{ \"data\": [] }"), is(-1));
    }

    @Test
    public void testHistoricalQuotesFollowsPagination() throws IOException, QuoteFeedException, URISyntaxException
    {
        var feed = feed();
        doReturn(FIRST_PAGE, SECOND_PAGE).when(feed).request(any());

        var data = feed.previewHistoricalQuotes(security("USD/JPY"));

        assertTrue(data.getErrors().isEmpty()); // NOSONAR
        assertThat(data.getLatestPrices().size(), is(3));
        assertThat(data.getLatestPrices().get(2).getDate(), is(LocalDate.of(2026, 6, 16)));
        assertThat(data.getLatestPrices().get(2).getValue(), is(Values.Quote.factorize(160.95)));
        assertThat(data.getResponses().size(), is(2));

        var requests = ArgumentCaptor.forClass(WebAccess.class);
        verify(feed, times(2)).request(requests.capture());

        var first = requests.getAllValues().get(0).getURL();
        assertThat(first, containsString("https://api.fxmacrodata.com/v1/forex/USD/JPY?"));
        assertThat(first, containsString("limit=100"));
        assertThat(first, containsString("offset=0"));
        assertThat(first, containsString("start_date=" + LocalDate.now().minusMonths(2)));

        var second = requests.getAllValues().get(1).getURL();
        assertThat(second, containsString("offset=100"));
    }

    @Test
    public void testApiKeyIsSentAsHeaderOnly() throws IOException, QuoteFeedException, URISyntaxException
    {
        var feed = feed();

        var requests = new ArrayList<WebAccess>();
        doAnswer(invocation -> {
            var webaccess = spy(new WebAccess(FXMacroDataQuoteFeed.HOST, invocation.getArgument(0, String.class)));
            requests.add(webaccess);
            return webaccess;
        }).when(feed).createWebAccess(anyString());
        doReturn(RESPONSE).when(feed).request(any());

        feed.getHistoricalQuotes(security("USDJPY"), false);

        assertThat(requests.size(), is(1));
        var webaccess = requests.get(0);

        verify(webaccess).addHeader("X-API-Key", API_KEY);
        verify(webaccess, never()).addParameter(anyString(), eq(API_KEY));
        assertThat(webaccess.getURL(), not(containsString(API_KEY)));
        assertThat(webaccess.getURL(), containsString("https://"));
    }

    @Test
    public void testLatestQuote() throws IOException, QuoteFeedException, URISyntaxException
    {
        var feed = feed();
        doReturn(RESPONSE).when(feed).request(any());

        var price = feed.getLatestQuote(security("USD/JPY")).orElseThrow();

        assertThat(price.getDate(), is(LocalDate.of(2026, 6, 18)));
        assertThat(price.getValue(), is(Values.Quote.factorize(161.1288)));

        var requests = ArgumentCaptor.forClass(WebAccess.class);
        verify(feed, times(1)).request(requests.capture());
        assertThat(requests.getValue().getURL(), containsString("limit=1&"));
    }

    @Test
    public void testUnauthorizedReportsErrorMessage() throws IOException, QuoteFeedException
    {
        var feed = feed();
        doThrow(new WebAccessException("401 Unauthorized", 401, List.of(), UNAUTHORIZED)).when(feed).request(any());

        var data = feed.getHistoricalQuotes(security("USD/JPY"), false);

        assertTrue(data.getLatestPrices().isEmpty()); // NOSONAR
        assertThat(data.getErrors().size(), is(1));
        assertThat(data.getErrors().get(0).getMessage(), containsString("FXMacroData"));
        assertThat(data.getErrors().get(0).getMessage(), containsString("API key not recognised"));
        assertThat(data.getErrors().get(0).getMessage(), not(containsString(API_KEY)));
    }

    @Test
    public void testRateLimitExceeded() throws IOException
    {
        var feed = feed();
        doThrow(new WebAccessException("429 Too Many Requests", 429, List.of(), null)).when(feed).request(any());

        var security = security("USD/JPY");
        assertThrows(RateLimitExceededException.class, () -> feed.getHistoricalQuotes(security, false));
    }

    @Test
    public void testNoApiKey() throws IOException, QuoteFeedException
    {
        var feed = spy(new FXMacroDataQuoteFeed());

        var data = feed.getHistoricalQuotes(security("USD/JPY"), false);

        assertTrue(data.getLatestPrices().isEmpty()); // NOSONAR
        assertFalse(data.getErrors().isEmpty());
        verify(feed, never()).request(any());
    }

    @Test
    public void testInvalidTickerSymbol() throws IOException, QuoteFeedException
    {
        var feed = feed();

        var data = feed.getHistoricalQuotes(security("AAPL"), false);

        assertTrue(data.getLatestPrices().isEmpty()); // NOSONAR
        assertThat(data.getErrors().size(), is(1));
        assertThat(data.getErrors().get(0).getMessage(), containsString("AAPL"));
        verify(feed, never()).request(any());
    }

    @Test
    public void testNoTickerSymbol() throws IOException, QuoteFeedException
    {
        var feed = feed();

        var data = feed.getHistoricalQuotes(security(null), false);

        assertTrue(data.getLatestPrices().isEmpty()); // NOSONAR
        assertFalse(data.getErrors().isEmpty());
        verify(feed, never()).request(any());
    }
}
