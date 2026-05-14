package name.abuchen.portfolio.model;

public class ClientMergeResult
{
    private final Client client;
    private final int filesMerged;
    private final int securitiesReused;
    private final int securitiesAdded;
    private final int accountsImported;
    private final int portfoliosImported;
    private final int investmentPlansImported;
    private final int watchlistsImported;

    public ClientMergeResult(Client client, int filesMerged, int securitiesReused, int securitiesAdded,
                    int accountsImported, int portfoliosImported, int investmentPlansImported, int watchlistsImported)
    {
        this.client = client;
        this.filesMerged = filesMerged;
        this.securitiesReused = securitiesReused;
        this.securitiesAdded = securitiesAdded;
        this.accountsImported = accountsImported;
        this.portfoliosImported = portfoliosImported;
        this.investmentPlansImported = investmentPlansImported;
        this.watchlistsImported = watchlistsImported;
    }

    public Client getClient()
    {
        return client;
    }

    public int getFilesMerged()
    {
        return filesMerged;
    }

    public int getSecuritiesReused()
    {
        return securitiesReused;
    }

    public int getSecuritiesAdded()
    {
        return securitiesAdded;
    }

    public int getAccountsImported()
    {
        return accountsImported;
    }

    public int getPortfoliosImported()
    {
        return portfoliosImported;
    }

    public int getInvestmentPlansImported()
    {
        return investmentPlansImported;
    }

    public int getWatchlistsImported()
    {
        return watchlistsImported;
    }
}
