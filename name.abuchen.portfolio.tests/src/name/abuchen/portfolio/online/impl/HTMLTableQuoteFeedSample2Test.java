package name.abuchen.portfolio.online.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.junit.TestUtilities;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeedData;

@SuppressWarnings("nls")
public class HTMLTableQuoteFeedSample2Test
{
    @Test
    public void testHistorical()
    {
        HTMLTableQuoteFeed feed = new HTMLTableQuoteFeed();

        String html = TestUtilities.read(getClass(), "HTMLTableQuoteFeedHistoricalSample2.html");

        QuoteFeedData data = feed.getHistoricalQuotes(html);

        List<LatestSecurityPrice> prices = data.getLatestPrices();

        assertThat(prices.size(), is(2));

        Collections.sort(prices, new SecurityPrice.ByDate());

        assertThat(prices.get(1).getDate(), is(LocalDate.parse("2022-08-24")));
        assertThat(prices.get(1).getValue(), is(Values.Quote.factorize(1.55)));
        assertThat(prices.get(1).getHigh(), is(Values.Quote.factorize(1.78)));
        assertThat(prices.get(1).getLow(), is(Values.Quote.factorize(1.54)));
    }
}
