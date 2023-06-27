package name.abuchen.portfolio.online.impl;

import static name.abuchen.portfolio.util.TextUtil.trim;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.util.WebAccess;

/**
 * @see https://leeway.tech/api-doc/general?lang=ger&dataapi=true
 * @implNote https://api.leeway.tech/swagger_output.json
 * @apiNote There are only 50 API/day available, so only query with validated
 *          ISIN.
 *
 * Return json for prices
 * @formatter:off
 * @array [
 *          {
 *              "_id":
 *              "645b022702156e5e1b51825a",
 *              "date": "1994-02-01",
 *              "open": 5.0618,
 *              "high": 5.0874,
 *              "low": 4.9979,
 *              "close": 5.0618,
 *              "adjusted_close": 0.7622,
 *              "volume": 807001
 *          }
 *        ]
 * @formatter:on
 *
 * --------------------------------------------------------------------------
 *
 * @apiNote If the exchange market is not present in the ExchangeLabels list, then we insert it.
 *          If we go through the API and make a query of the unknown exchange market, then we get a
 *          response back in the form of an array, but we don't get any results via the new "code".
 *
 * Return json for unknown exchange labels
 * @formatter:off
 * @array [
 *          {
 *              "name": "CREF Equity Index Account - R2",
 *              "code": "QCEQPX",
 *              "exchange": "US",
 *              "type": "Common Stock"
 *          }
 *        ]
 * @formatter:on
 */

public class LeewayQuoteFeed implements QuoteFeed
{
    private String apiKey;
    private final PageCache<ResponseData> cache = new PageCache<>();

    public static final String ID = "LEEWAY"; //$NON-NLS-1$

    private static class ResponseData
    {
        LocalDate quoteStartDate;
        String json;
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "PWP Leeway UG"; //$NON-NLS-1$
    }

    public void setApiKey(String apiKey)
    {
        this.apiKey = apiKey;
    }

    @Override
    public boolean mergeDownloadRequests()
    {
        return true;
    }

    @Override
    public Optional<LatestSecurityPrice> getLatestQuote(Security security)
    {
        List<LatestSecurityPrice> prices = getHistoricalQuotes(security, true, LocalDate.now()).getLatestPrices();
        return prices.isEmpty() ? Optional.empty() : Optional.of(prices.get(prices.size() - 1));
    }

    @Override
    public QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse)
    {
        LocalDate quoteStartDate = null;

        if (!security.getPrices().isEmpty())
            quoteStartDate = security.getPrices().get(security.getPrices().size() - 1).getDate();
        else
            quoteStartDate = LocalDate.of(2000, 01, 01);

        return getHistoricalQuotes(security, collectRawResponse, quoteStartDate);
    }

    @Override
    public QuoteFeedData previewHistoricalQuotes(Security security)
    {
        return getHistoricalQuotes(security, true, LocalDate.now().minusMonths(2));
    }

    @SuppressWarnings("nls")
    public QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse, LocalDate quoteStartDate)
    {
        String securityTickerSymbol = trim(security.getTickerSymbol()).toUpperCase();

        if (apiKey == null || apiKey.isBlank())
        {
            PortfolioLog.error(Messages.MsgErrorLeewayAPIKeyMissing);
            return QuoteFeedData.withError(new IllegalArgumentException(Messages.MsgErrorLeewayAPIKeyMissing));
        }

        if (securityTickerSymbol == null || securityTickerSymbol.isEmpty())
        {
            PortfolioLog.error(MessageFormat.format(Messages.MsgMissingTickerSymbol, security.getName()));
            return QuoteFeedData.withError(
                            new IOException(MessageFormat.format(Messages.MsgMissingTickerSymbol, security.getName())));
        }

        QuoteFeedData data = new QuoteFeedData();

        try
        {
            WebAccess webaccess = new WebAccess("api.leeway.tech", //
                            "/api/v1/public/historicalquotes/" + securityTickerSymbol) //
                                            .addParameter("apitoken", apiKey) //
                                            .addParameter("from", quoteStartDate.toString());

            // Lookup cached response for securityTickerSymbol
            ResponseData response = cache.lookup(securityTickerSymbol);

            // Check if cached response is null or if it has an earlier
            // quoteStartDate than the requested quoteStartDate
            if (response == null || response.quoteStartDate.isAfter(quoteStartDate))
            {
                response = new ResponseData();
                response.quoteStartDate = quoteStartDate;
                response.json = webaccess.get();
            }

            if (collectRawResponse)
                data.addResponse(webaccess.getURL(), response.json);

            // Check if the response.array contains a non-empty JSON array
            if (response.json != null && !response.json.isEmpty())
            {
                Object jsonResponse = new JSONParser().parse(response.json);

                if (jsonResponse instanceof JSONArray jsonArray)
                {
                    // If the symbol is unknown, the response is
                    //
                    // @formatter:off
                    // { "msg": "No quotes found for FOO.XETRA" } // NOSONAR
                    // @formatter:on

                    // cache response if it is a valid non-empty array
                    if (!jsonArray.isEmpty())
                        cache.put(securityTickerSymbol, response);

                    for (Object obj : jsonArray)
                    {
                        JSONObject quoteEntry = (JSONObject) obj;

                        LocalDate date = LocalDate.parse(String.valueOf(quoteEntry.get("date")));
                        long high = asPrice(quoteEntry.get("high"));
                        long low = asPrice(quoteEntry.get("low"));
                        long close = asPrice(quoteEntry.get("close"));
                        long volume = asNumber(quoteEntry.get("volume"));

                        LatestSecurityPrice price = new LatestSecurityPrice();

                        price.setDate(date);
                        price.setHigh(high);
                        price.setLow(low);
                        price.setValue(close);
                        price.setVolume(volume);

                        if (date != null && close > 0L)
                            data.addPrice(price);
                    }
                }
            }
        }
        catch (IOException | URISyntaxException | ParseException e)
        {
            data.addError(e);
        }

        return data;
    }

    @Override
    public final List<Exchange> getExchanges(Security subject, List<Exception> errors)
    {
        List<Exchange> answer = new ArrayList<>();

        String tickerSymbol = subject.getTickerSymbol();
        if (tickerSymbol == null)
            return answer;

        // Extract the exchange from the ticker symbol, if present
        int p = tickerSymbol.indexOf('.');
        String securityExchange = p >= 0 ? tickerSymbol.substring(p + 1).toUpperCase() : null;

        // Extract the symbol from the ticker symbol without the stock market
        // information
        String symbol = p >= 0 ? tickerSymbol.substring(0, p).trim().toUpperCase() : tickerSymbol.trim().toUpperCase();

        if (!symbol.trim().isEmpty())
        {
            // Get a list of all exchange keys
            List<String> exchangeKeys = ExchangeLabels.getAllExchangeKeys("leeway."); //$NON-NLS-1$

            // If a security exchange is specified and it's not present in the
            // exchange keys, add the symbol to the answer list
            if (securityExchange != null && !exchangeKeys.contains(securityExchange))
                answer.add(createExchange(symbol + "." + securityExchange)); //$NON-NLS-1$

            exchangeKeys.forEach(e -> answer.add(createExchange(symbol + "." + e))); //$NON-NLS-1$
        }

        return answer;
    }

    private Exchange createExchange(String symbol)
    {
        int e = symbol.indexOf('.');
        String exchange = e >= 0 ? symbol.substring(e) : ".default"; //$NON-NLS-1$
        String label = ExchangeLabels.getString("leeway" + exchange); //$NON-NLS-1$
        return new Exchange(symbol, label);
    }

    private long asPrice(Object number)
    {
        if (number == null)
            return LatestSecurityPrice.NOT_AVAILABLE;

        if (number instanceof Number n)
            return Values.Quote.factorize(n.doubleValue());

        throw new IllegalArgumentException(number.getClass().toString());
    }

    private long asNumber(Object number)
    {
        if (number == null)
            return LatestSecurityPrice.NOT_AVAILABLE;

        if (number instanceof Number n)
            return n.longValue();

        throw new IllegalArgumentException(number.getClass().toString());
    }
}
