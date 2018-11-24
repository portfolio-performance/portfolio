package name.abuchen.portfolio.util;

import de.jollyday.HolidayCalendar;
import de.jollyday.HolidayManager;
import de.jollyday.ManagerParameters;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

public class TradeCalendar
{
    public boolean isHoliday(LocalDate date)
    {
        return checkIsHoliday (date, HolidayCalendar.GERMANY);
    }

    public boolean isHoliday(LocalDate date, HolidayCalendar manager)
    {
        return checkIsHoliday (date, manager);
    }

    public boolean isHoliday(LocalDate date, String manager)
    {
        return checkIsHoliday (date, manager == null ? HolidayCalendar.GERMANY : HolidayCalendar.valueOf(manager));
    }

    private boolean checkIsHoliday(LocalDate date, HolidayCalendar manager)
    {
        Set<DayOfWeek> weekend = EnumSet.of( DayOfWeek.SATURDAY , DayOfWeek.SUNDAY );
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (weekend.contains(dayOfWeek))
        {
            return true;
        }
        HolidayManager tradingDayManager = HolidayManager.getInstance(ManagerParameters.create(manager));
        return tradingDayManager.isHoliday(date);
    }
}
