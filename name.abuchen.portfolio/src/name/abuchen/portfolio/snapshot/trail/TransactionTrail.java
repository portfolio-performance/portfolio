package name.abuchen.portfolio.snapshot.trail;

import java.time.LocalDate;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.Money;

/* package */ class TransactionTrail implements TrailRecord
{
    private final Transaction transaction;

    public TransactionTrail(Transaction t)
    {
        this.transaction = t;
    }

    @Override
    public LocalDate getDate()
    {
        return transaction.getDateTime().toLocalDate();
    }

    @Override
    public String getLabel()
    {
        if (transaction instanceof PortfolioTransaction pt)
            return pt.getType().toString();
        else if (transaction instanceof AccountTransaction at)
            return at.getType().toString();
        else
            return transaction.toString();
    }

    @Override
    public Long getShares()
    {
        return transaction.getShares();
    }

    @Override
    public Money getValue()
    {
        return transaction.getMonetaryAmount();
    }
}
