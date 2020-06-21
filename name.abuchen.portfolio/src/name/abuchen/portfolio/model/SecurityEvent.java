package name.abuchen.portfolio.model;

import java.time.LocalDate;
import java.util.ResourceBundle;

public class SecurityEvent
{
    public enum Type
    {
        STOCK_SPLIT(false), NOTE(true);

        private static final ResourceBundle RESOURCES = ResourceBundle.getBundle("name.abuchen.portfolio.model.labels"); //$NON-NLS-1$

        private boolean isUserEditable;

        private Type(boolean isUserEditable)
        {
            this.isUserEditable = isUserEditable;
        }

        public boolean isUserEditable()
        {
            return isUserEditable;
        }

        @Override
        public String toString()
        {
            return RESOURCES.getString("event." + name()); //$NON-NLS-1$
        }
    }

    private LocalDate date;
    private Type type;
    private String details;

    public SecurityEvent()
    {
        // xstream
    }

    public SecurityEvent(LocalDate date, Type type, String details)
    {
        this.date = date;
        this.type = type;
        this.details = details;
    }

    public LocalDate getDate()
    {
        return date;
    }

    public void setDate(LocalDate date)
    {
        this.date = date;
    }

    public Type getType()
    {
        return type;
    }

    public void setType(Type type)
    {
        this.type = type;
    }

    public String getDetails()
    {
        return details;
    }

    public void setDetails(String details)
    {
        this.details = details;
    }
}
