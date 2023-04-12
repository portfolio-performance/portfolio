package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.google.common.util.concurrent.RateLimiter;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.util.RateLimitExceededException;
import name.abuchen.portfolio.util.WebAccess;

public final class CoinGeckoQuoteFeed implements QuoteFeed
{
    /* package */ static class Coin
    {
        private String id;
        private String symbol;
        private String name;

        public String getId()
        {
            return id;
        }

        public String getSymbol()
        {
            return symbol;
        }

        public String getName()
        {
            return name;
        }
    }

    public static final String ID = "COINGECKO"; //$NON-NLS-1$
    public static final String COINGECKO_COIN_ID = "COINGECKOCOINID"; //$NON-NLS-1$

    /**
     * Use rate limiter with CoinGecko. The free API plan allows between 10 to
     * 50 calls per minute. The Guava RateLimiter uses permits per second.
     * However, with 10 permits per 60 seconds, we still get an error message
     * every once in a while. Therefore we are a little bit more conservative
     * when setting up the rate limiter.
     */
    private static RateLimiter rateLimiter = RateLimiter.create(40 / 60d);

    private List<Coin> coins;

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "CoinGecko"; //$NON-NLS-1$
    }

    @Override
    public Optional<LatestSecurityPrice> getLatestQuote(Security security)
    {
        QuoteFeedData data = getHistoricalQuotes(security, false, LocalDate.now());

        if (!data.getErrors().isEmpty())
            PortfolioLog.error(data.getErrors());

        List<LatestSecurityPrice> prices = data.getLatestPrices();

        if (prices.isEmpty())
            return Optional.empty();

        Collections.sort(prices, new SecurityPrice.ByDate());

        return Optional.of(prices.get(prices.size() - 1));
    }

    @Override
    public QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse)
    {
        LocalDate quoteStartDate = LocalDate.of(1970, 01, 01);

        if (!security.getPrices().isEmpty())
            quoteStartDate = security.getPrices().get(security.getPrices().size() - 1).getDate();

        return getHistoricalQuotes(security, collectRawResponse, quoteStartDate);
    }

    @Override
    public QuoteFeedData previewHistoricalQuotes(Security security)
    {
        return getHistoricalQuotes(security, true, LocalDate.now().minusMonths(2));
    }

    private void convertCoinGeckoJsonArray(JSONArray ohlcArray, QuoteFeedData data)
    {
        if (ohlcArray.isEmpty())
            return;

        List<LatestSecurityPrice> prices = new ArrayList<>();
        LatestSecurityPrice previous = null;

        for (Object price : ohlcArray)
        {
            if (price instanceof JSONArray priceArray)
            {
                try
                {
                    LatestSecurityPrice p = fromArray(priceArray);

                    if (previous != null && previous.getDate().equals(p.getDate()))
                        prices.set(prices.size() - 1, p);
                    else
                        prices.add(p);

                    previous = p;
                }
                catch (ParseException ex)
                {
                    data.addError(ex);
                }
            }
        }

        data.addAllPrices(prices);
    }

    private LatestSecurityPrice fromArray(JSONArray ohlcArray) throws ParseException
    {
        long timestamp = Long.parseLong(ohlcArray.get(0).toString());
        long close = ohlcArray.get(1) != null ? YahooHelper.asPrice(ohlcArray.get(1).toString()) : 0;

        ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC);

        LatestSecurityPrice price = new LatestSecurityPrice();
        price.setValue(close);
        price.setHigh(LatestSecurityPrice.NOT_AVAILABLE);
        price.setLow(LatestSecurityPrice.NOT_AVAILABLE);
        price.setVolume(LatestSecurityPrice.NOT_AVAILABLE);

        // Closing prices will be returned with time 00:00:00 of the next day
        if (date.getHour() == 0 && date.getMinute() == 0 && date.getSecond() == 0)
        {
            price.setDate(LocalDate.of(date.getYear(), date.getMonth(), date.getDayOfMonth()).minusDays(1));
        }
        else
        {
            // Note: Although CoinGecko does return quotes in UTC, we must treat
            // them as if they're 'localtime,' due to PP not storing its own
            // data in UTC. This avoids the bug where users in timezones later
            // than UTC see "latest quotes" one day late during the second part
            // of the day. See:
            // https://github.com/buchen/portfolio/issues/2106#issuecomment-822023523

            date = date.withZoneSameInstant(ZoneId.systemDefault());

            price.setDate(LocalDate.of(date.getYear(), date.getMonth(), date.getDayOfMonth()));
        }

        return price;
    }

    private QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse, LocalDate start)
    {
        if (!rateLimiter.tryAcquire(Duration.ofSeconds(30)))
            throw new RateLimitExceededException("CoinGecko rate limit exceeded"); //$NON-NLS-1$

        if (security.getTickerSymbol() == null)
            return QuoteFeedData.withError(
                            new IOException(MessageFormat.format(Messages.MsgMissingTickerSymbol, security.getName())));

        QuoteFeedData data = new QuoteFeedData();

        try
        {
            String coinGeckoId;

            // The coin ID may be provided directly as a feed parameter (in case
            // the ticker is ambiguously defined)
            Optional<String> coinGeckoIdProperty = security.getPropertyValue(SecurityProperty.Type.FEED,
                            COINGECKO_COIN_ID);

            if (coinGeckoIdProperty.isPresent())
                coinGeckoId = coinGeckoIdProperty.get();
            else
                // If not specified explicitly, try to map the ticker symbol to
                // a coin ID
                coinGeckoId = getCoinGeckoIdForTicker(security.getTickerSymbol().toLowerCase());

            String endpoint = "/api/v3/coins/" + coinGeckoId + "/market_chart"; //$NON-NLS-1$ //$NON-NLS-2$
            long days = ChronoUnit.DAYS.between(start, LocalDate.now()) + 1;

            WebAccess webaccess = new WebAccess("api.coingecko.com", endpoint) //$NON-NLS-1$
                            .addParameter("vs_currency", security.getCurrencyCode()) //$NON-NLS-1$
                            .addParameter("days", Long.toString(days)) //$NON-NLS-1$
                            .addParameter("interval", "daily"); //$NON-NLS-1$ //$NON-NLS-2$
            String json = webaccess.get();

            if (collectRawResponse)
                data.addResponse(webaccess.getURL(), json);

            JSONObject marketChartObject = (JSONObject) JSONValue.parse(json);

            if (marketChartObject != null && marketChartObject.containsKey("prices")) //$NON-NLS-1$
            {
                JSONArray priceArray = (JSONArray) marketChartObject.get("prices"); //$NON-NLS-1$
                convertCoinGeckoJsonArray(priceArray, data);
            }
        }
        catch (IOException | URISyntaxException e)
        {
            data.addError(e);
        }

        return data;
    }

    /**
     * Provide the internal CoinGecko ID for an "official" cryptocurrency ticker
     * symbol
     * 
     * @param tickerSymbol
     *            Cryptocurrency ticker symbol
     * @return Internal CoinGecko ID for use in further API calls
     * @throws IOException
     *             No mapping found for ticker
     */
    private String getCoinGeckoIdForTicker(String tickerSymbol) throws IOException
    {
        Optional<Coin> coinGeckoId = getCoins().stream().filter(c -> c.symbol.equals(tickerSymbol)).findFirst();

        if (coinGeckoId.isPresent())
            return coinGeckoId.get().id;
        else
            throw new IOException(MessageFormat.format(Messages.MsgMissingTickerSymbol, tickerSymbol));
    }

    /**
     * The CoinGecko API only allows to fetch a complete set of ID mappings for
     * all coins existing on the platform. In order to avoid unnecessary calls
     * the mapping will be buffered locally in a HashMap.
     */
    /* package */ synchronized List<Coin> getCoins() throws IOException
    {
        if (coins == null)
        {
            List<Coin> coinList = new ArrayList<>();

            WebAccess webaccess = new WebAccess("api.coingecko.com", "/api/v3/coins/list"); //$NON-NLS-1$ //$NON-NLS-2$
            String html = webaccess.get();

            JSONArray coinArray = (JSONArray) JSONValue.parse(html);

            if (coinArray != null)
            {
                for (Object object : coinArray)
                {
                    JSONObject coinObject = (JSONObject) object;

                    Coin coin = new Coin();
                    coin.id = (String) coinObject.get("id"); //$NON-NLS-1$
                    coin.symbol = (String) coinObject.get("symbol"); //$NON-NLS-1$
                    coin.name = (String) coinObject.get("name"); //$NON-NLS-1$

                    coinList.add(coin);
                }
            }

            coins = coinList;
        }

        return coins;
    }
}
