package name.abuchen.portfolio.online.impl;

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
    private static final long EXPIRATION_TIME = 5L * 60L * 1000L; // 5 minutes

    private HashMap<String, PageEntry<T>> map = new LinkedHashMap<String, PageEntry<T>>()
    {
        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, PageEntry<T>> eldest)
        {
            return size() >= CACHE_SIZE;
        }
    };

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

        if (entry.ts < System.currentTimeMillis() - EXPIRATION_TIME)
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
        map.put(url, new PageEntry<T>(prices));
    }
}
