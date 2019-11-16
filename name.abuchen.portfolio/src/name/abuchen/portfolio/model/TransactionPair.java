package name.abuchen.portfolio.model;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

import name.abuchen.portfolio.money.Values;

/**
 * A pair of transaction owner (account or portfolio) and a transaction.
 */
public class TransactionPair<T extends Transaction> implements Adaptable
{
    public static final class ByDate implements Comparator<TransactionPair<?>>, Serializable
    {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare(TransactionPair<?> t1, TransactionPair<?> t2)
        {
            return t1.getTransaction().getDateTime().compareTo(t2.getTransaction().getDateTime());
        }
    }

    private final TransactionOwner<T> owner;
    private final T transaction;

    public TransactionPair(TransactionOwner<T> owner, T transaction)
    {
        this.owner = Objects.requireNonNull(owner);
        this.transaction = Objects.requireNonNull(transaction);
    }

    public TransactionOwner<T> getOwner()
    {
        return owner;
    }

    public T getTransaction()
    {
        return transaction;
    }

    /**
     * Returns this if it wraps an AccountTransaction.
     */
    @SuppressWarnings("unchecked")
    public Optional<TransactionPair<AccountTransaction>> withAccountTransaction()
    {
        if (transaction instanceof AccountTransaction)
            return Optional.of((TransactionPair<AccountTransaction>) this);
        else
            return Optional.empty();
    }

    /**
     * Returns this if it wraps an PortfolioTransaction.
     */
    @SuppressWarnings("unchecked")
    public Optional<TransactionPair<PortfolioTransaction>> withPortfolioTransaction()
    {
        if (transaction instanceof PortfolioTransaction)
            return Optional.of((TransactionPair<PortfolioTransaction>) this);
        else
            return Optional.empty();
    }

    /**
     * Deletes the transaction from the transaction owner, e.g. the portfolio or
     * account.
     */
    public void deleteTransaction(Client client)
    {
        owner.deleteTransaction(transaction, client);
    }

    @Override
    public <A> A adapt(Class<A> type)
    {
        if (type == Annotated.class)
            return type.cast(transaction);
        else if (type == Transaction.class)
            return type.cast(transaction);
        else if (type == Security.class)
            return type.cast(transaction.getSecurity());
        else
            return null;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((owner == null) ? 0 : owner.hashCode());
        result = prime * result + ((transaction == null) ? 0 : transaction.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        TransactionPair<?> other = (TransactionPair<?>) obj;
        return owner.equals(other.owner) && transaction.equals(other.transaction);
    }

    @Override
    public String toString()
    {
        return String.format("%s %10s Stk. %-10s %s  %s", //$NON-NLS-1$
                        Values.DateTime.format(transaction.getDateTime()), //
                        Values.Share.format(transaction.getShares()), //
                        getTypeString(), //
                        Values.Money.format(transaction.getMonetaryAmount()), //
                        owner.toString());
    }

    private String getTypeString()
    {
        if (transaction instanceof AccountTransaction)
            return ((AccountTransaction) transaction).getType().toString();
        else if (transaction instanceof PortfolioTransaction)
            return ((PortfolioTransaction) transaction).getType().toString();
        else
            return ""; //$NON-NLS-1$
    }
}
