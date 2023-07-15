package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.SecuritySearchProvider;
import name.abuchen.portfolio.util.WebAccess;
import name.abuchen.portfolio.util.WebAccess.WebAccessException;

/**
 * Use the <a href="https://finnhub.io/">Finnhub</a> API to search for
 * securities by symbol or ISIN. This API can be used with the free API key that
 * you can obtain by registering on <a href="https://finnhub.io/>FinnHub.io</a>.
 */
public class FinnhubSearchProvider implements SecuritySearchProvider
{
    static class Result implements ResultItem
    {
        private String symbol;
        private String description;
        private String type;

        public static Optional<Result> from(JSONObject json)
        {
            Object symbol = json.get("symbol"); //$NON-NLS-1$
            if (symbol == null)
                return Optional.empty();

            Object description = json.get("description"); //$NON-NLS-1$

            Object type = json.get("type"); //$NON-NLS-1$

            return Optional.of(new Result(String.valueOf(symbol), String.valueOf(description), String.valueOf(type)));
        }

        private Result(String symbol, String description, String type)
        {
            this.symbol = symbol;
            this.description = description;
            this.type = type;
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
        public String getSource()
        {
            return NAME;
        }

        @Override
        public Security create(Client client)
        {
            Security security = new Security(description, client.getBaseCurrency());
            security.setName(description);
            security.setTickerSymbol(symbol);
            security.setFeed(FinnhubQuoteFeed.ID);
            return security;
        }
    }

    private static final String NAME = "Finnhub"; //$NON-NLS-1$
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

    /**
     * <p>
     * Search for the symbol or ISIN provided in the <code>query</code>
     * parameter. The <code>type</code> parameter is not used.
     * </p>
     * <p>
     * If the FinnHub API key is null or blank then an empty <code>List</code>
     * is returned. This prevents <code>401 Unauthorized</code> errors for those
     * users who have not configured FinnHub.
     * </p>
     * 
     * @param query
     *            symbol or ISIN to look up
     * @param type
     *            not used
     * @return <code>List</code> of the found securities.
     */
    @Override
    public List<ResultItem> search(String query, Type type) throws IOException
    {
        if (apiKey == null || apiKey.isBlank())
            return Collections.emptyList();

        List<ResultItem> answer = new ArrayList<>();

        addSymbolSearchResults(answer, query);

        if (answer.size() >= 10)
        {
            Result item = new Result(Messages.MsgMoreResultsAvailable);
            answer.add(item);
        }

        return answer;
    }

    private void addSymbolSearchResults(List<ResultItem> answer, String query) throws IOException
    {
        try
        {
            @SuppressWarnings("nls")
            String html = new WebAccess("finnhub.io", "api/v1/search").addParameter("q", query)
                            .addHeader("X-Finnhub-Token", apiKey).get();

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
    }
}
