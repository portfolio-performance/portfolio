package name.abuchen.portfolio.snapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.model.Values;

public class ClientSnapshot
{
    // //////////////////////////////////////////////////////////////
    // factory methods
    // //////////////////////////////////////////////////////////////

    public static ClientSnapshot create(Client client, Date time)
    {
        ClientSnapshot snapshot = new ClientSnapshot(client, time);

        for (Account account : client.getAccounts())
            snapshot.accounts.add(AccountSnapshot.create(account, time));

        for (Portfolio portfolio : client.getPortfolios())
            snapshot.portfolios.add(PortfolioSnapshot.create(portfolio, time));

        if (snapshot.portfolios.isEmpty())
            snapshot.jointPortfolio = PortfolioSnapshot.create(new Portfolio(), time);
        else if (snapshot.portfolios.size() == 1)
            snapshot.jointPortfolio = snapshot.portfolios.get(0);
        else
            snapshot.jointPortfolio = PortfolioSnapshot.merge(snapshot.portfolios);

        return snapshot;
    }

    // //////////////////////////////////////////////////////////////
    // instance impl
    // //////////////////////////////////////////////////////////////

    private Client client;
    private Date time;

    private List<AccountSnapshot> accounts = new ArrayList<AccountSnapshot>();
    private List<PortfolioSnapshot> portfolios = new ArrayList<PortfolioSnapshot>();
    private PortfolioSnapshot jointPortfolio = null;

    private ClientSnapshot(Client client, Date time)
    {
        this.client = client;
        this.time = time;
    }

    public Client getClient()
    {
        return client;
    }

    public Date getTime()
    {
        return time;
    }

    public List<AccountSnapshot> getAccounts()
    {
        return accounts;
    }

    public List<PortfolioSnapshot> getPortfolios()
    {
        return portfolios;
    }

    public PortfolioSnapshot getJointPortfolio()
    {
        return jointPortfolio;
    }

    public long getAssets()
    {
        long assets = 0;

        for (AccountSnapshot account : accounts)
            assets += account.getFunds();

        for (PortfolioSnapshot portfolio : portfolios)
            assets += portfolio.getValue();

        return assets;
    }

    public GroupByTaxonomy groupByTaxonomy(Taxonomy taxonomy)
    {
        return new GroupByTaxonomy(taxonomy, this);
    }

    public Map<InvestmentVehicle, AssetPosition> getPositionsByVehicle()
    {
        return getAssetPositions().collect(Collectors.toMap(AssetPosition::getInvestmentVehicle, v -> v));
    }

    public Stream<AssetPosition> getAssetPositions()
    {
        List<AssetPosition> answer = new ArrayList<AssetPosition>();

        long assets = getAssets();

        for (SecurityPosition p : jointPortfolio.getPositions())
            answer.add(new AssetPosition(p.getSecurity(), p, assets));

        for (AccountSnapshot a : accounts)
        {
            SecurityPosition sp = new SecurityPosition(new SecurityPrice(getTime(), a.getFunds()),
                            Values.Share.factor());
            answer.add(new AssetPosition(a.getAccount(), sp, assets));
        }

        return answer.stream();
    }
}
