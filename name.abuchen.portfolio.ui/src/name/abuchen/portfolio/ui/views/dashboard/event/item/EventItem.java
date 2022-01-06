package name.abuchen.portfolio.ui.views.dashboard.event.item;

import java.time.LocalDate;
import java.util.Objects;

public abstract class EventItem implements Comparable<EventItem>
{
    
    private final EventType type;
    private final LocalDate date;
    
    public EventItem(EventType type, LocalDate date)
    {
        this.type = type;
        this.date = date;
    }

    public EventType getType()
    {
        return type;
    }

    public LocalDate getDate()
    {
        return date;
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(date, type);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        
        EventItem other = (EventItem) obj;
        return Objects.equals(type, other.type) && Objects.equals(date, other.date);
    }

    @Override
    public String toString()
    {
        return "EventItem [type=" + type + ", date=" + date + "]";  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    @Override
    public int compareTo(EventItem o)
    {
        int result = getDate().compareTo(o.getDate());
        
        if (result == 0)
            result = getType().compareTo(o.getType());
        
        return result;
    }
    
}