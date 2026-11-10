package name.abuchen.portfolio.snapshot.trades;

import name.abuchen.portfolio.Messages;

/**
 * Determines how the {@link TradeCollector} groups the transactions of a
 * security into trades.
 */
public enum TradeGrouping
{
    /**
     * All acquisitions that are open at the same time - or that are closed by
     * the same sale - are combined into one trade.
     */
    COMBINED(Messages.LabelTradeGroupingCombined),

    /**
     * Every acquisition creates its own trade. A trade spans at most one
     * opening lot and at most one closing transaction.
     */
    PER_LOT(Messages.LabelTradeGroupingPerLot);

    private final String label;

    private TradeGrouping(String label)
    {
        this.label = label;
    }

    @Override
    public String toString()
    {
        return label;
    }

    public String getLabel()
    {
        return label;
    }

    public static TradeGrouping fromString(String name)
    {
        if (name == null || name.isBlank())
            return COMBINED;

        for (TradeGrouping grouping : values())
        {
            if (grouping.name().equals(name))
                return grouping;
        }

        return COMBINED;
    }
}
