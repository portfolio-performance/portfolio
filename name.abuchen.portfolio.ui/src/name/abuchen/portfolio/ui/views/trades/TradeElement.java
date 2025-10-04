package name.abuchen.portfolio.ui.views.trades;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.snapshot.trades.Trade;
import name.abuchen.portfolio.snapshot.trades.TradeCategory;

/**
 * Wrapper element for displaying trades in a flat table with taxonomy
 * grouping. Can represent either a category (taxonomy classification) or an
 * individual trade.
 */
public class TradeElement
{
    private final TradeCategory category;
    private final Trade trade;
    private final int sortOrder;

    /**
     * Creates a category element
     */
    public TradeElement(TradeCategory category, int sortOrder)
    {
        this.category = category;
        this.trade = null;
        this.sortOrder = sortOrder;
    }

    /**
     * Creates a trade element
     */
    public TradeElement(Trade trade, int sortOrder)
    {
        this.category = null;
        this.trade = trade;
        this.sortOrder = sortOrder;
    }

    public boolean isCategory()
    {
        return category != null;
    }

    public boolean isTrade()
    {
        return trade != null;
    }

    public TradeCategory getCategory()
    {
        return category;
    }

    public Trade getTrade()
    {
        return trade;
    }

    public Classification getClassification()
    {
        return category != null ? category.getClassification() : null;
    }

    public int getSortOrder()
    {
        return sortOrder;
    }
}
