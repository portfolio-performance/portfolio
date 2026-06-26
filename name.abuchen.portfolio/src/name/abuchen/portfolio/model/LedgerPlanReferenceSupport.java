package name.abuchen.portfolio.model;

import java.util.Objects;

import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;

/**
 * Checks whether generated transaction references can follow a Ledger conversion.
 * This is internal model support for investment-plan references. It treats plan references
 * as links to generated transactions, not as ownership of the transaction.
 */
final class LedgerPlanReferenceSupport
{
    private LedgerPlanReferenceSupport()
    {
    }

    static RoleChange roleChange(String projectionUUID, LedgerProjectionRole sourceRole,
                    LedgerProjectionRole targetRole)
    {
        return new RoleChange(Objects.requireNonNull(projectionUUID), Objects.requireNonNull(sourceRole),
                        Objects.requireNonNull(targetRole));
    }

    static boolean currentRefsResolveUniquely(Client client, LedgerEntry entry)
    {
        return refsForEntry(client, entry).allMatch(ref -> matchingProjections(entry, ref) == 1);
    }

    static boolean refsFollowRoleChanges(Client client, LedgerEntry entry, RoleChange... changes)
    {
        return currentRefsResolveUniquely(client, entry)
                        && refsForEntry(client, entry).allMatch(ref -> canFollowAny(ref, changes));
    }

    static String projectionUUID(LedgerEntry entry, LedgerProjectionRole role)
    {
        return entry.getProjectionRefs().stream() //
                        .filter(projection -> projection.getRole() == role) //
                        .map(LedgerProjectionRef::getUUID) //
                        .findFirst() //
                        .orElse(""); //$NON-NLS-1$
    }

    private static java.util.stream.Stream<InvestmentPlan.LedgerExecutionRef> refsForEntry(Client client,
                    LedgerEntry entry)
    {
        return client.getPlans().stream() //
                        .flatMap(plan -> plan.getLedgerExecutionRefs().stream()) //
                        .filter(ref -> entry.getUUID().equals(ref.getLedgerEntryUUID()));
    }

    private static long matchingProjections(LedgerEntry entry, InvestmentPlan.LedgerExecutionRef ref)
    {
        return entry.getProjectionRefs().stream().filter(projection -> matches(ref, projection)).count();
    }

    private static boolean matches(InvestmentPlan.LedgerExecutionRef ref, LedgerProjectionRef projection)
    {
        return (ref.getProjectionUUID() == null || ref.getProjectionUUID().equals(projection.getUUID()))
                        && (ref.getProjectionRole() == null || ref.getProjectionRole() == projection.getRole());
    }

    private static boolean canFollowAny(InvestmentPlan.LedgerExecutionRef ref, RoleChange... changes)
    {
        for (var change : changes)
            if (canFollow(ref, change))
                return true;

        return false;
    }

    private static boolean canFollow(InvestmentPlan.LedgerExecutionRef ref, RoleChange change)
    {
        if (ref.getProjectionUUID() != null && !ref.getProjectionUUID().equals(change.projectionUUID()))
            return false;

        if (ref.getProjectionUUID() == null && ref.getProjectionRole() == null)
            return false;

        return ref.getProjectionRole() == null || ref.getProjectionRole() == change.sourceRole();
    }

    record RoleChange(String projectionUUID, LedgerProjectionRole sourceRole, LedgerProjectionRole targetRole)
    {
    }
}
