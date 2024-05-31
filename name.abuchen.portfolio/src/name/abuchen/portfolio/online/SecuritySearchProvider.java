package name.abuchen.portfolio.online;

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

        String getIsin();

        String getWkn();

        String getType();

        String getExchange();

        String getSource();

        default String getCurrencyCode()
        {
            return null;
        }

        default String getExtraAttributes()
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

    }

    public enum Type
    {
        ALL(Messages.LabelSearchAll), //
        SHARE(Messages.LabelSearchShare), //
        BOND(Messages.LabelSearchBond), //
        CRYPTO(Messages.LabelSearchCryptoCurrency);

        private final String label;

        private Type(String label)
        {
            this.label = label;
        }

        @Override
        public String toString()
        {
            return label;
        }
    }

    default String getName()
    {
        return getClass().getSimpleName();
    }

    default List<ResultItem> search(String query, Type type) throws IOException
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
        // Convert the security type to a standard value
        Map<String, String> typeMap = new HashMap<>();

        typeMap.put("common stock", Messages.LabelSearchShare);
        typeMap.put("common", Messages.LabelSearchShare);
        typeMap.put("new york registered shares", Messages.LabelSearchShare);
        typeMap.put("preferred stock", Messages.LabelSearchPreferredStock);
        typeMap.put("bond", Messages.LabelSearchBond);
        typeMap.put("warrant", Messages.LabelSearchWarrant);
        typeMap.put("etf", Messages.LabelSearchETF);
        typeMap.put("etc", Messages.LabelSearchETC);
        typeMap.put("exchange-traded note", Messages.LabelSearchETN);
        typeMap.put("fund", Messages.LabelSearchFund);
        typeMap.put("mutual fund", Messages.LabelSearchMutualFund);
        typeMap.put("mutualfund", Messages.LabelSearchMutualFund);
        typeMap.put("closed-end fund", Messages.LabelSearchCloseEndFund);
        typeMap.put("digital currency", Messages.LabelSearchCryptoCurrency);
        typeMap.put("cryptocurrency", Messages.LabelSearchCryptoCurrency);
        typeMap.put("index", Messages.LabelSearchIndex);
        typeMap.put("reit", Messages.LabelSearchReit);
        typeMap.put("real estate investment trust (reit)", Messages.LabelSearchReit);
        typeMap.put("future", Messages.LabelSearchFuture);
        typeMap.put("currency", Messages.LabelSearchCurrency);
        typeMap.put("physical currency", Messages.LabelSearchCurrency);

        return typeMap.getOrDefault(type, type);
    }
}
