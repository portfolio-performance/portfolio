package name.abuchen.portfolio.checks.impl;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.checks.QuickFix;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionOwner;
import name.abuchen.portfolio.model.TransactionPair;

/* package */class DeleteTransactionFix<T extends Transaction> implements QuickFix
{
    private Client client;
    private TransactionOwner<T> owner;
    private T transaction;

    public DeleteTransactionFix(Client client, TransactionOwner<T> owner, T transaction)
    {
        this.client = client;
        this.owner = owner;
        this.transaction = transaction;
    }

    public DeleteTransactionFix(Client client, TransactionPair<T> tx)
    {
        this(client, tx.getOwner(), tx.getTransaction());
    }

    @Override
    public String getLabel()
    {
        return Messages.FixDeleteTransaction;
    }

    @Override
    public String getDoneLabel()
    {
        return Messages.FixDeleteTransactionDone;
    }

    @Override
    public void execute()
    {
        owner.deleteTransaction(transaction, client);
    }
}
