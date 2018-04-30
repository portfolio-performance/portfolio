package name.abuchen.portfolio.online.impl;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.model.LatestSecurityPrice;

/* package */ class PageCache
{
    private static class PageEntry
    {
        long ts;
        List<LatestSecurityPrice> answer;

        public PageEntry(List<LatestSecurityPrice> prices)
        {
            this.ts = System.currentTimeMillis();
            this.answer = prices;
        }
    }

    private static final int CACHE_SIZE = 50;
    private static final long EXPIRATION_TIME = 5L * 60L * 1000L; // 5 minutes

    private HashMap<String, PageEntry> map = new LinkedHashMap<String, PageEntry>()
    {
        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, PageEntry> eldest)
        {
            return size() >= CACHE_SIZE;
        }
    };

    /**
     * Returns a cached list of security prices for a given URL.
     * 
     * @return list of prices; null if no cache entry exists
     */
    public synchronized List<LatestSecurityPrice> lookup(String url)
    {
        PageEntry entry = map.get(url);

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

    public synchronized void put(String url, List<LatestSecurityPrice> prices)
    {
        map.put(url, new PageEntry(prices));
    }
}
