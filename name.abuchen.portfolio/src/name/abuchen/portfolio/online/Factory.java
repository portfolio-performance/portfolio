package name.abuchen.portfolio.online;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

public class Factory
{
    private static final List<QuoteFeed> FEEDS;
    private static final List<EventFeed> EVENTS;
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

    public static final List<EventFeed> getEventFeedProvider()
    {
        return EVENTS;
    }

    public static EventFeed getEventFeedProvider(String feedId)
    {
        for (EventFeed feed : EVENTS)
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

        EVENTS = new ArrayList<EventFeed>();
        Iterator<EventFeed> events = ServiceLoader.load(EventFeed.class).iterator();
        while (events.hasNext())
            EVENTS.add(events.next());

        // SEARCH = new ArrayList<SecuritySearchProvider>();
        // Iterator<SecuritySearchProvider> search = ServiceRegistry.lookupProviders(SecuritySearchProvider.class);
        SEARCH = new ArrayList<>();
        Iterator<SecuritySearchProvider> search = ServiceLoader.load(SecuritySearchProvider.class).iterator();
        while (search.hasNext())
            SEARCH.add(search.next());

    }

    public static final <T extends Feed>  List<Feed> cast2FeedList (List<T> iList)
    {
        if (iList != null)
        {
            List<Feed> oList = new ArrayList<>();
            for (T obj : iList)
            {
                    if (obj instanceof QuoteFeed)
                        oList.add((QuoteFeed) obj); // need to cast each object specifically
                    else if (obj instanceof EventFeed)
                        oList.add((EventFeed) obj); // need to cast each object specifically
            }
            return oList;
        }
        else
            return null;
    }
}
