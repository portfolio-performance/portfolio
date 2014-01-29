package name.abuchen.portfolio.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class Account implements TransactionOwner<AccountTransaction>, InvestmentVehicle
{
    public static final class ByName implements Comparator<Account>, Serializable
    {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare(Account a1, Account a2)
        {
            if (a1 == null)
                return a2 == null ? 0 : -1;
            return a1.name.compareTo(a2.name);
        }
    }

    private String uuid;
    private String name;
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
