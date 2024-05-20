package name.abuchen.portfolio.money;

import java.text.MessageFormat;
import java.util.Objects;

public final class Money implements Comparable<Money>
{
    private final String currencyCode;
    private final long amount;

    private Money(String currencyCode, long amount)
    {
        if (currencyCode == null || currencyCode.isEmpty())
            throw new NullPointerException();

        this.currencyCode = currencyCode;
        this.amount = amount;
    }

    public static Money of(String currencyCode, long amount)
    {
        return new Money(currencyCode, amount);
    }

    public String getCurrencyCode()
    {
        return currencyCode;
    }

    public long getAmount()
    {
        return amount;
    }

    public boolean isZero()
    {
        return amount == 0L;
    }

    public boolean isPositive()
    {
        return amount > 0L;
    }

    public boolean isNegative()
    {
        return amount < 0L;
    }

    public boolean isGreaterThan(Money other)
    {
        Objects.requireNonNull(other);
        if (!other.getCurrencyCode().equals(currencyCode))
            throw new MonetaryException();
        return amount > other.getAmount();
    }

    public boolean isGreaterOrEqualTo(Money other)
    {
        Objects.requireNonNull(other);
        if (!other.getCurrencyCode().equals(currencyCode))
            throw new MonetaryException();
        return amount >= other.getAmount();
    }

    public boolean isLessThan(Money other)
    {
        Objects.requireNonNull(other);
        if (!other.getCurrencyCode().equals(currencyCode))
            throw new MonetaryException();
        return amount < other.getAmount();
    }

    public boolean isLessOrEqualTo(Money other)
    {
        Objects.requireNonNull(other);
        if (!other.getCurrencyCode().equals(currencyCode))
            throw new MonetaryException();
        return amount <= other.getAmount();
    }

    public Money add(Money monetaryAmount)
    {
        if (!monetaryAmount.getCurrencyCode().equals(currencyCode))
            throw new MonetaryException(MessageFormat.format("Illegal addition: {0} + {1}", //$NON-NLS-1$
                            Values.Money.format(this), Values.Money.format(monetaryAmount)));

        return Money.of(currencyCode, amount + monetaryAmount.getAmount());
    }

    public Money subtract(Money monetaryAmount)
    {
        if (!monetaryAmount.getCurrencyCode().equals(currencyCode))
            throw new MonetaryException(MessageFormat.format("Illegal subtraction: {0} - {1}", //$NON-NLS-1$
                            Values.Money.format(this), Values.Money.format(monetaryAmount)));

        return Money.of(currencyCode, amount - monetaryAmount.getAmount());
    }

    public Money divide(long divisor)
    {
        return Money.of(currencyCode, Math.round(amount / (double) divisor));
    }

    public Money multiply(long multiplicand)
    {
        return Money.of(currencyCode, amount * multiplicand);
    }

    public Money multiplyAndRound(double multiplicand)
    {
        return Money.of(currencyCode, Math.round(amount * multiplicand));
    }

    public Money absolute()
    {
        return Money.of(currencyCode, Math.abs(amount));
    }

    public Money with(MonetaryOperator operator)
    {
        return operator.apply(this);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(currencyCode, amount);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        Money other = (Money) obj;
        if (amount != other.amount)
            return false;
        return Objects.equals(currencyCode, other.currencyCode);
    }

    @Override
    public int compareTo(Money other)
    {
        int compare = getCurrencyCode().compareTo(other.getCurrencyCode());
        if (compare != 0)
            return compare;
        return Long.compare(getAmount(), other.getAmount());
    }

    @Override
    public String toString()
    {
        return Values.Money.format(this);
    }
}
