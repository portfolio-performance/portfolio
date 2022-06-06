package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.ClientSettings;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;
import name.abuchen.portfolio.util.WebAccess;
import name.abuchen.portfolio.util.WebAccess.WebAccessException;

/**
 * Use the <a href="https://finnhub.io/docs/api/symbol-search">Finnhub Symbol Lookup</a> API 
 * to search for securities by symbol or ISIN. This API can be used with the free API key that
 * you can obtain by registering on <a href="https://finnhub.io/>FinnHub.io</a>.
 * 
 * @see FinnhubSearchProvider
 */
public class FinnhubSymbolSearch
{
    private String apiKey;

    public FinnhubSymbolSearch(String apiKey)
    {
        this.apiKey = apiKey;
    }

    public void setApiKey(String apiKey)
    {
        this.apiKey = apiKey;
    }

    static class Result implements ResultItem
    {
        private String symbol;
        private String description;
        private String type;
        private String displaySymbol;

        public static Optional<Result> from(JSONObject json)
        {
            Object symbol = json.get("symbol"); //$NON-NLS-1$
            if (symbol == null)
                return Optional.empty();

            Object description = json.get("description"); //$NON-NLS-1$

            Object type = json.get("type"); //$NON-NLS-1$

            Object displaySymbol = json.get("displaySymbol"); //$NON-NLS-1$

            return Optional.of(new Result(String.valueOf(symbol), String.valueOf(description), String.valueOf(type),
                            String.valueOf(displaySymbol)));
        }

        private Result(String symbol, String description, String type, String displaySymbol)
        {
            this.symbol = symbol;
            this.description = description;
            this.type = type;
            this.displaySymbol = displaySymbol;
        }

        /* package */ Result(String description)
        {
            this.description = description;
        }

        @Override
        public String getSymbol()
        {
            return symbol;
        }

        @Override
        public String getName()
        {
            return description;
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
        public Security create(ClientSettings settings)
        {
            Security security = new Security();
            security.setName(description);
            security.setTickerSymbol(symbol);
            security.setFeed(FinnhubQuoteFeed.ID);
            return security;
        }

        @SuppressWarnings("nls")
        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder();
            builder.append("Result [symbol=").append(symbol)
                .append(", description=").append(description)
                .append(", type=").append(type)
                .append(", displaySymbol=").append(displaySymbol)
                .append(']');
            return builder.toString();
        }
    }

    // https://finnhub.io/api/v1/search?q=
    public Stream<Result> search(String query) throws IOException
    {
        List<Result> answer = new ArrayList<>();

        try
        {
            @SuppressWarnings("nls")
            String html = new WebAccess("finnhub.io", "api/v1/search")
                            .addParameter("q", query)
                            .addHeader("X-Finnhub-Token", apiKey)
                            .get();

            JSONObject responseData = (JSONObject) JSONValue.parse(html);
            if (responseData != null)
            {
                JSONArray result = (JSONArray) responseData.get("result"); //$NON-NLS-1$
                if (result != null)
                {
                    for (int ii = 0; ii < result.size(); ii++)
                    {
                        Result.from((JSONObject) result.get(ii)).ifPresent(answer::add);
                    }
                }
            }
        }
        catch (WebAccessException ex)
        {
            PortfolioLog.error(ex);
        }

        return answer.stream();
    }
}
