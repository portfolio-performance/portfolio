package name.abuchen.portfolio.money;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Objects;

public class ExchangeRate implements Comparable<ExchangeRate>
{
    public static final class ByDate implements Comparator<ExchangeRate>, Serializable
    {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare(ExchangeRate p1, ExchangeRate p2)
        {
            return p1.time.compareTo(p2.time);
        }
    }

    private LocalDate time;
    private BigDecimal value;

    public ExchangeRate()
    {
        // empty constructor needed for xstream
    }

    public ExchangeRate(LocalDateTime time, BigDecimal value)
    {
        this(Objects.requireNonNull(time).toLocalDate(), value);
    }
    
    public ExchangeRate(LocalDate time, BigDecimal value)
    {
        Objects.requireNonNull(time);
        Objects.requireNonNull(value);
        this.time = time;
        this.value = value;
    }

    public LocalDate getTime()
    {
        return time;
    }

    public void setTime(LocalDate time)
    {
        Objects.requireNonNull(time);
        this.time = time;
    }

    public BigDecimal getValue()
    {
        return value;
    }

    public void setValue(BigDecimal value)
    {
        Objects.requireNonNull(value);
        this.value = value;
    }

    public ExchangeRate inverse()
    {
        BigDecimal inverse = BigDecimal.ONE.divide(value, 10, BigDecimal.ROUND_HALF_DOWN);
        return new ExchangeRate(time, inverse);
    }

    @Override
    public int compareTo(ExchangeRate o)
    {
        return time.compareTo(o.time);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(time, value);
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

        ExchangeRate other = (ExchangeRate) obj;
        if (!time.equals(other.time))
            return false;
        return value.equals(other.value);
    }

    @Override
    public String toString()
    {
        return String.format("%tF %,.10f", time, value); //$NON-NLS-1$
    }

    public static BigDecimal inverse(BigDecimal rate)
    {
        return BigDecimal.ONE.divide(rate, 10, BigDecimal.ROUND_HALF_DOWN);
    }
}
