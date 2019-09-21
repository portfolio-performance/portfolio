package name.abuchen.portfolio.online.impl;

import static name.abuchen.portfolio.online.impl.YahooHelper.asPrice;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.google.common.util.concurrent.RateLimiter;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.util.Dates;
import name.abuchen.portfolio.util.RateLimitExceededException;
import name.abuchen.portfolio.util.WebAccess;

public class AlphavantageQuoteFeed implements QuoteFeed
{
    private enum OutputSize
    {
        COMPACT, FULL
    }

    public static final String ID = "ALPHAVANTAGE"; //$NON-NLS-1$

    private static final int DAYS_THRESHOLD = 80;

    /**
     * Use rate limiter with Alpha Vantage. By default, Alpha Vantage allows 5
     * requests per minute. The Guava RateLimiter uses permits per second.
     * However, with 5 permits per 60 seconds, we still get an error message
     * every once in a while. Therefore we are a little bit more conservative
     * when setting up the rate limiter.
     */
    private static RateLimiter rateLimiter = RateLimiter.create((5 - .5) / 60d);

    private String apiKey;

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "Alpha Vantage"; //$NON-NLS-1$
    }

    public void setApiKey(String apiKey)
    {
        this.apiKey = apiKey;
    }

    /**
     * Sets the call frequency limit on AlphaVantage. By default, AlphaVantage
     * allows 5 calls per minute. Various premium API keys allow up to 600 calls
     * per minute.
     * 
     * @param limit
     *            requests per minute
     */
    public void setCallFrequencyLimit(int limit)
    {
        if (limit <= 0)
            throw new IllegalArgumentException();

        rateLimiter.setRate((limit - .5) / 60d);
    }

    @Override
    public boolean updateLatestQuotes(Security security, List<Exception> errors)
    {
        if (security.getTickerSymbol() == null)
        {
            errors.add(new IOException(MessageFormat.format(Messages.MsgMissingTickerSymbol, security.getName())));
            return false;
        }

        if (apiKey == null)
            throw new IllegalArgumentException(Messages.MsgAlphaVantageAPIKeyMissing);

        if (!rateLimiter.tryAcquire())
            throw new RateLimitExceededException(Messages.MsgAlphaVantageRateLimitExceeded);

        try
        {
            @SuppressWarnings("nls")
            String html = new WebAccess("www.alphavantage.co", "/query")
                            .addParameter("function", "TIME_SERIES_INTRADAY") //
                            .addParameter("symbol", security.getTickerSymbol()) //
                            .addParameter("interval", "1min") //
                            .addParameter("apikey", apiKey) //
                            .addParameter("datatype", "csv") //
                            .addParameter("outputsize", "compact") //
                            .get(); // $NON-NLS-1$

            String[] lines = html.split("\\r?\\n"); //$NON-NLS-1$
            if (lines.length <= 2)
                return false;

            // poor man's check
            if (!"timestamp,open,high,low,close,volume".equals(lines[0])) //$NON-NLS-1$
            {
                errors.add(new IOException(MessageFormat.format(Messages.MsgUnexpectedHeader, html)));
                return false;
            }

            String line = lines[1];

            String[] values = line.split(","); //$NON-NLS-1$
            if (values.length != 6)
                throw new IOException(MessageFormat.format(Messages.MsgUnexpectedValue, line));

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"); //$NON-NLS-1$

            LatestSecurityPrice price = new LatestSecurityPrice();

            price.setDate(LocalDate.parse(values[0], formatter));
            price.setValue(asPrice(values[4]));
            price.setHigh(asPrice(values[2]));
            price.setLow(asPrice(values[3]));
            price.setVolume(Long.parseLong(values[5]));
            price.setPreviousClose(LatestSecurityPrice.NOT_AVAILABLE);

            if (price.getValue() != 0)
                security.setLatest(price);

            return true;
        }
        catch (IOException | ParseException e)
        {
            errors.add(e);
            return false;
        }
    }

    @Override
    public boolean updateHistoricalQuotes(Security security, List<Exception> errors)
    {
        OutputSize outputSize = OutputSize.FULL;

        if (!security.getPrices().isEmpty())
        {
            SecurityPrice lastHistoricalQuote = security.getPrices().get(security.getPrices().size() - 1);
            int days = Dates.daysBetween(lastHistoricalQuote.getDate(), LocalDate.now());
            outputSize = days >= DAYS_THRESHOLD ? OutputSize.FULL : OutputSize.COMPACT;
        }

        try
        {
            List<SecurityPrice> prices = getHistoricalQuotes(SecurityPrice.class, security, outputSize, errors);

            boolean isUpdated = false;
            for (SecurityPrice p : prices)
            {
                boolean isAdded = security.addPrice(p);
                isUpdated = isUpdated || isAdded;
            }
            return isUpdated;
        }
        catch (InvocationTargetException | NoSuchMethodException e)
        {
            errors.add(e);
            return false;
        }
    }

    @Override
    public List<LatestSecurityPrice> getHistoricalQuotes(Security security, LocalDate start, List<Exception> errors)
    {
        try
        {
            int days = Dates.daysBetween(start, LocalDate.now());
            return getHistoricalQuotes(LatestSecurityPrice.class, security,
                            days >= DAYS_THRESHOLD ? OutputSize.FULL : OutputSize.COMPACT, errors);
        }
        catch (InvocationTargetException | NoSuchMethodException e)
        {
            errors.add(e);
            return Collections.emptyList();
        }
    }

    private <T extends SecurityPrice> List<T> getHistoricalQuotes(Class<T> klass, Security security,
                    OutputSize outputSize, List<Exception> errors)
                    throws InvocationTargetException, NoSuchMethodException
    {
        if (security.getTickerSymbol() == null)
        {
            errors.add(new IOException(MessageFormat.format(Messages.MsgMissingTickerSymbol, security.getName())));
            return Collections.emptyList();
        }

        if (apiKey == null)
            throw new IllegalArgumentException(Messages.MsgAlphaVantageAPIKeyMissing);

        if (!rateLimiter.tryAcquire())
            throw new RateLimitExceededException(Messages.MsgAlphaVantageRateLimitExceeded);

        try
        {
            @SuppressWarnings("nls")
            String html = new WebAccess("www.alphavantage.co", "/query") //
                            .addParameter("function", "TIME_SERIES_DAILY") //
                            .addParameter("symbol", security.getTickerSymbol()) //
                            .addParameter("apikey", apiKey) //
                            .addParameter("datatype", "csv") //
                            .addParameter("outputsize", outputSize.name().toLowerCase(Locale.US)) //
                            .get();

            String[] lines = html.split("\\r?\\n"); //$NON-NLS-1$
            if (lines.length <= 2)
                return Collections.emptyList();

            // poor man's check
            if (!"timestamp,open,high,low,close,volume".equals(lines[0])) //$NON-NLS-1$
            {
                errors.add(new IOException(MessageFormat.format(Messages.MsgUnexpectedHeader, html)));
                return Collections.emptyList();
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd"); //$NON-NLS-1$

            List<T> prices = new ArrayList<>();

            for (int ii = 1; ii < lines.length; ii++)
            {
                String line = lines[ii];

                String[] values = line.split(","); //$NON-NLS-1$
                if (values.length != 6)
                    throw new IOException(MessageFormat.format(Messages.MsgUnexpectedValue, line));

                T price = klass.getConstructor().newInstance();

                if (values[0].length() > 10)
                    values[0] = values[0].substring(0, 10);

                price.setDate(LocalDate.parse(values[0], formatter));
                price.setValue(asPrice(values[4]));

                if (price instanceof LatestSecurityPrice)
                {
                    LatestSecurityPrice lsp = (LatestSecurityPrice) price;
                    lsp.setHigh(asPrice(values[2]));
                    lsp.setLow(asPrice(values[3]));
                    lsp.setVolume(Long.parseLong(values[5]));
                    lsp.setPreviousClose(LatestSecurityPrice.NOT_AVAILABLE);
                }

                if (price.getValue() != 0)
                    prices.add(price);
            }

            return prices;
        }
        catch (IOException | ParseException | InstantiationException | IllegalAccessException e)
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
