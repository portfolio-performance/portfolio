package name.abuchen.portfolio.model;

import java.util.ArrayList;
import java.util.List;

public class Watchlist
{
    private String name;
    private List<Security> securities = new ArrayList<>();

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public List<Security> getSecurities()
    {
        return securities;
    }

    public void addSecurity(Security security)
    {
        if (!securities.contains(security))
            securities.add(security);
    }
}
