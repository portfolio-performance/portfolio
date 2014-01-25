package name.abuchen.portfolio.snapshot;

import java.util.Comparator;

import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.Security;

public class AssetPosition
{
    public static final class ByDescription implements Comparator<AssetPosition>
    {
        @Override
        public int compare(AssetPosition p1, AssetPosition p2)
        {
            return p1.getDescription().compareTo(p2.getDescription());
        }
    }

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

    public long getFIFOPurchaseValue()
    {
        return position.getFIFOPurchaseValue();
    }

    public long getProfitLoss()
    {
        return position.getProfitLoss();
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
}
