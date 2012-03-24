package name.abuchen.portfolio.model;

import java.util.Date;
import java.util.EnumSet;

public class PortfolioTransaction extends Transaction
{
    public enum Type
    {
        BUY, SELL, TRANSFER_IN, TRANSFER_OUT
    }

    private Type type;
    private int shares;
    private int amount;
    private int fees;

    public PortfolioTransaction()
    {}

    public PortfolioTransaction(Date date, Security security, Type type, int shares, int amount, int fees)
    {
        super(date, security);
        this.type = type;
        this.shares = shares;
        this.amount = amount;
        this.fees = fees;
    }

    public Type getType()
    {
        return type;
    }

    public void setType(Type type)
    {
        this.type = type;
    }

    public int getShares()
    {
        return shares;
    }

    public void setShares(int shares)
    {
        this.shares = shares;
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

    public int getFees()
    {
        return fees;
    }

    public void setFees(int fees)
    {
        this.fees = fees;
    }

    public int getActualPurchasePrice()
    {
        if (shares == 0)
            return 0;

        switch (this.type)
        {
            case BUY:
            case TRANSFER_IN:
                return (amount - fees) / shares;
            case SELL:
            case TRANSFER_OUT:
                return (amount + fees) / shares;
            default:
                throw new UnsupportedOperationException("Unsupport transaction type: "); //$NON-NLS-1$
        }
    }

    @Override
    @SuppressWarnings("nls")
    public String toString()
    {
        int v = getAmount();
        if (EnumSet.of(Type.BUY, Type.TRANSFER_IN).contains(type))
            v = -v;

        return String.format("%tF %-12s %-18s %,10.2f", getDate(), type, getSecurity().getTickerSymbol(), v / 100d);
    }

}
