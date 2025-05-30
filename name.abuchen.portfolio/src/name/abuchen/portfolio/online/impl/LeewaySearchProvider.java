package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import name.abuchen.portfolio.model.Client;
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

        private List<ResultItem> markets = new ArrayList<>();

        @SuppressWarnings("nls")
        public static Result from(JSONObject json)
        {
            // Extract values from the JSON object
            var tickerSymbol = (String) json.get("Code");
            var exchange = (String) json.get("Exchange");
            var name = (String) json.get("Name");
            var type = (String) json.get("Type");
            var isin = (String) json.get("ISIN");
            var currencyCode = (String) json.get("currencyCode");

            // Convert the security type using the SecuritySearchProvider
            // instance
            type = SecuritySearchProvider.convertType(type);

            // Combine the symbol and exchange codes to create the security ID
            var symbol = new StringBuilder(tickerSymbol);
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
            return markets.isEmpty() ? currencyCode
                            : markets.stream().map(e -> e.getCurrencyCode()).distinct()
                                            .collect(Collectors.joining(", ")); //$NON-NLS-1$
        }

        @Override
        public String getSource()
        {
            return NAME;
        }

        @Override
        public String getFeedId()
        {
            return LeewayQuoteFeed.ID;
        }

        @Override
        public boolean hasPrices()
        {
            return true;
        }

        @Override
        public List<ResultItem> getMarkets()
        {
            return markets;
        }

        Result copy()
        {
            return new Result(isin, symbol, currencyCode, name, type, exchange);
        }

        @Override
        public Security create(Client client)
        {
            var security = new Security(name, currencyCode);
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
    public List<ResultItem> search(String query) throws IOException
    {
        List<ResultItem> answer = new ArrayList<>();

        if (apiKey != null && !apiKey.isBlank() && Isin.isValid(query))
            addISINSearchPage(answer, query.trim());

        return answer;
    }

    @SuppressWarnings("nls")
    private void addISINSearchPage(List<ResultItem> answer, String query) throws IOException
    {
        var array = new WebAccess("api.leeway.tech", "/api/v1/public/general/isin/" + query) //
                        .addParameter("apitoken", apiKey) //
                        .get();

        var result = extract(array);

        // group by ISIN (= return with a list of markets)

        var isin2result = new HashMap<String, Result>();

        for (var item : result)
        {
            var isin = item.getIsin();
            if (isin == null || isin.isBlank())
            {
                answer.add(item);
            }
            else
            {
                var grouped = isin2result.get(isin);

                if (grouped != null)
                {
                    grouped.markets.add(item);
                }
                else
                {
                    grouped = item.copy();
                    isin2result.put(isin, grouped);
                    grouped.markets.add(item);
                    answer.add(grouped);
                }
            }
        }
    }

    private List<Result> extract(String array)
    {
        var jsonArray = (JSONArray) JSONValue.parse(array);

        if (jsonArray.isEmpty())
            return Collections.emptyList();

        List<Result> answer = new ArrayList<>();
        for (Object element : jsonArray)
        {
            var item = (JSONObject) element;
            answer.add(Result.from(item));
        }
        return answer;
    }
}
