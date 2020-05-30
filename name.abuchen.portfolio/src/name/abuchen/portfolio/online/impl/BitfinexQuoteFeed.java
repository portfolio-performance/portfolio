package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.util.WebAccess;

public final class BitfinexQuoteFeed implements QuoteFeed
{
    public static final String ID = "BITFINEX"; //$NON-NLS-1$

    private static final long SECONDS_PER_DAY = 24L * 60 * 60;

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "Bitfinex Cryptocurrency Exchange"; //$NON-NLS-1$
    }

    @Override
    public Optional<LatestSecurityPrice> getLatestQuote(Security security)
    {
        QuoteFeedData data = getHistoricalQuotes(security, false, LocalDate.now());

        if (!data.getErrors().isEmpty())
            PortfolioLog.error(data.getErrors());

        List<LatestSecurityPrice> prices = data.getLatestPrices();

        if (prices.isEmpty())
            return Optional.empty();

        Collections.sort(prices, new SecurityPrice.ByDate());

        return Optional.of(prices.get(prices.size() - 1));
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
    private void convertBitfinexJsonArray(JSONArray ohlcArray, QuoteFeedData data)
    {
        if (ohlcArray.isEmpty())
            return;

        List<LatestSecurityPrice> prices = new ArrayList<>();

        if (ohlcArray.get(0) instanceof JSONArray)
        {
            // ohlcArray is an array of quotes
            ohlcArray.forEach(e -> {
                try
                {
                    JSONArray quoteEntry = (JSONArray) e;
                    prices.add(fromArray(quoteEntry));
                }
                catch (ParseException | IllegalArgumentException ex)
                {
                    data.addError(ex);
                }
            });
        }
        else
        {
            // Single quote
            try
            {
                prices.add(fromArray(ohlcArray));
            }
            catch (ParseException | IllegalArgumentException ex)
            {
                data.addError(ex);
            }
        }

        data.addAllPrices(prices);
    }

    private LatestSecurityPrice fromArray(JSONArray ohlcArray) throws ParseException
    {
        long timestamp = Long.parseLong(ohlcArray.get(0).toString());

        long close = YahooHelper.asPrice(ohlcArray.get(2).toString());
        long high = YahooHelper.asPrice(ohlcArray.get(3).toString());
        long low = YahooHelper.asPrice(ohlcArray.get(4).toString());
        int volume = YahooHelper.asNumber(ohlcArray.get(5).toString());

        LatestSecurityPrice price = new LatestSecurityPrice();
        price.setDate(LocalDate.ofEpochDay(timestamp / 1000 / SECONDS_PER_DAY));
        price.setValue(close);
        price.setHigh(high);
        price.setLow(low);
        price.setVolume(volume);

        return price;
    }

    private QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse, LocalDate start)
    {
        if (security.getTickerSymbol() == null)
            return QuoteFeedData.withError(
                            new IOException(MessageFormat.format(Messages.MsgMissingTickerSymbol, security.getName())));

        QuoteFeedData data = new QuoteFeedData();

        final Long tickerStartEpochSeconds = start.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        final String histLatest = ((start.compareTo(LocalDate.now()) == 0) ? "last" : "hist"); //$NON-NLS-1$ //$NON-NLS-2$
        try
        {
            // Ticker: BTCUSD, IOTUSD, ...
            String path = "/v2/candles/trade:1D:t" + security.getTickerSymbol() + '/' + histLatest; //$NON-NLS-1$
            WebAccess webaccess = new WebAccess("api-pub.bitfinex.com", path) //$NON-NLS-1$
                            .addParameter("limit", "1000") // //$NON-NLS-1$ //$NON-NLS-2$
                            .addParameter("start", tickerStartEpochSeconds.toString()); //$NON-NLS-1$
            String html = webaccess.get();

            if (collectRawResponse)
                data.addResponse(webaccess.getURL(), html);

            JSONArray ohlcItems = (JSONArray) JSONValue.parse(html);

            convertBitfinexJsonArray(ohlcItems, data);
        }
        catch (IOException | URISyntaxException e)
        {
            data.addError(e);
        }

        return data;
    }
}
