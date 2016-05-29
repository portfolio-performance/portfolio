package name.abuchen.portfolio.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.ResourceBundle;

import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;

public class AccountTransaction extends Transaction
{
    public enum Type
    {
        DEPOSIT, REMOVAL, INTEREST, DIVIDENDS, FEES, TAXES, TAX_REFUND, BUY, SELL, TRANSFER_IN, TRANSFER_OUT;

        private static final ResourceBundle RESOURCES = ResourceBundle.getBundle("name.abuchen.portfolio.model.labels"); //$NON-NLS-1$

        @Override
        public String toString()
        {
            return RESOURCES.getString("account." + name()); //$NON-NLS-1$
        }
    }

    /**
     * Comparator to stort by date, amount, and type in order to have a stable
     * enough sort order to calculate the balance per transaction.
     */
    public static final class ByDateAmountAndType implements Comparator<AccountTransaction>, Serializable
    {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare(AccountTransaction t1, AccountTransaction t2)
        {
            int compare = t1.getDate().compareTo(t2.getDate());
            if (compare != 0)
                return compare;

            compare = Long.compare(t1.getAmount(), t2.getAmount());
            if (compare != 0)
                return compare;

            return t1.getType().compareTo(t2.getType());
        }
    }

    private Type type;

    public AccountTransaction()
    {
        // needed for xstream de-serialization
    }

    public AccountTransaction(LocalDate date, String currencyCode, long amount, Security security, Type type)
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

    /**
     * Returns the gross value, i.e. the value including taxes. See
     * {@link #getGrossValue()}.
     */
    public long getGrossValueAmount()
    {
        // at the moment, only dividend transaction support taxes
        if (!(this.type == Type.DIVIDENDS || this.type == Type.INTEREST))
            throw new UnsupportedOperationException();

        long taxes = getUnits().filter(u -> u.getType() == Unit.Type.TAX)
                        .collect(MoneyCollectors.sum(getCurrencyCode(), u -> u.getAmount())).getAmount();

        return getAmount() + taxes;
    }

    /**
     * Returns the gross value, i.e. the value before taxes are applied. At the
     * moment, only dividend transactions are supported.
     */
    public Money getGrossValue()
    {
        return Money.of(getCurrencyCode(), getGrossValueAmount());
    }
}
