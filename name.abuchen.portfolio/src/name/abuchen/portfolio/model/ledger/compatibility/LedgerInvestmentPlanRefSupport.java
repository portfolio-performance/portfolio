package name.abuchen.portfolio.model.ledger.compatibility;

import java.util.List;
import java.util.Objects;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;

/**
 * Keeps generated InvestmentPlan metadata compatible with Ledger conversions.
 * Plan executions are now linked by plan key and entry execution metadata, so role-only
 * conversions no longer need to rewrite projection-scoped plan references.
 */
final class LedgerInvestmentPlanRefSupport
{
    private LedgerInvestmentPlanRefSupport()
    {
    }

    static RoleChange roleChange(String runtimeProjectionId, LedgerProjectionRole sourceRole,
                    LedgerProjectionRole targetRole)
    {
        return new RoleChange(Objects.requireNonNull(runtimeProjectionId), Objects.requireNonNull(sourceRole),
                        Objects.requireNonNull(targetRole));
    }

    static void requireCurrentRefsResolveUniquely(Client client, LedgerEntry entry)
    {
        // Plan linkage is entry metadata; there are no projection-scoped plan refs to validate.
    }

    static SplitExecutionRefUpdates prepareAccountTransferSplitExecutionRefUpdates(Client client, LedgerEntry entry,
                    LedgerProjectionRole sourceRole, LedgerProjectionRole targetRole,
                    LedgerEntry removalEntry, LedgerEntry depositEntry)
    {
        return new SplitExecutionRefUpdates(List.of());
    }

    static void requireRefsFollowRoleChanges(Client client, LedgerEntry entry, RoleChange... changes)
    {
        // Role changes do not affect plan key / execution-date linkage.
    }

    static void updateProjectionRoles(Client client, LedgerEntry entry, RoleChange... changes)
    {
        // Projection role rewrites are no longer part of InvestmentPlan linkage.
    }

    record RoleChange(String runtimeProjectionId, LedgerProjectionRole sourceRole, LedgerProjectionRole targetRole)
    {
    }

    record SplitExecutionRefUpdates(List<ExecutionRefUpdate> updates)
    {
        void apply()
        {
            // No projection-scoped plan refs remain to update.
        }
    }

    private record ExecutionRefUpdate()
    {
    }
}
