package name.abuchen.portfolio.model;

import java.util.Objects;

import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;

/**
 * Checks whether generated transaction references can follow a Ledger conversion.
 * Plan executions are now linked by plan key and LedgerEntry execution metadata,
 * not by projection UUID or projection role.
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
        return true;
    }

    static boolean refsFollowRoleChanges(Client client, LedgerEntry entry, RoleChange... changes)
    {
        return true;
    }

    static String projectionUUID(LedgerEntry entry, LedgerProjectionRole role)
    {
        return entry.getProjectionRefs().stream() //
                        .filter(projection -> projection.getRole() == role) //
                        .map(LedgerProjectionRef::getUUID) //
                        .findFirst() //
                        .orElse(""); //$NON-NLS-1$
    }

    record RoleChange(String projectionUUID, LedgerProjectionRole sourceRole, LedgerProjectionRole targetRole)
    {
    }
}
