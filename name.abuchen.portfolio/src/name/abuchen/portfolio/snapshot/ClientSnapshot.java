package name.abuchen.portfolio.snapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MutableMoney;

public class ClientSnapshot
{
    // //////////////////////////////////////////////////////////////
    // factory methods
    // //////////////////////////////////////////////////////////////

    public static ClientSnapshot create(Client client, Date time)
    {
        CurrencyConverter converter = new CurrencyConverterImpl(null, client.getBaseCurrency(), time);
        return create(client, converter, time);
    }

    public static ClientSnapshot create(Client client, CurrencyConverter converter, Date time)
    {
        ClientSnapshot snapshot = new ClientSnapshot(client, converter);

        for (Account account : client.getAccounts())
            snapshot.accounts.add(AccountSnapshot.create(account, converter, time));

        for (Portfolio portfolio : client.getPortfolios())
            snapshot.portfolios.add(PortfolioSnapshot.create(portfolio, converter, time));

        if (snapshot.portfolios.isEmpty())
            snapshot.jointPortfolio = PortfolioSnapshot.create(new Portfolio(), converter, time);
        else if (snapshot.portfolios.size() == 1)
            snapshot.jointPortfolio = snapshot.portfolios.get(0);
        else
            snapshot.jointPortfolio = PortfolioSnapshot.merge(snapshot.portfolios);

        return snapshot;
    }

    // //////////////////////////////////////////////////////////////
    // instance impl
    // //////////////////////////////////////////////////////////////

    private final Client client;
    private final CurrencyConverter converter;

    private List<AccountSnapshot> accounts = new ArrayList<AccountSnapshot>();
    private List<PortfolioSnapshot> portfolios = new ArrayList<PortfolioSnapshot>();
    private PortfolioSnapshot jointPortfolio = null;

    private ClientSnapshot(Client client, CurrencyConverter converter)
    {
        this.client = client;
        this.converter = converter;
    }

    public Client getClient()
    {
        return client;
    }

    public String getCurrencyCode()
    {
        return converter.getTermCurrency();
    }

    public CurrencyConverter getCurrencyConverter()
    {
        return converter;
    }

    public Date getTime()
    {
        return converter.getTime();
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

    public Money getMonetaryAssets()
    {
        MutableMoney assets = MutableMoney.of(getCurrencyCode());

        for (AccountSnapshot account : accounts)
            assets.add(account.getFunds());

        for (PortfolioSnapshot portfolio : portfolios)
            assets.add(portfolio.getValue());

        return assets.toMoney();
    }

    @Deprecated
    public long getAssets()
    {
        return getMonetaryAssets().getAmount();
    }

    public GroupByTaxonomy groupByTaxonomy(Taxonomy taxonomy)
    {
        return new GroupByTaxonomy(taxonomy, this);
    }

    public Map<InvestmentVehicle, AssetPosition> getPositionsByVehicle()
    {
        Map<InvestmentVehicle, AssetPosition> answer = new HashMap<InvestmentVehicle, AssetPosition>();

        Money assets = getMonetaryAssets();

        for (SecurityPosition p : jointPortfolio.getPositions())
            answer.put(p.getSecurity(), new AssetPosition(p, converter, assets));

        for (AccountSnapshot account : accounts)
            answer.put(account.getAccount(), new AssetPosition(new SecurityPosition(account), converter, assets));

        return answer;
    }
}
