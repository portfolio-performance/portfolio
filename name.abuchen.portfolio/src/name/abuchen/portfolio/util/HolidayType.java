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
