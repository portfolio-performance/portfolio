package name.abuchen.portfolio.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Portfolio implements Named, TransactionOwner<PortfolioTransaction>, Attributable
{
    private String uuid;
    private String name;
    private String note;
    private boolean isRetired = false;

    private Account referenceAccount;

    private List<PortfolioTransaction> transactions = new ArrayList<>();

    private Attributes attributes;

    private Instant updatedAt;

    public Portfolio()
    {
        this.uuid = UUID.randomUUID().toString();
    }

    public Portfolio(String name)
    {
        this();
        this.name = name;
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
        this.updatedAt = Instant.now();
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
        this.updatedAt = Instant.now();
    }

    public boolean isRetired()
    {
        return isRetired;
    }

    public void setRetired(boolean isRetired)
    {
        this.isRetired = isRetired;
        this.updatedAt = Instant.now();
    }

    public Account getReferenceAccount()
    {
        return referenceAccount;
    }

    public void setReferenceAccount(Account referenceAccount)
    {
        this.referenceAccount = referenceAccount;
        this.updatedAt = Instant.now();
    }

    @Override
    public Attributes getAttributes()
    {
        if (attributes == null)
            attributes = new Attributes();
        return attributes;
    }

    @Override
    public void setAttributes(Attributes attributes)
    {
        this.attributes = attributes;
        this.updatedAt = Instant.now();
    }

    public Instant getUpdatedAt()
    {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt)
    {
        this.updatedAt = updatedAt;
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

    @Override
    public void shallowDeleteTransaction(PortfolioTransaction transaction, Client client)
    {
        this.transactions.remove(transaction);

        client.getPlans().stream().forEach(plan -> plan.removeTransaction(transaction));
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
