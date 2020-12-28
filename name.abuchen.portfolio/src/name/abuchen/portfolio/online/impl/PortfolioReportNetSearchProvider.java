package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.SecuritySearchProvider;

public class PortfolioReportNetSearchProvider implements SecuritySearchProvider
{

    @Override
    public String getName()
    {
        return PortfolioReportNet.PROVIDER_NAME;
    }

    @Override
    public List<ResultItem> search(String query, Type type) throws IOException
    {
        return new PortfolioReportNet().search(query, type);
    }

    @Override
    public boolean update(Security existingSecurity) throws IOException
    {
        PortfolioReportNet portfolioReport = new PortfolioReportNet();

        Optional<ResultItem> item = portfolioReport.getUpdatedValues(existingSecurity.getOnlineId());

        if (item.isPresent())
        {
            return PortfolioReportNet.updateWith(existingSecurity, item.get());
        }
        else
        {
//            TODO: Find an alternative to log update info without direct access to Client
//            PortfolioPlugin.info(MessageFormat.format("No data found for ''{0}'' with OnlineId {1}", //$NON-NLS-1$
//                            existingSecurity.getName(), existingSecurity.getOnlineId()));
        }
        return false;
    }

    @Override
    public String getLinkFor(Security existingSecurity)
    {
        return "https://www.portfolio-report.net/securities/" + existingSecurity.getOnlineId(); //$NON-NLS-1$
    }
}
