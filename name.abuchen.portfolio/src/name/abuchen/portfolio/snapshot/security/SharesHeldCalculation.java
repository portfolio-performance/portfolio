package name.abuchen.portfolio.snapshot.security;

import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.CurrencyConverter;

/* package */class SharesHeldCalculation extends Calculation
{
    private long sharesHeld;

    @Override
    public void visit(CurrencyConverter converter, CalculationLineItem.ValuationAtStart item)
    {
        // there can be multiple CalculationLineItem.ValuationAtStart if the
        // same security is held in multiple portfolios --> addition
        sharesHeld += item.getSecurityPosition().orElseThrow(IllegalArgumentException::new).getShares();
    }

    @Override
    public void visit(CurrencyConverter converter, CalculationLineItem.TransactionItem item, PortfolioTransaction t)
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
