package name.abuchen.portfolio.ui.jobs.priceupdate;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.Test;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.online.impl.EurostatHICPQuoteFeed;

@SuppressWarnings("nls")
public class HistoricalTaskTest
{
    private static class TestQuoteFeed implements QuoteFeed
    {
        private final String id;
        private final QuoteFeedData data;
        private final QuoteFeed.HistoricalUpdatePolicy policy;
        private final Optional<String> identity;

        TestQuoteFeed(String id, QuoteFeedData data, QuoteFeed.HistoricalUpdatePolicy policy, Optional<String> identity)
        {
            this.id = id;
            this.data = data;
            this.policy = policy;
            this.identity = identity;
        }

        @Override
        public String getId()
        {
            return id;
        }

        @Override
        public String getName()
        {
            return "Test Feed";
        }

        @Override
        public Optional<LatestSecurityPrice> getLatestQuote(Security security)
        {
            return Optional.empty();
        }

        @Override
        public QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse)
        {
            return data;
        }

        @Override
        public QuoteFeed.HistoricalUpdatePolicy getHistoricalUpdatePolicy(Security security)
        {
            return policy;
        }

        @Override
        public Optional<String> getHistoricalDataIdentity(Security security)
        {
            return identity;
        }
    }

    @Test
    public void testUpdateReplacesSeriesOnceWhenMigrationIsPending() throws Exception
    {
        var security = new Security("Germany (HVPI)", "EUR");
        security.setFeed(EurostatHICPQuoteFeed.ID);
        security.addPrice(new SecurityPrice(LocalDate.of(2025, 12, 1), 13280));
        security.addPrice(new SecurityPrice(LocalDate.of(2026, 1, 1), 13310));

        var data = new QuoteFeedData();
        data.addPrice(new LatestSecurityPrice(LocalDate.of(2025, 12, 1), 10067));
        data.addPrice(new LatestSecurityPrice(LocalDate.of(2026, 1, 1), 10079));

        var task = new Task.HistoricalTask("group",
                        new TestQuoteFeed(EurostatHICPQuoteFeed.ID, data,
                                        QuoteFeed.HistoricalUpdatePolicy.REPLACE_IF_SOURCE_CHANGED,
                                        Optional.of(EurostatHICPQuoteFeed.DATASET_VERSION)),
                        new FeedUpdateStatus(UpdateStatus.WAITING), security);

        var result = task.update();

        assertThat(result, is(UpdateStatus.MODIFIED));
        assertThat(security.getPrices().size(), is(2));
        assertThat(security.getPrices().get(0).getValue(), is(10067L));
        assertThat(security.getPrices().get(1).getValue(), is(10079L));
        assertThat(security.getPropertyValue(SecurityProperty.Type.FEED, QuoteFeed.HISTORICAL_DATA_IDENTITY)
                        .orElseThrow(), is(EurostatHICPQuoteFeed.DATASET_VERSION));
    }

    @Test
    public void testUpdateKeepsExistingPricesWhenMigrationDownloadFails() throws Exception
    {
        var security = new Security("Germany (HVPI)", "EUR");
        security.setFeed(EurostatHICPQuoteFeed.ID);
        security.addPrice(new SecurityPrice(LocalDate.of(2025, 12, 1), 13280));

        var data = QuoteFeedData.withError(new IllegalArgumentException("boom"));

        var task = new Task.HistoricalTask("group",
                        new TestQuoteFeed(EurostatHICPQuoteFeed.ID, data,
                                        QuoteFeed.HistoricalUpdatePolicy.REPLACE_IF_SOURCE_CHANGED,
                                        Optional.of(EurostatHICPQuoteFeed.DATASET_VERSION)),
                        new FeedUpdateStatus(UpdateStatus.WAITING), security);

        var result = task.update();

        assertThat(result, is(UpdateStatus.UNMODIFIED));
        assertThat(security.getPrices().size(), is(1));
        assertThat(security.getPrices().get(0).getValue(), is(13280L));
        assertTrue(security.getPropertyValue(SecurityProperty.Type.FEED, QuoteFeed.HISTORICAL_DATA_IDENTITY).isEmpty()); // NOSONAR
    }

    @Test
    public void testUpdateUsesNormalMergeAfterMigrationMarkerIsSet() throws Exception
    {
        var security = new Security("Germany (HVPI)", "EUR");
        security.setFeed(EurostatHICPQuoteFeed.ID);
        security.setPropertyValue(SecurityProperty.Type.FEED, QuoteFeed.HISTORICAL_DATA_IDENTITY,
                        EurostatHICPQuoteFeed.DATASET_VERSION);
        security.addPrice(new SecurityPrice(LocalDate.of(2025, 12, 1), 10067));
        security.addPrice(new SecurityPrice(LocalDate.of(2026, 1, 1), 10079));

        var data = new QuoteFeedData();
        data.addPrice(new LatestSecurityPrice(LocalDate.of(2025, 12, 1), 99999));
        data.addPrice(new LatestSecurityPrice(LocalDate.of(2026, 1, 1), 10101));
        data.addPrice(new LatestSecurityPrice(LocalDate.of(2026, 2, 1), 10123));

        var task = new Task.HistoricalTask("group",
                        new TestQuoteFeed(EurostatHICPQuoteFeed.ID, data,
                                        QuoteFeed.HistoricalUpdatePolicy.REPLACE_IF_SOURCE_CHANGED,
                                        Optional.of(EurostatHICPQuoteFeed.DATASET_VERSION)),
                        new FeedUpdateStatus(UpdateStatus.WAITING), security);

        var result = task.update();

        assertThat(result, is(UpdateStatus.MODIFIED));
        assertThat(security.getPrices().size(), is(3));
        assertThat(security.getPrices().get(0).getValue(), is(10067L));
        assertThat(security.getPrices().get(1).getValue(), is(10101L));
        assertThat(security.getPrices().get(2).getValue(), is(10123L));
    }

    @Test
    public void testUpdateReplacesSeriesWhenPolicyIsReplace() throws Exception
    {
        var security = new Security("Test", "EUR");
        security.setFeed("TEST");
        security.addPrice(new SecurityPrice(LocalDate.of(2025, 12, 1), 13280));
        security.addPrice(new SecurityPrice(LocalDate.of(2026, 1, 1), 13310));

        var data = new QuoteFeedData();
        data.addPrice(new LatestSecurityPrice(LocalDate.of(2026, 1, 1), 10079));
        data.addPrice(new LatestSecurityPrice(LocalDate.of(2026, 2, 1), 10123));

        var task = new Task.HistoricalTask("group",
                        new TestQuoteFeed("TEST", data, QuoteFeed.HistoricalUpdatePolicy.REPLACE, Optional.empty()),
                        new FeedUpdateStatus(UpdateStatus.WAITING), security);

        var result = task.update();

        assertThat(result, is(UpdateStatus.MODIFIED));
        assertThat(security.getPrices().size(), is(2));
        assertThat(security.getPrices().get(0).getDate(), is(LocalDate.of(2026, 1, 1)));
        assertThat(security.getPrices().get(0).getValue(), is(10079L));
        assertThat(security.getPrices().get(1).getDate(), is(LocalDate.of(2026, 2, 1)));
        assertThat(security.getPrices().get(1).getValue(), is(10123L));
    }

    @Test
    public void testUpdateClearsStoredIdentityWhenPolicyIsReplace() throws Exception
    {
        var security = new Security("Test", "EUR");
        security.setFeed("TEST");
        security.setPropertyValue(SecurityProperty.Type.FEED, QuoteFeed.HISTORICAL_DATA_IDENTITY, "old-identity");
        security.addPrice(new SecurityPrice(LocalDate.of(2025, 12, 1), 13280));

        var data = new QuoteFeedData();
        data.addPrice(new LatestSecurityPrice(LocalDate.of(2026, 1, 1), 10079));

        var task = new Task.HistoricalTask("group",
                        new TestQuoteFeed("TEST", data, QuoteFeed.HistoricalUpdatePolicy.REPLACE, Optional.empty()),
                        new FeedUpdateStatus(UpdateStatus.WAITING), security);

        var result = task.update();

        assertThat(result, is(UpdateStatus.MODIFIED));
        assertTrue(security.getPropertyValue(SecurityProperty.Type.FEED, QuoteFeed.HISTORICAL_DATA_IDENTITY).isEmpty()); // NOSONAR
        assertThat(security.getPrices().size(), is(1));
        assertThat(security.getPrices().get(0).getValue(), is(10079L));
    }
}
