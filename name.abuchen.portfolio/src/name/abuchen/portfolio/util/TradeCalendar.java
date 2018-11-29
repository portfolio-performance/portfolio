package name.abuchen.portfolio.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

import de.jollyday.HolidayCalendar;
import de.jollyday.HolidayManager;
import de.jollyday.ManagerParameters;

public class TradeCalendar
{
    public boolean isHoliday(LocalDate date)
    {
        return checkIsHoliday(date, HolidayCalendar.GERMANY, null);
    }

    public boolean isHoliday(LocalDate date, HolidayCalendar manager)
    {
        return checkIsHoliday(date, manager, null);
    }

    public boolean isHoliday(LocalDate date, String manager)
    {
        return checkIsHoliday(date, manager == null ? HolidayCalendar.GERMANY : HolidayCalendar.valueOf(manager), null);
    }

    public boolean isHoliday(LocalDate date, String manager, String managerProvince)
    {
        return checkIsHoliday(date, manager == null ? HolidayCalendar.GERMANY : HolidayCalendar.valueOf(manager),
                        managerProvince == null ? "" : managerProvince); //$NON-NLS-1$
    }

    private boolean checkIsHoliday(LocalDate date, HolidayCalendar manager, String managerProvince)
    {
        Set<DayOfWeek> weekend = EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (weekend.contains(dayOfWeek))
            return true;
        HolidayManager tradingDayManager = HolidayManager.getInstance(ManagerParameters.create(manager));
        return managerProvince != null ? tradingDayManager.isHoliday(date, managerProvince)
                        : tradingDayManager.isHoliday(date);
    }
}
