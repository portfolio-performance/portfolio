package name.abuchen.portfolio.util;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.jollyday.CalendarHierarchy;
import de.jollyday.HolidayCalendar;
import de.jollyday.HolidayManager;
import de.jollyday.ManagerParameters;

public final class TradeCalendarProvinceCode implements Comparable<TradeCalendarProvinceCode>
{
    private static Map<String, TradeCalendarProvinceCode> CACHE = new HashMap<>();

    private String calendarProvinceCode;
    private String displayProvinceName;

    private TradeCalendarProvinceCode(String calendarProvinceCode, String displayProvinceName)
    {
        this.calendarProvinceCode = calendarProvinceCode;
        this.displayProvinceName = displayProvinceName;
    }

    public static List<TradeCalendarProvinceCode> getAvailableCalendarProvinces(HolidayCalendar calendar)
    {
        CACHE.clear();
        HolidayManager tradingDayManager = HolidayManager.getInstance(ManagerParameters.create(calendar));
        // 2nd province specific public holidays if available for country
        Map<String, CalendarHierarchy> calendarProvince = tradingDayManager.getCalendarHierarchy().getChildren();
        calendarProvince.forEach((k, v) -> CACHE.put(k.toString(),
                        new TradeCalendarProvinceCode(k.toString(), v.getDescription())));

        return new ArrayList<>(CACHE.values());
    }

    public static TradeCalendarProvinceCode getInstance(String TradeCalendarProvinceCode)
    {
        return CACHE.get(TradeCalendarProvinceCode);
    }

    public String getCalendarProvinceCode()
    {
        return calendarProvinceCode;
    }

    public String getDisplayProvinceName()
    {
        return displayProvinceName;
    }

    public String getLabel()
    {
        return displayProvinceName;
    }

    @Override
    public String toString()
    {
        return getLabel();
    }

    @Override
    public int compareTo(TradeCalendarProvinceCode other)
    {
        // Create a generic locale.
        Locale clientLocale = new Locale(""); //$NON-NLS-1$
        Collator collator = Collator.getInstance(clientLocale);
        collator.setStrength(Collator.SECONDARY);
        return collator.compare(getDisplayProvinceName(), other.getDisplayProvinceName());
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((calendarProvinceCode == null) ? 0 : calendarProvinceCode.hashCode());
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
        TradeCalendarProvinceCode other = (TradeCalendarProvinceCode) obj;
        if (calendarProvinceCode == null)
        {
            if (other.calendarProvinceCode != null)
                return false;
        }
        else if (!calendarProvinceCode.equals(other.calendarProvinceCode))
            return false;
        return true;
    }
}
