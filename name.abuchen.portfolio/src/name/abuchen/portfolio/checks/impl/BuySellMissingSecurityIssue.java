package name.abuchen.portfolio.checks.impl;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.checks.QuickFix;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;

/* package */class BuySellMissingSecurityIssue extends AbstractAccountIssue
{
    public BuySellMissingSecurityIssue(Client client, Account account, AccountTransaction transaction)
    {
        super(client, account, transaction);
    }

    @Override
    public String getLabel()
    {
        return MessageFormat.format(Messages.IssueBuySellWithoutSecurity, transaction.getType().toString());
    }

    @Override
    public List<QuickFix> getAvailableFixes()
    {
        List<QuickFix> answer = new ArrayList<QuickFix>();
        answer.add(new DeleteTransactionFix<AccountTransaction>(client, account, transaction));
        return answer;
    }
}
