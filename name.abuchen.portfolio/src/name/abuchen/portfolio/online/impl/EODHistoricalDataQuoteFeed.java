package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
import name.abuchen.portfolio.util.WebAccess;

public final class EODHistoricalDataQuoteFeed implements QuoteFeed
{
    public static final String ID = "EODHISTORICALDATA"; //$NON-NLS-1$

    private String apiKey;

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "EOD Historical Data"; //$NON-NLS-1$
    }

    public void setApiKey(String apiKey)
    {
        this.apiKey = apiKey;
    }

    @Override
    public Optional<LatestSecurityPrice> getLatestQuote(Security security)
    {
        QuoteFeedData data = getHistoricalQuotes(security, false, LocalDate.now().minusDays(5));

        if (!data.getErrors().isEmpty())
            PortfolioLog.abbreviated(data.getErrors());

        List<LatestSecurityPrice> prices = data.getLatestPrices();
        if (prices.isEmpty())
            return Optional.empty();

        Collections.sort(prices, new SecurityPrice.ByDate());

        return Optional.of(prices.get(prices.size() - 1));
    }

    @Override
    public QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse)
    {
        LocalDate quoteStartDate = null;

        if (!security.getPrices().isEmpty())
            quoteStartDate = security.getPrices().get(security.getPrices().size() - 1).getDate();

        return getHistoricalQuotes(security, collectRawResponse, quoteStartDate);
    }

    @Override
    public QuoteFeedData previewHistoricalQuotes(Security security)
    {
        return getHistoricalQuotes(security, true, LocalDate.now().minusMonths(2));
    }

    @SuppressWarnings("unchecked")
    public QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse, LocalDate start)
    {
        if (apiKey == null)
            return QuoteFeedData.withError(new IllegalArgumentException(Messages.MsgErrorMissingAPIKey));

        if (security.getTickerSymbol() == null)
            return QuoteFeedData.withError(
                            new IOException(MessageFormat.format(Messages.MsgMissingTickerSymbol, security.getName())));

        QuoteFeedData data = new QuoteFeedData();

        try
        {
            // https://eodhistoricaldata.com/api/eod/MCD.US?api_token=615bf7afab49c8.09742409

            @SuppressWarnings("nls")
            WebAccess webaccess = new WebAccess("eodhistoricaldata.com", "/api/eod/" + security.getTickerSymbol())
                            .addParameter("api_token", apiKey) //
                            .addParameter("fmt", "json") //
                            .addParameter("period", "d");

            if (start != null)
                webaccess.addParameter("from", start.toString()); //$NON-NLS-1$

            String html = webaccess.get();

            if (collectRawResponse)
                data.addResponse(webaccess.getURL(), html);

            JSONArray json = (JSONArray) JSONValue.parse(html);

            if (json != null)
            {
                json.forEach(e -> {

                    JSONObject quoteEntry = (JSONObject) e;

                    try
                    {
                        LocalDate date = YahooHelper.fromISODate(String.valueOf(quoteEntry.get("date"))); //$NON-NLS-1$
                        long high = asPrice(quoteEntry.get("high")); //$NON-NLS-1$
                        long low = asPrice(quoteEntry.get("low")); //$NON-NLS-1$
                        long close = asPrice(quoteEntry.get("close")); //$NON-NLS-1$
                        long volume = asNumber(quoteEntry.get("volume")); //$NON-NLS-1$

                        LatestSecurityPrice price = new LatestSecurityPrice();
                        price.setDate(date);
                        price.setValue(close);
                        price.setHigh(high);
                        price.setLow(low);
                        price.setVolume(volume);

                        if (date != null && close > 0L)
                            data.addPrice(price);

                    }
                    catch (IllegalArgumentException ex)
                    {
                        data.addError(ex);
                    }
                });
            }

        }
        catch (IOException | URISyntaxException e)
        {
            data.addError(e);
        }

        return data;
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
