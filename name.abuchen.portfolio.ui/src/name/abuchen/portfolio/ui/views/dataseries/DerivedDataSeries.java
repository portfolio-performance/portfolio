package name.abuchen.portfolio.ui.views.dataseries;

import name.abuchen.portfolio.ui.views.dataseries.DataSeries.ClientDataSeries;

public class DerivedDataSeries
{
    private final DataSeries baseDataSeries;
    private final ClientDataSeries aspect;

    public DerivedDataSeries(DataSeries baseDataSeries, ClientDataSeries aspect)
    {
        this.baseDataSeries = baseDataSeries;
        this.aspect = aspect;
    }

    public String getUUID()
    {
        // eventually, the UUID is constructed as
        // Derived-(aspect)-(underlying data series)
        return aspect.name() + "-" + baseDataSeries.getUUID(); //$NON-NLS-1$
    }

    public DataSeries getBaseDataSeries()
    {
        return baseDataSeries;
    }

    public ClientDataSeries getAspect()
    {
        return aspect;
    }
}
