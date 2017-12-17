package name.abuchen.portfolio.online.impl;

import static name.abuchen.portfolio.online.impl.YahooHelper.asNumber;
import static name.abuchen.portfolio.online.impl.YahooHelper.asPrice;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.online.QuoteFeed;

public class YahooFinanceQuoteFeed implements QuoteFeed
{
    /* package */ interface CSVColumn // NOSONAR
    {
        int Date = 0;
        int Open = 1;
        int High = 2;
        int Low = 3;
        int Close = 4;
        int AdjClose = 5;
        int Volume = 6;
    }

    private static class Crumb
    {
        private final String id;
        private final Map<String, String> cookies;

        public Crumb(String id, Map<String, String> cookies)
        {
            this.id = id;
            this.cookies = cookies;
        }

        public String getId()
        {
            return id;
        }

        public Map<String, String> getCookies()
        {
            return cookies;
        }
    }

    public static final String ID = "YAHOO"; //$NON-NLS-1$

    @SuppressWarnings("nls")
    private static final String HISTORICAL_URL = "https://query1.finance.yahoo.com/v7/finance/download/{0}?period1={1}&period2={2}&interval=1d&events=history&crumb={3}";

    private Crumb crumb;

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
    public final boolean updateLatestQuotes(Security security, List<Exception> errors)
    {
        String wknUrl = MessageFormat.format("https://de.finance.yahoo.com/quote/{0}?ltr=1", //$NON-NLS-1$
                        security.getTickerSymbol());

        try
        {
            Response response = Jsoup.connect(wknUrl) //
                            .userAgent(OnlineHelper.getUserAgent()) //
                            .timeout(30000).execute();

            String body = response.body();

            // some quick and dirty extraction. This scraping code will anyway
            // not last long.

            int startIndex = body.indexOf("QuoteSummaryStore"); //$NON-NLS-1$
            if (startIndex < 0)
                return false;

            LatestSecurityPrice price = new LatestSecurityPrice();

            Optional<String> time = extract(body, startIndex, "\"regularMarketTime\":", ","); //$NON-NLS-1$ //$NON-NLS-2$
            if (time.isPresent())
            {
                long epoch = Long.parseLong(time.get());
                price.setDate(Instant.ofEpochSecond(epoch).atZone(ZoneId.systemDefault()).toLocalDate());
            }

            Optional<String> value = extract(body, startIndex, "\"regularMarketPrice\":{\"raw\":", ","); //$NON-NLS-1$ //$NON-NLS-2$
            if (value.isPresent())
                price.setValue(asPrice(value.get()));

            Optional<String> previousClose = extract(body, startIndex, "\"regularMarketPreviousClose\":{\"raw\":", ","); //$NON-NLS-1$ //$NON-NLS-2$
            if (previousClose.isPresent())
                price.setPreviousClose(asPrice(previousClose.get()));

            Optional<String> high = extract(body, startIndex, "\"regularMarketDayHigh\":{\"raw\":", ","); //$NON-NLS-1$ //$NON-NLS-2$
            if (high.isPresent())
                price.setHigh(asPrice(high.get()));

            Optional<String> low = extract(body, startIndex, "\"regularMarketDayLow\":{\"raw\":", ","); //$NON-NLS-1$ //$NON-NLS-2$
            if (low.isPresent())
                price.setLow(asPrice(low.get()));

            Optional<String> volume = extract(body, startIndex, "\"regularMarketVolume\":{\"raw\":", ","); //$NON-NLS-1$ //$NON-NLS-2$
            if (volume.isPresent())
                price.setVolume(asNumber(volume.get()));
            
            if (price.getDate() == null || price.getValue() <= 0)
            {
                errors.add(new IOException(body));
                return false;
            }

            security.setLatest(price);
            return true;

        }
        catch (IOException | ParseException e)
        {
            errors.add(e);
            return false;
        }
    }

    private Optional<String> extract(String body, int startIndex, String startToken, String endToken)
    {
        int begin = body.indexOf(startToken, startIndex);

        if (begin < 0)
            return Optional.empty();

        int end = body.indexOf(endToken, begin + startToken.length());
        if (end < 0)
            return Optional.empty();

        return Optional.of(body.substring(begin + startToken.length(), end));
    }

    @Override
    public final boolean updateHistoricalQuotes(Security security, List<Exception> errors)
    {
        LocalDate start = caculateStart(security);

        List<SecurityPrice> quotes = internalGetQuotes(SecurityPrice.class, security, start, errors);

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

    /**
     * Calculate the first date to request historical quotes for.
     */
    /* package */final LocalDate caculateStart(Security security)
    {
        if (!security.getPrices().isEmpty())
        {
            SecurityPrice lastHistoricalQuote = security.getPrices().get(security.getPrices().size() - 1);
            return lastHistoricalQuote.getDate();
        }
        else
        {
            return LocalDate.of(1900, 1, 1);
        }
    }

    @Override
    public final List<LatestSecurityPrice> getHistoricalQuotes(Security security, LocalDate start,
                    List<Exception> errors)
    {
        return internalGetQuotes(LatestSecurityPrice.class, security, start, errors);
    }

    @Override
    public List<LatestSecurityPrice> getHistoricalQuotes(String response, List<Exception> errors)
    {
        return extractQuotes(LatestSecurityPrice.class, response, errors);
    }

    private <T extends SecurityPrice> List<T> internalGetQuotes(Class<T> klass, Security security, LocalDate startDate,
                    List<Exception> errors)
    {
        if (security.getTickerSymbol() == null)
        {
            errors.add(new IOException(MessageFormat.format(Messages.MsgMissingTickerSymbol, security.getName())));
            return Collections.emptyList();
        }

        int attempt = 0;

        Crumb thisCrump = crumb;

        while (attempt < 2)
        {
            attempt++;

            try
            {
                if (thisCrump == null)
                    thisCrump = crumb = loadCrump(security.getTickerSymbol());

                String responseBody = requestData(security, startDate, thisCrump);

                return extractQuotes(klass, responseBody, errors);
            }
            catch (IOException e)
            {
                errors.add(new IOException(MessageFormat.format(Messages.MsgErrorDownloadYahoo, attempt,
                                security.getTickerSymbol(), e.getMessage()), e));

                thisCrump = crumb = null;
            }
        }

        return Collections.emptyList();
    }

    private Crumb loadCrump(String tickerSymbol) throws IOException
    {
        String url = MessageFormat.format("https://de.finance.yahoo.com/quote/{0}/history?p={0}", tickerSymbol); //$NON-NLS-1$

        Response response = Jsoup.connect(url).userAgent(OnlineHelper.getUserAgent()) //
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8") //$NON-NLS-1$ //$NON-NLS-2$
                        .header("Accept-Language", "en-US,en;q=0.5") // //$NON-NLS-1$ //$NON-NLS-2$
                        .timeout(30000)//
                        .execute();

        String KEY = "\"CrumbStore\":{\"crumb\":\""; //$NON-NLS-1$

        String body = response.body();

        int startIndex = body.indexOf(KEY);
        if (startIndex < 0)
            throw new IOException(Messages.MsgErrorNoCrumbFound);

        int endIndex = body.indexOf('"', startIndex + KEY.length());
        if (endIndex < 0)
            throw new IOException(Messages.MsgErrorNoCrumbFound);

        String crumb = body.substring(startIndex + KEY.length(), endIndex);
        crumb = StringEscapeUtils.unescapeJava(crumb);

        return new Crumb(crumb, response.cookies());
    }

    private String requestData(Security security, LocalDate startDate, Crumb requestCrumb) throws IOException
    {
        LocalDate stopDate = LocalDate.now();

        String wknUrl = MessageFormat.format(HISTORICAL_URL, //
                        security.getTickerSymbol(), //
                        String.valueOf(startDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond()), //
                        String.valueOf(stopDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond()),
                        URLEncoder.encode(requestCrumb.getId(), StandardCharsets.UTF_8.name()));

        Response response = Jsoup.connect(wknUrl) //
                        .userAgent(OnlineHelper.getUserAgent()) //
                        .cookies(requestCrumb.getCookies()) //
                        .timeout(30000).execute();

        if (response.statusCode() != HttpURLConnection.HTTP_OK)
            throw new IOException(MessageFormat.format(Messages.MsgErrorUnexpectedStatusCode,
                            security.getTickerSymbol(), response.statusCode(), wknUrl));

        return response.body();
    }

    private <T extends SecurityPrice> List<T> extractQuotes(Class<T> klass, String responseBody, List<Exception> errors)
    {
        String[] lines = responseBody.split("\\r?\\n"); //$NON-NLS-1$
        if (lines.length < 1)
            return Collections.emptyList();

        // poor man's check
        if (!"Date,Open,High,Low,Close,Adj Close,Volume".equals(lines[0])) //$NON-NLS-1$
        {
            errors.add(new IOException(MessageFormat.format(Messages.MsgUnexpectedHeader, lines[0])));
            return Collections.emptyList();
        }

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd"); //$NON-NLS-1$

        List<T> answer = new ArrayList<>();

        String line = null;

        try
        {
            for (int index = 1; index < lines.length; index++)
            {
                line = lines[index];

                String[] values = line.split(","); //$NON-NLS-1$
                if (values.length != 7)
                    throw new IOException(MessageFormat.format(Messages.MsgUnexpectedValue, line));

                // first check if all values except the date are not "null"
                if (!"null".equals(values[0])) //$NON-NLS-1$
                {
                    boolean hasValue = false;
                    for (int i = 1; i < values.length; i++)
                    {
                        if (!"null".equals(values[i])) //$NON-NLS-1$
                        {
                            hasValue = true;
                            break;
                        }
                    }
                    // skip this line if there are no values
                    if (!hasValue)
                    {
                        continue;
                    }
                }

                try
                {
                    T price = klass.newInstance();
                    fillValues(values, price, dateFormat);
                    answer.add(price);
                }
                catch (NumberFormatException | ParseException | DateTimeParseException e)
                {
                    errors.add(new IOException(MessageFormat.format(Messages.MsgErrorsConvertingValue, line), e));
                }

            }
        }
        catch (InstantiationException | IllegalAccessException | IOException e)
        {
            errors.add(e);
        }

        return answer;
    }

    protected <T extends SecurityPrice> void fillValues(String[] values, T price, DateTimeFormatter dateFormat)
                    throws ParseException, DateTimeParseException
    {
        LocalDate date = LocalDate.parse(values[CSVColumn.Date], dateFormat);

        long v = asPrice(values[CSVColumn.Close]);

        price.setDate(date);
        price.setValue(v);

        if (price instanceof LatestSecurityPrice)
        {
            LatestSecurityPrice latest = (LatestSecurityPrice) price;

            latest.setVolume(asNumber(values[CSVColumn.Volume]));
            latest.setHigh(asPrice(values[CSVColumn.High]));
            latest.setLow(asPrice(values[CSVColumn.Low]));
        }
    }

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
        new YahooSymbolSearch().search(query).map(r -> createExchange(r.getSymbol())).forEach(answer::add);
    }
}
