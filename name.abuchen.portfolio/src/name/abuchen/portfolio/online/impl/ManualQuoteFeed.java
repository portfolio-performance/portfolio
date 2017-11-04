package name.abuchen.portfolio.online.impl;

import java.time.LocalDate;
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
    public boolean updateLatestQuotes(Security security, List<Exception> errors)
    {
        return security.setLatest(null);
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
    public List<LatestSecurityPrice> getHistoricalQuotes(Security security, LocalDate start, List<Exception> errors)
    {
        return null;
    }

    @Override
    public List<LatestSecurityPrice> getHistoricalQuotes(String response, List<Exception> errors)
    {
        return null;
    }

}
