package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.osgi.framework.FrameworkUtil;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Attributes;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.model.AttributeType.PercentPlainConverter;
import name.abuchen.portfolio.model.AttributeType.StringConverter;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;
import name.abuchen.portfolio.util.WebAccess;

public class ETFDataCom
{
    /* package */ static class SymbolInfo
    {
        private String exchange;
        private String symbol;

        static List<SymbolInfo> from(JSONArray listings)
        {
            if (listings == null)
                return Collections.emptyList();

            List<SymbolInfo> res = new ArrayList<>();
            for (Object listingAsObject : listings)
            {
                JSONObject listing = (JSONObject) listingAsObject;

                SymbolInfo m = new SymbolInfo();
                m.exchange = listing.get("exchange").toString().toUpperCase(Locale.US); //$NON-NLS-1$
                m.symbol = listing.get("ticker").toString().toUpperCase(Locale.US); //$NON-NLS-1$
                res.add(m);
            }

            return(res);
        }

        private SymbolInfo()
        {
        }

        public String getExchange()
        {
            return exchange;
        }

        public String getSymbol()
        {
            return symbol;
        }
    }


    /* package */ static class OnlineItem implements ResultItem
    {
        private Double ter;
        private String indexName;
        private String name;
        private String type = "ETF"; //$NON-NLS-1$
        private String isin;
        private String provider;
        private String domicile;
        private String currencyCode;
        private String assetClass;
        private String replicationMethod;
        private String distributionFrequency;
        private String distributionType;

        private List<SymbolInfo> symbols;

        /* package */ static OnlineItem from(JSONObject jsonObject)
        {
            OnlineItem vehicle = new OnlineItem();

            vehicle.ter = (Double)  jsonObject.get("totalFee"); //$NON-NLS-1$
            vehicle.indexName = (String) jsonObject.get("indexName"); //$NON-NLS-1$
            vehicle.name = (String) jsonObject.get("name"); //$NON-NLS-1$
            vehicle.isin = (String) jsonObject.get("isin"); //$NON-NLS-1$
            vehicle.provider = (String) jsonObject.get("provider"); //$NON-NLS-1$
            vehicle.domicile = (String) jsonObject.get("domicile"); //$NON-NLS-1$
            vehicle.currencyCode = (String) jsonObject.get("baseCurrency"); //$NON-NLS-1$
            vehicle.replicationMethod = (String) jsonObject.get("replicationMethod"); //$NON-NLS-1$
            vehicle.distributionFrequency = (String) jsonObject.get("distributionFrequency"); //$NON-NLS-1$
            vehicle.distributionType = (String) jsonObject.get("distributionType"); //$NON-NLS-1$

            vehicle.symbols = SymbolInfo.from((JSONArray) jsonObject.get("listings")); //$NON-NLS-1$
            return vehicle;
        }

        private OnlineItem()
        {
        }

        @Override
        public String getOnlineId()
        {
            return isin;
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
            return ""; //$NON-NLS-1$
        }

        @Override
        public String getSymbol()
        {
            return symbols.stream().map(SymbolInfo::getSymbol).reduce((r, l) -> r + "," + l).orElse(null); //$NON-NLS-1$
        }

        @Override
        public String getType()
        {
            return type;
        }

        @Override
        public String getExchange()
        {
            return symbols.stream().map(SymbolInfo::getExchange).reduce((r, l) -> r + "," + l).orElse(null); //$NON-NLS-1$
        }

        @Override
        public boolean hasPrices()
        {
            return false;
        }

        private Map<String, String> attributeMapping()
        {
            Map<String, String> attributeMapping = new HashMap<String, String>();
            attributeMapping.put("indexName", indexName); //$NON-NLS-1$
            attributeMapping.put("vendor", provider); //$NON-NLS-1$
            attributeMapping.put("domicile", domicile); //$NON-NLS-1$
            attributeMapping.put("replicationMethod", replicationMethod); //$NON-NLS-1$
            attributeMapping.put("distributionFrequency", distributionFrequency); //$NON-NLS-1$
            attributeMapping.put("distributionType", distributionType); //$NON-NLS-1$

            return attributeMapping;
        }

        @Override
        public Security create()
        {
            Security security = new Security();

            security.setOnlineId(isin);

            security.setName(name);
            security.setIsin(isin);
            security.setTickerSymbol(symbols.stream().map(SymbolInfo::getSymbol).findAny().orElse(null));
            symbols.forEach(symbolInfo -> security.addProperty(new SecurityProperty(SecurityProperty.Type.MARKET,
                            symbolInfo.getExchange(), symbolInfo.getSymbol())));
            security.setCurrencyCode(CurrencyUnit.getInstance(currencyCode).getCurrencyCode());


            Attributes attributes = new Attributes();

            AttributeType terType = new AttributeType("ter"); //$NON-NLS-1$
            terType.setName(Messages.AttributesTERName);
            terType.setColumnLabel(Messages.AttributesTERColumn);
            terType.setTarget(Security.class);
            terType.setType(Double.class);
            terType.setConverter(PercentPlainConverter.class);
            attributes.put(terType, ter/100);

            for (Map.Entry<String, String> am : attributeMapping().entrySet())
            {
                AttributeType at = new AttributeType(am.getKey());
                at.setName(am.getKey());
                at.setColumnLabel(am.getKey());
                at.setTarget(Security.class);
                at.setType(String.class);
                at.setConverter(StringConverter.class);
                attributes.put(at, am.getValue());
            }

            security.setAttributes(attributes);

            return security;
        }

        public boolean update(Security security)
        {
          return false;
        }

        @Override
        public String getProvider()
        {
            return "ETF-Data.com"; //$NON-NLS-1$
        }

        @Override
        public String getExtraAttributeNames()
        {
            return String.join(", ", attributeMapping().keySet()); //$NON-NLS-1$
        }
    }


    private static final String HOST = "api.etf-data.com"; //$NON-NLS-1$

    public List<ResultItem> search(String isin) throws IOException
    {
        try
        {
            WebAccess webAccess = new WebAccess(HOST, "/product/" + isin) //$NON-NLS-1$
                            .addUserAgent("PortfolioPerformance/" //$NON-NLS-1$
                                            + FrameworkUtil.getBundle(ETFDataCom.class).getVersion().toString());
            return readItems(webAccess.get());
        }
        catch (IOException e)
        {
            return(new ArrayList<ResultItem>());
        }
    }

    private List<ResultItem> readItems(String jsonBlob)
    {
        List<ResultItem> onlineItems = new ArrayList<>();
        JSONObject response = (JSONObject) JSONValue.parse(jsonBlob);
        onlineItems.add(OnlineItem.from(response));

        return onlineItems;
    }

    public static boolean updateWith(Security security, ResultItem item)
    {
        if (!(item instanceof OnlineItem))
            throw new IllegalArgumentException();

        return ((OnlineItem) item).update(security);
    }
}
