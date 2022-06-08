package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.util.WebAccess;

public final class FinnhubQuoteFeed implements QuoteFeed
{
    public static final String ID = "FINNHUB-CANDLE"; //$NON-NLS-1$

    private String apiKey;

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "Finnhub"; //$NON-NLS-1$
    }

    public void setApiKey(String apiKey)
    {
        this.apiKey = apiKey;
    }

    @Override
    public Optional<LatestSecurityPrice> getLatestQuote(Security security)
    {
        QuoteFeedData data = getHistoricalQuotes(security, false, LocalDate.now().minusDays(5));

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
        else
            quoteStartDate = LocalDate.now().minusYears(10);

        return getHistoricalQuotes(security, collectRawResponse, quoteStartDate);
    }

    @Override
    public QuoteFeedData previewHistoricalQuotes(Security security)
    {
        return getHistoricalQuotes(security, true, LocalDate.now().minusMonths(2));
    }

    private QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse, LocalDate startDate)
    {
        if (security.getTickerSymbol() == null)
        {
            return QuoteFeedData.withError(
                            new IOException(MessageFormat.format(Messages.MsgMissingTickerSymbol, security.getName())));
        }

        QuoteFeedData data = new QuoteFeedData();

        try
        {
            @SuppressWarnings("nls")
            WebAccess webaccess = new WebAccess("finnhub.io", "/api/v1/stock/candle")
                            .addParameter("symbol", security.getTickerSymbol()) //
                            .addParameter("resolution", "D") //
                            .addParameter("from",
                                            String.valueOf(startDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC))) //
                            .addParameter("to", String.valueOf(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)));

            if (apiKey != null)
                webaccess.addParameter("token", apiKey); //$NON-NLS-1$

            String response = webaccess.get();

            if (collectRawResponse)
                data.addResponse(webaccess.getURL(), response);

            JSONObject json = (JSONObject) JSONValue.parse(response);

            String status = (String) json.get("s"); //$NON-NLS-1$
            if ("no_data".equals(status)) //$NON-NLS-1$
                return data;

            JSONArray timestamps = (JSONArray) json.get("t"); //$NON-NLS-1$
            JSONArray high = (JSONArray) json.get("h"); //$NON-NLS-1$
            JSONArray low = (JSONArray) json.get("l"); //$NON-NLS-1$
            JSONArray close = (JSONArray) json.get("c"); //$NON-NLS-1$
            JSONArray volume = (JSONArray) json.get("v"); //$NON-NLS-1$

            if (timestamps == null)
            {
                data.addError(new IOException(MessageFormat.format(Messages.MsgErrorMissingKeyValueInJSON, "t"))); //$NON-NLS-1$
                return data;
            }

            if (close == null)
            {
                data.addError(new IOException(MessageFormat.format(Messages.MsgErrorMissingKeyValueInJSON, "c"))); //$NON-NLS-1$
                return data;
            }

            int size = timestamps.size();

            for (int index = 0; index < size; index++)
            {
                LatestSecurityPrice price = new LatestSecurityPrice();
                price.setDate(LocalDateTime.ofEpochSecond((Long) timestamps.get(index), 0, ZoneOffset.UTC)
                                .toLocalDate());

                Number c = (Number) close.get(index);
                price.setValue(c == null ? LatestSecurityPrice.NOT_AVAILABLE : Values.Quote.factorize(c.doubleValue()));

                Number h = (Number) high.get(index);
                price.setHigh(h == null ? LatestSecurityPrice.NOT_AVAILABLE : Values.Quote.factorize(h.doubleValue()));

                Number l = (Number) low.get(index);
                price.setLow(l == null ? LatestSecurityPrice.NOT_AVAILABLE : Values.Quote.factorize(l.doubleValue()));

                Number v = (Number) volume.get(index);
                price.setVolume(v == null ? LatestSecurityPrice.NOT_AVAILABLE : v.longValue());

                if (price.getValue() > 0)
                    data.addPrice(price);
            }
        }
        catch (IOException | URISyntaxException e)
        {
            data.addError(e);
        }

        return data;
    }
}
