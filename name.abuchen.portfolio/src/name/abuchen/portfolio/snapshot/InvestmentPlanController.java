package name.abuchen.portfolio.snapshot;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.util.Dates;

public class InvestmentPlanController
{
    private static final int TOLERANCE = 3;

    private InvestmentPlan plan;

    public InvestmentPlanController(InvestmentPlan plan)
    {
        this.plan = plan;
    }

    private Date getLastActionDate()
    {
        Date result = null;
        for (PortfolioTransaction t : plan.getTransactions())
        {
            if (result == null || result.before(t.getDate()))
            {
                result = t.getDate();
            }
        }
        if (result == null) { return plan.getStart(); }
        return result;
    }

    public void generateTransactions()
    {
        List<PortfolioTransaction> present = plan.getPortfolio().getTransactions();
        Collections.sort(present, new Comparator<PortfolioTransaction>()
        {
            @Override
            public int compare(PortfolioTransaction p1, PortfolioTransaction p2)
            {
                return p1.getDate().compareTo(p2.getDate());
            }

        });
        // Start from the date of the latest transaction of the plan to not
        // re-create deleted transactions
        Date current = getLastActionDate();
        Date today = Dates.today();
        while (current.before(today))
        {
            boolean alreadyPresent = false;
            for (PortfolioTransaction p : present)
            {
                if (p.getSecurity().equals(plan.getSecurity()))
                {
                    if (Dates.isSameMonth(current, p.getDate()))
                    {
                        if (Dates.daysBetween(current, p.getDate()) <= TOLERANCE)
                        {
                            alreadyPresent = true;
                            break;
                        }
                    }
                }
            }
            if (!alreadyPresent)
            {
                long amount = plan.getAmount();
                long price = plan.getSecurity().getSecurityPrice(current).getValue();
                long shares = (long) (((double) amount / price) * Values.Share.factor());
                if (plan.isGenerateAccountTransactions())
                {
                    BuySellEntry entry = new BuySellEntry(plan.getPortfolio(), plan.getPortfolio()
                                    .getReferenceAccount());
                    entry.setType(PortfolioTransaction.Type.BUY);
                    entry.setDate(current);
                    entry.setShares(shares);
                    entry.setAmount(amount);
                    entry.setSecurity(plan.getSecurity());
                    entry.insert();
                    plan.addTransaction(entry.getPortfolioTransaction());
                }
                else
                {
                    PortfolioTransaction transaction = new PortfolioTransaction(current, plan.getSecurity(),
                                    PortfolioTransaction.Type.DELIVERY_INBOUND, shares, amount,
                                    plan.getTransactionCost());
                    plan.addTransaction(transaction);
                    plan.getPortfolio().addTransaction(transaction);
                }
            }
            current = Dates.progress(current, plan.getDayOfMonth());
        }
    }

}
