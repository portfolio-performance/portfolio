package name.abuchen.portfolio.model;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

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
        else
            return null;
    }
}
