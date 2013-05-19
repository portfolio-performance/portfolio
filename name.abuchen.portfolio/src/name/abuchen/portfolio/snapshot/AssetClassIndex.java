package name.abuchen.portfolio.snapshot;

import java.util.List;

import name.abuchen.portfolio.model.Category;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Security.AssetClass;

public class AssetClassIndex extends PerformanceIndex
{
    public static AssetClassIndex forPeriod(Client client, AssetClass assetClass, ReportingPeriod reportInterval,
                    List<Exception> warnings)
    {
        AssetClassIndex index = new AssetClassIndex(client, assetClass, reportInterval);
        index.calculate(warnings);
        return index;
    }

    private AssetClass assetClass;

    private AssetClassIndex(Client client, AssetClass assetClass, ReportingPeriod reportInterval)
    {
        super(client, reportInterval);
        this.assetClass = assetClass;
    }

    private void calculate(List<Exception> warnings)
    {
        Category category = new Category();

        for (Security security : getClient().getSecurities())
        {
            if (security.getType() == assetClass)
                category.addSecurity(security);
        }

        CategoryIndex categoryIndex = CategoryIndex.forPeriod(getClient(), category, getReportInterval(), warnings);

        dates = categoryIndex.getDates();
        totals = categoryIndex.getTotals();
        accumulated = categoryIndex.getAccumulatedPercentage();
        delta = categoryIndex.getDeltaPercentage();
        transferals = categoryIndex.getTransferals();
    }
}
