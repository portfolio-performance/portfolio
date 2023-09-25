package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.SecuritySearchProvider;
import name.abuchen.portfolio.util.WebAccess;
import name.abuchen.portfolio.util.WebAccess.WebAccessException;

/**
 * <p>
 * Use the <a href="https://eodhistoricaldata.com/">EODHistoricalData.com</a>
 * API to search for securities by name, symbol or ISIN. This API can be used
 * with the free API key that you can obtain by registering on
 * <a href="https://eodhistoricaldata.com/">EODHistoricalData.com</a>.
 * </p>
 * <p>
 * Refer to the <a href=
 * "https://eodhistoricaldata.com/financial-apis/search-api-for-stocks-etfs-mutual-funds-and-indices/">
 * EODHistoricalData.com Search API Documentation</a> for details of the
 * supported parameters and examples.
 * </p>
 * <p>
 * An example of the result from a search for ISIN ES0105336038.
 * 
 * <pre>
 * [
 *     {
 *         "Code": "BBVAI",
 *         "Exchange": "MC",
 *         "Name": "Accion IBEX 35 Cotizado Armonizado FI",
 *         "Type": "ETF",
 *         "Country": "Spain",
 *         "Currency": "EUR",
 *         "ISIN": "ES0105336038",
 *         "previousClose": 8.264,
 *         "previousCloseDate": "2022-07-08"
 *     }
 * ]
 * </pre>
 * </p>
 */
public class EODHistoricalDataSearchProvider implements SecuritySearchProvider
{
    static class Result implements ResultItem
    {
        private String symbol;
        private String name;
        private String type;
        private String exchange;
        private String country;
        private String currency;
        private String isin;
        private String previousClose;
        private String previousCloseDate;
        private LatestSecurityPrice latestSecurityPrice;

        public static Optional<Result> from(JSONObject json)
        {
            // code is the security symbol with no exchange suffix
            var code = json.get("Code"); //$NON-NLS-1$
            if (code == null)
                return Optional.empty();

            var symbol = String.valueOf(code);

            // exchange is a 2-character code that can be appended to the symbol
            var exchange = String.valueOf(json.get("Exchange")); //$NON-NLS-1$
            if (!exchange.isBlank())
            {
                symbol += '.' + exchange;
            }

            var name = json.get("Name"); //$NON-NLS-1$

            var type = json.get("Type"); //$NON-NLS-1$

            var country = json.get("Country"); //$NON-NLS-1$

            var currency = json.get("Currency"); //$NON-NLS-1$

            String isin = (String) json.get("ISIN"); //$NON-NLS-1$

            // Example: 8.264
            var previousClose = json.get("previousClose"); //$NON-NLS-1$
            // Example: "2022-07-08"
            var previousCloseDate = json.get("previousCloseDate"); //$NON-NLS-1$

            LatestSecurityPrice latestSecurityPrice = null;
            if (previousClose != null && previousCloseDate != null)
            {
                try
                {
                    var myPrice = (long) (Values.Quote.divider() * Double.valueOf(String.valueOf(previousClose)));
                    var myCloseDate = LocalDate.parse(String.valueOf(previousCloseDate));
                    latestSecurityPrice = new LatestSecurityPrice(myCloseDate, myPrice);
                }
                catch (DateTimeParseException | NumberFormatException e)
                {
                    // Ignore it and continue without adding a price to the
                    // security
                }
            }

            return Optional.of(new Result(symbol, String.valueOf(name), String.valueOf(type), String.valueOf(exchange),
                            String.valueOf(country), String.valueOf(currency), isin, String.valueOf(previousClose),
                            String.valueOf(previousCloseDate), latestSecurityPrice));
        }

        private Result(String symbol, String name, String type, String exchange, String country, String currency,
                        String isin, String previousClose, String previousCloseDate,
                        LatestSecurityPrice latestSecurityPrice)
        {
            this.symbol = symbol;
            this.name = name;
            this.type = type;
            this.exchange = exchange;
            this.country = country;
            this.currency = currency;
            this.isin = isin;
            this.previousClose = previousClose;
            this.previousCloseDate = previousCloseDate;
            this.latestSecurityPrice = latestSecurityPrice;
        }

        /* package */ Result(String description)
        {
            this.name = description;
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

        public String getCountry()
        {
            return country;
        }

        public String getCurrency()
        {
            return currency;
        }

        public String getPreviousClose()
        {
            return previousClose;
        }

        public String getPreviousCloseDate()
        {
            return previousCloseDate;
        }

        public LatestSecurityPrice getLatestSecurityPrice()
        {
            return latestSecurityPrice;
        }

        @Override
        public Security create(Client client)
        {
            var security = new Security(name, currency);
            security.setIsin(isin);
            security.setTickerSymbol(symbol);
            if (latestSecurityPrice != null)
                security.setLatest(latestSecurityPrice);
            security.setFeed(EODHistoricalDataQuoteFeed.ID);
            return security;
        }
        
        @Override
        public String getSource()
        {
            return NAME;
        }

        @SuppressWarnings("nls")
        @Override
        public String toString()
        {
            var builder = new StringBuilder();
            builder.append("Result [symbol=").append(symbol) //
                            .append(", name=").append(name) //
                            .append(", type=").append(type) //
                            .append(", exchange=").append(exchange) //
                            .append(", country=").append(country) //
                            .append(", currency=").append(currency) //
                            .append(", isin=").append(isin) //
                            .append(", previousClose=").append(previousClose) //
                            .append(", previousCloseDate=").append(previousCloseDate) //
                            .append(", latestSecurityPrice=").append(latestSecurityPrice) //
                            .append("]");
            return builder.toString();
        }
    }

    private static final String NAME = "EODHistoricalData.com"; //$NON-NLS-1$
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
     * If the EODHistoricalData API key is null or blank then an empty
     * <code>List</code> is returned. This prevents
     * <code>401 Unauthorized</code> errors for those users who have not
     * configured EODHistoricalData.
     * </p>
     * <p>
     * If the search query is null or blank then an empty <code>List</code> is
     * returned. This prevents pointless calls to the EODHistoricalData API.
     * </p>
     * 
     * @param query
     *            symbol or ISIN to look up
     * @param type
     *            The type of security
     * @return <code>List</code> of the found securities.
     */
    @Override
    public List<ResultItem> search(String query, Type type) throws IOException
    {
        if (apiKey == null || apiKey.isBlank() || query == null || query.isBlank())
            return Collections.emptyList();

        List<ResultItem> answer = new ArrayList<>();

        addSymbolSearchResults(answer, query, type);

        if (answer.size() >= 10)
        {
            var item = new Result(Messages.MsgMoreResultsAvailable);
            answer.add(item);
        }
        return answer;
    }

    /**
     * Method to encode a string value using `UTF-8` URL encoding scheme.
     */
    private static String urlEncodeQuery(String query)
    {
        try
        {
            return URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
        }
        catch (UnsupportedEncodingException ex)
        {
            PortfolioLog.error(ex);
            return ""; //$NON-NLS-1$
        }
    }

    /**
     * <p>
     * Map the Portfolio Performance (PP) {@link Type} to the {@link String}
     * used by the EODHistoricalData.com Search API.
     * </p>
     * <p>
     * The Search API documentation shows the following values for type:
     * <ul>
     * <li>all</li>
     * <li>stock</li>
     * <li>etf</li>
     * <li>fund</li>
     * <li>bonds</li>
     * <li>index</li>
     * <li>commodity</li>
     * <li>crypto</li>
     * </ul>
     * </p>
     * 
     * @param type
     *            the {@link Type} used by Portfolio Performance
     * @return the type used by the EODHistoricalData.com Search API
     */
    private static String mapPpTypeToEodType(Type type)
    {
        final String eodType;
        switch (type)
        {
            case BOND:
                eodType = "bonds"; //$NON-NLS-1$
                break;
            case SHARE:
                eodType = "stock"; //$NON-NLS-1$
                break;
            case ALL:
                // fall through
            default:
                eodType = "all"; //$NON-NLS-1$
                break;
        }
        return eodType;
    }

    private void addSymbolSearchResults(List<ResultItem> answer, String query, Type type) throws IOException
    {
        try
        {
            @SuppressWarnings("nls")
            final var webAccess = new WebAccess("eodhistoricaldata.com", "api/search/" + urlEncodeQuery(query))
                            .addParameter("api_token", apiKey) //
                            .addParameter("fmt", "json");
            if (type != null)
                webAccess.addParameter("type", mapPpTypeToEodType(type)); //$NON-NLS-1$

            var html = webAccess.get();

            var responseData = (JSONArray) JSONValue.parse(html);
            if (responseData != null)
            {
                for (int i = 0; i < responseData.size(); i++)
                {
                    Result.from((JSONObject) responseData.get(i)).ifPresent(answer::add);
                }
            }
        }
        catch (WebAccessException ex)
        {
            PortfolioLog.error(ex);
        }
    }
}
