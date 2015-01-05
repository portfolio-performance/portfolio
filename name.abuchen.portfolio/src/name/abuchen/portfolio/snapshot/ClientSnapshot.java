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

    @Deprecated
    public static ClientSnapshot create(Client client, Date time)
    {
        CurrencyConverter converter = new CurrencyConverterImpl(null, client.getBaseCurrency());
        return create(client, converter, time);
    }

    public static ClientSnapshot create(Client client, CurrencyConverter converter, Date date)
    {
        ClientSnapshot snapshot = new ClientSnapshot(client, converter, date);

        for (Account account : client.getAccounts())
            snapshot.accounts.add(AccountSnapshot.create(account, converter, date));

        for (Portfolio portfolio : client.getPortfolios())
            snapshot.portfolios.add(PortfolioSnapshot.create(portfolio, converter, date));

        if (snapshot.portfolios.isEmpty())
            snapshot.jointPortfolio = PortfolioSnapshot.create(new Portfolio(), converter, date);
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
    private final Date date;

    private List<AccountSnapshot> accounts = new ArrayList<AccountSnapshot>();
    private List<PortfolioSnapshot> portfolios = new ArrayList<PortfolioSnapshot>();
    private PortfolioSnapshot jointPortfolio = null;

    private ClientSnapshot(Client client, CurrencyConverter converter, Date date)
    {
        this.client = client;
        this.converter = converter;
        this.date = date;
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
        return date;
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
            answer.put(p.getSecurity(), new AssetPosition(p, converter, date, assets));

        for (AccountSnapshot account : accounts)
            answer.put(account.getAccount(), new AssetPosition(new SecurityPosition(account), converter, date, assets));

        return answer;
    }
}
