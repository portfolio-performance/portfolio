package name.abuchen.portfolio.snapshot.security;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.CorporateActionEntry;
import name.abuchen.portfolio.model.CorporateActionEntry.LegRole;
import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.TaxesAndFees;
import name.abuchen.portfolio.money.CurrencyConverter;

/* package */ final class DistributionBasis
{
    private DistributionBasis()
    {
    }

    /**
     * Returns the {@link CorporateActionEntry} linking this distribution leg
     * to its counterpart, if any. On a client produced by a filter that
     * copies transactions into synthetic legs (e.g.
     * {@code ClientClassificationFilter}, {@code WithoutTaxesFilter}), the
     * original cross-entry cannot be reconstructed -- it references the
     * un-filtered legs -- so it is absent here. Callers must treat an absent
     * entry as a no-op rather than fail; correct derived basis on such
     * filtered views is deferred to a later phase. A missing entry is
     * expected for such synthetic filtered/copied clients, but on a normal
     * (un-filtered) client it indicates malformed data -- e.g. a
     * hand-crafted or imported distribution transaction without its
     * counterpart -- and is logged as a warning.
     */
    private static Optional<CorporateActionEntry> entryOf(PortfolioTransaction leg)
    {
        if (leg.getCrossEntry() instanceof CorporateActionEntry entry)
            return Optional.of(entry);

        PortfolioLog.warning(MessageFormat.format(
                        "Distribution leg for security ''{0}'' on {1} has no CorporateActionEntry; "
                                        + "its basis reallocation is treated as zero. This is expected on "
                                        + "filtered/copied clients, but may indicate malformed or imported data "
                                        + "otherwise.",
                        leg.getSecurity() != null ? leg.getSecurity().getName() : "?", leg.getDateTime()));

        return Optional.empty();
    }

    /**
     * Returns the basis reduction ratio for a {@code DISTRIBUTION_OUTBOUND}
     * leg, or {@link BigDecimal#ZERO} (i.e. no reduction) if the leg is not
     * linked to a {@link CorporateActionEntry}.
     */
    public static BigDecimal basisRatio(PortfolioTransaction leg)
    {
        return entryOf(leg).map(CorporateActionEntry::getBasisRatio).orElse(BigDecimal.ZERO);
    }

    /**
     * Derives the incoming basis of a {@code DISTRIBUTION_INBOUND} leg as the
     * exact amount its matching {@code DISTRIBUTION_OUTBOUND} leg removed from
     * the parent, so {@code parent basis removed == spinco basis} holds to the
     * cent regardless of lot count (spec §4.3). The value is read from the
     * {@code cache} when the parent's {@link CostCalculation} has already
     * published it (spec §7 Option A); otherwise it is recomputed here as
     * {@code basisBefore - basisAfter}, where {@code basisAfter} re-applies only
     * this event's SOURCE leg to the parent's non-distribution history at the
     * ex-date, and cached for the other consumers.
     * <p>
     * Single-currency and single-event: the source history excludes all
     * {@code DISTRIBUTION_*} legs (no chained spin-offs, spec §7.1). If the leg
     * is not linked to a {@link CorporateActionEntry}, {@code 0} is returned and
     * the shares are still added by the calling calculation.
     */
    static long derivedInboundBasis(CurrencyConverter converter, Portfolio portfolio, PortfolioTransaction inboundLeg,
                    CostMethod costMethod, String termCurrency, DistributionBasisCache cache)
    {
        var entry = entryOf(inboundLeg);
        if (entry.isEmpty())
            return 0L;

        Long cached = cache.get(entry.get(), costMethod);
        if (cached != null)
            return cached;

        var sourceLegTx = entry.get().getLeg(LegRole.SOURCE).orElse(null);
        if (!(sourceLegTx instanceof PortfolioTransaction sourceLeg))
            return 0L;

        // true Option A (spec §7): force the parent's real cost pass to run and
        // publish the exact removed basis, so the spinco reads the same
        // interval-aware, own-currency, order-independent value the parent lost.
        // This resolves the pre-interval multi-lot divergence (the parent
        // aggregates its opening position into one ValuationAtStart lot) and
        // keeps the ECB reference rate out of the basis (spec §6). The sub-pass
        // below runs only when the parent record is unreachable -- e.g. on a
        // filtered/copied client whose cross-entry links the un-filtered legs.
        Long viaParent = cache.resolveViaParent(entry.get(), costMethod, sourceLeg.getSecurity());
        if (viaParent != null)
            return viaParent;

        var exDate = inboundLeg.getDateTime();

        var before = new ArrayList<>(portfolio.getTransactions().stream() //
                        .filter(pt -> pt.getType() != PortfolioTransaction.Type.DISTRIBUTION_INBOUND
                                        && pt.getType() != PortfolioTransaction.Type.DISTRIBUTION_OUTBOUND) //
                        .filter(pt -> !pt.getDateTime().isAfter(exDate)) //
                        .filter(pt -> sourceLeg.getSecurity().equals(pt.getSecurity())) //
                        .toList());

        long basisBefore = costOf(converter, portfolio, before, costMethod, termCurrency);

        var after = new ArrayList<>(before);
        after.add(sourceLeg);
        long basisAfter = costOf(converter, portfolio, after, costMethod, termCurrency);

        long removed = basisBefore - basisAfter;
        cache.put(entry.get(), costMethod, removed);
        return removed;
    }

    private static long costOf(CurrencyConverter converter, Portfolio portfolio, List<PortfolioTransaction> txns,
                    CostMethod costMethod, String termCurrency)
    {
        var cost = new CostCalculation();
        cost.setTermCurrency(termCurrency);
        var items = new ArrayList<>(txns.stream().map(pt -> CalculationLineItem.of(portfolio, pt)).toList());
        items.sort(new CalculationLineItemComparator());
        cost.visitAll(converter, items);
        return cost.getCost(costMethod, TaxesAndFees.INCLUDED).getAmount();
    }
}
