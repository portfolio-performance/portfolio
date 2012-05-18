package name.abuchen.portfolio.snapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
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

    private static class TotalsCategory extends AssetCategory
    {

        public TotalsCategory(long valuation)
        {
            super(null, valuation);
            this.valuation = valuation;
        }

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

    public List<AssetCategory> groupByCategory()
    {
        TotalsCategory totals = new TotalsCategory(this.getAssets());

        List<AssetCategory> categories = new ArrayList<AssetCategory>();
        Map<Security.AssetClass, AssetCategory> class2category = new HashMap<Security.AssetClass, AssetCategory>();

        for (Security.AssetClass ac : Security.AssetClass.values())
        {
            AssetCategory category = new AssetCategory(ac, totals.getValuation());
            categories.add(category);
            class2category.put(ac, category);
        }

        // total line
        categories.add(totals);

        // cash
        AssetCategory cash = class2category.get(Security.AssetClass.CASH);
        for (AccountSnapshot a : this.getAccounts())
        {
            SecurityPosition sp = new SecurityPosition(null);
            sp.setShares(Values.Share.factor());
            sp.setPrice(new SecurityPrice(this.getTime(), a.getFunds()));
            AssetPosition ap = new AssetPosition(sp, a.getAccount().getName(), totals.getValuation());
            cash.addPosition(ap);
        }

        // portfolios
        if (this.getJointPortfolio() != null)
        {
            for (SecurityPosition pos : this.getJointPortfolio().getPositions())
            {
                AssetCategory cat = class2category.get(pos.getSecurity().getType());
                cat.addPosition(new AssetPosition(pos, totals.getValuation()));
            }
        }

        for (AssetCategory cat : categories)
            Collections.sort(cat.getPositions());

        return categories;
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
            buf.append(String.format("%-66s %,10.2f\n", snapshot.getAccount().getName(), funds / 100d));
            assets += funds;
        }

        buf.append(SEPERATOR);

        for (PortfolioSnapshot snapshot : portfolios)
        {
            buf.append(String.format("%-66s %,10.2f\n", snapshot.getSource().getName(), snapshot.getValue() / 100d));

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
                                p.getPrice().getValue() / 100d, //
                                p.calculateValue() / 100d));

            buf.append(SEPERATOR);
            assets += snapshot.getValue();
        }

        buf.append(String.format("%-67s %,10.2f\n", "Total Assets", assets / 100d));
        buf.append(SEPERATOR);

        return buf.toString();
    }

}
