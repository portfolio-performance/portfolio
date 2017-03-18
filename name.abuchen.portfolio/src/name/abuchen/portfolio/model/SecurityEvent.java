package name.abuchen.portfolio.model;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import name.abuchen.portfolio.money.Values;

public class SecurityEvent extends SecurityElement
{
    public enum Type
    {
        STOCK_SPLIT, STOCK_DIVIDEND, STOCK_OTHER, NONE;

        private static final ResourceBundle RESOURCES = ResourceBundle.getBundle("name.abuchen.portfolio.model.labels"); //$NON-NLS-1$

        public String toString()
        {
            return RESOURCES.getString("event." + name()); //$NON-NLS-1$
        }
    }

    private Type type;
    public static final String NONE = "None";

    private String details = null;

    public SecurityEvent()
    {
        type = Type.NONE;
    }

    public SecurityEvent(LocalDate date, SecurityEvent.Type type, String currencyCode, double value)
    {
        this.setDate(date);
        this.setType(type);
        this.setDetails(currencyCode, BigDecimal.valueOf(value));
    }

    public SecurityEvent(LocalDate date, SecurityEvent.Type type, String currencyCode, BigDecimal value)
    {
        this.setDate(date);
        this.setType(type);
        this.setDetails(currencyCode, value);
    }

    public SecurityEvent(LocalDate date, SecurityEvent.Type type, String details)
    {
        this.setDate(date);
        this.setType(type);
        this.setDetails(details);
    }

    public void setDate(LocalDate date)
    {
        super.date = date;
    }

    public void setType(Type type)
    {
        this.type = type;
    }

    public void setDetails(String details)
    {
        this.details = details;
    }

    public void setDetails(String currencyCode, double value)
    {
        this.setDetails(currencyCode, BigDecimal.valueOf(value));
    }
    private void setDetails(String currencyCode, BigDecimal value)
    {
        DecimalFormat valueFormat = new DecimalFormat("#,##0.00####"); //$NON-NLS-1$
        this.details = currencyCode + " " + valueFormat.format(value);
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
        if (details != null)
        {
            return details;
        }
        else
        {
            return ""; //Long.toString(this.value);
        }
    }

    public String toString()
    {
        return String.format("EVENT %tF: [%s] value %,10.2f details: <%s>", date, type.toString(), value / Values.Quote.divider(), details);
    }

    public static List<SecurityEvent> castElement2EventList(List<SecurityElement> iList)
    {
        if (iList != null)
        {
            List<SecurityEvent> oList = new ArrayList<>();
            for (SecurityElement e : iList)
            {
                    if (e instanceof SecurityEvent)
                        oList.add((SecurityEvent) e); // need to cast each object specifically
            }
            return oList;
        }
        else
        {
            return null;
        }
    }
}