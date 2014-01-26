package name.abuchen.portfolio.online;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;

public interface QuoteFeed
{
    String MANUAL = "MANUAL"; //$NON-NLS-1$

    String getId();

    String getName();

    void updateLatestQuotes(List<Security> securities, List<Exception> errors) throws IOException;

    void updateHistoricalQuotes(Security security, List<Exception> errors) throws IOException;

    List<LatestSecurityPrice> getHistoricalQuotes(Security security, Date start, List<Exception> errors)
                    throws IOException;

    List<LatestSecurityPrice> getHistoricalQuotes(String response, List<Exception> errors) throws IOException;

    List<Exchange> getExchanges(Security subject, List<Exception> errors) throws IOException;
}
