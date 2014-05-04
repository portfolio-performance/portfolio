package name.abuchen.portfolio.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Portfolio implements Named, TransactionOwner<PortfolioTransaction>
{
    private String uuid;
    private String name;
    private String note;

    private Account referenceAccount;

    private List<PortfolioTransaction> transactions = new ArrayList<PortfolioTransaction>();

    public Portfolio()
    {
        this.uuid = UUID.randomUUID().toString();
    }

    public String getUUID()
    {
        return uuid;
    }

    /* package */void generateUUID()
    {
        // needed to assign UUIDs when loading older versions from XML
        uuid = UUID.randomUUID().toString();
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public String getNote()
    {
        return note;
    }

    @Override
    public void setNote(String note)
    {
        this.note = note;
    }

    public Account getReferenceAccount()
    {
        return referenceAccount;
    }

    public void setReferenceAccount(Account referenceAccount)
    {
        this.referenceAccount = referenceAccount;
    }

    @Override
    public List<PortfolioTransaction> getTransactions()
    {
        return transactions;
    }

    @Override
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
