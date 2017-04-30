package name.abuchen.portfolio.online.impl;

import static name.abuchen.portfolio.online.impl.YahooHelper.FMT_PRICE;
import static name.abuchen.portfolio.online.impl.YahooHelper.asDate;
import static name.abuchen.portfolio.online.impl.YahooHelper.asDouble;
//import static name.abuchen.portfolio.online.impl.YahooHelper.asNumber;
import static name.abuchen.portfolio.online.impl.YahooHelper.asPrice;
import static name.abuchen.portfolio.online.impl.YahooHelper.stripQuotes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityElement;
import name.abuchen.portfolio.model.SecurityEvent;
//import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.online.EventFeed;

public class YahooFinanceEventFeed extends EventFeed
{
    public static final String ID = YAHOO; //$NON-NLS-1$
    // Source = https://greenido.wordpress.com/2009/12/22/work-like-a-pro-with-yahoo-finance-hidden-api/
    // http://brusdeylins.info/tips_and_tricks/yahoo-finance-api/

    private static final String LATEST_URL = "http://download.finance.yahoo.com/d/quotes.csv?s={0}&f=sdqr1yc4"; //$NON-NLS-1$
    // s  = symbol
    // d  = dividend/share
    // q  = dividend ex-day 
    // r1 = dividend pay day
    // y  = dividend yield

    //private static final String CURRENCY_URL = "http://download.finance.yahoo.com/d/quotes.csv?s={0}&f=sc4"; //$NON-NLS-1$
    // s  = symbol
    // c4 = currency

    @SuppressWarnings("nls")
    private static final String HISTORICAL_URL = "http://ichart.finance.yahoo.com/x?ignore=.csv" //
                    + "&s={0}" // ticker symbol
                    + "&a={1}&b={2}&c={3}" // begin
                    + "&d={4}&e={5}&f={6}" // end
                    + "&g=v"; // dividends&splits

    
    
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
    public final boolean update(List<Security> securities, List<Exception> errors)
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
                if (values.length != 6)
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
                    SecurityEvent event = buildPrice(values);
                    if (event != null)
                    {
                        for (Security security : forSymbol)
                        {
                            boolean isAdded = security.addEvent(event);
                            isUpdated = isUpdated || isAdded;
                        }                        
                    }
                }
                catch (NumberFormatException | ParseException | DateTimeParseException e)
                {
                    errors.add(new IOException(MessageFormat.format(Messages.MsgErrorsConvertingValue, line), e));
                }
            }

            for (String symbol : symbol2security.keySet())
                errors.add(new IOException(MessageFormat.format(Messages.MsgMissingResponse, symbol + symbol2security.toString())));
        }
        catch (IOException e)
        {
            errors.add(e);
        }

        return isUpdated;
    }

    private SecurityEvent buildPrice(String[] values) throws ParseException
    {
        try
        {
        
            double lastDividend = asDouble(values[1]);
    
            LocalDate lastExDate = asDate(values[2]);
            if (lastExDate == null) // can't work w/o date
                lastExDate = LocalDate.now();
    
            LocalDate payDate = asDate(values[3]);

            long dividendYield = asPrice(values[4]);

            String currencyCode = Messages.LabelNoCurrencyCode;
            if (CurrencyUnit.isCurrencyCode(stripQuotes(values[5])))
                currencyCode = stripQuotes(values[5]);

            SecurityEvent event = new SecurityEvent();
                event.setType(SecurityEvent.Type.STOCK_DIVIDEND);
                if (payDate == null) // can't work w/o date
                {
                    event.setDate(lastExDate);
                }
                else
                {
                    event.setDate(payDate);
                    event.setExDate(lastExDate);
                }
                event.setAmount(currencyCode, BigDecimal.valueOf(lastDividend));
            return event;
        }
        catch (ParseException e)
        {
            return null;
        }

    }

    @Override
    public final boolean update(Security security, List<Exception> errors)
    {
        LocalDate start = caculateStart(security);

        List<SecurityEvent> events = internalGet(SecurityEvent.class, security, start, errors);

        boolean isUpdated = false;
        if (events != null)
        {
            for (SecurityEvent e : events)
            {
                boolean isAdded = security.addEvent(e);
                isUpdated = isUpdated || isAdded;
            }
        }
        return isUpdated;
    }

    /**
     * Calculate the first date to request historical events for.
     */
    /* package */final LocalDate caculateStart(Security security)
    {
        if (!security.getEvents().isEmpty())
        {
            SecurityEvent lastHistoricalEvent = security.getEvents().get(security.getEvents().size() - 1);
            return lastHistoricalEvent.getDate();
        }
        else
        {
            return LocalDate.of(1900, 1, 1);
        }
    }

    @Override
    public final List<SecurityElement> get(Security security, LocalDate start,
                    List<Exception> errors)
    {
        return SecurityElement.cast2ElementList(internalGet(SecurityEvent.class, security, start, errors));
    }

    @Override
    public List<SecurityElement> get(String response, List<Exception> errors)
    {
        throw new UnsupportedOperationException();
    }

    private <T extends SecurityEvent> List<T> internalGet(Class<T> klass, Security security, LocalDate startDate,
                    List<Exception> errors)
    {
        if (security.getTickerSymbol() == null)
        {
            errors.add(new IOException(MessageFormat.format(Messages.MsgMissingTickerSymbol, security.getName())));
            return Collections.emptyList();
        }

        LocalDate stopDate = LocalDate.now();

        String wknUrl = MessageFormat.format(HISTORICAL_URL, //
                        security.getTickerSymbol(), //
                        startDate.getMonth().getValue(), //
                        startDate.getDayOfMonth(), //
                        Integer.toString(startDate.getYear() - 5), //
                        stopDate.getMonth().getValue() - 1, //
                        stopDate.getDayOfMonth(), //
                        Integer.toString(stopDate.getYear()));

        List<T> answer = new ArrayList<>();

        String line = null;
        try (BufferedReader reader = openReader(wknUrl, errors))
        {
            if (reader == null)
                return answer;

            line = reader.readLine();

            // poor man's check
            if (!"Date,Dividends".equals(line)) //$NON-NLS-1$
                throw new IOException(MessageFormat.format(Messages.MsgUnexpectedHeader, line));

            DecimalFormat priceFormat = FMT_PRICE.get();
            DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(" yyyyMMdd"); //$NON-NLS-1$

            int cnt = 0;
            int expectedCnt = 0;
            int split = 0;
            int others = 0;
            
            while ((line = reader.readLine()) != null)
            {
                String[] values = line.split(","); //$NON-NLS-1$
                if (values.length == 3 && ("SPLIT".equals(values[0]) || "DIVIDEND".equals(values[0])))
                {
                    T event = klass.newInstance();
                    LocalDate date = LocalDate.parse(values[1], dateFormat);
                    event.setDate(date);
                    if ("DIVIDEND".equals(values[0])) 
                    {
                        cnt++;
                        event.setType(SecurityEvent.Type.STOCK_DIVIDEND);
                        event.setAmount(security.getCurrencyCode(), BigDecimal.valueOf(asDouble(values[2])));
                    } 
                    else if ("SPLIT".equals(values[0]))
                    {
                        split++;
                        String[] elements = values[2].split(":"); 
                        if (elements.length == 2)
                        {
                            event.setType(SecurityEvent.Type.STOCK_SPLIT);
                            event.setRatio(Double.parseDouble(elements[0]), Double.parseDouble(elements[1]));
                        }
                        else
                            throw new IOException(MessageFormat.format(Messages.MsgUnexpectedValue, line));
                    }
                    answer.add(event);
                }
                else if (values.length == 2 && ("TOTALSIZE".equals(values[0])))
                {
                    expectedCnt = Integer.parseInt(values[1].trim());  // may be used for a check in the future
                }
                else if (values.length == 2 && ("STARTDATE".equals(values[0]) || "ENDDATE".equals(values[0]) || "STATUS".equals(values[0])))
                {
                }
                else
                    throw new IOException(MessageFormat.format(Messages.MsgUnexpectedValue, line));
            }
        }
        catch (NumberFormatException | ParseException | DateTimeParseException e)
        {
            errors.add(new IOException(MessageFormat.format(Messages.MsgErrorsConvertingValue, line), e));
        }
        catch (InstantiationException | IllegalAccessException | IOException e)
        {
            errors.add(e);
        }
        return answer;
    }
//
//    protected <T extends SecurityEvent> void fillValues(String[] values, T event, DecimalFormat priceFormat,
//                    DateTimeFormatter dateFormat) throws ParseException, DateTimeParseException
//    {
//        LocalDate date = LocalDate.parse(values[0], dateFormat);
//
//        Number q = priceFormat.parse(values[4]);
//        long v = (long) (q.doubleValue() * Values.Quote.factor());
//
//        price.setDate(date);
//        price.setValue(v);
//
//        if (price instanceof LatestSecurityPrice)
//        {
//            LatestSecurityPrice latest = (LatestSecurityPrice) price;
//
//            q = priceFormat.parse(values[5]);
//            latest.setVolume(q.intValue());
//
//            q = priceFormat.parse(values[2]);
//            latest.setHigh((long) (q.doubleValue() * Values.Quote.factor()));
//
//            q = priceFormat.parse(values[3]);
//            latest.setLow((long) (q.doubleValue() * Values.Quote.factor()));
//        }
//    }

    @Override
    public final List<Exchange> getExchanges(Security subject, List<Exception> errors)
    {
        List<Exchange> answer = new ArrayList<>();

        String symbol = subject.getTickerSymbol();

        // if symbol is null, return empty list
        if (symbol == null || symbol.trim().length() == 0)
            return answer;

        // strip away exchange suffix to search for all available exchanges
        int p = symbol.indexOf('.');
        String prefix = p >= 0 ? symbol.substring(0, p + 1) : symbol + "."; //$NON-NLS-1$

        try
        {
            searchSymbols(answer, prefix);
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

    /* enable testing */
    protected void searchSymbols(List<Exchange> answer, String query) throws IOException
    {
        new YahooSymbolSearch().search(query).map(r -> createExchange(r.getSymbol())).forEach(e -> answer.add(e));
    }
}
