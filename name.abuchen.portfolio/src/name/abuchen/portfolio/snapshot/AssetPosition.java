package name.abuchen.portfolio.snapshot;

import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.Security;

public class AssetPosition implements Comparable<AssetPosition>
{
    private final InvestmentVehicle investmentVehicle;
    private final SecurityPosition position;
    private final long valuation;
    private final long totalAssets;

    /* package */AssetPosition(InvestmentVehicle investmentVehicle, SecurityPosition position, long totalAssets)
    {
        this.position = position;
        this.investmentVehicle = investmentVehicle;
        this.totalAssets = totalAssets;
        this.valuation = position.calculateValue();
    }

    public long getValuation()
    {
        return this.valuation;
    }

    public double getShare()
    {
        return (double) getValuation() / (double) this.totalAssets;
    }

    public String getDescription()
    {
        return investmentVehicle != null ? investmentVehicle.getName() : position.getSecurity().getName();
    }

    public Security getSecurity()
    {
        return position.getSecurity();
    }

    public SecurityPosition getPosition()
    {
        return position;
    }

    public InvestmentVehicle getInvestmentVehicle()
    {
        return investmentVehicle;
    }

    public int compareTo(AssetPosition o)
    {
        return getDescription().compareTo(o.getDescription());
    }
}
