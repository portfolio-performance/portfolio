package name.abuchen.portfolio.online.impl;

import static name.abuchen.portfolio.util.TextUtil.trim;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.ClientSettings;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.SecuritySearchProvider;
import name.abuchen.portfolio.util.Isin;
import name.abuchen.portfolio.util.WebAccess;

/**
 * @see https://leeway.tech/api-doc/general?lang=ger&dataapi=true
 * @implNote https://api.leeway.tech/swagger_output.json
 * @apiNote There are only 50 API/day available, so only query with validated
 *          ISIN.
 *          Returns marketplaces which also have historical prices.
 *
 * @formatter:off
 * @array [
 *          {
 *              "Code": "SAP",
 *              "Exchange": "XETRA",
 *              "Name": "SAP SE",
 *              "Type": "Common Stock",
 *              "ISIN": "DE0007164600",
 *              "previousClose": 122.6,
 *              "previousCloseDate": "2023-05-09",
 *              "countryName": "Germany",
 *              "currencyCode": "EUR"
 *          }
 *        ]
 * @formatter:on
 */

public class LeewaySearchProvider implements SecuritySearchProvider
{
    static class Result implements ResultItem
    {
        private String symbol;
        private String exchange;
        private String name;
        private String type;
        private String isin;
        private String currencyCode;

        @SuppressWarnings("nls")
        public static Result from(JSONObject json)
        {
            // Extract values from the JSON object
            String code = (String) json.get("Code");
            String exchange = (String) json.get("Exchange");
            String name = (String) json.get("Name");
            String type = (String) json.get("Type");
            String isin = (String) json.get("ISIN");
            String currencyCode = (String) json.get("currencyCode");

            // Map security types to standard values
            Map<String, String> typeMap = new HashMap<>();
            typeMap.put("common stock", SecuritySearchProvider.Type.SHARE.toString());
            typeMap.put("bond", SecuritySearchProvider.Type.BOND.toString());
            typeMap.put("etf", Messages.LabelSearchETF);
            typeMap.put("etc", Messages.LabelSearchETC);
            typeMap.put("fund", Messages.LabelSearchFund);

            // Convert the security type to a standard value
            type = typeMap.getOrDefault(trim(type.toLowerCase()), "");

            // Combine the symbol and exchange codes to create the security ID
            StringBuilder symbol = new StringBuilder(code);
            symbol.append(".");
            symbol.append(exchange);

            return new Result(isin, symbol.toString(), currencyCode, name, type, exchange);
        }

        public Result(String isin, String symbol, String currencyCode, String name, String type, String exchange)
        {
            this.isin = isin;
            this.symbol = symbol;
            this.name = name;
            this.type = type;
            this.currencyCode = currencyCode;
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
        public String getCurrencyCode()
        {
            return currencyCode;
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
            security.setCurrencyCode(currencyCode);
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

        if (Isin.isValid(query))
            addISINSearchPage(answer, query.trim());

        return answer;
    }

    @SuppressWarnings("nls")
    private void addISINSearchPage(List<ResultItem> answer, String query) throws IOException
    {
        String array = new WebAccess("api.leeway.tech", "/api/v1/public/general/isin/" + query) //
                        .addParameter("apitoken", apiKey) //
                        .get();

        extract(answer, array);
    }

    void extract(List<ResultItem> answer, String array)
    {
        JSONArray jsonArray = (JSONArray) JSONValue.parse(array);

        if (jsonArray.isEmpty())
            return;

        for (Object element : jsonArray)
        {
            JSONObject item = (JSONObject) element;
            answer.add(Result.from(item));
        }
    }
}
