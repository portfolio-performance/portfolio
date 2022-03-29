package name.abuchen.portfolio.ui.views.dashboard.event.item;

import java.time.LocalDate;
import java.util.Objects;

import name.abuchen.portfolio.model.Account;

public class AccountEventItem extends EventItem
{

    private final String message;
    private final Account account;
    
    public AccountEventItem(EventType type, LocalDate date, String message, Account account)
    {
        super(type, date);
        
        this.message = message;
        this.account = account;
    }
    
    public String getMessage()
    {
        return message;
    }

    public Account getAccount()
    {
        return account;
    }

    @Override
    public int hashCode()
    {
        int result = super.hashCode();
        result = 31 * result + Objects.hash(message, account);
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
        
        AccountEventItem other = (AccountEventItem) obj;
        return Objects.equals(message, other.message) && Objects.equals(account, other.account);
    }

    @Override
    public String toString()
    {
        return "AccountEventItem [type=" + getType() +                  //$NON-NLS-1$
                        ", date=" + getDate() +                         //$NON-NLS-1$
                        ", account=" + account.getUUID() +              //$NON-NLS-1$
                        ", message=" + getMessage() + "]";              //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public int compareTo(EventItem o)
    {
        int result = super.compareTo(o);
        
        if (result == 0 && o instanceof AccountEventItem)
        {
            AccountEventItem other = (AccountEventItem) o;
            result = account.getUUID().compareTo(other.getAccount().getUUID());
            
            if (result == 0)
            {
                result = message.compareTo(other.getMessage());
            }
        }
        
        return result;
    }

}
