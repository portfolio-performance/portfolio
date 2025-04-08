package name.abuchen.portfolio.checks.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.checks.Check;
import name.abuchen.portfolio.checks.Issue;
import name.abuchen.portfolio.checks.QuickFix;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionOwner;

public class MissingTransactionDateCheck implements Check
{
    public class MissingDateFix implements QuickFix
    {
        private Transaction tx;

        public MissingDateFix(Transaction tx)
        {
            this.tx = tx;
        }

        @Override
        public String getLabel()
        {
            return Messages.FixSetDateToToday;
        }

        @Override
        public String getDoneLabel()
        {
            return Messages.FixSetDateToTodayDone;
        }

        @Override
        public void execute()
        {
            tx.setDateTime(LocalDate.now().atStartOfDay());
        }
    }

    private final class MissingDateIssue<T extends Transaction> implements Issue
    {
        private final TransactionOwner<T> owner;
        private final T tx;

        public MissingDateIssue(TransactionOwner<T> owner, T tx)
        {
            this.owner = owner;
            this.tx = tx;
        }

        @Override
        public LocalDate getDate()
        {
            // if the transaction has been fixed, the date should be
            return tx.getDateTime() != null ? tx.getDateTime().toLocalDate() : null;
        }

        @Override
        public Object getEntity()
        {
            return owner;
        }

        @Override
        public Long getAmount()
        {
            return tx.getAmount();
        }

        @Override
        public String getLabel()
        {
            return Messages.IssueTransactionWithoutDate;
        }

        @Override
        public List<QuickFix> getAvailableFixes()
        {
            List<QuickFix> fixes = new ArrayList<>();
            fixes.add(new MissingDateFix(tx));
            return fixes;
        }
    }

    @Override
    public List<Issue> execute(Client client)
    {
        var answer = new ArrayList<Issue>();

        for (Account account : client.getAccounts())
        {
            for (AccountTransaction tx : account.getTransactions())
            {
                if (tx.getDateTime() == null)
                {
                    answer.add(new MissingDateIssue<>(account, tx));
                }
            }
        }

        for (Portfolio portfolio : client.getPortfolios())
        {
            for (PortfolioTransaction tx : portfolio.getTransactions())
            {
                if (tx.getDateTime() == null)
                {
                    answer.add(new MissingDateIssue<>(portfolio, tx));
                }
            }
        }

        return answer;
    }
}
