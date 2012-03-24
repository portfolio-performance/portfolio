package name.abuchen.portfolio.model;

import java.util.ArrayList;
import java.util.List;

public class Account
{
    private String name;

    private List<AccountTransaction> transactions = new ArrayList<AccountTransaction>();

    public String getName()
    {
        return name;
    }

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

    public int getCurrentAmount()
    {
        int amount = 0;

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
