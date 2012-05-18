package name.abuchen.portfolio.snapshot;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Values;

public class SecurityPosition
{
    private Security security;
    private SecurityPrice price;
    private long shares;

    public SecurityPosition(Security security)
    {
        this.security = security;
    }

    public SecurityPosition(Security security, SecurityPrice price, long shares)
    {
        this(security);
        this.price = price;
        this.shares = shares;
    }

    public Security getSecurity()
    {
        return security;
    }

    public SecurityPrice getPrice()
    {
        return price;
    }

    public void setPrice(SecurityPrice price)
    {
        this.price = price;
    }

    public long getShares()
    {
        return shares;
    }

    public void setShares(long shares)
    {
        this.shares = shares;
    }

    public long calculateValue()
    {
        long p = price != null ? price.getValue() : 0;
        return shares * p / Values.Share.factor();
    }

}
