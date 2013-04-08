package name.abuchen.portfolio.snapshot;

import java.util.List;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Category;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;

public class CategoryIndex extends PerformanceIndex
{
    public static CategoryIndex forPeriod(Client client, Category category, ReportingPeriod reportInterval,
                    List<Exception> warnings)
    {
        CategoryIndex index = new CategoryIndex(client, category, reportInterval);
        index.calculate(warnings);
        return index;
    }

    private Category category;

    private CategoryIndex(Client client, Category category, ReportingPeriod reportInterval)
    {
        super(client, reportInterval);
        this.category = category;
    }

    private void calculate(List<Exception> warnings)
    {
        Client pseudoClient = new Client();
        Portfolio portfolio = new Portfolio();
        pseudoClient.addPortfolio(portfolio);

        for (Object object : category.getTreeElements())
        {
            if (object instanceof Security)
            {
                Security security = (Security) object;
                pseudoClient.addSecurity(security);

                for (Portfolio p : getClient().getPortfolios())
                {
                    for (PortfolioTransaction t : p.getTransactions())
                    {
                        if (t.getSecurity().equals(security))
                            portfolio.addTransaction(t);
                    }
                }
            }
            else if (object instanceof Account)
            {
                pseudoClient.addAccount((Account) object);
            }
        }

        ClientIndex clientIndex = ClientIndex.forPeriod(pseudoClient, getReportInterval(), warnings);

        dates = clientIndex.getDates();
        totals = clientIndex.getTotals();
    }
}
