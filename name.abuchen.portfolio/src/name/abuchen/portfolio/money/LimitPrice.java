package name.abuchen.portfolio.money;

import java.math.BigDecimal;
import java.util.Objects;

public class LimitPrice implements Comparable<LimitPrice>
{
    public enum CompareType
    {
        GREATER(">"), //$NON-NLS-1$
        GREATER_OR_EQUAL(">="), //$NON-NLS-1$
        SMALLER("<"), //$NON-NLS-1$
        SMALLER_OR_EQUAL("<="); //$NON-NLS-1$
        
        private String str;
        
        CompareType(String compareString) {
            this.str = compareString;
        }
     
        public String getCompareString() {
            return str;
        }
    }
    
    private CompareType compareType;
    private BigDecimal limitPrice;
    
    public CompareType getCompareType()
    {
        return compareType;
    }

    public BigDecimal getLimitPrice()
    {
        return limitPrice;
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(compareType, limitPrice);
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
        if (limitPrice != other.limitPrice)
            return false;
        return Objects.equals(compareType.getCompareString(), other.compareType.getCompareString());
    }

    @Override
    public int compareTo(LimitPrice other)
    {
        int compare = compareType.getCompareString().compareTo(other.getCompareType().getCompareString());
        if (compare != 0)
            return compare;
        return limitPrice.compareTo(other.getLimitPrice());
    }

    @Override
    public String toString()
    {
        return compareType.getCompareString() + limitPrice.toString();
    }
}
