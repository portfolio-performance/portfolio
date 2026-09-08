package name.abuchen.portfolio.online.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.Test;

import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;

/**
 * Malformed responses of a search provider must not throw a
 * {@link NullPointerException} because that aborts the search of all other
 * providers as well.
 *
 * @see <a href=
 *      "https://github.com/portfolio-performance/portfolio/issues/5919">Issue
 *      #5919</a>
 */
@SuppressWarnings("nls")
public class SecuritySearchProviderTest
{
    @Test
    public void testYahooSearchIgnoresMalformedDocuments()
    {
        var json = """
                        {"finance":{"result":[{"documents":[
                          null,
                          "not an object",
                          {"noSymbol":"value"},
                          {"symbol":"META","shortName":"Meta Platforms, Inc.","quoteType":"EQUITY","exchange":"NMS"}
                        ]}]}}
                        """;

        List<ResultItem> answer = new ArrayList<>();
        new YahooSearchProvider().extractFrom(answer, json);

        assertThat(answer.size(), is(1));
        assertThat(answer.get(0).getSymbol(), is("META"));
        assertThat(answer.get(0).getName(), is("Meta Platforms, Inc."));
    }

    @Test
    public void testYahooSearchIgnoresUnexpectedJsonStructure()
    {
        List<ResultItem> answer = new ArrayList<>();
        var provider = new YahooSearchProvider();

        // the response is an array instead of an object
        provider.extractFrom(answer, "[\"service unavailable\"]");

        // the nested attributes have an unexpected type
        provider.extractFrom(answer, "{\"finance\":\"error\"}");
        provider.extractFrom(answer, "{\"finance\":{\"result\":\"error\"}}");
        provider.extractFrom(answer, "{\"finance\":{\"result\":[\"not an object\"]}}");
        provider.extractFrom(answer, "{\"finance\":{\"result\":[{\"documents\":\"error\"}]}}");

        assertTrue(answer.isEmpty());
    }

    @Test
    public void testYahooSymbolSearchIgnoresNullQuote()
    {
        assertTrue(YahooSymbolSearch.Result.from(null).isEmpty());
    }

    @Test
    public void testPortfolioPerformanceSearchIgnoresMalformedMarkets()
    {
        var json = """
                        {"provider":"PP","description":"Meta Platforms","isin":"US30303M1027","markets":[
                          null,
                          "not an object",
                          {"symbol":"META","currency":"USD","exchange":"XNAS"},
                          {"symbol":"FB2A","currency":"EUR","exchange":"XETR"}
                        ]}
                        """;

        var item = PortfolioPerformanceSearchProvider.Result.from((JSONObject) JSONValue.parse(json));

        assertTrue(item.isPresent());
        assertThat(item.get().getMarkets().size(), is(2));
    }

    @Test
    public void testPortfolioPerformanceSearchIgnoresUnexpectedJsonStructure()
    {
        var json = """
                        {"provider":"PP","description":"Meta Platforms","markets":"error"}
                        """;

        var item = PortfolioPerformanceSearchProvider.Result.from((JSONObject) JSONValue.parse(json));

        assertTrue(item.isPresent());
        assertTrue(item.get().getMarkets().isEmpty());
    }

    @Test
    public void testPortfolioPerformanceSearchIgnoresNullItem()
    {
        assertTrue(PortfolioPerformanceSearchProvider.Result.from(null).isEmpty());
    }

    @Test
    public void testTwelveDataSearchIgnoresMalformedResponse()
    {
        List<ResultItem> answer = new ArrayList<>();
        var provider = new TwelveDataSearchProvider();

        // response is not valid JSON at all
        provider.extract(answer, "<html>service unavailable</html>");
        assertTrue(answer.isEmpty());

        // response is an array instead of an object
        provider.extract(answer, "[\"service unavailable\"]");
        assertTrue(answer.isEmpty());

        // the data attribute has an unexpected type
        provider.extract(answer, "{\"data\":\"error\"}");
        assertTrue(answer.isEmpty());

        provider.extract(answer, """
                        {"data":[
                          null,
                          "not an object",
                          {"instrument_name":"Meta Platforms Inc"},
                          {"symbol":"META","instrument_name":"Meta Platforms Inc","instrument_type":"Common Stock","currency":"USD"},
                          {"symbol":"META","instrument_name":"Meta Platforms Inc","mic_code":"XNAS","currency":"USD"}
                        ],"status":"ok"}
                        """);

        assertThat(answer.size(), is(2));

        // without a market identifier code, no suffix is added to the symbol
        assertThat(answer.get(0).getSymbol(), is("META"));
        assertThat(answer.get(1).getSymbol(), is("META.XNAS"));
    }

    @Test
    public void testLeewaySearchIgnoresMalformedResponse()
    {
        var provider = new LeewaySearchProvider();

        // response is not valid JSON at all
        assertTrue(provider.extract("<html>service unavailable</html>").isEmpty());

        // response is an object instead of an array
        assertTrue(provider.extract("{\"error\":\"service unavailable\"}").isEmpty());

        var result = provider.extract("""
                        [
                          null,
                          "not an object",
                          {"Name":"Meta Platforms Inc"},
                          {"Code":"META","Name":"Meta Platforms Inc","Type":"Common Stock","ISIN":"US30303M1027","currencyCode":"USD"},
                          {"Code":"FB2A","Exchange":"XETRA","Name":"Meta Platforms Inc","ISIN":"US30303M1027","currencyCode":"EUR"}
                        ]
                        """);

        assertThat(result.size(), is(2));

        // without an exchange, no suffix is added to the symbol
        assertThat(result.get(0).getSymbol(), is("META"));
        assertThat(result.get(1).getSymbol(), is("FB2A.XETRA"));
    }

    @Test
    public void testFinnhubSearchIgnoresNullResult()
    {
        assertTrue(FinnhubSearchProvider.Result.from(new JSONObject()).isEmpty());
    }

    @Test
    public void testFinnhubSearchDoesNotCreateNullLiterals()
    {
        var json = (JSONObject) JSONValue.parse("""
                        {"symbol":"META"}
                        """);

        var item = FinnhubSearchProvider.Result.from(json).orElseThrow();

        assertThat(item.getSymbol(), is("META"));
        assertNull(item.getName());
        assertNull(item.getType());
    }

    @Test
    public void testEODHistoricalDataSearchIgnoresNullResult()
    {
        assertTrue(EODHistoricalDataSearchProvider.Result.from(new JSONObject()).isEmpty());
    }

    @Test
    public void testEODHistoricalDataSearchDoesNotCreateNullLiterals()
    {
        var json = (JSONObject) JSONValue.parse("""
                        {"Code":"META"}
                        """);

        var item = EODHistoricalDataSearchProvider.Result.from(json).orElseThrow();

        // without an exchange, no suffix is added to the symbol
        assertThat(item.getSymbol(), is("META"));
        assertNull(item.getName());
        assertNull(item.getType());
        assertNull(item.getExchange());
        assertNull(item.getCountry());
        assertNull(item.getCurrency());
        assertNull(item.getPreviousClose());
        assertNull(item.getPreviousCloseDate());
    }

    @Test
    public void testCoinGeckoSearchIgnoresIncompleteCoins()
    {
        var json = """
                        [
                          null,
                          "not an object",
                          {"symbol":"meta","name":"Metadium"},
                          {"id":"metadium","name":"Metadium"},
                          {"id":"metadium","symbol":"meta","name":"Metadium"}
                        ]
                        """;

        var coins = CoinGeckoQuoteFeed.extractCoins(json);

        assertThat(coins.size(), is(1));
        assertThat(coins.get(0).getId(), is("metadium"));

        // response is not valid JSON at all
        assertTrue(CoinGeckoQuoteFeed.extractCoins("<html>service unavailable</html>").isEmpty());

        // response is the error object returned when the rate limit is exceeded
        assertTrue(CoinGeckoQuoteFeed.extractCoins("{\"status\":{\"error_code\":429}}").isEmpty());
    }

    @Test
    public void testCoinGeckoDoesNotCacheRejectedCoinList() throws IOException
    {
        // the error object returned if the rate limit is exceeded
        var responses = new ArrayDeque<>(List.of("{\"status\":{\"error_code\":429}}",
                        "[{\"id\":\"metadium\",\"symbol\":\"meta\",\"name\":\"Metadium\"}]"));

        var feed = new CoinGeckoQuoteFeed()
        {
            @Override
            String requestCoinList() throws IOException
            {
                return responses.poll();
            }
        };

        assertTrue(feed.getCoins().isEmpty());

        // the rejected response must not be cached
        assertThat(feed.getCoins().size(), is(1));
    }

    @Test
    public void testCoinGeckoSearchIgnoresCoinsWithoutName()
    {
        var coins = List.of(new CoinGeckoQuoteFeed.Coin("metadium", "meta", null),
                        new CoinGeckoQuoteFeed.Coin("meta-masters", "mms", "Meta Masters"));

        var items = CoinGeckoSearchProvider.filter(coins, "Meta");

        assertThat(items.size(), is(1));
        assertThat(items.get(0).getName(), is("Meta Masters"));
    }
}
