package name.abuchen.portfolio.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import de.jollyday.HolidayCalendar;
import de.jollyday.HolidayManager;
import de.jollyday.ManagerParameters;

public class TradeCalendar
{
    public boolean isHoliday(LocalDate date)
    {
        IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode("name.abuchen.portfolio.ui"); //$NON-NLS-1$
        String clientCalendar = prefs.get("CALENDAR", "GERMANY"); //$NON-NLS-1$ //$NON-NLS-2$
        return checkIsHoliday(date, HolidayCalendar.valueOf(clientCalendar), null);
    }

    public boolean isHoliday(LocalDate date, HolidayCalendar manager)
    {
        return checkIsHoliday(date, manager, null);
    }

    public boolean isHoliday(LocalDate date, String manager)
    {
        if (manager == null)
        {
            IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode("name.abuchen.portfolio.ui"); //$NON-NLS-1$
            String clientCalendar = prefs.get("CALENDAR", "GERMANY"); //$NON-NLS-1$ //$NON-NLS-2$
            return checkIsHoliday(date, HolidayCalendar.valueOf(clientCalendar), null);
        }
        else
            return checkIsHoliday(date, HolidayCalendar.valueOf(manager), null);
    }

    public boolean isHoliday(LocalDate date, String manager, String managerProvince)
    {
        if (manager == null)
        {
            IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode("name.abuchen.portfolio.ui"); //$NON-NLS-1$
            String clientCalendar = prefs.get("CALENDAR", "GERMANY"); //$NON-NLS-1$ //$NON-NLS-2$
            return checkIsHoliday(date, HolidayCalendar.valueOf(clientCalendar), managerProvince);
        }
        else
            return checkIsHoliday(date, HolidayCalendar.valueOf(manager), managerProvince);
    }

    private boolean checkIsHoliday(LocalDate date, HolidayCalendar manager, String managerProvince)
    {
        Set<DayOfWeek> weekend = EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (weekend.contains(dayOfWeek))
            return true;
        HolidayManager tradingDayManager = HolidayManager.getInstance(ManagerParameters.create(manager));
        return managerProvince != null
                        ? tradingDayManager.isHoliday(date, managerProvince.isEmpty() ? "" : managerProvince) //$NON-NLS-1$
                        : tradingDayManager.isHoliday(date);
    }
}
