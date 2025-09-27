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

import name.abuchen.portfolio.util.HebrewCalendar.HebrewDate;

/* package */ abstract class HolidayType
{
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
            LocalDate date = LocalDate.of(year, month.getValue(), dayOfMonth);
            return new Holiday(getName(), date);
        }
    }

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
            LocalDate date = LocalDate.of(year, month, 1);
            return new Holiday(getName(), date.with(dayOfWeekInMonth(which, weekday)));
        }
    }

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
            LocalDate easterSunday = calculateEasterSunday(year);

            return new Holiday(getName(), easterSunday.plusDays(daysToAdd));
        }

        private LocalDate calculateEasterSunday(int nYear)
        {
            // @formatter:off

            // Taken from
            // http://www.java2s.com/Code/Java/Data-Type/CalculateHolidays.htm
            // and published under the
            // GNU General Public License version 2
            
            /*  Calculate Easter Sunday
            Written by Gregory N. Mirsky
            Source: 2nd Edition by Peter Duffett-Smith. It was originally from
            Butcher's Ecclesiastical Calendar, published in 1876. This
            algorithm has also been published in the 1922 book General
            Astronomy by Spencer Jones; in The Journal of the British
            Astronomical Association (Vol.88, page 91, December 1977); and in
            Astronomical Algorithms (1991) by Jean Meeus.
            This algorithm holds for any year in the Gregorian Calendar, which
            (of course) means years including and after 1583.
                  a=year%19
                  b=year/100
                  c=year%100
                  d=b/4
                  e=b%4
                  f=(b+8)/25
                  g=(b-f+1)/3
                  h=(19*a+b-d-g+15)%30
                  i=c/4
                  k=c%4
                  l=(32+2*e+2*i-h-k)%7
                  m=(a+11*h+22*l)/451
                  Easter Month =(h+l-7*m+114)/31  [3=March, 4=April]
                  p=(h+l-7*m+114)%31
                  Easter Date=p+1     (date in Easter Month)
            Note: Integer truncation is already factored into the
            calculations. Using higher percision variables will cause
            inaccurate calculations. 
            */
            // @formatter:on

            int nA = 0;
            int nB = 0;
            int nC = 0;
            int nD = 0;
            int nE = 0;
            int nF = 0;
            int nG = 0;
            int nH = 0;
            int nI = 0;
            int nK = 0;
            int nL = 0;
            int nM = 0;
            int nP = 0;
            int nEasterMonth = 0;
            int nEasterDay = 0;

            nA = nYear % 19;
            nB = nYear / 100;
            nC = nYear % 100;
            nD = nB / 4;
            nE = nB % 4;
            nF = (nB + 8) / 25;
            nG = (nB - nF + 1) / 3;
            nH = (19 * nA + nB - nD - nG + 15) % 30;
            nI = nC / 4;
            nK = nC % 4;
            nL = (32 + 2 * nE + 2 * nI - nH - nK) % 7;
            nM = (nA + 11 * nH + 22 * nL) / 451;

            // [3=March, 4=April]
            nEasterMonth = (nH + nL - 7 * nM + 114) / 31;
            nP = (nH + nL - 7 * nM + 114) % 31;

            // Date in Easter Month.
            nEasterDay = nP + 1;

            // Populate the date object...
            return LocalDate.of(nYear, nEasterMonth, nEasterDay);
        }
    }

    /**
     * Holiday type for Israeli Memorial/Independence Days. Dates vary based on
     * day of week to avoid Sabbath conflicts.
     */
    private static class IsraeliMemorialCalendarHolidayType extends HolidayType
    {
        private final HebrewCalendar calendar = new HebrewCalendar();

        public IsraeliMemorialCalendarHolidayType(HolidayName name)
        {
            super(name);
        }

        @Override
        protected Holiday doGetHoliday(int gregorianYear)
        {
            var startOfYearAbsolute = calendar.getAbsoluteFromGregorianDate(LocalDate.of(gregorianYear, 1, 1));
            var hebrewYearStart = calendar.getHebrewDateFromAbsolute(startOfYearAbsolute);

            var memorialDate = calculateMemorialDate(hebrewYearStart.getYear());
            var gregorianDate = calendar.getGregorianDateFromAbsolute(calendar.getAbsoluteFromHebrewDate(memorialDate));

            return new Holiday(getName(), gregorianDate);
        }

        /**
         * Calculates the Hebrew date for Memorial Day. Iyyar 4, but moved to
         * avoid Thursday/Friday/Saturday.
         */
        protected HebrewDate calculateMemorialDate(int hebrewYear)
        {
            var weekday = getWeekdayOfHebrewDate(4, 2, hebrewYear); // Iyyar 4

            if (weekday == 5)
            {
                // Friday - move to Wednesday
                return HebrewDate.of(hebrewYear, 2, 2);
            }
            else if (weekday == 4)
            {
                // Thursday - move to Wednesday
                return HebrewDate.of(hebrewYear, 2, 3);
            }
            else if (hebrewYear >= 5764 && weekday == 0)
            {
                // Saturday after 2004 - move to Sunday
                return HebrewDate.of(hebrewYear, 2, 5);
            }
            else
            {
                return HebrewDate.of(hebrewYear, 2, 4); // Default date
            }
        }

        private int getWeekdayOfHebrewDate(int day, int month, int year)
        {
            var absoluteDate = calendar.getAbsoluteFromHebrewDate(HebrewDate.of(year, month, day));
            return absoluteDate % 7;
        }
    }

    /**
     * Holiday type for Israeli Independence Day (Yom Ha'atzmaut). Date varies
     * based on day of week and follows Memorial Day.
     */
    private static class IsraeliIndependenceCalendarHolidayType extends IsraeliMemorialCalendarHolidayType
    {
        private final HebrewCalendar calendar = new HebrewCalendar();

        public IsraeliIndependenceCalendarHolidayType(HolidayName name)
        {
            super(name);
        }

        @Override
        protected Holiday doGetHoliday(int gregorianYear)
        {
            var startOfYearAbsolute = calendar.getAbsoluteFromGregorianDate(LocalDate.of(gregorianYear, 1, 1));
            var hebrewYearStart = calendar.getHebrewDateFromAbsolute(startOfYearAbsolute);

            var independenceDate = calculateIndependenceDate(hebrewYearStart.getYear());
            var gregorianDate = calendar
                            .getGregorianDateFromAbsolute(calendar.getAbsoluteFromHebrewDate(independenceDate));

            return new Holiday(getName(), gregorianDate);
        }

        /**
         * Calculates the Hebrew date for Independence Day. Usually Iyyar 5, but
         * moved to follow Memorial Day rules and avoid Sabbath conflicts.
         */
        private HebrewDate calculateIndependenceDate(int hebrewYear)
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

            return HebrewDate.of(independenceYear, independenceMonth, independenceDay);
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
            var gregorianDate = calculateHebrewHolidayInGregorianYear(year, hebrewMonth, hebrewDayOfMonth, daysToAdd);
            return new Holiday(getName(), gregorianDate);
        }

        /**
         * Utility method to calculate Hebrew holidays that fall within a
         * Gregorian year. Handles the complexity of Hebrew calendar spanning
         * Gregorian years.
         */
        private LocalDate calculateHebrewHolidayInGregorianYear(int gregorianYear, int hebrewMonth, int hebrewDay,
                        int additionalDays)
        {
            var calendar = new HebrewCalendar();

            // Get absolute dates for start and end of Gregorian year
            var yearStartAbsolute = calendar.getAbsoluteFromGregorianDate(LocalDate.of(gregorianYear, 1, 1));
            var yearEndAbsolute = calendar.getAbsoluteFromGregorianDate(LocalDate.of(gregorianYear, 12, 31));

            // Get corresponding Hebrew dates
            var hebrewYearStart = calendar.getHebrewDateFromAbsolute(yearStartAbsolute);
            var hebrewYearEnd = calendar.getHebrewDateFromAbsolute(yearEndAbsolute);

            var holidayDay = hebrewDay + additionalDays;

            // Try the Hebrew holiday in the same Hebrew year as start of
            // Gregorian
            // year
            var holidayCurrentYear = HebrewDate.of(hebrewYearStart.getYear(), hebrewMonth, holidayDay);
            var gregorianCurrentYear = calendar
                            .getGregorianDateFromAbsolute(calendar.getAbsoluteFromHebrewDate(holidayCurrentYear));

            if (gregorianCurrentYear.getYear() == gregorianYear)
                return gregorianCurrentYear;

            // Try the Hebrew holiday in the next Hebrew year
            var holidayNextYear = HebrewDate.of(hebrewYearEnd.getYear(), hebrewMonth, holidayDay);
            return calendar.getGregorianDateFromAbsolute(calendar.getAbsoluteFromHebrewDate(holidayNextYear));
        }
    }

    /**
     * Holiday type for Purim, which falls on different Hebrew months in leap
     * years.
     */
    private static class JewishPurimCalendarHolidayType extends HolidayType
    {
        private final HebrewCalendar calendar = new HebrewCalendar();

        public JewishPurimCalendarHolidayType(HolidayName name)
        {
            super(name);
        }

        @Override
        protected Holiday doGetHoliday(int gregorianYear)
        {
            var startOfYearAbsolute = calendar.getAbsoluteFromGregorianDate(LocalDate.of(gregorianYear, 1, 1));
            var hebrewYearStart = calendar.getHebrewDateFromAbsolute(startOfYearAbsolute);

            var purimDate = calculatePurimDate(hebrewYearStart.getYear());
            var gregorianDate = calendar.getGregorianDateFromAbsolute(calendar.getAbsoluteFromHebrewDate(purimDate));

            return new Holiday(getName(), gregorianDate);
        }

        /**
         * Calculates the Hebrew date for Purim. Adar 14 (or Adar II 14 in leap
         * years).
         */
        private HebrewDate calculatePurimDate(int hebrewYear)
        {
            // Adar II or Adar
            var purimMonth = calendar.isHebrewLeapYear(hebrewYear) ? 13 : 12;
            return HebrewDate.of(hebrewYear, purimMonth, 14);
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

    public static HolidayType jewishPurimCalendar(HolidayName name)
    {
        return new JewishPurimCalendarHolidayType(name);
    }

    public static HolidayType israeliMemorialCalendar(HolidayName name)
    {
        return new IsraeliMemorialCalendarHolidayType(name);
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

    public Holiday getHoliday(int year)
    {
        if (validFrom != -1 && year < validFrom)
            return null;

        if (validTo != -1 && year > validTo)
            return null;

        if (exceptIn.contains(year))
            return null;

        Holiday answer = doGetHoliday(year);

        if (moveIf.isEmpty() && moveTo == null)
            return answer;

        LocalDate date = answer.getDate();

        for (MoveIf mv : moveIf)
            date = mv.apply(date);

        if (moveTo != null)
            date = date.with(nextOrSame(moveTo));

        return new Holiday(answer.getName(), date);
    }

    protected abstract Holiday doGetHoliday(int year);
}
