package name.abuchen.portfolio.snapshot.trail;

import java.time.LocalDate;

import name.abuchen.portfolio.model.TransactionOwner;
import name.abuchen.portfolio.money.Money;

/* package */ final class EmptyTrail implements TrailRecord
{
    /* package */ static final TrailRecord INSTANCE = new EmptyTrail();

    private EmptyTrail()
    {
    }

    @Override
    public LocalDate getDate()
    {
        return null;
    }

    @Override
    public String getLabel()
    {
        return ""; //$NON-NLS-1$
    }

    @Override
    public Long getShares()
    {
        return null;
    }

    @Override
    public Money getValue()
    {
        return null;
    }

    @Override
    public TrailRecord add(TrailRecord trail)
    {
        return trail;
    }

    @Override
    public TrailRecord fraction(Money money, long numerator, long denominator)
    {
        return this;
    }

    @Override
    public TrailRecord transfer(LocalDate date, TransactionOwner<?> source, TransactionOwner<?> target)
    {
        return this;
    }

    @Override
    public TrailRecord subtract(TrailRecord trail)
    {
        throw new UnsupportedOperationException();
    }
}
