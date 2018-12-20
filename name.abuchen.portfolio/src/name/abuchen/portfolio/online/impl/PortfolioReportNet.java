package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;

public class PortfolioReportNet
{
    public static class Market
    {
        private String name;
        private String symbol;

        static List<Market> from(JSONObject json)
        {
            if (json == null || json.isEmpty())
                return Collections.emptyList();

            @SuppressWarnings("unchecked")
            Set<Map.Entry<Object, Object>> set = json.entrySet();

            return set.stream().map(entry -> {
                Market m = new Market();
                m.name = entry.getKey().toString();
                m.symbol = (String) ((JSONObject) entry.getValue()).get("symbol"); //$NON-NLS-1$
                return m;
            }).filter(m -> m.getSymbol() != null && !m.getSymbol().isEmpty()).collect(Collectors.toList());
        }

        private Market()
        {}

        public String getName()
        {
            return name;
        }

        public String getSymbol()
        {
            return symbol;
        }
    }

    public static class OnlineItem implements ResultItem
    {
        private String id;
        private String name;
        private String type;

        private String isin;
        private String wkn;
        private List<Market> markets;

        /* package */ static OnlineItem from(JSONObject json)
        {
            OnlineItem vehicle = new OnlineItem();
            vehicle.id = (String) json.get("uuid"); //$NON-NLS-1$
            vehicle.name = (String) json.get("name"); //$NON-NLS-1$
            vehicle.type = (String) json.get("security_type"); //$NON-NLS-1$

            vehicle.isin = (String) json.get("isin"); //$NON-NLS-1$
            vehicle.wkn = (String) json.get("wkn"); //$NON-NLS-1$
            vehicle.markets = Market.from((JSONObject) json.get("markets")); //$NON-NLS-1$
            return vehicle;
        }

        private OnlineItem()
        {}

        public String getId()
        {
            return id;
        }

        @Override
        public String getName()
        {
            return name;
        }

        @Override
        public String getIsin()
        {
            return isin;
        }

        @Override
        public String getWkn()
        {
            return wkn;
        }

        @Override
        public String getSymbol()
        {
            return markets.stream().findAny().map(Market::getSymbol).orElse(null);
        }

        @Override
        public String getType()
        {
            return type;
        }

        @Override
        public String getExchange()
        {
            return null;
        }

        @Override
        public void applyTo(Security security)
        {
            security.setOnlineId(id);

            security.setName(name);

            security.setIsin(isin);
            security.setWkn(wkn);
            security.setTickerSymbol(getSymbol());
        }
    }

    private static final String QUERY_URL;

    static
    {
        String baseURL = System.getProperty("net.portfolio-report.baseUrl"); //$NON-NLS-1$
        if (baseURL == null || baseURL.isEmpty())
            baseURL = "https://www.portfolio-report.net"; //$NON-NLS-1$

        QUERY_URL = baseURL + "/api/securities/search/{0}"; //$NON-NLS-1$
    }

    public List<ResultItem> search(String query) throws IOException
    {
        String searchUrl = MessageFormat.format(QUERY_URL, URLEncoder.encode(query, StandardCharsets.UTF_8.name()));

        URL url = new URL(searchUrl);
        URLConnection con = url.openConnection();
        con.setConnectTimeout(500);
        con.setReadTimeout(2000);

        return readItems(con);
    }

    private List<ResultItem> readItems(URLConnection con) throws IOException
    {
        List<ResultItem> onlineItems = new ArrayList<>();

        try (Scanner scanner = new Scanner(con.getInputStream(), StandardCharsets.UTF_8.name()))
        {
            String body = scanner.useDelimiter("\\A").next(); //$NON-NLS-1$

            JSONArray response = (JSONArray) JSONValue.parse(body);
            if (response != null)
            {
                for (int ii = 0; ii < response.size(); ii++)
                    onlineItems.add(OnlineItem.from((JSONObject) response.get(ii)));
            }
        }

        return onlineItems;
    }
}
