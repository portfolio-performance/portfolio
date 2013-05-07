package name.abuchen.portfolio.snapshot;

import java.util.Date;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;

public class InvestmentPlanController
{

    private InvestmentPlan plan;

    public InvestmentPlanController(InvestmentPlan plan)
    {
        this.plan = plan;
    }

    public void generateTransactions()
    {
        // Generate a dummy for testing
        PortfolioTransaction temp = new PortfolioTransaction(new Date(), plan.getSecurity(),
                        PortfolioTransaction.Type.BUY, 1, 1000, 10);
        plan.addTransaction(temp);
    }

    public void updateTransactions()
    {
        Portfolio portfolio = plan.getPortfolio();
        Account account = portfolio.getReferenceAccount();
        for (PortfolioTransaction trans : plan.getTransactions())
        {
            if (!portfolio.getTransactions().contains(trans))
            {
                portfolio.addTransaction(trans);
                // Create the cross transaction
                AccountTransaction crossTransaction = new AccountTransaction(trans.getDate(), trans.getSecurity(),
                                AccountTransaction.Type.BUY, trans.getAmount());
                account.addTransaction(crossTransaction);
            }
        }
    }

}
