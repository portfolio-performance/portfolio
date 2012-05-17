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
    private long shares;
    private long amount;
    private long fees;

    public PortfolioTransaction()
    {}

    public PortfolioTransaction(Date date, Security security, Type type, long shares, long amount, long fees)
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

    public long getFees()
    {
        return fees;
    }

    public void setFees(long fees)
    {
        this.fees = fees;
    }

    public long getActualPurchasePrice()
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
        long v = getAmount();
        if (EnumSet.of(Type.BUY, Type.TRANSFER_IN).contains(type))
            v = -v;

        return String.format("%tF %-12s %-18s %,10.2f", getDate(), type, getSecurity().getTickerSymbol(), v / 100d);
    }

}
