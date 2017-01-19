package name.abuchen.portfolio.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Comparator;

import name.abuchen.portfolio.money.Values;

public class SecurityElement
{
    private LocalDate time;
    private long value;

    public SecurityElement()
    {}

    public LocalDate getTime()
    {
        return time;
    }

    public void setTime(LocalDate time)
    {
        this.time = time;
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
    @SuppressWarnings("nls")
    public String toString()
    {
        return String.format("%tF: %,10.2f", time, value / Values.Quote.divider());
    }

}
