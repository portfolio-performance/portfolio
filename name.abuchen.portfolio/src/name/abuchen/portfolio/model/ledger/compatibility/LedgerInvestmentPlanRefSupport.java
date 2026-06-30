package name.abuchen.portfolio.model.ledger.compatibility;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;

/**
 * Keeps generated transaction references valid during Ledger conversions.
 * This is internal support for investment-plan references. It prevents conversions from
 * guessing a target when the old reference is ambiguous.
 */
final class LedgerInvestmentPlanRefSupport
{
    private LedgerInvestmentPlanRefSupport()
    {
    }

    static RoleChange roleChange(String projectionUUID, LedgerProjectionRole sourceRole,
                    LedgerProjectionRole targetRole)
    {
        return new RoleChange(Objects.requireNonNull(projectionUUID), Objects.requireNonNull(sourceRole),
                        Objects.requireNonNull(targetRole));
    }

    static void requireCurrentRefsResolveUniquely(Client client, LedgerEntry entry)
    {
        forEachRef(client, entry, ref -> {
            var matches = matchingProjections(entry, ref);
            if (matches != 1)
                throw new UnsupportedOperationException(referenceResolutionFailure(matches));
        });
    }

    static SplitExecutionRefUpdates prepareAccountTransferSplitExecutionRefUpdates(Client client, LedgerEntry entry,
                    LedgerProjectionRef sourceProjection, LedgerProjectionRef targetProjection,
                    LedgerEntry removalEntry, LedgerEntry depositEntry)
    {
        var removalProjection = uniqueAccountProjection(removalEntry);
        var depositProjection = uniqueAccountProjection(depositEntry);
        var updates = new ArrayList<ExecutionRefUpdate>();

        for (var plan : client.getPlans())
        {
            var refs = plan.getLedgerExecutionRefs();

            for (int ii = 0; ii < refs.size(); ii++)
            {
                var ref = refs.get(ii);

                if (!entry.getUUID().equals(ref.getLedgerEntryUUID()))
                    continue;

                if (splitSide(ref, sourceProjection, targetProjection) == SplitSide.SOURCE)
                    updates.add(new ExecutionRefUpdate(plan, ii, new InvestmentPlan.LedgerExecutionRef(
                                    removalEntry.getUUID(), removalProjection.getUUID(), removalProjection.getRole())));
                else
                    updates.add(new ExecutionRefUpdate(plan, ii, new InvestmentPlan.LedgerExecutionRef(
                                    depositEntry.getUUID(), depositProjection.getUUID(), depositProjection.getRole())));
            }
        }

        return new SplitExecutionRefUpdates(List.copyOf(updates));
    }

    static void requireRefsFollowRoleChanges(Client client, LedgerEntry entry, RoleChange... changes)
    {
        requireCurrentRefsResolveUniquely(client, entry);

        forEachRef(client, entry, ref -> {
            if (!canFollowAny(ref, changes))
                throw new UnsupportedOperationException(cannotFollowFailure(ref));
        });
    }

    static void updateProjectionRoles(Client client, LedgerEntry entry, RoleChange... changes)
    {
        for (var plan : client.getPlans())
        {
            var refs = plan.getLedgerExecutionRefs();

            for (int ii = 0; ii < refs.size(); ii++)
            {
                var ref = refs.get(ii);

                if (!entry.getUUID().equals(ref.getLedgerEntryUUID()))
                    continue;

                for (var change : changes)
                {
                    if (canFollow(ref, change) && ref.getProjectionRole() != null)
                    {
                        refs.set(ii, new InvestmentPlan.LedgerExecutionRef(ref.getLedgerEntryUUID(),
                                        ref.getProjectionUUID(), change.targetRole()));
                        break;
                    }
                }
            }
        }
    }

    private static void forEachRef(Client client, LedgerEntry entry, RefConsumer consumer)
    {
        for (var plan : client.getPlans())
            for (var ref : plan.getLedgerExecutionRefs())
                if (entry.getUUID().equals(ref.getLedgerEntryUUID()))
                    consumer.accept(ref);
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

    private static SplitSide splitSide(InvestmentPlan.LedgerExecutionRef ref, LedgerProjectionRef sourceProjection,
                    LedgerProjectionRef targetProjection)
    {
        var matchesSource = matches(ref, sourceProjection);
        var matchesTarget = matches(ref, targetProjection);

        if (matchesSource == matchesTarget)
            throw new UnsupportedOperationException(
                            LedgerDiagnosticCode.LEDGER_CONVERT_045.message("Ledger plan reference cannot be mapped to a split transfer side")); //$NON-NLS-1$

        return matchesSource ? SplitSide.SOURCE : SplitSide.TARGET;
    }

    private static LedgerProjectionRef uniqueAccountProjection(LedgerEntry entry)
    {
        var projections = entry.getProjectionRefs().stream()
                        .filter(projection -> projection.getRole() == LedgerProjectionRole.ACCOUNT).toList();

        if (projections.size() != 1)
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_PROJ_041
                            .message("Ledger plan reference cannot be mapped to a split transfer side")); //$NON-NLS-1$

        return projections.get(0);
    }

    private static String referenceResolutionFailure(long matches)
    {
        if (matches == 0)
            return "Ledger plan reference cannot resolve selected projection"; //$NON-NLS-1$

        return "Ledger plan reference would become ambiguous"; //$NON-NLS-1$
    }

    private static String cannotFollowFailure(InvestmentPlan.LedgerExecutionRef ref)
    {
        if (ref.getProjectionUUID() != null)
            return "Ledger plan reference projection would be removed by conversion"; //$NON-NLS-1$

        return "Ledger plan reference would become ambiguous after conversion"; //$NON-NLS-1$
    }

    record RoleChange(String projectionUUID, LedgerProjectionRole sourceRole, LedgerProjectionRole targetRole)
    {
    }

    record SplitExecutionRefUpdates(List<ExecutionRefUpdate> updates)
    {
        void apply()
        {
            for (var update : updates)
                update.plan().getLedgerExecutionRefs().set(update.index(), update.ref());
        }
    }

    private record ExecutionRefUpdate(InvestmentPlan plan, int index, InvestmentPlan.LedgerExecutionRef ref)
    {
    }

    private enum SplitSide
    {
        SOURCE, TARGET
    }

    @FunctionalInterface
    private interface RefConsumer
    {
        void accept(InvestmentPlan.LedgerExecutionRef ref);
    }
}
