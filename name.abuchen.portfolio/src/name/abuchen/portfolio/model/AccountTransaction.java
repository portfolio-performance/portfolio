package name.abuchen.portfolio.model;

import java.util.Date;
import java.util.ResourceBundle;

public class AccountTransaction extends Transaction
{
    public enum Type
    {
        DEPOSIT, REMOVAL, INTEREST, DIVIDENDS, FEES, TAXES, BUY, SELL, TRANSFER_IN, TRANSFER_OUT;

        private static final ResourceBundle RESOURCES = ResourceBundle.getBundle("name.abuchen.portfolio.model.labels"); //$NON-NLS-1$

        public String toString()
        {
            return RESOURCES.getString("account." + name()); //$NON-NLS-1$
        }
    }

    private Type type;

    private long shares;
    private long amount;
    
    private Object owner;

    public void setOwner(Object owner)
    {
        this.owner = owner;
    }

    public Object getOwner()
    {
        return owner;
    }

    public AccountTransaction()
    {}

    public AccountTransaction(Date date, Security security, Type type, long amount)
    {
        super(date, security);
        this.type = type;
        this.amount = amount;
    }

    public Type getType()
    {
        return type;
    }

    public void setType(Type type)
    {
        this.type = type;
    }

    public long getShares()
    {
        return shares;
    }

    public void setShares(long shares)
    {
        this.shares = shares;
    }

    @Override
    public long getAmount()
    {
        return amount;
    }

    public void setAmount(long amount)
    {
        this.amount = amount;
    }
}
