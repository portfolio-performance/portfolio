package name.abuchen.portfolio.model;

import java.util.Objects;

import name.abuchen.portfolio.money.Values;

public class LimitPrice implements Comparable<LimitPrice>
{
    public enum CompareType
    {
        GREATER_OR_EQUAL(">="), //$NON-NLS-1$
        SMALLER_OR_EQUAL("<="), //$NON-NLS-1$
        GREATER(">"), //$NON-NLS-1$
        SMALLER("<"); //$NON-NLS-1$

        private String str;

        CompareType(String compareString)
        {
            this.str = compareString;
        }

        public String getCompareString()
        {
            return str;
        }
    }

    private CompareType compareType = null;
    private long value;

    public LimitPrice(CompareType type, long value)
    {
        this.compareType = type;
        this.value = value;
    }

    public CompareType getCompareType()
    {
        return compareType;
    }

    public long getValue()
    {
        return value;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(compareType, value);
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

        LimitPrice other = (LimitPrice) obj;
        if (value != other.value)
            return false;
        return Objects.equals(compareType.getCompareString(), other.compareType.getCompareString());
    }

    @Override
    public int compareTo(LimitPrice other)
    {
        int compare = compareType.getCompareString().compareTo(other.getCompareType().getCompareString());
        if (compare != 0)
            return compare;
        return (int) (value - other.getValue());
    }

    @Override
    public String toString()
    {
        return (compareType != null ? compareType.getCompareString() + Values.Quote.format(value) : ""); //$NON-NLS-1$
    }
}
