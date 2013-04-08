package name.abuchen.portfolio.snapshot;

import java.util.List;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;

public class SecurityInvestmentIndex extends PerformanceIndex
{
    public static SecurityInvestmentIndex forPeriod(Client client, Security security, ReportingPeriod reportInterval,
                    List<Exception> warnings)
    {
        SecurityInvestmentIndex index = new SecurityInvestmentIndex(client, security, reportInterval);
        index.calculate(warnings);
        return index;
    }

    private Security security;

    private SecurityInvestmentIndex(Client client, Security security, ReportingPeriod reportInterval)
    {
        super(client, reportInterval);
        this.security = security;
    }

    private void calculate(List<Exception> warnings)
    {
        Client c = new Client();
        Portfolio portfolio = new Portfolio();
        c.addPortfolio(portfolio);
        c.addSecurity(security);

        for (Portfolio p : getClient().getPortfolios())
        {
            for (PortfolioTransaction t : p.getTransactions())
            {
                if (t.getSecurity().equals(security))
                    portfolio.addTransaction(t);
            }
        }

        PortfolioIndex portfolioIndex = PortfolioIndex.forPeriod(c, portfolio, getReportInterval(), warnings);

        dates = portfolioIndex.getDates();
        totals = portfolioIndex.getTotals();
    }
}
