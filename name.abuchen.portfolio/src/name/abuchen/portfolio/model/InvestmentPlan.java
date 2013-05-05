package name.abuchen.portfolio.model;

import java.util.Date;
import java.util.List;

public class InvestmentPlan
{
    private Security security;
    private float amount;
    private List<PortfolioTransaction> transactions;
    private Portfolio portfolio;
    private String name;
    private Date start;
    private int period;
    
    public InvestmentPlan(String name)
    {
        super();
        this.setName(name);
    }
    public Security getSecurity()
    {
        return security;
    }
    public void setSecurity(Security security)
    {
        this.security = security;
    }
    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        this.name = name;
    }

}
