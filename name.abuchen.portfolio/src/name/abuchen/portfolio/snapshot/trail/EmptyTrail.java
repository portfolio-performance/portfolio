package name.abuchen.portfolio.snapshot.trail;

import java.time.LocalDate;

import name.abuchen.portfolio.Messages;
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
        if (trail.isEmpty())
            return this;

        // an empty trail stands for "nothing", i.e. zero: render 0 - trail
        // (in the currency of the trail)
        TrailRecord zero = new DefaultTrail(null, Messages.LabelSum, null,
                        Money.of(trail.getValue().getCurrencyCode(), 0));
        return new ArithmeticTrail(ArithmeticTrail.Operation.SUBTRACTION, Messages.LabelDifference, zero, trail);
    }
}
