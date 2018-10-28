package name.abuchen.portfolio.snapshot.security;

import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.CurrencyConverter;

/* package */class SharesHeldCalculation extends Calculation
{
    private long sharesHeld;

    @Override
    public void visit(CurrencyConverter converter, PortfolioTransaction t)
    {
        switch (t.getType())
        {
            case BUY:
            case DELIVERY_INBOUND:
                sharesHeld += t.getShares();
                break;
            case SELL:
            case DELIVERY_OUTBOUND:
                sharesHeld -= t.getShares();
                break;
            case TRANSFER_IN:
            case TRANSFER_OUT:
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    public long getSharesHeld()
    {
        return sharesHeld;
    }
}
