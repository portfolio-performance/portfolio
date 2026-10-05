package name.abuchen.portfolio.online.impl;

import static name.abuchen.portfolio.util.TextUtil.trim;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.hc.core5.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.online.QuoteFeedException;
import name.abuchen.portfolio.online.RateLimitExceededException;
import name.abuchen.portfolio.util.WebAccess;
import name.abuchen.portfolio.util.WebAccess.WebAccessException;

/**
 * Load daily exchange rates from FXMacroData.
 *
 * @implNote https://fxmacrodata.com/documentation/reference - the OpenAPI
 *           specification is available at
 *           https://api.fxmacrodata.com/openapi.json
 * @apiNote The instrument is identified by a currency pair as ticker symbol,
 *          for example EUR/USD, EURUSD or EUR-USD. The rates are official
 *          reference rates (one value per day). The API key is sent in the
 *          X-API-Key header. The rows are returned most recent first and are
 *          paginated with an offset.
 *
 * @formatter:off
 * @json {
 *          "base": "USD",
 *          "quote": "JPY",
 *          "start_date": "2026-06-17",
 *          "end_date": "2026-06-18",
 *          "pagination": {
 *                      "limit": 100,
 *                      "offset": 0,
 *                      "returned_count": 2,
 *                      "total_count": 2,
 *                      "has_more": false,
 *                      "next_offset": null
 *          },
 *          "data": [
 *                      {
 *                      "date": "2026-06-18",
 *                      "val": 161.1288
 *                      },
 *                      {
 *                      "date": "2026-06-17",
 *                      "val": 161.1288
 *                      }
 *                  ]
 *       }
 * @formatter:on
 */
public class FXMacroDataQuoteFeed implements QuoteFeed
{
    /**
     * A currency pair parsed from the ticker symbol.
     */
    /* package */ record CurrencyPair(String base, String quote)
    {
        // Accepts EUR/USD, EURUSD, EUR-USD, EUR_USD and EUR USD as well as the
        // Yahoo notation EURUSD=X so that the ticker symbol remains usable
        // when switching between quote feeds.
        private static final Pattern PATTERN = Pattern.compile("^([A-Z]{3})\\s*[/\\-_]?\\s*([A-Z]{3})(=X)?$"); //$NON-NLS-1$

        /* package */ static Optional<CurrencyPair> parse(String tickerSymbol)
        {
            var symbol = trim(tickerSymbol);
            if (symbol == null || symbol.isEmpty())
                return Optional.empty();

            var matcher = PATTERN.matcher(symbol.toUpperCase(Locale.ROOT));
            if (!matcher.matches())
                return Optional.empty();

            var base = matcher.group(1);
            var quote = matcher.group(2);

            if (base.equals(quote))
                return Optional.empty();

            return Optional.of(new CurrencyPair(base, quote));
        }

        /* package */ String path()
        {
            return "/v1/forex/" + base + "/" + quote; //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    public static final String ID = "FXMACRODATA"; //$NON-NLS-1$

    /* package */ static final String HOST = "api.fxmacrodata.com"; //$NON-NLS-1$

    /**
     * The maximum number of rows the API returns per request.
     */
    /* package */ static final int PAGE_SIZE = 100;

    /**
     * Guards against an endless loop while moving through the pages.
     */
    private static final int MAX_PAGES = 100;

    /**
     * Number of years loaded if the security does not have any prices yet.
     */
    private static final int DEFAULT_YEARS = 10;

    private String apiKey;

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "FXMacroData"; //$NON-NLS-1$
    }

    public void setApiKey(String apiKey)
    {
        this.apiKey = apiKey;
    }

    @Override
    public Optional<String> getHelpURL()
    {
        return Optional.of("https://fxmacrodata.com/documentation/reference"); //$NON-NLS-1$
    }

    @Override
    public Optional<LatestSecurityPrice> getLatestQuote(Security security) throws QuoteFeedException
    {
        // reference rates are published once per day. The most recent row is
        // the first row of the answer
        var data = getHistoricalQuotes(security, false, LocalDate.now().minusDays(14), 1);

        if (!data.getErrors().isEmpty())
            PortfolioLog.abbreviated(data.getErrors());

        return data.getLatestPrices().stream().max(Comparator.comparing(SecurityPrice::getDate));
    }

    @Override
    public QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse) throws QuoteFeedException
    {
        var start = LocalDate.now().minusYears(DEFAULT_YEARS);

        if (!security.getPrices().isEmpty())
            start = security.getPrices().get(security.getPrices().size() - 1).getDate();

        return getHistoricalQuotes(security, collectRawResponse, start, Integer.MAX_VALUE);
    }

    @Override
    public QuoteFeedData previewHistoricalQuotes(Security security) throws QuoteFeedException
    {
        return getHistoricalQuotes(security, true, LocalDate.now().minusMonths(2), Integer.MAX_VALUE);
    }

    @SuppressWarnings("nls")
    private QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse, LocalDate start,
                    int maxRows) throws QuoteFeedException
    {
        if (apiKey == null || apiKey.isBlank())
            return QuoteFeedData.withError(new IllegalArgumentException(Messages.MsgErrorMissingAPIKey));

        var symbol = trim(security.getTickerSymbol());
        if (symbol == null || symbol.isEmpty())
            return QuoteFeedData.withError(
                            new IOException(MessageFormat.format(Messages.MsgMissingTickerSymbol, security.getName())));

        var pair = CurrencyPair.parse(symbol);
        if (pair.isEmpty())
            return QuoteFeedData.withError(new IOException(
                            MessageFormat.format(Messages.MsgErrorFXMacroDataInvalidCurrencyPair, symbol)));

        var data = new QuoteFeedData();

        var today = LocalDate.now();
        var from = start.isAfter(today) ? today : start;
        var limit = Math.min(PAGE_SIZE, maxRows);
        var offset = 0;
        var rows = 0;

        for (int page = 0; page < MAX_PAGES; page++)
        {
            var webaccess = createWebAccess(pair.get().path()) //
                            .addParameter("start_date", from.toString()) //
                            .addParameter("end_date", today.toString()) //
                            .addParameter("limit", String.valueOf(limit)) //
                            .addParameter("offset", String.valueOf(offset)) //
                            .addHeader("X-API-Key", apiKey);

            String json;
            try
            {
                json = request(webaccess);

                if (collectRawResponse)
                    data.addResponse(webaccess.getURL(), json);
            }
            catch (WebAccessException e)
            {
                if (e.getHttpErrorCode() == HttpStatus.SC_TOO_MANY_REQUESTS)
                    throw new RateLimitExceededException(Duration.ofSeconds(30),
                                    MessageFormat.format(Messages.MsgRateLimitExceeded, getName()));

                data.addError(new IOException(getName() + ": " + extractErrorMessage(e), e));
                break;
            }
            catch (IOException | URISyntaxException e)
            {
                data.addError(e);
                break;
            }

            rows += extract(json, data);

            var nextOffset = extractNextOffset(json);
            if (nextOffset <= offset || rows >= maxRows)
                break;

            offset = nextOffset;
        }

        return data;
    }

    /* package */ WebAccess createWebAccess(String path)
    {
        return new WebAccess(HOST, path);
    }

    /* package */ String request(WebAccess webaccess) throws IOException
    {
        return webaccess.get();
    }

    /**
     * Adds the rates of the answer to the given data and returns the number of
     * rows of the answer (including rows without a value).
     */
    /* package */ int extract(String json, QuoteFeedData data)
    {
        var response = JSONValue.parse(json);
        if (!(response instanceof JSONObject object) || !(object.get("data") instanceof JSONArray rows)) //$NON-NLS-1$
        {
            data.addError(new IOException(MessageFormat.format(Messages.MsgErrorMissingKeyValueInJSON, "data"))); //$NON-NLS-1$
            return 0;
        }

        for (Object element : rows)
        {
            if (!(element instanceof JSONObject row))
                continue;

            var date = asDate(row.get("date")); //$NON-NLS-1$
            if (date == null || !(row.get("val") instanceof Number value)) //$NON-NLS-1$
                continue;

            var price = new LatestSecurityPrice();
            price.setDate(date);
            price.setValue(Values.Quote.factorize(value.doubleValue()));
            price.setHigh(LatestSecurityPrice.NOT_AVAILABLE);
            price.setLow(LatestSecurityPrice.NOT_AVAILABLE);
            price.setVolume(LatestSecurityPrice.NOT_AVAILABLE);

            if (price.getValue() > 0)
                data.addPrice(price);
        }

        return rows.size();
    }

    /**
     * Returns the offset of the next page or -1 if there are no more rows.
     */
    /* package */ int extractNextOffset(String json)
    {
        if (JSONValue.parse(json) instanceof JSONObject response
                        && response.get("pagination") instanceof JSONObject pagination //$NON-NLS-1$
                        && Boolean.TRUE.equals(pagination.get("has_more")) //$NON-NLS-1$
                        && pagination.get("next_offset") instanceof Number nextOffset) //$NON-NLS-1$
            return nextOffset.intValue();

        return -1;
    }

    private String extractErrorMessage(WebAccessException e)
    {
        // errors are answered with {"detail": "...", "code": "..."}
        if (e.getBody() != null && JSONValue.parse(e.getBody()) instanceof JSONObject json
                        && json.get("detail") instanceof String detail) //$NON-NLS-1$
            return detail;

        return e.getMessage();
    }

    private LocalDate asDate(Object value)
    {
        if (!(value instanceof String text))
            return null;

        try
        {
            return LocalDate.parse(text);
        }
        catch (DateTimeParseException e)
        {
            return null;
        }
    }
}
