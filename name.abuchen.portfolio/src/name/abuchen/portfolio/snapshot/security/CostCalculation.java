package name.abuchen.portfolio.snapshot.security;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.TaxesAndFees;
import name.abuchen.portfolio.model.TransactionOwner;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.snapshot.trail.TrailRecord;

/* package */class CostCalculation extends Calculation
{
    public record DividendCostContext(Money cost, long totalShares)
    {
    }

    public record CostCalculationResult(Money cost, Money netCost, long sharesHeld, Optional<TrailRecord> trail,
                    Map<CalculationLineItem.DividendPayment, DividendCostContext> dividendCosts)
    {
        public CostCalculationResult
        {
            // Calculation and consumers share the same line-item instances;
            // identity therefore uniquely identifies each dividend event.
            dividendCosts = Collections.unmodifiableMap(new IdentityHashMap<>(dividendCosts));
        }

        public Optional<DividendCostContext> getDividendCost(CalculationLineItem.DividendPayment payment)
        {
            return Optional.ofNullable(dividendCosts.get(payment));
        }
    }

    private interface CostState
    {
        void visit(CurrencyConverter converter, CalculationLineItem.ValuationAtStart item, String termCurrency);

        void visit(CurrencyConverter converter, CalculationLineItem.TransactionItem item, PortfolioTransaction t,
                        String termCurrency);

        long getCost(TaxesAndFees taxesAndFees);

        long getSharesHeld();

        Optional<TrailRecord> getTrail(String termCurrency);
    }

    private static final class FifoCostState implements CostState
    {
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

            public LineItem(TransactionOwner<?> owner, long shares, long grossAmount, long netAmount,
                            TrailRecord trail)
            {
                this.owner = owner;
                this.shares = shares;
                this.grossAmount = grossAmount;
                this.netAmount = netAmount;
                this.trail = trail;
                this.originalShares = shares;
            }
        }

        private final List<LineItem> fifo = new ArrayList<>();

        @Override
        public void visit(CurrencyConverter converter, CalculationLineItem.ValuationAtStart item,
                        String termCurrency)
        {
            Money valuation = item.getValue();
            SecurityPosition position = item.getSecurityPosition().orElseThrow(IllegalArgumentException::new);
            long amount = converter.convert(item.getDateTime(), valuation).getAmount();

            add(converter, item, position, valuation, amount, termCurrency);
        }

        public void add(CurrencyConverter converter, CalculationLineItem.ValuationAtStart item,
                        SecurityPosition position, Money valuation, long amount, String termCurrency)
        {
            TrailRecord trail = TrailRecord.ofPosition(item.getDateTime().toLocalDate(), (Portfolio) item.getOwner(),
                            position);

            if (!termCurrency.equals(valuation.getCurrencyCode()))
                trail = trail.convert(Money.of(termCurrency, amount),
                                converter.getRate(item.getDateTime(), valuation.getCurrencyCode()));

            fifo.add(new LineItem(item.getOwner(), position.getShares(), amount, amount, trail));
        }

        public void add(CurrencyConverter converter, CalculationLineItem.TransactionItem item, PortfolioTransaction t,
                        long grossAmount, long netAmount, String termCurrency)
        {
            TrailRecord trail = TrailRecord.ofTransaction(t);
            if (!termCurrency.equals(t.getCurrencyCode()))
                trail = trail.convert(Money.of(termCurrency, grossAmount),
                                converter.getRate(t.getDateTime(), t.getCurrencyCode()));

            fifo.add(new LineItem(item.getOwner(), t.getShares(), grossAmount, netAmount, trail));
        }

        @Override
        public void visit(CurrencyConverter converter, CalculationLineItem.TransactionItem item,
                        PortfolioTransaction t, String termCurrency)
        {
            switch (t.getType())
            {
                case BUY:
                case DELIVERY_INBOUND:
                    add(converter, item, t, t.getMonetaryAmount(converter).getAmount(),
                                    t.getGrossValue(converter).getAmount(), termCurrency);
                    break;
                case SELL:
                case DELIVERY_OUTBOUND:
                    remove(item, t);
                    break;
                case TRANSFER_IN:
                    transfer(item, t, termCurrency);
                    break;
                case TRANSFER_OUT:
                    // ignore -> handled via TRANSFER_IN
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }

        public void remove(CalculationLineItem.TransactionItem item, PortfolioTransaction t)
        {
            long sold = t.getShares();

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
        }

        public void transfer(CalculationLineItem.TransactionItem item, PortfolioTransaction t, String termCurrency)
        {
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
                                    entry.trail.fraction(Money.of(termCurrency, transferredGrossAmount), n,
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
        }

        @Override
        public long getCost(TaxesAndFees taxesAndFees)
        {
            long cost = 0;
            for (LineItem entry : fifo)
                cost += taxesAndFees.isIncluded() ? entry.grossAmount : entry.netAmount;
            return cost;
        }

        @Override
        public Optional<TrailRecord> getTrail(String termCurrency)
        {
            return Optional.of(TrailRecord.of(fifo.stream().filter(entry -> entry.grossAmount > 0) //
                            .map(entry -> entry.trail.fraction(Money.of(termCurrency, entry.grossAmount), entry.shares,
                                            entry.originalShares))
                            .toList()));
        }

        @Override
        public long getSharesHeld()
        {
            long shares = 0;
            for (LineItem entry : fifo)
                shares += entry.shares;
            return shares;
        }
    }

    private static final class MovingAverageCostState implements CostState
    {
        private long movingRelativeCost;
        private long movingRelativeNetCost;
        private long heldShares;

        @Override
        public void visit(CurrencyConverter converter, CalculationLineItem.ValuationAtStart item,
                        String termCurrency)
        {
            Money valuation = item.getValue();
            SecurityPosition position = item.getSecurityPosition().orElseThrow(IllegalArgumentException::new);
            long amount = converter.convert(item.getDateTime(), valuation).getAmount();

            add(amount, position.getShares());
        }

        public void add(long amount, long shares)
        {
            movingRelativeCost += amount;
            movingRelativeNetCost += amount;
            heldShares += shares;
        }

        public void add(long grossAmount, long netAmount, long shares)
        {
            movingRelativeCost += grossAmount;
            movingRelativeNetCost += netAmount;
            heldShares += shares;
        }

        public void remove(long soldShares)
        {
            long remaining = heldShares - soldShares;
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
        }

        public void transfer()
        {
            // client-wide costs and shares do not change on portfolio transfers
        }

        @Override
        public void visit(CurrencyConverter converter, CalculationLineItem.TransactionItem item,
                        PortfolioTransaction t, String termCurrency)
        {
            switch (t.getType())
            {
                case BUY:
                case DELIVERY_INBOUND:
                    add(t.getMonetaryAmount(converter).getAmount(), t.getGrossValue(converter).getAmount(),
                                    t.getShares());
                    break;
                case SELL:
                case DELIVERY_OUTBOUND:
                    remove(t.getShares());
                    break;
                case TRANSFER_IN:
                case TRANSFER_OUT:
                    transfer();
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }

        @Override
        public long getCost(TaxesAndFees taxesAndFees)
        {
            return taxesAndFees.isIncluded() ? movingRelativeCost : movingRelativeNetCost;
        }

        @Override
        public long getSharesHeld()
        {
            return heldShares;
        }

        @Override
        public Optional<TrailRecord> getTrail(String termCurrency)
        {
            return Optional.empty();
        }
    }

    private final CostMethod costMethod;
    private final CostState state;

    private final Map<CalculationLineItem.DividendPayment, DividendCostContext> dividendCosts = new IdentityHashMap<>();

    public CostCalculation(CostMethod costMethod)
    {
        this.costMethod = Objects.requireNonNull(costMethod);
        this.state = createState(costMethod);
    }

    private static CostState createState(CostMethod costMethod)
    {
        return switch (costMethod)
        {
            case FIFO -> new FifoCostState();
            case MOVING_AVERAGE -> new MovingAverageCostState();
        };
    }

    @Override
    public void visit(CurrencyConverter converter, CalculationLineItem.ValuationAtStart item)
    {
        state.visit(converter, item, getTermCurrency());
    }

    @Override
    public void visit(CurrencyConverter converter, CalculationLineItem.TransactionItem item, PortfolioTransaction t)
    {
        state.visit(converter, item, t, getTermCurrency());
    }

    @Override
    public void visit(CurrencyConverter converter, CalculationLineItem.DividendPayment t)
    {
        dividendCosts.put(t, new DividendCostContext(getCost(TaxesAndFees.INCLUDED), state.getSharesHeld()));
    }

    public CostCalculationResult getResult()
    {
        return new CostCalculationResult(getCost(TaxesAndFees.INCLUDED), getCost(TaxesAndFees.NOT_INCLUDED),
                        state.getSharesHeld(), state.getTrail(getTermCurrency()), dividendCosts);
    }

    public Optional<TrailRecord> getCostTrail()
    {
        return state.getTrail(getTermCurrency());
    }

    public Money getCost(TaxesAndFees taxesAndFees)
    {
        return Money.of(getTermCurrency(), state.getCost(taxesAndFees));
    }
}
