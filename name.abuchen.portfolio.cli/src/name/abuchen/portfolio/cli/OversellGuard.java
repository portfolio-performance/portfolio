package name.abuchen.portfolio.cli;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.TreeMap;

import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;

/**
 * Refuses a write that would make an investment account's holding of an
 * instrument negative at the end of a date. Only the dates are compared; the
 * time of day is ignored. An oversell that is already in the file is
 * tolerated: a date is only at fault if the holding there is lower than it was
 * before the write and below zero.
 */
final class OversellGuard
{
    /** Shares that a side of a record adds to or takes from an investment account on a date. */
    record Effect(Portfolio portfolio, Security security, LocalDate date, long shares)
    {
    }

    private record Pair(Portfolio portfolio, Security security)
    {
        boolean covers(Effect effect)
        {
            return portfolio == effect.portfolio() && security == effect.security();
        }
    }

    private OversellGuard()
    {
    }

    /** What the investment-account sides among the given ones do to the holdings. */
    static List<Effect> effectsOf(List<BuiltRecord.Side> sides)
    {
        return sides.stream().filter(side -> side.transaction() instanceof PortfolioTransaction
                        && side.owner() instanceof Portfolio && side.transaction().getSecurity() != null)
                        .map(side -> new Effect((Portfolio) side.owner(), side.transaction().getSecurity(),
                                        side.transaction().getDateTime().toLocalDate(),
                                        signed((PortfolioTransaction) side.transaction())))
                        .toList();
    }

    /**
     * Reports a <code>negative-holding</code> failure for every holding that
     * the change would oversell.
     *
     * @param field
     *            the field the failure is reported on
     * @param removed
     *            effects that are in the model now and that the change takes
     *            away
     * @param added
     *            effects that the change adds
     */
    static void check(OperationContext context, String field, List<Effect> removed, List<Effect> added)
    {
        var pairs = new LinkedHashSet<Pair>();
        removed.forEach(e -> pairs.add(new Pair(e.portfolio(), e.security())));
        added.forEach(e -> pairs.add(new Pair(e.portfolio(), e.security())));

        for (var pair : pairs)
            check(context, field, pair, removed, added);
    }

    private static void check(OperationContext context, String field, Pair pair, List<Effect> removed,
                    List<Effect> added)
    {
        var before = new TreeMap<LocalDate, Long>();
        for (var transaction : pair.portfolio().getTransactions())
        {
            if (transaction.getSecurity() == pair.security())
                before.merge(transaction.getDateTime().toLocalDate(), signed(transaction), Long::sum);
        }

        var after = new TreeMap<>(before);
        removed.stream().filter(pair::covers).forEach(e -> after.merge(e.date(), -e.shares(), Long::sum));
        added.stream().filter(pair::covers).forEach(e -> after.merge(e.date(), e.shares(), Long::sum));

        long holdingBefore = 0;
        long holdingAfter = 0;
        for (var day : after.keySet())
        {
            holdingBefore += before.getOrDefault(day, 0L);
            holdingAfter += after.get(day);

            if (holdingAfter < 0 && holdingAfter < holdingBefore)
            {
                context.fail(field, "negative-holding",
                                pair.portfolio().getName() + " would hold " + plain(holdingAfter) + " of "
                                                + pair.security().getName() + " at the end of " + day
                                                + ", a shortfall of " + plain(-holdingAfter) + " shares.");
                return;
            }
        }
    }

    private static long signed(PortfolioTransaction transaction)
    {
        return transaction.getType().isPurchase() ? transaction.getShares() : -transaction.getShares();
    }

    /** The stored share count without trailing zeros, never in exponent form. */
    private static String plain(long stored)
    {
        return BigDecimal.valueOf(stored, Values.Share.precision()).stripTrailingZeros().toPlainString();
    }
}
