package name.abuchen.portfolio.money;

import java.math.BigDecimal;
import java.util.Objects;

public final class Quote implements Comparable<Quote>
{
    private final String currencyCode;
    private final long amount;

    private Quote(String currencyCode, long amount)
    {
        Objects.requireNonNull(currencyCode);

        this.currencyCode = currencyCode;
        this.amount = amount;
    }

    public static Quote of(String currencyCode, long amount)
    {
        return new Quote(currencyCode, amount);
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

    public boolean isNotZero()
    {
        return amount != 0L;
    }

    public Money toMoney()
    {
        return Money.of(currencyCode, Math.round(amount / Values.Quote.dividerToMoney()));
    }

    public BigDecimal toBigDecimal()
    {
        return BigDecimal.valueOf(amount / Values.Quote.divider());
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

        Quote other = (Quote) obj;
        if (amount != other.amount)
            return false;
        return Objects.equals(currencyCode, other.currencyCode);
    }

    @Override
    public int compareTo(Quote other)
    {
        int compare = getCurrencyCode().compareTo(other.getCurrencyCode());
        if (compare != 0)
            return compare;
        return Long.compare(getAmount(), other.getAmount());
    }

    @Override
    public String toString()
    {
        return Values.Quote.format(this);
    }

}
