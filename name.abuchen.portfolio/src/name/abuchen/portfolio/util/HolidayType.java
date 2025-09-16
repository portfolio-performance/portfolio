package name.abuchen.portfolio.util;

import static java.time.temporal.TemporalAdjusters.dayOfWeekInMonth;
import static java.time.temporal.TemporalAdjusters.nextOrSame;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import name.abuchen.portfolio.util.JewishCalendar.CalendarImpl;
import name.abuchen.portfolio.util.JewishCalendar.JewishCalendarDate;

/**
 * Abstract base class for different types of holidays. Supports fixed dates,
 * weekday-based dates, Easter-relative dates, and Jewish calendar dates.
 */
/* package */ abstract class HolidayType
{

    /**
     * Holiday type for Israeli Independence Day (Yom Ha'atzmaut). Date varies
     * based on day of week and follows Memorial Day.
     */
    private static class IsraeliIndependenceCalendarHolidayType extends HolidayType
    {
        private final CalendarImpl calendar = new CalendarImpl();

        public IsraeliIndependenceCalendarHolidayType(HolidayName name)
        {
            super(name);
        }

        @Override
        protected Holiday doGetHoliday(int gregorianYear)
        {
            var startOfYearAbsolute = calendar.absoluteFromGregorianDate(new JewishCalendarDate(1, 1, gregorianYear));
            var hebrewYearStart = calendar.hebrewDateFromAbsolute(startOfYearAbsolute);

            var independenceDate = calculateIndependenceDate(hebrewYearStart.getYear());
            var gregorianDate = calendar
                            .gregorianDateFromAbsolute(calendar.absoluteFromHebrewDate(independenceDate));

            var date = LocalDate.of(gregorianDate.getYear(), gregorianDate.getMonth(), gregorianDate.getDay());
            return new Holiday(getName(), date);
        }

        /**
         * Calculates the Hebrew date for Independence Day. Usually Iyyar 5, but
         * moved to follow Memorial Day rules and avoid Sabbath conflicts.
         */
        private JewishCalendarDate calculateIndependenceDate(int hebrewYear)
        {
            // Independence Day follows Memorial Day, so we need to calculate
            // Memorial Day first
            var memorialDate = calculateMemorialDate(hebrewYear);

            // Independence Day is the day after Memorial Day
            var independenceDay = memorialDate.getDay() + 1;
            var independenceMonth = memorialDate.getMonth();
            var independenceYear = memorialDate.getYear();

            // Handle month overflow (though unlikely with Iyyar)
            var maxDayInMonth = calendar.getLastDayOfHebrewMonth(independenceMonth, independenceYear);
            if (independenceDay > maxDayInMonth)
            {
                independenceDay = 1;
                independenceMonth++;
                if (independenceMonth > calendar.getLastMonthOfHebrewYear(independenceYear))
                {
                    independenceMonth = 1;
                    independenceYear++;
                }
            }

            return new JewishCalendarDate(independenceDay, independenceMonth, independenceYear);
        }

        /**
         * Calculates the Hebrew date for Memorial Day (shared logic). Iyyar 4,
         * but moved to avoid Thursday/Friday/Saturday.
         */
        private JewishCalendarDate calculateMemorialDate(int hebrewYear)
        {
            var weekday = getWeekdayOfHebrewDate(4, 2, hebrewYear); // Iyyar 4

            if (weekday == 5)
            { // Friday - move to Wednesday
                return new JewishCalendarDate(2, 2, hebrewYear);
            }
            else if (weekday == 4)
            { // Thursday - move to Wednesday
                return new JewishCalendarDate(3, 2, hebrewYear);
            }
            else if (hebrewYear >= 5764 && weekday == 0)
            { // Saturday after 2004 - move to Sunday
                return new JewishCalendarDate(5, 2, hebrewYear);
            }
            else
            {
                return new JewishCalendarDate(4, 2, hebrewYear); // Default date
            }
        }

        private int getWeekdayOfHebrewDate(int day, int month, int year)
        {
            var absoluteDate = calendar.absoluteFromHebrewDate(new JewishCalendarDate(day, month, year));
            return absoluteDate % 7;
        }
    }

    /**
     * Helper class for conditional date movement based on day of week.
     */
    private static class MoveIf
    {
        private final DayOfWeek dayOfWeek;
        private final int daysToAdd;

        public MoveIf(DayOfWeek dayOfWeek, int daysToAdd)
        {
            this.dayOfWeek = dayOfWeek;
            this.daysToAdd = daysToAdd;
        }

        public LocalDate apply(LocalDate date)
        {
            return date.getDayOfWeek() == dayOfWeek ? date.plusDays(daysToAdd) : date;
        }
    }

    /**
     * Holiday type for fixed calendar dates (e.g., Christmas on December 25).
     */
    private static class FixedHolidayType extends HolidayType
    {
        private final Month month;
        private final int dayOfMonth;

        public FixedHolidayType(HolidayName name, Month month, int dayOfMonth)
        {
            super(name);
            this.month = month;
            this.dayOfMonth = dayOfMonth;
        }

        @Override
        protected Holiday doGetHoliday(int year)
        {
            var date = LocalDate.of(year, month.getValue(), dayOfMonth);
            return new Holiday(getName(), date);
        }
    }

    /**
     * Holiday type for weekday-based dates (e.g., first Monday in September).
     */
    private static class FixedWeekdayHolidayType extends HolidayType
    {
        private final int which;
        private final DayOfWeek weekday;
        private final Month month;

        public FixedWeekdayHolidayType(HolidayName name, int which, DayOfWeek weekday, Month month)
        {
            super(name);
            this.which = which;
            this.weekday = weekday;
            this.month = month;
        }

        @Override
        protected Holiday doGetHoliday(int year)
        {
            var date = LocalDate.of(year, month, 1);
            return new Holiday(getName(), date.with(dayOfWeekInMonth(which, weekday)));
        }
    }

    /**
     * Holiday type for Easter-relative dates (e.g., Good Friday = Easter - 2
     * days).
     */
    private static class RelativeToEasterHolidayType extends HolidayType
    {
        private final int daysToAdd;

        public RelativeToEasterHolidayType(HolidayName name, int daysToAdd)
        {
            super(name);
            this.daysToAdd = daysToAdd;
        }

        @Override
        protected Holiday doGetHoliday(int year)
        {
            var easterSunday = calculateEasterSunday(year);
            return new Holiday(getName(), easterSunday.plusDays(daysToAdd));
        }

        /**
         * Calculates Easter Sunday using the ecclesiastical algorithm.
         */
        private LocalDate calculateEasterSunday(int year)
        {
            // Easter calculation algorithm from Butcher's Ecclesiastical
            // Calendar (1876)
            // Valid for Gregorian Calendar (years >= 1583)

            var a = year % 19;
            var b = year / 100;
            var c = year % 100;
            var d = b / 4;
            var e = b % 4;
            var f = (b + 8) / 25;
            var g = (b - f + 1) / 3;
            var h = (19 * a + b - d - g + 15) % 30;
            var i = c / 4;
            var k = c % 4;
            var l = (32 + 2 * e + 2 * i - h - k) % 7;
            var m = (a + 11 * h + 22 * l) / 451;

            var easterMonth = (h + l - 7 * m + 114) / 31; // 3=March, 4=April
            var easterDay = ((h + l - 7 * m + 114) % 31) + 1;

            return LocalDate.of(year, easterMonth, easterDay);
        }
    }

    /**
     * Holiday type for fixed Jewish calendar dates.
     */
    private static class FixedJewishCalendarHolidayType extends HolidayType
    {
        private final int hebrewMonth;
        private final int hebrewDayOfMonth;
        private final int daysToAdd;

        public FixedJewishCalendarHolidayType(HolidayName name, int hebrewMonth, int hebrewDayOfMonth, int daysToAdd)
        {
            super(name);
            this.hebrewMonth = hebrewMonth;
            this.hebrewDayOfMonth = hebrewDayOfMonth;
            this.daysToAdd = daysToAdd;
        }

        @Override
        protected Holiday doGetHoliday(int year)
        {
            var gregorianDate = calculateHebrewHolidayInGregorianYear(year, hebrewMonth,
                            hebrewDayOfMonth, daysToAdd);
            var date = LocalDate.of(gregorianDate.getYear(), gregorianDate.getMonth(), gregorianDate.getDay());
            return new Holiday(getName(), date);
        }
    }

    /**
     * Holiday type for Israeli Holocaust Remembrance Day (Yom HaShoah). Date
     * varies based on day of week to avoid Sabbath conflicts.
     */
    private static class IsraeliHolocaustCalendarHolidayType extends HolidayType
    {
        private final CalendarImpl calendar = new CalendarImpl();

        public IsraeliHolocaustCalendarHolidayType(HolidayName name)
        {
            super(name);
        }

        @Override
        protected Holiday doGetHoliday(int gregorianYear)
        {
            var startOfYearAbsolute = calendar.absoluteFromGregorianDate(new JewishCalendarDate(1, 1, gregorianYear));
            var hebrewYearStart = calendar.hebrewDateFromAbsolute(startOfYearAbsolute);

            var holocaustDate = calculateHolocaustDate(hebrewYearStart.getYear());
            var gregorianDate = calendar
                            .gregorianDateFromAbsolute(calendar.absoluteFromHebrewDate(holocaustDate));

            var date = LocalDate.of(gregorianDate.getYear(), gregorianDate.getMonth(), gregorianDate.getDay());
            return new Holiday(getName(), date);
        }

        /**
         * Calculates the Hebrew date for Holocaust Remembrance Day. Nissan 27,
         * but moved to avoid Friday/Saturday.
         */
        private JewishCalendarDate calculateHolocaustDate(int hebrewYear)
        {
            var weekday = getWeekdayOfHebrewDate(27, 1, hebrewYear); // Nissan
                                                                     // 27

            if (weekday == 5)
            { // Friday - move to Thursday
                return new JewishCalendarDate(26, 1, hebrewYear);
            }
            else if (hebrewYear >= 5757 && weekday == 0)
            { // Saturday after 1997 - move to Sunday
                return new JewishCalendarDate(28, 1, hebrewYear);
            }
            else
            {
                return new JewishCalendarDate(27, 1, hebrewYear); // Default
                                                                  // date
            }
        }

        private int getWeekdayOfHebrewDate(int day, int month, int year)
        {
            var absoluteDate = calendar.absoluteFromHebrewDate(new JewishCalendarDate(day, month, year));
            return absoluteDate % 7;
        }
    }

    /**
     * Holiday type for Purim, which falls on different Hebrew months in leap
     * years.
     */
    private static class JewishPurimCalendarHolidayType extends HolidayType
    {
        private final CalendarImpl calendar = new CalendarImpl();

        public JewishPurimCalendarHolidayType(HolidayName name)
        {
            super(name);
        }

        @Override
        protected Holiday doGetHoliday(int gregorianYear)
        {
            var startOfYearAbsolute = calendar.absoluteFromGregorianDate(new JewishCalendarDate(1, 1, gregorianYear));
            var hebrewYearStart = calendar.hebrewDateFromAbsolute(startOfYearAbsolute);

            var purimDate = calculatePurimDate(hebrewYearStart.getYear());
            var gregorianDate = calendar
                            .gregorianDateFromAbsolute(calendar.absoluteFromHebrewDate(purimDate));

            var date = LocalDate.of(gregorianDate.getYear(), gregorianDate.getMonth(), gregorianDate.getDay());
            return new Holiday(getName(), date);
        }

        /**
         * Calculates the Hebrew date for Purim. Adar 14 (or Adar II 14 in leap
         * years).
         */
        private JewishCalendarDate calculatePurimDate(int hebrewYear)
        {
            // Adar II or Adar
            var purimMonth = calendar.isHebrewLeapYear(hebrewYear) ? 13 : 12;
            return new JewishCalendarDate(14, purimMonth, hebrewYear);
        }
    }

    /**
     * Holiday type for Israeli Memorial/Independence Days. Dates vary based on
     * day of week to avoid Sabbath conflicts.
     */
    private static class IsraeliMemorialCalendarHolidayType extends HolidayType
    {
        private final int daysToAdd;
        private final CalendarImpl calendar = new CalendarImpl();

        public IsraeliMemorialCalendarHolidayType(HolidayName name, int daysToAdd)
        {
            super(name);
            this.daysToAdd = daysToAdd;
        }

        @Override
        protected Holiday doGetHoliday(int gregorianYear)
        {
            var startOfYearAbsolute = calendar.absoluteFromGregorianDate(new JewishCalendarDate(1, 1, gregorianYear));
            var hebrewYearStart = calendar.hebrewDateFromAbsolute(startOfYearAbsolute);

            var memorialDate = calculateMemorialDate(hebrewYearStart.getYear());
            var gregorianDate = calendar
                            .gregorianDateFromAbsolute(calendar.absoluteFromHebrewDate(memorialDate) + daysToAdd);

            var date = LocalDate.of(gregorianDate.getYear(), gregorianDate.getMonth(), gregorianDate.getDay());
            return new Holiday(getName(), date);
        }

        /**
         * Calculates the Hebrew date for Memorial Day. Iyyar 4, but moved to
         * avoid Thursday/Friday/Saturday.
         */
        private JewishCalendarDate calculateMemorialDate(int hebrewYear)
        {
            var weekday = getWeekdayOfHebrewDate(4, 2, hebrewYear); // Iyyar 4

            if (weekday == 5)
            { // Friday - move to Wednesday
                return new JewishCalendarDate(2, 2, hebrewYear);
            }
            else if (weekday == 4)
            { // Thursday - move to Wednesday
                return new JewishCalendarDate(3, 2, hebrewYear);
            }
            else if (hebrewYear >= 5764 && weekday == 0)
            { // Saturday after 2004 - move to Sunday
                return new JewishCalendarDate(5, 2, hebrewYear);
            }
            else
            {
                return new JewishCalendarDate(4, 2, hebrewYear); // Default date
            }
        }

        private int getWeekdayOfHebrewDate(int day, int month, int year)
        {
            var absoluteDate = calendar.absoluteFromHebrewDate(new JewishCalendarDate(day, month, year));
            return absoluteDate % 7;
        }
    }

    // Instance variables
    private final HolidayName name;
    private int validFrom = -1;
    private int validTo = -1;
    private final Set<Integer> exceptIn = new HashSet<>();
    private final List<MoveIf> moveIf = new ArrayList<>();
    private DayOfWeek moveTo = null;

    protected HolidayType(HolidayName name)
    {
        this.name = name;
    }

    // Factory methods
    public static HolidayType fixedJewishCalendar(HolidayName name, int hebrewMonth, int hebrewDayOfMonth,
                    int daysToAdd)
    {
        return new FixedJewishCalendarHolidayType(name, hebrewMonth, hebrewDayOfMonth, daysToAdd);
    }

    public static HolidayType jewishPurimCalendar(HolidayName name, int daysToAdd)
    {
        return new JewishPurimCalendarHolidayType(name);
    }

    public static HolidayType israeliHolocaustCalendar(HolidayName name, int daysToAdd)
    {
        return new IsraeliHolocaustCalendarHolidayType(name);
    }

    public static HolidayType israeliMemorialCalendar(HolidayName name)
    {
        return new IsraeliMemorialCalendarHolidayType(name, 0);
    }

    public static HolidayType israeliIndependenceCalendar(HolidayName name)
    {
        return new IsraeliIndependenceCalendarHolidayType(name);
    }

    public static HolidayType fixed(HolidayName name, Month month, int dayOfMonth)
    {
        return new FixedHolidayType(name, month, dayOfMonth);
    }

    public static HolidayType weekday(HolidayName name, int which, DayOfWeek weekday, Month month)
    {
        return new FixedWeekdayHolidayType(name, which, weekday, month);
    }

    public static HolidayType easter(HolidayName name, int daysToAdd)
    {
        return new RelativeToEasterHolidayType(name, daysToAdd);
    }

    // Configuration methods
    public HolidayName getName()
    {
        return name;
    }

    public HolidayType validFrom(int year)
    {
        this.validFrom = year;
        return this;
    }

    public HolidayType validTo(int year)
    {
        this.validTo = year;
        return this;
    }

    public HolidayType onlyIn(int year)
    {
        this.validFrom = year;
        this.validTo = year;
        return this;
    }

    public HolidayType exceptIn(int year)
    {
        this.exceptIn.add(year);
        return this;
    }

    public HolidayType moveIf(DayOfWeek dayOfWeek, int daysToAdd)
    {
        moveIf.add(new MoveIf(dayOfWeek, daysToAdd));
        return this;
    }

    public HolidayType moveTo(DayOfWeek dayOfWeek)
    {
        moveTo = dayOfWeek;
        return this;
    }

    // Main holiday calculation method
    public Holiday getHoliday(int year)
    {
        // Check validity period
        if (validFrom != -1 && year < validFrom)
            return null;
        if (validTo != -1 && year > validTo)
            return null;
        if (exceptIn.contains(year))
            return null;

        var holiday = doGetHoliday(year);

        // Apply date movements if configured
        if (moveIf.isEmpty() && moveTo == null)
            return holiday;

        var date = holiday.getDate();

        // Apply conditional movements
        for (MoveIf movement : moveIf)
        {
            date = movement.apply(date);
        }

        // Apply absolute movement to specific weekday
        if (moveTo != null)
        {
            date = date.with(nextOrSame(moveTo));
        }

        return new Holiday(holiday.getName(), date);
    }

    protected abstract Holiday doGetHoliday(int year);

    /**
     * Utility method to calculate Hebrew holidays that fall within a Gregorian
     * year. Handles the complexity of Hebrew calendar spanning Gregorian years.
     */
    public JewishCalendarDate calculateHebrewHolidayInGregorianYear(int gregorianYear, int hebrewMonth, int hebrewDay,
                    int additionalDays)
    {
        var calendar = new CalendarImpl();

        // Get absolute dates for start and end of Gregorian year
        var yearStartAbsolute = calendar.absoluteFromGregorianDate(new JewishCalendarDate(1, 1, gregorianYear));
        var yearEndAbsolute = calendar.absoluteFromGregorianDate(new JewishCalendarDate(31, 12, gregorianYear));

        // Get corresponding Hebrew dates
        var hebrewYearStart = calendar.hebrewDateFromAbsolute(yearStartAbsolute);
        var hebrewYearEnd = calendar.hebrewDateFromAbsolute(yearEndAbsolute);

        var holidayDay = hebrewDay + additionalDays;

        // Try the Hebrew holiday in the same Hebrew year as start of Gregorian
        // year
        var holidayCurrentYear = new JewishCalendarDate(holidayDay, hebrewMonth,
                        hebrewYearStart.getYear());
        var gregorianCurrentYear = calendar
                        .gregorianDateFromAbsolute(calendar.absoluteFromHebrewDate(holidayCurrentYear));

        if (gregorianCurrentYear.getYear() == gregorianYear)
            return gregorianCurrentYear;

        // Try the Hebrew holiday in the next Hebrew year
        var holidayNextYear = new JewishCalendarDate(holidayDay, hebrewMonth, hebrewYearEnd.getYear());
        var gregorianNextYear = calendar
                        .gregorianDateFromAbsolute(calendar.absoluteFromHebrewDate(holidayNextYear));

        return gregorianNextYear;
    }

    // Legacy method for backward compatibility
    @Deprecated
    public JewishCalendarDate jewishHoliday(int gregorianYear, int hebrewMonth, int hebrewDay, int additionalDays)
    {
        return calculateHebrewHolidayInGregorianYear(gregorianYear, hebrewMonth, hebrewDay, additionalDays);
    }
}
