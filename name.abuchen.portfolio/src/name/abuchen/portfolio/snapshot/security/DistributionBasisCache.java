package name.abuchen.portfolio.snapshot.security;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import name.abuchen.portfolio.model.CorporateActionEntry;
import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.model.Security;

/**
 * Snapshot-scoped memo of the exact cost basis a {@code DISTRIBUTION_OUTBOUND}
 * leg removed from its parent, keyed by the owning {@link CorporateActionEntry}
 * and {@link CostMethod}. The parent's {@code CostCalculation} publishes the
 * value while processing the outbound leg (spec §7 Option A); the spinco's
 * calculations read it back as the derived inbound basis. On a cache miss the
 * value is recomputed by {@link DistributionBasis#derivedInboundBasis} via a
 * source before/after sub-pass, so correctness never depends on record ordering.
 */
/* package */ final class DistributionBasisCache
{
    private record Key(CorporateActionEntry entry, CostMethod method)
    {
    }

    private final Map<Key, Long> removed = new HashMap<>();

    public void put(CorporateActionEntry entry, CostMethod method, long removedBasis)
    {
        removed.put(new Key(entry, method), removedBasis);
    }

    public Long get(CorporateActionEntry entry, CostMethod method)
    {
        return removed.get(new Key(entry, method));
    }

    private Consumer<Security> parentCostTrigger;
    private final Set<Security> resolving = new HashSet<>();

    /**
     * Wires the record graph so a cache miss can force the parent (source)
     * security's cost record to compute, which publishes the exact removed
     * basis into this cache (spec §7 Option A). Set once per snapshot by
     * {@link SecurityPerformanceSnapshotBuilder}.
     */
    public void setParentCostTrigger(Consumer<Security> parentCostTrigger)
    {
        this.parentCostTrigger = parentCostTrigger;
    }

    /**
     * Forces the parent's real {@link CostCalculation} pass to run (via the
     * wired trigger), which publishes the exact amount its
     * {@code DISTRIBUTION_OUTBOUND} leg removed, then returns that now-cached
     * value. Returns {@code null} when it cannot be resolved -- no trigger is
     * wired (e.g. a stand-alone calculation), the parent record produced no
     * value, or the event is re-entrant (a chained spin-off, out of scope):
     * the caller then falls back to the local sub-pass. The per-security guard
     * prevents unbounded recursion on chained events.
     */
    public Long resolveViaParent(CorporateActionEntry entry, CostMethod method, Security sourceSecurity)
    {
        if (parentCostTrigger == null || !resolving.add(sourceSecurity))
            return get(entry, method);

        try
        {
            parentCostTrigger.accept(sourceSecurity);
        }
        finally
        {
            resolving.remove(sourceSecurity);
        }

        return get(entry, method);
    }
}
