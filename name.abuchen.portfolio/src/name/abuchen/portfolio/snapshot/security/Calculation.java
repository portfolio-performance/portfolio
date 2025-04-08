package name.abuchen.portfolio.snapshot.security;

import java.util.List;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.CurrencyConverter;

/* package */abstract class Calculation
{
    private Security security;
    private String termCurrency;

    /**
     * Prepare calculations.
     */
    public void prepare()
    {
    }

    /**
     * Finish up all calculations.
     */
    public void finish(CurrencyConverter converter, List<CalculationLineItem> lineItems)
    {
    }

    /**
     * Gets the underlying {@link Security}.
     * 
     * @return {@link Security} on success, else null
     */
    public Security getSecurity()
    {
        return this.security;
    }

    /**
     * Sets the underlying {@link Security}.
     * 
     * @param security
     *            {@link Security} (can be null)
     */
    public void setSecurity(Security security)
    {
        this.security = security;
    }

    public String getTermCurrency()
    {
        return termCurrency;
    }

    public void setTermCurrency(String termCurrency)
    {
        this.termCurrency = termCurrency;
    }

    public void visit(CurrencyConverter converter, CalculationLineItem.ValuationAtStart t)
    {
    }

    public void visit(CurrencyConverter converter, CalculationLineItem.ValuationAtEnd t)
    {
    }

    public void visit(CurrencyConverter converter, CalculationLineItem.DividendPayment t)
    {
    }

    public void visit(CurrencyConverter converter, CalculationLineItem.TransactionItem item, PortfolioTransaction t)
    {
    }

    public void visit(CurrencyConverter converter, CalculationLineItem.TransactionItem item, AccountTransaction t)
    {
    }

    /* package */ final void visitAll(CurrencyConverter converter, List<CalculationLineItem> lineItems)
    {
        for (CalculationLineItem item : lineItems)
        {
            if (item instanceof CalculationLineItem.ValuationAtStart atStart)
                visit(converter, atStart);
            else if (item instanceof CalculationLineItem.ValuationAtEnd atEnd)
                visit(converter, atEnd);
            else if (item instanceof CalculationLineItem.DividendPayment dividend)
                visit(converter, dividend);
            else if (item instanceof CalculationLineItem.TransactionItem txItem)
            {
                Transaction tx = txItem.getTransaction().orElseThrow(IllegalArgumentException::new);
                if (tx instanceof PortfolioTransaction pt)
                    visit(converter, (CalculationLineItem.TransactionItem) item, pt);
                else if (tx instanceof AccountTransaction at)
                    visit(converter, (CalculationLineItem.TransactionItem) item, at);
                else
                    throw new UnsupportedOperationException();
            }
            else
                throw new UnsupportedOperationException();
        }
    }

    public static <T extends Calculation> T perform(Class<T> type, CurrencyConverter converter, Security security,
                    List<CalculationLineItem> lineItems)
    {
        try
        {
            T thing = type.getDeclaredConstructor().newInstance();
            thing.setSecurity(security);
            thing.setTermCurrency(converter.getTermCurrency());
            thing.prepare();
            thing.visitAll(converter, lineItems);
            thing.finish(converter, lineItems);
            return thing;
        }
        catch (Exception e)
        {
            throw new UnsupportedOperationException(e);
        }
    }
}
