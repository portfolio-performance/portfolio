package name.abuchen.portfolio.ui.views.dataseries;

import name.abuchen.portfolio.ui.views.dataseries.DataSeries.ClientDataSeries;

public class ParentObjectClientDataSeries
{
    private Object parentObject;
    private ClientDataSeries clientDataSeries;

    public ParentObjectClientDataSeries(Object parentObject, ClientDataSeries clientDataSeries)
    {
        this.parentObject = parentObject;
        this.clientDataSeries = clientDataSeries;
    }

    public String getId()
    {
        return parentObject.toString() + "-" + clientDataSeries.name(); //$NON-NLS-1$
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
