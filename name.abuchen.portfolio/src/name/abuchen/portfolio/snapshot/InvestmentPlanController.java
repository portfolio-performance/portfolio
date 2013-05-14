package name.abuchen.portfolio.snapshot;

import java.util.Date;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Values;

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
        long amount = plan.getAmount();
        long price = plan.getSecurity().getLatest().getValue();
        float shareF = (float) amount / price;
        long shares = new Float(shareF * Values.Share.divider()).longValue();
        PortfolioTransaction temp = new PortfolioTransaction(new Date(), plan.getSecurity(),
                        PortfolioTransaction.Type.BUY, shares, amount, 0);
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
