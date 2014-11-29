package name.abuchen.portfolio.snapshot.security;

import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.PortfolioTransaction;

/* package */class CostCalculation extends Calculation
{
    private List<long[]> fifo = new ArrayList<long[]>();

    private long fees;
    private long taxes;

    @Override
    public void visit(DividendInitialTransaction t)
    {
        fifo.add(new long[] { t.getPosition().getShares(), t.getAmount() });
    }

    @Override
    public void visit(PortfolioTransaction t)
    {
        fees += t.getFees();
        taxes += t.getTaxes();

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
                    if (entry[0] == 0)
                        continue;

                    if (sold <= 0)
                        break;

                    long n = Math.min(sold, entry[0]);

                    entry[1] -= Math.round(n * entry[1] / entry[0]);
                    entry[0] -= n;

                    sold -= n;
                }

                if (sold > 0)
                {
                    // FIXME Oops. More sold than bought. Report error? Ignore?
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
        t.setTotalShares(getSharesHeld());
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

    public long getFees()
    {
        return fees;
    }

    public long getTaxes()
    {
        return taxes;
    }

}
