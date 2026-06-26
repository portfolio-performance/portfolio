package name.abuchen.portfolio.checks.impl;

import java.text.MessageFormat;
import java.util.List;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.checks.QuickFix;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;

/* package */class MissingBuySellPortfolioIssue extends AbstractAccountIssue
{
    public MissingBuySellPortfolioIssue(Client client, Account account, AccountTransaction transaction)
    {
        super(client, account, transaction);
    }

    @Override
    public String getLabel()
    {
        return MessageFormat.format(Messages.IssueMissingBuySellInPortfolio, transaction.getType().toString(),
                        transaction.getSecurity().getName());
    }

    @Override
    public List<QuickFix> getAvailableFixes()
    {
        return List.of(new DeleteTransactionFix<AccountTransaction>(client, account, transaction));
    }
}
