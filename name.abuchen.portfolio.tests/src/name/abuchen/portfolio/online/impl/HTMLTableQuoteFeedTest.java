package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.QuoteFeedData;

public class HTMLTableQuoteFeedTest
{

    @Test public void testHistorical()
    {
        HTMLTableQuoteFeed feed = new HTMLTableQuoteFeed();

        String html = read("HTMLTableQuoteFeedHistoricalSample.html");

        QuoteFeedData data = feed.getHistoricalQuotes(html);

        List<LatestSecurityPrice> prices = data.getLatestPrices();

        assertPrice(prices.get(0), "2020-12-03", 6750000, 6750000, 6658900);
        assertPrice(prices.get(1), "2020-12-04", 6753700, 6753700, 6674600);
        assertPrice(prices.get(2), "2020-12-07", 6771300, 6803100, 6771300);
    }

    @Test
    public void testGetLatestQuote()
    {
        HTMLTableQuoteFeed feed = new HTMLTableQuoteFeed() {
            @Override String getHtml(String url) throws IOException, URISyntaxException
            {
                return read("HTMLTableQuoteFeedNonHistoricalSample.html");
            }

            @Override void logErrors(QuoteFeedData data)
            {
                // do nothing
            }
        };
        Security security = new Security("foo", "EUR");
        security.setLatestFeed("Tabelle");
        security.setLatestFeedURL("http://www.example.com/foo");

        Optional<LatestSecurityPrice> quote = feed.getLatestQuote(security);

        assertThat(quote.isPresent(), is(true));

        System.out.println("quote = " + quote);
        LatestSecurityPrice price = quote.get();
        assertThat(price.getValue(), is(6714000L));

    }

    private void assertPrice(LatestSecurityPrice price, String date, long value, long high, long low)
    {
        assertThat(price.getDate(), is(LocalDate.parse(date)));
        assertThat(price.getValue(), is(value));
        assertThat(price.getHigh(), is(high));
        assertThat(price.getLow(), is(low));
    }

    private String read(String resourceName)
    {
        try (Scanner scanner = new Scanner(getClass().getResourceAsStream(resourceName), "UTF-8"))
        {
            return scanner.useDelimiter("\\A").next();
        }
    }

}
