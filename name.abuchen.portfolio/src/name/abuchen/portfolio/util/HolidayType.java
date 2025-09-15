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

    private static class FixedJewishCalendarHolidayType extends HolidayType
    {
        private final int jewishMonth;
        private final int jewishDayOfMonth;
        private final int dayToAdd;

        public FixedJewishCalendarHolidayType(HolidayName name, int jewishMonth, int jewishDayOfMonth, int daysToAdd)
        {
            super(name);
            this.jewishMonth = jewishMonth;
            this.jewishDayOfMonth = jewishDayOfMonth;
            this.dayToAdd = daysToAdd;
        }

        @Override
        protected Holiday doGetHoliday(int year)
        {
            JewishCalendarDate jewishDate = jewishHoliday(year, this.jewishMonth, this.jewishDayOfMonth, this.dayToAdd);
            LocalDate date = LocalDate.of(jewishDate.getYear(), jewishDate.getMonth(), jewishDate.getDay());
            return new Holiday(getName(), date);
        }
    }

    private static class IsraeliHolocaustCalendarHolidayType extends HolidayType
    {
        private final CalendarImpl i = new CalendarImpl();

        public IsraeliHolocaustCalendarHolidayType(HolidayName name, int daysToAdd)
        {
            super(name);
        }

        private static int getWeekdayOfHebrewDate(int hebDay, int hebMonth, int hebYear, CalendarImpl i)
        {
            int absDate = i.absoluteFromJewishDate(new JewishCalendarDate(hebDay, hebMonth, hebYear));
            return absDate % 7;
        }

        private JewishCalendarDate calcHolocaustDateHeb(CalendarImpl i, int hebYear, int georgYear)
        {

            if (getWeekdayOfHebrewDate(27, 1, hebYear, i) == 5)
            {
                return new JewishCalendarDate(26, 1, hebYear);
            }
            else if (hebYear >= 5757 && getWeekdayOfHebrewDate(27, 1, hebYear, i) == 0)
            {
                return new JewishCalendarDate(28, 1, hebYear);
            }
            else
            {
                return new JewishCalendarDate(27, 1, hebYear);
            }

        }

        @Override
        protected Holiday doGetHoliday(int georg_year)
        {
            int absDateStartOfGeogYear = i.absoluteFromGregorianDate(new JewishCalendarDate(1, 1, georg_year));

            JewishCalendarDate jewishDateStart = i.jewishDateFromAbsolute(absDateStartOfGeogYear);

            JewishCalendarDate hebDate = calcHolocaustDateHeb(i, jewishDateStart.getYear(), georg_year);
            JewishCalendarDate geogDate = i.gregorianDateFromAbsolute(i.absoluteFromJewishDate(hebDate));
            LocalDate date = LocalDate.of(geogDate.getYear(), geogDate.getMonth(), geogDate.getDay());
            return new Holiday(getName(), date);

        }
    }

    private static class JewishPurimCalendarHolidayType extends HolidayType
    {
        private final CalendarImpl i = new CalendarImpl();

        public JewishPurimCalendarHolidayType(HolidayName name, int daysToAdd)
        {
            super(name);
        }

        private JewishCalendarDate calcPurimDateHeb(CalendarImpl i, int hebYear, int georgYear)
        {

            int monthEsther;
            if (i.hebrewLeapYear(hebYear))
                monthEsther = 13;
            else
                monthEsther = 12;

            return new JewishCalendarDate(14, monthEsther, hebYear);

        }

        @Override
        protected Holiday doGetHoliday(int georg_year)
        {
            int absDateStartOfGeogYear = i.absoluteFromGregorianDate(new JewishCalendarDate(1, 1, georg_year));

            JewishCalendarDate jewishDateStart = i.jewishDateFromAbsolute(absDateStartOfGeogYear);

            JewishCalendarDate hebDate = calcPurimDateHeb(i, jewishDateStart.getYear(), georg_year);
            JewishCalendarDate geogDate = i.gregorianDateFromAbsolute(i.absoluteFromJewishDate(hebDate));
            LocalDate date = LocalDate.of(geogDate.getYear(), geogDate.getMonth(), geogDate.getDay());
            return new Holiday(getName(), date);

        }
    }

    private static class IsraeliMemorialCalendarHolidayType extends HolidayType
    {

        private final int dayToAdd;
        private final CalendarImpl i = new CalendarImpl();

        public IsraeliMemorialCalendarHolidayType(HolidayName name, int daysToAdd)
        {
            super(name);
            this.dayToAdd = daysToAdd;
        }

        private static int getWeekdayOfHebrewDate(int hebDay, int hebMonth, int hebYear, CalendarImpl i)
        {
            int absDate = i.absoluteFromJewishDate(new JewishCalendarDate(hebDay, hebMonth, hebYear));
            return absDate % 7;
        }

        private JewishCalendarDate calcMemorialDateHeb(CalendarImpl i, int hebYear, int georg_year)
        {
            if (getWeekdayOfHebrewDate(4, 2, hebYear, i) == 5)
            {
                return new JewishCalendarDate(2, 2, hebYear);
            }
            else if (getWeekdayOfHebrewDate(4, 2, hebYear, i) == 4)
            {
                return new JewishCalendarDate(3, 2, hebYear);
            }
            else if (hebYear >= 5764 && getWeekdayOfHebrewDate(4, 2, hebYear, i) == 0)
            {
                return new JewishCalendarDate(5, 2, hebYear);
            }
            else
            {
                return new JewishCalendarDate(4, 2, hebYear);
            }
        }

        @Override
        protected Holiday doGetHoliday(int georg_year)
        {
            int absDateStartOfGeogYear = i.absoluteFromGregorianDate(new JewishCalendarDate(1, 1, georg_year));

            JewishCalendarDate jewishDateStartofGeogYear = i.jewishDateFromAbsolute(absDateStartOfGeogYear);

            JewishCalendarDate hebDate = calcMemorialDateHeb(i, jewishDateStartofGeogYear.getYear(), georg_year);
            JewishCalendarDate geogDate = i
                            .gregorianDateFromAbsolute(i.absoluteFromJewishDate(hebDate) + this.dayToAdd);

            LocalDate date = LocalDate.of(geogDate.getYear(), geogDate.getMonth(), geogDate.getDay());
            return new Holiday(getName(), date);
        }
    }

    private final HolidayName name;

    private int validFrom = -1;
    private int validTo = -1;
    private final Set<Integer> exceptIn = new HashSet<>();

    private final List<MoveIf> moveIf = new ArrayList<>();
    private DayOfWeek moveTo = null;

    public HolidayType(HolidayName name)
    {
        this.name = name;
    }

    public static HolidayType fixedJewishCalendar(HolidayName name, int jewishMonth, int jewishDayOfMonth,
                    int daysToAdd)
    {
        return new FixedJewishCalendarHolidayType(name, jewishMonth, jewishDayOfMonth, daysToAdd);
    }

    public static HolidayType jewishPurimCalendar(HolidayName name, int daysToAdd)
    {
        return new JewishPurimCalendarHolidayType(name, daysToAdd);
    }

    public static HolidayType israeliHolocaustCalendar(HolidayName name, int daysToAdd)
    {
        return new IsraeliHolocaustCalendarHolidayType(name, daysToAdd);
    }

    public static HolidayType israeliMemorialCalendar(HolidayName name)
    {
        return new IsraeliMemorialCalendarHolidayType(name, 0);
    }

    public static HolidayType israeliIndepenenceCalendar(HolidayName name)
    {
        return new IsraeliMemorialCalendarHolidayType(name, 1);
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

    public JewishCalendarDate jewishHoliday(int georg_year, int jewish_month, int jewish_day, int add)
    {
        CalendarImpl i = new CalendarImpl();

        int absDateGeogYear = i.absoluteFromGregorianDate(new JewishCalendarDate(1, 1, georg_year));
        int absDateNextGeogYear = i.absoluteFromGregorianDate(new JewishCalendarDate(31, 12, georg_year));

        JewishCalendarDate jewishDateSameYear = i.jewishDateFromAbsolute(absDateGeogYear);
        JewishCalendarDate jewishDateNextYear = i.jewishDateFromAbsolute(absDateNextGeogYear);

        int hebDay = jewish_day + add;
        int hebMonth = jewish_month;

        JewishCalendarDate JewishDateSameYear = new JewishCalendarDate(hebDay, hebMonth, jewishDateSameYear.getYear());
        JewishCalendarDate JewishDateNextYear = new JewishCalendarDate(hebDay, hebMonth, jewishDateNextYear.getYear());

        JewishCalendarDate GeorgeanDateSameYear = i
                        .gregorianDateFromAbsolute(i.absoluteFromJewishDate(JewishDateSameYear));
        JewishCalendarDate GeorgeanDateNextYear = i
                        .gregorianDateFromAbsolute(i.absoluteFromJewishDate(JewishDateNextYear));

        if (GeorgeanDateSameYear.getYear() == georg_year)
            return GeorgeanDateSameYear;

        return GeorgeanDateNextYear;
    }
}
