package name.abuchen.portfolio.snapshot;

import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Security.AssetClass;

public class AssetCategory
{
    private final Security.AssetClass assetClass;
    final List<AssetPosition> positions = new ArrayList<AssetPosition>();
    private final int totalAssets;
    protected int valuation = 0;

    /* package */AssetCategory(AssetClass assetClass, int totalAssets)
    {
        this.assetClass = assetClass;
        this.totalAssets = totalAssets;
    }

    public int getValuation()
    {
        return this.valuation;
    }

    public double getShare()
    {
        return (double) this.valuation / (double) this.totalAssets;
    }

    public AssetClass getAssetClass()
    {
        return this.assetClass;
    }

    public List<AssetPosition> getPositions()
    {
        return positions;
    }

    public void addPosition(AssetPosition p)
    {
        this.positions.add(p);
        this.valuation += p.getValuation();
    }
}
