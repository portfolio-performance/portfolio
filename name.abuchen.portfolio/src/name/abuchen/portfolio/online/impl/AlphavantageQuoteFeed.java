package name.abuchen.portfolio.online.impl;

import static name.abuchen.portfolio.online.impl.YahooHelper.asPrice;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

import com.google.common.util.concurrent.RateLimiter;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.QuoteFeedData;
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
    public Optional<LatestSecurityPrice> getLatestQuote(Security security)
    {
        if (security.getTickerSymbol() == null)
        {
            PortfolioLog.error(MessageFormat.format(Messages.MsgMissingTickerSymbol, security.getName()));
            return Optional.empty();
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
                return Optional.empty();

            // poor man's check
            if (!"timestamp,open,high,low,close,volume".equals(lines[0])) //$NON-NLS-1$
            {
                PortfolioLog.error(MessageFormat.format(Messages.MsgUnexpectedHeader, html));
                return Optional.empty();
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
                return Optional.of(price);
        }
        catch (IOException | ParseException e)
        {
            PortfolioLog.error(e);
        }

        return Optional.empty();
    }

    @Override
    public QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse)
    {
        OutputSize outputSize = OutputSize.FULL;

        if (!security.getPrices().isEmpty())
        {
            SecurityPrice lastHistoricalQuote = security.getPrices().get(security.getPrices().size() - 1);
            int days = Dates.daysBetween(lastHistoricalQuote.getDate(), LocalDate.now());
            outputSize = days >= DAYS_THRESHOLD ? OutputSize.FULL : OutputSize.COMPACT;
        }

        return getHistoricalQuotes(security, collectRawResponse, outputSize);
    }

    @Override
    public QuoteFeedData previewHistoricalQuotes(Security security)
    {
        LocalDate now = LocalDate.now();
        int days = Dates.daysBetween(now.minusMonths(2), now);
        return getHistoricalQuotes(security, true, days >= DAYS_THRESHOLD ? OutputSize.FULL : OutputSize.COMPACT);
    }

    private QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse, OutputSize outputSize)
    {
        if (security.getTickerSymbol() == null)
            return QuoteFeedData.withError(
                            new IOException(MessageFormat.format(Messages.MsgMissingTickerSymbol, security.getName())));

        if (apiKey == null)
            throw new IllegalArgumentException(Messages.MsgAlphaVantageAPIKeyMissing);

        if (!rateLimiter.tryAcquire())
            throw new RateLimitExceededException(Messages.MsgAlphaVantageRateLimitExceeded);

        QuoteFeedData data = new QuoteFeedData();

        try
        {
            @SuppressWarnings("nls")
            WebAccess webaccess = new WebAccess("www.alphavantage.co", "/query") //
                            .addParameter("function", "TIME_SERIES_DAILY") //
                            .addParameter("symbol", security.getTickerSymbol()) //
                            .addParameter("apikey", apiKey) //
                            .addParameter("datatype", "csv") //
                            .addParameter("outputsize", outputSize.name().toLowerCase(Locale.US));
            String html = webaccess.get();

            if (collectRawResponse)
                data.addResponse(webaccess.getURL(), html);

            String[] lines = html.split("\\r?\\n"); //$NON-NLS-1$
            if (lines.length <= 2)
                return data;

            // poor man's check
            if (!"timestamp,open,high,low,close,volume".equals(lines[0])) //$NON-NLS-1$
            {
                data.addError(new IOException(MessageFormat.format(Messages.MsgUnexpectedHeader, html)));
                return data;
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd"); //$NON-NLS-1$

            for (int ii = 1; ii < lines.length; ii++)
            {
                String line = lines[ii];

                String[] values = line.split(","); //$NON-NLS-1$
                if (values.length != 6)
                    throw new IOException(MessageFormat.format(Messages.MsgUnexpectedValue, line));

                LatestSecurityPrice price = new LatestSecurityPrice();

                if (values[0].length() > 10)
                    values[0] = values[0].substring(0, 10);

                price.setDate(LocalDate.parse(values[0], formatter));
                price.setValue(asPrice(values[4]));

                price.setHigh(asPrice(values[2]));
                price.setLow(asPrice(values[3]));
                price.setVolume(Long.parseLong(values[5]));
                price.setPreviousClose(LatestSecurityPrice.NOT_AVAILABLE);

                if (price.getValue() != 0)
                    data.addPrice(price);
            }

        }
        catch (IOException | ParseException | URISyntaxException e)
        {
            data.addError(e);
        }

        return data;
    }
}
