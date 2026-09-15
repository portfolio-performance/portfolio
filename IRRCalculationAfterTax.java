package name.abuchen.portfolio.snapshot.security;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Values;

/**
 * Calculates the internal rate of return (IRR) of a security based on the
 * actual, after-tax cash flows, i.e. unlike {@link IRRCalculation} taxes are
 * NOT added back to reconstruct a value before taxes:
 * <ul>
 * <li>dividends are used at their net (post-withholding) amount</li>
 * <li>buys are used at the full amount actually paid (including taxes)</li>
 * <li>sells are used at the full amount actually received (after
 * taxes)</li>
 * <li>standalone tax transactions and tax refunds linked to the security
 * (i.e. not already part of a buy, sell or dividend) are included as real
 * cash flows instead of being ignored</li>
 * </ul>
 * Fees continue to be treated exactly as in {@link IRRCalculation}, i.e.
 * always as real cash flows, because they are never added back in the gross
 * calculation either.
 */
/* package */ class IRRCalculationAfterTax extends IRRCalculation
{
    @Override
    public void visit(CurrencyConverter converter, CalculationLineItem.DividendPayment t)
    {
        dates.add(t.getDateTime().toLocalDate());

        long amount = t.getValue().with(converter.at(t.getDateTime())).getAmount();

        values.add(amount / Values.Amount.divider());
    }

    @Override
    public void visit(CurrencyConverter converter, CalculationLineItem.TransactionItem item, AccountTransaction t)
    {
        switch (t.getType())
        {
            case TAXES:
                // unlike the gross IRR, a standalone tax charge on the
                // security is a real cash outflow and must be counted
                dates.add(t.getDateTime().toLocalDate());
                values.add(-converter.convert(t.getDateTime(), t.getMonetaryAmount()).getAmount()
                                / Values.Amount.divider());
                break;
            case TAX_REFUND:
                dates.add(t.getDateTime().toLocalDate());
                values.add(converter.convert(t.getDateTime(), t.getMonetaryAmount()).getAmount()
                                / Values.Amount.divider());
                break;
            default:
                // FEES / FEES_REFUND are handled identically to the gross
                // calculation; all other types are ignored, same as in the
                // parent class
                super.visit(converter, item, t);
        }
    }

    @Override
    public void visit(CurrencyConverter converter, CalculationLineItem.TransactionItem item, PortfolioTransaction t)
    {
        dates.add(t.getDateTime().toLocalDate());

        long amount = t.getMonetaryAmount(converter).getAmount();

        switch (t.getType())
        {
            case BUY:
            case DELIVERY_INBOUND:
            case TRANSFER_IN:
                values.add(-amount / Values.Amount.divider());
                break;
            case SELL:
            case DELIVERY_OUTBOUND:
            case TRANSFER_OUT:
                values.add(amount / Values.Amount.divider());
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }
}
