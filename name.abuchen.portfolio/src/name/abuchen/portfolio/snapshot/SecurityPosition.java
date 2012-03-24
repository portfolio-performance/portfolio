package name.abuchen.portfolio.snapshot;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;

public class SecurityPosition
{
    private Security security;
    private SecurityPrice price;
    private int shares;

    public SecurityPosition(Security security)
    {
        this.security = security;
    }

    public SecurityPosition(Security security, SecurityPrice price, int count)
    {
        this(security);
        this.price = price;
        this.shares = count;
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

    public int getShares()
    {
        return shares;
    }

    public void setShares(int shares)
    {
        this.shares = shares;
    }

    public int calculateValue()
    {
        int p = price != null ? price.getValue() : 0;
        return shares * p;
    }

}
