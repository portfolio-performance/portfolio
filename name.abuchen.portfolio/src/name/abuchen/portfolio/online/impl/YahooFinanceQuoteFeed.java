package name.abuchen.portfolio.online.impl;

import static name.abuchen.portfolio.online.impl.YahooHelper.FMT_PRICE;
import static name.abuchen.portfolio.online.impl.YahooHelper.FMT_QUOTE_DATE;
import static name.abuchen.portfolio.online.impl.YahooHelper.asDate;
import static name.abuchen.portfolio.online.impl.YahooHelper.asNumber;
import static name.abuchen.portfolio.online.impl.YahooHelper.asPrice;
import static name.abuchen.portfolio.online.impl.YahooHelper.stripQuotes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

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
    public final boolean updateLatestQuotes(List<Security> securities, List<Exception> errors)
    {
        Map<String, List<Security>> symbol2security = securities.stream()
                        //
                        .filter(s -> s.getTickerSymbol() != null)
                        .collect(Collectors.groupingBy(s -> s.getTickerSymbol().toUpperCase(Locale.ROOT)));

        String symbolString = symbol2security.keySet().stream().collect(Collectors.joining("+")); //$NON-NLS-1$

        boolean isUpdated = false;

        String url = MessageFormat.format(LATEST_URL, symbolString.toString());

        try (BufferedReader reader = openReader(url, errors))
        {
            if (reader == null)
                return false;

            String line = null;
            while ((line = reader.readLine()) != null)
            {
                String[] values = line.split(","); //$NON-NLS-1$
                if (values.length != 7)
                {
                    errors.add(new IOException(MessageFormat.format(Messages.MsgUnexpectedValue, line)));
                    return false;
                }

                String symbol = stripQuotes(values[0]);
                List<Security> forSymbol = symbol2security.remove(symbol);
                if (forSymbol == null)
                {
                    errors.add(new IOException(MessageFormat.format(Messages.MsgUnexpectedSymbol, symbol, line)));
                    continue;
                }

                try
                {
                    LatestSecurityPrice price = buildPrice(values);

                    for (Security security : forSymbol)
                    {
                        boolean isAdded = security.setLatest(price);
                        isUpdated = isUpdated || isAdded;
                    }
                }
                catch (NumberFormatException | ParseException e)
                {
                    errors.add(new IOException(MessageFormat.format(Messages.MsgErrorsConvertingValue, line), e));
                }
            }

            for (String symbol : symbol2security.keySet())
                errors.add(new IOException(MessageFormat.format(Messages.MsgMissingResponse, symbol)));
        }
        catch (IOException e)
        {
            errors.add(e);
        }

        return isUpdated;
    }

    private LatestSecurityPrice buildPrice(String[] values) throws ParseException
    {
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

        return price;
    }

    @Override
    public final boolean updateHistoricalQuotes(Security security, List<Exception> errors)
    {
        Calendar start = caculateStart(security);

        List<SecurityPrice> quotes = internalGetQuotes(SecurityPrice.class, security, start.getTime(), errors);

        boolean isUpdated = false;
        if (quotes != null)
        {
            for (SecurityPrice p : quotes)
            {
                boolean isAdded = security.addPrice(p);
                isUpdated = isUpdated || isAdded;
            }
        }
        return isUpdated;
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
    {
        return internalGetQuotes(LatestSecurityPrice.class, security, start, errors);
    }

    @Override
    public List<LatestSecurityPrice> getHistoricalQuotes(String response, List<Exception> errors)
    {
        throw new UnsupportedOperationException();
    }

    private <T extends SecurityPrice> List<T> internalGetQuotes(Class<T> klass, Security security, Date startDate,
                    List<Exception> errors)
    {
        if (security.getTickerSymbol() == null)
        {
            errors.add(new IOException(MessageFormat.format(Messages.MsgMissingTickerSymbol, security.getName())));
            return Collections.emptyList();
        }

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

        String line = null;
        try (BufferedReader reader = openReader(wknUrl, errors))
        {
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
        catch (NumberFormatException | ParseException e)
        {
            errors.add(new IOException(MessageFormat.format(Messages.MsgErrorsConvertingValue, line), e));
        }
        catch (InstantiationException | IllegalAccessException | IOException e)
        {
            errors.add(e);
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
        List<Exchange> answer = new ArrayList<Exchange>();

        String symbol = subject.getTickerSymbol();

        // if symbol is null, return empty list
        if (symbol == null || symbol.trim().length() == 0)
            return answer;

        // strip away exchange suffix to search for all available exchanges
        int p = symbol.indexOf('.');
        String prefix = p >= 0 ? symbol.substring(0, p + 1) : symbol + "."; //$NON-NLS-1$

        // http://stackoverflow.com/questions/885456/stock-ticker-symbol-lookup-api
        String searchUrl = MessageFormat.format(SEARCH_URL, prefix);

        try (Scanner scanner = new Scanner(openStream(searchUrl)))
        {
            String html = scanner.useDelimiter("\\A").next(); //$NON-NLS-1$

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

        // Issue #251
        // sometimes Yahoo does not return the default exchange which prevents
        // selecting this security (example: searching for GOOG does return only
        // unimportant exchanges)
        Optional<Exchange> defaultExchange = answer.stream() //
                        .filter(e -> e.getId().equals(subject.getTickerSymbol())).findAny();
        if (!defaultExchange.isPresent())
            answer.add(new Exchange(subject.getTickerSymbol(), subject.getTickerSymbol()));

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
