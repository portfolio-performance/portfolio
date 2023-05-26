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
import org.json.simple.JSONValue;

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
 * @see https://cran.r-project.org/web/packages/td/td.pdf
 * @implNote https://twelvedata.com/docs#getting-started
 * @apiNote There are only 800 API/day and 8 API/minute available.
 *          However, we do not need a RateLimiter.
 *          If we have an error, it will be issued, no matter if limit is reached,
 *          API calls are exceeded or the exchange place is not released due to the rate selection (free / paid).
 *          The stock exchanges are created using the four-digit market identifier codes.
 *          These are listed according to ISO-10383.
 *
 * @formatter:off
 * @json {
 *          "meta": {
 *                      "symbol": "SAP",
 *                      "interval": "1day",
 *                      "currency": "USD",
 *                      "exchange_timezone": "America/New_York",
 *                      "exchange": "NASDAQ",
 *                      "mic_code": "XNAS",
 *                      "type": "Common Stock"
 *          },
 *          "values": [
 *                      {
 *                      "datetime": "2021-09-16 15:59:00",
 *                      "open": "148.73500",
 *                      "high": "148.86000",
 *                      "low": "148.73000",
 *                      "close": "148.85001",
 *                      "volume": "624277"
 *                      }
 *                    ]
 *          },
 *          "status": "ok"
 *       }
 * @formatter:on
 */

public class TwelveDataQuoteFeed implements QuoteFeed
{
    private String apiKey;
    private final PageCache<ResponseData> cache = new PageCache<>();

    public static final String ID = "TWELVEDATA"; //$NON-NLS-1$

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
        return "Twelve Data"; //$NON-NLS-1$
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
            quoteStartDate = LocalDate.of(1970, 01, 01);

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
        String securityTickerSymbolWithoutStockMarket = trim(security.getTickerSymbolWithoutStockMarket())
                        .toUpperCase();

        // Extract the exchange from the ticker symbol, if present
        String securityExchange = null;
        if (security.getTickerSymbol().contains(".")) //$NON-NLS-1$
            securityExchange = security.getTickerSymbol().substring(security.getTickerSymbol().indexOf('.') + 1)
                            .toUpperCase();

        if (apiKey == null)
        {
            PortfolioLog.error(Messages.MsgErrorTwelveDataAPIKeyMissing);
            return QuoteFeedData.withError(new IllegalArgumentException(Messages.MsgErrorTwelveDataAPIKeyMissing));
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
            WebAccess webaccess = new WebAccess("api.twelvedata.com", "/time_series") //
                            .addParameter("symbol", securityTickerSymbolWithoutStockMarket) //
                            .addParameter("mic_code", securityExchange) //
                            .addParameter("start_date", quoteStartDate.toString()) //
                            .addParameter("interval", "1day") //
                            .addParameter("apikey", apiKey);

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

            JSONObject json = (JSONObject) JSONValue.parse(response.json);

            // If any error with your plan, the response is
            //
            // @formatter:off
            // { "code":XXX,"message":"error message","status":"error" } // NOSONAR
            // @formatter:on
            if (json != null && !json.isEmpty() && json.containsKey("code") && "error".equals(json.get("status")))
            {
                PortfolioLog.error(MessageFormat.format(json.get("message").toString(), json.get("code").toString()));
                return QuoteFeedData.withError(new IOException(
                                MessageFormat.format(json.get("message").toString(), json.get("code").toString())));
            }

            // Check if the json contains a non-empty JSON array
            if (json != null && !json.isEmpty() && json.containsKey("values") && !"error".equals(json.get("status")))
            {
                if (json.get("values") instanceof JSONArray jsonArray)
                {
                    // cache response if it is a valid non-empty array
                    if (!jsonArray.isEmpty())
                        cache.put(securityTickerSymbol, response);

                    for (Object obj : jsonArray)
                    {
                        JSONObject quoteEntry = (JSONObject) obj;

                        LocalDate date = LocalDate.parse(String.valueOf(quoteEntry.get("datetime")));
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
        catch (IOException | URISyntaxException e)
        {
            data.addError(e);
        }

        return data;
    }

    @Override
    public final List<Exchange> getExchanges(Security subject, List<Exception> errors)
    {
        List<Exchange> answer = new ArrayList<>();

        if (subject.getTickerSymbol() == null)
            return answer;

        // Extract the exchange from the ticker symbol, if present
        String securityExchange = null;
        if (subject.getTickerSymbol().contains(".")) //$NON-NLS-1$
            securityExchange = subject.getTickerSymbol().substring(subject.getTickerSymbol().indexOf('.') + 1)
                            .toUpperCase();

        // Extract the symbol from the ticker symbol without the stock market
        // information
        String symbol = subject.getTickerSymbolWithoutStockMarket();

        if (symbol != null && !symbol.trim().isEmpty())
        {
            // Get a list of all exchange keys
            List<String> exchangeKeys = ExchangeLabels.getAllExchangeKeys("mic-ISO-10383."); //$NON-NLS-1$

            // If a security exchange is specified and it's not present in the
            // exchange keys, add the symbol to the answer list
            if (securityExchange != null && !exchangeKeys.contains(securityExchange))
                answer.add(new Exchange(symbol + "." + securityExchange, //$NON-NLS-1$
                                ExchangeLabels.getString("unknowStockExchange"))); //$NON-NLS-1$

            exchangeKeys.forEach(e -> answer.add(createExchange(symbol.trim().toUpperCase() + "." + e))); //$NON-NLS-1$
        }

        return answer;
    }

    private Exchange createExchange(String symbol)
    {
        int e = symbol.indexOf('.');
        String exchange = e >= 0 ? symbol.substring(e) : ".default"; //$NON-NLS-1$
        String label = ExchangeLabels.getString("mic-ISO-10383" + exchange); //$NON-NLS-1$
        return new Exchange(symbol, label);
    }

    private long asPrice(Object number)
    {
        if (number == null)
            return LatestSecurityPrice.NOT_AVAILABLE;

        if (number instanceof Number n)
            return Values.Quote.factorize(n.doubleValue());

        if (number instanceof String s)
        {
            double parsedValue = Double.parseDouble(s);
            return Values.Quote.factorize(parsedValue);
        }

        throw new IllegalArgumentException(number.getClass().toString());
    }

    private long asNumber(Object number)
    {
        if (number == null)
            return LatestSecurityPrice.NOT_AVAILABLE;

        if (number instanceof Number n)
            return n.longValue();

        if (number instanceof String s)
            return Long.parseLong(s);

        throw new IllegalArgumentException(number.getClass().toString());
    }
}
