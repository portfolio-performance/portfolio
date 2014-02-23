package name.abuchen.portfolio.snapshot.security;

import name.abuchen.portfolio.model.PortfolioTransaction;

/* package */class DeltaCalculation extends Calculation
{
    private long delta;

    @Override
    public void visit(DividendInitialTransaction t)
    {
        delta -= t.getAmount();
    }

    @Override
    public void visit(DividendFinalTransaction t)
    {
        delta += t.getAmount();
    }

    @Override
    public void visit(DividendTransaction t)
    {
        delta += t.getAmount();
    }

    @Override
    public void visit(PortfolioTransaction t)
    {
        switch (t.getType())
        {
            case BUY:
            case DELIVERY_INBOUND:
                delta -= t.getAmount();
                break;
            case SELL:
            case DELIVERY_OUTBOUND:
                delta += t.getAmount();
                break;
            case TRANSFER_IN:
            case TRANSFER_OUT:
                // transferals do not contribute to the delta
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    public long getDelta()
    {
        return delta;
    }
}
