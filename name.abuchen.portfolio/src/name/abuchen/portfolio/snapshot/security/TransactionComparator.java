package name.abuchen.portfolio.snapshot.security;

import java.util.Comparator;

import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.model.Transaction;

/**
 * Sorts transactions by date and - if they have the same date - prefer inbound
 * transactions (buy, transfer in, inbound delivery) over outbound transactions
 * (sell, transfer out, outbound delivery). Needed to support FIFO based
 * calculations.
 */
/* package */final class TransactionComparator implements Comparator<Transaction>
{
    @Override
    public int compare(Transaction t1, Transaction t2)
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

    private boolean isInbound(Transaction t)
    {
        if (t instanceof DividendInitialTransaction)
            return true;

        if (t instanceof PortfolioTransaction)
        {
            Type type = ((PortfolioTransaction) t).getType();
            return type == Type.BUY || type == Type.DELIVERY_INBOUND || type == Type.TRANSFER_IN;
        }

        return false;
    }
}
