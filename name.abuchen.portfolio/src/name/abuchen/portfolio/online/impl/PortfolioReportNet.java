package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.osgi.framework.FrameworkUtil;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.json.JClient;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.online.SecuritySearchProvider;
import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;
import name.abuchen.portfolio.util.WebAccess;

public class PortfolioReportNet
{
    public static class MarketInfo
    {
        private String marketCode;
        private String currencyCode;
        private String symbol;
        private LocalDate firstPriceDate;
        private LocalDate lastPriceDate;

        static List<MarketInfo> from(JSONArray json)
        {
            if (json == null)
                return Collections.emptyList();

            List<MarketInfo> answer = new ArrayList<>();

            for (Object obj : json)
            {
                JSONObject market = (JSONObject) obj;

                MarketInfo info = new MarketInfo();
                info.marketCode = (String) market.get("marketCode"); //$NON-NLS-1$
                info.currencyCode = (String) market.get("currencyCode"); //$NON-NLS-1$
                info.symbol = (String) market.get("symbol"); //$NON-NLS-1$
                info.firstPriceDate = YahooHelper.fromISODate((String) market.get("firstPriceDate")); //$NON-NLS-1$
                info.lastPriceDate = YahooHelper.fromISODate((String) market.get("lastPriceDate")); //$NON-NLS-1$
                answer.add(info);
            }

            return answer;
        }

        private MarketInfo()
        {
        }

        public String getMarketCode()
        {
            return marketCode;
        }

        public String getCurrencyCode()
        {
            return currencyCode;
        }

        public String getSymbol()
        {
            return symbol;
        }

        public LocalDate getFirstPriceDate()
        {
            return firstPriceDate;
        }

        public LocalDate getLastPriceDate()
        {
            return lastPriceDate;
        }
    }

    public static class OnlineItem implements ResultItem
    {
        private String id;
        private String name;
        private String type;

        private String isin;
        private String wkn;
        private List<MarketInfo> markets;

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
            else if (TYPE_FUND.equals(t))
                vehicle.type = Messages.LabelSearchFund;
            else
                vehicle.type = t;

            vehicle.isin = (String) jsonObject.get("isin"); //$NON-NLS-1$
            vehicle.wkn = (String) jsonObject.get("wkn"); //$NON-NLS-1$
            vehicle.markets = MarketInfo.from((JSONArray) jsonObject.get("markets")); //$NON-NLS-1$
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
            return markets.stream().map(MarketInfo::getSymbol).reduce((r, l) -> r + "," + l).orElse(null); //$NON-NLS-1$
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
        public boolean hasPrices()
        {
            return !markets.isEmpty();
        }
        
        public List<MarketInfo> getMarkets()
        {
            return markets;
        }

        @Override
        public Security create(Client client)
        {
            Security security = new Security(name, client.getBaseCurrency());

            security.setOnlineId(id);

            security.setIsin(isin);
            security.setWkn(wkn);
            security.setTickerSymbol(markets.stream().map(MarketInfo::getSymbol).findAny().orElse(null));

            security.setPropertyValue(SecurityProperty.Type.FEED, PortfolioReportQuoteFeed.MARKETS_PROPERTY_NAME,
                            markets.isEmpty() ? null : JClient.GSON.toJson(markets));

            if (!markets.isEmpty())
            {
                var market = markets.get(0);

                security.setCurrencyCode(market.getCurrencyCode());
                security.setFeed(PortfolioReportQuoteFeed.ID);
                security.setPropertyValue(SecurityProperty.Type.FEED, PortfolioReportQuoteFeed.MARKET_PROPERTY_NAME,
                                market.getMarketCode());
            }

            return security;
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

            if (security.setPropertyValue(SecurityProperty.Type.FEED, PortfolioReportQuoteFeed.MARKETS_PROPERTY_NAME,
                            markets.isEmpty() ? null : JClient.GSON.toJson(markets)))
                isDirty = true;

            return isDirty;
        }

        @Override
        public String getSource()
        {
            return "Porfolio Report"; //$NON-NLS-1$
        }
    }

    private static final String TYPE_SHARE = "share"; //$NON-NLS-1$
    private static final String TYPE_BOND = "bond"; //$NON-NLS-1$
    private static final String TYPE_FUND = "fund"; //$NON-NLS-1$

    private static final String HOST = "api.portfolio-report.net"; //$NON-NLS-1$

    public List<ResultItem> search(String query, SecuritySearchProvider.Type type) throws IOException
    {
        WebAccess webAccess = new WebAccess(HOST, "/securities/search/" + query) //$NON-NLS-1$
                        .addUserAgent("PortfolioPerformance/" //$NON-NLS-1$
                                        + FrameworkUtil.getBundle(PortfolioReportNet.class).getVersion().toString());

        switch (type)
        {
            case SHARE:
                webAccess.addParameter("securityType", TYPE_SHARE); //$NON-NLS-1$
                break;
            case BOND:
                webAccess.addParameter("securityType", TYPE_BOND); //$NON-NLS-1$
                break;
            case ALL:
            default:
                // do nothing
        }

        return readItems(webAccess.get());
    }

    public Optional<ResultItem> getUpdatedValues(String onlineId) throws IOException
    {
        @SuppressWarnings("nls")
        String html = new WebAccess(HOST, "/securities/uuid/" + onlineId)
                        .addUserAgent("PortfolioPerformance/"
                                        + FrameworkUtil.getBundle(PortfolioReportNet.class).getVersion().toString())
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

    public static Security createFrom(ResultItem item, Client client)
    {
        if (item instanceof OnlineItem onlineItem)
        {
            return onlineItem.create(client);
        }
        else
        {
            throw new IllegalArgumentException();
        }
    }
}
