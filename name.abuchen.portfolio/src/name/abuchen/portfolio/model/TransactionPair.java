package name.abuchen.portfolio.model;

/**
 * A pair of transaction owner (account or portfolio) and a transaction.
 */
public class TransactionPair<T extends Transaction>
{
    private final TransactionOwner<T> owner;
    private final T transaction;

    public TransactionPair(TransactionOwner<T> owner, T transaction)
    {
        this.owner = owner;
        this.transaction = transaction;
    }

    public TransactionOwner<T> getOwner()
    {
        return owner;
    }

    public T getTransaction()
    {
        return transaction;
    }
}
