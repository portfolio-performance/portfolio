package name.abuchen.portfolio.snapshot.security;

import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.PortfolioTransaction;

/* package */class CostCalculation extends AbstractTransactionVisitor
{
    private List<long[]> fifo = new ArrayList<long[]>();

    @Override
    public void visit(DividendInitialTransaction t)
    {
        fifo.add(new long[] { t.getPosition().getShares(), t.getAmount() });
    }

    @Override
    public void visit(PortfolioTransaction t)
    {
        switch (t.getType())
        {
            case BUY:
            case DELIVERY_INBOUND:
                fifo.add(new long[] { t.getShares(), t.getAmount() });
                break;
            case SELL:
            case DELIVERY_OUTBOUND:
                long sold = t.getShares();
                for (long[] entry : fifo)
                {
                    if (sold <= 0)
                        break;

                    long n = Math.min(sold, entry[0]);

                    entry[1] -= Math.round(n * entry[1] / entry[0]);
                    entry[0] -= n;

                    sold -= n;
                }
                break;
            case TRANSFER_IN:
            case TRANSFER_OUT:
                // do nothing
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public void visit(DividendTransaction t)
    {
        t.setFifoCost(getFifoCost());
    }

    public long getFifoCost()
    {
        long cost = 0;
        for (long[] entry : fifo)
            cost += entry[1];
        return cost;
    }

    public long getSharesHeld()
    {
        long shares = 0;
        for (long[] entry : fifo)
            shares += entry[0];
        return shares;
    }
}
