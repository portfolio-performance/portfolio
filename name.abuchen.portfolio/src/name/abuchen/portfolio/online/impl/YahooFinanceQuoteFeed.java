package name.abuchen.portfolio.online.impl;

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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.SecuritySearchProvider;
import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;
import name.abuchen.portfolio.util.Dates;

public class YahooFinanceQuoteFeed implements QuoteFeed
{
    public static final String ID = "YAHOO"; //$NON-NLS-1$

    private static final String LATEST_URL = "http://finance.yahoo.com/d/quotes.csv?s={0}&f=sl1d1hgpv"; //$NON-NLS-1$
    // s = symbol
    // l1 = last trade (price only)
    // d1 = last trade date
    // h = day's high
    // g = day's low
    // p = previous close
    // v = volume
    // Source = http://cliffngan.net/a/13

    private static final ThreadLocal<DecimalFormat> FMT_PRICE = new ThreadLocal<DecimalFormat>()
    {
        protected DecimalFormat initialValue()
        {
            return new DecimalFormat("0.###", new DecimalFormatSymbols(Locale.US)); //$NON-NLS-1$
        }
    };

    private static final ThreadLocal<SimpleDateFormat> FMT_TRADE_DATE = new ThreadLocal<SimpleDateFormat>()
    {
        protected SimpleDateFormat initialValue()
        {
            return new SimpleDateFormat("\"MM/dd/yyyy\""); //$NON-NLS-1$
        }
    };

    @SuppressWarnings("nls")
    private static final String HISTORICAL_URL = "http://ichart.finance.yahoo.com/table.csv?ignore=.csv" //
                    + "&s={0}" // ticker symbol
                    + "&a={1}&b={2}&c={3}" // begin
                    + "&d={4}&e={5}&f={6}" // end
                    + "&g=d"; // daily

    private SecuritySearchProvider searchProvider = new YahooSearchProvider();

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return Messages.LabelYahooFinance;
    }

    @Override
    public void updateLatestQuotes(List<Security> securities, List<Exception> errors) throws IOException
    {
        Map<String, Security> requested = new HashMap<String, Security>();

        StringBuilder symbolString = new StringBuilder();
        for (Security security : securities)
        {
            if (security.getTickerSymbol() == null)
                continue;

            if (symbolString.length() > 0)
                symbolString.append("+"); //$NON-NLS-1$
            symbolString.append(security.getTickerSymbol());
            requested.put(security.getTickerSymbol(), security);
        }

        String url = MessageFormat.format(LATEST_URL, symbolString.toString());

        InputStream is = null;
        String line = null;

        try
        {
            is = openStream(url);
            BufferedReader dis = new BufferedReader(new InputStreamReader(is));

            while ((line = dis.readLine()) != null)
            {
                String[] values = line.split(","); //$NON-NLS-1$
                if (values.length != 7)
                    throw new IOException(MessageFormat.format(Messages.MsgUnexpectedValue, line));

                String symbol = stripQuotes(values[0]);

                Security security = requested.remove(symbol);
                if (security == null)
                {
                    errors.add(new IOException(MessageFormat.format(Messages.MsgUnexpectedSymbol, symbol, line)));
                    continue;
                }

                long lastTrade = asPrice(values[1]);

                Date lastTradeDate = asDate(values[2]);
                if (lastTradeDate == null) // can't work w/o date
                    lastTradeDate = Dates.today();

                long daysHigh = asPrice(values[3]);

                long daysLow = asPrice(values[4]);

                long previousClose = asPrice(values[5]);

                int volume = asNumber(values[6]);

                LatestSecurityPrice price = new LatestSecurityPrice(lastTradeDate, lastTrade);
                price.setHigh(daysHigh);
                price.setLow(daysLow);
                price.setPreviousClose(previousClose);
                price.setVolume(volume);

                security.setLatest(price);
            }

            for (Security s : requested.values())
                errors.add(new IOException(MessageFormat.format(Messages.MsgMissingResponse, s.getTickerSymbol())));
        }
        catch (NumberFormatException e)
        {
            throw new IOException(MessageFormat.format(Messages.MsgErrorsConvertingValue, line), e);
        }
        catch (ParseException e)
        {
            throw new IOException(MessageFormat.format(Messages.MsgErrorsConvertingValue, line), e);
        }
        finally
        {
            if (is != null)
                is.close();
        }

    }

    private long asPrice(String s) throws ParseException
    {
        if ("N/A".equals(s)) //$NON-NLS-1$
            return -1;
        return (long) (FMT_PRICE.get().parse(s).doubleValue() * 100);
    }

    private int asNumber(String s) throws ParseException
    {
        if ("N/A".equals(s)) //$NON-NLS-1$
            return -1;
        return FMT_PRICE.get().parse(s).intValue();
    }

    private Date asDate(String s) throws ParseException
    {
        if ("\"N/A\"".equals(s)) //$NON-NLS-1$
            return null;
        return FMT_TRADE_DATE.get().parse(s);
    }

    private String stripQuotes(String s)
    {
        return s.substring(1, s.length() - 1);
    }

    @Override
    public void updateHistoricalQuotes(Security security) throws IOException
    {
        Calendar start = caculateStart(security);

        List<SecurityPrice> quotes = internalGetQuotes(SecurityPrice.class, security, start.getTime());
        for (SecurityPrice p : quotes)
            security.addPrice(p);
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

    @Override
    public List<LatestSecurityPrice> getHistoricalQuotes(Security security, Date start) throws IOException
    {
        return internalGetQuotes(LatestSecurityPrice.class, security, start);
    }

    private <T extends SecurityPrice> List<T> internalGetQuotes(Class<T> klass, Security security, Date startDate)
                    throws IOException
    {
        if (security.getTickerSymbol() == null)
            throw new IOException(MessageFormat.format(Messages.MsgMissingTickerSymbol, security.getName()));

        Calendar start = Calendar.getInstance();
        start.setTime(startDate);
        Calendar stop = Calendar.getInstance();

        String wknUrl = MessageFormat.format(HISTORICAL_URL, //
                        security.getTickerSymbol(), //
                        start.get(Calendar.MONTH), //
                        start.get(Calendar.DATE), //
                        Integer.toString(start.get(Calendar.YEAR)), //
                        stop.get(Calendar.MONTH), //
                        stop.get(Calendar.DATE), //
                        Integer.toString(stop.get(Calendar.YEAR)));

        List<T> answer = new ArrayList<T>();

        InputStream is = null;
        String line = null;

        try
        {
            is = openStream(wknUrl);
            BufferedReader dis = new BufferedReader(new InputStreamReader(is));

            line = dis.readLine();

            // poor man's check
            if (!"Date,Open,High,Low,Close,Volume,Adj Close".equals(line)) //$NON-NLS-1$
                throw new IOException(MessageFormat.format(Messages.MsgUnexpectedHeader, line));

            DecimalFormat priceFormat = FMT_PRICE.get();

            while ((line = dis.readLine()) != null)
            {
                String[] values = line.split(","); //$NON-NLS-1$
                if (values.length != 7)
                    throw new IOException(MessageFormat.format(Messages.MsgUnexpectedValue, line));

                T price = klass.newInstance();

                SimpleDateFormat dfmt = new SimpleDateFormat("yyyy-MM-dd"); //$NON-NLS-1$
                Date date = dfmt.parse(values[0]);

                Number q = priceFormat.parse(values[6]);
                long v = (long) (q.doubleValue() * 100);

                price.setTime(date);
                price.setValue(v);

                if (price instanceof LatestSecurityPrice)
                {
                    LatestSecurityPrice latest = (LatestSecurityPrice) price;

                    q = priceFormat.parse(values[5]);
                    latest.setVolume(q.intValue());

                    q = priceFormat.parse(values[2]);
                    latest.setHigh((long) (q.doubleValue() * 100));

                    q = priceFormat.parse(values[3]);
                    latest.setLow((long) (q.doubleValue() * 100));
                }

                answer.add(price);
            }
        }
        catch (NumberFormatException e)
        {
            throw new IOException(MessageFormat.format(Messages.MsgErrorsConvertingValue, line), e);
        }
        catch (ParseException e)
        {
            throw new IOException(MessageFormat.format(Messages.MsgErrorsConvertingValue, line), e);
        }
        catch (InstantiationException e)
        {
            throw new IOException(e);
        }
        catch (IllegalAccessException e)
        {
            throw new IOException(e);
        }
        finally
        {
            if (is != null)
                is.close();
        }

        return answer;
    }

    @Override
    public List<Exchange> getExchanges(Security subject) throws IOException
    {
        String symbol = subject.getTickerSymbol();
        int p = symbol.indexOf('.');
        String prefix = p >= 0 ? symbol.substring(0, p + 1) : symbol + "."; //$NON-NLS-1$
        if (p >= 0)
            symbol = symbol.substring(0, p);

        List<Exchange> answer = new ArrayList<Exchange>();

        List<ResultItem> result = searchProvider.search(prefix);
        for (ResultItem item : result)
        {
            if (item.getSymbol() != null && (symbol.equals(item.getSymbol()) || item.getSymbol().startsWith(prefix)))
            {
                answer.add(createExchange(item.getSymbol()));
            }
        }

        if (answer.isEmpty())
        {
            // Issue #29
            // at least add the given ticker symbol if the search returns
            // nothing (sometimes accidentally)
            answer.add(createExchange(subject.getTickerSymbol()));
        }

        return answer;
    }

    private Exchange createExchange(String symbol)
    {
        int e = symbol.indexOf('.');
        String exchange = e >= 0 ? symbol.substring(e) : ".default"; //$NON-NLS-1$
        String label = ExchangeLabels.getString("yahoo" + exchange); //$NON-NLS-1$
        return new Exchange(symbol, String.format("%s (%s)", label, symbol)); //$NON-NLS-1$
    }

    /* enable testing */
    protected InputStream openStream(String wknUrl) throws MalformedURLException, IOException
    {
        return new URL(wknUrl).openStream();
    }

    /* enable testing */
    /* package */void setSearchProvider(SecuritySearchProvider searchProvider)
    {
        this.searchProvider = searchProvider;
    }

}
