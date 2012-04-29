package name.abuchen.portfolio.online;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;

public interface QuoteFeed
{
    static final String MANUAL = "MANUAL"; //$NON-NLS-1$

    String getId();

    String getName();

    void updateLatestQuotes(List<Security> securities) throws IOException;

    void updateHistoricalQuotes(Security security) throws IOException;

    List<LatestSecurityPrice> getHistoricalQuotes(Security security, Date start) throws IOException;

    List<Exchange> getExchanges(Security subject) throws IOException;
}
