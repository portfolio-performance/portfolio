package name.abuchen.portfolio.checks.impl;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.checks.Check;
import name.abuchen.portfolio.checks.Issue;
import name.abuchen.portfolio.checks.QuickFix;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionOwner;
import name.abuchen.portfolio.model.TransactionPair;

/**
 * Checks if there is at least one account or security without a currency.
 */
public class TransactionCurrencyCheck implements Check
{
    private static class TransactionMissingCurrencyIssue implements Issue
    {
        private Client client;
        private TransactionPair<Transaction> pair;

        public TransactionMissingCurrencyIssue(Client client, TransactionPair<Transaction> pair)
        {
            this.client = client;
            this.pair = pair;
        }

        @Override
        public LocalDate getDate()
        {
            return pair.getTransaction().getDateTime().toLocalDate();
        }

        @Override
        public Object getEntity()
        {
            return pair.getOwner();
        }

        @Override
        public Long getAmount()
        {
            return pair.getTransaction().getAmount();
        }

        @Override
        public String getLabel()
        {
            String type = pair.getTransaction() instanceof AccountTransaction at ? at.getType().toString()
                            : ((PortfolioTransaction) pair.getTransaction())
                            .getType().toString();
            return MessageFormat.format(Messages.IssueTransactionMissingCurrencyCode, type);
        }

        @Override
        public List<QuickFix> getAvailableFixes()
        {
            return List.of(new DeleteTransactionFix<Transaction>(client, pair.getOwner(), pair.getTransaction()));
        }
    }

    @Override
    public List<Issue> execute(Client client)
    {
        Set<Object> transactions = new HashSet<Object>();

        for (Account account : client.getAccounts())
        {
            account.getTransactions()
                            .stream()
                            .filter(t -> t.getCurrencyCode() == null)
                            .forEach(t -> transactions.add(t.getCrossEntry() != null ? t.getCrossEntry()
                                            : new TransactionPair<AccountTransaction>(account, t)));
        }

        for (Portfolio portfolio : client.getPortfolios())
        {
            portfolio.getTransactions()
                            .stream()
                            .filter(t -> t.getCurrencyCode() == null)
                            .forEach(t -> transactions.add(t.getCrossEntry() != null ? t.getCrossEntry()
                                            : new TransactionPair<PortfolioTransaction>(portfolio, t)));
        }

        List<Issue> issues = new ArrayList<Issue>();

        for (Object t : transactions)
        {
            if (t instanceof TransactionPair<?>)
            {
                @SuppressWarnings("unchecked")
                TransactionPair<Transaction> pair = (TransactionPair<Transaction>) t;
                issues.add(new TransactionMissingCurrencyIssue(client, pair));
            }
            else if (t instanceof BuySellEntry entry)
            {
                @SuppressWarnings("unchecked")
                TransactionPair<Transaction> pair = new TransactionPair<Transaction>(
                                (TransactionOwner<Transaction>) entry.getOwner(entry.getAccountTransaction()),
                                entry.getAccountTransaction());
                issues.add(new TransactionMissingCurrencyIssue(client, pair));
            }
            else if (t instanceof AccountTransferEntry entry)
            {
                @SuppressWarnings("unchecked")
                TransactionPair<Transaction> pair = new TransactionPair<Transaction>(
                                (TransactionOwner<Transaction>) entry.getOwner(entry.getSourceTransaction()),
                                entry.getSourceTransaction());
                issues.add(new TransactionMissingCurrencyIssue(client, pair));
            }
            else if (t instanceof PortfolioTransferEntry entry)
            {
                @SuppressWarnings("unchecked")
                TransactionPair<Transaction> pair = new TransactionPair<Transaction>(
                                (TransactionOwner<Transaction>) entry.getOwner(entry.getSourceTransaction()),
                                entry.getSourceTransaction());
                issues.add(new TransactionMissingCurrencyIssue(client, pair));
            }
            else
            {
                throw new IllegalArgumentException(
                                "unsupported transaction entry " + t.getClass() + ": " + t.toString()); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }

        return issues;
    }
}
