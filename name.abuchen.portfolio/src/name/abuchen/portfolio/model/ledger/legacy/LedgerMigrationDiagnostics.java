package name.abuchen.portfolio.model.ledger.legacy;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;

public final class LedgerMigrationDiagnostics
{
    private static final String SHADOW_PREFIX = "ledger-shadow:"; //$NON-NLS-1$
    private static final int EXAMPLE_LIMIT = 10;
    private static final Pattern REASON_PATTERN = Pattern.compile("\\breason=([A-Z0-9_]+)"); //$NON-NLS-1$

    private LedgerMigrationDiagnostics()
    {
    }

    public static void logMigrationAttempt(LegacyTransactionToLedgerMigrator.MigrationResult result)
    {
        var warning = result.getFailedCount() > 0 || result.getPreservedLegacyTransactionCount() > 0
                        || result.getMixedStateCount() > 0;
        var message = format("migration", result.getInspectedLegacyTransactionCount(), //$NON-NLS-1$
                        result.getMigratedTransactionCount(), result.getPreservedLegacyTransactionCount(),
                        result.getFailedCount(), result.getMixedStateCount(), reasonCategories(result.getDiagnostics()),
                        result.getDiagnostics(), warning);

        if (warning)
            PortfolioLog.warning(message);
        else
            PortfolioLog.info(message);
    }

    public static void logMixedState(Client client, String source)
    {
        var mixedStateCount = countLegacySourceRows(client);

        if (mixedStateCount == 0)
            return;

        PortfolioLog.warning(format(source, 0, 0, mixedStateCount, 0, mixedStateCount, List.of("MIXED_STATE"), //$NON-NLS-1$
                        List.of("reason=MIXED_STATE ledgerTruthExists=true"), true)); //$NON-NLS-1$
    }

    static int countLegacySourceRows(Client client)
    {
        var count = 0;

        for (var account : client.getAccounts())
            count += countLegacySourceRows(account.getTransactions());

        for (var portfolio : client.getPortfolios())
            count += countLegacySourceRows(portfolio.getTransactions());

        return count;
    }

    static int failureCount(List<String> diagnostics)
    {
        var count = 0;

        for (var diagnostic : diagnostics)
            if (!diagnostic.contains("reason=SKIPPED_ALREADY_MIGRATED")) //$NON-NLS-1$
                count++;

        return count;
    }

    private static int countLegacySourceRows(List<? extends Transaction> transactions)
    {
        var count = 0;

        for (var transaction : transactions)
            if (!(transaction instanceof LedgerBackedTransaction) && !isCompatibilityShadow(transaction))
                count++;

        return count;
    }

    private static boolean isCompatibilityShadow(Transaction transaction)
    {
        return transaction.getUUID() != null && transaction.getUUID().startsWith(SHADOW_PREFIX);
    }

    private static List<String> reasonCategories(List<String> diagnostics)
    {
        var reasons = new LinkedHashSet<String>();

        for (var diagnostic : diagnostics)
        {
            var matcher = REASON_PATTERN.matcher(diagnostic);
            while (matcher.find())
                reasons.add(matcher.group(1));
        }

        if (reasons.isEmpty())
            reasons.add("COMPLETE"); //$NON-NLS-1$

        return List.copyOf(reasons);
    }

    private static String format(String source, int inspected, int migrated, int preserved, int failed, int mixedState,
                    List<String> reasons, List<String> diagnostics, boolean warning)
    {
        var message = new StringBuilder("Ledger migration diagnostics") //$NON-NLS-1$
                        .append(" source=").append(source) //$NON-NLS-1$
                        .append(" inspected=").append(inspected) //$NON-NLS-1$
                        .append(" migrated=").append(migrated) //$NON-NLS-1$
                        .append(" preserved=").append(preserved) //$NON-NLS-1$
                        .append(" failed=").append(failed) //$NON-NLS-1$
                        .append(" mixedState=").append(mixedState) //$NON-NLS-1$
                        .append(" reasons=").append(reasons); //$NON-NLS-1$

        if (!diagnostics.isEmpty())
        {
            var examples = diagnostics.stream().limit(EXAMPLE_LIMIT).toList();
            message.append(" examples=").append(examples); //$NON-NLS-1$

            if (diagnostics.size() > EXAMPLE_LIMIT)
                message.append(" omittedExamples=").append(diagnostics.size() - EXAMPLE_LIMIT); //$NON-NLS-1$
        }

        if (warning)
        {
            message.append(" preserved legacy rows were not deleted."); //$NON-NLS-1$
            message.append(" No silent remigration loop will run after Ledger truth exists."); //$NON-NLS-1$
        }
        else
        {
            message.append(" preserved legacy count is zero."); //$NON-NLS-1$
        }

        return message.toString();
    }
}
