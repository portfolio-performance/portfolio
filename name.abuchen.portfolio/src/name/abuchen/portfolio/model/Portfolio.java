package name.abuchen.portfolio.model;

import java.util.ArrayList;
import java.util.List;

public class Portfolio
{
    private String name;

    private Account referenceAccount;

    private List<PortfolioTransaction> transactions = new ArrayList<PortfolioTransaction>();

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public Account getReferenceAccount()
    {
        return referenceAccount;
    }

    public void setReferenceAccount(Account referenceAccount)
    {
        this.referenceAccount = referenceAccount;
    }

    public List<PortfolioTransaction> getTransactions()
    {
        return transactions;
    }

    public void addTransaction(PortfolioTransaction transaction)
    {
        this.transactions.add(transaction);
    }

    public void addAllTransaction(List<PortfolioTransaction> transactions)
    {
        this.transactions.addAll(transactions);
    }

    @Override
    public String toString()
    {
        return name;
    }
}
