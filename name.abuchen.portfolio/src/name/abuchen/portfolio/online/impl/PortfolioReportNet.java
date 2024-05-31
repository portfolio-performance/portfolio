package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.osgi.framework.FrameworkUtil;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.SecuritySearchProvider;
import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;
import name.abuchen.portfolio.util.WebAccess;

public class PortfolioReportNet
{
    public static class OnlineItem implements ResultItem
    {
        private String id;
        private String name;
        private String securityType;
        private boolean pricesAvailable;

        private String isin;
        private String wkn;
        private String code;

        /* package */ static OnlineItem from(JSONObject jsonObject)
        {
            OnlineItem vehicle = new OnlineItem();
            vehicle.id = (String) jsonObject.get("uuid"); //$NON-NLS-1$
            vehicle.name = (String) jsonObject.get("name"); //$NON-NLS-1$
            vehicle.securityType = (String) jsonObject.get(PROPERTY_SECURITY_TYPE);

            vehicle.isin = (String) jsonObject.get("isin"); //$NON-NLS-1$
            vehicle.wkn = (String) jsonObject.get("wkn"); //$NON-NLS-1$
            vehicle.code = (String) jsonObject.get("code"); //$NON-NLS-1$

            vehicle.pricesAvailable = (boolean) jsonObject.get("pricesAvailable"); //$NON-NLS-1$
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
            return code;
        }

        @Override
        public String getType()
        {
            if (TYPE_SHARE.equals(securityType))
                return SecuritySearchProvider.Type.SHARE.toString();
            else if (TYPE_BOND.equals(securityType))
                return SecuritySearchProvider.Type.BOND.toString();
            else if (TYPE_FUND.equals(securityType))
                return Messages.LabelSearchFund;
            else if (TYPE_CRYPTO.equals(securityType))
                return Messages.LabelCryptocurrency;
            else
                return securityType;
        }

        @Override
        public String getExchange()
        {
            return null;
        }

        @Override
        public boolean hasPrices()
        {
            return pricesAvailable;
        }

        @Override
        public Security create(Client client)
        {
            Security security = new Security(name, client.getBaseCurrency());

            security.setOnlineId(id);
            security.setIsin(isin);
            security.setWkn(wkn);
            security.setTickerSymbol(code);

            if (pricesAvailable)
            {
                security.setFeed(PortfolioReportQuoteFeed.ID);
                security.setLatestFeed(QuoteFeed.MANUAL);
            }

            if (!TYPE_CRYPTO.equals(securityType))
            {
                // for the time being, we set the currency to EUR for all other
                // instruments. We know that the primary currency is EUR.
                security.setCurrencyCode(CurrencyUnit.EUR);
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

            return isDirty;
        }

        @Override
        public String getSource()
        {
            return "Porfolio Report"; //$NON-NLS-1$
        }
    }

    private static final String PROPERTY_SECURITY_TYPE = "securityType"; //$NON-NLS-1$

    private static final String TYPE_SHARE = "share"; //$NON-NLS-1$
    private static final String TYPE_BOND = "bond"; //$NON-NLS-1$
    private static final String TYPE_FUND = "fund"; //$NON-NLS-1$
    private static final String TYPE_CRYPTO = "crypto"; //$NON-NLS-1$

    private static final String HOST = "api.portfolio-report.net"; //$NON-NLS-1$

    public List<ResultItem> search(String query, SecuritySearchProvider.Type type) throws IOException
    {
        var version = FrameworkUtil.getBundle(PortfolioReportNet.class).getVersion().toString();

        WebAccess webAccess = new WebAccess(HOST, "/v1/securities/search") //$NON-NLS-1$
                        .addUserAgent("PortfolioPerformance/" + version); //$NON-NLS-1$

        webAccess.addParameter("q", query); //$NON-NLS-1$

        switch (type)
        {
            case SHARE:
                webAccess.addParameter(PROPERTY_SECURITY_TYPE, TYPE_SHARE);
                break;
            case BOND:
                webAccess.addParameter(PROPERTY_SECURITY_TYPE, TYPE_BOND);
                break;
            case CRYPTO:
                webAccess.addParameter(PROPERTY_SECURITY_TYPE, TYPE_CRYPTO);
                break;
            case ALL:
            default:
                // do nothing
        }

        return readItems(webAccess.get());
    }

    public Optional<ResultItem> getUpdatedValues(String onlineId) throws IOException
    {
        var version = FrameworkUtil.getBundle(PortfolioReportNet.class).getVersion().toString();

        @SuppressWarnings("nls")
        String html = new WebAccess(HOST, "/securities/uuid/" + onlineId)
                        .addUserAgent("PortfolioPerformance/" + version)
                        .addHeader("X-Source", "Portfolio Peformance " + version)
                        .addHeader("X-Reason", "periodic update")
                        .addHeader("Content-Type", "application/json;chartset=UTF-8") //
                        .get();

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
        if (!(item instanceof OnlineItem oItem))
            throw new IllegalArgumentException("result item is null or of wrong type: " + item); //$NON-NLS-1$

        return oItem.update(security);
    }

    public static Security createFrom(ResultItem item, Client client)
    {
        if (item instanceof OnlineItem onlineItem)
        {
            return onlineItem.create(client);
        }
        else
        {
            throw new IllegalArgumentException("result item is null or of wrong type: " + item); //$NON-NLS-1$
        }
    }
}
