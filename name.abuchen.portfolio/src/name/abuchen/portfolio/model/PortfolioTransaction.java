package name.abuchen.portfolio.model;

import java.util.Date;
import java.util.ResourceBundle;

import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

import org.joda.time.DateMidnight;

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
    private long fees;
    private long taxes;

    public PortfolioTransaction()
    {}

    public PortfolioTransaction(Date date, String currencyCode, long amount, Security security, long shares, Type type,
                    long fees, long taxes)
    {
        super(date, currencyCode, amount, security, shares, null);
        this.type = type;
        this.fees = fees;
        this.taxes = taxes;
    }

    public PortfolioTransaction(String date, String currencyCode, long amount, Security security, long shares,
                    Type type, long fees, long taxes)
    {
        this(new DateMidnight(date).toDate(), currencyCode, amount, security, shares, type, fees, taxes);
    }

    public Type getType()
    {
        return type;
    }

    public void setType(Type type)
    {
        this.type = type;
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
                return getAmount() - fees - taxes;
            case SELL:
            case TRANSFER_OUT:
            case DELIVERY_OUTBOUND:
                return getAmount() + fees + taxes;
            default:
                throw new UnsupportedOperationException("Unsupport transaction type: "); //$NON-NLS-1$
        }
    }

    public Money getLumpSum()
    {
        return Money.of(getCurrencyCode(), getLumpSumPrice());
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

    public Money getPricePerShare()
    {
        return Money.of(getCurrencyCode(), getActualPurchasePrice());
    }
}
