package name.abuchen.portfolio.money;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;

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

    private Date time;
    private long value;

    public ExchangeRate()
    {
        // empty constructor needed for xstream
    }

    public ExchangeRate(Date time, long value)
    {
        this.time = time;
        this.value = value;
    }

    public Date getTime()
    {
        return time;
    }

    public void setTime(Date time)
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
    public int compareTo(ExchangeRate o)
    {
        return time.compareTo(o.time);
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((time == null) ? 0 : time.hashCode());
        result = prime * result + (int) (value ^ (value >>> 32));
        return result;
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
        if (time == null)
        {
            if (other.time != null)
                return false;
        }
        else if (!time.equals(other.time))
            return false;
        if (value != other.value)
            return false;
        return true;
    }
}
