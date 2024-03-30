package name.abuchen.portfolio.online.impl;

import static name.abuchen.portfolio.online.impl.YahooHelper.asPrice;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.MessageFormat;
import java.text.ParseException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
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

    @SuppressWarnings("nls")
    public String rpcLatestQuote(Security security) throws IOException
    {
        return new WebAccess("query1.finance.yahoo.com", "/v8/finance/chart/" + security.getTickerSymbol())
                        .addParameter("lang", "en-US").addParameter("region", "US")
                        .addParameter("corsDomain", "finance.yahoo.com").get();
    }

    @Override
    public Optional<LatestSecurityPrice> getLatestQuote(Security security)
    {
        try
        {
            String json = this.rpcLatestQuote(security);

            LatestSecurityPrice price = new LatestSecurityPrice();

            Optional<String> time = extract(json, 0, "\"regularMarketTime\":", ","); //$NON-NLS-1$ //$NON-NLS-2$
            if (time.isPresent())
            {
                long epoch = Long.parseLong(time.get());
                price.setDate(Instant.ofEpochSecond(epoch).atZone(ZoneId.systemDefault()).toLocalDate());
            }

            Optional<String> value = extract(json, 0, "\"regularMarketPrice\":", ","); //$NON-NLS-1$ //$NON-NLS-2$
            if (value.isPresent())
                price.setValue(asPrice(value.get()));

            price.setHigh(LatestSecurityPrice.NOT_AVAILABLE);
            price.setLow(LatestSecurityPrice.NOT_AVAILABLE);
            price.setVolume(LatestSecurityPrice.NOT_AVAILABLE);

            if (price.getDate() == null || price.getValue() <= 0)
            {
                PortfolioLog.error(MessageFormat.format(Messages.MsgErrorDownloadYahoo, 1, security.getTickerSymbol(),
                                json));
                return Optional.empty();
            }
            else
            {
                return Optional.of(price);
            }
        }
        catch (IOException | ParseException e)
        {
            PortfolioLog.abbreviated(e);
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

            ZoneId exchangeZoneId = ZoneOffset.UTC;
            if (result0.containsKey("meta")) //$NON-NLS-1$
            {
                JSONObject meta = (JSONObject) result0.get("meta"); //$NON-NLS-1$

                String exchangeTimezoneName = (String) meta.get("exchangeTimezoneName"); //$NON-NLS-1$
                if (exchangeTimezoneName != null)
                {
                    try
                    {
                        exchangeZoneId = ZoneId.of(exchangeTimezoneName);
                    }
                    catch (DateTimeException e)
                    {
                        // Ignore
                    }
                }
            }

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
                    price.setDate(LocalDateTime.ofInstant(Instant.ofEpochSecond(ts), exchangeZoneId).toLocalDate());

                    // yahoo api seesm to return floating numbers --> limit to 4
                    // digits which seems to round it to the right value
                    double v = Math.round(q * 10000) / 10000d;
                    price.setValue(Values.Quote.factorize(v));
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
        data.addResponse("n/a", responseBody); //$NON-NLS-1$
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

        var markets = PortfolioReportQuoteFeed.getMarkets(subject);

        markets.stream().map(market -> {
            Exchange exchange = new Exchange(market.getSymbol(),
                            MarketIdentifierCodes.getLabel(market.getMarketCode()));

            // symbol might be null because it was added only later to
            // MarketInfo, when deserializing from JSON, it might not be set
            if (market.getSymbol() != null)
            {
                if ("XFRA".equals(market.getMarketCode())) //$NON-NLS-1$
                    exchange.setId(exchange.getId() + ".F"); //$NON-NLS-1$
                if ("XETR".equals(market.getMarketCode())) //$NON-NLS-1$
                    exchange.setId(exchange.getId() + ".DE"); //$NON-NLS-1$
            }

            return exchange;
        }).filter(e -> e.getId() != null).forEach(answer::add);

        Set<String> candidates = new HashSet<>();
        answer.forEach(e -> candidates.add(e.getId()));

        // At the moment, we do not have a reasonable way to search for
        // exchanges of a given symbol. For the time being, we add all
        // exchanges...

        String symbol = subject.getTickerSymbol();
        if (symbol != null && !symbol.isEmpty())
        {
            // strip away exchange suffix
            int p = symbol.indexOf('.');
            String plainSymbol = p >= 0 ? symbol.substring(0, p) : symbol;

            answer.add(createExchange(plainSymbol));

            ExchangeLabels.getAllExchangeKeys("yahoo.").forEach(e -> answer.add(createExchange(plainSymbol + "." + e))); //$NON-NLS-1$ //$NON-NLS-2$
        }

        return answer;
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
}
