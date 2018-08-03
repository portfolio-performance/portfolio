package name.abuchen.portfolio.model;

public abstract class CrossEntry
{
    public abstract void updateFrom(Transaction t);

    public abstract TransactionOwner<? extends Transaction> getOwner(Transaction t);

    public abstract Transaction getCrossTransaction(Transaction t);

    public abstract TransactionOwner<? extends Transaction> getCrossOwner(Transaction t);

    public abstract void insert();

    private TransactionOwner<Transaction> transactionOwner;

    private TransactionOwner<Transaction> otherTransactionOwner;

    public abstract void setTransactionOwner(TransactionOwner<Transaction> transactionOwner);

    public abstract void setOtherTransactionOwner(TransactionOwner<Transaction> transactionOwner);

    public abstract TransactionOwner<Transaction> getTransactionOwner();

    public abstract TransactionOwner<Transaction> getOtherTransactionOwner();
}
