package name.abuchen.portfolio.snapshot.security;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransaction.Type;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MutableMoney;
import name.abuchen.portfolio.snapshot.trail.TrailRecord;

/* package */class DeltaCalculation extends Calculation
{
    private MutableMoney delta;
    private MutableMoney cost;

    // the delta is built from contributions that either increase it (market
    // value at end, sells, dividends, refunds) or decrease it (valuation at
    // start, buys, taxes, fees). They are collected separately so the trail can
    // be rendered as "sum of inflows - sum of outflows"
    private final List<TrailRecord> positive = new ArrayList<>();
    private final List<TrailRecord> negative = new ArrayList<>();

    @Override
    public void setTermCurrency(String termCurrency)
    {
        super.setTermCurrency(termCurrency);
        this.delta = MutableMoney.of(termCurrency);
        this.cost = MutableMoney.of(termCurrency);
    }

    @Override
    public void visit(CurrencyConverter converter, CalculationLineItem.ValuationAtStart t)
    {
        Money amount = t.getValue().with(converter.at(t.getDateTime()));
        delta.subtract(amount);
        cost.add(amount);

        negative.add(valuationTrail(converter, t, amount));
    }

    @Override
    public void visit(CurrencyConverter converter, CalculationLineItem.ValuationAtEnd t)
    {
        Money amount = t.getValue().with(converter.at(t.getDateTime()));
        delta.add(amount);

        positive.add(valuationTrail(converter, t, amount));
    }

    @Override
    public void visit(CurrencyConverter converter, CalculationLineItem.DividendPayment t)
    {
        Money amount = t.getValue().with(converter.at(t.getDateTime()));
        delta.add(amount);

        positive.add(transactionTrail(converter, t, amount));
    }

    @Override
    public void visit(CurrencyConverter converter, CalculationLineItem.TransactionItem item, AccountTransaction t)
    {
        Money amount = t.getMonetaryAmount().with(converter.at(t.getDateTime()));

        Type type = t.getType();
        switch (type)
        {
            case TAXES:
            case FEES:
                delta.subtract(amount);
                negative.add(transactionTrail(converter, item, amount));
                break;
            case TAX_REFUND:
            case FEES_REFUND:
                delta.add(amount);
                positive.add(transactionTrail(converter, item, amount));
                break;
            default:
                throw new IllegalArgumentException("unsupported type " + type); //$NON-NLS-1$
        }

    }

    @Override
    public void visit(CurrencyConverter converter, CalculationLineItem.TransactionItem item, PortfolioTransaction t)
    {
        Money amount = t.getMonetaryAmount().with(converter.at(t.getDateTime()));

        name.abuchen.portfolio.model.PortfolioTransaction.Type type = t.getType();
        switch (type)
        {
            case BUY:
            case DELIVERY_INBOUND:
                delta.subtract(amount);
                cost.add(amount);
                negative.add(transactionTrail(converter, item, amount));
                break;
            case SELL:
            case DELIVERY_OUTBOUND:
                delta.add(amount);
                positive.add(transactionTrail(converter, item, amount));
                break;
            case TRANSFER_IN:
            case TRANSFER_OUT:
                // transferals do not contribute to the delta
                break;
            default:
                throw new UnsupportedOperationException("unsupported type " + type); //$NON-NLS-1$
        }
    }

    public Money getDelta()
    {
        return delta.toMoney();
    }

    public double getDeltaPercent()
    {
        if (delta.getAmount() == 0L && cost.getAmount() == 0L)
            return 0d;

        return delta.getAmount() / (double) cost.getAmount();
    }

    /**
     * Builds the trail that explains how {@link #getDelta()} is calculated:
     * the sum of all inflows minus the sum of all outflows.
     */
    public TrailRecord getDeltaTrail()
    {
        if (negative.isEmpty())
            return TrailRecord.of(positive);

        // TrailRecord.subtract() cannot be called on an empty trail. When there
        // are no inflows the delta is a pure outflow (e.g. a standalone fee),
        // so use a zero-valued trail as the minuend to render "0 - outflows".
        TrailRecord inflows = positive.isEmpty() ? TrailRecord.ofZero(getTermCurrency()) : TrailRecord.of(positive);

        return inflows.subtract(TrailRecord.of(negative));
    }

    private TrailRecord valuationTrail(CurrencyConverter converter, CalculationLineItem.Valuation valuation,
                    Money converted)
    {
        var position = valuation.getSecurityPosition().orElseThrow(IllegalArgumentException::new);
        TrailRecord trail = TrailRecord.ofPosition(valuation.getDateTime().toLocalDate(),
                        (Portfolio) valuation.getOwner(), position);
        return convertIfNeeded(converter, trail, valuation.getValue(), converted, valuation.getDateTime());
    }

    private TrailRecord transactionTrail(CurrencyConverter converter, CalculationLineItem.TransactionItem item,
                    Money converted)
    {
        var transaction = item.getTransaction().orElseThrow(IllegalArgumentException::new);
        TrailRecord trail = TrailRecord.ofTransaction(transaction);
        return convertIfNeeded(converter, trail, transaction.getMonetaryAmount(), converted, item.getDateTime());
    }

    private TrailRecord convertIfNeeded(CurrencyConverter converter, TrailRecord trail, Money original,
                    Money converted, LocalDateTime date)
    {
        if (original.getCurrencyCode().equals(getTermCurrency()))
            return trail;
        return trail.convert(converted, converter.getRate(date, original.getCurrencyCode()));
    }
}
