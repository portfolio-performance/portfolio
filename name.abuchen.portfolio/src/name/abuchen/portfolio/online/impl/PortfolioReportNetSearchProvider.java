package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.util.List;

import name.abuchen.portfolio.online.SecuritySearchProvider;

public class PortfolioReportNetSearchProvider implements SecuritySearchProvider
{

    @Override
    public String getName()
    {
        return "Portfolio Report Search Provider"; //$NON-NLS-1$
    }

    @Override
    public List<ResultItem> search(String query) throws IOException
    {
        return new PortfolioReportNet().search(query);
    }
}
