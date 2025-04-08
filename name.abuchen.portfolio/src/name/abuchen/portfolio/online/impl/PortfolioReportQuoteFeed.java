package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.osgi.framework.FrameworkUtil;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.util.WebAccess;

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
    public Optional<LatestSecurityPrice> getLatestQuote(Security security)
    {
        List<LatestSecurityPrice> prices = getHistoricalQuotes(security, true, LocalDate.now()).getLatestPrices();
        return prices.isEmpty() ? Optional.empty() : Optional.of(prices.get(prices.size() - 1));
    }

    @Override
    public QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse)
    {
        LocalDate start = null;

        if (!security.getPrices().isEmpty())
            start = security.getPrices().get(security.getPrices().size() - 1).getDate().plusDays(1);
        else
            start = LocalDate.of(2000, 1, 1);

        return getHistoricalQuotes(security, collectRawResponse, start);
    }

    @Override
    public QuoteFeedData previewHistoricalQuotes(Security security)
    {
        return getHistoricalQuotes(security, true, LocalDate.now().minusMonths(2));
    }

    @SuppressWarnings("unchecked")
    private QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse, LocalDate start)
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
        catch (IOException | URISyntaxException e)
        {
            data.addError(e);
        }

        return data;
    }
}
