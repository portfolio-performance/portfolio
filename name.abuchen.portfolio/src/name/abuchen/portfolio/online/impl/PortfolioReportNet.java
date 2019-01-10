package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.http.client.utils.URIBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.osgi.framework.FrameworkUtil;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.SecuritySearchProvider;
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
        {
        }

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

            String t = (String) json.get("security_type"); //$NON-NLS-1$
            if (TYPE_SHARE.equals(t))
                vehicle.type = SecuritySearchProvider.Type.SHARE.toString();
            else if (TYPE_BOND.equals(t))
                vehicle.type = SecuritySearchProvider.Type.BOND.toString();
            else
                vehicle.type = t;

            vehicle.isin = (String) json.get("isin"); //$NON-NLS-1$
            vehicle.wkn = (String) json.get("wkn"); //$NON-NLS-1$
            vehicle.markets = Market.from((JSONObject) json.get("markets")); //$NON-NLS-1$
            return vehicle;
        }

        private OnlineItem()
        {
        }

        @Override
        public String getOnlineId()
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

    private static final String TYPE_SHARE = "share"; //$NON-NLS-1$
    private static final String TYPE_BOND = "bond"; //$NON-NLS-1$

    private static final String HOST = "www.portfolio-report.net"; //$NON-NLS-1$

    public List<ResultItem> search(String query, SecuritySearchProvider.Type type) throws IOException
    {
        try
        {
            URIBuilder uriBuilder = new URIBuilder().setScheme("https").setHost(HOST) //$NON-NLS-1$
                            .setPath("/api/securities/search/" + query); //$NON-NLS-1$

            if (type != null)
            {
                if (type == SecuritySearchProvider.Type.SHARE)
                    uriBuilder.addParameter("type", TYPE_SHARE); //$NON-NLS-1$
                else if (type == SecuritySearchProvider.Type.BOND)
                    uriBuilder.addParameter("type", TYPE_BOND); //$NON-NLS-1$
            }

            URL searchUrl = uriBuilder.build().toURL();

            URLConnection con = searchUrl.openConnection();
            con.setConnectTimeout(500);
            con.setReadTimeout(2000);

            return readItems(con);
        }
        catch (URISyntaxException e)
        {
            throw new IOException(e);
        }
    }

    public Optional<ResultItem> getUpdatedValues(String onlineId) throws IOException
    {
        try
        {
            URL objectUrl = new URIBuilder().setScheme("https").setHost(HOST).setPath("/api/securities/" + onlineId) //$NON-NLS-1$ //$NON-NLS-2$
                            .build().toURL();

            HttpURLConnection con = (HttpURLConnection) objectUrl.openConnection();
            con.setConnectTimeout(1000);
            con.setReadTimeout(2000);

            con.setRequestProperty("X-Source", "Portfolio Peformance " //$NON-NLS-1$ //$NON-NLS-2$
                            + FrameworkUtil.getBundle(PortfolioReportNet.class).getVersion().toString());
            con.setRequestProperty("X-Reason", "periodic update"); //$NON-NLS-1$ //$NON-NLS-2$
            con.setRequestProperty("Content-Type", "application/json;chartset=UTF-8"); //$NON-NLS-1$ //$NON-NLS-2$

            Optional<ResultItem> onlineItem = Optional.empty();

            int responseCode = con.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK)
                throw new IOException(objectUrl + " --> " + responseCode); //$NON-NLS-1$

            try (Scanner scanner = new Scanner(con.getInputStream(), StandardCharsets.UTF_8.name()))
            {
                String html = scanner.useDelimiter("\\A").next(); //$NON-NLS-1$

                JSONObject response = (JSONObject) JSONValue.parse(html);
                if (response != null)
                    onlineItem = Optional.of(OnlineItem.from(response));
            }

            return onlineItem;
        }
        catch (URISyntaxException e)
        {
            throw new IOException(e);
        }
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
