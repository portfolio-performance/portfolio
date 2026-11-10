package name.abuchen.portfolio.online.impl;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.util.WebAccess;

@SuppressWarnings("nls")
public class MOEXQuoteFeedTest
{
    private String read(String filename) throws IOException
    {
        try (InputStream in = getClass().getResourceAsStream(filename))
        {
            try (Scanner scanner = new Scanner(in, StandardCharsets.UTF_8))
            {
                return scanner.useDelimiter("\\A").next();
            }
        }
    }

    @Test
    public void testParsingShareHistory() throws IOException
    {
        var feed = new MOEXQuoteFeed();
        var data = new QuoteFeedData();

        var rows = feed.parseHistory(read("sber_history.json"), data);

        assertThat(rows, is(4));
        assertThat(data.getErrors().isEmpty(), is(true));

        LatestSecurityPrice first = data.getLatestPrices().get(0);
        assertThat(first.getDate(), is(LocalDate.of(2023, 1, 3)));
        assertThat(first.getValue(), is(Values.Quote.factorize(141.78)));
        assertThat(first.getHigh(), is(Values.Quote.factorize(143.25)));
        assertThat(first.getLow(), is(Values.Quote.factorize(141.56)));
        assertThat(first.getVolume(), is(21098550L));

        LatestSecurityPrice last = data.getLatestPrices().get(3);
        assertThat(last.getDate(), is(LocalDate.of(2023, 1, 6)));
        assertThat(last.getValue(), is(Values.Quote.factorize(141.4)));
    }

    @Test
    public void testParsingIndexHistory() throws IOException
    {
        MOEXQuoteFeed feed = new MOEXQuoteFeed();
        QuoteFeedData data = new QuoteFeedData();

        int rows = feed.parseHistory(read("imoex_history.json"), data);

        assertThat(rows, is(5));
        assertThat(data.getErrors().isEmpty(), is(true));

        LatestSecurityPrice first = data.getLatestPrices().get(0);
        assertThat(first.getDate(), is(LocalDate.of(2026, 8, 3)));
        assertThat(first.getValue(), is(Values.Quote.factorize(2262.75)));
        assertThat(first.getHigh(), is(Values.Quote.factorize(2263.52)));
        assertThat(first.getLow(), is(Values.Quote.factorize(2240.54)));
    }

    @Test
    public void testTotalFromCursor() throws IOException
    {
        MOEXQuoteFeed feed = new MOEXQuoteFeed();
        assertThat(feed.getTotal(read("sber_history.json")), is(4L));
        assertThat(feed.getTotal("{}"), is(-1L));
    }

    @Test
    public void testGetLatestQuote() throws IOException
    {
        String json = read("sber_history.json");

        MOEXQuoteFeed feed = Mockito.spy(new MOEXQuoteFeed());
        Mockito.doReturn(json).when(feed).getJson(any(WebAccess.class));

        Security security = new Security();
        security.setTickerSymbol("SBER");
        security.setPropertyValue(SecurityProperty.Type.FEED, MOEXQuoteFeed.MOEX_MARKET, "shares");

        Optional<LatestSecurityPrice> quote = feed.getLatestQuote(security);

        assertThat(quote.isPresent(), is(true));
        assertThat(quote.get().getDate(), is(LocalDate.of(2023, 1, 6)));
        assertThat(quote.get().getValue(), is(Values.Quote.factorize(141.4)));
    }

    @Test
    public void testGetLatestQuoteFromMarketData() throws IOException
    {
        String json = read("sber_marketdata.json");

        MOEXQuoteFeed feed = Mockito.spy(new MOEXQuoteFeed());
        Mockito.doReturn(json).when(feed).getJson(any(WebAccess.class));

        Security security = new Security();
        security.setTickerSymbol("SBER");
        security.setPropertyValue(SecurityProperty.Type.FEED, MOEXQuoteFeed.MOEX_MARKET, "shares");

        Optional<LatestSecurityPrice> quote = feed.getLatestQuote(security);

        assertThat(quote.isPresent(), is(true));
        assertThat(quote.get().getDate(), is(LocalDate.of(2026, 8, 8)));
        assertThat(quote.get().getValue(), is(Values.Quote.factorize(282.77)));
    }

    @Test
    public void testParseMarketData() throws IOException
    {
        MOEXQuoteFeed feed = new MOEXQuoteFeed();

        Optional<LatestSecurityPrice> quote = feed.parseMarketData(read("sber_marketdata.json"));

        assertThat(quote.isPresent(), is(true));
        assertThat(quote.get().getDate(), is(LocalDate.of(2026, 8, 8)));
        assertThat(quote.get().getValue(), is(Values.Quote.factorize(282.77)));
    }

    @Test
    public void testParseMarketDataEmpty()
    {
        MOEXQuoteFeed feed = new MOEXQuoteFeed();

        assertThat(feed.parseMarketData("{}").isPresent(), is(false));
        assertThat(feed.parseMarketData("{\"marketdata\":{\"columns\":[\"LAST\"],\"data\":[]}}").isPresent(),
                        is(false));
    }

    @Test
    public void testParsingCurrencyHistory() throws IOException
    {
        var feed = new MOEXQuoteFeed();
        var data = new QuoteFeedData();

        var rows = feed.parseHistory(read("gldrub_history.json"), data);

        assertThat(rows, is(3));
        assertThat(data.getErrors().isEmpty(), is(true));

        var first = data.getLatestPrices().get(0);
        assertThat(first.getDate(), is(LocalDate.of(2026, 7, 1)));
        assertThat(first.getValue(), is(Values.Quote.factorize(10115)));
        assertThat(first.getHigh(), is(Values.Quote.factorize(10200)));
        assertThat(first.getLow(), is(Values.Quote.factorize(9850)));
    }

    @Test
    public void testGetLatestQuoteForCurrency() throws IOException
    {
        var json = read("gldrub_history.json");

        var feed = Mockito.spy(new MOEXQuoteFeed());
        Mockito.doReturn(json).when(feed).getJson(any(WebAccess.class));

        var security = new Security();
        security.setTickerSymbol("GLDRUB_TOM");
        security.setPropertyValue(SecurityProperty.Type.FEED, MOEXQuoteFeed.MOEX_ENGINE, "currency");
        security.setPropertyValue(SecurityProperty.Type.FEED, MOEXQuoteFeed.MOEX_MARKET, "selt");
        security.setPropertyValue(SecurityProperty.Type.FEED, MOEXQuoteFeed.MOEX_BOARD, "CETS");

        // marketdata returns nothing for the fixture, so the feed falls back to
        // the most recent completed session
        var quote = feed.getLatestQuote(security);

        assertThat(quote.isPresent(), is(true));
        assertThat(quote.get().getDate(), is(LocalDate.of(2026, 7, 3)));
        assertThat(quote.get().getValue(), is(Values.Quote.factorize(10352.5)));
    }

    @Test
    public void testCurrencyHistoryUsesEngineAndBoard() throws IOException, URISyntaxException
    {
        var page = read("gldrub_history.json");

        var feed = Mockito.spy(new MOEXQuoteFeed());
        Mockito.doReturn(page).when(feed).getJson(any(WebAccess.class));

        var security = new Security();
        security.setTickerSymbol("GLDRUB_TOM");
        security.setPropertyValue(SecurityProperty.Type.FEED, MOEXQuoteFeed.MOEX_ENGINE, "currency");
        security.setPropertyValue(SecurityProperty.Type.FEED, MOEXQuoteFeed.MOEX_MARKET, "selt");
        security.setPropertyValue(SecurityProperty.Type.FEED, MOEXQuoteFeed.MOEX_BOARD, "CETS");

        var data = feed.getHistoricalQuotes(security, false);
        assertThat(data.getLatestPrices().size(), is(3));

        var captor = ArgumentCaptor.forClass(WebAccess.class);
        Mockito.verify(feed, Mockito.times(1)).getJson(captor.capture());

        var url = captor.getValue().getURL();
        assertThat(url, containsString("/engines/currency/markets/selt/boards/CETS/securities/GLDRUB_TOM.json"));
    }

    @Test
    public void testMissingTickerSymbol() throws IOException
    {
        MOEXQuoteFeed feed = new MOEXQuoteFeed();

        Security security = new Security();
        security.setName("SBER");

        QuoteFeedData data = feed.getHistoricalQuotes(security, false);

        assertThat(data.getErrors().isEmpty(), is(false));
    }

    @Test
    public void testParseHistoryReturnsSourceRowCount() throws IOException
    {
        var feed = new MOEXQuoteFeed();
        var data = new QuoteFeedData();

        // the fixture is a full page of 100 rows with one invalid row (no
        // trade); the source row count is returned while only 99 prices are
        // accepted
        var rows = feed.parseHistory(read("sber_history_page1_invalid.json"), data);

        assertThat(rows, is(100));
        assertThat(data.getLatestPrices().size(), is(99));
    }

    @Test
    public void testPaginationContinuesPastFullPageWithInvalidRow() throws IOException, URISyntaxException
    {
        var page1 = read("sber_history_page1_invalid.json");
        var page2 = read("sber_history_page2.json");

        var feed = Mockito.spy(new MOEXQuoteFeed());
        Mockito.doReturn(page1, page2).when(feed).getJson(any(WebAccess.class));

        var security = new Security();
        security.setTickerSymbol("SBER");
        security.setPropertyValue(SecurityProperty.Type.FEED, MOEXQuoteFeed.MOEX_MARKET, "shares");

        QuoteFeedData data = feed.getHistoricalQuotes(security, false);

        // 99 accepted prices from the full page plus 2 from the second page
        assertThat(data.getLatestPrices().size(), is(101));
        assertThat(data.getErrors().isEmpty(), is(true));

        // the second page request must continue at the source row offset
        ArgumentCaptor<WebAccess> captor = ArgumentCaptor.forClass(WebAccess.class);
        Mockito.verify(feed, Mockito.times(2)).getJson(captor.capture());

        List<WebAccess> requests = captor.getAllValues();
        assertThat(requests.size(), is(2));
        assertThat(requests.get(1).getURL(), containsString("start=100"));
    }
}
