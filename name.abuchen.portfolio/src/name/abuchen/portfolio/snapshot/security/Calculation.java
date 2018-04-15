package name.abuchen.portfolio.snapshot.security;

import java.util.List;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.CurrencyConverter;

/* package */abstract class Calculation
{
    private String termCurrency;

    /**
     * Finish up all calculations.
     */
    public void finish()
    {}
    
    public String getTermCurrency()
    {
        return termCurrency;
    }

    public void setTermCurrency(String termCurrency)
    {
        this.termCurrency = termCurrency;
    }

    public void visit(CurrencyConverter converter, DividendInitialTransaction t)
    {}

    public void visit(CurrencyConverter converter, DividendFinalTransaction t)
    {}

    public void visit(CurrencyConverter converter, DividendTransaction t)
    {}

    public void visit(CurrencyConverter converter, PortfolioTransaction t)
    {}

    public void visit(CurrencyConverter converter, AccountTransaction t)
    {}

    public final void visitAll(CurrencyConverter converter, List<? extends Transaction> transactions)
    {
        for (Transaction t : transactions)
        {
            if (t instanceof DividendInitialTransaction)
                visit(converter, (DividendInitialTransaction) t);
            else if (t instanceof DividendFinalTransaction)
                visit(converter, (DividendFinalTransaction) t);
            else if (t instanceof DividendTransaction)
                visit(converter, (DividendTransaction) t);
            else if (t instanceof PortfolioTransaction)
                visit(converter, (PortfolioTransaction) t);
            else if (t instanceof AccountTransaction)
                visit(converter, (AccountTransaction) t);
            else
                throw new UnsupportedOperationException();
        }
    }

    public static <T extends Calculation> T perform(Class<T> type, CurrencyConverter converter,
                    List<? extends Transaction> transactions)
    {
        try
        {
            T thing = type.newInstance();
            thing.setTermCurrency(converter.getTermCurrency());
            thing.visitAll(converter, transactions);
            thing.finish();
            return thing;
        }
        catch (Exception e)
        {
            throw new UnsupportedOperationException(e);
        }
    }
}
