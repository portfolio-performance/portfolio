package name.abuchen.portfolio.ui.jobs.priceupdate;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.Test;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.online.QuoteFeedException;

@SuppressWarnings("nls")
public class LatestTaskTest
{
    private static class TestQuoteFeed implements QuoteFeed
    {
        private final Optional<LatestSecurityPrice> latestQuote;
        private Security receivedSecurity;

        TestQuoteFeed(Optional<LatestSecurityPrice> latestQuote)
        {
            this.latestQuote = latestQuote;
        }

        @Override
        public Optional<LatestSecurityPrice> getLatestQuote(Security security) throws QuoteFeedException
        {
            this.receivedSecurity = security;
            return latestQuote;
        }

        @Override
        public String getId()
        {
            return "TEST";
        }

        @Override
        public String getName()
        {
            return "Test Feed";
        }

        @Override
        public QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse)
        {
            return new QuoteFeedData();
        }

        Security getReceivedSecurity()
        {
            return receivedSecurity;
        }
    }

    @Test
    public void testUpdateUsesMainTickerWhenNoOverrideIsSet() throws Exception
    {
        var security = new Security("Test", "EUR");
        security.setTickerSymbol("MAIN.DE");

        var latestPrice = new LatestSecurityPrice(LocalDate.now(), 10000);
        var feed = new TestQuoteFeed(Optional.of(latestPrice));

        var status = new FeedUpdateStatus(UpdateStatus.WAITING);
        var task = new Task.LatestTask("group", feed, status, security);

        var result = task.update();

        assertThat(result, is(UpdateStatus.MODIFIED));
        assertThat(feed.getReceivedSecurity().getTickerSymbol(), is("MAIN.DE"));
        assertThat(security.getLatest(), is(latestPrice));
    }

    @Test
    public void testUpdateUsesOverrideTickerWhenPropertyIsSet() throws Exception
    {
        var security = new Security("Test", "EUR");
        security.setTickerSymbol("MAIN.DE");
        security.addProperty(
                        new SecurityProperty(SecurityProperty.Type.FEED, QuoteFeed.TICKER_SYMBOL_LATEST, "ALT.L"));

        var latestPrice = new LatestSecurityPrice(LocalDate.now(), 20000);
        var feed = new TestQuoteFeed(Optional.of(latestPrice));

        var status = new FeedUpdateStatus(UpdateStatus.WAITING);
        var task = new Task.LatestTask("group", feed, status, security);

        var result = task.update();

        assertThat(result, is(UpdateStatus.MODIFIED));
        assertThat(feed.getReceivedSecurity().getTickerSymbol(), is("ALT.L"));
        assertThat(security.getLatest(), is(latestPrice));
        assertThat(security.getTickerSymbol(), is("MAIN.DE"));
    }

    @Test
    public void testUpdateReturnsUnmodifiedWhenFeedReturnsEmpty() throws Exception
    {
        var security = new Security("Test", "EUR");
        security.setTickerSymbol("MAIN.DE");

        var feed = new TestQuoteFeed(Optional.empty());

        var status = new FeedUpdateStatus(UpdateStatus.WAITING);
        var task = new Task.LatestTask("group", feed, status, security);

        var result = task.update();

        assertThat(result, is(UpdateStatus.UNMODIFIED));
        assertThat(security.getLatest(), is(nullValue()));
    }

    @Test
    public void testUpdateReturnsUnmodifiedWhenSamePriceIsSetAgain() throws Exception
    {
        var latestPrice = new LatestSecurityPrice(LocalDate.now(), 10000);

        var security = new Security("Test", "EUR");
        security.setTickerSymbol("MAIN.DE");
        security.setLatest(latestPrice);

        var feed = new TestQuoteFeed(Optional.of(latestPrice));

        var status = new FeedUpdateStatus(UpdateStatus.WAITING);
        var task = new Task.LatestTask("group", feed, status, security);

        var result = task.update();

        assertThat(result, is(UpdateStatus.UNMODIFIED));
    }
}
