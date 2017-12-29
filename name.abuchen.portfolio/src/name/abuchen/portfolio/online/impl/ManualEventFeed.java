package name.abuchen.portfolio.online.impl;

import java.time.LocalDate;
import java.util.List;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.SecurityElement;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.EventFeed;

public final class ManualEventFeed extends EventFeed
{
    
    public static final String ID = MANUAL; //$NON-NLS-1$    
    
    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return Messages.EventFeedManual;
    }

    @Override
    public boolean updateLatest(Security security, List<Exception> errors)
    {
        return false;
    }

    @Override
    public boolean updateHistorical(Security security, List<Exception> errors)
    {
        return false;
    }

    @Override
    public List<Exchange> getExchanges(Security subject, List<Exception> errors)
    {
        return null;
    }

    @Override
    public List<SecurityElement> get(Security security, LocalDate start, List<Exception> errors)
    {
        return null;
    }

    @Override
    public List<SecurityElement> get(String response, List<Exception> errors)
    {
        return null;
    }

}
