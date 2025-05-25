package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.hc.core5.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.osgi.framework.FrameworkUtil;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.FeedConfigurationException;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.online.QuoteFeedException;
import name.abuchen.portfolio.online.RateLimitExceededException;
import name.abuchen.portfolio.util.TradeCalendarManager;
import name.abuchen.portfolio.util.WebAccess;
import name.abuchen.portfolio.util.WebAccess.WebAccessException;

public final class PortfolioReportQuoteFeed implements QuoteFeed
{
    private static class ResponseData
    {
        LocalDate start;
        String json;
    }

    public static final String ID = "PORTFOLIO-REPORT"; //$NON-NLS-1$

    private final PageCache<ResponseData> cache = new PageCache<>();

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "Portfolio Report"; //$NON-NLS-1$
    }

    @Override
    public boolean mergeDownloadRequests()
    {
        return true;
    }

    @Override
    public Optional<LatestSecurityPrice> getLatestQuote(Security security) throws QuoteFeedException
    {
        List<LatestSecurityPrice> prices = getHistoricalQuotes(security, true, LocalDate.now()).getLatestPrices();
        return prices.isEmpty() ? Optional.empty() : Optional.of(prices.get(prices.size() - 1));
    }

    @Override
    public QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse) throws QuoteFeedException
    {
        LocalDate start = null;

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

                // End of day prices are only available the next day.
                var expectedAvailablePrice = utcToday.minusDays(1);

                // Use Xetra calendar
                var tradeCalendar = TradeCalendarManager.getInstance("de"); //$NON-NLS-1$

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
            start = lastPriceDate.plusDays(1);
        }
        else
        {
            start = LocalDate.of(2000, 1, 1);
        }

        return getHistoricalQuotes(security, collectRawResponse, start);
    }

    @Override
    public QuoteFeedData previewHistoricalQuotes(Security security) throws QuoteFeedException
    {
        return getHistoricalQuotes(security, true, LocalDate.now().minusMonths(2));
    }

    @SuppressWarnings("unchecked")
    private QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse, LocalDate start)
                    throws QuoteFeedException
    {
        if (security.getOnlineId() == null)
        {
            return QuoteFeedData.withError(new IOException(
                            MessageFormat.format(Messages.MsgErrorMissingOnlineId, security.getName())));
        }

        QuoteFeedData data = new QuoteFeedData();

        try
        {
            @SuppressWarnings("nls")
            WebAccess webaccess = new WebAccess("api.portfolio-report.net", //
                            "/securities/uuid/" + security.getOnlineId() + "/prices/" + security.getCurrencyCode())
                                            .addUserAgent("PortfolioPerformance/"
                                                            + FrameworkUtil.getBundle(PortfolioReportNet.class)
                                                                            .getVersion().toString())
                                            .addParameter("from", start.toString());

            ResponseData response = cache.lookup(security.getOnlineId() + security.getCurrencyCode());

            if (response == null || response.start.isAfter(start))
            {
                response = new ResponseData();
                response.start = start;
                response.json = webaccess.get();

                if (response.json != null)
                    cache.put(security.getOnlineId() + security.getCurrencyCode(), response);
            }

            if (collectRawResponse)
                data.addResponse(webaccess.getURL(), response.json);

            JSONArray pricesJson = (JSONArray) JSONValue.parse(response.json);

            if (pricesJson == null)
            {
                data.addError(new IOException(MessageFormat.format(Messages.MsgErrorMissingKeyValueInJSON, "prices"))); //$NON-NLS-1$
                return data;
            }

            pricesJson.forEach(entry -> {
                JSONObject row = (JSONObject) entry;

                LocalDate date = LocalDate.parse(row.get("date").toString()); //$NON-NLS-1$

                Number c = (Number) row.get("close"); //$NON-NLS-1$
                long close = c == null ? LatestSecurityPrice.NOT_AVAILABLE : Values.Quote.factorize(c.doubleValue());

                if (close > 0)
                {
                    LatestSecurityPrice price = new LatestSecurityPrice();
                    price.setDate(date);
                    price.setValue(close);

                    price.setHigh(LatestSecurityPrice.NOT_AVAILABLE);
                    price.setLow(LatestSecurityPrice.NOT_AVAILABLE);
                    price.setVolume(LatestSecurityPrice.NOT_AVAILABLE);

                    data.addPrice(price);
                }
            });

        }
        catch (WebAccessException e)
        {
            switch (e.getHttpErrorCode())
            {
                case HttpStatus.SC_TOO_MANY_REQUESTS:
                    throw new RateLimitExceededException(Duration.ofMinutes(1),
                                    MessageFormat.format(Messages.MsgRateLimitExceeded, getName()));
                case HttpStatus.SC_NOT_FOUND:
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
}
