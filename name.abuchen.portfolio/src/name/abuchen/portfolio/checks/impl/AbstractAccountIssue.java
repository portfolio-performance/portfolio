package name.abuchen.portfolio.checks.impl;

import java.time.LocalDate;

import name.abuchen.portfolio.checks.Issue;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;

/* package */abstract class AbstractAccountIssue implements Issue
{
    protected Client client;
    protected Account account;
    protected AccountTransaction transaction;

    public AbstractAccountIssue(Client client, Account account, AccountTransaction transaction)
    {
        this.client = client;
        this.account = account;
        this.transaction = transaction;
    }

    @Override
    public LocalDate getDate()
    {
        return transaction.getDateTime().toLocalDate();
    }

    @Override
    public Account getEntity()
    {
        return account;
    }

    @Override
    public Long getAmount()
    {
        return transaction.getAmount();
    }
}
