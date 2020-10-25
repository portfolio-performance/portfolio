package name.abuchen.portfolio.online.impl;

import static name.abuchen.portfolio.online.impl.YahooHelper.asNumber;
import static name.abuchen.portfolio.online.impl.YahooHelper.asPrice;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.util.Dates;
import name.abuchen.portfolio.util.WebAccess;

public class YahooFinanceQuoteFeed implements QuoteFeed
{
    public static final String ID = "YAHOO"; //$NON-NLS-1$

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

    public String rpcLatestQuote(Security security) throws IOException
    {
        return new WebAccess("query1.finance.yahoo.com", "/v7/finance/quote") //$NON-NLS-1$ //$NON-NLS-2$
                        .addParameter("lang", "en-US").addParameter("region", "US") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                        .addParameter("corsDomain", "finance.yahoo.com") //$NON-NLS-1$ //$NON-NLS-2$
                        .addParameter("symbols", security.getTickerSymbol()).get(); //$NON-NLS-1$
    }

    @Override
    public Optional<LatestSecurityPrice> getLatestQuote(Security security)
    {
        try
        {
            String html = this.rpcLatestQuote(security);

            int startIndex = html.indexOf("quoteResponse"); //$NON-NLS-1$
            if (startIndex < 0)
                return Optional.empty();

            LatestSecurityPrice price = new LatestSecurityPrice();

            Optional<String> time = extract(html, startIndex, "\"regularMarketTime\":", ","); //$NON-NLS-1$ //$NON-NLS-2$
            if (time.isPresent())
            {
                long epoch = Long.parseLong(time.get());
                price.setDate(Instant.ofEpochSecond(epoch).atZone(ZoneId.systemDefault()).toLocalDate());
            }

            Optional<String> value = extract(html, startIndex, "\"regularMarketPrice\":", ","); //$NON-NLS-1$ //$NON-NLS-2$
            if (value.isPresent())
                price.setValue(asPrice(value.get()));

            Optional<String> previousClose = extract(html, startIndex, "\"regularMarketPreviousClose\":", ","); //$NON-NLS-1$ //$NON-NLS-2$
            if (previousClose.isPresent())
                price.setPreviousClose(asPrice(previousClose.get()));

            Optional<String> high = extract(html, startIndex, "\"regularMarketDayHigh\":", ","); //$NON-NLS-1$ //$NON-NLS-2$
            if (high.isPresent())
                price.setHigh(asPrice(high.get()));

            Optional<String> low = extract(html, startIndex, "\"regularMarketDayLow\":", ","); //$NON-NLS-1$ //$NON-NLS-2$
            if (low.isPresent())
                price.setLow(asPrice(low.get()));

            Optional<String> volume = extract(html, startIndex, "\"regularMarketVolume\":", ","); //$NON-NLS-1$ //$NON-NLS-2$
            if (volume.isPresent())
                price.setVolume(asNumber(volume.get()));

            if (price.getDate() == null || price.getValue() <= 0)
            {
                PortfolioLog.error(html);
                return Optional.empty();
            }
            else
            {
                return Optional.of(price);
            }
        }
        catch (IOException | ParseException e)
        {
            PortfolioLog.error(e);
            return Optional.empty();
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
    public QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse)
    {
        LocalDate start = caculateStart(security);
        return internalGetQuotes(security, start);
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
    public QuoteFeedData previewHistoricalQuotes(Security security)
    {
        return internalGetQuotes(security, LocalDate.now().minusMonths(2));
    }

    private QuoteFeedData internalGetQuotes(Security security, LocalDate startDate)
    {
        if (security.getTickerSymbol() == null)
        {
            return QuoteFeedData.withError(
                            new IOException(MessageFormat.format(Messages.MsgMissingTickerSymbol, security.getName())));
        }

        try
        {
            String responseBody = requestData(security, startDate);
            return extractQuotes(responseBody);
        }
        catch (IOException e)
        {
            return QuoteFeedData.withError(new IOException(MessageFormat.format(Messages.MsgErrorDownloadYahoo, 1,
                            security.getTickerSymbol(), e.getMessage()), e));
        }
    }

    @SuppressWarnings("nls")
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

        return new WebAccess("query1.finance.yahoo.com", "/v8/finance/chart/" + security.getTickerSymbol()) //
                        .addParameter("range", range) //
                        .addParameter("interval", "1d").get();

    }

    /* package */ QuoteFeedData extractQuotes(String responseBody)
    {
        List<LatestSecurityPrice> answer = new ArrayList<>();

        try
        {
            JSONObject responseData = (JSONObject) JSONValue.parse(responseBody);
            if (responseData == null)
                throw new IOException("responseBody"); //$NON-NLS-1$

            JSONObject resultSet = (JSONObject) responseData.get("chart"); //$NON-NLS-1$
            if (resultSet == null)
                throw new IOException("chart"); //$NON-NLS-1$

            JSONArray result = (JSONArray) resultSet.get("result"); //$NON-NLS-1$
            if (result == null || result.isEmpty())
                throw new IOException("result"); //$NON-NLS-1$

            JSONObject result0 = (JSONObject) result.get(0);
            if (result0 == null)
                throw new IOException("result[0]"); //$NON-NLS-1$

            JSONArray timestamp = (JSONArray) result0.get("timestamp"); //$NON-NLS-1$

            JSONObject indicators = (JSONObject) result0.get("indicators"); //$NON-NLS-1$
            if (indicators == null)
                throw new IOException("indicators"); //$NON-NLS-1$

            JSONArray quotes = extractQuotesArray(indicators);

            int size = quotes.size();

            for (int index = 0; index < size; index++)
            {
                Long ts = (Long) timestamp.get(index);
                Double q = (Double) quotes.get(index);

                if (ts != null && q != null && q.doubleValue() > 0)
                {
                    LatestSecurityPrice price = new LatestSecurityPrice();
                    price.setDate(LocalDateTime.ofEpochSecond(ts, 0, ZoneOffset.UTC).toLocalDate());
                    price.setValue(Values.Quote.factorize(q));
                    answer.add(price);
                }
            }
        }
        catch (IOException | IndexOutOfBoundsException | IllegalArgumentException e)
        {
            return QuoteFeedData.withError(e);
        }

        QuoteFeedData data = new QuoteFeedData();
        data.getLatestPrices().addAll(answer);
        return data;
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
