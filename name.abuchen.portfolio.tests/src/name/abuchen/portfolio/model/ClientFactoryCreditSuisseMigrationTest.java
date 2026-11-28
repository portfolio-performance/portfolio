package name.abuchen.portfolio.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.Test;

import name.abuchen.portfolio.online.impl.SwissFundDataQuoteFeed;
import name.abuchen.portfolio.online.impl.YahooFinanceQuoteFeed;

@SuppressWarnings("nls")
public class ClientFactoryCreditSuisseMigrationTest
{
    private static final String CREDIT_SUISSE = "CREDITSUISSE_HTML_TABLE";

    private Security createSecurity(Client client, String isin, String feed, String feedURL)
    {
        var security = new Security("Fund " + isin, "CHF");
        security.setIsin(isin);
        security.setFeed(feed);
        security.setFeedURL(feedURL);
        client.addSecurity(security);
        return security;
    }

    @Test
    public void testMigrateCreditSuisseQuoteFeed()
    {
        var client = new Client();
        client.setVersion(70);

        var withIsin = createSecurity(client, "CH0117044971", CREDIT_SUISSE,
                        "https://am.credit-suisse.com/content/dam/fund-related-documents/nav-history/"
                                        + "NAVHistory_CH0117044971.csv");

        // URL format used by the first version of the Credit Suisse feed
        var isinFromUrl = createSecurity(client, null, CREDIT_SUISSE,
                        "https://amfunds.credit-suisse.com/ch/de/institutional/fund/history/CH0209106761?currency=USD");

        var latestOnly = createSecurity(client, "CH0110869143", YahooFinanceQuoteFeed.ID, null);
        latestOnly.setLatestFeed(CREDIT_SUISSE);
        latestOnly.setLatestFeedURL("https://am.credit-suisse.com/content/dam/fund-related-documents/nav-history/"
                        + "NAVHistory_CH0110869143.csv");

        var otherFeed = createSecurity(client, "CH0012032048", YahooFinanceQuoteFeed.ID, null);

        ClientFactory.upgradeModel(client);

        assertThat(client.getVersion(), is(Client.CURRENT_VERSION));

        assertThat(withIsin.getFeed(), is(SwissFundDataQuoteFeed.ID));
        assertThat(withIsin.getFeedURL(), nullValue());
        assertThat(withIsin.getIsin(), is("CH0117044971"));

        assertThat(isinFromUrl.getFeed(), is(SwissFundDataQuoteFeed.ID));
        assertThat(isinFromUrl.getFeedURL(), nullValue());
        assertThat(isinFromUrl.getIsin(), is("CH0209106761"));

        assertThat(latestOnly.getFeed(), is(YahooFinanceQuoteFeed.ID));
        assertThat(latestOnly.getLatestFeed(), is(SwissFundDataQuoteFeed.ID));
        assertThat(latestOnly.getLatestFeedURL(), nullValue());

        assertThat(otherFeed.getFeed(), is(YahooFinanceQuoteFeed.ID));
        assertThat(otherFeed.getIsin(), is("CH0012032048"));
    }
}
