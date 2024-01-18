package name.abuchen.portfolio.ui.views.dataseries;

import java.util.List;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.ui.util.ClientFilterMenu;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries.ClientDataSeries;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries.ClientDataSeriesType;
import name.abuchen.portfolio.util.Interval;

public class GroupedDataSeries
{
    private Object parentObject;
    private ClientDataSeriesType clientDataSeriesType;
    private DataSeries.Type parentObjectDataSeriesType;
    private Boolean isPortfolioPlusReferenceAccount = false;

    public GroupedDataSeries(Object parentObject, ClientDataSeriesType clientDataSeriesType,
                    DataSeries.Type parentObjectDataSeriesType)
    {
        this.parentObject = parentObject;
        this.clientDataSeriesType = clientDataSeriesType;
        this.parentObjectDataSeriesType = parentObjectDataSeriesType;
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
            return PerformanceIndex.forAccount(client, converter, account, reportInterval, warnings);

        if (parentObject instanceof Classification classification)
            return PerformanceIndex.forClassification(client, converter, classification, reportInterval, warnings);

        if (parentObject instanceof ClientFilterMenu.Item item)
            return PerformanceIndex.forClient(item.getFilter().filter(client), converter, reportInterval, warnings);

        if (parentObject instanceof Security security)
            return PerformanceIndex.forInvestment(client, converter, security, reportInterval, warnings);

        throw new IllegalArgumentException(parentObject.getClass().getTypeName());
    }

    public String getId()
    {
        return this.parentObjectDataSeriesType.buildUUID(parentObject).concat(this.clientDataSeriesType.name());
    }

    public Object getParentObject()
    {
        return parentObject;
    }

    public DataSeries.Type getParentObjectType()
    {
        return parentObjectDataSeriesType;
    }

    public ClientDataSeries getClientDataSeries()
    {
        return clientDataSeriesType.getClientDataSeries();
    }

    public String getClientDataSeriesLabel()
    {
        return clientDataSeriesType.toString();
    }
}
