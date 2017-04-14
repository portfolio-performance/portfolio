package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.SecurityElement;
import name.abuchen.portfolio.model.SecurityEvent;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.EventFeed;

public class HTMLTableEventFeed extends EventFeed
{

    public static final String ID = HTML; //$NON-NLS-1$

    private final HTMLTableEventParser Parser = new HTMLTableEventParser();

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return Messages.LabelHTMLTable;
    }

    @Override
    public boolean update(List<Security> securities, List<Exception> errors)
    {
        return false;
    }

    @Override
    public boolean update(Security security, List<Exception> errors)
    {
        List<SecurityEvent> events = internalGetEvents(security, security.getEventFeedURL(), errors);

        boolean isUpdated = false;
        for (SecurityEvent event: events)
        {
            boolean isAdded = security.addEvent(new SecurityEvent(event.getDate(), event.getType(), event.getDetails()));
            isUpdated = isUpdated || isAdded;
        }

        return isUpdated;
    }

    @Override
    public List<SecurityElement> get(Security security, LocalDate start, List<Exception> errors)
    {
        return SecurityElement.cast2ElementList(internalGetEvents(security, security.getEventFeedURL(), errors));
    }

    private List<SecurityEvent> internalGetEvents(Security security, String feedURL, List<Exception> errors)
    {
        if (feedURL == null || feedURL.length() == 0)
        {
            errors.add(new IOException(MessageFormat.format(Messages.MsgMissingEventFeedURL, security.getName())));
            return Collections.emptyList();
        }

        List<SecurityElement> elements = Parser.parseFromURL(feedURL, errors);
        List<SecurityEvent> events = new ArrayList<>();

        for (SecurityElement e : elements)
        {
            if (e instanceof SecurityEvent) 
                    events.add((SecurityEvent) e); // need to cast each object specifically
        }

        return events; 
    }

    @Override
    public List<SecurityElement> get(String response, List<Exception> errors)
    {
        return Parser.parseFromHTML(response, errors);
    }

    @Override
    public List<Exchange> getExchanges(Security subject, List<Exception> errors)
    {
        return Collections.emptyList();
    }

}
