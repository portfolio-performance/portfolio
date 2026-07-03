package name.abuchen.portfolio.model;

import java.util.List;

public interface CrossEntry
{
    void updateFrom(Transaction t);

    TransactionOwner<? extends Transaction> getOwner(Transaction t);

    void setOwner(Transaction t, TransactionOwner<? extends Transaction> owner);

    Transaction getCrossTransaction(Transaction t);

    /**
     * Returns every other transaction linked by this cross entry (all legs
     * except {@code t}). A binary cross entry (buy/sell, transfers) has exactly
     * one counterpart, so the default reduces to {@link #getCrossTransaction};
     * an N-ary {@link CorporateActionEntry} returns all sibling legs.
     */
    default List<Transaction> getCrossTransactions(Transaction t)
    {
        return List.of(getCrossTransaction(t));
    }

    TransactionOwner<? extends Transaction> getCrossOwner(Transaction t);

    void insert();

    String getSource();

    void setSource(String source);
}
