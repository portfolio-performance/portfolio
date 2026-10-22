package name.abuchen.portfolio.cli;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private OversellGuard()
    {
    }

    /**
     * Reports a <code>negative-holding</code> failure if adding the change
     * would oversell.
     *
     * @param signedShares
     *            the stored shares that are added to the holding, negative if
     *            they leave it
     */
    static void check(OperationContext context, Portfolio portfolio, Security security, LocalDate date,
                    long signedShares)
    {
        var before = new TreeMap<LocalDate, Long>();
        for (var transaction : portfolio.getTransactions())
        {
            if (transaction.getSecurity() == security)
                before.merge(transaction.getDateTime().toLocalDate(), signed(transaction), Long::sum);
        }

        var after = new TreeMap<>(before);
        after.merge(date, signedShares, Long::sum);

        long holdingBefore = 0;
        long holdingAfter = 0;
        for (var day : after.keySet())
        {
            holdingBefore += before.getOrDefault(day, 0L);
            holdingAfter += after.get(day);

            if (holdingAfter < 0 && holdingAfter < holdingBefore)
            {
                context.fail("shares", "negative-holding",
                                portfolio.getName() + " would hold " + plain(holdingAfter) + " of "
                                                + security.getName() + " at the end of " + day
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
