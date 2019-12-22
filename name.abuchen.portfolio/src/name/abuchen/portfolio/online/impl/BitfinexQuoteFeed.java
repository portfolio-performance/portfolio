package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.online.QuoteFeed;
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
    public boolean updateLatestQuotes(Security security, List<Exception> errors)
    {
        if (security.getTickerSymbol() == null)
        {
            errors.add(new IOException(MessageFormat.format(Messages.MsgMissingTickerSymbol, security.getName())));
            return false;
        }

        List<LatestSecurityPrice> prices = getHistoricalQuotes(security, LocalDate.now(), errors);

        if (prices.isEmpty())
        {
            return false;
        }
        else
        {
            LatestSecurityPrice price = prices.get(prices.size() - 1);
            if (price.getValue() != 0)
                security.setLatest(price);

            return true;
        }
    }

    @Override
    public boolean updateHistoricalQuotes(Security security, List<Exception> errors)
    {
        if (security.getTickerSymbol() == null)
        {
            errors.add(new IOException(MessageFormat.format(Messages.MsgMissingTickerSymbol, security.getName())));
            return false;
        }

        LocalDate quoteStartDate = LocalDate.MIN;

        if (!security.getPrices().isEmpty())
            quoteStartDate = security.getPrices().get(security.getPrices().size() - 1).getDate();

        List<SecurityPrice> prices = getHistoricalQuotes(SecurityPrice.class, security, quoteStartDate, errors);

        boolean isUpdated = false;
        for (SecurityPrice p : prices)
        {
            if (p.getDate().isBefore(LocalDate.now()))
            {
                boolean isAdded = security.addPrice(p);
                isUpdated = isUpdated || isAdded;
            }
        }
        return isUpdated;
    }

    @SuppressWarnings("unchecked")
    private <T extends SecurityPrice> List<T> convertBitfinexJsonArray(Class<T> klass, JSONArray ohlcArray,
                    List<Exception> errors)
    {
        List<T> prices = new ArrayList<>();

        if (ohlcArray.isEmpty())
            return prices;

        if (ohlcArray.get(0) instanceof JSONArray)
        {
            // ohlcArray is an array of quotes
            ohlcArray.forEach(e -> {
                try
                {
                    JSONArray quoteEntry = (JSONArray) e;
                    prices.add(fromArray(klass, quoteEntry));
                }
                catch (ReflectiveOperationException | ParseException | IllegalArgumentException | SecurityException ex)
                {
                    errors.add(ex);
                }
            });
        }
        else
        {
            // Single quote
            try
            {
                prices.add(fromArray(klass, ohlcArray));
            }
            catch (ReflectiveOperationException | ParseException | IllegalArgumentException | SecurityException ex)
            {
                errors.add(ex);
            }
        }
        return prices;
    }

    private <T extends SecurityPrice> T fromArray(Class<T> klass, JSONArray ohlcArray)
                    throws ParseException, ReflectiveOperationException
    {
        long timestamp = Long.parseLong(ohlcArray.get(0).toString());

        long open = YahooHelper.asPrice(ohlcArray.get(1).toString());
        long close = YahooHelper.asPrice(ohlcArray.get(2).toString());
        long high = YahooHelper.asPrice(ohlcArray.get(3).toString());
        long low = YahooHelper.asPrice(ohlcArray.get(4).toString());
        int volume = YahooHelper.asNumber(ohlcArray.get(5).toString());

        T price = klass.getConstructor().newInstance();
        price.setDate(LocalDate.ofEpochDay(timestamp / 1000 / SECONDS_PER_DAY));
        price.setValue(close);

        if (price instanceof LatestSecurityPrice)
        {
            LatestSecurityPrice lsp = (LatestSecurityPrice) price;
            lsp.setHigh(high);
            lsp.setLow(low);
            lsp.setVolume(volume);
            lsp.setPreviousClose(open);
        }
        return price;
    }

    @Override
    public List<LatestSecurityPrice> getHistoricalQuotes(Security security, LocalDate start, List<Exception> errors)
    {
        return getHistoricalQuotes(LatestSecurityPrice.class, security, start, errors);
    }

    private <T extends SecurityPrice> List<T> getHistoricalQuotes(Class<T> klass, Security security, LocalDate start,
                    List<Exception> errors)
    {
        final Long tickerStartEpochSeconds = start.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        final String histLatest = ((start.compareTo(LocalDate.now()) == 0) ? "last" : "hist"); //$NON-NLS-1$ //$NON-NLS-2$
        try
        {
            // Ticker: BTCUSD, IOTUSD, ...
            String path = "/v2/candles/trade:1D:t" + security.getTickerSymbol() + '/' + histLatest; //$NON-NLS-1$
            String html = new WebAccess("api-pub.bitfinex.com", path) //$NON-NLS-1$
                            .addParameter("limit", "1000") // //$NON-NLS-1$ //$NON-NLS-2$
                            .addParameter("start", tickerStartEpochSeconds.toString()) // //$NON-NLS-1$
                            .get();

            JSONArray ohlcItems = (JSONArray) JSONValue.parse(html);

            return convertBitfinexJsonArray(klass, ohlcItems, errors);
        }
        catch (IOException e)
        {
            errors.add(e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<LatestSecurityPrice> getHistoricalQuotes(String response, List<Exception> errors)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Exchange> getExchanges(Security subject, List<Exception> errors)
    {
        return Collections.emptyList();
    }
}
