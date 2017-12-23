package name.abuchen.portfolio.checks.impl;

import java.time.LocalDate;

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
    public LocalDate getDate()
    {
        return transaction != null ? transaction.getDateTime().toLocalDate() : null;
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
