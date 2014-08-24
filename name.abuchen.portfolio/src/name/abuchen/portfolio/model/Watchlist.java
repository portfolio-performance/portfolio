package name.abuchen.portfolio.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Watchlist
{
    private String name;
    private List<Security> securities = new ArrayList<Security>();
    private Map<Security, Float> targetPrices = new HashMap<Security, Float>();

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
        securities.add(security);
    }

    public void addTargetPrice(Security security, Float price)
    {
        targetPrices.put(security, price);
    }

    public Map<Security, Float> getTargetPrices()
    {
        return this.targetPrices;
    }

    public Float getTargetPriceForSecurity(Security security)
    {
        if (this.targetPrices.containsKey(security)) { return this.targetPrices.get(security); }
        return null;
    }
}
