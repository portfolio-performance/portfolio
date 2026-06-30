package name.abuchen.portfolio.checks.impl;

import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.checks.Check;
import name.abuchen.portfolio.checks.Issue;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;

public class CrossEntryCheck implements Check
{

    @Override
    public List<Issue> execute(Client client)
    {
        List<Issue> issues = new ArrayList<Issue>();

        for (Account account : client.getAccounts())
        {
            for (AccountTransaction transaction : account.getTransactions())
            {
                if (transaction.getCrossEntry() != null)
                    continue;

                reportAccountIssue(client, account, transaction, issues);
            }
        }

        for (Portfolio portfolio : client.getPortfolios())
        {
            for (PortfolioTransaction transaction : portfolio.getTransactions())
            {
                if (transaction.getCrossEntry() != null)
                    continue;

                reportPortfolioIssue(client, portfolio, transaction, issues);
            }
        }

        return issues;
    }

    private void reportAccountIssue(Client client, Account account, AccountTransaction transaction, List<Issue> issues)
    {
        switch (transaction.getType())
        {
            case BUY:
            case SELL:
                if (LedgerCheckSupport.isLedgerBacked(transaction))
                    issues.add(new MissingBuySellPortfolioIssue(client, account, transaction));
                else if (transaction.getSecurity() == null)
                    issues.add(new BuySellMissingSecurityIssue(client, account, transaction));
                else
                    issues.add(new MissingBuySellPortfolioIssue(client, account, transaction));
                break;
            case TRANSFER_IN:
            case TRANSFER_OUT:
                issues.add(new MissingAccountTransferIssue(client, account, transaction));
                break;
            default:
                break;
        }
    }

    private void reportPortfolioIssue(Client client, Portfolio portfolio, PortfolioTransaction transaction,
                    List<Issue> issues)
    {
        switch (transaction.getType())
        {
            case BUY:
            case SELL:
                if (transaction.getSecurity() == null)
                    issues.add(new PortfolioTransactionWithoutSecurityCheck.MissingSecurityIssue(client, portfolio,
                                    transaction));
                else
                    issues.add(new MissingBuySellAccountIssue(client, portfolio, transaction));
                break;
            case TRANSFER_IN:
            case TRANSFER_OUT:
                if (transaction.getSecurity() == null)
                    issues.add(new PortfolioTransactionWithoutSecurityCheck.MissingSecurityIssue(client, portfolio,
                                    transaction));
                else
                    issues.add(new MissingPortfolioTransferIssue(client, portfolio, transaction));
                break;
            default:
                break;
        }
    }
}
