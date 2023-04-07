package name.abuchen.portfolio.snapshot.security;

import java.util.Comparator;
import java.util.Optional;

import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;

/**
 * Sorts {@link CalculationLineItem} by date and ensures that a) inbound
 * transaction occur before outbound transactions and b) that valuations are
 * sorted at the start or end.
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

        compare = t1.getDateTime().compareTo(t2.getDateTime());
        if (compare != 0)
            return compare;

        boolean first = isInbound(t1);
        boolean second = isInbound(t2);

        if (first ^ second)
            return first ? -1 : 1;
        else
            return 0;
    }

    private int typeOrder(CalculationLineItem t1)
    {
        if (t1 instanceof CalculationLineItem.ValuationAtStart)
            return -1;
        else if (t1 instanceof CalculationLineItem.ValuationAtEnd)
            return 1;
        else
            return 0;
    }

    private boolean isInbound(CalculationLineItem data)
    {
        if (data instanceof CalculationLineItem.ValuationAtStart)
            return true;

        Optional<Transaction> transaction = data.getTransaction();
        if (transaction.isPresent() && transaction.get() instanceof PortfolioTransaction pt)
            return pt.getType().isPurchase();

        return false;
    }
}
