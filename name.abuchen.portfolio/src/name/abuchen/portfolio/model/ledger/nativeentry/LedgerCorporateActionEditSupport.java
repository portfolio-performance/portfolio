package name.abuchen.portfolio.model.ledger.nativeentry;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerMutationContext;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerLegRole;
import name.abuchen.portfolio.model.ledger.configuration.LedgerNativeEntryDefinitionValidator;

/**
 * Semantic edit helpers for native Corporate Action Ledger entries.
 */
public final class LedgerCorporateActionEditSupport
{
    private LedgerCorporateActionEditSupport()
    {
    }

    public static Optional<LedgerPosting> findPostingBySemanticKey(LedgerEntry entry, LedgerLegRole role,
                    String localKey)
    {
        return findPostingBySemanticKey(entry, role, localKey, null);
    }

    public static Optional<LedgerPosting> findPostingBySemanticKey(LedgerEntry entry, LedgerLegRole role,
                    String localKey, String groupKey)
    {
        var matches = matchingPostings(entry, role, localKey, groupKey);

        if (matches.size() > 1)
            throw new IllegalArgumentException("Corporate Action semantic posting key is ambiguous: " + role //$NON-NLS-1$
                            + "/" + localKey); //$NON-NLS-1$

        return matches.stream().findFirst();
    }

    public static LedgerPosting postingBySemanticKey(LedgerEntry entry, LedgerLegRole role, String localKey,
                    String groupKey)
    {
        return findPostingBySemanticKey(entry, role, localKey, groupKey)
                        .orElseThrow(() -> new IllegalArgumentException(
                                        "Corporate Action semantic posting was not found: " + role + "/" //$NON-NLS-1$ //$NON-NLS-2$
                                                        + localKey));
    }

    public static void mutatePosting(Client client, LedgerEntry entry, LedgerLegRole role, String localKey,
                    String groupKey, Consumer<LedgerPosting> mutation)
    {
        Objects.requireNonNull(client);
        Objects.requireNonNull(entry);
        Objects.requireNonNull(mutation);

        new LedgerMutationContext(client).mutateEntry(entry, liveEntry -> {
            mutation.accept(postingBySemanticKey(liveEntry, role, localKey, groupKey));

            var result = LedgerNativeEntryDefinitionValidator.validate(liveEntry);
            if (!result.isOK())
                throw LedgerNativeEntryAssembler.issue(
                                LedgerNativeEntryAssemblyIssue.NATIVE_DEFINITION_VALIDATION_FAILED,
                                result.format());
        });
    }

    public static void mutatePosting(Client client, LedgerCorporateActionLegHandle handle,
                    Consumer<LedgerPosting> mutation)
    {
        Objects.requireNonNull(client);
        Objects.requireNonNull(handle);

        var liveEntry = client.getLedger().getEntries().stream()
                        .filter(entry -> Objects.equals(entry.getUUID(), handle.entry().getUUID())).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                        "Corporate Action handle does not belong to this client")); //$NON-NLS-1$

        var key = handle.toSemanticKey();
        mutatePosting(client, liveEntry, key.role(), key.localKey(), key.groupKey(), mutation);
    }

    private static List<LedgerPosting> matchingPostings(LedgerEntry entry, LedgerLegRole role, String localKey,
                    String groupKey)
    {
        Objects.requireNonNull(entry);
        Objects.requireNonNull(role);
        Objects.requireNonNull(localKey);

        if (entry.getType() != LedgerEntryType.CORPORATE_ACTION)
            return List.of();

        return entry.getPostings().stream() //
                        .filter(posting -> LedgerCorporateActionLegRoles.matches(posting, role)) //
                        .filter(posting -> localKey.equals(posting.getLocalKey())) //
                        .filter(posting -> groupKey == null || groupKey.equals(posting.getGroupKey())) //
                        .toList();
    }
}
