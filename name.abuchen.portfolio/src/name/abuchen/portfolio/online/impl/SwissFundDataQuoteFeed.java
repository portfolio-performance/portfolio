package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.util.WebAccess;

/**
 * Historical prices of Swiss funds published by SIX on Swiss Fund Data
 * (swissfunddata.ch). Many of these share classes are institutional and are
 * only shown to qualified investors, for example funds held via Swiss third
 * pillar providers. The share class is looked up by the ISIN of the
 * instrument.
 */
@SuppressWarnings("nls")
public class SwissFundDataQuoteFeed implements QuoteFeed
{
    public static final String ID = "SWISSFUNDDATA";

    private static final String HOST = "swissfunddata.ch";
    private static final String API_PATH = "/sfdpub/api/funds/";

    /**
     * The website asks for the investor category and stores the answer in
     * cookies. Without the qualified investor category, the data of
     * institutional share classes is not returned.
     */
    private static final String COOKIE = "investorType=qualified; consentAccepted=true";

    /**
     * Values of the same day which differ less than this tolerance are the
     * same price with a different number of decimal places.
     */
    private static final double SAME_PRICE_TOLERANCE = 0.001;

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return Messages.LabelSwissFundData;
    }

    @Override
    public Optional<LatestSecurityPrice> getLatestQuote(Security security)
    {
        return Optional.empty();
    }

    @Override
    public QuoteFeedData previewHistoricalQuotes(Security security)
    {
        return getHistoricalQuotes(security, true);
    }

    @Override
    public QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse)
    {
        var isin = security.getIsin();
        if (isin == null || isin.isBlank())
            return QuoteFeedData.withError(new IOException(MessageFormat
                            .format(Messages.MsgErrorMissingIdentifierForSecurity, "ISIN", security.getName())));

        var data = new QuoteFeedData();

        try
        {
            var suggestions = request(API_PATH + "suggestions").addParameter("q", isin.trim());
            var suggestionsResponse = suggestions.get();
            if (collectRawResponse)
                data.addResponse(suggestions.getURL(), suggestionsResponse);

            var shareClassId = findShareClassId(suggestionsResponse, isin.trim());
            if (shareClassId.isEmpty())
            {
                data.addError(new IOException(MessageFormat.format(Messages.MsgErrorSwissFundDataNoPrices, isin)));
                return data;
            }

            var performance = request(API_PATH + shareClassId.get() + "/performance")
                            .addParameter("currency", security.getCurrencyCode());
            var performanceResponse = performance.get();
            if (collectRawResponse)
                data.addResponse(performance.getURL(), performanceResponse);

            parsePrices(security, performanceResponse, data);
        }
        catch (IOException | URISyntaxException e)
        {
            data.addError(e);
        }

        return data;
    }

    private WebAccess request(String path)
    {
        return new WebAccess(HOST, path) //
                        .addHeader("Accept", "application/json") //
                        .addHeader("Cookie", COOKIE);
    }

    /* package */ static Optional<String> findShareClassId(String response, String isin)
    {
        var json = JSONValue.parse(response);
        if (!(json instanceof JSONObject root) || !(root.get("groups") instanceof JSONObject groups)
                        || !(groups.get("shareClasses") instanceof JSONArray shareClasses))
            return Optional.empty();

        for (Object item : shareClasses)
        {
            if (item instanceof JSONObject shareClass
                            && isin.equalsIgnoreCase((String) shareClass.get("shareClassIsin"))
                            && shareClass.get("id") instanceof String id)
                return Optional.of(id);
        }

        return Optional.empty();
    }

    /* package */ static void parsePrices(Security security, String response, QuoteFeedData data)
    {
        var json = JSONValue.parse(response);
        if (!(json instanceof JSONObject root))
        {
            data.addError(new IOException(MessageFormat.format(Messages.MsgErrorSwissFundDataNoPrices,
                            security.getIsin())));
            return;
        }

        if (Boolean.TRUE.equals(root.get("isAccessRestricted"))
                        || !(root.get("historicalData") instanceof JSONArray historicalData)
                        || historicalData.isEmpty())
        {
            data.addError(new IOException(MessageFormat.format(Messages.MsgErrorSwissFundDataNoPrices,
                            security.getIsin())));
            return;
        }

        // the service falls back to another currency if the requested
        // currency is not available
        var currency = root.get("chosenCurrency");
        if (!security.getCurrencyCode().equals(currency))
        {
            data.addError(new IOException(MessageFormat.format(Messages.MsgErrorFeedCurrencyMismatch,
                            security.getName(), currency, security.getCurrencyCode())));
            return;
        }

        // the response may contain several values per day: the same price
        // with a different number of decimal places, and occasionally the
        // price in another currency
        var valuesPerDay = new TreeMap<LocalDate, List<BigDecimal>>();
        for (Object item : historicalData)
        {
            if (!(item instanceof JSONArray entry) || entry.size() < 2
                            || !(entry.get(0) instanceof Number timestamp) || !(entry.get(1) instanceof Number))
                continue;

            var date = Instant.ofEpochMilli(timestamp.longValue()).atZone(ZoneOffset.UTC).toLocalDate();
            var value = new BigDecimal(entry.get(1).toString());
            if (value.signum() > 0)
                valuesPerDay.computeIfAbsent(date, d -> new ArrayList<>()).add(value);
        }

        BigDecimal previous = null;
        for (Map.Entry<LocalDate, List<BigDecimal>> day : valuesPerDay.entrySet())
        {
            var value = selectValue(day.getValue(), previous);

            var price = new LatestSecurityPrice(day.getKey(), Values.Quote.factorize(value.doubleValue()));
            price.setHigh(LatestSecurityPrice.NOT_AVAILABLE);
            price.setLow(LatestSecurityPrice.NOT_AVAILABLE);
            price.setVolume(LatestSecurityPrice.NOT_AVAILABLE);
            data.addPrice(price);

            previous = value;
        }
    }

    /**
     * Selects the price of a day out of several candidates. The value closest
     * to the price of the previous day wins. If there is no previous price,
     * the value with the most matching candidates wins. Among values that only
     * differ by rounding, the most precise one is used.
     */
    /* package */ static BigDecimal selectValue(List<BigDecimal> candidates, BigDecimal previous)
    {
        var best = candidates.get(0);

        if (previous != null)
        {
            for (var candidate : candidates)
            {
                if (candidate.subtract(previous).abs().compareTo(best.subtract(previous).abs()) < 0)
                    best = candidate;
            }
        }
        else
        {
            var bestCount = 0;
            for (var candidate : candidates)
            {
                var count = candidates.stream().filter(c -> isSamePrice(c, candidate)).count();
                if (count > bestCount)
                {
                    best = candidate;
                    bestCount = (int) count;
                }
            }
        }

        for (var candidate : candidates)
        {
            if (isSamePrice(candidate, best)
                            && candidate.stripTrailingZeros().scale() > best.stripTrailingZeros().scale())
                best = candidate;
        }

        return best;
    }

    private static boolean isSamePrice(BigDecimal a, BigDecimal b)
    {
        return a.subtract(b).abs().doubleValue() < SAME_PRICE_TOLERANCE;
    }
}
