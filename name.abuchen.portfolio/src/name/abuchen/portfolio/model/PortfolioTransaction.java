package name.abuchen.portfolio.model;

import java.time.LocalDate;
import java.util.ResourceBundle;

import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.money.Values;

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

    @Deprecated
    /* package */transient long fees;

    @Deprecated
    /* package */transient long taxes;

    public PortfolioTransaction()
    {}

    public PortfolioTransaction(LocalDate date, String currencyCode, long amount, Security security, long shares,
                    Type type, long fees, long taxes)
    {
        super(date, currencyCode, amount, security, shares, null);
        this.type = type;

        if (fees != 0)
            addUnit(new Unit(Unit.Type.FEE, Money.of(currencyCode, fees)));
        if (taxes != 0)
            addUnit(new Unit(Unit.Type.TAX, Money.of(currencyCode, taxes)));
    }

    public PortfolioTransaction(String date, String currencyCode, long amount, Security security, long shares,
                    Type type, long fees, long taxes)
    {
        this(LocalDate.parse(date), currencyCode, amount, security, shares, type, fees, taxes);
    }

    public Type getType()
    {
        return type;
    }

    public void setType(Type type)
    {
        this.type = type;
    }

    /**
     * Returns the gross value, i.e. the value including taxes and fees. See
     * {@link #getGrossValue()}.
     * @return long
     */
    public long getGrossValueAmount()
    {
        long taxAndFees = getUnits().filter(u -> u.getType() == Unit.Type.TAX || u.getType() == Unit.Type.FEE)
                        .collect(MoneyCollectors.sum(getCurrencyCode(), u -> u.getAmount())).getAmount();

        switch (this.type)
        {
            case BUY:
            case TRANSFER_IN:
            case DELIVERY_INBOUND:
                return getAmount() - taxAndFees;
            case SELL:
            case TRANSFER_OUT:
            case DELIVERY_OUTBOUND:
                return getAmount() + taxAndFees;
            default:
                throw new UnsupportedOperationException("Unsupport transaction type: "); //$NON-NLS-1$
        }
    }

    /**
     * Returns the gross value, i.e. the value before taxes and fees are
     * applied. In the case of a buy transaction, that are the gross costs, i.e.
     * before adding additional taxes and fees. In the case of sell
     * transactions, that are the gross proceeds before the deduction of taxes
     * and fees.
     * @return Money
     */
    public Money getGrossValue()
    {
        return Money.of(getCurrencyCode(), getGrossValueAmount());
    }

    /**
     * Returns the gross price per share. See {@link #getGrossPricePerShare()}.
     * @return long
     */
    public long getGrossPricePerShareAmount()
    {
        if (getShares() == 0)
            return 0;

        return getGrossValueAmount() * Values.Share.factor() / getShares();
    }

    /**
     * Returns the gross price per share, i.e. the gross value divided by the
     * number of shares bought or sold.
     * @return Money
     */
    public Money getGrossPricePerShare()
    {
        return Money.of(getCurrencyCode(), getGrossPricePerShareAmount());
    }
}
