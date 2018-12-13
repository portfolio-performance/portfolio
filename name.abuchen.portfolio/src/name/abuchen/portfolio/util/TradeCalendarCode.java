package name.abuchen.portfolio.util;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.jollyday.HolidayCalendar;
import de.jollyday.HolidayManager;
import de.jollyday.ManagerParameters;

public final class TradeCalendarCode implements Comparable<TradeCalendarCode>
{
    private static final Map<String, TradeCalendarCode> CACHE = new HashMap<>();

    static
    {
        for (HolidayCalendar c : HolidayCalendar.values())
        {
            HolidayManager tradingDayManager = HolidayManager.getInstance(ManagerParameters.create(c));
            String calendarCode = c.toString();
            CACHE.put(calendarCode, new TradeCalendarCode(c.toString(),
                            tradingDayManager.getCalendarHierarchy().getDescription()));
        }
    }

    private String calendarCode;
    private String displayName;

    private TradeCalendarCode(String calendarCode, String displayName)
    {
        this.calendarCode = calendarCode;
        this.displayName = displayName;
    }

    public static List<TradeCalendarCode> getAvailableCalendar()
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
        // Create a generic locale.
        Locale clientLocale = new Locale(""); //$NON-NLS-1$
        Collator collator = Collator.getInstance(clientLocale);
        collator.setStrength(Collator.SECONDARY);
        return collator.compare(getDisplayName(), other.getDisplayName());
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
