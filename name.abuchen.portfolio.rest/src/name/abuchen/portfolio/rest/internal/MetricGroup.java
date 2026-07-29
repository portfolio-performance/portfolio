package name.abuchen.portfolio.rest.internal;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The metric groups of the per-instrument performance resource. Selection is by
 * group rather than by individual field because the cost boundary is a group
 * boundary: {@link #TIME_WEIGHTED} and {@link #RISK} share one
 * {@code PerformanceIndex} - a full daily series per instrument, which dominates
 * the cost of the request - while every other group comes from a cheap linear
 * pass over the line items.
 * <p/>
 * A closed enum also stays documentable in OpenAPI, which a free-form field mask
 * would not: every property keeps its declared type and meaning whether it is
 * present or absent.
 */
public enum MetricGroup
{
    VALUATION("valuation"), //$NON-NLS-1$
    GAINS("gains"), //$NON-NLS-1$
    INCOME("income"), //$NON-NLS-1$
    EXPENSES("expenses"), //$NON-NLS-1$
    MONEY_WEIGHTED("moneyWeighted"), //$NON-NLS-1$
    TIME_WEIGHTED("timeWeighted"), //$NON-NLS-1$
    RISK("risk"); //$NON-NLS-1$

    private final String wireName;

    MetricGroup(String wireName)
    {
        this.wireName = wireName;
    }

    public String getWireName()
    {
        return wireName;
    }

    /** the wire names in declaration order, for echoing and for error messages */
    public static List<String> wireNames()
    {
        return EnumSet.allOf(MetricGroup.class).stream().map(MetricGroup::getWireName).toList();
    }

    public static MetricGroup ofWireName(String name)
    {
        for (MetricGroup group : values())
        {
            if (group.wireName.equals(name))
                return group;
        }
        return null;
    }

    /**
     * Parses the comma-separated {@code metrics} parameter. A null parameter
     * selects every group: the naive call is complete and predictable, and a
     * client that cares about latency opts down.
     */
    public static Set<MetricGroup> parse(String param, List<ApiException.FieldError> errors)
    {
        if (param == null)
            return EnumSet.allOf(MetricGroup.class);

        var selected = EnumSet.noneOf(MetricGroup.class);

        for (String name : param.split(",", -1)) //$NON-NLS-1$
        {
            var group = ofWireName(name.strip());
            if (group == null)
            {
                errors.add(new ApiException.FieldError("metrics", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                                "metrics must be a comma-separated list of " + String.join(", ", wireNames()))); //$NON-NLS-1$ //$NON-NLS-2$
                return selected;
            }
            selected.add(group);
        }

        return selected;
    }
}
