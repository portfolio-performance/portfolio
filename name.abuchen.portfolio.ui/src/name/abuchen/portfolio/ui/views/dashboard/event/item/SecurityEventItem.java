package name.abuchen.portfolio.ui.views.dashboard.event.item;

import java.time.LocalDate;
import java.util.Objects;

import name.abuchen.portfolio.model.Security;

public class SecurityEventItem extends EventItem
{

    private final Security security;
    private final String message;
    
    public SecurityEventItem(EventType type, LocalDate date, String message, Security security)
    {
        super(type, date);
        
        this.message = message;
        this.security = security;
    }

    public String getMessage()
    {
        return message;
    }

    public Security getSecurity()
    {
        return security;
    }

    @Override
    public int hashCode()
    {
        int result = super.hashCode();
        result = 31 * result + Objects.hash(message, security);
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        
        SecurityEventItem other = (SecurityEventItem) obj;
        return Objects.equals(message, other.message) && Objects.equals(security, other.security);
    }

    @Override
    public String toString()
    {
        return "SecurityEventItem [type=" + getType() +         //$NON-NLS-1$
                        ", date=" + getDate() +                 //$NON-NLS-1$
                        ", security=" + security.getUUID() +    //$NON-NLS-1$
                        ", message=" + getMessage() + "]";      //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public int compareTo(EventItem o)
    {
        int result = super.compareTo(o);
        
        if (result == 0 && o instanceof SecurityEventItem)
        {
            SecurityEventItem other = (SecurityEventItem) o;
            result = security.getUUID().compareTo(other.getSecurity().getUUID());
            
            if (result == 0)
            {
                result = message.compareTo(other.getMessage());
            }
        }
        
        return result;
    }

}
