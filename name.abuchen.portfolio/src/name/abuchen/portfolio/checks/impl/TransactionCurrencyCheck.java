package name.abuchen.portfolio.checks.impl;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
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
import name.abuchen.portfolio.money.CurrencyUnit;

/**
 * Checks if there is at least one account or security without a currency.
 */
public class TransactionCurrencyCheck implements Check
{
    public static class TransactionCurrencyQuickFix implements QuickFix
    {
        private TransactionPair<?> pair;
        private String currencyCode;

        public TransactionCurrencyQuickFix(Client client, TransactionPair<?> pair)
        {
            this.pair = pair;

            // either take currency from account or from security. Use base
            // currency as a fallback
            this.currencyCode = pair.getOwner() instanceof Account ? ((Account) pair.getOwner()).getCurrencyCode()
                            : (pair.getOwner() instanceof Portfolio ? ((PortfolioTransaction) pair.getTransaction())
                                            .getSecurity().getCurrencyCode() : client.getBaseCurrency());
        }

        @Override
        public String getLabel()
        {
            return CurrencyUnit.getInstance(currencyCode).getLabel();
        }

        @Override
        public String getDoneLabel()
        {
            return MessageFormat.format(Messages.FixAssignCurrencyCodeDone, currencyCode);
        }

        @Override
        public void execute()
        {
            pair.getTransaction().setCurrencyCode(currencyCode);

            // since currency fixes are only created if the currency is
            // identical, we can safely set the currency on both transactions
            if (pair.getTransaction().getCrossEntry() != null)
            {
                pair.getTransaction().getCrossEntry().getCrossTransaction(pair.getTransaction())
                                .setCurrencyCode(currencyCode);
            }
        }
    }

    private static class TransactionMissingCurrencyIssue implements Issue
    {
        private Client client;
        private TransactionPair<Transaction> pair;
        private boolean isFixable;

        public TransactionMissingCurrencyIssue(Client client, TransactionPair<Transaction> pair)
        {
            this(client, pair, true);
        }

        public TransactionMissingCurrencyIssue(Client client, TransactionPair<Transaction> pair, boolean isFixable)
        {
            this.client = client;
            this.pair = pair;
            this.isFixable = isFixable;
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
            String type = pair.getTransaction() instanceof AccountTransaction ? ((AccountTransaction) pair
                            .getTransaction()).getType().toString() : ((PortfolioTransaction) pair.getTransaction())
                            .getType().toString();
            return MessageFormat.format(Messages.IssueTransactionMissingCurrencyCode, type);
        }

        @Override
        public List<QuickFix> getAvailableFixes()
        {
            List<QuickFix> fixes = new ArrayList<QuickFix>();

            fixes.add(new DeleteTransactionFix<Transaction>(client, pair.getOwner(), pair.getTransaction()));
            if (isFixable)
                fixes.add(new TransactionCurrencyQuickFix(client, pair));

            return fixes;
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
            else if (t instanceof BuySellEntry)
            {
                // attempt to fix it if both currencies are identical. If a fix
                // involves currency conversion plus exchange rates, just offer
                // to delete the transaction.

                BuySellEntry entry = (BuySellEntry) t;
                String accountCurrency = entry.getAccount().getCurrencyCode();
                String securityCurrency = entry.getPortfolioTransaction().getSecurity().getCurrencyCode();

                @SuppressWarnings("unchecked")
                TransactionPair<Transaction> pair = new TransactionPair<Transaction>(
                                (TransactionOwner<Transaction>) entry.getOwner(entry.getAccountTransaction()),
                                entry.getAccountTransaction());
                issues.add(new TransactionMissingCurrencyIssue(client, pair, Objects.equals(accountCurrency,
                                securityCurrency)));
            }
            else if (t instanceof AccountTransferEntry)
            {
                // same story as with purchases: only offer to fix if currencies
                // match

                AccountTransferEntry entry = (AccountTransferEntry) t;
                String sourceCurrency = entry.getSourceAccount().getCurrencyCode();
                String targetCurrency = entry.getTargetAccount().getCurrencyCode();

                @SuppressWarnings("unchecked")
                TransactionPair<Transaction> pair = new TransactionPair<Transaction>(
                                (TransactionOwner<Transaction>) entry.getOwner(entry.getSourceTransaction()),
                                entry.getSourceTransaction());
                issues.add(new TransactionMissingCurrencyIssue(client, pair, Objects.equals(sourceCurrency,
                                targetCurrency)));
            }
            else if (t instanceof PortfolioTransferEntry)
            {
                // transferring a security involves no currency change because
                // the currency is defined the security itself

                PortfolioTransferEntry entry = (PortfolioTransferEntry) t;

                @SuppressWarnings("unchecked")
                TransactionPair<Transaction> pair = new TransactionPair<Transaction>(
                                (TransactionOwner<Transaction>) entry.getOwner(entry.getSourceTransaction()),
                                entry.getSourceTransaction());
                issues.add(new TransactionMissingCurrencyIssue(client, pair));
            }
            else
            {
                throw new IllegalArgumentException();
            }
        }

        return issues;
    }
}
