package name.abuchen.portfolio.online.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import name.abuchen.portfolio.junit.TestUtilities;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeedData;

@SuppressWarnings("nls")
public class HTMLTableQuoteFeedTest
{

    @Test
    public void testHistorical()
    {
        HTMLTableQuoteFeed feed = new HTMLTableQuoteFeed();

        String html = TestUtilities.read(getClass(), "HTMLTableQuoteFeedHistoricalSample.html");

        QuoteFeedData data = feed.getHistoricalQuotes(html);

        List<LatestSecurityPrice> prices = data.getLatestPrices();

        assertThat(prices.size(), is(3));

        Collections.sort(prices, new SecurityPrice.ByDate());

        assertPrice(prices.get(0), "2020-12-03", 675, 675, 665.89);
        assertPrice(prices.get(1), "2020-12-04", 675.37, 675.37, 667.46);
        assertPrice(prices.get(2), "2020-12-07", 677.13, 680.31, 677.13);
    }

    @Test
    public void testGetLatestQuote()
    {
        HTMLTableQuoteFeed feed = new HTMLTableQuoteFeed()
        {
            @Override
            String getHtml(String url) throws IOException, URISyntaxException
            {
                return TestUtilities.read(getClass(), "HTMLTableQuoteFeedNonHistoricalSample.html");
            }
        };
        Security security = new Security("foo", "EUR");
        security.setLatestFeed("Tabelle");
        security.setLatestFeedURL("http://www.example.com/foo");

        Optional<LatestSecurityPrice> quote = feed.getLatestQuote(security);

        assertThat(quote.isPresent(), is(true));

        LatestSecurityPrice price = quote.orElseThrow(IllegalArgumentException::new);
        assertThat(price.getValue(), is(Values.Quote.factorize(671.4)));

    }

    private void assertPrice(LatestSecurityPrice price, String date, double value, double high, double low)
    {
        assertThat(price.getDate(), is(LocalDate.parse(date)));
        assertThat(price.getValue(), is(Values.Quote.factorize(value)));
        assertThat(price.getHigh(), is(Values.Quote.factorize(high)));
        assertThat(price.getLow(), is(Values.Quote.factorize(low)));
    }
}
