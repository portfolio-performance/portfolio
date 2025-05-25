package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.hc.core5.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.oauth.AccessToken;
import name.abuchen.portfolio.oauth.AuthenticationException;
import name.abuchen.portfolio.oauth.OAuthClient;
import name.abuchen.portfolio.online.AuthenticationExpiredException;
import name.abuchen.portfolio.online.FeedConfigurationException;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.online.QuoteFeedException;
import name.abuchen.portfolio.online.RateLimitExceededException;
import name.abuchen.portfolio.util.TradeCalendarManager;
import name.abuchen.portfolio.util.WebAccess;
import name.abuchen.portfolio.util.WebAccess.WebAccessException;

public final class PortfolioPerformanceFeed implements QuoteFeed
{
    private static class CachedResponse
    {
        LocalDate start;
        String json;
    }

    public static final String ID = "PP"; //$NON-NLS-1$

    private static final String ENDPOINT = "api.portfolio-performance.info"; //$NON-NLS-1$

    private static final String PATH_PREFIX_SAMPLE = "/sample"; //$NON-NLS-1$

    private static final String PATH_HISTORIC = "/v1/candle"; //$NON-NLS-1$

    /**
     * Sample symbols that can be used without authentication. It is also
     * checked on the server but we need to use a different endpoint.
     */
    @SuppressWarnings("nls")
    private static final Set<String> SAMPLE_SYMBOLS = Set.of( //
                    "AMZN", // Amazon
                    "NVD.F", // Nvidia
                    "MBG.DE", // Mercedes Benz
                    "IQQY.DE", // iShares Core MSCI Europe UCITS ETF EUR (Dist)
                    "SXRS.DE", // iShares Diversified Commodity Swap UCITS ETF
                    "EUNH.DE", // iShares Core Euro Government Bond UCITS ETF
                    "IQQN.DE", // iShares MSCI North America UCITS ETF
                    "X014.DE", // Lyxor MSCI Pacific UCITS ETF
                    "IQQE.DE" // iShares MSCI EM UCITS ETF (Dist)
    );

    private static final OAuthClient oauthClient = OAuthClient.INSTANCE;

    private final PageCache<CachedResponse> cache = new PageCache<>();

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "Portfolio Performance (built-in)"; //$NON-NLS-1$
    }

    /**
     * Returns true if any of the securities require authentication for this
     * feed.
     */
    public boolean requiresAuthentication(List<Security> securities)
    {
        return securities.stream().anyMatch(s -> {
            if (!ID.equals(s.getFeed()))
                return false;
            if (s.getTickerSymbol() == null)
                return false;
            return !SAMPLE_SYMBOLS.contains(s.getTickerSymbol());
        });
    }

    @Override
    public Optional<LatestSecurityPrice> getLatestQuote(Security security)
    {
        return Optional.empty();
    }

    @Override
    public QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse) throws QuoteFeedException
    {
        if (security.getTickerSymbol() == null)
        {
            return QuoteFeedData.withError(
                            new IOException(MessageFormat.format(Messages.MsgMissingTickerSymbol, security.getName())));
        }

        LocalDate quoteStartDate = null;

        if (!security.getPrices().isEmpty())
        {
            var lastPriceDate = security.getPrices().get(security.getPrices().size() - 1).getDate();

            // skip the download if
            // a) the configuration has not changed and we therefore can assume
            // historical prices have been provided by this feed *and*
            // b) there cannot be a newer price available on the server

            var configChanged = security.getEphemeralData().getFeedConfigurationChanged();
            var feedUpdate = security.getEphemeralData().getFeedLastUpdate();
            var configHasNotChanged = configChanged.isEmpty()
                            || (feedUpdate.isPresent() && feedUpdate.get().isAfter(configChanged.get()));

            if (configHasNotChanged)
            {
                var utcToday = ZonedDateTime.now(ZoneOffset.UTC).toLocalDate();

                // For EU equities, it will be available only the next day.
                // For US equities, a couple hours after market closing at 22:00
                // UTC.
                var expectedAvailablePrice = utcToday.minusDays(1);

                // For the time being, use a minimal calendar (weekends,
                // christmas, new year). We can possibly switch to
                // exchange-specific trade calendar, however, let's start with
                // less aggressive caching. Do not use the trade calendar
                // configured by the user.
                var tradeCalendar = TradeCalendarManager.getInstance(TradeCalendarManager.MINIMAL_CALENDAR_CODE);

                while (tradeCalendar.isHoliday(expectedAvailablePrice))
                {
                    expectedAvailablePrice = expectedAvailablePrice.minusDays(1);
                }

                if (lastPriceDate.equals(expectedAvailablePrice))
                {
                    // skip update b/c server cannot have newer data
                    return new QuoteFeedData();
                }
            }

            // adjust request to Monday to enable more aggressive caching
            quoteStartDate = lastPriceDate;
            if (quoteStartDate.getDayOfWeek() != DayOfWeek.MONDAY)
                quoteStartDate = quoteStartDate.with(TemporalAdjusters.previous(DayOfWeek.MONDAY));
        }
        else
        {
            // request 10 years starting Jan 1st to enable caching
            quoteStartDate = LocalDate.now().minusYears(10).withDayOfYear(1);
        }

        return getHistoricalQuotes(security, collectRawResponse, quoteStartDate);
    }

    @Override
    public QuoteFeedData previewHistoricalQuotes(Security security) throws QuoteFeedException
    {
        if (security.getTickerSymbol() == null)
        {
            return QuoteFeedData.withError(
                            new IOException(MessageFormat.format(Messages.MsgMissingTickerSymbol, security.getName())));
        }

        return getHistoricalQuotes(security, true, LocalDate.now().minusMonths(2));
    }

    private QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse, LocalDate startDate)
                    throws QuoteFeedException
    {
        var isSample = SAMPLE_SYMBOLS.contains(security.getTickerSymbol());
        var isAuthenticated = oauthClient != null && oauthClient.isAuthenticated();

        if (!isSample && !isAuthenticated)
            return QuoteFeedData.withError(new IllegalArgumentException(Messages.LabelLoginToRetrieveHistoricalPrices));

        Optional<AccessToken> accessToken = Optional.empty();

        if (!isSample)
        {
            try
            {
                accessToken = oauthClient.getAPIAccessToken();
            }
            catch (AuthenticationException e)
            {
                PortfolioLog.error(e);
            }

            if (accessToken.isEmpty())
                throw new AuthenticationExpiredException();
        }

        QuoteFeedData data = new QuoteFeedData();

        try
        {
            // move the start day to a Monday to enable more aggressive caching
            if (startDate.getDayOfWeek() != DayOfWeek.MONDAY)
                startDate = startDate.with(TemporalAdjusters.previous(DayOfWeek.MONDAY));

            var to = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toEpochSecond();

            @SuppressWarnings("nls")
            WebAccess webaccess = new WebAccess(ENDPOINT, isSample ? PATH_PREFIX_SAMPLE + PATH_HISTORIC : PATH_HISTORIC) //
                            .addParameter("symbol", security.getTickerSymbol()) //
                            .addParameter("from",
                                            String.valueOf(startDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC))) //
                            .addParameter("to", String.valueOf(to));

            if (accessToken.isPresent())
                webaccess.addBearer(accessToken.get().getToken());

            // check cache first

            var response = cache.lookup(security.getTickerSymbol());
            if (response == null || response.start.isAfter(startDate))
            {
                response = new CachedResponse();
                response.start = startDate;
                response.json = webaccess.get();

                if (response.json != null)
                    cache.put(security.getTickerSymbol(), response);
            }

            if (collectRawResponse)
                data.addResponse(webaccess.getURL(), response.json);

            parseCandle(response.json, data);
        }
        catch (WebAccessException e)
        {
            switch (e.getHttpErrorCode())
            {
                case HttpStatus.SC_TOO_MANY_REQUESTS:
                    throw new RateLimitExceededException(Duration.ofMinutes(1),
                                    MessageFormat.format(Messages.MsgRateLimitExceeded, getName()));
                case HttpStatus.SC_NOT_FOUND, HttpStatus.SC_FORBIDDEN:
                    throw new FeedConfigurationException();
                default:
                    data.addError(e);
            }
        }
        catch (IOException | URISyntaxException e)
        {
            data.addError(e);
        }

        return data;
    }

    private void parseCandle(String response, QuoteFeedData data)
    {
        if (response == null)
            return;

        JSONObject json = (JSONObject) JSONValue.parse(response);

        String status = (String) json.get("s"); //$NON-NLS-1$
        if ("no_data".equals(status)) //$NON-NLS-1$
            return;

        JSONArray timestamps = (JSONArray) json.get("t"); //$NON-NLS-1$
        JSONArray high = (JSONArray) json.get("h"); //$NON-NLS-1$
        JSONArray low = (JSONArray) json.get("l"); //$NON-NLS-1$
        JSONArray close = (JSONArray) json.get("c"); //$NON-NLS-1$

        if (timestamps == null)
        {
            data.addError(new IOException(MessageFormat.format(Messages.MsgErrorMissingKeyValueInJSON, "t"))); //$NON-NLS-1$
            return;
        }

        if (close == null)
        {
            data.addError(new IOException(MessageFormat.format(Messages.MsgErrorMissingKeyValueInJSON, "c"))); //$NON-NLS-1$
            return;
        }

        int size = timestamps.size();

        for (int index = 0; index < size; index++)
        {
            LatestSecurityPrice price = new LatestSecurityPrice();
            price.setDate(LocalDateTime.ofEpochSecond((Long) timestamps.get(index), 0, ZoneOffset.UTC).toLocalDate());

            Number c = (Number) close.get(index);
            price.setValue(c == null ? LatestSecurityPrice.NOT_AVAILABLE : Values.Quote.factorize(c.doubleValue()));

            Number h = (Number) high.get(index);
            price.setHigh(h == null ? LatestSecurityPrice.NOT_AVAILABLE : Values.Quote.factorize(h.doubleValue()));

            Number l = (Number) low.get(index);
            price.setLow(l == null ? LatestSecurityPrice.NOT_AVAILABLE : Values.Quote.factorize(l.doubleValue()));

            price.setVolume(LatestSecurityPrice.NOT_AVAILABLE);

            if (price.getValue() > 0)
                data.addPrice(price);
        }
    }

    @Override
    public List<Exchange> getExchanges(Security subject, List<Exception> errors)
    {
        var parameter = "isin"; //$NON-NLS-1$
        var query = subject.getIsin();

        if (query == null || query.isBlank())
        {
            parameter = "symbol"; //$NON-NLS-1$
            query = subject.getTickerSymbol();
        }

        if (query == null || query.isBlank())
            return Collections.emptyList();

        try
        {
            var answer = new ArrayList<Exchange>();
            var candidates = new PortfolioPerformanceSearchProvider().internalSearch(parameter, query);

            for (var candidate : candidates) // NOSONAR
            {
                (candidate.getMarkets().isEmpty() ? List.of(candidate) : candidate.getMarkets()) //
                                .stream().forEach(r -> {
                                    var label = MessageFormat.format("{0} * {1}", //$NON-NLS-1$
                                                    r.getCurrencyCode(),
                                                    MarketIdentifierCodes.getLabel(r.getExchange()));
                                    var exchange = new Exchange(r.getSymbol(), label);
                                    answer.add(exchange);
                                });
            }

            return answer;

        }
        catch (IOException e)
        {
            errors.add(e);
            return Collections.emptyList();
        }
    }
}
