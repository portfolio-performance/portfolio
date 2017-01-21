package name.abuchen.portfolio.model;

import java.time.LocalDate;
import java.util.ResourceBundle;

import name.abuchen.portfolio.money.Money;

public class SecurityEvent
{
    public enum Type
    {
        STOCK_SPLIT, STOCK_DIVIDEND;

        private static final ResourceBundle RESOURCES = ResourceBundle.getBundle("name.abuchen.portfolio.model.labels"); //$NON-NLS-1$

        public String toString()
        {
            return RESOURCES.getString("event." + name()); //$NON-NLS-1$
        }
    }

    private LocalDate date;
    private Type type;
    private String details;
    private Money dividend;

    public SecurityEvent()
    {
        // xstream
    }

    public SecurityEvent(LocalDate date, Type type, String details)
    {
        this.date = date;
        this.type = type;
        this.details = details;
        this.dividend = null;
    }

    public SecurityEvent(LocalDate date, Type type, Money money)
    {
        this.date = date;
        this.type = type;
        this.dividend = money;
        this.details = null;
    }

    
    public LocalDate getDate()
    {
        return date;
    }

    public Type getType()
    {
        return type;
    }

    public String getDetails()
    {
        if (details != null) {
            return details;
        } else {
            // details == null
            return dividend.toString();
        }
    }
    
}
