package name.abuchen.portfolio.snapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Security.AssetClass;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Values;

public class GroupByAssetClass
{
    private final long valuation;
    private final List<AssetCategory> categories;
    private final Map<Security.AssetClass, AssetCategory> class2category;

    /* package */GroupByAssetClass(ClientSnapshot snapshot)
    {
        this(snapshot.getAssets());

        // cash
        AssetCategory cash = class2category.get(Security.AssetClass.CASH);
        for (AccountSnapshot a : snapshot.getAccounts())
        {
            SecurityPosition sp = new SecurityPosition(null);
            sp.setShares(Values.Share.factor());
            sp.setPrice(new SecurityPrice(snapshot.getTime(), a.getFunds()));
            AssetPosition ap = new AssetPosition(sp, a.getAccount(), valuation);
            cash.addPosition(ap);
        }

        // portfolio
        if (snapshot.getJointPortfolio() != null)
        {
            for (SecurityPosition pos : snapshot.getJointPortfolio().getPositions())
            {
                AssetCategory cat = class2category.get(pos.getSecurity().getType());
                cat.addPosition(new AssetPosition(pos, valuation));
            }
        }

        pack();
    }

    /* package */public GroupByAssetClass(PortfolioSnapshot snapshot)
    {
        this(snapshot.getValue());

        for (SecurityPosition pos : snapshot.getPositions())
        {
            AssetCategory cat = class2category.get(pos.getSecurity().getType());
            cat.addPosition(new AssetPosition(pos, valuation));
        }

        pack();
    }

    private GroupByAssetClass(long valuation)
    {
        this.valuation = valuation;

        categories = new ArrayList<AssetCategory>();
        class2category = new HashMap<Security.AssetClass, AssetCategory>();

        for (Security.AssetClass ac : Security.AssetClass.values())
        {
            AssetCategory category = new AssetCategory(ac, valuation);
            categories.add(category);
            class2category.put(ac, category);
        }
    }

    private void pack()
    {
        Iterator<AssetCategory> iter = categories.iterator();
        while (iter.hasNext())
        {
            AssetCategory c = iter.next();
            if (c.getValuation() == 0)
            {
                class2category.remove(c.getAssetClass());
                iter.remove();
            }
        }

        for (AssetCategory cat : categories)
            Collections.sort(cat.getPositions());
    }

    public long getValuation()
    {
        return valuation;
    }

    public List<AssetCategory> asList()
    {
        return categories;
    }

    public AssetCategory byClass(AssetClass assetClass)
    {
        return class2category.get(assetClass);
    }
}
