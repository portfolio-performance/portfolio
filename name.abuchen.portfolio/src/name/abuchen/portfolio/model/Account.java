package name.abuchen.portfolio.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Account implements TransactionOwner<AccountTransaction>, InvestmentVehicle
{
    private String uuid;
    private String name;
    private String note;
    private boolean isRetired = false;

    private List<AccountTransaction> transactions = new ArrayList<AccountTransaction>();

    public Account()
    {
        this.uuid = UUID.randomUUID().toString();
    }

    public Account(String name)
    {
        this();
        this.name = name;
    }

    @Override
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

    public boolean isRetired()
    {
        return isRetired;
    }

    public void setRetired(boolean isRetired)
    {
        this.isRetired = isRetired;
    }

    @Override
    public List<AccountTransaction> getTransactions()
    {
        return transactions;
    }

    @Override
    public void addTransaction(AccountTransaction transaction)
    {
        this.transactions.add(transaction);
    }

    @Override
    public void deleteTransaction(AccountTransaction transaction, Client client)
    {
        // FIXME Use Java 8 default methods
        if (transaction.getCrossEntry() != null)
        {
            Transaction other = transaction.getCrossEntry().getCrossTransaction(transaction);
            @SuppressWarnings("unchecked")
            TransactionOwner<Transaction> owner = (TransactionOwner<Transaction>) transaction.getCrossEntry()
                            .getEntity(other);

            owner.shallowDeleteTransaction(other, client);
        }

        shallowDeleteTransaction(transaction, client);
    }

    @Override
    public void shallowDeleteTransaction(AccountTransaction transaction, Client client)
    {
        this.transactions.remove(transaction);
    }

    public long getCurrentAmount()
    {
        long amount = 0;

        for (AccountTransaction t : transactions)
        {
            switch (t.getType())
            {
                case DEPOSIT:
                case DIVIDENDS:
                case INTEREST:
                case SELL:
                case TRANSFER_IN:
                    amount += t.getAmount();
                    break;
                case FEES:
                case TAXES:
                case REMOVAL:
                case BUY:
                case TRANSFER_OUT:
                    amount -= t.getAmount();
                    break;
                default:
                    throw new RuntimeException("Unknown Account Transaction type: " + t.getType()); //$NON-NLS-1$
            }
        }

        return amount;
    }

    @Override
    public String toString()
    {
        return getName();
    }

}
