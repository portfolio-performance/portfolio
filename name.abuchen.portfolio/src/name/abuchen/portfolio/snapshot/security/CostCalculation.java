package name.abuchen.portfolio.snapshot.security;

import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;

/* package */class CostCalculation extends Calculation
{
    private static class LineItem
    {
        public long shares;
        public long grossAmount;
        public long netAmount;

        public LineItem(long shares, long grossAmount, long netAmount)
        {
            this.shares = shares;
            this.grossAmount = grossAmount;
            this.netAmount = netAmount;
        }
    }

    private List<LineItem> fifo = new ArrayList<>();

    private long fees;
    private long taxes;

    @Override
    public void visit(CurrencyConverter converter, DividendInitialTransaction t)
    {
        long amount = converter.convert(t.getDate(), t.getMonetaryAmount()).getAmount();
        fifo.add(new LineItem(t.getPosition().getShares(), amount, amount));
    }

    @Override
    public void visit(CurrencyConverter converter, PortfolioTransaction t)
    {
        fees += t.getUnitSum(Unit.Type.FEE, converter).getAmount();
        taxes += t.getUnitSum(Unit.Type.TAX, converter).getAmount();

        switch (t.getType())
        {
            case BUY:
            case DELIVERY_INBOUND:
                long grossAmount = converter.convert(t.getDate(), t.getMonetaryAmount()).getAmount();
                long netAmount = converter.convert(t.getDate(), t.getGrossValue()).getAmount();
                fifo.add(new LineItem(t.getShares(), grossAmount, netAmount));
                break;
            case SELL:
            case DELIVERY_OUTBOUND:
                long sold = t.getShares();
                for (LineItem entry : fifo)
                {
                    if (entry.shares == 0)
                        continue;

                    if (sold <= 0)
                        break;

                    long n = Math.min(sold, entry.shares);

                    entry.grossAmount -= Math.round(n * entry.grossAmount / entry.shares);
                    entry.netAmount -= Math.round(n * entry.netAmount / entry.shares);
                    entry.shares -= n;

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
    public void visit(CurrencyConverter converter, AccountTransaction t)
    {
        if (t.getType() == AccountTransaction.Type.TAX_REFUND)
            taxes -= converter.convert(t.getDate(), t.getMonetaryAmount()).getAmount();
    }

    @Override
    public void visit(CurrencyConverter converter, DividendTransaction t)
    {
        t.setFifoCost(getFifoCost());
        t.setTotalShares(getSharesHeld());
    }

    /**
     * gross investment
     * @return Money
     */
    public Money getFifoCost()
    {
        long cost = 0;
        for (LineItem entry : fifo)
            cost += entry.grossAmount;
        return Money.of(getTermCurrency(), cost);
    }

    /**
     * net investment, i.e. without fees and taxes
     * @return Money
     */
    public Money getNetFifoCost()
    {
        long cost = 0;
        for (LineItem entry : fifo)
            cost += entry.netAmount;
        return Money.of(getTermCurrency(), cost);
    }

    public long getSharesHeld()
    {
        long shares = 0;
        for (LineItem entry : fifo)
            shares += entry.shares;
        return shares;
    }

    public Money getFees()
    {
        return Money.of(getTermCurrency(), fees);
    }

    public Money getTaxes()
    {
        return Money.of(getTermCurrency(), taxes);
    }

}
