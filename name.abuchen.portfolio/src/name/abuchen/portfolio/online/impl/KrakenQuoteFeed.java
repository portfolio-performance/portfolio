package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.online.QuoteFeed;

public final class KrakenQuoteFeed implements QuoteFeed
{

    public static final String ID = "KRAKEN"; //$NON-NLS-1$

    private static final String QUOTE_URL = "https://api.kraken.com/0/public/OHLC?pair={0}&since={1}&interval=1440"; //$NON-NLS-1$

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "Kraken Bitcoin Exchange"; //$NON-NLS-1$
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
    @Override
    public List<LatestSecurityPrice> getHistoricalQuotes(Security security, LocalDate start, List<Exception> errors)
    {
        final Long secondsPerDay = 24L * 60 * 60;
        final Long tickerStartEpochSeconds = start.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        try (CloseableHttpClient client = HttpClients.createSystem())
        {
            String olhcUrl = MessageFormat.format(QUOTE_URL,
                            URLEncoder.encode(security.getTickerSymbol(), StandardCharsets.UTF_8.name()),
                            tickerStartEpochSeconds);

            try (CloseableHttpResponse response = client.execute(new HttpGet(olhcUrl)))
            {
                if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
                    throw new IOException(olhcUrl + " --> " + response.getStatusLine().getStatusCode()); //$NON-NLS-1$

                String body = EntityUtils.toString(response.getEntity());
                JSONObject json = (JSONObject) JSONValue.parse(body);
                JSONArray errorItems = (JSONArray) json.get("error"); //$NON-NLS-1$
                if (!errorItems.isEmpty())
                    throw new IOException(olhcUrl + " --> " + errorItems.toString()); //$NON-NLS-1$
                JSONObject result = (JSONObject) json.get("result"); //$NON-NLS-1$
                JSONArray ohlcItems = (JSONArray) result.get(security.getTickerSymbol());
                List<LatestSecurityPrice> prices = new ArrayList<>();
                ohlcItems.forEach(e -> {
                    JSONArray quoteEntry = (JSONArray) e;
                    Long timestamp = Long.parseLong(quoteEntry.get(0).toString());

                    try
                    {
                        Long open = YahooHelper.asPrice(quoteEntry.get(1).toString());
                        Long high = YahooHelper.asPrice(quoteEntry.get(2).toString());
                        Long low = YahooHelper.asPrice(quoteEntry.get(3).toString());
                        Long close = YahooHelper.asPrice(quoteEntry.get(4).toString());
                        Integer volume = YahooHelper.asNumber(quoteEntry.get(6).toString());

                        LatestSecurityPrice price = new LatestSecurityPrice(
                                        LocalDate.ofEpochDay(timestamp / secondsPerDay), close);
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
                return prices;
            }

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
