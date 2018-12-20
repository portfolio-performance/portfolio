package name.abuchen.portfolio.util;

import java.text.Collator;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import de.jollyday.Holiday;
import de.jollyday.HolidayManager;

public class TradeCalendar implements Comparable<TradeCalendar>
{
    private static final Set<DayOfWeek> WEEKEND = EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    private final String code;
    private final String description;
    private final HolidayManager holidayManager;

    /* package */ TradeCalendar(String code, String description, HolidayManager holidayManager)
    {
        this.code = Objects.requireNonNull(code);
        this.description = Objects.requireNonNull(description);
        this.holidayManager = holidayManager;
    }

    public String getCode()
    {
        return code;
    }

    public String getDescription()
    {
        return description;
    }

    @Override
    public String toString()
    {
        return getDescription();
    }

    public boolean isHoliday(LocalDate date)
    {
        if (WEEKEND.contains(date.getDayOfWeek()))
            return true;

        return holidayManager.isHoliday(date);
    }

    public Set<Holiday> getHolidays(int year)
    {
        return holidayManager.getHolidays(year);
    }

    @Override
    public int compareTo(TradeCalendar other)
    {
        Collator collator = Collator.getInstance();
        collator.setStrength(Collator.SECONDARY);
        return collator.compare(getDescription(), other.getDescription());
    }

    @Override
    public int hashCode()
    {
        return code.hashCode();
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
        TradeCalendar other = (TradeCalendar) obj;
        return code.equals(other.code);
    }
}
