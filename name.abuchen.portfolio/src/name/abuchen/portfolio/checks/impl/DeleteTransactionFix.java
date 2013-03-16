package name.abuchen.portfolio.checks.impl;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.checks.QuickFix;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Transaction;

/* package */class DeleteTransactionFix implements QuickFix
{
    private Object entity;
    private Transaction transaction;

    public DeleteTransactionFix(Object entity, Transaction transaction)
    {
        this.entity = entity;
        this.transaction = transaction;
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
        if (entity instanceof Account)
            ((Account) entity).getTransactions().remove(transaction);
        else
            ((Portfolio) entity).getTransactions().remove(transaction);
    }
}
