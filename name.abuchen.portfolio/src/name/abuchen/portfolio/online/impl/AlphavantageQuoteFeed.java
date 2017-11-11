package name.abuchen.portfolio.online.impl;

import static name.abuchen.portfolio.online.impl.YahooHelper.asPrice;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.util.Dates;

public class AlphavantageQuoteFeed implements QuoteFeed
{
    private enum OutputSize
    {
        COMPACT, FULL
    }

    public static final String ID = "ALPHAVANTAGE"; //$NON-NLS-1$

    private static final int DAYS_THRESHOLD = 80;

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

    @Override
    public boolean updateLatestQuotes(Security security, List<Exception> errors)
    {
        if (apiKey == null)
            throw new IllegalArgumentException(Messages.MsgAlphaVantageAPIKeyMissing);

        String wknUrl = MessageFormat.format("https://www.alphavantage.co/query?function=TIME_SERIES_INTRADAY" //$NON-NLS-1$
                        + "&symbol={0}&interval=1min&apikey={1}&datatype=csv&outputsize=compact", //$NON-NLS-1$
                        security.getTickerSymbol(), apiKey);

        try
        {
            URL obj = new URL(wknUrl);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setConnectTimeout(1000);
            con.setReadTimeout(20000);

            int responseCode = con.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK)
                throw new IOException(wknUrl + " --> " + responseCode); //$NON-NLS-1$

            try (Scanner scanner = new Scanner(con.getInputStream(), StandardCharsets.UTF_8.name()))
            {
                String body = scanner.useDelimiter("\\A").next(); //$NON-NLS-1$

                String[] lines = body.split("\\r?\\n"); //$NON-NLS-1$
                if (lines.length <= 2)
                    return false;

                // poor man's check
                if (!"timestamp,open,high,low,close,volume".equals(lines[0])) //$NON-NLS-1$
                {
                    errors.add(new IOException(MessageFormat.format(Messages.MsgUnexpectedHeader, body)));
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

                security.setLatest(price);

                return true;
            }

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

        List<SecurityPrice> prices = getHistoricalQuotes(SecurityPrice.class, security, outputSize, errors);

        boolean isUpdated = false;
        for (SecurityPrice p : prices)
        {
            boolean isAdded = security.addPrice(p);
            isUpdated = isUpdated || isAdded;
        }
        return isUpdated;
    }

    @Override
    public List<LatestSecurityPrice> getHistoricalQuotes(Security security, LocalDate start, List<Exception> errors)
    {
        int days = Dates.daysBetween(start, LocalDate.now());
        return getHistoricalQuotes(LatestSecurityPrice.class, security,
                        days >= DAYS_THRESHOLD ? OutputSize.FULL : OutputSize.COMPACT, errors);
    }

    private <T extends SecurityPrice> List<T> getHistoricalQuotes(Class<T> klass, Security security,
                    OutputSize outputSize, List<Exception> errors)
    {
        if (apiKey == null)
            throw new IllegalArgumentException(Messages.MsgAlphaVantageAPIKeyMissing);

        String wknUrl = MessageFormat.format("https://www.alphavantage.co/query?function=TIME_SERIES_DAILY" //$NON-NLS-1$
                        + "&symbol={0}&apikey={1}&datatype=csv&outputsize={2}", //$NON-NLS-1$
                        security.getTickerSymbol(), apiKey, outputSize.name().toLowerCase(Locale.US));

        try
        {
            URL obj = new URL(wknUrl);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setConnectTimeout(1000);
            con.setReadTimeout(20000);

            int responseCode = con.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK)
                throw new IOException(wknUrl + " --> " + responseCode); //$NON-NLS-1$

            try (Scanner scanner = new Scanner(con.getInputStream(), StandardCharsets.UTF_8.name()))
            {
                String body = scanner.useDelimiter("\\A").next(); //$NON-NLS-1$

                String[] lines = body.split("\\r?\\n"); //$NON-NLS-1$
                if (lines.length <= 2)
                    return Collections.emptyList();

                // poor man's check
                if (!"timestamp,open,high,low,close,volume".equals(lines[0])) //$NON-NLS-1$
                {
                    errors.add(new IOException(MessageFormat.format(Messages.MsgUnexpectedHeader, lines[0])));
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

                    T price = klass.newInstance();
                    
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

                    prices.add(price);
                }

                return prices;
            }
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
