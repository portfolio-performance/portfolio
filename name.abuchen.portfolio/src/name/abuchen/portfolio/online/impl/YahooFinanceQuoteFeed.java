package name.abuchen.portfolio.online.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import name.abuchen.portfolio.util.Dates;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

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

    private static final String SEARCH_URL = "http://d.yimg.com/autoc.finance.yahoo.com/autoc?query={0}&callback=YAHOO.Finance.SymbolSuggest.ssCallback"; //$NON-NLS-1$

    protected static final ThreadLocal<DecimalFormat> FMT_PRICE = new ThreadLocal<DecimalFormat>()
    {
        protected DecimalFormat initialValue()
        {
            return new DecimalFormat("0.###", new DecimalFormatSymbols(Locale.US)); //$NON-NLS-1$
        }
    };

    protected static final ThreadLocal<SimpleDateFormat> FMT_TRADE_DATE = new ThreadLocal<SimpleDateFormat>()
    {
        protected SimpleDateFormat initialValue()
        {
            return new SimpleDateFormat("\"MM/dd/yyyy\""); //$NON-NLS-1$
        }
    };

    protected static final ThreadLocal<SimpleDateFormat> FMT_QUOTE_DATE = new ThreadLocal<SimpleDateFormat>()
    {
        protected SimpleDateFormat initialValue()
        {
            return new SimpleDateFormat("yyyy-MM-dd"); //$NON-NLS-1$
        }
    };

    @SuppressWarnings("nls")
    private static final String HISTORICAL_URL = "http://ichart.finance.yahoo.com/table.csv?ignore=.csv" //
                    + "&s={0}" // ticker symbol
                    + "&a={1}&b={2}&c={3}" // begin
                    + "&d={4}&e={5}&f={6}" // end
                    + "&g=d"; // daily

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
    public final void updateLatestQuotes(List<Security> securities, List<Exception> errors) throws IOException
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
            requested.put(security.getTickerSymbol().toUpperCase(), security);
        }

        String url = MessageFormat.format(LATEST_URL, symbolString.toString());

        BufferedReader reader = null;
        String line = null;

        try
        {
            reader = openReader(url, errors);
            if (reader == null)
                return;

            while ((line = reader.readLine()) != null)
            {
                String[] values = line.split(","); //$NON-NLS-1$
                if (values.length != 7)
                {
                    errors.add(new IOException(MessageFormat.format(Messages.MsgUnexpectedValue, line)));
                    return;
                }

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
            errors.add(new IOException(MessageFormat.format(Messages.MsgErrorsConvertingValue, line), e));
        }
        catch (ParseException e)
        {
            errors.add(new IOException(MessageFormat.format(Messages.MsgErrorsConvertingValue, line), e));
        }
        finally
        {
            if (reader != null)
                reader.close();
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
    public final void updateHistoricalQuotes(Security security, List<Exception> errors) throws IOException
    {
        Calendar start = caculateStart(security);

        List<SecurityPrice> quotes = internalGetQuotes(SecurityPrice.class, security, start.getTime(), errors);
        if (quotes != null)
            for (SecurityPrice p : quotes)
                security.addPrice(p);
    }

    /* package */final Calendar caculateStart(Security security)
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
    public final List<LatestSecurityPrice> getHistoricalQuotes(Security security, Date start, List<Exception> errors)
                    throws IOException
    {
        return internalGetQuotes(LatestSecurityPrice.class, security, start, errors);
    }

    @Override
    public List<LatestSecurityPrice> getHistoricalQuotes(String response, List<Exception> errors) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    private <T extends SecurityPrice> List<T> internalGetQuotes(Class<T> klass, Security security, Date startDate,
                    List<Exception> errors) throws IOException
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

        BufferedReader reader = null;
        String line = null;

        try
        {
            reader = openReader(wknUrl, errors);
            if (reader == null)
                return answer;

            line = reader.readLine();

            // poor man's check
            if (!"Date,Open,High,Low,Close,Volume,Adj Close".equals(line)) //$NON-NLS-1$
                throw new IOException(MessageFormat.format(Messages.MsgUnexpectedHeader, line));

            DecimalFormat priceFormat = FMT_PRICE.get();
            SimpleDateFormat dateFormat = FMT_QUOTE_DATE.get();

            while ((line = reader.readLine()) != null)
            {
                String[] values = line.split(","); //$NON-NLS-1$
                if (values.length != 7)
                    throw new IOException(MessageFormat.format(Messages.MsgUnexpectedValue, line));

                T price = klass.newInstance();

                fillValues(values, price, priceFormat, dateFormat);

                answer.add(price);
            }
        }
        catch (NumberFormatException e)
        {
            errors.add(new IOException(MessageFormat.format(Messages.MsgErrorsConvertingValue, line), e));
        }
        catch (ParseException e)
        {
            errors.add(new IOException(MessageFormat.format(Messages.MsgErrorsConvertingValue, line), e));
        }
        catch (InstantiationException e)
        {
            errors.add(e);
        }
        catch (IllegalAccessException e)
        {
            errors.add(e);
        }
        finally
        {
            if (reader != null)
                reader.close();
        }

        return answer;
    }

    protected <T extends SecurityPrice> void fillValues(String[] values, T price, DecimalFormat priceFormat,
                    SimpleDateFormat dateFormat) throws ParseException
    {
        Date date = dateFormat.parse(values[0]);

        Number q = priceFormat.parse(values[4]);
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
    }

    @Override
    public final List<Exchange> getExchanges(Security subject, List<Exception> errors)
    {
        // strip away exchange suffix to search for all available exchanges
        String symbol = subject.getTickerSymbol();
        int p = symbol.indexOf('.');
        String prefix = p >= 0 ? symbol.substring(0, p + 1) : symbol + "."; //$NON-NLS-1$

        List<Exchange> answer = new ArrayList<Exchange>();

        // http://stackoverflow.com/questions/885456/stock-ticker-symbol-lookup-api
        String searchUrl = MessageFormat.format(SEARCH_URL, prefix);

        try
        {
            String html = new java.util.Scanner(openStream(searchUrl)).useDelimiter("\\A").next(); //$NON-NLS-1$

            // strip away java script call back method
            p = html.indexOf('(');
            html = html.substring(p + 1, html.length() - 1);

            JSONObject response = (JSONObject) JSONValue.parse(html);
            if (response != null)
            {
                JSONObject resultSet = (JSONObject) response.get("ResultSet"); //$NON-NLS-1$
                if (resultSet != null)
                {
                    JSONArray result = (JSONArray) resultSet.get("Result"); //$NON-NLS-1$
                    if (result != null)
                    {
                        for (int ii = 0; ii < result.size(); ii++)
                            answer.add(createExchange(((JSONObject) result.get(ii)).get("symbol").toString())); //$NON-NLS-1$
                    }
                }
            }
        }
        catch (IOException e)
        {
            errors.add(e);
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

    protected BufferedReader openReader(String url, List<Exception> errors)
    {
        try
        {
            return new BufferedReader(new InputStreamReader(openStream(url)));
        }
        catch (IOException e)
        {
            errors.add(e);
        }
        return null;
    }

    /* enable testing */
    protected InputStream openStream(String wknUrl) throws IOException
    {
        return new URL(wknUrl).openStream();
    }
}
