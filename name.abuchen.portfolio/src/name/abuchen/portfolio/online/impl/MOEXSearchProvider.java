package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.online.SecuritySearchProvider;
import name.abuchen.portfolio.online.SecurityType;
import name.abuchen.portfolio.util.WebAccess;

/**
 * Search for securities on the Moscow Exchange using the ISS API.
 * <p>
 * The search endpoint returns all instruments known to the exchange, including
 * price fixings, iNAVs, futures and options. The provider therefore filters the
 * results to tradeable securities (shares, bonds, exchange traded funds,
 * indices and currency pairs) that either have a dedicated market price board,
 * represent a plain stock exchange index, or are quoted as a currency pair on
 * the currency engine (e.g. <code>GLDRUB_TOM</code>).
 * <p>
 * The MOEX security ID (secid) is used as the ticker symbol. The trading
 * engine, the market path segment and the primary trading board are stored as
 * feed properties so that the {@link MOEXQuoteFeed} can load the correct
 * historical data. Currency pairs are created as exchange rate securities with
 * the base currency as currency and the term currency as target currency.
 */
public class MOEXSearchProvider implements SecuritySearchProvider
{
    private static final String NAME = "MOEX"; //$NON-NLS-1$
    private static final String HOST = "iss.moex.com"; //$NON-NLS-1$
    private static final String EXCHANGE = "MISX"; //$NON-NLS-1$

    /**
     * ISS instrument types that the provider understands. Everything else (for
     * example price fixings, iNAVs or open-ended interval funds) is filtered
     * out even if it is currently traded and has a market price board.
     */
    private static final Set<String> SUPPORTED_TYPES = Set.of( //
                    "common_share", //$NON-NLS-1$
                    "preferred_share", //$NON-NLS-1$
                    "exchange_bond", //$NON-NLS-1$
                    "corporate_bond", //$NON-NLS-1$
                    "etf_ppif", //$NON-NLS-1$
                    "exchange_ppif", //$NON-NLS-1$
                    "stock_index", //$NON-NLS-1$
                    "currency", //$NON-NLS-1$
                    "gold_metal", //$NON-NLS-1$
                    "silver_metal", //$NON-NLS-1$
                    "other_metal"); //$NON-NLS-1$

    /**
     * ISS instrument groups that are traded on the currency engine. These
     * instruments are quoted as currency pairs (e.g. <code>USD/RUB</code> or
     * <code>GLD/RUB</code>) and do not have a market price board of their own.
     */
    private static final Set<String> CURRENCY_GROUPS = Set.of( //
                    "currency_selt", //$NON-NLS-1$
                    "currency_metal"); //$NON-NLS-1$

    static class Result implements ResultItem
    {
        private String secid;
        private String shortname;
        private String name;
        private String isin;
        private String type;
        private String group;
        private String engine;
        private String market;
        private String board;
        private String baseCurrency;
        private String termCurrency;

        /**
         * Creates a search result from one row of the ISS securities
         * response. For currency instruments the currency pair is parsed from
         * the short name and the trading engine, market and primary board are
         * derived from the instrument group.
         *
         * @param json
         *            the JSON object holding a single security
         * @return the search result
         */
        public static Result from(JSONObject json)
        {
            Result result = new Result();
            result.secid = (String) json.get("secid"); //$NON-NLS-1$
            result.shortname = (String) json.get("shortname"); //$NON-NLS-1$
            result.name = (String) json.get("name"); //$NON-NLS-1$
            result.isin = (String) json.get("isin"); //$NON-NLS-1$
            result.type = (String) json.get("type"); //$NON-NLS-1$
            result.group = (String) json.get("group"); //$NON-NLS-1$
            result.engine = engineForGroup(result.group);
            result.market = marketForGroup(result.group);
            result.board = (String) json.get("primary_boardid"); //$NON-NLS-1$

            if (CURRENCY_GROUPS.contains(result.group))
                parseCurrencyPair(result);

            return result;
        }

        /**
         * Parses the currency pair (e.g. <code>GLD/RUB</code>) from the short
         * name of a currency instrument and stores it as base and term
         * currency. The short name follows the pattern
         * <code>&lt;secid&gt; - &lt;BASE&gt;/&lt;TERM&gt;</code>.
         */
        private static void parseCurrencyPair(Result result)
        {
            // the shortname contains the currency pair, e.g. "GLDRUB_TOM -
            // GLD/RUB" or "USDRUB_TOM - USD/RUB"
            var shortname = result.shortname != null ? result.shortname : result.name;
            int index = shortname.indexOf(" - "); //$NON-NLS-1$
            if (index < 0)
                return;

            String pair = shortname.substring(index + 3).trim();
            int slash = pair.indexOf('/');
            if (slash <= 0 || slash == pair.length() - 1)
                return;

            result.baseCurrency = pair.substring(0, slash).trim();
            result.termCurrency = pair.substring(slash + 1).trim();
        }

        /**
         * Returns the trading engine for the given ISS instrument group.
         * Currency instruments are traded on the <code>currency</code> engine,
         * everything else on the <code>stock</code> engine.
         */
        private static String engineForGroup(String group)
        {
            if (CURRENCY_GROUPS.contains(group))
                return "currency"; //$NON-NLS-1$

            return "stock"; //$NON-NLS-1$
        }

        /**
         * Returns the market path segment for the given ISS instrument group.
         */
        private static String marketForGroup(String group)
        {
            if (group == null)
                return "shares"; //$NON-NLS-1$

            if (CURRENCY_GROUPS.contains(group))
                return "selt"; //$NON-NLS-1$

            switch (group)
            {
                case "stock_bonds": //$NON-NLS-1$
                    return "bonds"; //$NON-NLS-1$
                case "stock_index": //$NON-NLS-1$
                    return "index"; //$NON-NLS-1$
                case "stock_foreignshares": //$NON-NLS-1$
                case "stock_dr": //$NON-NLS-1$
                    return "foreignshares"; //$NON-NLS-1$
                default:
                    return "shares"; //$NON-NLS-1$
            }
        }

        @Override
        public String getSymbol()
        {
            return secid;
        }

        @Override
        public String getName()
        {
            return shortname != null ? shortname : name;
        }

        @Override
        public String getType()
        {
            return convertType(type);
        }

        /**
         * Maps the ISS instrument type to a human readable security type label.
         */
        private String convertType(String type)
        {
            if (type == null)
                return null;

            switch (type)
            {
                case "common_share": //$NON-NLS-1$
                    return Messages.LabelSearchShare;
                case "preferred_share": //$NON-NLS-1$
                    return Messages.LabelSearchPreferredStock;
                case "exchange_bond": //$NON-NLS-1$
                case "corporate_bond": //$NON-NLS-1$
                    return Messages.LabelSearchBond;
                case "etf_ppif": //$NON-NLS-1$
                case "exchange_ppif": //$NON-NLS-1$
                    return Messages.LabelSearchETF;
                case "stock_index": //$NON-NLS-1$
                    return Messages.LabelSearchIndex;
                case "currency": //$NON-NLS-1$
                case "gold_metal": //$NON-NLS-1$
                case "silver_metal": //$NON-NLS-1$
                case "other_metal": //$NON-NLS-1$
                    return Messages.LabelSearchCurrency;
                default:
                    return SecurityType.convertType(type);
            }
        }

        @Override
        public String getIsin()
        {
            return isin;
        }

        @Override
        public String getWkn()
        {
            return null;
        }

        @Override
        public String getExchange()
        {
            return EXCHANGE;
        }

        @Override
        public String getCurrencyCode()
        {
            if (baseCurrency != null && termCurrency != null)
                return baseCurrency + "/" + termCurrency; //$NON-NLS-1$

            return "RUB"; //$NON-NLS-1$
        }

        @Override
        public String getSource()
        {
            return NAME;
        }

        @Override
        public String getFeedId()
        {
            return MOEXQuoteFeed.ID;
        }

        @Override
        public boolean hasPrices()
        {
            return true;
        }

        /**
         * Creates the security for the search result. Currency instruments are
         * created as exchange rate securities: the base currency of the pair is
         * stored as the security currency and the term currency as the target
         * currency. The trading engine, market and primary board are stored as
         * feed properties so that the {@link MOEXQuoteFeed} can load the quotes.
         */
        @Override
        public Security create(Client client)
        {
            // currency instruments are quoted as currency pairs, e.g.
            // GLD/RUB; the base currency is stored as the security currency
            // and the term currency as the target currency so that the app
            // treats the security as an exchange rate
            var security = new Security(getName(),
                            baseCurrency != null ? baseCurrency : getCurrencyCode());
            security.setTickerSymbol(secid);
            security.setIsin(isin);
            security.setFeed(MOEXQuoteFeed.ID);
            if (termCurrency != null)
                security.setTargetCurrencyCode(termCurrency);
            security.setPropertyValue(SecurityProperty.Type.FEED, MOEXQuoteFeed.MOEX_ENGINE, engine);
            security.setPropertyValue(SecurityProperty.Type.FEED, MOEXQuoteFeed.MOEX_MARKET, market);
            security.setPropertyValue(SecurityProperty.Type.FEED, MOEXQuoteFeed.MOEX_BOARD, board);
            return security;
        }
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    /**
     * Searches the MOEX ISS securities endpoint for the given query and returns
     * the matching tradeable instruments.
     *
     * @param query
     *            the search term
     * @return the matching instruments
     * @throws IOException
     *             if the request fails
     */
    @Override
    public List<ResultItem> search(String query) throws IOException
    {
        if (query == null || query.isBlank())
            return Collections.emptyList();

        List<ResultItem> answer = new ArrayList<>();

        String json = new WebAccess(HOST, "/iss/securities.json") //$NON-NLS-1$
                        .addParameter("q", query) //$NON-NLS-1$
                        .addParameter("iss.meta", "off") //$NON-NLS-1$ //$NON-NLS-2$
                        .addParameter("lang", "en") //$NON-NLS-1$ //$NON-NLS-2$
                        .addParameter("is_trading", "1") //$NON-NLS-1$ //$NON-NLS-2$
                        .addParameter("limit", "100") //$NON-NLS-1$
                        .get();

        extract(answer, json);

        return answer;
    }

    /**
     * Parses the securities table of the ISS response and adds the tradeable
     * instruments to the given answer list. Instruments are filtered to those
     * that are currently trading, have a supported instrument type and either
     * have a market price board, are a plain stock exchange index, or are
     * quoted as a currency pair.
     */
    /* testing */ void extract(List<ResultItem> answer, String json)
    {
        JSONObject response = (JSONObject) JSONValue.parse(json);
        if (response == null)
            return;

        JSONObject securities = (JSONObject) response.get("securities"); //$NON-NLS-1$
        if (securities == null)
            return;

        JSONArray columns = (JSONArray) securities.get("columns"); //$NON-NLS-1$
        JSONArray data = (JSONArray) securities.get("data"); //$NON-NLS-1$
        if (columns == null || data == null)
            return;

        Map<String, Integer> index = new HashMap<>();
        for (int ii = 0; ii < columns.size(); ii++)
            index.put(String.valueOf(columns.get(ii)), ii);

        Integer isTradedIdx = index.get("is_traded"); //$NON-NLS-1$
        Integer marketPriceBoardIdx = index.get("marketprice_boardid"); //$NON-NLS-1$
        Integer typeIdx = index.get("type"); //$NON-NLS-1$
        Integer groupIdx = index.get("group"); //$NON-NLS-1$

        for (Object rowObj : data)
        {
            JSONArray row = (JSONArray) rowObj;

            // only include securities that are currently trading
            if (isTradedIdx != null && !isTrading(row, isTradedIdx))
                continue;

            // only include securities with a supported type
            String type = typeIdx != null ? (String) row.get(typeIdx) : null;
            if (type == null || !SUPPORTED_TYPES.contains(type))
                continue;

            String group = groupIdx != null ? (String) row.get(groupIdx) : null;

            // currencies are traded on the currency engine and have no market
            // price board of their own
            boolean hasMarketPriceBoard = marketPriceBoardIdx != null && row.get(marketPriceBoardIdx) != null;
            boolean isIndex = "stock_index".equals(type); //$NON-NLS-1$
            boolean isCurrency = CURRENCY_GROUPS.contains(group);

            if (!hasMarketPriceBoard && !isIndex && !isCurrency)
                continue;

            JSONObject item = new JSONObject();
            for (Map.Entry<String, Integer> entry : index.entrySet())
                item.put(entry.getKey(), row.get(entry.getValue()));

            answer.add(Result.from(item));
        }
    }

    /**
     * Returns whether the given row represents an instrument that is currently
     * trading, i.e. its <code>is_traded</code> column has the value 1.
     */
    private boolean isTrading(JSONArray row, Integer isTradedIdx)
    {
        Object value = row.get(isTradedIdx);
        return value instanceof Number number ? number.intValue() == 1 : "1".equals(value); //$NON-NLS-1$
    }
}
