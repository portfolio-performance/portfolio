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

        List<LatestSecurityPrice> prices = getHistoricalQuotes(security, quoteStartDate, errors);

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
    private List<LatestSecurityPrice> convertBitfinexJsonArray(JSONArray ohlcArray, List<Exception> errors)
    {
        final Long secondsPerDay = 24L * 60 * 60;
        List<LatestSecurityPrice> prices = new ArrayList<>();
        
        if(ohlcArray.size() == 0)
            return prices;
        
        if (ohlcArray.get(0) instanceof JSONArray)
        {
            // ohlcArray is an array of quotes
            ohlcArray.forEach(e -> {
                JSONArray quoteEntry = (JSONArray) e;
                Long timestamp = Long.parseLong(quoteEntry.get(0).toString());

                try
                {
                    Long open = YahooHelper.asPrice(quoteEntry.get(1).toString());
                    Long close = YahooHelper.asPrice(quoteEntry.get(2).toString());
                    Long high = YahooHelper.asPrice(quoteEntry.get(3).toString());
                    Long low = YahooHelper.asPrice(quoteEntry.get(4).toString());
                    Integer volume = YahooHelper.asNumber(quoteEntry.get(5).toString());

                    LatestSecurityPrice price = new LatestSecurityPrice(LocalDate.ofEpochDay(timestamp / 1000 / secondsPerDay), close);
                    price.setHigh(high);
                    price.setLow(low);
                    price.setVolume(volume);
                    price.setPreviousClose(open);

                    prices.add(price);

                }
                catch (ParseException ex)
                {
                    errors.add(ex);
                }
            });
        }
        else
        {
           // Single quote
            Long timestamp = Long.parseLong(ohlcArray.get(0).toString());

            try
            {
                Long open = YahooHelper.asPrice(ohlcArray.get(1).toString());
                Long close = YahooHelper.asPrice(ohlcArray.get(2).toString());
                Long high = YahooHelper.asPrice(ohlcArray.get(3).toString());
                Long low = YahooHelper.asPrice(ohlcArray.get(4).toString());
                Integer volume = YahooHelper.asNumber(ohlcArray.get(5).toString());

                LatestSecurityPrice price = new LatestSecurityPrice(LocalDate.ofEpochDay(timestamp / 1000 / secondsPerDay), close);
                price.setHigh(high);
                price.setLow(low);
                price.setVolume(volume);
                price.setPreviousClose(open);

                prices.add(price);

            }
            catch (ParseException ex)
            {
                errors.add(ex);
            }
        }
        return prices;
    }

    @Override
    public List<LatestSecurityPrice> getHistoricalQuotes(Security security, LocalDate start, List<Exception> errors)
    {
        final Long tickerStartEpochSeconds = start.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        final String histLatest = ((start.compareTo(LocalDate.now()) == 0) ? "last" : "hist"); //$NON-NLS-1$ //$NON-NLS-2$
        try
        {
            @SuppressWarnings("nls")
            String path = "/v2/candles/trade:1D:t" + security.getTickerSymbol() + "/" + histLatest; // Ticker: BTCUSD, IOTUSD, ...
            String html = new WebAccess("api-pub.bitfinex.com", path) //$NON-NLS-1$
                            .addParameter("limit", "1000") // //$NON-NLS-1$ //$NON-NLS-2$
                            .addParameter("start", tickerStartEpochSeconds.toString()) // //$NON-NLS-1$
                            .get();

            JSONArray ohlcItems = (JSONArray) JSONValue.parse(html);    
            
            List<LatestSecurityPrice> prices = convertBitfinexJsonArray(ohlcItems, errors);

            return prices;

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