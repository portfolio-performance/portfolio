package name.abuchen.portfolio.money;

import java.text.MessageFormat;
import java.util.Objects;

/**
 * A monetary amount, such as 'EUR 2,500.50', combining a currency with an
 * amount.
 * <p>
 * Monetary values are stored as long values in minor currency units (e.g.,
 * cents for EUR/USD).
 * <p>
 * To avoid overflow of long integers, particularly when mathematically
 * combining different values such as monetary amounts with prices or shares, it
 * is recommended to manipulate values with the help of the BigDecimal class.
 */
public final class Money implements Comparable<Money>
{
    private final String currencyCode;
    private final long amount;

    private Money(String currencyCode, long amount)
    {
        if (currencyCode == null || currencyCode.isEmpty())
            throw new NullPointerException("currencyCode"); //$NON-NLS-1$

        this.currencyCode = currencyCode;
        this.amount = amount;
    }

    /**
     * Creates a new Money object with the specified currency and amount.
     *
     * @param currencyCode
     *            the ISO 4217 currency code, e.g. 'EUR'
     * @param amount
     *            the amount in minor currency units (e.g., cents for EUR/USD)
     * @return the new Money object
     */
    public static Money of(String currencyCode, long amount)
    {
        return new Money(currencyCode, amount);
    }

    /**
     * Returns the currency code of this Money value.
     *
     * @return the currency code, e.g. 'EUR'
     */
    public String getCurrencyCode()
    {
        return currencyCode;
    }

    /**
     * Returns the amount of money in minor currency units (e.g., cents for
     * EUR/USD).
     *
     * @return the amount as a long integer
     */
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
