package name.abuchen.portfolio.model;

import java.util.List;

public class InvestmentPlan
{
    private Security security;
    private float amount;
    private List<PortfolioTransaction> transactions;
    private Portfolio portfolio;
    public Security getSecurity()
    {
        return security;
    }
    public void setSecurity(Security security)
    {
        this.security = security;
    }

}
