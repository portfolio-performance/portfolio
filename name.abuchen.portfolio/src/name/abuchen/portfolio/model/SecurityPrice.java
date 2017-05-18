package name.abuchen.portfolio.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Comparator;

public class SecurityPrice extends SecurityElement implements Comparable<SecurityPrice>
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

    public SecurityPrice()
    {}

    public SecurityPrice(LocalDate date, long price)
    {
        super.setDate(date);
        super.setValue(price);
    }

    @Override
    public int compareTo(SecurityPrice o)
    {
        return super.date.compareTo(o.date);
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        long value = getValue();
        result = prime * result + ((date == null) ? 0 : date.hashCode());
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
        SecurityPrice other = (SecurityPrice) obj;
        if (date == null)
        {
            if (other.date != null)
                return false;
        }
        else if (!date.equals(other.date))
            return false;
        if (getValue() != other.getValue())
            return false;
        return true;
    }

}
