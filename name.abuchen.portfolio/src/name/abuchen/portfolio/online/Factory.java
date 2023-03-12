package name.abuchen.portfolio.online;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

public class Factory
{
    private static final List<QuoteFeed> FEEDS;
    private static final List<DividendFeed> DIVIDEND_FEEDS;
    private static final List<SecuritySearchProvider> SEARCH;

    private Factory()
    {
    }

    /**
     * Returns all the {@link QuoteFeed QuoteFeeds} declared in
     * <code>META-INF/services/name.abuchen.portfolio.online.QuoteFeed</code>.
     */
    public static final List<QuoteFeed> getQuoteFeedProvider()
    {
        return FEEDS;
    }

    /**
     * Returns the {@link QuoteFeed} for the given ID. This makes it possible to
     * get one specific QuoteFeed provider from those classes declared in
     * <code>META-INF/services/name.abuchen.portfolio.online.QuoteFeed</code>.
     * 
     * @see QuoteFeed#getId()
     */
    public static QuoteFeed getQuoteFeedProvider(String feedId)
    {
        for (QuoteFeed feed : FEEDS)
        {
            if (feed.getId().equals(feedId))
                return feed;
        }
        return null;
    }

    /**
     * Returns the {@link QuoteFeed} for the given feedType. This makes it
     * possible to get one specific QuoteFeed provider from those classes
     * declared in
     * <code>META-INF/services/name.abuchen.portfolio.online.QuoteFeed</code>.
     */

    public static <F extends QuoteFeed> F getQuoteFeed(Class<F> feedType)
    {
        return feedType.cast(FEEDS.stream().filter(c -> feedType.equals(c.getClass())).findAny()
                        .orElseThrow(IllegalArgumentException::new));
    }

    /**
     * Returns the {@link DividendFeed} for the given feedType. This makes it possible to
     * get one specific provider from those classes declared in
     * <code>META-INF/services/name.abuchen.portfolio.online.DividendFeed</code>.
     */
    public static <F extends DividendFeed> F getDividendFeed(Class<F> feedType)
    {
        return feedType.cast(DIVIDEND_FEEDS.stream().filter(c -> feedType.equals(c.getClass())).findAny()
                        .orElseThrow(IllegalArgumentException::new));
    }

    /**
     * Returns all the {@link SecuritySearchProvider SecuritySearchProviders} declared in
     * <code>META-INF/services/name.abuchen.portfolio.online.SecuritySearchProvider</code>.
     */
    public static final List<SecuritySearchProvider> getSearchProvider()
    {
        return SEARCH;
    }

    /**
     * Returns the {@link SecuritySearchProvider} for the given ID. This makes it possible to
     * get one specific provider from those classes declared in
     * <code>META-INF/services/name.abuchen.portfolio.online.SecuritySearchProvider</code>.
     * 
     * @see SecuritySearchProvider#getId()
     */
    public static <S extends SecuritySearchProvider> S getSearchProvider(Class<S> feedType)
    {
        return feedType.cast(SEARCH.stream()
                        .filter(c -> feedType.equals(c.getClass()))
                        .findAny()
                        .orElseThrow(IllegalArgumentException::new));
    }

    static
    {
        FEEDS = new ArrayList<>();
        Iterator<QuoteFeed> feeds = ServiceLoader.load(QuoteFeed.class).iterator();
        while (feeds.hasNext())
            FEEDS.add(feeds.next());

        DIVIDEND_FEEDS = new ArrayList<>();
        Iterator<DividendFeed> dividendFeeds = ServiceLoader.load(DividendFeed.class).iterator();
        while (dividendFeeds.hasNext())
            DIVIDEND_FEEDS.add(dividendFeeds.next());

        SEARCH = new ArrayList<>();
        Iterator<SecuritySearchProvider> search = ServiceLoader.load(SecuritySearchProvider.class).iterator();
        while (search.hasNext())
            SEARCH.add(search.next());

    }
}
