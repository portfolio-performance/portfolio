package name.abuchen.portfolio.online;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.spi.ServiceRegistry;

public class Factory
{
    private static final List<QuoteFeed> FEEDS;

    public static final List<QuoteFeed> getQuoteFeedProvider()
    {
        return FEEDS;
    }

    public static QuoteFeed getQuoteFeedProvider(String feedId)
    {
        for (QuoteFeed feed : FEEDS)
        {
            if (feedId.equals(feed.getId()))
                return feed;
        }
        return null;
    }

    static
    {
        FEEDS = new ArrayList<QuoteFeed>();

        Iterator<QuoteFeed> feeds = ServiceRegistry.lookupProviders(QuoteFeed.class);
        while (feeds.hasNext())
            FEEDS.add(feeds.next());
    }
}
