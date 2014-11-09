package name.abuchen.portfolio.online.impl;

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
    public boolean updateLatestQuotes(List<Security> securities, List<Exception> errors)
    {
        boolean isUpdated = false;
        for (Security security : securities)
        {
            boolean isAdded = security.setLatest(null);
            isUpdated = isUpdated || isAdded;
        }
        return isUpdated;
    }

    @Override
    public boolean updateHistoricalQuotes(Security security, List<Exception> errors)
    {
        return false;
    }

    @Override
    public List<Exchange> getExchanges(Security subject, List<Exception> errors)
    {
        return null;
    }

    @Override
    public List<LatestSecurityPrice> getHistoricalQuotes(Security security, Date start, List<Exception> errors)
    {
        return null;
    }

    @Override
    public List<LatestSecurityPrice> getHistoricalQuotes(String response, List<Exception> errors)
    {
        return null;
    }

}
