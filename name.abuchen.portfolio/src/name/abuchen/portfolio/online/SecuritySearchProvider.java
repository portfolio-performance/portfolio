package name.abuchen.portfolio.online;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

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

        /**
         * Returns the ID of the feed that provides historical prices for the
         * instrument. Returns null, if no feed will be configured (for example
         * in the case of DivvyDiary search provider).
         */
        String getFeedId();

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
}
