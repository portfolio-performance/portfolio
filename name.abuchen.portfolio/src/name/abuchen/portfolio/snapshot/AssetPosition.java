package name.abuchen.portfolio.snapshot;

import name.abuchen.portfolio.model.Security;

public class AssetPosition implements Comparable<AssetPosition>
{
    private final SecurityPosition position;
    private final int valuation;
    private final int totalAssets;
    private final String description;

    /* package */AssetPosition(SecurityPosition position, int totalAssets)
    {
        this.position = position;
        this.totalAssets = totalAssets;
        this.valuation = position.calculateValue();
        this.description = position.getSecurity().getName();
    }

    /* package */AssetPosition(SecurityPosition position, String description, int totalAssets)
    {
        this.position = position;
        this.totalAssets = totalAssets;
        this.valuation = position.calculateValue();
        this.description = description;
    }

    public int getValuation()
    {
        return this.valuation;
    }

    public double getShare()
    {
        return (double) getValuation() / (double) this.totalAssets;
    }

    public String getDescription()
    {
        return description;
    }

    public Security getSecurity()
    {
        return position.getSecurity();
    }

    public SecurityPosition getPosition()
    {
        return position;
    }

    public int compareTo(AssetPosition o)
    {
        return description.compareTo(o.description);
    }
}
