package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import name.abuchen.portfolio.model.ClientSettings;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.SecuritySearchProvider;
import name.abuchen.portfolio.util.Isin;
import name.abuchen.portfolio.util.WebAccess;

public class LeewaySearchProvider implements SecuritySearchProvider
{
    static class Result implements ResultItem
    {
        private String symbol;
        private String exchange;
        private String name;
        private String type;
        private String isin;

        @SuppressWarnings("nls")
        public static Result from(JSONObject json)
        {
            // Extract values from the JSON object
            String code = (String) json.get("Code");
            String exchange = (String) json.get("Exchange");
            String name = (String) json.get("Name");
            String type = (String) json.get("Type");
            String isin = (String) json.get("ISIN");

            // Map security types to standard values
            Map<String, String> typeMap = new HashMap<>();
            typeMap.put("common stock", SecuritySearchProvider.Type.SHARE.toString());
            typeMap.put("etf", "ETF");
            typeMap.put("fund", "Fund");

            // Convert the security type to a standard value
            type = typeMap.getOrDefault(type.toLowerCase(), "");

            // Combine the symbol and exchange codes to create the security ID
            StringBuilder symbol = new StringBuilder(code);
            symbol.append(".");
            symbol.append(exchange);

            return new Result(isin, symbol.toString(), name, type, exchange);
        }

        public Result(String isin, String symbol, String name, String type, String exchange)
        {
            this.isin = isin;
            this.symbol = symbol;
            this.name = name;
            this.type = type;
            this.exchange = exchange;
        }

        @Override
        public String getSymbol()
        {
            return symbol;
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
        public String getSource()
        {
            return NAME;
        }

        @Override
        public boolean hasPrices()
        {
            return true;
        }

        @Override
        public Security create(ClientSettings settings)
        {
            Security security = new Security();
            security.setName(name);
            security.setTickerSymbol(symbol);
            security.setIsin(isin);
            security.setFeed(LeewayQuoteFeed.ID);
            return security;
        }
    }

    private static final String NAME = "PWP Leeway UG"; //$NON-NLS-1$
    private String apiKey;

    @Override
    public String getName()
    {
        return NAME;
    }

    public void setApiKey(String apiKey)
    {
        this.apiKey = apiKey;
    }

    @Override
    public List<ResultItem> search(String query, Type type) throws IOException
    {
        List<ResultItem> answer = new ArrayList<>();

        // @formatter:off
        // There are only 50 API/day available, so only query with validated ISIN.
        // @formatter:on

        if (Isin.isValid(query))
            addISINSearchPage(answer, query.trim());

        return answer;
    }

    private void addISINSearchPage(List<ResultItem> answer, String query) throws IOException
    {
        String html = new WebAccess("api.leeway.tech", "/api/v1/public/general/isin/" + query) //$NON-NLS-1$ //$NON-NLS-2$
                        .addParameter("apitoken", apiKey) //$NON-NLS-1$
                        .get();

        extract(answer, html);
    }

    @SuppressWarnings("nls")
    void extract(List<ResultItem> answer, String html)
    {
        JSONArray jsonArray = (JSONArray) JSONValue.parse(html);

        if (jsonArray.isEmpty())
            return;

        for (int i = 0; i < jsonArray.size(); i++)
        {
            JSONObject item = (JSONObject) jsonArray.get(i);

            if (item.get("Code") == null || item.get("Exchange") == null || item.get("Name") == null
                            || item.get("Type") == null || item.get("ISIN") == null || item.get("previousClose") == null
                            || item.get("previousCloseDate") == null || item.get("countryName") == null
                            || item.get("currencyCode") == null)
                continue;

            Result result = Result.from(item);
            if (result != null)
                answer.add(result);
        }
    }
}
