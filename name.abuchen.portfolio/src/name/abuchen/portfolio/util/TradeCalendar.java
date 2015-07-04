package name.abuchen.portfolio.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TradeCalendar
{
    private interface Holiday
    {
        boolean isOn(LocalDate date);
    }

    private static class Weekend implements Holiday
    {
        @Override
        public boolean isOn(LocalDate date)
        {
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            if (dayOfWeek == DayOfWeek.SATURDAY)
                return true;
            if (dayOfWeek == DayOfWeek.SUNDAY)
                return true;

            return false;
        }
    }

    private static class FixedHoliday implements Holiday
    {
        private final Month month;
        private final int dayOfMonth;

        public FixedHoliday(Month month, int dayOfMonth)
        {
            this.month = month;
            this.dayOfMonth = dayOfMonth;
        }

        @Override
        public boolean isOn(LocalDate date)
        {
            return date.getMonth() == month && date.getDayOfMonth() == dayOfMonth;
        }
    }

    private static class EasterHoliday implements Holiday
    {
        private Map<Integer, LocalDate> year2eastern = new HashMap<>();

        @Override
        public boolean isOn(LocalDate date)
        {
            LocalDate easterSunday = year2eastern.computeIfAbsent(date.getYear(), year -> calculateEasterSunday(year));

            if (easterSunday.plusDays(1).equals(date))
                return true;
            if (easterSunday.plusDays(-2).equals(date))
                return true;

            return false;
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

    private static final List<Holiday> HOLIDAYS = Arrays.asList(new Weekend(), //
                    new FixedHoliday(Month.JANUARY, 1), //
                    new FixedHoliday(Month.MAY, 1), //
                    new FixedHoliday(Month.DECEMBER, 24), //
                    new FixedHoliday(Month.DECEMBER, 25), //
                    new FixedHoliday(Month.DECEMBER, 26), //
                    new EasterHoliday());

    public boolean isHoliday(LocalDate date)
    {
        for (Holiday holiday : HOLIDAYS)
        {
            if (holiday.isOn(date))
                return true;
        }

        return false;
    }
}
