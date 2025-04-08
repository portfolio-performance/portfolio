package name.abuchen.portfolio.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Objects;

import name.abuchen.portfolio.money.Values;

public class SecurityPrice implements Comparable<SecurityPrice>
{
    public static final class ByDate implements Comparator<SecurityPrice>, Serializable
    {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare(SecurityPrice p1, SecurityPrice p2)
        {
            return p1.date.compareTo(p2.date);
        }
    }

    private LocalDate date;
    private long value;

    public SecurityPrice()
    {
    }

    public SecurityPrice(LocalDate date, long price)
    {
        this.value = price;
        this.date = date;
    }

    public LocalDate getDate()
    {
        return date;
    }

    public void setDate(LocalDate date)
    {
        this.date = date;
    }

    public long getValue()
    {
        return value;
    }

    public void setValue(long value)
    {
        this.value = value;
    }

    @Override
    public int compareTo(SecurityPrice o)
    {
        return this.date.compareTo(o.date);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(date, value);
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
        SecurityPrice other = (SecurityPrice) obj;
        return Objects.equals(date, other.date) && value == other.value;
    }

    @Override
    @SuppressWarnings("nls")
    public String toString()
    {
        return String.format("%tF: %,10.2f", date, value / Values.Quote.divider());
    }

}
