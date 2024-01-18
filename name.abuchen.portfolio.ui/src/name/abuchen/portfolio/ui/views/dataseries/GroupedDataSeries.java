package name.abuchen.portfolio.ui.views.dataseries;

import java.util.List;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.ClientFilterMenu;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries.ClientDataSeries;
import name.abuchen.portfolio.util.Interval;

public class GroupedDataSeries
{
    private Object parentObject;
    private ClientDataSeries clientDataSeries;
    private Boolean isPortfolioPlusReferenceAccount = false;

    public GroupedDataSeries(Object parentObject, ClientDataSeries clientDataSeries)
    {
        this.parentObject = parentObject;
        this.clientDataSeries = clientDataSeries;
    }

    public void setIsPortfolioPlusReferenceAccount(Boolean value)
    {
        this.isPortfolioPlusReferenceAccount = value;
    }
    
    public PerformanceIndex getPerformanceIndexMethod(Client client, CurrencyConverter converter, Interval reportInterval, List<Exception> warnings)
    {
        if (parentObject instanceof Portfolio portfolio && this.isPortfolioPlusReferenceAccount)
            return PerformanceIndex.forPortfolioPlusAccount(client, converter, portfolio, reportInterval, warnings);
        
        if (parentObject instanceof Portfolio portfolio && !this.isPortfolioPlusReferenceAccount)
            return PerformanceIndex.forPortfolio(client, converter, portfolio, reportInterval, warnings);

        if (parentObject instanceof Account account)
            return PerformanceIndex.forAccount(client,  converter, account, reportInterval, warnings);

        if (parentObject instanceof Classification classification)
            return PerformanceIndex.forClassification(client, converter, classification, reportInterval, warnings);

        if (parentObject instanceof ClientFilterMenu.Item item)
            return PerformanceIndex.forClient(item.getFilter().filter(client), converter, reportInterval, warnings);

        throw new IllegalArgumentException(parentObject.getClass().getTypeName());
    }

    public String getTopLevelLabel()
    {
        if (parentObject instanceof Portfolio)
            return Messages.LabelSecurities;

        if (parentObject instanceof Account)
            return Messages.LabelAccounts;

        if (parentObject instanceof Classification)
            return Messages.LabelTaxonomies;

        if (parentObject instanceof Client)
            return Messages.PerformanceChartLabelEntirePortfolio;

        if (parentObject instanceof ClientFilterMenu.Item)
            return Messages.LabelClientFilter;

        throw new IllegalArgumentException("Unable to determine parent object type."); //$NON-NLS-1$
    }

    public String getId()
    {
        return parentObject.getClass().getTypeName() + " + " + parentObject.toString() + "-" //$NON-NLS-1$ //$NON-NLS-2$
                        + isPortfolioPlusReferenceAccount + "-" + clientDataSeries.name(); //$NON-NLS-1$
    }

    public Object getParentObject()
    {
        return parentObject;
    }

    public ClientDataSeries getClientDataSeries()
    {
        return clientDataSeries;
    }
}
