package name.abuchen.portfolio.online.portfolioreport;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import name.abuchen.portfolio.model.SecurityEvent;
import name.abuchen.portfolio.model.SecurityProperty;

public class PRSecurity
{
    private String uuid;
    private String name;
    private String currencyCode;
    private String isin;
    private String wkn;
    private String symbol;
    private boolean active;
    private String note;
    private String securityUuid;
    private Instant updatedAt;
    private List<PRSecurityProperty> properties;
    private List<PRSecurityEvent> events;

    public String calendar;
    public String feed;
    public String feedUrl;
    public String latestFeed;
    public String latestFeedUrl;

    public String getUuid()
    {
        return uuid;
    }

    public void setUuid(String uuid)
    {
        this.uuid = uuid;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getCurrencyCode()
    {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode)
    {
        this.currencyCode = currencyCode;
    }

    public String getIsin()
    {
        return isin;
    }

    public void setIsin(String isin)
    {
        this.isin = isin;
    }

    public String getWkn()
    {
        return wkn;
    }

    public void setWkn(String wkn)
    {
        this.wkn = wkn;
    }

    public String getSymbol()
    {
        return symbol;
    }

    public void setSymbol(String symbol)
    {
        this.symbol = symbol;
    }

    public boolean isActive()
    {
        return active;
    }

    public void setActive(boolean active)
    {
        this.active = active;
    }

    public String getNote()
    {
        return note;
    }

    public void setNote(String note)
    {
        this.note = note;
    }

    public Instant getUpdatedAt()
    {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt)
    {
        this.updatedAt = updatedAt;
    }

    public String getSecurityUuid()
    {
        return securityUuid;
    }

    @SuppressWarnings("nls")
    public void setSecurityUuid(String securityUuid)
    {
        if (securityUuid != null)
            this.securityUuid = securityUuid.replaceFirst(
                            "([0-9a-f]{8})([0-9a-f]{4})([0-9a-f]{4})([0-9a-f]{4})([0-9a-f]+)", "$1-$2-$3-$4-$5");
        else
            this.securityUuid = null;
    }

    public void setProperties(Stream<SecurityProperty> properties)
    {
        this.properties = new ArrayList<PRSecurityProperty>();

        for (SecurityProperty p : properties.collect(Collectors.toList()))
        {
            this.properties.add(new PRSecurityProperty(p));
        }
    }

    public void setEvents(List<SecurityEvent> events)
    {
        this.events = new ArrayList<>();

        for (SecurityEvent event : events)
        {
            this.events.add(new PRSecurityEvent(event));
        }
    }

}
