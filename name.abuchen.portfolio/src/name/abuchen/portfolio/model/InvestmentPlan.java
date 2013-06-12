package name.abuchen.portfolio.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
    private int dayOfMonth;
    private boolean generateAccountTransactions;
    private boolean hasChanged;
    private List<Transaction> newTransactions;

    public InvestmentPlan()
    {
        transactions = new ArrayList<PortfolioTransaction>();
    }

    public InvestmentPlan(String name)
    {
        this();
        this.setName(name);
    }

    public boolean hasChanged()
    {
        return hasChanged;
    }

    public List<Transaction> getNewTransactions()
    {
        return newTransactions;
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

    public int getDayOfMonth()
    {
        return dayOfMonth;
    }

    public void setDayOfMonth(int period)
    {
        this.dayOfMonth = period;
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

    public boolean isGenerateAccountTransactions()
    {
        return generateAccountTransactions;
    }

    public void setGenerateAccountTransactions(boolean generateAccountTransaction)
    {
        this.generateAccountTransactions = generateAccountTransaction;
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

    public void generateTransactions()
    {
        hasChanged = false;
        newTransactions = new ArrayList<Transaction>();
        List<PortfolioTransaction> present = getPortfolio().getTransactions();
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
                if (p.getSecurity().equals(getSecurity()))
                {
                    if (Dates.isSameMonth(current, p.getDate()))
                    {
                        if (Dates.daysBetween(current, p.getDate()) <= TOLERANCE)
                        {
                            alreadyPresent = true;
                            if (!transactions.contains(p))
                            {
                                transactions.add(p);
                            }
                            break;
                        }
                    }
                }
            }
            if (!alreadyPresent)
            {
                hasChanged = true;
                long amount = getAmount();
                long price = getSecurity().getSecurityPrice(current).getValue();
                long shares = (long) (((double) amount / price) * Values.Share.factor());
                if (isGenerateAccountTransactions())
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
            current = Dates.progress(current, getDayOfMonth());
        }
    }

}
