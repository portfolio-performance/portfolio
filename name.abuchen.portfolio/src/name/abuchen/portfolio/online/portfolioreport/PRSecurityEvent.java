package name.abuchen.portfolio.online.portfolioreport;

import java.time.LocalDate;

import name.abuchen.portfolio.model.SecurityEvent;

public class PRSecurityEvent
{
    public LocalDate date;
    public String type;
    public String details;

    public PRSecurityEvent(SecurityEvent event)
    {
        this.date = event.getDate();
        this.type = event.getType().toString();
        this.details = event.getDetails();
    }

}
