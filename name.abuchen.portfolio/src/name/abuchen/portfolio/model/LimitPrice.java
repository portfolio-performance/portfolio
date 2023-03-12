package name.abuchen.portfolio.model;

import java.util.Objects;
import java.util.Optional;

import name.abuchen.portfolio.money.Values;

public class LimitPrice implements Comparable<LimitPrice>
{
    public enum RelationalOperator
    {
        GREATER_OR_EQUAL(">="), //$NON-NLS-1$
        SMALLER_OR_EQUAL("<="), //$NON-NLS-1$
        GREATER(">"), //$NON-NLS-1$
        SMALLER("<"); //$NON-NLS-1$

        private String operatorString;

        RelationalOperator(String operator)
        {
            this.operatorString = operator;
        }

        public String getOperatorString()
        {
            return operatorString;
        }

        public boolean isGreater()
        {
            return this == GREATER_OR_EQUAL || this == GREATER;
        }

        public static Optional<RelationalOperator> findByOperator(String op)
        {
            for (RelationalOperator t : RelationalOperator.values())
            {
                if (t.getOperatorString().equals(op))
                    return Optional.of(t);
            }

            return Optional.empty();
        }
    }

    private RelationalOperator operator = null;
    private long value;

    public LimitPrice(RelationalOperator operator, long value)
    {
        this.operator = Objects.requireNonNull(operator);
        this.value = value;
    }

    public RelationalOperator getRelationalOperator()
    {
        return operator;
    }

    public long getValue()
    {
        return value;
    }
    
    public double calculateRelativeDistance(long price)
    {
        return ((double) value / price - 1);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(operator, value);
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
        return operator == other.operator;
    }

    @Override
    public int compareTo(LimitPrice other)
    {
        int compare = operator.getOperatorString().compareTo(other.getRelationalOperator().getOperatorString());
        if (compare != 0)
            return compare;
        return Long.compare(value, other.getValue());
    }

    @Override
    public String toString()
    {
        return operator.getOperatorString() + " " + Values.Quote.format(value); //$NON-NLS-1$
    }

    public boolean isExceeded(SecurityPrice price)
    {
        switch (getRelationalOperator())
        {
            case GREATER_OR_EQUAL:
                return price.getValue() >= getValue();
            case SMALLER_OR_EQUAL:
                return price.getValue() <= getValue();
            case GREATER:
                return price.getValue() > getValue();
            case SMALLER:
                return price.getValue() < getValue();
            default:
                return false;
        }
    }
}
