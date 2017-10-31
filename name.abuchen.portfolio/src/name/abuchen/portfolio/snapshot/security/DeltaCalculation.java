package name.abuchen.portfolio.snapshot.security;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MutableMoney;

/* package */class DeltaCalculation extends Calculation
{
    private MutableMoney delta;
    private MutableMoney cost;

    @Override
    public void setTermCurrency(String termCurrency)
    {
        super.setTermCurrency(termCurrency);
        this.delta = MutableMoney.of(termCurrency);
        this.cost = MutableMoney.of(termCurrency);
    }

    @Override
    public void visit(CurrencyConverter converter, DividendInitialTransaction t)
    {
        Money amount = t.getMonetaryAmount().with(converter.at(t.getDate()));
        delta.subtract(amount);
        cost.add(amount);
    }

    @Override
    public void visit(CurrencyConverter converter, DividendFinalTransaction t)
    {
        delta.add(t.getMonetaryAmount().with(converter.at(t.getDate())));
    }

    @Override
    public void visit(CurrencyConverter converter, DividendTransaction t)
    {
        delta.add(t.getMonetaryAmount().with(converter.at(t.getDate())));
    }

    @Override
    public void visit(CurrencyConverter converter, AccountTransaction t)
    {
        switch(t.getType())
        {
            case TAXES:
            case FEES:
                delta.subtract(t.getMonetaryAmount().with(converter.at(t.getDate())));
                break;
            case TAX_REFUND:
            case FEES_REFUND:
                delta.add(t.getMonetaryAmount().with(converter.at(t.getDate())));
                break;
            default:
                throw new IllegalArgumentException();
        }
        
    }

    @Override
    public void visit(CurrencyConverter converter, PortfolioTransaction t)
    {
        switch (t.getType())
        {
            case BUY:
            case DELIVERY_INBOUND:
                Money amount = t.getMonetaryAmount().with(converter.at(t.getDate()));
                delta.subtract(amount);
                cost.add(amount);
                break;
            case SELL:
            case DELIVERY_OUTBOUND:
                delta.add(t.getMonetaryAmount().with(converter.at(t.getDate())));
                break;
            case TRANSFER_IN:
            case TRANSFER_OUT:
                // transferals do not contribute to the delta
                break;
            default:
                throw new UnsupportedOperationException();
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
}
