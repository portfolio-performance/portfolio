package name.abuchen.portfolio.checks.impl;

import java.util.Date;

import name.abuchen.portfolio.checks.Issue;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;

/* package */abstract class AbstractPortfolioIssue implements Issue
{
    protected Client client;
    protected Portfolio portfolio;
    protected PortfolioTransaction transaction;

    public AbstractPortfolioIssue(Client client, Portfolio portfolio, PortfolioTransaction transaction)
    {
        this.client = client;
        this.portfolio = portfolio;
        this.transaction = transaction;
    }

    @Override
    public Date getDate()
    {
        return transaction != null ? transaction.getDate() : null;
    }

    @Override
    public Portfolio getEntity()
    {
        return portfolio;
    }

    @Override
    public Long getAmount()
    {
        return transaction != null ? transaction.getAmount() : null;
    }
}
