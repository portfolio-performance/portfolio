package name.abuchen.portfolio.snapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Values;

public class ClientSnapshot
{
    private static final String SEPERATOR = "------------------------------------------------------------------------------\n"; //$NON-NLS-1$

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

    public GroupByAssetClass groupByAssetClass()
    {
        return new GroupByAssetClass(this);
    }

    @Override
    @SuppressWarnings("nls")
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append(SEPERATOR);
        buf.append(String.format("Date: %tF\n", time));
        buf.append(SEPERATOR);

        long assets = 0;

        for (AccountSnapshot snapshot : accounts)
        {
            long funds = snapshot.getFunds();
            buf.append(String.format("%-66s %,10.2f\n", snapshot.getAccount().getName(),
                            funds / Values.Amount.divider()));
            assets += funds;
        }

        buf.append(SEPERATOR);

        for (PortfolioSnapshot snapshot : portfolios)
        {
            buf.append(String.format("%-66s %,10.2f\n", snapshot.getSource().getName(), //
                            snapshot.getValue() / Values.Amount.divider()));

            buf.append(SEPERATOR);

            List<SecurityPosition> list = new ArrayList<SecurityPosition>(snapshot.getPositions());
            Collections.sort(list, new Comparator<SecurityPosition>()
            {
                public int compare(SecurityPosition o1, SecurityPosition o2)
                {
                    return o1.getSecurity().getName().compareTo(o2.getSecurity().getName());
                }
            });

            for (SecurityPosition p : list)
                buf.append(String.format("%5d %-50s %,10.2f %,10.2f\n", //
                                p.getShares(), //
                                p.getSecurity().getName(), //
                                p.getPrice().getValue() / Values.Quote.divider(), //
                                p.calculateValue() / Values.Amount.divider()));

            buf.append(SEPERATOR);
            assets += snapshot.getValue();
        }

        buf.append(String.format("%-67s %,10.2f\n", "Total Assets", assets / Values.Amount.divider()));
        buf.append(SEPERATOR);

        return buf.toString();
    }

}
