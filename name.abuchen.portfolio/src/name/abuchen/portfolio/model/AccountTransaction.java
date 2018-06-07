package name.abuchen.portfolio.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.ResourceBundle;

import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.money.Values;

public class AccountTransaction extends Transaction
{
    public enum Type
    {
        DEPOSIT(false), REMOVAL(true), //
        INTEREST(false), INTEREST_CHARGE(true), //
        DIVIDENDS(false), DIVIDEND_CHARGE(true), //
        FEES(true), FEES_REFUND(false), //
        TAXES(true), TAX_REFUND(false), //
        BUY(true), SELL(false), //
        TRANSFER_IN(false), TRANSFER_OUT(true);

        private static final ResourceBundle RESOURCES = ResourceBundle.getBundle("name.abuchen.portfolio.model.labels"); //$NON-NLS-1$

        private final boolean isDebit;

        private Type(boolean isDebit)
        {
            this.isDebit = isDebit;
        }

        public boolean isDebit()
        {
            return isDebit;
        }

        public boolean isCredit()
        {
            return !isDebit;
        }

        @Override
        public String toString()
        {
            return RESOURCES.getString("account." + name()); //$NON-NLS-1$
        }

        public Type getSibling()
        {
            switch (this)
            {
                case DEPOSIT:
                    return REMOVAL;
                case REMOVAL:
                    return DEPOSIT;
                case INTEREST:
                    return INTEREST_CHARGE;
                case INTEREST_CHARGE:
                    return INTEREST;
                case DIVIDEND_CHARGE:
                    return DIVIDENDS;
                case DIVIDENDS:
                    return DIVIDEND_CHARGE;
                case FEES:
                    return FEES_REFUND;
                case FEES_REFUND:
                    return FEES;
                case TAXES:
                    return TAX_REFUND;
                case TAX_REFUND:
                    return TAXES;
                default:
                    return this;
            }
        }
    }

    /**
     * Comparator to sort by date, amount, type, and hash code in order to have
     * a stable enough sort order to calculate the balance per transaction.
     */
    public static final class ByDateAmountTypeAndHashCode implements Comparator<AccountTransaction>, Serializable
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

            compare = t1.getType().compareTo(t2.getType());
            if (compare != 0)
                return compare;

            return Integer.compare(t1.hashCode(), t2.hashCode());
        }
    }

    private Type type;

    private Peer peer;

    public AccountTransaction()
    {
        // needed for xstream de-serialization
    }

    public AccountTransaction(LocalDate date, String currencyCode, long amount, Security security, Type type)
    {
        super(date, currencyCode, amount, security, 0, null);
        this.type = type;
        voidPeer();
    }

    public Peer getPeer()
    {
        return peer;
    }

    public void setPeer(Peer peer)
    {
        this.peer = peer;
    }

    public void voidPeer()
    {
        this.peer = null;
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
        if (!(this.type == Type.DIVIDENDS || this.type == Type.INTEREST || this.type == Type.DIVIDEND_CHARGE))
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

    @Override
    public String toString()
    {
        return String.format("%s %-17s %s %9s %s %s %s"
                        , Values.Date.format(getDate())
                        , type.name()
                        , getCurrencyCode() //$NON-NLS-1$
                        , Values.Amount.format(getAmount())
                        , getSecurity() != null ? getSecurity().getName() : "<no Security>"
                        , peer != null ? peer.getName() : "<no Peer>"
                        , getCrossEntry() != null && getCrossEntry().getCrossOwner(this) != null? getCrossEntry().getCrossOwner(this).toString() : "<no XEntry>"
                            ); //$NON-NLS-1$
    }
}
