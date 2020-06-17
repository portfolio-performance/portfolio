package name.abuchen.portfolio.snapshot.security;

import java.util.Comparator;
import java.util.Optional;

import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;

/**
 * Sorts transactions by date and - if they have the same date - prefer inbound
 * transactions (buy, transfer in, inbound delivery) over outbound transactions
 * (sell, transfer out, outbound delivery). Needed to support FIFO based
 * calculations.
 */
/* package */final class CalculationLineItemComparator implements Comparator<CalculationLineItem>
{
    @Override
    public int compare(CalculationLineItem t1, CalculationLineItem t2)
    {
        int compare = t1.getDateTime().compareTo(t2.getDateTime());
        if (compare != 0)
            return compare;

        boolean first = isInbound(t1);
        boolean second = isInbound(t2);

        if (first ^ second)
            return first ? -1 : 1;
        else
            return 0;
    }

    private boolean isInbound(CalculationLineItem data)
    {
        if (data instanceof CalculationLineItem.ValuationAtStart)
            return true;

        Optional<Transaction> transaction = data.getTransaction();
        if (transaction.isPresent() && transaction.get() instanceof PortfolioTransaction)
            return ((PortfolioTransaction) transaction.get()).getType().isPurchase();

        return false;
    }
}
