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

        int compare = Integer.compare(typeOrder(t1), typeOrder(t2));
        if (compare != 0)
            return compare;

        var dt1 = t1.getDateTime();
        var dt2 = t2.getDateTime();

        // Compare year, month, day
        if (dt1.getYear() != dt2.getYear())
            return Integer.compare(dt1.getYear(), dt2.getYear());
        if (dt1.getMonthValue() != dt2.getMonthValue())
            return Integer.compare(dt1.getMonthValue(), dt2.getMonthValue());
        if (dt1.getDayOfMonth() != dt2.getDayOfMonth())
            return Integer.compare(dt1.getDayOfMonth(), dt2.getDayOfMonth());

        var sortOrder = Integer.compare(getSortOrder(t1), getSortOrder(t2));
        if (sortOrder != 0)
            return sortOrder;

        // Compare hour
        if (dt1.getHour() != dt2.getHour())
            return Integer.compare(dt1.getHour(), dt2.getHour());

        // Compare minute
        return Integer.compare(dt1.getMinute(), dt2.getMinute());
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

            return txa.getType().isCredit() ? 1 : 3;
        }
        else
        {
            throw new IllegalArgumentException(tx.get().getClass().getName());
        }
    }

}
