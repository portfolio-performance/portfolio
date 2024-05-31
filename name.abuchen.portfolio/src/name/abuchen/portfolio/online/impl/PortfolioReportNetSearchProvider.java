package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.util.List;

import name.abuchen.portfolio.online.SecuritySearchProvider;

public class PortfolioReportNetSearchProvider implements SecuritySearchProvider
{
    private List<ResultItem> coins;

    @Override
    public String getName()
    {
        return "Portfolio Report Search Provider"; //$NON-NLS-1$
    }

    @Override
    public List<ResultItem> search(String query, Type type) throws IOException
    {
        return new PortfolioReportNet().search(query, type);
    }

    /**
     * As the available cryptos are needed fairly often (and change rarely), we
     * cache the search result.
     */
    @Override
    public synchronized List<ResultItem> getCoins() throws IOException
    {
        if (coins == null)
        {
            coins = search("", Type.CRYPTO); //$NON-NLS-1$
        }

        return coins;
    }
}
