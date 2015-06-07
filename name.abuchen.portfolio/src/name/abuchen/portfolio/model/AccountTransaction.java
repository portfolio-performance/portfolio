package name.abuchen.portfolio.model;

import java.util.Date;
import java.util.ResourceBundle;

public class AccountTransaction extends Transaction
{
    public enum Type
    {
        DEPOSIT, REMOVAL, INTEREST, DIVIDENDS, FEES, TAXES, TAX_REFUND, BUY, SELL, TRANSFER_IN, TRANSFER_OUT;

        private static final ResourceBundle RESOURCES = ResourceBundle.getBundle("name.abuchen.portfolio.model.labels"); //$NON-NLS-1$

        public String toString()
        {
            return RESOURCES.getString("account." + name()); //$NON-NLS-1$
        }
    }

    private Type type;

    public AccountTransaction()
    {}

    public AccountTransaction(Date date, String currencyCode, long amount, Security security, Type type)
    {
        super(date, currencyCode, amount, security, 0, null);
        this.type = type;
    }

    public Type getType()
    {
        return type;
    }

    public void setType(Type type)
    {
        this.type = type;
    }

    @Override
    public boolean isPotentialDuplicate(Transaction other)
    {
        if (!(other instanceof AccountTransaction))
            return false;

        if (!super.isPotentialDuplicate(other))
            return false;

        return type == ((AccountTransaction) other).getType();
    }
}
