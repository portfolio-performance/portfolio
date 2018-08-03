package name.abuchen.portfolio.model;

public interface CrossEntry
{
    void updateFrom(Transaction t);

    TransactionOwner<? extends Transaction> getOwner(Transaction t);

    Transaction getCrossTransaction(Transaction t);

    TransactionOwner<? extends Transaction> getCrossOwner(Transaction t);

    void insert();

    void setPrimaryTransactionOwner(TransactionOwner<Transaction> transactionOwner);

    TransactionOwner<Transaction> getPrimaryTransactionOwner();

    void setSecondaryTransactionOwner(TransactionOwner<Transaction> transactionOwner);

    TransactionOwner<Transaction> getSecondaryTransactionOwner();
}
