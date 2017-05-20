package name.abuchen.portfolio.online;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.spi.ServiceRegistry;

public class Factory
{
    private static final List<QuoteFeed> FEEDS;
    private static final List<SecuritySearchProvider> SEARCH;

    private Factory()
    {}

    public static final List<QuoteFeed> getQuoteFeedProvider()
    {
        return FEEDS;
    }

    public static QuoteFeed getQuoteFeedProvider(String feedId)
    {
        for (QuoteFeed feed : FEEDS)
        {
            if (feed.getId().equals(feedId))
                return feed;
        }
        return null;
    }

    public static final List<SecuritySearchProvider> getSearchProvider()
    {
        return SEARCH;
    }

    static
    {
        FEEDS = new ArrayList<>();
        Iterator<QuoteFeed> feeds = ServiceRegistry.lookupProviders(QuoteFeed.class);
        while (feeds.hasNext())
            FEEDS.add(feeds.next());

        SEARCH = new ArrayList<>();
        Iterator<SecuritySearchProvider> search = ServiceRegistry.lookupProviders(SecuritySearchProvider.class);
        while (search.hasNext())
            SEARCH.add(search.next());

    }
}
