package name.abuchen.portfolio.online.impl;

import static name.abuchen.portfolio.online.impl.YahooHelper.asNumber;
import static name.abuchen.portfolio.online.impl.YahooHelper.asPrice;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.util.Dates;

public class YahooFinanceQuoteFeed implements QuoteFeed
{
    public static final String ID = "YAHOO"; //$NON-NLS-1$

    private static final String LATEST_URL = "https://query1.finance.yahoo.com/v7/finance/quote?lang=en-US&region=US&corsDomain=finance.yahoo.com&symbols={0}"; //$NON-NLS-1$

    private static final String HISTORICAL_URL = "https://query1.finance.yahoo.com/v7/finance/spark?symbols={0}&range={1}&interval=1d"; //$NON-NLS-1$

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

        try (CloseableHttpClient client = HttpClients.createSystem())
        {
            String wknUrl = MessageFormat.format(LATEST_URL,
                            URLEncoder.encode(security.getTickerSymbol(), StandardCharsets.UTF_8.name()));

            try (CloseableHttpResponse response = client.execute(new HttpGet(wknUrl)))
            {
                if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
                    throw new IOException(wknUrl + " --> " + response.getStatusLine().getStatusCode()); //$NON-NLS-1$

                String body = EntityUtils.toString(response.getEntity());

                int startIndex = body.indexOf("quoteResponse"); //$NON-NLS-1$
                if (startIndex < 0)
                    return false;

                LatestSecurityPrice price = new LatestSecurityPrice();

                Optional<String> time = extract(body, startIndex, "\"regularMarketTime\":", ","); //$NON-NLS-1$ //$NON-NLS-2$
                if (time.isPresent())
                {
                    long epoch = Long.parseLong(time.get());
                    price.setDate(Instant.ofEpochSecond(epoch).atZone(ZoneId.systemDefault()).toLocalDate());
                }

                Optional<String> value = extract(body, startIndex, "\"regularMarketPrice\":", ","); //$NON-NLS-1$ //$NON-NLS-2$
                if (value.isPresent())
                    price.setValue(asPrice(value.get()));

                Optional<String> previousClose = extract(body, startIndex, "\"regularMarketPreviousClose\":", ","); //$NON-NLS-1$ //$NON-NLS-2$
                if (previousClose.isPresent())
                    price.setPreviousClose(asPrice(previousClose.get()));

                Optional<String> high = extract(body, startIndex, "\"regularMarketDayHigh\":", ","); //$NON-NLS-1$ //$NON-NLS-2$
                if (high.isPresent())
                    price.setHigh(asPrice(high.get()));

                Optional<String> low = extract(body, startIndex, "\"regularMarketDayLow\":", ","); //$NON-NLS-1$ //$NON-NLS-2$
                if (low.isPresent())
                    price.setLow(asPrice(low.get()));

                Optional<String> volume = extract(body, startIndex, "\"regularMarketVolume\":", ","); //$NON-NLS-1$ //$NON-NLS-2$
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

        try
        {
            String responseBody = requestData(security, startDate);
            return extractQuotes(klass, responseBody, errors);
        }
        catch (IOException e)
        {
            errors.add(new IOException(MessageFormat.format(Messages.MsgErrorDownloadYahoo, 1,
                            security.getTickerSymbol(), e.getMessage()), e));
        }

        return Collections.emptyList();
    }

    private String requestData(Security security, LocalDate startDate) throws IOException
    {
        int days = Dates.daysBetween(startDate, LocalDate.now());

        // "max" only returns a sample of quotes
        String range = "10y"; //$NON-NLS-1$

        if (days < 25)
            range = "1mo"; //$NON-NLS-1$
        else if (days < 75)
            range = "3mo"; //$NON-NLS-1$
        else if (days < 150)
            range = "6mo"; //$NON-NLS-1$
        else if (days < 300)
            range = "1y"; //$NON-NLS-1$
        else if (days < 600)
            range = "2y"; //$NON-NLS-1$
        else if (days < 1500)
            range = "5y"; //$NON-NLS-1$

        try (CloseableHttpClient client = HttpClients.createSystem())
        {
            String wknUrl = MessageFormat.format(HISTORICAL_URL, //
                            URLEncoder.encode(security.getTickerSymbol(), StandardCharsets.UTF_8.name()), //
                            range);

            try (CloseableHttpResponse response = client.execute(new HttpGet(wknUrl)))
            {
                if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
                    throw new IOException(wknUrl + " --> " + response.getStatusLine().getStatusCode()); //$NON-NLS-1$

                return EntityUtils.toString(response.getEntity());
            }
        }
    }

    private <T extends SecurityPrice> List<T> extractQuotes(Class<T> klass, String responseBody, List<Exception> errors)
    {
        List<T> answer = new ArrayList<>();

        try
        {
            JSONObject responseData = (JSONObject) JSONValue.parse(responseBody);
            if (responseData == null)
                throw new IOException("responseBody"); //$NON-NLS-1$

            JSONObject resultSet = (JSONObject) responseData.get("spark"); //$NON-NLS-1$
            if (resultSet == null)
                throw new IOException("spark"); //$NON-NLS-1$

            JSONArray result = (JSONArray) resultSet.get("result"); //$NON-NLS-1$
            if (result == null || result.isEmpty())
                throw new IOException("result"); //$NON-NLS-1$

            JSONObject result0 = (JSONObject) result.get(0);
            if (result0 == null)
                throw new IOException("result[0]"); //$NON-NLS-1$

            JSONArray response = (JSONArray) result0.get("response"); //$NON-NLS-1$
            if (response == null || response.isEmpty())
                throw new IOException("response"); //$NON-NLS-1$

            JSONObject response0 = (JSONObject) response.get(0);
            if (response0 == null)
                throw new IOException("response[0]"); //$NON-NLS-1$

            JSONArray timestamp = (JSONArray) response0.get("timestamp"); //$NON-NLS-1$

            JSONObject indicators = (JSONObject) response0.get("indicators"); //$NON-NLS-1$
            if (indicators == null)
                throw new IOException("indicators"); //$NON-NLS-1$

            JSONArray quotes = extractQuotesArray(indicators);

            int size = quotes.size();

            for (int index = 0; index < size; index++)
            {
                Long ts = (Long) timestamp.get(index);
                Double q = (Double) quotes.get(index);

                if (ts != null && q != null)
                {
                    T price = klass.getDeclaredConstructor().newInstance();
                    price.setDate(LocalDateTime.ofEpochSecond(ts, 0, ZoneOffset.UTC).toLocalDate());
                    price.setValue(Values.Quote.factorize(q));
                    answer.add(price);
                }
            }
        }
        catch (IOException | InstantiationException | IllegalAccessException | IndexOutOfBoundsException
                        | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
                        | SecurityException e)
        {
            errors.add(e);
        }

        return answer;
    }

    protected JSONArray extractQuotesArray(JSONObject indicators) throws IOException
    {
        JSONArray quotes = (JSONArray) indicators.get("quote"); //$NON-NLS-1$
        if (quotes == null || quotes.isEmpty())
            throw new IOException("quote"); //$NON-NLS-1$

        JSONObject quote = (JSONObject) quotes.get(0);
        if (quote == null)
            throw new IOException();

        JSONArray close = (JSONArray) quote.get("close"); //$NON-NLS-1$
        if (close == null || close.isEmpty())
            throw new IOException("close"); //$NON-NLS-1$

        return close;
    }

    @Override
    public final List<Exchange> getExchanges(Security subject, List<Exception> errors)
    {
        List<Exchange> answer = new ArrayList<>();

        // This is not the best place to include the market information from
        // portfolio-report.net, but for now the list of exchanges is only
        // available for Yahoo search provider.

        List<SecurityProperty> markets = subject.getProperties()
                        .filter(p -> p.getType() == SecurityProperty.Type.MARKET).collect(Collectors.toList());

        markets.stream().map(p -> {
            Exchange exchange = new Exchange(p.getValue(), ExchangeLabels.getString("portfolio-report." + p.getName())); //$NON-NLS-1$
            if ("XFRA".equals(p.getName())) //$NON-NLS-1$
                exchange.setId(exchange.getId() + ".F"); //$NON-NLS-1$
            return exchange;
        }).forEach(answer::add);

        Set<String> candidates = new HashSet<>();
        answer.forEach(e -> candidates.add(e.getId()));

        // add existing ticker symbol as well
        String symbol = subject.getTickerSymbol();
        if (symbol != null && !symbol.isEmpty())
            candidates.add(symbol);

        for (String candidate : candidates)
        {
            searchExchanges(candidate, answer, errors);
        }

        if (symbol != null && !symbol.isEmpty())
        {
            // Issue #251
            // sometimes Yahoo does not return the default exchange which
            // prevents selecting this security (example: searching for GOOG
            // does return only unimportant exchanges)
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
        }

        Collections.sort(answer, (r, l) -> r.getId().compareTo(l.getId()));

        return answer;
    }

    private void searchExchanges(String candidate, List<Exchange> answer, List<Exception> errors)
    {
        // strip away exchange suffix to search for all available exchanges
        int p = candidate.indexOf('.');
        String prefix = p >= 0 ? candidate.substring(0, p + 1) : candidate + "."; //$NON-NLS-1$

        // ensure we do not add duplicates
        Set<String> duplicates = new HashSet<>();
        answer.forEach(e -> duplicates.add(e.getId()));

        try
        {
            searchSymbols(prefix) //
                            .filter(r -> !duplicates.contains(r.getSymbol())) //
                            .map(r -> createExchange(r.getSymbol())).forEach(e -> {
                                duplicates.add(e.getId());
                                answer.add(e);
                            });
        }
        catch (IOException e)
        {
            errors.add(e);
        }

    }

    private Exchange createExchange(String symbol)
    {
        int e = symbol.indexOf('.');
        String exchange = e >= 0 ? symbol.substring(e) : ".default"; //$NON-NLS-1$
        String label = ExchangeLabels.getString("yahoo" + exchange); //$NON-NLS-1$
        return new Exchange(symbol, label);
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
    protected Stream<YahooSymbolSearch.Result> searchSymbols(String query) throws IOException
    {
        return new YahooSymbolSearch().search(query);
    }
}
