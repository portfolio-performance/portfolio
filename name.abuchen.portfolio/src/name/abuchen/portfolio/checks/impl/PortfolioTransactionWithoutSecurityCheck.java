package name.abuchen.portfolio.checks.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.checks.Check;
import name.abuchen.portfolio.checks.Issue;
import name.abuchen.portfolio.checks.QuickFix;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;

public class PortfolioTransactionWithoutSecurityCheck implements Check
{
    public static class MissingSecurityIssue implements Issue
    {
        private Client client;
        private Portfolio portfolio;
        private PortfolioTransaction transaction;

        public MissingSecurityIssue(Client client, Portfolio portfolio, PortfolioTransaction transaction)
        {
            this.client = client;
            this.portfolio = portfolio;
            this.transaction = transaction;
        }

        @Override
        public LocalDate getDate()
        {
            return transaction.getDateTime().toLocalDate();
        }

        @Override
        public Object getEntity()
        {
            return portfolio;
        }

        @Override
        public Long getAmount()
        {
            return transaction.getAmount();
        }

        @Override
        public String getLabel()
        {
            return Messages.IssuePortfolioTransactionWithoutSecurity;
        }

        @Override
        public List<QuickFix> getAvailableFixes()
        {
            return List.of(new DeleteTransactionFix<PortfolioTransaction>(client, portfolio, transaction));
        }
    }

    @Override
    public List<Issue> execute(Client client)
    {
        List<Issue> issues = new ArrayList<Issue>();

        for (Portfolio portfolio : client.getPortfolios())
        {
            portfolio.getTransactions().stream() //
                            .filter(t -> t.getSecurity() == null) //
                            .forEach(t -> issues.add(new MissingSecurityIssue(client, portfolio, t)));

        }

        return issues;
    }

}
