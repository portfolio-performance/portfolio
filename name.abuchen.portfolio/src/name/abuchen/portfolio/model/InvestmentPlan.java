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
    
    public InvestmentPlan() {
        
    }
    
    public InvestmentPlan(String name)
    {
        this();
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
    public float getAmount()
    {
        return amount;
    }
    public void setAmount(float amount)
    {
        this.amount = amount;
    }
    public Portfolio getPortfolio()
    {
        return portfolio;
    }
    public void setPortfolio(Portfolio portfolio)
    {
        this.portfolio = portfolio;
    }
    public Date getStart()
    {
        return start;
    }
    public void setStart(Date start)
    {
        this.start = start;
    }
    public int getPeriod()
    {
        return period;
    }
    public void setPeriod(int period)
    {
        this.period = period;
    }

}
