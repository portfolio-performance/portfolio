package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.osgi.framework.FrameworkUtil;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.model.SecurityProperty.Type;
import name.abuchen.portfolio.online.SecuritySearchProvider;
import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;
import name.abuchen.portfolio.util.WebAccess;;

public class PortfolioReportNet
{
    /* package */ static class Market
    {
        private static final String SYMBOL_KEY = "symbol"; //$NON-NLS-1$

        private String name;
        private String symbol;

        static List<Market> from(JSONObject json)
        {
            if (json == null)
                return Collections.emptyList();

            @SuppressWarnings("unchecked")
            Set<Map.Entry<Object, Object>> set = json.entrySet();

            return set.stream() //
                            .filter(entry -> entry.getKey().toString().startsWith(SYMBOL_KEY)) //
                            .map(entry -> {
                                Market m = new Market();
                                String key = entry.getKey().toString();
                                m.name = key.substring(SYMBOL_KEY.length()).toUpperCase(Locale.US);
                                m.symbol = entry.getValue() == null ? null : entry.getValue().toString();
                                return m;
                            }) //
                            .filter(m -> m.getSymbol() != null && !m.getSymbol().isEmpty())
                            .collect(Collectors.toList());
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

    /* package */ static class OnlineItem implements ResultItem
    {
        private String id;
        private String name;
        private String type;

        private String isin;
        private String wkn;
        private List<Market> markets;

        /* package */ static OnlineItem from(JSONObject jsonObject)
        {
            OnlineItem vehicle = new OnlineItem();
            vehicle.id = (String) jsonObject.get("uuid"); //$NON-NLS-1$
            vehicle.name = (String) jsonObject.get("name"); //$NON-NLS-1$

            String t = (String) jsonObject.get("securityType"); //$NON-NLS-1$
            if (TYPE_SHARE.equals(t))
                vehicle.type = SecuritySearchProvider.Type.SHARE.toString();
            else if (TYPE_BOND.equals(t))
                vehicle.type = SecuritySearchProvider.Type.BOND.toString();
            else
                vehicle.type = t;

            vehicle.isin = (String) jsonObject.get("isin"); //$NON-NLS-1$
            vehicle.wkn = (String) jsonObject.get("wkn"); //$NON-NLS-1$
            vehicle.markets = Market.from(jsonObject);
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
            return markets.stream().map(Market::getSymbol).reduce((r, l) -> r + "," + l).orElse(null); //$NON-NLS-1$
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
            security.setTickerSymbol(markets.stream().map(Market::getSymbol).findAny().orElse(null));
            markets.forEach(market -> security.addProperty(
                            new SecurityProperty(SecurityProperty.Type.MARKET, market.getName(), market.getSymbol())));
        }

        public boolean update(Security security)
        {
            boolean isDirty = false;

            if (!Objects.equals(isin, security.getIsin()))
            {
                security.setIsin(isin);
                isDirty = true;
            }

            if (!Objects.equals(wkn, security.getWkn()))
            {
                security.setWkn(wkn);
                isDirty = true;
            }

            List<SecurityProperty> local = security.getProperties()
                            .filter(property -> property.getType() == SecurityProperty.Type.MARKET)
                            .collect(Collectors.toList());

            // we can collect the list into a map because the original JSON data
            // structure is actually a map
            Map<String, String> remote = markets.stream().collect(Collectors.toMap(Market::getName, Market::getSymbol));

            for (SecurityProperty property : local)
            {
                String symbol = remote.remove(property.getName());
                if (symbol == null)
                {
                    security.removeProperty(property);
                    isDirty = true;
                }
                else if (!symbol.equals(property.getValue()))
                {
                    security.removeProperty(property);
                    security.addProperty(new SecurityProperty(Type.MARKET, property.getName(), symbol));
                    isDirty = true;
                }
            }

            remote.forEach((k, v) -> security.addProperty(new SecurityProperty(Type.MARKET, k, v)));

            return isDirty || !remote.isEmpty();
        }
    }

    private static final String TYPE_SHARE = "share"; //$NON-NLS-1$
    private static final String TYPE_BOND = "bond"; //$NON-NLS-1$

    private static final String HOST = "www.portfolio-report.net"; //$NON-NLS-1$

    public List<ResultItem> search(String query, SecuritySearchProvider.Type type) throws IOException
    {
        String html;

        if (type != null)
        {
            html = new WebAccess(HOST, "/api/securities/search/" + query) //$NON-NLS-1$
                            .addParameter("securityType", //$NON-NLS-1$
                                            type == SecuritySearchProvider.Type.SHARE ? TYPE_SHARE : TYPE_BOND)
                            .get();

        }
        else
        {
            html = new WebAccess(HOST, "/api/securities/search/" + query).get(); //$NON-NLS-1$
        }

        return readItems(html);
    }

    public Optional<ResultItem> getUpdatedValues(String onlineId) throws IOException
    {
        @SuppressWarnings("nls")
        String html = new WebAccess(HOST, "/api/securities/uuid/" + onlineId)
                        .addHeader("X-Source",
                                        "Portfolio Peformance " + FrameworkUtil.getBundle(PortfolioReportNet.class)
                                                        .getVersion().toString())
                        .addHeader("X-Reason", "periodic update")
                        .addHeader("Content-Type", "application/json;chartset=UTF-8").get();

        Optional<ResultItem> onlineItem = Optional.empty();
        JSONObject response = (JSONObject) JSONValue.parse(html);
        if (response != null)
            onlineItem = Optional.of(OnlineItem.from(response));

        return onlineItem;
    }

    private List<ResultItem> readItems(String html)
    {
        List<ResultItem> onlineItems = new ArrayList<>();
        JSONArray response = (JSONArray) JSONValue.parse(html);
        if (response != null)
        {
            for (int ii = 0; ii < response.size(); ii++)
                onlineItems.add(OnlineItem.from((JSONObject) response.get(ii)));
        }

        return onlineItems;
    }

    public static boolean updateWith(Security security, ResultItem item)
    {
        if (!(item instanceof OnlineItem))
            throw new IllegalArgumentException();

        return ((OnlineItem) item).update(security);
    }
}
