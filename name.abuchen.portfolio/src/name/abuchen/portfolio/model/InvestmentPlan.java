package name.abuchen.portfolio.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class InvestmentPlan
{
    private Security security;
    private long amount;
    private long transactionCost;
    private List<PortfolioTransaction> transactions;
    private Portfolio portfolio;
    private String name;
    private Date start;
    private int dayOfMonth;
    
    public InvestmentPlan() {
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



}
