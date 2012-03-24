package name.abuchen.portfolio.model;

import java.util.Date;
import java.util.EnumSet;

public class AccountTransaction extends Transaction
{
    public enum Type
    {
        DEPOSIT, REMOVAL, INTEREST, DIVIDENDS, FEES, TAXES, BUY, SELL, TRANSFER_IN, TRANSFER_OUT
    }

    private Type type;

    private int amount;

    public AccountTransaction()
    {}

    public AccountTransaction(Date date, Security security, Type type, int amount)
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

    @Override
    public int getAmount()
    {
        return amount;
    }

    public void setAmount(int amount)
    {
        this.amount = amount;
    }

    @Override
    @SuppressWarnings("nls")
    public String toString()
    {
        int v = amount;
        if (EnumSet.of(Type.REMOVAL, Type.FEES, Type.TAXES, Type.TRANSFER_OUT, Type.BUY).contains(type))
            v = -v;

        return String.format("%tF                                 %,10.2f", getDate(), v / 100d);
    }

}
