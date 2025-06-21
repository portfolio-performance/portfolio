package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.online.Factory;
import name.abuchen.portfolio.online.SecuritySearchProvider;
import name.abuchen.portfolio.online.SecurityType;
import name.abuchen.portfolio.util.WebAccess;
import name.abuchen.portfolio.util.WebAccess.WebAccessException;

public class PortfolioPerformanceSearchProvider implements SecuritySearchProvider
{
    static class Result implements ResultItem
    {
        private String provider;
        private String name;
        private String isin;
        private String type;

        private List<ResultItem> markets = new ArrayList<>();

        private String exchange;
        private String currency;
        private String symbol;
        private String url;
        private Map<String, String> properties;

        public static Optional<ResultItem> from(JSONObject json)
        {
            var answer = new Result();

            answer.provider = (String) json.get("provider"); //$NON-NLS-1$
            answer.name = (String) json.get("description"); //$NON-NLS-1$
            answer.isin = (String) json.get("isin"); //$NON-NLS-1$
            answer.type = (String) json.get("type"); //$NON-NLS-1$

            answer.type = SecurityType.convertType(answer.type);

            var markets = (JSONArray) json.get("markets"); //$NON-NLS-1$

            if (markets != null)
            {
                for (var m : markets)
                {
                    var market = (JSONObject) m;

                    var item = new Result();
                    item.provider = answer.provider;
                    item.name = answer.name;
                    item.isin = answer.isin;
                    item.type = answer.type;
                    item.symbol = (String) market.get("symbol"); //$NON-NLS-1$
                    item.currency = (String) market.get("currency"); //$NON-NLS-1$
                    item.exchange = (String) market.get("exchange"); //$NON-NLS-1$
                    item.url = (String) market.get("url"); //$NON-NLS-1$

                    var properties = (JSONObject) market.get("properties"); //$NON-NLS-1$
                    if (properties != null)
                    {
                        item.properties = new HashMap<>();
                        for (var key : properties.keySet()) // NOSONAR
                            item.properties.put((String) key, (String) properties.get(key));
                    }

                    answer.markets.add(item);
                }

                answer.markets.sort(new ByExchangeComparator());
            }

            // if the instrument is traded only on one market, do only return
            // this one market to simplify the user experience in the search
            // dialog
            return Optional.of(answer.markets.size() == 1 ? answer.markets.get(0) : answer);
        }

        private Result()
        {
        }

        @Override
        public String getSymbol()
        {
            return markets.isEmpty() ? symbol
                            : markets.stream().map(e -> e.getSymbol()).distinct().collect(Collectors.joining(", ")); //$NON-NLS-1$
        }

        @Override
        public String getName()
        {
            return name;
        }

        @Override
        public String getType()
        {
            return type;
        }

        @Override
        public String getExchange()
        {
            return exchange;
        }

        @Override
        public String getIsin()
        {
            return isin;
        }

        @Override
        public String getWkn()
        {
            return null;
        }

        @Override
        public String getCurrencyCode()
        {
            return markets.isEmpty() ? currency
                            : markets.stream().map(e -> e.getCurrencyCode()).distinct()
                                            .collect(Collectors.joining(", ")); //$NON-NLS-1$
        }

        @Override
        public String getSource()
        {
            var feed = Factory.getQuoteFeedProvider(provider);
            return feed != null ? feed.getName() : provider;
        }

        @Override
        public String getFeedId()
        {
            return provider;
        }

        @Override
        public List<ResultItem> getMarkets()
        {
            return markets;
        }

        @Override
        public Security create(Client client)
        {
            var security = new Security(name, currency);
            security.setIsin(isin);
            security.setTickerSymbol(symbol);
            security.setFeed(provider);

            if (url != null)
                security.setFeedURL(url);

            if (properties != null)
                properties.forEach((k, v) -> security.setPropertyValue(SecurityProperty.Type.FEED, k, v));

            return security;
        }
    }

    private static final class ByExchangeComparator implements Comparator<ResultItem>, Serializable
    {
        private static final long serialVersionUID = 1L;

        @SuppressWarnings("nls")
        private static final List<String> EXCHANGES = List.of("XNAS", "XNYS", "OOTC", "XETR", "XSWX", "XLON");

        @Override
        public int compare(ResultItem left, ResultItem right)
        {
            var cmp = compareByPriority(left.getExchange(), right.getExchange(), EXCHANGES);
            if (cmp != 0)
                return cmp;

            return compareAlphabetically(left.getExchange(), right.getExchange());

        }

        private static int compareByPriority(String left, String right, List<String> priorityList)
        {
            var li = priorityList.indexOf(left);
            var ri = priorityList.indexOf(right);

            if (li != ri)
                return li == -1 ? 1 : ri == -1 ? -1 : li - ri; // NOSONAR

            return 0;
        }

        private static int compareAlphabetically(String left, String right)
        {
            if (left == null && right == null)
                return 0;
            if (left == null)
                return 1;
            if (right == null)
                return -1;
            return left.compareTo(right);
        }
    }

    private static final String NAME = "Portfolio Performance"; //$NON-NLS-1$

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public List<ResultItem> search(String query) throws IOException
    {
        return internalSearch("q", query); //$NON-NLS-1$
    }

    /* package */ List<ResultItem> internalSearch(String parameter, String query) throws IOException
    {
        try
        {
            @SuppressWarnings("nls")
            var json = new WebAccess("api.portfolio-performance.info", "/v1/search") //
                            .addParameter(parameter, query).get();

            var response = (JSONArray) JSONValue.parse(json);
            if (response != null)
            {
                List<ResultItem> answer = new ArrayList<>();

                for (var ii = 0; ii < response.size(); ii++)
                {
                    Result.from((JSONObject) response.get(ii)).ifPresent(answer::add);
                }
                return answer;
            }
        }
        catch (WebAccessException ex)
        {
            PortfolioLog.error(ex);
        }

        return Collections.emptyList();
    }
}
