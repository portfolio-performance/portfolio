package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.QuoteFeed;

public final class ManualQuoteFeed implements QuoteFeed
{
    @Override
    public String getId()
    {
        return QuoteFeed.MANUAL;
    }

    @Override
    public String getName()
    {
        return Messages.QuoteFeedManual;
    }

    @Override
    public void updateLatestQuotes(List<Security> securities, List<Exception> errors)
    {
        for (Security security : securities)
            security.setLatest(null);
    }

    @Override
    public void updateHistoricalQuotes(Security security, List<Exception> errors) throws IOException
    {}

    @Override
    public List<Exchange> getExchanges(Security subject)
    {
        return null;
    }

    @Override
    public List<LatestSecurityPrice> getHistoricalQuotes(Security security, Date start, List<Exception> errors)
    {
        return null;
    }

}
