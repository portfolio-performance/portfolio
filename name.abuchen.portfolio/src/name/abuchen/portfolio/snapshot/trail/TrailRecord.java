package name.abuchen.portfolio.snapshot.trail;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionOwner;
import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.SecurityPosition;

public interface TrailRecord
{
    public static TrailRecord ofPosition(LocalDate time, Portfolio portfolio, SecurityPosition position)
    {
        return new DefaultTrail(time, Messages.LabelStatementOfAssets + " " + portfolio.getName(), //$NON-NLS-1$
                        position.getShares(), position.calculateValue());
    }

    public static TrailRecord ofTransaction(Transaction t)
    {
        return new TransactionTrail(t);
    }

    public static TrailRecord empty()
    {
        return EmptyTrail.INSTANCE;
    }

    public static TrailRecord of(List<TrailRecord> trails)
    {
        if (trails.isEmpty())
            return empty();
        if (trails.size() == 1)
            return trails.get(0);
        else
            return new ArithmeticTrail(ArithmeticTrail.Operation.ADDITION, Messages.LabelSum,
                            trails.toArray(new TrailRecord[0]));
    }

    LocalDate getDate();

    String getLabel();

    Long getShares();

    Money getValue();

    default boolean isEmpty()
    {
        return getValue() == null;
    }

    default List<TrailRecord> getInputs()
    {
        return Collections.emptyList();
    }

    default TrailRecord add(TrailRecord trail)
    {
        if (trail instanceof EmptyTrail)
            return this;
        return new ArithmeticTrail(ArithmeticTrail.Operation.ADDITION, Messages.LabelSum, this, trail);
    }

    default TrailRecord subtract(TrailRecord trail)
    {
        if (trail instanceof EmptyTrail)
            return this;
        return new ArithmeticTrail(ArithmeticTrail.Operation.SUBTRACTION, Messages.LabelDifference, this, trail);
    }

    default TrailRecord fraction(Money value, long numerator, long denominator)
    {
        if (numerator == denominator || numerator == 0L)
            return this;
        return new DefaultTrail(null, MessageFormat.format(Messages.LabelTrailXofYShares,
                        Values.Share.format(numerator), Values.Share.format(denominator)), numerator, value, this);
    }

    default TrailRecord convert(Money value, ExchangeRate rate)
    {
        return new DefaultTrail(rate.getTime(),
                        Messages.CSVColumn_ExchangeRate + ": " + Values.ExchangeRate.format(rate.getValue()), //$NON-NLS-1$
                        this.getShares(), value, this);
    }

    default TrailRecord transfer(LocalDate date, TransactionOwner<?> source, TransactionOwner<?> target)
    {
        return new DefaultTrail(date,
                        MessageFormat.format(Messages.LabelTrailTransferFromXtoY, source.toString(), target.toString()),
                        this.getShares(), this.getValue(), this);
    }

    default TrailRecord asGrossValue(Money grossValue)
    {
        if (grossValue.equals(getValue()))
            return this;
        else
            return new DefaultTrail(null, Messages.LabelTrailWithoutTaxesAndFees, this.getShares(), grossValue, this);
    }
}
