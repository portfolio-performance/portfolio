package name.abuchen.portfolio.snapshot.security;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.TransactionOwner;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.snapshot.trail.TrailRecord;

/**
 * CostCalculation is a class used to calculate indicators related to the cost
 * of given security. CostCalculation traverses different types of transaction
 * records (such as portfolio transactions, account transactions, dividend
 * payments, etc.), and uses the FIFO method and the moving average cost method
 * to calculate the cost, fees and taxes of holding stocks.
 */
class CostCalculation extends Calculation
{

    public record CostCalculationResult(long sharesHeld, Money fifoCost, TrailRecord fifoCostTrail, Money netFifoCost,
                    Money movingAverageCost, Money netMovingAverageCost, Money fees, Money taxes)
    {
    }

    private static class LineItem
    {
        private TransactionOwner<?> owner;
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

        public LineItem(TransactionOwner<?> owner, long shares, long grossAmount, long netAmount, TrailRecord trail)
        {
            this.owner = owner;
            this.shares = shares;
            this.grossAmount = grossAmount;
            this.netAmount = netAmount;
            this.trail = trail;
            this.originalShares = shares;
        }
    }

    /**
     * Store transaction records in FIFO order.
     */
    private List<LineItem> fifo = new ArrayList<>();

    private long movingRelativeCost = 0;

    private long movingRelativeNetCost = 0;

    private long heldShares = 0;

    private long fees;

    private long taxes;

    /**
     * The caller will sequentially call the `visit` method for all transaction
     * of a certain stock to perform the aggregation.
     */
    @Override
    public void visit(CurrencyConverter converter, CalculationLineItem.ValuationAtStart item)
    {
        // Convert the starting valuation to the specified currency.
        Money valuation = item.getValue();
        SecurityPosition position = item.getSecurityPosition().orElseThrow(IllegalArgumentException::new);

        long amount = converter.convert(item.getDateTime(), valuation).getAmount();

        TrailRecord trail = TrailRecord.ofPosition(item.getDateTime().toLocalDate(), (Portfolio) item.getOwner(),
                        position);

        if (!getTermCurrency().equals(valuation.getCurrencyCode()))
            trail = trail.convert(Money.of(getTermCurrency(), amount),
                            converter.getRate(item.getDateTime(), valuation.getCurrencyCode()));

        // Record the relevant information into the fifo list.
        fifo.add(new LineItem(item.getOwner(), position.getShares(), amount, amount, trail));
        movingRelativeCost += amount;
        movingRelativeNetCost += amount;
        heldShares += position.getShares();
    }

    /**
     * This method handles portfolio transactions. First, it accumulates the
     * fees and taxes in the transaction. Then, depending on the transaction
     * type (buy, sell, transfer-in, transfer-out, etc.), it updates the fifo
     * list, the moving average cost, and the number of held shares. For a sell
     * operation, it deducts the corresponding stocks and amounts from the fifo
     * list according to the FIFO principle. For a transfer-in operation, it
     * transfers the corresponding stocks and amounts from the source owner's
     * records to the target owner.
     */
    @Override
    public void visit(CurrencyConverter converter, CalculationLineItem.TransactionItem item, PortfolioTransaction t)
    {
        long fee = t.getUnitSum(Unit.Type.FEE, converter).getAmount();
        long tax = t.getUnitSum(Unit.Type.TAX, converter).getAmount();
        fees += fee;
        taxes += tax;

        switch (t.getType())
        {
            case BUY, DELIVERY_INBOUND:
                // 'buy' only need to add transaction to fifo, and update
                // movingRelativeXXX, heldShares.
                long grossAmount = t.getMonetaryAmount(converter).getAmount();
                long netAmount = t.getGrossValue(converter).getAmount();

                TrailRecord trail = TrailRecord.ofTransaction(t);
                if (!getTermCurrency().equals(t.getCurrencyCode()))
                    trail = trail.convert(Money.of(getTermCurrency(), grossAmount),
                                    converter.getRate(t.getDateTime(), t.getCurrencyCode()));

                fifo.add(new LineItem(item.getOwner(), t.getShares(), grossAmount, netAmount, trail));
                movingRelativeCost += grossAmount;
                movingRelativeNetCost += netAmount;
                heldShares += t.getShares();
                break;

            case SELL, DELIVERY_OUTBOUND:
                // 'sell' is more complex than 'buy', we need to traverse `fifo`
                // list from earliest to latest, to sell the shares in order.
                long sold = t.getShares();

                long remaining = heldShares - sold;
                if (remaining <= 0) // Quickly check for liquidation operation
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

                // traverse fifo to find held shares and mark them as sold
                for (LineItem entry : fifo)
                {
                    if (sold <= 0)
                        break;

                    if (!entry.owner.equals(item.getOwner()))
                        continue;

                    if (entry.shares == 0)
                        continue;

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
                long moved = t.getShares();

                TransactionOwner<?> source = t.getCrossEntry().getCrossOwner(t);

                // iterate on copy b/c underlying list can be changed
                for (LineItem entry : new ArrayList<>(fifo))
                {
                    if (moved <= 0)
                        break;

                    if (!entry.owner.equals(source))
                        continue;

                    if (entry.shares == 0)
                        continue;

                    long n = Math.min(moved, entry.shares);

                    if (n == entry.shares)
                    {
                        // if all shares are moved, simply re-assign owner of
                        // the shares
                        entry.owner = item.getOwner();
                    }
                    else
                    {
                        long transferredGrossAmount = Math.round(n / (double) entry.shares * entry.grossAmount);
                        long transferredNetAmount = Math.round(n / (double) entry.shares * entry.netAmount);

                        LineItem transfer = new LineItem(item.getOwner(), //
                                        n, //
                                        transferredGrossAmount, //
                                        transferredNetAmount, //
                                        entry.trail.fraction(Money.of(getTermCurrency(), transferredGrossAmount), n,
                                                        entry.originalShares) //
                                                        .transfer(item.getDateTime().toLocalDate(), entry.owner,
                                                                        item.getOwner()));

                        entry.grossAmount -= transferredGrossAmount;
                        entry.netAmount -= transferredNetAmount;
                        entry.shares -= n;

                        fifo.add(fifo.indexOf(entry) + 1, transfer);
                    }

                    moved -= n;
                }

                if (moved > 0)
                {
                    // FIXME Oops. More moved than available.
                    PortfolioLog.warning(MessageFormat.format(Messages.MsgNegativeHoldingsDuringFIFOCostCalculation,
                                    Values.Share.format(moved), t.getSecurity().getName(),
                                    Values.DateTime.format(t.getDateTime())));
                }

                break;

            case TRANSFER_OUT:
                // ignore -> handled via TRANSFER_IN
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    /**
     * Accumulate or reduce the corresponding fees and taxes according to the
     * transaction type (tax, tax refund, fee, fee refund).
     */
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

    public CostCalculationResult getResult()
    {
        return new CostCalculationResult(getSharesHeld(), getFifoCost(), getFifoCostTrail(), getNetFifoCost(),
                        getMovingAverageCost(), getNetMovingAverageCost(), getFees(), getTaxes());
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
                        .toList());
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
