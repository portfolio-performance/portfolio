package name.abuchen.portfolio.model;

public abstract class CrossEntry
{
    public abstract void updateFrom(Transaction t);

    public abstract TransactionOwner<? extends Transaction> getOwner(Transaction t);

    public abstract Transaction getCrossTransaction(Transaction t);

    public abstract TransactionOwner<? extends Transaction> getCrossOwner(Transaction t);

    public abstract void insert();

    private TransactionOwner<Transaction> primaryTransactionOwner;

    private TransactionOwner<Transaction> secondaryTransactionOwner;

    public abstract void setPrimaryTransactionOwner(TransactionOwner<Transaction> transactionOwner);

    public abstract void setSecondaryTransactionOwner(TransactionOwner<Transaction> transactionOwner);

    public abstract TransactionOwner<Transaction> getPrimaryTransactionOwner();

    public abstract TransactionOwner<Transaction> getSecondaryTransactionOwner();
}
