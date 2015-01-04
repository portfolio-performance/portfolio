package name.abuchen.portfolio.money;

import java.util.Objects;

import name.abuchen.portfolio.model.Values;

public final class Money
{
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
    public String toString()
    {
        return Values.Money.format(this);
    }
}
