package name.abuchen.portfolio.datatransfer.pdf;

import java.io.IOException;
import java.util.List;

import name.abuchen.portfolio.online.SecuritySearchProvider;
import name.abuchen.portfolio.online.impl.CoinGeckoQuoteFeed.Coin;

public class TestCoinSearchProvider
{
    /**
     * Returns a list of search providers that include Bitcoin, Etherum, and
     * Stellar crypto currencies for testing purposes.
     */
    public static List<SecuritySearchProvider> cryptoProvider()
    {
        // mock the list of coins to avoid remote call
        return List.of(new SecuritySearchProvider()
        {
            @SuppressWarnings("nls")
            @Override
            public List<ResultItem> getCoins() throws IOException
            {
                return List.of( //
                                new Coin("bitcoin", "BTC", "Bitcoin").asResultItem(),
                                new Coin("ethereum", "ETH", "Ethereum").asResultItem(),
                                new Coin("stellar", "XLM", "Stellar").asResultItem());
            }
        });
    }

}
