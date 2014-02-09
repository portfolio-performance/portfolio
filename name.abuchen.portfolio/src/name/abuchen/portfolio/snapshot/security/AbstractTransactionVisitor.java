package name.abuchen.portfolio.snapshot.security;

import java.util.List;

import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;

/* package */abstract class AbstractTransactionVisitor
{
    public void visit(DividendInitialTransaction t)
    {}

    public void visit(DividendFinalTransaction t)
    {}

    public void visit(DividendTransaction t)
    {}

    public void visit(PortfolioTransaction t)
    {}

    public final void visitAll(List<? extends Transaction> transactions)
    {
        for (Transaction t : transactions)
        {
            if (t instanceof DividendInitialTransaction)
                visit((DividendInitialTransaction) t);
            else if (t instanceof DividendFinalTransaction)
                visit((DividendFinalTransaction) t);
            else if (t instanceof DividendTransaction)
                visit((DividendTransaction) t);
            else if (t instanceof PortfolioTransaction)
                visit((PortfolioTransaction) t);
            else
                throw new UnsupportedOperationException();
        }
    }
}
