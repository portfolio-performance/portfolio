package name.abuchen.portfolio.snapshot.security;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;

/* package */final class FeesAndTaxesCalculation extends Calculation
{
    public record FeesAndTaxesResult(Money fees, Money taxes)
    {
    }

    private long fees;
    private long taxes;

    @Override
    public void visit(CurrencyConverter converter, CalculationLineItem.TransactionItem item, PortfolioTransaction t)
    {
        fees += t.getUnitSum(Unit.Type.FEE, converter).getAmount();
        taxes += t.getUnitSum(Unit.Type.TAX, converter).getAmount();
    }

    @Override
    public void visit(CurrencyConverter converter, CalculationLineItem.TransactionItem item, AccountTransaction t)
    {
        switch (t.getType())
        {
            case TAXES -> taxes += converter.convert(t.getDateTime(), t.getMonetaryAmount()).getAmount();
            case TAX_REFUND -> taxes -= converter.convert(t.getDateTime(), t.getMonetaryAmount()).getAmount();
            case FEES -> fees += converter.convert(t.getDateTime(), t.getMonetaryAmount()).getAmount();
            case FEES_REFUND -> fees -= converter.convert(t.getDateTime(), t.getMonetaryAmount()).getAmount();
            default -> {
            }
        }
    }

    @Override
    public void visit(CurrencyConverter converter, CalculationLineItem.DividendPayment payment)
    {
        taxes += payment.getTransaction().orElseThrow(IllegalArgumentException::new)
                        .getUnitSum(Unit.Type.TAX, converter).getAmount();
    }

    public FeesAndTaxesResult getResult()
    {
        return new FeesAndTaxesResult(Money.of(getTermCurrency(), fees), Money.of(getTermCurrency(), taxes));
    }
}
