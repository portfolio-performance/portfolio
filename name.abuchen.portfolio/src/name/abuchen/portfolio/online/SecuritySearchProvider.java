package name.abuchen.portfolio.online;

import static name.abuchen.portfolio.util.TextUtil.trim;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

public interface SecuritySearchProvider
{
    public interface ResultItem
    {
        String getName();

        String getSymbol();

        /**
         * Returns the ticker symbol (if available) without the stock market
         * extension.
         */
        default String getSymbolWithoutStockMarket()
        {
            String symbol = getSymbol();
            if (symbol == null)
                return null;

            int p = symbol.indexOf('.');
            return p >= 0 ? symbol.substring(0, p) : symbol;
        }

        String getIsin();

        String getWkn();

        String getType();

        String getExchange();

        String getSource();

        default String getCurrencyCode()
        {
            return null;
        }

        default String getOnlineId()
        {
            return null;
        }

        default boolean hasPrices()
        {
            return false;
        }

        Security create(Client client);

        default List<ResultItem> getMarkets()
        {
            return Collections.emptyList();
        }
    }

    default String getName()
    {
        return getClass().getSimpleName();
    }

    default List<ResultItem> search(String query) throws IOException
    {
        return Collections.emptyList();
    }

    default List<ResultItem> getCoins() throws IOException
    {
        return Collections.emptyList();
    }

    @SuppressWarnings("nls")
    static String convertType(String type)
    {
        if (type == null)
            return null;

        // Convert the security type to a standard value
        Map<String, String> typeMap = new HashMap<>();

        typeMap.put("bond", Messages.LabelSearchBond);
        typeMap.put("closed-end fund", Messages.LabelSearchCloseEndFund);
        typeMap.put("common", Messages.LabelSearchShare);
        typeMap.put("common stock", Messages.LabelSearchShare);
        typeMap.put("currency", Messages.LabelSearchCurrency);
        typeMap.put("digital currency", Messages.LabelSearchCryptoCurrency);
        typeMap.put("cryptocurrency", Messages.LabelSearchCryptoCurrency);
        typeMap.put("crypto", Messages.LabelSearchCryptoCurrency);
        typeMap.put("etf", Messages.LabelSearchETF);
        typeMap.put("etc", Messages.LabelSearchETC);
        typeMap.put("exchange-traded note", Messages.LabelSearchETN);
        typeMap.put("equity", Messages.LabelSearchShare);
        typeMap.put("fund", Messages.LabelSearchFund);
        typeMap.put("future", Messages.LabelSearchFuture);
        typeMap.put("index", Messages.LabelSearchIndex);
        typeMap.put("mutual fund", Messages.LabelSearchMutualFund);
        typeMap.put("mutualfund", Messages.LabelSearchMutualFund);
        typeMap.put("new york registered shares", Messages.LabelSearchShare);
        typeMap.put("physical currency", Messages.LabelSearchCurrency);
        typeMap.put("preferred stock", Messages.LabelSearchPreferredStock);
        typeMap.put("real estate investment trust (reit)", Messages.LabelSearchReit);
        typeMap.put("reit", Messages.LabelSearchReit);
        typeMap.put("warrant", Messages.LabelSearchWarrant);

        // keep original capitalization for unknown types
        return typeMap.getOrDefault(trim(type).toLowerCase(), type);
    }
}
