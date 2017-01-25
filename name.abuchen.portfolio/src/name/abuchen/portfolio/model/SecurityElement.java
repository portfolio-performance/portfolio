package name.abuchen.portfolio.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Comparator;

import name.abuchen.portfolio.money.Values;

public class SecurityElement
{
    protected LocalDate date;
    protected long value;

    public SecurityElement()
    {}

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
    @SuppressWarnings("nls")
    public String toString()
    {
        return String.format("%tF: %,10.2f", date, value / Values.Quote.divider());
    }

}
