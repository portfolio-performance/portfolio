package name.abuchen.portfolio.money;

import java.text.MessageFormat;
import java.util.Objects;

public final class Money implements Comparable<Money>
{
    // Special value representing a zero balance. No need to worry about the currency in this case ;).
    public static final Money ZERO = of("", 0); //$NON-NLS-1$
    
    private final String currencyCode;
    private final long amount;

    private Money(String currencyCode, long amount)
    {
        Objects.requireNonNull(currencyCode);

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

    public boolean isGreaterOrEqualThan(Money other)
    {
        Objects.requireNonNull(other);
        if (!other.getCurrencyCode().equals(currencyCode) && amount != 0 && other.amount != 0)
            throw new MonetaryException();
        return amount >= other.getAmount();
    }

    public Money add(Money monetaryAmount)
    {
        if (!monetaryAmount.getCurrencyCode().equals(currencyCode) && amount != 0 && monetaryAmount.amount != 0)
            throw new MonetaryException(MessageFormat.format("Illegal addition: {0} + {1}", //$NON-NLS-1$
                            Values.Money.format(this), Values.Money.format(monetaryAmount)));

        return Money.of(amount == 0 && currencyCode.equals("") ? monetaryAmount.currencyCode : currencyCode, //$NON-NLS-1$
                        amount + monetaryAmount.getAmount());
    }

    public Money subtract(Money monetaryAmount)
    {
        if (!monetaryAmount.getCurrencyCode().equals(currencyCode) && amount != 0 && monetaryAmount.amount != 0)
            throw new MonetaryException(MessageFormat.format("Illegal subtraction: {0} - {1}", //$NON-NLS-1$
                            Values.Money.format(this), Values.Money.format(monetaryAmount)));

        return Money.of(amount == 0 && currencyCode.equals("") ? monetaryAmount.currencyCode : currencyCode, //$NON-NLS-1$
                        amount - monetaryAmount.getAmount());
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
