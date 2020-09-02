package name.abuchen.portfolio.checks.impl;

import java.text.MessageFormat;
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
import name.abuchen.portfolio.model.Security;

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
            List<QuickFix> fixes = new ArrayList<QuickFix>();

            fixes.add(new DeleteTransactionFix<PortfolioTransaction>(client, portfolio, transaction));

            for (Security security : client.getSecurities())
                fixes.add(new SetSecurityFix(security, transaction));

            return fixes;
        }
    }

    public static class SetSecurityFix implements QuickFix
    {
        private Security security;
        private PortfolioTransaction transaction;

        public SetSecurityFix(Security security, PortfolioTransaction transaction)
        {
            this.security = security;
            this.transaction = transaction;
        }

        @Override
        public String getLabel()
        {
            return MessageFormat.format(Messages.FixSetSecurity, security.getName());
        }

        @Override
        public String getDoneLabel()
        {
            return MessageFormat.format(Messages.FixSetSecurityDone, security.getName());
        }

        @Override
        public void execute()
        {
            transaction.setSecurity(security);
            if (transaction.getCrossEntry() != null)
                transaction.getCrossEntry().updateFrom(transaction);
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
