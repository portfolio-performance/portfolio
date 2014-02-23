package name.abuchen.portfolio.model;

public interface CrossEntry
{
    void updateFrom(Transaction t);

    void delete();

    TransactionOwner<? extends Transaction> getEntity(Transaction t);

    Transaction getCrossTransaction(Transaction t);

    TransactionOwner<? extends Transaction> getCrossEntity(Transaction t);
}
