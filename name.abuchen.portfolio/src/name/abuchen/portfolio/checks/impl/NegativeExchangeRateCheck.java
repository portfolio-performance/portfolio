package name.abuchen.portfolio.checks.impl;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.checks.Check;
import name.abuchen.portfolio.checks.Issue;
import name.abuchen.portfolio.checks.QuickFix;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.money.Values;

/**
 * Checks if there are transaction units with negative exchange rates.
 */
public class NegativeExchangeRateCheck implements Check
{
    private static class NegativeExchangeRateIssue implements Issue
    {
        private Client client;
        private TransactionPair<?> pair;
        private String label;

        public NegativeExchangeRateIssue(Client client, TransactionPair<?> pair, String label)
        {
            this.client = client;
            this.pair = pair;
            this.label = label;
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
            return label;
        }

        @SuppressWarnings("unchecked")
        @Override
        public List<QuickFix> getAvailableFixes()
        {
            List<QuickFix> fixes = new ArrayList<>();

            fixes.add(new DeleteTransactionFix<Transaction>(client, (TransactionPair<Transaction>) pair));

            return fixes;
        }
    }

    @Override
    public List<Issue> execute(Client client)
    {
        List<Issue> issues = new ArrayList<>();
        Set<String> reportedLedgerEntries = new HashSet<>();

        for (Account account : client.getAccounts())
        {
            for (AccountTransaction t : account.getTransactions()) // NOSONAR
            {
                if (t instanceof LedgerBackedTransaction ledgerBacked)
                {
                    if (reportedLedgerEntries.add(ledgerBacked.getLedgerEntry().getUUID()))
                        addLedgerBackedIssue(client, issues, new TransactionPair<>(account, t),
                                        ledgerBacked.getLedgerEntry());
                    continue;
                }

                if (t.getCrossEntry() instanceof BuySellEntry)
                    continue;

                if (t.getType() == AccountTransaction.Type.TRANSFER_IN)
                    continue;

                for (Transaction.Unit unit : t.getUnits().collect(Collectors.toList()))
                {
                    if (unit.getExchangeRate() != null && unit.getExchangeRate().signum() < 0)
                    {
                        issues.add(new NegativeExchangeRateIssue(client, new TransactionPair<>(account, t),
                                        MessageFormat.format(Messages.IssueExchangeRateIsNegative,
                                                        Values.ExchangeRate.format(unit.getExchangeRate()),
                                                        t.getType())));
                        break;
                    }
                }
            }
        }

        for (Portfolio portfolio : client.getPortfolios())
        {
            for (PortfolioTransaction t : portfolio.getTransactions()) // NOSONAR
            {
                if (t instanceof LedgerBackedTransaction ledgerBacked)
                {
                    if (reportedLedgerEntries.add(ledgerBacked.getLedgerEntry().getUUID()))
                        addLedgerBackedIssue(client, issues, new TransactionPair<>(portfolio, t),
                                        ledgerBacked.getLedgerEntry());
                    continue;
                }

                if (t.getType() == PortfolioTransaction.Type.TRANSFER_IN)
                    continue;

                for (Transaction.Unit unit : t.getUnits().collect(Collectors.toList()))
                {
                    if (unit.getExchangeRate() != null && unit.getExchangeRate().signum() < 0)
                    {
                        issues.add(new NegativeExchangeRateIssue(client, new TransactionPair<>(portfolio, t),
                                        MessageFormat.format(Messages.IssueExchangeRateIsNegative,
                                                        Values.ExchangeRate.format(unit.getExchangeRate()),
                                                        t.getType())));
                        break;
                    }
                }
            }
        }

        return issues;
    }

    private void addLedgerBackedIssue(Client client, List<Issue> issues, TransactionPair<?> pair, LedgerEntry entry)
    {
        firstNegativeExchangeRatePosting(entry).ifPresent(posting -> issues.add(
                        new NegativeExchangeRateIssue(client, pair,
                                        MessageFormat.format(Messages.IssueExchangeRateIsNegative,
                                                        Values.ExchangeRate.format(posting.getExchangeRate()),
                                                        transactionType(pair.getTransaction())))));
    }

    private Optional<LedgerPosting> firstNegativeExchangeRatePosting(LedgerEntry entry)
    {
        return entry.getPostings().stream() //
                        .filter(posting -> posting.getExchangeRate() != null)
                        .filter(posting -> posting.getExchangeRate().signum() < 0) //
                        .findFirst();
    }

    private Object transactionType(Transaction transaction)
    {
        if (transaction instanceof AccountTransaction accountTransaction)
            return accountTransaction.getType();

        return ((PortfolioTransaction) transaction).getType();
    }
}
