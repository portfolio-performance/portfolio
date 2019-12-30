package name.abuchen.portfolio.model;

public interface CrossEntry
{
    void updateFrom(Transaction t);

    TransactionOwner<? extends Transaction> getOwner(Transaction t);

    void setOwner(Transaction t, TransactionOwner<? extends Transaction> owner);

    Transaction getCrossTransaction(Transaction t);

    TransactionOwner<? extends Transaction> getCrossOwner(Transaction t);

    void insert();
}
