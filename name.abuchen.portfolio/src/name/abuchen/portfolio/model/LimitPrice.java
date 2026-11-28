package name.abuchen.portfolio.model;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

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

    public enum TargetMetric
    {
        STATIC_PRICE,
        PURCHASE_PRICE_FIFO,
        AVG_POSITION_VALUE
    }

    private RelationalOperator operator = null;
    private long value;

    /**
     * Null for {@link TargetMetric#STATIC_PRICE} so XStream XML serialization
     * of static limits remains identical to legacy versions.
     */
    private TargetMetric target;
    private Double factor;
    private String groupFilter;

    public LimitPrice(RelationalOperator operator, long value)
    {
        this.operator = Objects.requireNonNull(operator);
        this.value = value;
        this.target = null;
        this.factor = null;
        this.groupFilter = null;
    }

    public LimitPrice(RelationalOperator operator, TargetMetric target, double factor, String groupFilter)
    {
        this.operator = Objects.requireNonNull(operator);
        Objects.requireNonNull(target);

        if (target == TargetMetric.STATIC_PRICE)
            throw new IllegalArgumentException("Use LimitPrice(operator, value) for static limits"); //$NON-NLS-1$

        if (!Double.isFinite(factor) || factor <= 0d)
            throw new IllegalArgumentException("Factor must be positive: " + factor); //$NON-NLS-1$

        var trimmedGroup = groupFilter != null && !groupFilter.isBlank() ? groupFilter.trim() : null;
        if (target != TargetMetric.AVG_POSITION_VALUE && trimmedGroup != null)
            throw new IllegalArgumentException("Group filter is only supported for AVG_POSITION_VALUE"); //$NON-NLS-1$

        this.value = 0L;
        this.target = target;
        this.factor = Double.valueOf(factor);
        this.groupFilter = trimmedGroup;
    }

    public RelationalOperator getRelationalOperator()
    {
        return operator;
    }

    public TargetMetric getTargetMetric()
    {
        return target != null ? target : TargetMetric.STATIC_PRICE;
    }

    public boolean isStatic()
    {
        return getTargetMetric() == TargetMetric.STATIC_PRICE;
    }

    public long getValue()
    {
        return value;
    }

    public double getFactor()
    {
        return factor != null ? factor.doubleValue() : 0d;
    }

    public String getGroupFilter()
    {
        return groupFilter;
    }

    public OptionalLong resolveValue(LimitPriceResolver resolver, Security security)
    {
        if (isStatic())
            return OptionalLong.of(value);

        if (resolver == null || security == null)
            return OptionalLong.empty();

        return resolver.resolveQuote(this, security);
    }

    public static double calculateRelativeDistance(long limitQuote, long currentPrice)
    {
        return ((double) limitQuote / currentPrice - 1);
    }

    public double calculateRelativeDistance(long price)
    {
        return calculateRelativeDistance(value, price);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(operator, getTargetMetric(), value, getFactor(), groupFilter);
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
        if (operator != other.operator)
            return false;
        if (getTargetMetric() != other.getTargetMetric())
            return false;
        if (value != other.value)
            return false;
        if (Double.compare(getFactor(), other.getFactor()) != 0)
            return false;
        return Objects.equals(groupFilter, other.groupFilter);
    }

    @Override
    public int compareTo(LimitPrice other)
    {
        int compare = operator.getOperatorString().compareTo(other.getRelationalOperator().getOperatorString());
        if (compare != 0)
            return compare;

        compare = Integer.compare(getTargetMetric().ordinal(), other.getTargetMetric().ordinal());
        if (compare != 0)
            return compare;

        if (isStatic())
            return Long.compare(value, other.getValue());

        compare = Double.compare(getFactor(), other.getFactor());
        if (compare != 0)
            return compare;

        return Objects.compare(groupFilter, other.getGroupFilter(),
                        Comparator.nullsFirst(String::compareToIgnoreCase));
    }

    @Override
    public String toString()
    {
        if (isStatic())
            return operator.getOperatorString() + " " + Values.Quote.format(value); //$NON-NLS-1$

        var df = new DecimalFormat("0.######", //$NON-NLS-1$
                        DecimalFormatSymbols.getInstance(Locale.getDefault(Locale.Category.FORMAT)));
        var percentage = BigDecimal.valueOf(getFactor()).multiply(BigDecimal.valueOf(100)).stripTrailingZeros();
        var pctText = df.format(percentage) + "%"; //$NON-NLS-1$

        switch (getTargetMetric())
        {
            case PURCHASE_PRICE_FIFO:
                return operator.getOperatorString() + " " + pctText + " PP"; //$NON-NLS-1$ //$NON-NLS-2$
            case AVG_POSITION_VALUE:
                if (groupFilter == null)
                    return operator.getOperatorString() + " " + pctText + " AVG"; //$NON-NLS-1$ //$NON-NLS-2$
                else
                    return operator.getOperatorString() + " " + pctText + " AVG(" + groupFilter + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            case STATIC_PRICE:
            default:
                return operator.getOperatorString() + " " + Values.Quote.format(value); //$NON-NLS-1$
        }
    }

    public static boolean isExceeded(RelationalOperator operator, long limitQuote, SecurityPrice price)
    {
        if (price == null)
            return false;

        switch (operator)
        {
            case GREATER_OR_EQUAL:
                return price.getValue() >= limitQuote;
            case SMALLER_OR_EQUAL:
                return price.getValue() <= limitQuote;
            case GREATER:
                return price.getValue() > limitQuote;
            case SMALLER:
                return price.getValue() < limitQuote;
            default:
                return false;
        }
    }

    public boolean isExceeded(SecurityPrice price)
    {
        if (!isStatic())
            return false;

        return isExceeded(getRelationalOperator(), getValue(), price);
    }

    public boolean isExceeded(SecurityPrice price, LimitPriceResolver resolver, Security security)
    {
        if (price == null)
            return false;

        var resolved = resolveValue(resolver, security);
        return resolved.isPresent() && isExceeded(getRelationalOperator(), resolved.getAsLong(), price);
    }
}
