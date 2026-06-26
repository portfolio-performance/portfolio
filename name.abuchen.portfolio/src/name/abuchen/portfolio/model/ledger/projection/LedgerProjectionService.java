package name.abuchen.portfolio.model.ledger.projection;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.ledger.Ledger;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
import name.abuchen.portfolio.model.ledger.LedgerStructuralValidator;
import name.abuchen.portfolio.model.ledger.ProjectionMembershipRole;

/**
 * Coordinates materialization and refresh of runtime projections for Ledger entries.
 * This is projection infrastructure. Contributor code should use it only when restoring or
 * refreshing views from Ledger truth.
 */
public final class LedgerProjectionService
{
    private LedgerProjectionService()
    {
    }

    public static void materialize(Client client)
    {
        new LedgerProjectionMaterializer().materialize(client);
    }

    public static Transaction createProjection(LedgerEntry entry, LedgerProjectionRef projectionRef)
    {
        return new LedgerProjectionFactory().createProjection(entry, projectionRef);
    }

    public static List<Transaction> createProjections(LedgerEntry entry)
    {
        return new LedgerProjectionFactory().createProjections(entry);
    }

    public static LedgerStructuralValidator.ValidationResult restoreIfValid(Client client)
    {
        return new LedgerRuntimeProjectionRestorer().restoreIfValid(client);
    }

    public static void adaptLegacyScalarMemberships(Client client)
    {
        for (var entry : client.getLedger().getEntries())
        {
            var postingUUIDs = entry.getPostings().stream().map(LedgerPosting::getUUID).collect(Collectors.toSet());

            for (var projectionRef : entry.getProjectionRefs())
                adaptLegacyScalarMemberships(projectionRef, postingUUIDs);
        }
    }

    private static void adaptLegacyScalarMemberships(LedgerProjectionRef projectionRef, Set<String> postingUUIDs)
    {
        if (projectionRef.getPrimaryMembership().isEmpty()
                        && postingUUIDs.contains(projectionRef.getPrimaryPostingUUID()))
            projectionRef.addMembership(projectionRef.getPrimaryPostingUUID(), ProjectionMembershipRole.PRIMARY);

        if (projectionRef.getMembershipsByRole(ProjectionMembershipRole.GROUP_ANCHOR).isEmpty()
                        && postingUUIDs.contains(projectionRef.getPostingGroupUUID()))
            projectionRef.addMembership(projectionRef.getPostingGroupUUID(), ProjectionMembershipRole.GROUP_ANCHOR);
    }

    public static void logSkipped(LedgerStructuralValidator.ValidationResult result)
    {
        LedgerRuntimeProjectionRestorer.logSkipped(result);
    }

    public static void logSkipped(Ledger ledger, LedgerStructuralValidator.ValidationResult result)
    {
        LedgerRuntimeProjectionRestorer.logSkipped(ledger, result);
    }
}
