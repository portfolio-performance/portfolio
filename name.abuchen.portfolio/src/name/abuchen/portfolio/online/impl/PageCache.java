package name.abuchen.portfolio.online.impl;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/* package */ class PageCache<T>
{
    private static class PageEntry<T>
    {
        long ts;
        T answer;

        public PageEntry(T prices)
        {
            this.ts = System.currentTimeMillis();
            this.answer = prices;
        }
    }

    private static final int CACHE_SIZE = 50;

    private final long expirationTime;

    private HashMap<String, PageEntry<T>> map = new LinkedHashMap<String, PageEntry<T>>()
    {
        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, PageEntry<T>> eldest)
        {
            return size() >= CACHE_SIZE;
        }
    };

    public PageCache()
    {
        this.expirationTime = Duration.ofMinutes(5).toMillis();
    }

    public PageCache(Duration expirationTime)
    {
        this.expirationTime = expirationTime.toMillis();
    }

    /**
     * Returns a cached list of security prices for a given URL.
     * 
     * @return list of prices; null if no cache entry exists
     */
    public synchronized T lookup(String url)
    {
        PageEntry<T> entry = map.get(url);

        if (entry == null)
            return null; // NOSONAR

        if (entry.ts < System.currentTimeMillis() - expirationTime)
        {
            map.remove(url);
            return null; // NOSONAR
        }
        else
        {
            return entry.answer;
        }
    }

    public synchronized void put(String url, T prices)
    {
        map.put(url, new PageEntry<>(prices));
    }

    public synchronized void computeIfAbsent(String url, T prices)
    {
        map.computeIfAbsent(url, key -> new PageEntry<>(prices));
    }
}
