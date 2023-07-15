package name.abuchen.portfolio.online.impl;

import static name.abuchen.portfolio.util.TextUtil.trim;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.SecuritySearchProvider;
import name.abuchen.portfolio.util.WebAccess;

/**
 * @implNote https://twelvedata.com/docs#getting-started
 * @apiNote There are only 800 API/day and 8 API/minute available.
 *          The stock exchanges are created using the four-digit market identifier codes. 
 *          These are listed according to ISO-10383.
 *
 * @formatter:off
 * @json {
 *          "data": [
 *                      {
 *                      "symbol": "SAP",
 *                      "instrument_name": "SAP SE",
 *                      "exchange": "XETR",
 *                      "mic_code": "XETR",
 *                      "exchange_timezone": "Europe/Berlin",
 *                      "instrument_type": "Common Stock",
 *                      "country": "Germany",
 *                      "currency": "EUR"
 *                      }
 *              ]
 *         "status": "ok"
 *      }
 * @formatter:on
 */

public class TwelveDataSearchProvider implements SecuritySearchProvider
{
    static class Result implements ResultItem
    {
        private String symbol;
        private String name;
        private String exchange;
        private String type;
        private String currencyCode;

        @SuppressWarnings("nls")
        public static Result from(JSONObject json)
        {
            // Extract values from the JSON object
            String tickerSymbol = (String) json.get("symbol");
            String name = (String) json.get("instrument_name");
            String exchange = (String) json.get("mic_code");
            String type = (String) json.get("instrument_type");
            String currencyCode = (String) json.get("currency");

            // Convert the security type using the SecuritySearchProvider
            // instance
            type = SecuritySearchProvider.convertType(trim(type.toLowerCase()));

            // Combine the symbol and exchange codes to create the security ID
            StringBuilder symbol = new StringBuilder(tickerSymbol);
            symbol.append(".");
            symbol.append(exchange);

            return new Result(symbol.toString(), name, exchange, type, currencyCode);
        }

        public Result(String symbol, String name, String exchange, String type, String currencyCode)
        {
            this.symbol = symbol;
            this.name = name;
            this.exchange = exchange;
            this.type = type;
            this.currencyCode = currencyCode;
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
            return null;
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
        public Security create(Client client)
        {
            Security security = new Security(name, currencyCode);
            security.setTickerSymbol(symbol);
            security.setFeed(TwelveDataQuoteFeed.ID);
            return security;
        }
    }

    private static final String NAME = "Twelve Data"; //$NON-NLS-1$
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

        if (apiKey != null && !apiKey.isBlank())
            addStockSearchPage(answer, query.trim());

        return answer;
    }

    @SuppressWarnings("nls")
    private void addStockSearchPage(List<ResultItem> answer, String query) throws IOException
    {
        String json = new WebAccess("api.twelvedata.com", "/symbol_search") //
                        .addParameter("apikey", apiKey) //
                        .addParameter("symbol", query) //
                        .get();

        extract(answer, json);
    }

    void extract(List<ResultItem> answer, String json)
    {
        JSONObject jsonObject = (JSONObject) JSONValue.parse(json);

        if (jsonObject.isEmpty())
            return;

        JSONArray jsonArray = (JSONArray) jsonObject.get("data"); //$NON-NLS-1$

        if (jsonArray == null || jsonArray.isEmpty())
            return;

        for (Object element : jsonArray)
        {
            JSONObject item = (JSONObject) element;
            answer.add(Result.from(item));
        }
    }
}
