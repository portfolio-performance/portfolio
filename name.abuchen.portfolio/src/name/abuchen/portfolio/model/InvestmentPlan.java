package name.abuchen.portfolio.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import name.abuchen.portfolio.util.Dates;

public class InvestmentPlan
{

    private static final int TOLERANCE = 3;

    private Security security;
    private long amount;
    private long transactionCost;
    private List<PortfolioTransaction> transactions;
    private Portfolio portfolio;
    private String name;
    private Date start;
    private boolean isBuySellEntry;

    public InvestmentPlan()
    {
        transactions = new ArrayList<PortfolioTransaction>();
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

    public long getAmount()
    {
        return amount;
    }

    public void setAmount(long amount)
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

    public List<PortfolioTransaction> getTransactions()
    {
        return transactions;
    }

    public void addTransaction(PortfolioTransaction temp)
    {
        transactions.add(temp);
    }

    public long getTransactionCost()
    {
        return transactionCost;
    }

    public void setTransactionCost(long transactionCost)
    {
        this.transactionCost = transactionCost;
    }

    public boolean isBuySellEntry()
    {
        return isBuySellEntry;
    }

    public void setIsBuySellEntry(boolean isBuySellEntry)
    {
        this.isBuySellEntry = isBuySellEntry;
    }

    private Date getLastActionDate()
    {
        Date result = null;
        for (PortfolioTransaction t : getTransactions())
        {
            if (result == null || result.before(t.getDate()))
            {
                result = t.getDate();
            }
        }
        if (result == null) { return getStart(); }
        return result;
    }

    public List<Transaction> generateTransactions()
    {
        List<Transaction> newTransactions = new ArrayList<Transaction>();
        Transaction.sortByDate(transactions);
        // Start from the date of the latest transaction of the plan to not
        // re-create deleted transactions
        Date current = getLastActionDate();
        Date today = Dates.today();

        while (current.before(today))
        {
            boolean alreadyPresent = false;
            for (PortfolioTransaction p : transactions)
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
            if (!alreadyPresent)
            {
                long amount = getAmount();
                long price = getSecurity().getSecurityPrice(current).getValue();
                long shares = (long) (((double) amount / price) * Values.Share.factor());
                if (isBuySellEntry())
                {
                    BuySellEntry entry = new BuySellEntry(getPortfolio(), getPortfolio().getReferenceAccount());
                    entry.setType(PortfolioTransaction.Type.BUY);
                    entry.setDate(current);
                    entry.setShares(shares);
                    entry.setAmount(amount);
                    entry.setSecurity(getSecurity());
                    entry.insert();
                    addTransaction(entry.getPortfolioTransaction());
                    newTransactions.add(entry.getAccountTransaction());
                    newTransactions.add(entry.getPortfolioTransaction());
                }
                else
                {
                    PortfolioTransaction transaction = new PortfolioTransaction(current, getSecurity(),
                                    PortfolioTransaction.Type.DELIVERY_INBOUND, shares, amount, getTransactionCost());
                    addTransaction(transaction);
                    getPortfolio().addTransaction(transaction);
                    newTransactions.add(transaction);
                }
            }
            current = Dates.progress(current);
        }

        return newTransactions;
    }

}
