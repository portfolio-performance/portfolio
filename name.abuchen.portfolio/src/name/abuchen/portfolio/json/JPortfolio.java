package name.abuchen.portfolio.json;

import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.snapshot.PortfolioSnapshot;

public class JPortfolio
{
    private String name;
    private String uuid;
    private Money snapshotValue;

    public String getName()
    {
        return name;
    }

    public String getUUID() 
    {
        return uuid;
    }
    
    public Money getSnapshotValue()
    {
        return snapshotValue;
    }
    
    public static JPortfolio from(Portfolio portfolio, PortfolioSnapshot snapshot)
    {
        JPortfolio p = new JPortfolio();
        p.name = portfolio.getName();
        p.uuid = portfolio.getUUID();
        p.snapshotValue = snapshot.getValue();
        return p;
    }

}
