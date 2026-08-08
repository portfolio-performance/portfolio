package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.util.TextUtil;
import name.abuchen.portfolio.util.WebAccess;

/**
 * Load prices from the Moscow Exchange via the ISS (Informational and
 * Statistical Server) API.
 * <p>
 * The ticker symbol of the security holds the MOEX security ID (secid), e.g.
 * <code>SBER</code> or <code>GAZP</code>. The market (shares, bonds, index,
 * etc.) is stored as a feed property (see {@link #MOEX_MARKET}) and is set when
 * the security is created from a search result. If the property is missing,
 * the shares market is used as a sensible default.
 * <p>
 * Historical quotes are retrieved via
 * <code>iss/history/engines/stock/markets/{market}/securities/{secid}.json</code>.
 * The primary board filter is used for the shares, bonds and foreignshares
 * markets to avoid duplicate quotes across boards.
 */
public class MOEXQuoteFeed implements QuoteFeed
{
    public static final String ID = "MOEX"; //$NON-NLS-1$

    /**
     * Name of the security property (of type {@link SecurityProperty.Type#FEED})
     * that stores the MOEX market path segment, e.g. <code>shares</code> or
     * <code>bonds</code>.
     */
    public static final String MOEX_MARKET = "MOEX-MARKET"; //$NON-NLS-1$

    /**
     * Name of the security property (of type {@link SecurityProperty.Type#FEED})
     * that stores the MOEX trading engine, e.g. <code>stock</code> or
     * <code>currency</code>.
     */
    public static final String MOEX_ENGINE = "MOEX-ENGINE"; //$NON-NLS-1$

    /**
     * Name of the security property (of type {@link SecurityProperty.Type#FEED})
     * that stores the primary trading board, e.g. <code>TQBR</code> or
     * <code>CETS</code>. Used to request board-specific data so that quotes
     * from secondary boards are not mixed in.
     */
    public static final String MOEX_BOARD = "MOEX-BOARD"; //$NON-NLS-1$

    private static final String HOST = "iss.moex.com"; //$NON-NLS-1$
    private static final String DEFAULT_ENGINE = "stock"; //$NON-NLS-1$
    private static final String DEFAULT_MARKET = "shares"; //$NON-NLS-1$
    private static final int MAX_ROWS = 100;

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "MOEX"; //$NON-NLS-1$
    }

    @Override
    public Optional<LatestSecurityPrice> getLatestQuote(Security security)
    {
        var secid = TextUtil.trim(security.getTickerSymbol());
        if (secid == null || secid.isEmpty())
            return Optional.empty();

        var engine = getEngine(security);
        var market = getMarket(security);
        var board = getBoard(security);

        try
        {
            WebAccess webaccess = new WebAccess(HOST, createMarketDataPath(engine, market, board, secid))
                            .addParameter("iss.meta", "off") //$NON-NLS-1$ //$NON-NLS-2$
                            .addParameter("iss.only", "marketdata"); //$NON-NLS-1$ //$NON-NLS-2$

            if (board == null && supportsMarketPriceBoard(market))
                webaccess.addParameter("marketprice_board", "1"); //$NON-NLS-1$ //$NON-NLS-2$

            var json = getJson(webaccess);

            var quote = parseMarketData(json);
            if (quote.isPresent())
                return quote;
        }
        catch (IOException e)
        {
            PortfolioLog.error(e);
        }

        // fall back to the most recent completed session
        return getLatestFromHistory(security);
    }

    private String getEngine(Security security)
    {
        return security.getPropertyValue(SecurityProperty.Type.FEED, MOEX_ENGINE).orElse(DEFAULT_ENGINE);
    }

    private String getMarket(Security security)
    {
        return security.getPropertyValue(SecurityProperty.Type.FEED, MOEX_MARKET).orElse(DEFAULT_MARKET);
    }

    private String getBoard(Security security)
    {
        return security.getPropertyValue(SecurityProperty.Type.FEED, MOEX_BOARD).orElse(null);
    }

    private Optional<LatestSecurityPrice> getLatestFromHistory(Security security)
    {
        QuoteFeedData data = getHistoricalQuotes(security, false, LocalDate.now().minusDays(14));

        if (!data.getErrors().isEmpty())
            PortfolioLog.abbreviated(data.getErrors());

        List<LatestSecurityPrice> prices = data.getLatestPrices();

        if (prices.isEmpty())
            return Optional.empty();

        Collections.sort(prices, new SecurityPrice.ByDate());

        return Optional.of(prices.get(prices.size() - 1));
    }

    @Override
    public QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse)
    {
        LocalDate start = null;

        if (!security.getPrices().isEmpty())
            start = security.getPrices().get(security.getPrices().size() - 1).getDate();

        return getHistoricalQuotes(security, collectRawResponse, start);
    }

    @Override
    public QuoteFeedData previewHistoricalQuotes(Security security)
    {
        return getHistoricalQuotes(security, true, LocalDate.now().minusMonths(2));
    }

    private QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse, LocalDate from)
    {
        String secid = TextUtil.trim(security.getTickerSymbol());
        if (secid == null || secid.isEmpty())
            return QuoteFeedData.withError(
                            new IOException(MessageFormat.format(Messages.MsgMissingTickerSymbol, security.getName())));

        var engine = getEngine(security);
        var market = getMarket(security);
        var board = getBoard(security);

        QuoteFeedData data = new QuoteFeedData();

        try
        {
            int start = 0;
            long total = -1;

            while (true) // NOSONAR
            {
                WebAccess webaccess = createUrl(engine, market, board, secid, from, start);

                String json = getJson(webaccess);

                if (collectRawResponse)
                    data.addResponse(webaccess.getURL(), json);

                // advance by the number of source rows fetched, not by the
                // number of accepted prices, so that pagination continues until
                // the source page is exhausted even if rows are skipped
                int rowsFetched = parseHistory(json, data);

                if (rowsFetched == 0)
                    break;

                long newTotal = getTotal(json);
                if (newTotal >= 0)
                    total = newTotal;

                start += rowsFetched;

                if (total >= 0 ? start >= total : rowsFetched < MAX_ROWS)
                    break;
            }
        }
        catch (IOException | URISyntaxException e)
        {
            data.addError(e);
        }

        return data;
    }

    private WebAccess createUrl(String engine, String market, String board, String secid, LocalDate from, int start)
                    throws URISyntaxException
    {
        WebAccess webaccess = new WebAccess(HOST, createHistoryPath(engine, market, board, secid))
                        .addParameter("iss.meta", "off") //$NON-NLS-1$ //$NON-NLS-2$
                        .addParameter("iss.only", "history,history.cursor") //$NON-NLS-1$ //$NON-NLS-2$
                        .addParameter("start", String.valueOf(start)) //$NON-NLS-1$
                        .addParameter("limit", String.valueOf(MAX_ROWS)); //$NON-NLS-1$

        if (from != null)
            webaccess.addParameter("from", from.toString()); //$NON-NLS-1$

        if (board == null && supportsMarketPriceBoard(market))
            webaccess.addParameter("marketprice_board", "1"); //$NON-NLS-1$ //$NON-NLS-2$

        return webaccess;
    }

    private String createHistoryPath(String engine, String market, String board, String secid)
    {
        StringBuilder path = new StringBuilder("/iss/history/engines/"); //$NON-NLS-1$
        path.append(engine).append("/markets/").append(market); //$NON-NLS-1$
        if (board != null)
            path.append("/boards/").append(board); //$NON-NLS-1$
        path.append("/securities/").append(secid).append(".json"); //$NON-NLS-1$ //$NON-NLS-2$
        return path.toString();
    }

    private String createMarketDataPath(String engine, String market, String board, String secid)
    {
        StringBuilder path = new StringBuilder("/iss/engines/"); //$NON-NLS-1$
        path.append(engine).append("/markets/").append(market); //$NON-NLS-1$
        if (board != null)
            path.append("/boards/").append(board); //$NON-NLS-1$
        path.append("/securities/").append(secid).append(".json"); //$NON-NLS-1$ //$NON-NLS-2$
        return path.toString();
    }

    /* testing */ String getJson(WebAccess webaccess) throws IOException
    {
        return webaccess.get();
    }

    private boolean supportsMarketPriceBoard(String market)
    {
        return market.equals("shares") || market.equals("bonds") || market.equals("foreignshares"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    /**
     * Parses one page of the history response and adds all accepted prices to
     * the given {@link QuoteFeedData}. The accepted prices can be retrieved via
     * {@link QuoteFeedData#getLatestPrices()}.
     *
     * @return the number of source rows fetched from the page. This differs
     *         from the number of accepted prices when individual rows are
     *         skipped (for example because they contain no trade); the return
     *         value is used for pagination so that paging continues until the
     *         source page is exhausted.
     */
    /* testing */ int parseHistory(String json, QuoteFeedData data)
    {
        JSONObject response = (JSONObject) JSONValue.parse(json);
        if (response == null)
            return 0;

        JSONObject history = (JSONObject) response.get("history"); //$NON-NLS-1$
        if (history == null)
            return 0;

        JSONArray columns = (JSONArray) history.get("columns"); //$NON-NLS-1$
        JSONArray rows = (JSONArray) history.get("data"); //$NON-NLS-1$
        if (columns == null || rows == null)
            return 0;

        Map<String, Integer> index = new HashMap<>();
        for (int ii = 0; ii < columns.size(); ii++)
            index.put(String.valueOf(columns.get(ii)), ii);

        Integer dateIdx = index.get("TRADEDATE"); //$NON-NLS-1$
        Integer closeIdx = index.get("CLOSE"); //$NON-NLS-1$
        Integer legalCloseIdx = index.get("LEGALCLOSEPRICE"); //$NON-NLS-1$
        Integer wapriceIdx = index.get("WAPRICE"); //$NON-NLS-1$
        Integer highIdx = index.get("HIGH"); //$NON-NLS-1$
        Integer lowIdx = index.get("LOW"); //$NON-NLS-1$
        Integer volumeIdx = index.get("VOLUME"); //$NON-NLS-1$

        if (dateIdx == null)
            return 0;

        for (Object rowObj : rows)
        {
            JSONArray row = (JSONArray) rowObj;

            try
            {
                LocalDate date = LocalDate.parse((String) row.get(dateIdx));

                long value = asPrice(row, closeIdx);
                if (value <= 0 && legalCloseIdx != null)
                    value = asPrice(row, legalCloseIdx);
                if (value <= 0 && wapriceIdx != null)
                    value = asPrice(row, wapriceIdx);

                if (value <= 0)
                    continue;

                long high = highIdx != null ? asPrice(row, highIdx) : LatestSecurityPrice.NOT_AVAILABLE;
                long low = lowIdx != null ? asPrice(row, lowIdx) : LatestSecurityPrice.NOT_AVAILABLE;
                long volume = volumeIdx != null ? asNumber(row, volumeIdx) : LatestSecurityPrice.NOT_AVAILABLE;

                data.addPrice(new LatestSecurityPrice(date, value, high, low, volume));
            }
            catch (RuntimeException e)
            {
                data.addError(e);
            }
        }

        return rows.size();
    }

    /* testing */ long getTotal(String json)
    {
        JSONObject response = (JSONObject) JSONValue.parse(json);
        if (response == null)
            return -1;

        JSONObject cursor = (JSONObject) response.get("history.cursor"); //$NON-NLS-1$
        if (cursor == null)
            return -1;

        JSONArray columns = (JSONArray) cursor.get("columns"); //$NON-NLS-1$
        JSONArray data = (JSONArray) cursor.get("data"); //$NON-NLS-1$
        if (columns == null || data == null || data.isEmpty())
            return -1;

        Map<String, Integer> index = new HashMap<>();
        for (int ii = 0; ii < columns.size(); ii++)
            index.put(String.valueOf(columns.get(ii)), ii);

        Integer totalIdx = index.get("TOTAL"); //$NON-NLS-1$
        if (totalIdx == null)
            return -1;

        Object total = ((JSONArray) data.get(0)).get(totalIdx);
        return total instanceof Number number ? number.longValue() : -1;
    }

    private long asPrice(JSONArray row, Integer idx)
    {
        if (idx == null)
            return LatestSecurityPrice.NOT_AVAILABLE;

        Object value = row.get(idx);
        if (value == null)
            return LatestSecurityPrice.NOT_AVAILABLE;

        if (value instanceof Number number)
            return Values.Quote.factorize(number.doubleValue());

        if (value instanceof String string)
        {
            try
            {
                return Values.Quote.factorize(Double.parseDouble(string));
            }
            catch (NumberFormatException e)
            {
                return LatestSecurityPrice.NOT_AVAILABLE;
            }
        }

        return LatestSecurityPrice.NOT_AVAILABLE;
    }

    /**
     * Parses the marketdata response into a single latest quote. The price is
     * taken from the <code>LAST</code> column (or <code>MARKETPRICE</code> if
     * no trade happened yet) and the date from the <code>SYSTIME</code> column
     * (the server time at which the market data was generated).
     */
    /* testing */ Optional<LatestSecurityPrice> parseMarketData(String json)
    {
        JSONObject response = (JSONObject) JSONValue.parse(json);
        if (response == null)
            return Optional.empty();

        JSONObject marketdata = (JSONObject) response.get("marketdata"); //$NON-NLS-1$
        if (marketdata == null)
            return Optional.empty();

        JSONArray columns = (JSONArray) marketdata.get("columns"); //$NON-NLS-1$
        JSONArray rows = (JSONArray) marketdata.get("data"); //$NON-NLS-1$
        if (columns == null || rows == null || rows.isEmpty())
            return Optional.empty();

        Map<String, Integer> index = new HashMap<>();
        for (int ii = 0; ii < columns.size(); ii++)
            index.put(String.valueOf(columns.get(ii)), ii);

        Integer lastIdx = index.get("LAST"); //$NON-NLS-1$
        Integer marketPriceIdx = index.get("MARKETPRICE"); //$NON-NLS-1$
        Integer systimeIdx = index.get("SYSTIME"); //$NON-NLS-1$

        for (Object rowObj : rows)
        {
            JSONArray row = (JSONArray) rowObj;

            long value = lastIdx != null ? asPrice(row, lastIdx) : LatestSecurityPrice.NOT_AVAILABLE;
            if (value <= 0 && marketPriceIdx != null)
                value = asPrice(row, marketPriceIdx);

            if (value <= 0)
                continue;

            LatestSecurityPrice price = new LatestSecurityPrice();
            price.setValue(value);
            price.setHigh(LatestSecurityPrice.NOT_AVAILABLE);
            price.setLow(LatestSecurityPrice.NOT_AVAILABLE);
            price.setVolume(LatestSecurityPrice.NOT_AVAILABLE);

            if (systimeIdx != null)
            {
                Object systime = row.get(systimeIdx);
                if (systime instanceof String string)
                {
                    try
                    {
                        price.setDate(LocalDate.parse(string.substring(0, 10)));
                    }
                    catch (RuntimeException ignore)
                    {
                        price.setDate(LocalDate.now());
                    }
                }
            }

            if (price.getDate() == null)
                price.setDate(LocalDate.now());

            return Optional.of(price);
        }

        return Optional.empty();
    }

    private long asNumber(JSONArray row, Integer idx)
    {
        if (idx == null)
            return LatestSecurityPrice.NOT_AVAILABLE;

        Object value = row.get(idx);
        if (value == null)
            return LatestSecurityPrice.NOT_AVAILABLE;

        if (value instanceof Number number)
            return number.longValue();

        if (value instanceof String string)
        {
            try
            {
                return Long.parseLong(string);
            }
            catch (NumberFormatException e)
            {
                return LatestSecurityPrice.NOT_AVAILABLE;
            }
        }

        return LatestSecurityPrice.NOT_AVAILABLE;
    }
}
