package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.util.Dates;
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
    public boolean updateLatestQuotes(Security security, List<Exception> errors)
    {
        List<LatestSecurityPrice> prices = getHistoricalQuotes(LatestSecurityPrice.class, security, 5, errors);

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
        int count = 20000;

        if (!security.getPrices().isEmpty())
        {
            LocalDate startDate = security.getPrices().get(security.getPrices().size() - 1).getDate();
            count = Dates.daysBetween(startDate, LocalDate.now()) + 5;
        }

        List<SecurityPrice> prices = getHistoricalQuotes(SecurityPrice.class, security, count, errors);

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

    @Override
    public List<LatestSecurityPrice> getHistoricalQuotes(Security security, LocalDate start, List<Exception> errors)
    {
        return getHistoricalQuotes(LatestSecurityPrice.class, security, 100, errors);
    }

    public <T extends SecurityPrice> List<T> getHistoricalQuotes(Class<T> klass, Security security, int count,
                    List<Exception> errors)
    {
        if (security.getTickerSymbol() == null)
        {
            errors.add(new IOException(MessageFormat.format(Messages.MsgMissingTickerSymbol, security.getName())));
            return Collections.emptyList();
        }

        try
        {
            @SuppressWarnings("nls")
            WebAccess webaccess = new WebAccess("finnhub.io", "/api/v1/stock/candle")
                            .addParameter("symbol", security.getTickerSymbol()).addParameter("resolution", "D")
                            .addParameter("count", String.valueOf(count));

            if (apiKey != null)
                webaccess.addParameter("token", apiKey); //$NON-NLS-1$

            String response = webaccess.get();

            JSONObject json = (JSONObject) JSONValue.parse(response);

            String status = (String) json.get("s"); //$NON-NLS-1$
            if ("no_data".equals(status)) //$NON-NLS-1$
                return Collections.emptyList();

            JSONArray timestamps = (JSONArray) json.get("t"); //$NON-NLS-1$
            JSONArray high = (JSONArray) json.get("h"); //$NON-NLS-1$
            JSONArray low = (JSONArray) json.get("l"); //$NON-NLS-1$
            JSONArray close = (JSONArray) json.get("c"); //$NON-NLS-1$
            JSONArray volume = (JSONArray) json.get("v"); //$NON-NLS-1$

            if (timestamps == null)
            {
                errors.add(new IOException(MessageFormat.format(Messages.MsgErrorMissingKeyValueInJSON, "t"))); //$NON-NLS-1$
                return Collections.emptyList();
            }

            if (close == null)
            {
                errors.add(new IOException(MessageFormat.format(Messages.MsgErrorMissingKeyValueInJSON, "c"))); //$NON-NLS-1$
                return Collections.emptyList();
            }

            List<T> prices = new ArrayList<>();

            int size = timestamps.size();

            for (int index = 0; index < size; index++)
            {
                T price = klass.getConstructor().newInstance();
                price.setDate(LocalDateTime.ofEpochSecond((Long) timestamps.get(index), 0, ZoneOffset.UTC)
                                .toLocalDate());

                Number c = (Number) close.get(index);
                price.setValue(c == null ? LatestSecurityPrice.NOT_AVAILABLE : Values.Quote.factorize(c.doubleValue()));

                if (price instanceof LatestSecurityPrice)
                {
                    LatestSecurityPrice lsp = (LatestSecurityPrice) price;

                    Number h = (Number) high.get(index);
                    lsp.setHigh(h == null ? LatestSecurityPrice.NOT_AVAILABLE
                                    : Values.Quote.factorize(h.doubleValue()));

                    Number l = (Number) low.get(index);
                    lsp.setLow(l == null ? LatestSecurityPrice.NOT_AVAILABLE : Values.Quote.factorize(l.doubleValue()));

                    Number v = (Number) volume.get(index);
                    lsp.setVolume(v == null ? LatestSecurityPrice.NOT_AVAILABLE : v.longValue());
                }

                if (price.getValue() > 0)
                    prices.add(price);
            }

            return prices;

        }
        catch (ReflectiveOperationException | SecurityException | IOException e)
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
