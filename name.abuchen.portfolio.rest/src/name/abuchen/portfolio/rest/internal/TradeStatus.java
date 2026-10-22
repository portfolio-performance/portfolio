package name.abuchen.portfolio.rest.internal;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import name.abuchen.portfolio.snapshot.trades.Trade;

/**
 * Open/closed trade filter and response discriminator.
 * <p/>
 * Absence selects both values.
 */
public enum TradeStatus
{
    OPEN("open"), //$NON-NLS-1$
    CLOSED("closed"); //$NON-NLS-1$

    private final String wireName;

    TradeStatus(String wireName)
    {
        this.wireName = wireName;
    }

    public String getWireName()
    {
        return wireName;
    }

    public static TradeStatus of(Trade trade)
    {
        return trade.isClosed() ? CLOSED : OPEN;
    }

    /** the wire names in declaration order, for echoing and for error messages */
    public static List<String> wireNames()
    {
        return EnumSet.allOf(TradeStatus.class).stream().map(TradeStatus::getWireName).toList();
    }

    public static TradeStatus ofWireName(String name)
    {
        for (TradeStatus status : values())
        {
            if (status.wireName.equals(name))
                return status;
        }
        return null;
    }

    /** Parses {@code status}; null selects both. */
    public static Set<TradeStatus> parse(String param, List<ApiException.FieldError> errors)
    {
        if (param == null)
            return EnumSet.allOf(TradeStatus.class);

        var selected = EnumSet.noneOf(TradeStatus.class);

        for (var name : param.split(",", -1)) //$NON-NLS-1$
        {
            var status = ofWireName(name.strip());
            if (status == null)
            {
                errors.add(new ApiException.FieldError("status", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                                "status must be a comma-separated list of " + String.join(", ", wireNames()))); //$NON-NLS-1$ //$NON-NLS-2$
                return selected;
            }
            selected.add(status);
        }

        return selected;
    }
}
