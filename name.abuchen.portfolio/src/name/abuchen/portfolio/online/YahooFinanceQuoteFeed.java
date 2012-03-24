package name.abuchen.portfolio.online;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.util.Dates;

public class YahooFinanceQuoteFeed implements QuoteFeed
{
    private static final String LATEST_URL = "http://finance.yahoo.com/d/quotes.csv?s={0}&f=l1d1hgpv"; //$NON-NLS-1$
    // l1 = last trade (price only)
    // d1 = last trade date
    // h = day's high
    // g = day's low
    // p = previous close
    // v = volume
    // Source = http://cliffngan.net/a/13

    private static final DecimalFormat FMT_PRICE = new DecimalFormat("0.###", new DecimalFormatSymbols(Locale.US)); //$NON-NLS-1$

    private final SimpleDateFormat fmtTradeDate = new SimpleDateFormat("\"MM/dd/yyyy\""); //$NON-NLS-1$

    @SuppressWarnings("nls")
    private static final String HISTORICAL_URL = "http://ichart.finance.yahoo.com/table.csv?ignore=.csv" //
                    + "&s={0}" // ticker symbol
                    + "&a={1}&b={2}&c={3}" // begin
                    + "&d={4}&e={5}&f={6}" // end
                    + "&g=d"; // daily

    @Override
    public String getId()
    {
        return "YAHOO"; //$NON-NLS-1$
    }

    @Override
    public String getName()
    {
        return Messages.LabelYahooFinance;
    }

    @Override
    public void updateLatestQuote(List<Security> securities) throws IOException
    {
        List<Security> requested = new ArrayList<Security>();

        StringBuilder symbolString = new StringBuilder();
        for (Security security : securities)
        {
            if (security.getTickerSymbol() == null)
                continue;

            if (symbolString.length() > 0)
                symbolString.append("+"); //$NON-NLS-1$
            symbolString.append(security.getTickerSymbol());
            requested.add(security);
        }

        String url = MessageFormat.format(LATEST_URL, symbolString.toString());

        InputStream is = null;

        try
        {
            is = openStream(url);
            BufferedReader dis = new BufferedReader(new InputStreamReader(is));

            for (Security security : requested)
            {
                String line = dis.readLine();
                if (line == null)
                    throw new IOException(MessageFormat.format(Messages.MsgMissingResponse, security));

                String[] values = line.split(","); //$NON-NLS-1$
                if (values.length != 6)
                    throw new IOException(MessageFormat.format(Messages.MsgUnexpectedValue, line));

                int lastTrade = asPrice(values[0]);

                Date lastTradeDate = asDate(values[1]);
                if (lastTradeDate == null) // can't work w/o date
                    lastTradeDate = Dates.today();

                int daysHigh = asPrice(values[2]);

                int daysLow = asPrice(values[3]);

                int previousClose = asPrice(values[4]);

                int volume = asNumber(values[5]);

                LatestSecurityPrice price = new LatestSecurityPrice(lastTradeDate, lastTrade);
                price.setHigh(daysHigh);
                price.setLow(daysLow);
                price.setPreviousClose(previousClose);
                price.setVolume(volume);

                security.setLatest(price);
            }
        }
        catch (ParseException e)
        {
            throw new IOException(Messages.MsgErrorsConvertingValue, e);
        }
        finally
        {
            if (is != null)
                is.close();
        }

    }

    private int asPrice(String s) throws ParseException
    {
        if ("N/A".equals(s)) //$NON-NLS-1$
            return -1;
        return (int) (FMT_PRICE.parse(s).doubleValue() * 100);
    }

    private int asNumber(String s) throws ParseException
    {
        if ("N/A".equals(s)) //$NON-NLS-1$
            return -1;
        return FMT_PRICE.parse(s).intValue();
    }

    private Date asDate(String s) throws ParseException
    {
        if ("\"N/A\"".equals(s)) //$NON-NLS-1$
            return null;
        return fmtTradeDate.parse(s);
    }

    @Override
    public void updateHistoricalQuotes(Security security) throws IOException
    {
        // http://ichart.finance.yahoo.com/table.csv?s=DAI.DE&d=8&e=26&f=2011&g=d&a=0&b=1&c=2003&ignore=.csv

        // Date,Open,High,Low,Close,Volume,Adj Close
        // 2011-09-22,34.10,34.16,32.35,32.74,10825200,32.74

        if (security.getTickerSymbol() == null)
            throw new IOException(MessageFormat.format(Messages.MsgMissingTickerSymbol, security.getName()));

        Calendar start = caculateStart(security);
        Calendar stop = Calendar.getInstance();

        String wknUrl = MessageFormat.format(HISTORICAL_URL, //
                        security.getTickerSymbol(), //
                        start.get(Calendar.MONTH), //
                        start.get(Calendar.DATE), //
                        Integer.toString(start.get(Calendar.YEAR)), //
                        stop.get(Calendar.MONTH), //
                        stop.get(Calendar.DATE), //
                        Integer.toString(stop.get(Calendar.YEAR)));

        InputStream is = null;

        try
        {
            is = openStream(wknUrl);
            BufferedReader dis = new BufferedReader(new InputStreamReader(is));

            String line = dis.readLine();

            // poor man's check
            if (!"Date,Open,High,Low,Close,Volume,Adj Close".equals(line)) //$NON-NLS-1$
                throw new IOException(MessageFormat.format(Messages.MsgUnexpectedHeader, line));

            while ((line = dis.readLine()) != null)
            {
                String[] values = line.split(","); //$NON-NLS-1$
                if (values.length != 7)
                    throw new IOException(MessageFormat.format(Messages.MsgUnexpectedValue, line));

                SimpleDateFormat dfmt = new SimpleDateFormat("yyyy-MM-dd"); //$NON-NLS-1$
                Date date = dfmt.parse(values[0]);

                Number q = FMT_PRICE.parse(values[6]);
                int v = (int) (q.doubleValue() * 100);

                SecurityPrice price = new SecurityPrice(date, v);
                security.addPrice(price);
            }
        }
        catch (ParseException e)
        {
            throw new IOException(Messages.MsgErrorsConvertingValue, e);
        }
        finally
        {
            if (is != null)
                is.close();
        }
    }

    /* package */Calendar caculateStart(Security security)
    {
        Calendar start = Calendar.getInstance();
        start.setTime(Dates.today());

        if (!security.getPrices().isEmpty())
        {
            SecurityPrice lastHistoricalQuote = security.getPrices().get(security.getPrices().size() - 1);
            start.setTime(lastHistoricalQuote.getTime());
        }
        else
        {
            start.add(Calendar.YEAR, -5);
        }
        return start;
    }

    /* enable testing */
    protected InputStream openStream(String wknUrl) throws MalformedURLException, IOException
    {
        return new URL(wknUrl).openStream();
    }

}
