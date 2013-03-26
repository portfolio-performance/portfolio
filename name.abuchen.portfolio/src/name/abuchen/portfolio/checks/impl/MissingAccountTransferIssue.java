package name.abuchen.portfolio.checks.impl;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.checks.QuickFix;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.Client;

/* package */class MissingAccountTransferIssue extends AbstractAccountIssue
{
    private final class CreateTransferFix implements QuickFix
    {
        private Account crossAccount;

        private CreateTransferFix(Account crossAccount)
        {
            this.crossAccount = crossAccount;
        }

        @Override
        public String getLabel()
        {
            return MessageFormat.format(Messages.FixCreateTransfer, crossAccount.getName());
        }

        @Override
        public String getDoneLabel()
        {
            AccountTransaction.Type target = null;
            if (transaction.getType() == AccountTransaction.Type.TRANSFER_IN)
                target = AccountTransaction.Type.TRANSFER_OUT;
            else
                target = AccountTransaction.Type.TRANSFER_IN;

            return MessageFormat.format(Messages.FixCreateTransferDone, target.toString());
        }

        @Override
        public void execute()
        {
            Account from = null;
            Account to = null;

            if (transaction.getType() == AccountTransaction.Type.TRANSFER_IN)
            {
                from = crossAccount;
                to = account;
            }
            else
            {
                from = account;
                to = crossAccount;
            }

            AccountTransferEntry entry = new AccountTransferEntry(from, to);
            entry.setDate(transaction.getDate());
            entry.setAmount(transaction.getAmount());
            entry.insert();

            account.getTransactions().remove(transaction);
        }
    }

    public MissingAccountTransferIssue(Client client, Account account, AccountTransaction transaction)
    {
        super(client, account, transaction);
    }

    @Override
    public String getLabel()
    {
        return MessageFormat.format(Messages.IssueMissingAccountTransfer, transaction.getType().toString());
    }

    @Override
    public List<QuickFix> getAvailableFixes()
    {
        List<QuickFix> answer = new ArrayList<QuickFix>();

        for (Account a : client.getAccounts())
        {
            if (a.equals(account))
                continue;
            answer.add(new CreateTransferFix(a));
        }
        answer.add(new DeleteTransactionFix(account, transaction));

        return answer;
    }

}
