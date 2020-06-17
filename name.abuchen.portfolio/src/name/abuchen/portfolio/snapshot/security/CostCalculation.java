package name.abuchen.portfolio.snapshot.security;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.snapshot.trail.TrailRecord;

/* package */class CostCalculation extends Calculation
{
    private static class LineItem
    {
        private long shares;
        private long grossAmount;
        private long netAmount;

        private final TrailRecord trail;

        /**
         * Holds the original number of shares (of the transaction). The
         * original shares are needed to calculate fractions if the transaction
         * is split up multiple times
         */
        private final long originalShares;

        public LineItem(long shares, long grossAmount, long netAmount, TrailRecord trail)
        {
            this.shares = shares;
            this.grossAmount = grossAmount;
            this.netAmount = netAmount;
            this.trail = trail;
            this.originalShares = shares;
        }
    }

    private List<LineItem> fifo = new ArrayList<>();

    private long movingRelativeCost = 0;
    private long movingRelativeNetCost = 0;
    private long heldShares = 0;

    private long fees;
    private long taxes;

    @Override
    public void visit(CurrencyConverter converter, CalculationLineItem.ValuationAtStart item)
    {
        Money valuation = item.getValue();
        SecurityPosition position = item.getSecurityPosition().orElseThrow(IllegalArgumentException::new);

        long amount = converter.convert(item.getDateTime(), valuation).getAmount();

        TrailRecord trail = TrailRecord.ofPosition(item.getDateTime().toLocalDate(), position);

        if (!getTermCurrency().equals(valuation.getCurrencyCode()))
            trail = trail.convert(Money.of(getTermCurrency(), amount),
                            converter.getRate(item.getDateTime(), valuation.getCurrencyCode()));

        fifo.add(new LineItem(position.getShares(), amount, amount, trail));
        movingRelativeCost += amount;
        movingRelativeNetCost += amount;
        heldShares += position.getShares();
    }

    @Override
    public void visit(CurrencyConverter converter, CalculationLineItem.TransactionItem item, PortfolioTransaction t)
    {
        long fee = t.getUnitSum(Unit.Type.FEE, converter).getAmount();
        long tax = t.getUnitSum(Unit.Type.TAX, converter).getAmount();
        fees += fee;
        taxes += tax;

        switch (t.getType())
        {
            case BUY:
            case DELIVERY_INBOUND:
                long grossAmount = t.getMonetaryAmount(converter).getAmount();
                long netAmount = t.getGrossValue(converter).getAmount();

                TrailRecord trail = TrailRecord.ofTransaction(t);
                if (!getTermCurrency().equals(t.getCurrencyCode()))
                    trail = trail.convert(Money.of(getTermCurrency(), grossAmount),
                                    converter.getRate(t.getDateTime(), t.getCurrencyCode()));

                fifo.add(new LineItem(t.getShares(), grossAmount, netAmount, trail));
                movingRelativeCost += grossAmount;
                movingRelativeNetCost += netAmount;
                heldShares += t.getShares();
                break;
            case SELL:
            case DELIVERY_OUTBOUND:
                long sold = t.getShares();

                long remaining = heldShares - sold;
                if (remaining <= 0)
                {
                    movingRelativeCost = 0;
                    movingRelativeNetCost = 0;
                    heldShares = 0;
                }
                else
                {
                    movingRelativeCost = Math.round(movingRelativeCost / (double) heldShares * remaining);
                    movingRelativeNetCost = Math.round(movingRelativeNetCost / (double) heldShares * remaining);
                    heldShares = remaining;
                }

                for (LineItem entry : fifo)
                {
                    if (entry.shares == 0)
                        continue;

                    if (sold <= 0)
                        break;

                    long n = Math.min(sold, entry.shares);

                    entry.grossAmount -= Math.round(n / (double) entry.shares * entry.grossAmount);
                    entry.netAmount -= Math.round(n / (double) entry.shares * entry.netAmount);
                    entry.shares -= n;

                    sold -= n;

                }

                if (sold > 0)
                {
                    // FIXME Oops. More sold than bought.
                    PortfolioLog.warning(MessageFormat.format(Messages.MsgNegativeHoldingsDuringFIFOCostCalculation,
                                    Values.Share.format(sold), t.getSecurity().getName(),
                                    Values.DateTime.format(t.getDateTime())));
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
    public void visit(CurrencyConverter converter, CalculationLineItem.TransactionItem item, AccountTransaction t)
    {
        switch (t.getType())
        {
            case TAXES:
                taxes += converter.convert(t.getDateTime(), t.getMonetaryAmount()).getAmount();
                break;
            case TAX_REFUND:
                taxes -= converter.convert(t.getDateTime(), t.getMonetaryAmount()).getAmount();
                break;
            case FEES:
                fees += converter.convert(t.getDateTime(), t.getMonetaryAmount()).getAmount();
                break;
            case FEES_REFUND:
                fees -= converter.convert(t.getDateTime(), t.getMonetaryAmount()).getAmount();
                break;
            default:
        }
    }

    @Override
    public void visit(CurrencyConverter converter, CalculationLineItem.DividendPayment t)
    {
        taxes += t.getTransaction().orElseThrow(IllegalArgumentException::new).getUnitSum(Unit.Type.TAX, converter)
                        .getAmount();

        t.setFifoCost(getFifoCost());
        t.setMovingAverageCost(getMovingAverageCost());
        t.setTotalShares(getSharesHeld());
    }

    /**
     * gross investment
     */
    public Money getFifoCost()
    {
        long cost = 0;
        for (LineItem entry : fifo)
            cost += entry.grossAmount;
        return Money.of(getTermCurrency(), cost);
    }

    public TrailRecord getFifoCostTrail()
    {
        return TrailRecord.of(fifo.stream().filter(entry -> entry.grossAmount > 0) //
                        .map(entry -> entry.trail.fraction(Money.of(getTermCurrency(), entry.grossAmount), entry.shares,
                                        entry.originalShares))
                        .collect(Collectors.toList()));
    }

    /**
     * net investment, i.e. without fees and taxes
     */
    public Money getNetFifoCost()
    {
        long cost = 0;
        for (LineItem entry : fifo)
            cost += entry.netAmount;
        return Money.of(getTermCurrency(), cost);
    }

    /**
     * gross investment
     */
    public Money getMovingAverageCost()
    {
        return Money.of(getTermCurrency(), movingRelativeCost);
    }

    /**
     * net investment, i.e. without fees and taxes
     */
    public Money getNetMovingAverageCost()
    {
        return Money.of(getTermCurrency(), movingRelativeNetCost);
    }

    private long getSharesHeld()
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
