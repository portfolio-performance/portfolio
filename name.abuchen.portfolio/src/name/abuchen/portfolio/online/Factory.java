package name.abuchen.portfolio.online;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

public class Factory
{
    private static final List<QuoteFeed> FEEDS;
    private static final List<SecuritySearchProvider> SEARCH;

    private Factory()
    {
    }

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
        Iterator<QuoteFeed> feeds = ServiceLoader.load(QuoteFeed.class).iterator();
        while (feeds.hasNext())
            FEEDS.add(feeds.next());

        SEARCH = new ArrayList<>();
        Iterator<SecuritySearchProvider> search = ServiceLoader.load(SecuritySearchProvider.class).iterator();
        while (search.hasNext())
            SEARCH.add(search.next());

    }
}
