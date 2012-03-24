package name.abuchen.portfolio.online;

import java.io.IOException;
import java.util.List;

import name.abuchen.portfolio.model.Security;

public interface QuoteFeed
{
    String getId();

    String getName();

    void updateLatestQuote(List<Security> securities) throws IOException;

    void updateHistoricalQuotes(Security security) throws IOException;
}
