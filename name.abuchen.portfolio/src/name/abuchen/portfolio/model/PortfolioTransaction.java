package name.abuchen.portfolio.model;

import java.util.Date;
import java.util.ResourceBundle;

public class PortfolioTransaction extends Transaction
{
    public enum Type
    {
        BUY, SELL, TRANSFER_IN, TRANSFER_OUT, DELIVERY_INBOUND, DELIVERY_OUTBOUND;

        private static final ResourceBundle RESOURCES = ResourceBundle.getBundle("name.abuchen.portfolio.model.labels"); //$NON-NLS-1$

        public String toString()
        {
            return RESOURCES.getString("portfolio." + name()); //$NON-NLS-1$
        }
    }

    private Type type;
    private long amount;
    private long fees;
    private long taxes;

    public PortfolioTransaction()
    {}

    public PortfolioTransaction(Date date, Security security, Type type, long shares, long amount, long fees, long taxes)
    {
        super(date, security, shares);
        this.type = type;
        this.amount = amount;
        this.fees = fees;
        this.taxes = taxes;
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

    public long getTaxes()
    {
        return taxes;
    }

    public void setTaxes(long taxes)
    {
        this.taxes = taxes;
    }

    public long getLumpSumPrice()
    {
        switch (this.type)
        {
            case BUY:
            case TRANSFER_IN:
            case DELIVERY_INBOUND:
                return amount - fees - taxes;
            case SELL:
            case TRANSFER_OUT:
            case DELIVERY_OUTBOUND:
                return amount + fees + taxes;
            default:
                throw new UnsupportedOperationException("Unsupport transaction type: "); //$NON-NLS-1$
        }
    }

    /**
     * Returns the purchase price before fees
     */
    public long getActualPurchasePrice()
    {
        if (getShares() == 0)
            return 0;

        return getLumpSumPrice() * Values.Share.factor() / getShares();
    }
}
