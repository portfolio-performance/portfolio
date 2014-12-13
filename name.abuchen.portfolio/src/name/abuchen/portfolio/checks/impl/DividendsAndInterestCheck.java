package name.abuchen.portfolio.checks.impl;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.checks.Check;
import name.abuchen.portfolio.checks.Issue;
import name.abuchen.portfolio.checks.QuickFix;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransaction.Type;
import name.abuchen.portfolio.model.Client;

public class DividendsAndInterestCheck implements Check
{
    private static final class DividendsAndInterestIssue extends AbstractAccountIssue
    {
        private Type target;

        public DividendsAndInterestIssue(Client client, Account account, AccountTransaction transaction, Type target)
        {
            super(client, account, transaction);
            this.target = target;
        }

        @Override
        public String getLabel()
        {
            if (target == Type.INTEREST)
                return Messages.IssueDividendWithoutSecurity;
            else
                return MessageFormat.format(Messages.IssueInterestWithSecurity, transaction.getSecurity().getName());
        }

        @Override
        public List<QuickFix> getAvailableFixes()
        {
            List<QuickFix> answer = new ArrayList<QuickFix>();
            answer.add(new ConvertFix(transaction, target));
            answer.add(new DeleteTransactionFix<AccountTransaction>(client, account, transaction));
            return answer;
        }
    }

    private static final class ConvertFix implements QuickFix
    {
        private AccountTransaction transaction;
        private Type target;

        public ConvertFix(AccountTransaction transaction, Type target)
        {
            this.transaction = transaction;
            this.target = target;
        }

        @Override
        public String getLabel()
        {
            return MessageFormat.format(Messages.FixConvertToType, target);
        }

        @Override
        public void execute()
        {
            transaction.setType(target);
        }

        @Override
        public String getDoneLabel()
        {
            return MessageFormat.format(Messages.FixConvertToTypeDone, target);
        }
    }

    @Override
    public List<Issue> execute(Client client)
    {
        List<Issue> answer = new ArrayList<Issue>();

        for (Account account : client.getAccounts())
        {
            for (AccountTransaction transaction : account.getTransactions())
            {
                if (transaction.getType() == Type.DIVIDENDS && transaction.getSecurity() == null)
                {
                    answer.add(new DividendsAndInterestIssue(client, account, transaction, Type.INTEREST));
                }
                else if (transaction.getType() == Type.INTEREST && transaction.getSecurity() != null)
                {
                    answer.add(new DividendsAndInterestIssue(client, account, transaction, Type.DIVIDENDS));
                }
            }
        }

        return answer;
    }
}
