package name.abuchen.portfolio.snapshot.security;

import java.util.Comparator;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction;

/**
 * Sorts {@link CalculationLineItem} by date and ensures that <br/>
 * a) inbound transaction occur before transfers and outbound transactions and
 * <br/>
 * b) that valuations are sorted at the start or end.
 */
/* package */final class CalculationLineItemComparator implements Comparator<CalculationLineItem>
{
    @Override
    public int compare(CalculationLineItem t1, CalculationLineItem t2)
    {
        // make sure that "valuation at start" items are always first and
        // "valuation at end" items are always last

        int compare = typeOrder(t1) - typeOrder(t2);
        if (compare != 0)
            return compare;

        var dt1 = t1.getDateTime();
        var dt2 = t2.getDateTime();

        // the date differs, just sort by date (no need to check types)
        if (dt1.getYear() != dt2.getYear() || dt1.getMonth() != dt2.getMonth()
                        || dt1.getDayOfMonth() != dt2.getDayOfMonth())
            return dt1.compareTo(dt2);

        var hasTime1 = dt1.getHour() != 0 || dt1.getMinute() != 0;
        var hasTime2 = dt2.getHour() != 0 || dt2.getMinute() != 0;

        // if both transactions have a time, then sort by time
        if (hasTime1 && hasTime2)
            return dt1.compareTo(dt2);

        // otherwise sort: inbounds, transfers, outbounds
        return getSortOrder(t1) - getSortOrder(t2);
    }

    private int typeOrder(CalculationLineItem item)
    {
        if (item instanceof CalculationLineItem.ValuationAtStart)
            return -1;
        else if (item instanceof CalculationLineItem.ValuationAtEnd)
            return 1;
        else
            return 0;
    }

    /**
     * Returns 1 for inbound types (purchase, inbound delivery), 2 for
     * transfers, and 3 for outbound types
     */
    private int getSortOrder(CalculationLineItem item)
    {
        var tx = item.getTransaction();
        if (!tx.isPresent())
            return 2;

        if (tx.get() instanceof PortfolioTransaction txp)
        {
            if (txp.getType() == PortfolioTransaction.Type.TRANSFER_IN
                            || txp.getType() == PortfolioTransaction.Type.TRANSFER_OUT)
                return 2;

            return txp.getType().isPurchase() ? 1 : 3;
        }
        else if (tx.get() instanceof AccountTransaction txa)
        {
            if (txa.getType() == AccountTransaction.Type.TRANSFER_IN
                            || txa.getType() == AccountTransaction.Type.TRANSFER_OUT)
                return 2;

            return txa.getType().isDebit() ? 1 : 3;
        }
        else
        {
            throw new IllegalArgumentException(tx.get().getClass().getName());
        }
    }

}
