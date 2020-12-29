package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.SecuritySearchProvider;
import name.abuchen.portfolio.util.Isin;

public class ETFDataComSearchProvider implements SecuritySearchProvider
{

    @Override
    public String getName()
    {
        return ETFDataCom.PROVIDER_NAME;
    }

    @Override
    public List<ResultItem> search(String query, Type type) throws IOException
    {
        if (!Isin.isValid(query))
            return new ArrayList<>();

        return new ETFDataCom().search(query);
    }

    @Override
    public boolean update(Security existingSecurity) throws IOException
    {
        ETFDataCom etfDataCom = new ETFDataCom();
        ResultItem searchResult = etfDataCom.search(existingSecurity.getOnlineId()).get(0);

        return ETFDataCom.updateWith(existingSecurity, searchResult);
    }

    @Override
    public String getLinkFor(Security existingSecurity)
    {
        return "https://api.etf-data.com/product/" + existingSecurity.getIsin(); //$NON-NLS-1$
    }

    @Override
    public boolean supportsUpdates()
    {
        return true;
    }
}
