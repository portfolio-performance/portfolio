package name.abuchen.portfolio.online.impl;

import static name.abuchen.portfolio.util.TextUtil.trim;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.apache.hc.core5.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.google.common.util.concurrent.RateLimiter;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.FeedConfigurationException;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.online.QuoteFeedException;
import name.abuchen.portfolio.online.RateLimitExceededException;
import name.abuchen.portfolio.util.WebAccess;
import name.abuchen.portfolio.util.WebAccess.WebAccessException;

/**
 * Load historical and latest prices from Sifting.
 *
 * @implNote https://sifting.io/docs - the OpenAPI specification is available at
 *           https://sifting.io/openapi.yaml
 * @apiNote The instrument is identified by the plain ticker symbol. The asset
 *          class is part of the path and therefore stored separately as a feed
 *          property. Prices are not adjusted for splits or dividends and the
 *          response does not indicate a currency.
 *
 * @formatter:off
 * @json {
 *          "data": [
 *                      {
 *                      "t": 1789084800000,
 *                      "o": 76574.15,
 *                      "h": 79893.15,
 *                      "l": 76070.15,
 *                      "c": 77225.705,
 *                      "v": 8013.5065939599945
 *                      }
 *                  ],
 *          "meta": {
 *                      "as_of": "2026-09-12T13:09:38Z",
 *                      "next_cursor": "U0ZUQ1IxfDE1NzgxODI0MDAwMDA",
 *                      "symbol": "BTCUSD",
 *                      "interval": "1d"
 *          }
 *       }
 * @formatter:on
 */
public class SiftingQuoteFeed implements QuoteFeed
{
    /**
     * The asset class that Sifting expects as part of the request path. It is
     * stored as a feed property and not as part of the ticker symbol: the
     * ticker symbol is shared with all other quote feeds and must remain usable
     * when the user switches the quote feed provider.
     */
    public enum AssetClass
    {
        STOCKS("stocks"), //$NON-NLS-1$
        FOREX("forex"), //$NON-NLS-1$
        CRYPTO("crypto"), //$NON-NLS-1$
        COMMODITIES("commodities"); //$NON-NLS-1$

        private final String pathSegment;

        private AssetClass(String pathSegment)
        {
            this.pathSegment = pathSegment;
        }

        public String getPathSegment()
        {
            return pathSegment;
        }

        public String getLabel()
        {
            return switch (this)
            {
                case STOCKS -> Messages.LabelSearchShare;
                case FOREX -> Messages.LabelSearchCurrency;
                case CRYPTO -> Messages.LabelSearchCryptoCurrency;
                case COMMODITIES -> Messages.LabelSearchCommodity;
            };
        }

        /**
         * Returns the asset class for the given path segment. Falls back to
         * equities if the property is not set.
         */
        public static AssetClass of(String pathSegment)
        {
            if (pathSegment != null)
            {
                for (AssetClass assetClass : values())
                {
                    if (assetClass.pathSegment.equalsIgnoreCase(pathSegment))
                        return assetClass;
                }
            }

            return STOCKS;
        }
    }

    private static class Page
    {
        final String url;
        final String json;

        Page(String url, String json)
        {
            this.url = url;
            this.json = json;
        }
    }

    private static class ResponseData
    {
        LocalDate start;
        List<Page> pages = new ArrayList<>();

        /**
         * True if all pages have been retrieved. An incomplete answer - for
         * example because a request ran into a timeout - must not be cached:
         * otherwise the missing pages are not retrieved again.
         */
        boolean complete;
    }

    public static final String ID = "SIFTING"; //$NON-NLS-1$
    public static final String SIFTING_ASSET_CLASS = "SIFTINGASSETCLASS"; //$NON-NLS-1$

    private static final String HOST = "api.sifting.io"; //$NON-NLS-1$

    /**
     * Sifting supports daily data back to the year 2000.
     */
    private static final LocalDate DEFAULT_START = LocalDate.of(2000, Month.JANUARY, 1);

    private static final int PAGE_SIZE = 1000;

    /**
     * The length of the period of one request. Must be smaller than the page
     * size so that a period is never cut off by the limit.
     */
    private static final int WINDOW_DAYS = 900;

    /**
     * Guards against an endless loop while moving through the periods.
     */
    private static final int MAX_PAGES = 50;

    /**
     * The free plan permits 5 requests per second. Stay below that limit.
     */
    private static final double RATE_LIMIT = 4.0;

    /**
     * The API answers with a 502 or runs into a timeout if it has to calculate
     * the answer. A second attempt is usually answered from the cache of the
     * server.
     */
    private static final int REQUEST_ATTEMPTS = 2;

    private static final Duration PAUSE_BETWEEN_ATTEMPTS = Duration.ofSeconds(2);

    private String apiKey;

    private final RateLimiter rateLimiter = RateLimiter.create(RATE_LIMIT);

    private final PageCache<ResponseData> cache = new PageCache<>();

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "Sifting"; //$NON-NLS-1$
    }

    public void setApiKey(String apiKey)
    {
        this.apiKey = apiKey;
    }

    @Override
    public Optional<String> getHelpURL()
    {
        return Optional.of("https://sifting.io/docs"); //$NON-NLS-1$
    }

    @Override
    public int getMaxRateLimitAttempts()
    {
        return 10;
    }

    @Override
    public Optional<LatestSecurityPrice> getLatestQuote(Security security) throws QuoteFeedException
    {
        // The daily bars include the (still incomplete) bar of the current day.
        // Deliberately no request to /v1/last/... because those endpoints
        // return values that differ from the daily bars and would create an
        // inconsistency between the latest price and the historical price of
        // the very same day.

        var data = getHistoricalQuotes(security, false, LocalDate.now().minusDays(7));

        if (!data.getErrors().isEmpty())
            PortfolioLog.abbreviated(data.getErrors());

        return data.getLatestPrices().stream().max(Comparator.comparing(SecurityPrice::getDate));
    }

    @Override
    public QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse) throws QuoteFeedException
    {
        var start = DEFAULT_START;

        if (!security.getPrices().isEmpty())
            start = security.getPrices().get(security.getPrices().size() - 1).getDate();

        return getHistoricalQuotes(security, collectRawResponse, start);
    }

    @Override
    public QuoteFeedData previewHistoricalQuotes(Security security) throws QuoteFeedException
    {
        return getHistoricalQuotes(security, true, LocalDate.now().minusMonths(2));
    }

    public QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse, LocalDate start)
                    throws QuoteFeedException
    {
        if (apiKey == null || apiKey.isBlank())
            return QuoteFeedData.withError(new IllegalArgumentException(Messages.MsgErrorMissingAPIKey));

        var symbol = trim(security.getTickerSymbol());
        if (symbol == null || symbol.isEmpty())
            return QuoteFeedData.withError(
                            new IOException(MessageFormat.format(Messages.MsgMissingTickerSymbol, security.getName())));
        symbol = symbol.toUpperCase();

        var assetClass = AssetClass
                        .of(security.getPropertyValue(SecurityProperty.Type.FEED, SIFTING_ASSET_CLASS).orElse(null));

        var data = new QuoteFeedData();

        var cacheKey = assetClass.getPathSegment() + '/' + symbol;

        // reuse the cached response only if it covers the requested period
        var response = cache.lookup(cacheKey);
        if (response == null || response.start.isAfter(start))
        {
            response = load(symbol, assetClass, start, data);

            if (response.complete && !response.pages.isEmpty())
                cache.put(cacheKey, response);
        }

        for (var page : response.pages)
        {
            if (collectRawResponse)
                data.addResponse(page.url, page.json);

            extract(page.json, data);
        }

        return data;
    }

    @SuppressWarnings("nls")
    private ResponseData load(String symbol, AssetClass assetClass, LocalDate start, QuoteFeedData data)
                    throws QuoteFeedException
    {
        var response = new ResponseData();
        response.start = start;

        var path = "/v1/hist/" + assetClass.getPathSegment() + "/" + symbol + "/bars";

        // The API does paginate with a cursor, but a request with a cursor is
        // answered with a 502 after 30 seconds. Requests for a bounded period
        // are answered reliably instead. Therefore ask for one period after the
        // other. The end date is inclusive.

        var today = LocalDate.now();
        var from = start.isAfter(today) ? today : start;

        for (int page = 0; page < MAX_PAGES; page++)
        {
            LocalDate to = from.plusDays(WINDOW_DAYS);

            var isLastPeriod = !to.isBefore(today);
            if (isLastPeriod)
                to = today;

            var webaccess = new WebAccess(HOST, path) //
                            .addParameter("interval", "1d") //
                            .addParameter("start", from.toString()) //
                            .addParameter("end", to.toString()) //
                            .addParameter("limit", String.valueOf(PAGE_SIZE)) //
                            .addHeader("X-API-Key", apiKey);

            var json = request(webaccess, data);
            if (json == null)
                break;

            try
            {
                response.pages.add(new Page(webaccess.getURL(), json));
            }
            catch (URISyntaxException e)
            {
                data.addError(e);
                break;
            }

            // if the answer is filled up to the limit, then the period was cut
            // off. Continue after the last bar that was delivered
            var last = extractLastDate(json);
            if (countDataRows(json) >= PAGE_SIZE && last != null && last.isAfter(from))
            {
                from = last.plusDays(1);
                continue;
            }

            if (isLastPeriod)
            {
                response.complete = true;
                break;
            }

            from = to.plusDays(1);
        }

        return response;
    }

    /**
     * Executes the request and returns the answer. Returns null - and attaches
     * an error to the given data - if the answer cannot be retrieved.
     */
    private String request(WebAccess webaccess, QuoteFeedData data) throws QuoteFeedException
    {
        for (int attempt = 1; attempt <= REQUEST_ATTEMPTS; attempt++)
        {
            try
            {
                if (!rateLimiter.tryAcquire(Duration.ofSeconds(30)))
                    throw new RateLimitExceededException(Duration.ofSeconds(10),
                                    MessageFormat.format(Messages.MsgRateLimitExceeded, getName()));

                return webaccess.get();
            }
            catch (WebAccessException e)
            {
                if (e.getHttpErrorCode() == HttpStatus.SC_TOO_MANY_REQUESTS)
                    throw new RateLimitExceededException(extractRetryAfter(e),
                                    MessageFormat.format(Messages.MsgRateLimitExceeded, getName()));

                // Sifting checks the entitlement before it checks the symbol.
                // Only if the error names the feature we know that it is the
                // plan of the user - and not a typo in the ticker symbol - that
                // prevents the download.
                if (e.getHttpErrorCode() == HttpStatus.SC_FORBIDDEN && e.getBody() != null
                                && e.getBody().contains("\"feature\"")) //$NON-NLS-1$
                    throw new FeedConfigurationException(extractErrorMessage(e));

                if (attempt == REQUEST_ATTEMPTS || !isTemporary(e.getHttpErrorCode()))
                {
                    data.addError(e);
                    return null;
                }
            }
            catch (IOException e)
            {
                // for example a timeout
                if (attempt == REQUEST_ATTEMPTS)
                {
                    data.addError(e);
                    return null;
                }
            }

            try
            {
                Thread.sleep(PAUSE_BETWEEN_ATTEMPTS.toMillis());
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        return null;
    }

    private boolean isTemporary(int httpErrorCode)
    {
        return httpErrorCode == HttpStatus.SC_BAD_GATEWAY //
                        || httpErrorCode == HttpStatus.SC_SERVICE_UNAVAILABLE //
                        || httpErrorCode == HttpStatus.SC_GATEWAY_TIMEOUT;
    }

    /* package */ void extract(String json, QuoteFeedData data)
    {
        JSONObject response = (JSONObject) JSONValue.parse(json);
        if (response == null)
            return;

        if (response.get("data") instanceof JSONArray bars) //$NON-NLS-1$
        {
            for (Object element : bars)
            {
                if (!(element instanceof JSONObject bar))
                    continue;

                var date = asDate(bar.get("t")); //$NON-NLS-1$
                var close = asPrice(bar.get("c")); //$NON-NLS-1$

                if (date == null || close <= 0L)
                    continue;

                var price = new LatestSecurityPrice();
                price.setDate(date);
                price.setValue(close);
                price.setHigh(asPrice(bar.get("h"))); //$NON-NLS-1$
                price.setLow(asPrice(bar.get("l"))); //$NON-NLS-1$
                price.setVolume(asNumber(bar.get("v"))); //$NON-NLS-1$

                data.addPrice(price);
            }
        }
    }

    /**
     * Returns the number of bars in the answer - including bars that are
     * skipped because they have no closing price. A page that is not filled
     * completely indicates that no further data is available.
     */
    /* package */ int countDataRows(String json)
    {
        var response = (JSONObject) JSONValue.parse(json);

        if (response != null && response.get("data") instanceof JSONArray bars) //$NON-NLS-1$
            return bars.size();

        return 0;
    }

    /**
     * Returns the date of the last bar of the answer. The bars are sorted in
     * ascending order.
     */
    /* package */ LocalDate extractLastDate(String json)
    {
        var response = (JSONObject) JSONValue.parse(json);

        if (response != null && response.get("data") instanceof JSONArray bars && !bars.isEmpty() //$NON-NLS-1$
                        && bars.get(bars.size() - 1) instanceof JSONObject bar)
            return asDate(bar.get("t")); //$NON-NLS-1$

        return null;
    }

    private String extractErrorMessage(WebAccessException e)
    {
        Object response = JSONValue.parse(e.getBody());

        if (response instanceof JSONObject json && json.get("error") != null) //$NON-NLS-1$
            return String.valueOf(json.get("error")); //$NON-NLS-1$

        return e.getMessage();
    }

    private Duration extractRetryAfter(WebAccessException e)
    {
        Object response = JSONValue.parse(e.getBody());

        if (response instanceof JSONObject json && json.get("retry_after") instanceof Number seconds) //$NON-NLS-1$
            return Duration.ofSeconds(seconds.longValue());

        return Duration.ofSeconds(30);
    }

    private LocalDate asDate(Object number)
    {
        if (number instanceof Number n)
            return Instant.ofEpochMilli(n.longValue()).atZone(ZoneOffset.UTC).toLocalDate();

        return null;
    }

    private long asPrice(Object number)
    {
        if (number == null)
            return LatestSecurityPrice.NOT_AVAILABLE;

        if (number instanceof Number n)
            return Values.Quote.factorize(n.doubleValue());

        if (number instanceof String s)
            return Values.Quote.factorize(Double.parseDouble(s));

        throw new IllegalArgumentException(number.getClass().toString());
    }

    private long asNumber(Object number)
    {
        if (number == null)
            return LatestSecurityPrice.NOT_AVAILABLE;

        if (number instanceof Number n)
            return n.longValue();

        if (number instanceof String s)
            return (long) Double.parseDouble(s);

        throw new IllegalArgumentException(number.getClass().toString());
    }
}
