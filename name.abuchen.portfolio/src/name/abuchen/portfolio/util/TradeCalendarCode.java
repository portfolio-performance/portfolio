package name.abuchen.portfolio.util;

import de.jollyday.HolidayCalendar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import name.abuchen.portfolio.Messages;

public final class TradeCalendarCode implements Comparable<TradeCalendarCode>
{
    private static final Map<String, TradeCalendarCode> CACHE = new HashMap<>();

    static
    {
        for (HolidayCalendar c : HolidayCalendar.values())
        {
            String calendarCode = c.toString();

            String displayName;
            Locale obj = new Locale("", c.getId()); //$NON-NLS-1$
            String displayNameISO = obj.getDisplayCountry();
            
            switch (c.getId())
            {
                case "DJ_STOXX": //$NON-NLS-1$
                    displayName = Messages.CalendarNameDJ_STOXX;
                    break;
                case "LME": //$NON-NLS-1$
                    displayName = Messages.CalendarNameLME;
                    break;
                case "NYSE": //$NON-NLS-1$
                    displayName = Messages.CalendarNameNYSE;
                    break;
                case "TARGET": //$NON-NLS-1$
                    displayName = Messages.CalendarNameTARGET;
                    break;
                default:
                    displayName = displayNameISO;
            }

            CACHE.put(calendarCode, new TradeCalendarCode(calendarCode, displayName));
        }
    }

    private String calendarCode;
    private String displayName;

    private TradeCalendarCode(String calendarCode, String displayName)
    {
        this.calendarCode = calendarCode;
        this.displayName = displayName;
    }

    public static List<TradeCalendarCode> getAvailableCalendars()
    {
        return new ArrayList<>(CACHE.values());
    }

    public static TradeCalendarCode getInstance(String calendarCode)
    {
        return CACHE.get(calendarCode);
    }

    public String getCalendarCode()
    {
        return calendarCode;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public String getLabel()
    {
        return displayName;
    }

    @Override
    public String toString()
    {
        return getLabel();
    }

    @Override
    public int compareTo(TradeCalendarCode other)
    {
        return getCalendarCode().compareTo(other.getCalendarCode());
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((calendarCode == null) ? 0 : calendarCode.hashCode());
        return result;
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
        TradeCalendarCode other = (TradeCalendarCode) obj;
        if (calendarCode == null)
        {
            if (other.calendarCode != null)
                return false;
        }
        else if (!calendarCode.equals(other.calendarCode))
            return false;
        return true;
    }
}
