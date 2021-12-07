package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.util.WebAccess;

public final class KrakenQuoteFeed implements QuoteFeed
{
    public static final String ID = "KRAKEN"; //$NON-NLS-1$

    private static final long SECONDS_PER_DAY = 24L * 60 * 60;

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "Kraken Cryptocurrency Exchange"; //$NON-NLS-1$
    }

    @Override
    public Optional<LatestSecurityPrice> getLatestQuote(Security security)
    {
        QuoteFeedData data = getHistoricalQuotes(security, false, LocalDate.now());

        if (!data.getErrors().isEmpty())
            PortfolioLog.error(data.getErrors());

        List<LatestSecurityPrice> prices = data.getLatestPrices();
        return prices.isEmpty() ? Optional.empty() : Optional.of(prices.get(prices.size() - 1));
    }

    @Override
    public QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse)
    {
        LocalDate quoteStartDate = LocalDate.MIN;

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
        if (security.getTickerSymbol() == null)
            return QuoteFeedData.withError(
                            new IOException(MessageFormat.format(Messages.MsgMissingTickerSymbol, security.getName())));

        QuoteFeedData data = new QuoteFeedData();

        final long tickerStartEpochSeconds = start.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        try
        {
            @SuppressWarnings("nls")
            WebAccess webaccess = new WebAccess("api.kraken.com", "/0/public/OHLC")
                            .addParameter("pair", security.getTickerSymbol()) //
                            .addParameter("since", String.valueOf(tickerStartEpochSeconds)) //
                            .addParameter("interval", "1440");
            String html = webaccess.get();

            if (collectRawResponse)
                data.addResponse(webaccess.getURL(), html);

            JSONObject json = (JSONObject) JSONValue.parse(html);
            JSONArray errorItems = (JSONArray) json.get("error"); //$NON-NLS-1$
            if (!errorItems.isEmpty())
                throw new IOException(this.getName() + " --> " + errorItems.toString()); //$NON-NLS-1$
            JSONObject result = (JSONObject) json.get("result"); //$NON-NLS-1$
            JSONArray ohlcItems = (JSONArray) result.get(security.getTickerSymbol());

            if (ohlcItems != null)
            {
                ohlcItems.forEach(e -> {
                    JSONArray quoteEntry = (JSONArray) e;
                    Long timestamp = Long.parseLong(quoteEntry.get(0).toString());

                    try
                    {
                        long high = YahooHelper.asPrice(quoteEntry.get(2).toString());
                        long low = YahooHelper.asPrice(quoteEntry.get(3).toString());
                        long close = YahooHelper.asPrice(quoteEntry.get(4).toString());
                        int volume = YahooHelper.asNumber(quoteEntry.get(6).toString());

                        LatestSecurityPrice price = new LatestSecurityPrice();
                        price.setDate(LocalDate.ofEpochDay(timestamp / SECONDS_PER_DAY));
                        price.setValue(close);
                        price.setHigh(high);
                        price.setLow(low);
                        price.setVolume(volume);

                        if (close > 0L)
                            data.addPrice(price);

                    }
                    catch (ParseException | IllegalArgumentException ex)
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
}
