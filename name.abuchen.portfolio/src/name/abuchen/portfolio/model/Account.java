package name.abuchen.portfolio.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Account implements InvestmentVehicle
{
    private String uuid;
    private String name;

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

    public List<AccountTransaction> getTransactions()
    {
        return transactions;
    }

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
