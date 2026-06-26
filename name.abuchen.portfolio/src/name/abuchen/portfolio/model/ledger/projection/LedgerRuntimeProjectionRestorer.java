package name.abuchen.portfolio.model.ledger.projection;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Consumer;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.ledger.Ledger;
import name.abuchen.portfolio.model.ledger.LedgerDiagnosticMessageFormatter;
import name.abuchen.portfolio.model.ledger.LedgerStructuralValidator;

/**
 * Restores runtime account and portfolio transactions from persisted Ledger entries after load.
 * The restored legacy transactions are views. They must not be treated as a second persisted
 * source of transaction truth.
 */
final class LedgerRuntimeProjectionRestorer
{
    private static final String ENTRY_UUID_DETAIL = "entryUUID"; //$NON-NLS-1$

    @FunctionalInterface
    interface Logger
    {
        void log(Severity severity, String message);
    }

    enum Severity
    {
        INFO,
        WARNING
    }

    private static final class ProjectionKey
    {
        private final String ownerType;
        private final Object owner;
        private final String projectionUUID;

        private ProjectionKey(String ownerType, Object owner, String projectionUUID)
        {
            this.ownerType = ownerType;
            this.owner = owner;
            this.projectionUUID = projectionUUID;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(ownerType, System.identityHashCode(owner), projectionUUID);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;

            if (!(obj instanceof ProjectionKey other))
                return false;

            return owner == other.owner && Objects.equals(ownerType, other.ownerType)
                            && Objects.equals(projectionUUID, other.projectionUUID);
        }
    }

    private static final class RestorationDiagnostics
    {
        private final Set<Object> affectedOwners = new LinkedHashSet<>();
        private final Set<String> missingProjectionUUIDs = new LinkedHashSet<>();
        private final Set<String> duplicateProjectionUUIDs = new LinkedHashSet<>();
        private final Set<String> staleProjectionUUIDs = new LinkedHashSet<>();

        private boolean hasChanges()
        {
            return !affectedOwners.isEmpty();
        }

        private boolean hasUnexpectedOwnerListState()
        {
            return !duplicateProjectionUUIDs.isEmpty() || !staleProjectionUUIDs.isEmpty();
        }
    }

    private final LedgerProjectionMaterializer materializer;
    private final Logger logger;

    LedgerRuntimeProjectionRestorer()
    {
        this(new LedgerProjectionMaterializer(), LedgerRuntimeProjectionRestorer::logToPortfolioLog);
    }

    LedgerRuntimeProjectionRestorer(LedgerProjectionMaterializer materializer)
    {
        this(materializer, LedgerRuntimeProjectionRestorer::logToPortfolioLog);
    }

    LedgerRuntimeProjectionRestorer(LedgerProjectionMaterializer materializer, Logger logger)
    {
        this.materializer = Objects.requireNonNull(materializer);
        this.logger = Objects.requireNonNull(logger);
    }

    LedgerStructuralValidator.ValidationResult restoreIfValid(Client client)
    {
        Objects.requireNonNull(client);

        var result = LedgerStructuralValidator.validate(client.getLedger());

        if (!result.isOK())
        {
            var invalidEntryUUIDs = invalidEntryUUIDs(result);
            if (invalidEntryUUIDs == null)
            {
                logSkippedInvalidLedger(client.getLedger(), result);
                return result;
            }

            var expectedProjections = expectedProjectionCounts(client);
            var existingProjections = existingProjectionCounts(client);
            var diagnostics = restorationDiagnostics(expectedProjections, existingProjections);

            var removedProjections = removeLedgerBackedProjections(client);
            materializer.materialize(client, entry -> !invalidEntryUUIDs.contains(entry.getUUID()));
            var postRestoreDiagnostics = restorationDiagnostics(expectedProjections, existingProjectionCounts(client));

            logPartiallyRestored(client.getLedger(), result, removedProjections, countLedgerBackedProjections(client),
                            invalidEntryUUIDs.size(),
                            diagnostics.hasUnexpectedOwnerListState() ? diagnostics : postRestoreDiagnostics);
            return result;
        }

        var expectedProjections = expectedProjectionCounts(client);
        var existingProjections = existingProjectionCounts(client);
        var diagnostics = restorationDiagnostics(expectedProjections, existingProjections);

        var removedProjections = removeLedgerBackedProjections(client);
        materializer.materialize(client);
        var postRestoreDiagnostics = restorationDiagnostics(expectedProjections, existingProjectionCounts(client));

        if (diagnostics.hasUnexpectedOwnerListState() || postRestoreDiagnostics.hasChanges())
            logRestored(removedProjections, countLedgerBackedProjections(client), diagnostics);

        return result;
    }

    static void logSkipped(LedgerStructuralValidator.ValidationResult result)
    {
        logSkipped(result, LedgerRuntimeProjectionRestorer::warning);
    }

    static void logSkipped(LedgerStructuralValidator.ValidationResult result, Consumer<String> warningLogger)
    {
        warningLogger.accept(invalidLedgerMessage(result));
    }

    static void logSkipped(Ledger ledger, LedgerStructuralValidator.ValidationResult result)
    {
        logSkipped(ledger, result, LedgerRuntimeProjectionRestorer::warning);
    }

    static void logSkipped(Ledger ledger, LedgerStructuralValidator.ValidationResult result,
                    Consumer<String> warningLogger)
    {
        warningLogger.accept(invalidLedgerMessage(ledger, result));
    }

    static String invalidLedgerMessage(LedgerStructuralValidator.ValidationResult result)
    {
        return MessageFormat.format(Messages.LedgerRuntimeProjectionRestorerInvalidLedger, result.format());
    }

    static String invalidLedgerMessage(Ledger ledger, LedgerStructuralValidator.ValidationResult result)
    {
        return MessageFormat.format(Messages.LedgerRuntimeProjectionRestorerInvalidLedger,
                        LedgerDiagnosticMessageFormatter.formatValidationResult(ledger, result));
    }

    static String restoredLedgerMessage(int removedProjections, int materializedProjections,
                    int affectedOwnerCount, Set<String> missingProjectionUUIDs, Set<String> duplicateProjectionUUIDs,
                    Set<String> staleProjectionUUIDs)
    {
        return MessageFormat.format(Messages.LedgerRuntimeProjectionRestorerRestored, removedProjections,
                        materializedProjections, affectedOwnerCount, formatUUIDs(missingProjectionUUIDs),
                        formatUUIDs(duplicateProjectionUUIDs), formatUUIDs(staleProjectionUUIDs));
    }

    static String partiallyRestoredLedgerMessage(int removedProjections, int materializedProjections,
                    int skippedEntryCount, int affectedOwnerCount, Set<String> missingProjectionUUIDs,
                    Set<String> duplicateProjectionUUIDs, Set<String> staleProjectionUUIDs,
                    LedgerStructuralValidator.ValidationResult result)
    {
        return MessageFormat.format(Messages.LedgerRuntimeProjectionRestorerPartiallyRestored, removedProjections,
                        materializedProjections, skippedEntryCount, affectedOwnerCount,
                        formatUUIDs(missingProjectionUUIDs), formatUUIDs(duplicateProjectionUUIDs),
                        formatUUIDs(staleProjectionUUIDs), result.format());
    }

    static String partiallyRestoredLedgerMessage(int removedProjections, int materializedProjections,
                    int skippedEntryCount, int affectedOwnerCount, Set<String> missingProjectionUUIDs,
                    Set<String> duplicateProjectionUUIDs, Set<String> staleProjectionUUIDs, Ledger ledger,
                    LedgerStructuralValidator.ValidationResult result)
    {
        return MessageFormat.format(Messages.LedgerRuntimeProjectionRestorerPartiallyRestored, removedProjections,
                        materializedProjections, skippedEntryCount, affectedOwnerCount,
                        formatUUIDs(missingProjectionUUIDs), formatUUIDs(duplicateProjectionUUIDs),
                        formatUUIDs(staleProjectionUUIDs),
                        LedgerDiagnosticMessageFormatter.formatValidationResult(ledger, result));
    }

    private void logRestored(int removedProjections, int materializedProjections, RestorationDiagnostics diagnostics)
    {
        logger.log(Severity.INFO, restoredLedgerMessage(removedProjections, materializedProjections,
                        diagnostics.affectedOwners.size(), diagnostics.missingProjectionUUIDs,
                        diagnostics.duplicateProjectionUUIDs, diagnostics.staleProjectionUUIDs));
    }

    private void logPartiallyRestored(Ledger ledger, LedgerStructuralValidator.ValidationResult result,
                    int removedProjections, int materializedProjections, int skippedEntryCount,
                    RestorationDiagnostics diagnostics)
    {
        logger.log(Severity.WARNING,
                        partiallyRestoredLedgerMessage(removedProjections, materializedProjections, skippedEntryCount,
                                        diagnostics.affectedOwners.size(), diagnostics.missingProjectionUUIDs,
                                        diagnostics.duplicateProjectionUUIDs, diagnostics.staleProjectionUUIDs, ledger,
                                        result));
    }

    private void logSkippedInvalidLedger(Ledger ledger, LedgerStructuralValidator.ValidationResult result)
    {
        logger.log(Severity.WARNING, invalidLedgerMessage(ledger, result));
    }


    private static void logToPortfolioLog(Severity severity, String message)
    {
        switch (severity)
        {
            case INFO -> PortfolioLog.info(message);
            case WARNING -> PortfolioLog.warning(message);
            default -> throw new IllegalArgumentException(
                            LedgerDiagnosticCode.LEDGER_PROJ_077.message("Unsupported log severity: " + severity)); //$NON-NLS-1$
        }
    }

    private static void warning(String message)
    {
        PortfolioLog.warning(message);
    }

    private Set<String> invalidEntryUUIDs(LedgerStructuralValidator.ValidationResult result)
    {
        var invalidEntryUUIDs = new LinkedHashSet<String>();

        for (var issue : result.getIssues())
        {
            var entryUUID = issue.getDetails().get(ENTRY_UUID_DETAIL);
            if (entryUUID == null || entryUUID.isBlank() || "<missing>".equals(entryUUID)) //$NON-NLS-1$
                return null;

            invalidEntryUUIDs.add(entryUUID);
        }

        return invalidEntryUUIDs;
    }

    private int removeLedgerBackedProjections(Client client)
    {
        var removedProjections = 0;

        for (var account : client.getAccounts())
            removedProjections += removeAccountProjections(account.getTransactions());

        for (var portfolio : client.getPortfolios())
            removedProjections += removePortfolioProjections(portfolio.getTransactions());

        return removedProjections;
    }

    private int removeAccountProjections(List<AccountTransaction> transactions)
    {
        var size = transactions.size();
        transactions.removeIf(LedgerBackedTransaction.class::isInstance);
        return size - transactions.size();
    }

    private int removePortfolioProjections(List<PortfolioTransaction> transactions)
    {
        var size = transactions.size();
        transactions.removeIf(LedgerBackedTransaction.class::isInstance);
        return size - transactions.size();
    }

    private Map<ProjectionKey, Integer> expectedProjectionCounts(Client client)
    {
        Map<ProjectionKey, Integer> projections = new HashMap<>();

        for (var entry : client.getLedger().getEntries())
        {
            for (var projection : entry.getProjectionRefs())
            {
                if (projection.getAccount() != null)
                    addProjection(projections,
                                    new ProjectionKey("account", projection.getAccount(), projection.getUUID())); //$NON-NLS-1$
                else if (projection.getPortfolio() != null)
                    addProjection(projections,
                                    new ProjectionKey("portfolio", projection.getPortfolio(), projection.getUUID())); //$NON-NLS-1$
            }
        }

        return projections;
    }

    private Map<ProjectionKey, Integer> existingProjectionCounts(Client client)
    {
        Map<ProjectionKey, Integer> projections = new HashMap<>();

        for (var account : client.getAccounts())
        {
            for (var transaction : account.getTransactions())
            {
                if (transaction instanceof LedgerBackedTransaction ledgerBacked)
                    addProjection(projections, new ProjectionKey("account", account, //$NON-NLS-1$
                                    ledgerBacked.getLedgerProjectionRef().getUUID()));
            }
        }

        for (var portfolio : client.getPortfolios())
        {
            for (var transaction : portfolio.getTransactions())
            {
                if (transaction instanceof LedgerBackedTransaction ledgerBacked)
                    addProjection(projections, new ProjectionKey("portfolio", portfolio, //$NON-NLS-1$
                                    ledgerBacked.getLedgerProjectionRef().getUUID()));
            }
        }

        return projections;
    }

    private void addProjection(Map<ProjectionKey, Integer> projections, ProjectionKey key)
    {
        projections.merge(key, 1, Integer::sum);
    }

    private RestorationDiagnostics restorationDiagnostics(Map<ProjectionKey, Integer> expectedProjections,
                    Map<ProjectionKey, Integer> existingProjections)
    {
        var diagnostics = new RestorationDiagnostics();
        var keys = new LinkedHashSet<ProjectionKey>();

        keys.addAll(expectedProjections.keySet());
        keys.addAll(existingProjections.keySet());

        for (var key : keys)
        {
            var expectedCount = expectedProjections.getOrDefault(key, 0);
            var existingCount = existingProjections.getOrDefault(key, 0);

            if (expectedCount == existingCount)
                continue;

            diagnostics.affectedOwners.add(key.owner);

            if (expectedCount > existingCount)
                diagnostics.missingProjectionUUIDs.add(key.projectionUUID);
            else if (expectedCount == 0)
                diagnostics.staleProjectionUUIDs.add(key.projectionUUID);
            else
                diagnostics.duplicateProjectionUUIDs.add(key.projectionUUID);
        }

        return diagnostics;
    }

    private int countLedgerBackedProjections(Client client)
    {
        var projections = 0;

        for (var account : client.getAccounts())
            projections += countAccountProjections(account.getTransactions());

        for (var portfolio : client.getPortfolios())
            projections += countPortfolioProjections(portfolio.getTransactions());

        return projections;
    }

    private int countAccountProjections(List<AccountTransaction> transactions)
    {
        return (int) transactions.stream().filter(LedgerBackedTransaction.class::isInstance).count();
    }

    private int countPortfolioProjections(List<PortfolioTransaction> transactions)
    {
        return (int) transactions.stream().filter(LedgerBackedTransaction.class::isInstance).count();
    }

    private static String formatUUIDs(Set<String> uuids)
    {
        if (uuids.isEmpty())
            return "[]"; //$NON-NLS-1$

        var joiner = new StringJoiner(", ", "[", "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        uuids.stream().limit(10).forEach(joiner::add);

        if (uuids.size() > 10)
            joiner.add("... +" + (uuids.size() - 10)); //$NON-NLS-1$

        return joiner.toString();
    }
}
