package name.abuchen.portfolio.model.ledger;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * Adds best-effort user context to Ledger diagnostics while preserving the
 * original technical message and identifiers.
 */
@SuppressWarnings("nls")
public final class LedgerDiagnosticMessageFormatter
{
    private LedgerDiagnosticMessageFormatter()
    {
    }

    public static String formatValidationResult(Ledger ledger, LedgerStructuralValidator.ValidationResult result)
    {
        Objects.requireNonNull(result);

        if (result.isOK())
            return result.format();

        try
        {
            var builder = new StringBuilder();

            for (var issue : result.getIssues())
            {
                if (!builder.isEmpty())
                    builder.append("\n\n");

                builder.append(issue.format());
                builder.append(formatEntryContext(findEntry(ledger, issue).orElse(null)));
            }

            return builder.toString();
        }
        catch (RuntimeException e)
        {
            return result.format();
        }
    }

    public static String formatMigrationDiagnostic(Client client, String technicalMessage,
                    Transaction... transactions)
    {
        Objects.requireNonNull(technicalMessage);

        try
        {
            return technicalMessage + formatTransactionContext(client, transactions);
        }
        catch (RuntimeException e)
        {
            return technicalMessage;
        }
    }

    public static String formatEntryContext(LedgerEntry entry)
    {
        var context = new ContextLines();

        if (entry != null)
        {
            context.add(Messages.LedgerDiagnosticMessageFormatterDate, entry.getDateTime());
            context.add(Messages.LedgerDiagnosticMessageFormatterType, entry.getType());
            context.add(Messages.LedgerDiagnosticMessageFormatterAccount, accountNames(entry));
            context.add(Messages.LedgerDiagnosticMessageFormatterPortfolio, portfolioNames(entry));
            context.add(Messages.LedgerDiagnosticMessageFormatterSecurity, securityNames(entry));
            context.add(Messages.LedgerDiagnosticMessageFormatterAmount, amounts(entry));
            context.add(Messages.LedgerDiagnosticMessageFormatterShares, shares(entry));
            context.add(Messages.LedgerDiagnosticMessageFormatterSource, entry.getSource());
            context.add(Messages.LedgerDiagnosticMessageFormatterNote, entry.getNote());
        }

        return context.format();
    }

    private static String formatTransactionContext(Client client, Transaction... transactions)
    {
        var context = new ContextLines();

        if (transactions != null)
        {
            for (var transaction : transactions)
            {
                if (transaction == null)
                    continue;

                context.add(Messages.LedgerDiagnosticMessageFormatterDate, transaction.getDateTime());
                context.add(Messages.LedgerDiagnosticMessageFormatterType, transactionType(transaction));
                context.add(Messages.LedgerDiagnosticMessageFormatterAccount, accountName(client, transaction));
                context.add(Messages.LedgerDiagnosticMessageFormatterPortfolio, portfolioName(client, transaction));
                context.add(Messages.LedgerDiagnosticMessageFormatterSecurity, securitySummary(transaction.getSecurity()));

                if (transaction.getCurrencyCode() != null)
                    context.add(Messages.LedgerDiagnosticMessageFormatterAmount,
                                    Values.Money.format(Money.of(transaction.getCurrencyCode(),
                                                    transaction.getAmount())));

                if (transaction.getShares() != 0)
                    context.add(Messages.LedgerDiagnosticMessageFormatterShares,
                                    Values.Share.format(transaction.getShares()));

                context.add(Messages.LedgerDiagnosticMessageFormatterSource, transaction.getSource());
                context.add(Messages.LedgerDiagnosticMessageFormatterNote, transaction.getNote());
            }
        }

        return context.format();
    }

    private static Optional<LedgerEntry> findEntry(Ledger ledger, LedgerStructuralValidator.ValidationIssue issue)
    {
        if (ledger == null)
            return Optional.empty();

        var details = issue.getDetails();
        var entryUUID = details.get("entryUUID");

        if (entryUUID != null && !entryUUID.isBlank() && !"<missing>".equals(entryUUID))
        {
            var entry = ledger.getEntries().stream().filter(candidate -> entryUUID.equals(candidate.getUUID()))
                            .findFirst();

            if (entry.isPresent())
                return entry;
        }

        return ledger.getEntries().stream().filter(entry -> matchesEntry(entry, details.get("postingUUID")))
                        .findFirst();
    }

    private static boolean matchesEntry(LedgerEntry entry, String... uuids)
    {
        for (var uuid : uuids)
        {
            if (uuid == null || uuid.isBlank() || "<missing>".equals(uuid))
                continue;

            if (entry.getPostings().stream().anyMatch(posting -> uuid.equals(posting.getUUID())))
                return true;
        }

        return false;
    }

    private static Set<String> accountNames(LedgerEntry entry)
    {
        var values = new LinkedHashSet<String>();

        for (var posting : entry.getPostings())
            add(values, accountSummary(posting.getAccount()));

        return values;
    }

    private static Set<String> portfolioNames(LedgerEntry entry)
    {
        var values = new LinkedHashSet<String>();

        for (var posting : entry.getPostings())
            add(values, portfolioSummary(posting.getPortfolio()));

        return values;
    }

    private static Set<String> securityNames(LedgerEntry entry)
    {
        var values = new LinkedHashSet<String>();

        for (var posting : entry.getPostings())
            add(values, securitySummary(posting.getSecurity()));

        return values;
    }

    private static Set<String> amounts(LedgerEntry entry)
    {
        var values = new LinkedHashSet<String>();

        for (var posting : entry.getPostings())
        {
            if (posting.getCurrency() != null)
                add(values, Values.Money.format(Money.of(posting.getCurrency(), posting.getAmount())));
        }

        return values;
    }

    private static Set<String> shares(LedgerEntry entry)
    {
        var values = new LinkedHashSet<String>();

        for (var posting : entry.getPostings())
        {
            if (posting.getShares() != 0)
                add(values, Values.Share.format(posting.getShares()));
        }

        return values;
    }

    private static String transactionType(Transaction transaction)
    {
        if (transaction instanceof AccountTransaction accountTransaction)
            return String.valueOf(accountTransaction.getType());
        if (transaction instanceof PortfolioTransaction portfolioTransaction)
            return String.valueOf(portfolioTransaction.getType());

        return transaction.getClass().getSimpleName();
    }

    private static String accountName(Client client, Transaction transaction)
    {
        if (!(transaction instanceof AccountTransaction accountTransaction) || client == null)
            return null;

        return client.getAccounts().stream()
                        .filter(account -> account.getTransactions().stream()
                                        .anyMatch(candidate -> candidate == accountTransaction
                                                        || candidate.getUUID().equals(accountTransaction.getUUID())))
                        .findFirst().map(LedgerDiagnosticMessageFormatter::accountSummary).orElse(null);
    }

    private static String portfolioName(Client client, Transaction transaction)
    {
        if (!(transaction instanceof PortfolioTransaction portfolioTransaction) || client == null)
            return null;

        return client.getPortfolios().stream()
                        .filter(portfolio -> portfolio.getTransactions().stream()
                                        .anyMatch(candidate -> candidate == portfolioTransaction
                                                        || candidate.getUUID().equals(portfolioTransaction.getUUID())))
                        .findFirst().map(LedgerDiagnosticMessageFormatter::portfolioSummary).orElse(null);
    }

    private static String accountSummary(Account account)
    {
        return account == null ? null : nameOrFallback(account.getName(),
                        Messages.LedgerDiagnosticMessageFormatterUnnamedAccount);
    }

    private static String portfolioSummary(Portfolio portfolio)
    {
        return portfolio == null ? null : nameOrFallback(portfolio.getName(),
                        Messages.LedgerDiagnosticMessageFormatterUnnamedPortfolio);
    }

    private static String securitySummary(Security security)
    {
        if (security == null)
            return null;

        var builder = new StringBuilder(nameOrFallback(security.getName(),
                        Messages.LedgerDiagnosticMessageFormatterUnnamedSecurity));
        var identifiers = new LinkedHashSet<String>();

        add(identifiers, labeled(Messages.LedgerDiagnosticMessageFormatterIsin, security.getIsin()));
        add(identifiers, labeled(Messages.LedgerDiagnosticMessageFormatterWkn, security.getWkn()));
        add(identifiers, labeled(Messages.LedgerDiagnosticMessageFormatterTicker, security.getTickerSymbol()));

        if (!identifiers.isEmpty())
            builder.append(" (").append(String.join(", ", identifiers)).append(")");

        return builder.toString();
    }

    private static String labeled(String label, String value)
    {
        return isBlank(value) ? null : label + "=" + value;
    }

    private static String nameOrFallback(String name, String fallback)
    {
        return isBlank(name) ? "<" + fallback + ">" : name;
    }

    private static void add(Set<String> values, Object value)
    {
        if (value == null)
            return;

        var string = String.valueOf(value);

        if (!isBlank(string))
            values.add(string);
    }

    private static boolean isBlank(String value)
    {
        return value == null || value.isBlank();
    }

    private static final class ContextLines
    {
        private final StringBuilder builder = new StringBuilder();

        private void add(String label, Object value)
        {
            if (value instanceof Set<?> values)
            {
                if (!values.isEmpty())
                    append(label, String.join(", ", values.stream().map(String::valueOf).toList()));
                return;
            }

            if (value == null)
                return;

            var string = String.valueOf(value);

            if (!string.isBlank())
                append(label, string);
        }

        private void append(String label, String value)
        {
            if (builder.isEmpty())
                builder.append("\n\n").append(Messages.LedgerDiagnosticMessageFormatterTransactionContext)
                                .append(":");

            builder.append("\n  ").append(label).append(": ").append(value);
        }

        private String format()
        {
            if (builder.isEmpty())
                return "\n\n" + Messages.LedgerDiagnosticMessageFormatterTransactionContext + ":\n  " //$NON-NLS-1$ //$NON-NLS-2$
                                + Messages.LedgerDiagnosticMessageFormatterContextUnavailable;

            return builder.toString();
        }
    }
}
