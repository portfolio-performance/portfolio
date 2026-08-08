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
 * results to tradeable securities (shares, bonds, exchange traded funds and
 * indices) that either have a dedicated market price board or represent a plain
 * stock exchange index.
 * <p>
 * The MOEX security ID (secid) is used as the ticker symbol and the market path
 * segment is stored as a feed property so that the {@link MOEXQuoteFeed} can
 * load the correct historical data.
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
                    "stock_index"); //$NON-NLS-1$

    static class Result implements ResultItem
    {
        private String secid;
        private String shortname;
        private String name;
        private String isin;
        private String type;
        private String group;
        private String market;

        public static Result from(JSONObject json)
        {
            Result result = new Result();
            result.secid = (String) json.get("secid"); //$NON-NLS-1$
            result.shortname = (String) json.get("shortname"); //$NON-NLS-1$
            result.name = (String) json.get("name"); //$NON-NLS-1$
            result.isin = (String) json.get("isin"); //$NON-NLS-1$
            result.type = (String) json.get("type"); //$NON-NLS-1$
            result.group = (String) json.get("group"); //$NON-NLS-1$
            result.market = marketForGroup(result.group);
            return result;
        }

        private static String marketForGroup(String group)
        {
            if (group == null)
                return "shares"; //$NON-NLS-1$

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

        @Override
        public Security create(Client client)
        {
            var security = new Security(getName(), getCurrencyCode());
            security.setTickerSymbol(secid);
            security.setIsin(isin);
            security.setFeed(MOEXQuoteFeed.ID);
            security.setPropertyValue(SecurityProperty.Type.FEED, MOEXQuoteFeed.MOEX_MARKET, market);
            return security;
        }
    }

    @Override
    public String getName()
    {
        return NAME;
    }

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

        for (Object rowObj : data)
        {
            JSONArray row = (JSONArray) rowObj;

            // only include securities that are currently trading
            if (isTradedIdx != null && !isTrading(row, isTradedIdx))
                continue;

            // only include securities with a supported type, a market price
            // board or plain indices
            String type = typeIdx != null ? (String) row.get(typeIdx) : null;
            if (type == null || !SUPPORTED_TYPES.contains(type))
                continue;

            boolean hasMarketPriceBoard = marketPriceBoardIdx != null && row.get(marketPriceBoardIdx) != null;
            boolean isIndex = "stock_index".equals(type); //$NON-NLS-1$

            if (!hasMarketPriceBoard && !isIndex)
                continue;

            JSONObject item = new JSONObject();
            for (Map.Entry<String, Integer> entry : index.entrySet())
                item.put(entry.getKey(), row.get(entry.getValue()));

            answer.add(Result.from(item));
        }
    }

    private boolean isTrading(JSONArray row, Integer isTradedIdx)
    {
        Object value = row.get(isTradedIdx);
        return value instanceof Number number ? number.intValue() == 1 : "1".equals(value); //$NON-NLS-1$
    }
}
