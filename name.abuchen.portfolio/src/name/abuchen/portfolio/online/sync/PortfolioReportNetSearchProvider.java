package name.abuchen.portfolio.online.sync;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import name.abuchen.portfolio.online.SecuritySearchProvider;
import name.abuchen.portfolio.online.sync.PortfolioReportNet.OnlineItem;

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
        return new PortfolioReportNet().search(query).stream().map(OnlineItem::toResultItem).collect(Collectors.toList());
    }
}
