package name.abuchen.portfolio.model.ledger;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionService;

/**
 * Applies atomic changes to Ledger entries and their runtime projections.
 * This is internal Ledger mutation infrastructure. Contributors should normally use
 * higher-level creators, editors, converters, or deleters instead of calling it directly.
 */
public final class LedgerMutationContext
{
    private final Client client;
    private final Consumer<Client> materializer;

    public LedgerMutationContext(Client client)
    {
        this(client, LedgerProjectionService::materialize);
    }

    LedgerMutationContext(Client client, Consumer<Client> materializer)
    {
        this.client = Objects.requireNonNull(client);
        this.materializer = Objects.requireNonNull(materializer);
    }

    public void mutateEntry(LedgerEntry entry, Consumer<LedgerEntry> mutation)
    {
        Objects.requireNonNull(entry);
        Objects.requireNonNull(mutation);

        mutateEntries(List.of(entry.getUUID()), ledger -> mutation.accept(entryByUUID(ledger, entry.getUUID())));
    }

    public void mutateEntries(Collection<String> entryUUIDs, Consumer<Ledger> mutation)
    {
        mutateEntries(entryUUIDs, mutation, true);
    }

    private void mutateEntries(Collection<String> entryUUIDs, Consumer<Ledger> mutation, boolean refreshProjections)
    {
        Objects.requireNonNull(entryUUIDs);
        Objects.requireNonNull(mutation);

        var affectedEntryUUIDs = Set.copyOf(entryUUIDs);
        var staleProjectionUUIDs = projectionUUIDs(client.getLedger(), affectedEntryUUIDs);
        var candidate = LedgerModelCopy.copyLedger(client.getLedger());

        mutation.accept(candidate);
        validate(candidate);
        staleProjectionUUIDs.addAll(projectionUUIDs(candidate, affectedEntryUUIDs));

        synchronizeLiveLedger(candidate, affectedEntryUUIDs);

        if (refreshProjections)
        {
            removeMaterializedProjections(staleProjectionUUIDs);
            materializer.accept(client);
        }
    }

    public void refresh()
    {
        materializer.accept(client);
    }

    public LedgerEntry attachEntry(LedgerEntry entry)
    {
        Objects.requireNonNull(entry);

        var entryUUID = entry.getUUID();

        mutateEntries(List.of(entryUUID),
                        ledger -> LedgerGraphWriter.addEntry(ledger, LedgerModelCopy.copyEntry(entry)), false);

        return entryByUUID(client.getLedger(), entryUUID);
    }

    public void removeEntry(LedgerEntry entry)
    {
        Objects.requireNonNull(entry);

        var entryUUID = entry.getUUID();

        mutateEntries(List.of(entryUUID), ledger -> {
            var current = entryByUUID(ledger, entryUUID);

            LedgerGraphWriter.removeEntry(ledger, current);
        });
    }

    public void replaceSameShapeEntry(LedgerEntry currentEntry, LedgerEntry replacement)
    {
        Objects.requireNonNull(currentEntry);
        Objects.requireNonNull(replacement);

        var currentUUID = currentEntry.getUUID();
        var preparedReplacement = prepareSameShapeReplacement(currentEntry, replacement);

        mutateEntries(List.of(currentUUID), ledger -> replaceEntry(ledger, currentUUID,
                        LedgerModelCopy.copyEntry(preparedReplacement)));
    }

    public void splitEntry(LedgerEntry entry, List<LedgerEntry> replacementEntries)
    {
        Objects.requireNonNull(entry);
        Objects.requireNonNull(replacementEntries);

        if (replacementEntries.isEmpty())
            throw new IllegalArgumentException(
                            LedgerDiagnosticCode.LEDGER_CORE_007.message("Ledger entry split requires replacement entries")); //$NON-NLS-1$

        var replacementUUIDs = replacementEntries.stream().map(LedgerEntry::getUUID).collect(Collectors.toSet());

        if (replacementUUIDs.size() != replacementEntries.size())
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CORE_008
                            .message("Ledger entry split replacement UUIDs must be unique")); //$NON-NLS-1$

        if (!replacementUUIDs.contains(entry.getUUID()))
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CORE_009
                            .message("Ledger entry split must preserve the original entry UUID on one side")); //$NON-NLS-1$

        var affectedEntryUUIDs = new HashSet<>(replacementUUIDs);

        affectedEntryUUIDs.add(entry.getUUID());

        mutateEntries(affectedEntryUUIDs, ledger -> {
            var current = entryByUUID(ledger, entry.getUUID());

            LedgerGraphWriter.removeEntry(ledger, current);
            replacementEntries.stream().map(LedgerModelCopy::copyEntry)
                            .forEach(replacement -> LedgerGraphWriter.addEntry(ledger, replacement));
        });
    }

    Ledger getLedger()
    {
        return client.getLedger();
    }

    private LedgerEntry prepareSameShapeReplacement(LedgerEntry currentEntry, LedgerEntry replacement)
    {
        if (currentEntry.getType() != replacement.getType())
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CORE_010
                            .message("Entry replacement must keep the same LedgerEntryType")); //$NON-NLS-1$

        var currentByRole = projectionsByUniqueRole(currentEntry);
        var replacementByRole = projectionsByUniqueRole(replacement);

        if (!currentByRole.keySet().equals(replacementByRole.keySet()))
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_002
                            .message("Entry replacement projection roles do not match")); //$NON-NLS-1$

        var prepared = LedgerModelCopy.copyEntry(replacement);

        prepared.setUUID(currentEntry.getUUID());

        for (var projection : prepared.getProjectionRefs())
        {
            var currentProjection = currentByRole.get(projection.getRole());

            projection.setUUID(currentProjection.getUUID());
        }

        return prepared;
    }

    private java.util.Map<LedgerProjectionRole, LedgerProjectionRef> projectionsByUniqueRole(LedgerEntry entry)
    {
        var result = new java.util.EnumMap<LedgerProjectionRole, LedgerProjectionRef>(LedgerProjectionRole.class);

        for (var projection : entry.getProjectionRefs())
        {
            if (result.put(projection.getRole(), projection) != null)
                throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_003
                                .message("Projection role is ambiguous: " + projection.getRole())); //$NON-NLS-1$
        }

        return result;
    }

    private void replaceEntry(Ledger ledger, String currentUUID, LedgerEntry replacement)
    {
        var current = entryByUUID(ledger, currentUUID);

        LedgerGraphWriter.replaceEntry(ledger, current, replacement);
    }

    private void synchronizeLiveLedger(Ledger candidate, Set<String> affectedEntryUUIDs)
    {
        for (var entryUUID : affectedEntryUUIDs)
        {
            var liveEntry = findEntry(client.getLedger(), entryUUID);
            var candidateEntry = findEntry(candidate, entryUUID);

            if (candidateEntry.isPresent())
            {
                if (liveEntry.isPresent())
                    LedgerGraphWriter.replaceEntryContents(liveEntry.get(), candidateEntry.get());
                else
                    LedgerGraphWriter.addEntry(client.getLedger(), LedgerModelCopy.copyEntry(candidateEntry.get()));
            }
            else
            {
                liveEntry.ifPresent(entry -> LedgerGraphWriter.removeEntry(client.getLedger(), entry));
            }
        }
    }

    private void validate(Ledger ledger)
    {
        var result = LedgerStructuralValidator.validate(ledger);

        if (!result.isOK())
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CORE_011
                            .message("Invalid ledger structural mutation: " + result.format())); //$NON-NLS-1$
    }

    static LedgerEntry entryByUUID(Ledger ledger, String uuid)
    {
        return findEntry(ledger, uuid)
                        .orElseThrow(() -> new IllegalArgumentException("Ledger entry not found: " + uuid)); //$NON-NLS-1$
    }

    private static Optional<LedgerEntry> findEntry(Ledger ledger, String uuid)
    {
        return ledger.getEntries().stream() //
                        .filter(entry -> entry.getUUID().equals(uuid)) //
                        .findFirst();
    }

    private Set<String> projectionUUIDs(Ledger ledger, Set<String> affectedEntryUUIDs)
    {
        return ledger.getEntries().stream() //
                        .filter(entry -> affectedEntryUUIDs.contains(entry.getUUID())) //
                        .flatMap(entry -> entry.getProjectionRefs().stream()) //
                        .map(LedgerProjectionRef::getUUID) //
                        .collect(Collectors.toCollection(HashSet::new));
    }

    private void removeMaterializedProjections(Set<String> projectionUUIDs)
    {
        if (projectionUUIDs.isEmpty())
            return;

        for (var account : client.getAccounts())
            removeAccountProjections(account.getTransactions(), projectionUUIDs);

        for (var portfolio : client.getPortfolios())
            removePortfolioProjections(portfolio.getTransactions(), projectionUUIDs);
    }

    private void removeAccountProjections(List<AccountTransaction> transactions, Set<String> projectionUUIDs)
    {
        transactions.removeIf(transaction -> transaction instanceof LedgerBackedTransaction
                        && projectionUUIDs.contains(transaction.getUUID()));
    }

    private void removePortfolioProjections(List<PortfolioTransaction> transactions, Set<String> projectionUUIDs)
    {
        transactions.removeIf(transaction -> transaction instanceof LedgerBackedTransaction
                        && projectionUUIDs.contains(transaction.getUUID()));
    }
}
