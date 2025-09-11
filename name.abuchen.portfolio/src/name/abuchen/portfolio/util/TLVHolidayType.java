package name.abuchen.portfolio.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;





public class TLVHolidayType extends HolidayType
{
    private static class CalendarDate
    {
        CalendarDate(int day, int month, int year)
        {
            this.day = day;
            this.month = month;
            this.year = year;
        }

        CalendarDate(CalendarDate date)
        {
            this.day = date.getDay();
            this.month = date.getMonth();
            this.year = date.getYear();
        }

        public int getDay()
        {
            return day;
        }

        public int getMonth()
        {
            return month;
        }

        public int getYear()
        {
            return year;
        }

        public void setDay(int day)
        {
            this.day = day;
        }

        public void setMonth(int month)
        {
            this.month = month;
        }

        public void setYear(int year)
        {
            this.year = year;
        }

        public boolean areDatesEqual(CalendarDate date)
        {
            if ((day == date.getDay()) && (month == date.getMonth()) && (year == date.getYear()))
                return true;
            else
                return false;
        }

        public int getHashCode()
        {
            return (year - 1583) * 366 + month * 31 + day;
        }

        public String toString()
        {
            return day + "." + month + "." + year;
        }

        private int day;
        private int month;
        private int year;
    }

    private static class CalendarImpl
    {


        // public static int getWeekday(int absDate)
        // {
        // return (absDate % 7);
        // }
        //
        private int month_list[] = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

        public int getLastDayOfGregorianMonth(int month, int year)
        {
            if ((month == 2) && ((year % 4) == 0) && ((year % 400) != 100) && ((year % 400) != 200)
                            && ((year % 400) != 300))
                return 29;
            return month_list[month - 1];
        }

        public int absoluteFromGregorianDate(CalendarDate date)
        {
            int value, m;

            /* Days so far this month */
            value = date.getDay();

            /* Days in prior months this year */
            for (m = 1; m < date.getMonth(); m++)
                value += getLastDayOfGregorianMonth(m, date.getYear());

            /* Days in prior years */
            value += (365 * (date.getYear() - 1));

            /* Julian leap days in prior years ... */
            value += ((date.getYear() - 1) / 4);

            /* ... minus prior century years ... */
            value -= ((date.getYear() - 1) / 100);

            /* ... plus prior years divisible by 400 */
            value += ((date.getYear() - 1) / 400);

            return (value);
        }

        public CalendarDate gregorianDateFromAbsolute(int absDate)
        {
            int approx, y, m, day, month, year, temp;

            /* Approximation */
            approx = absDate / 366;

            /* Search forward from the approximation */
            y = approx;
            for (;;)
            {
                temp = absoluteFromGregorianDate(new CalendarDate(1, 1, y + 1));
                if (absDate < temp)
                    break;
                y++;
            }
            year = y;

            /* Search forward from January */
            m = 1;
            for (;;)
            {
                temp = absoluteFromGregorianDate(new CalendarDate(getLastDayOfGregorianMonth(m, year), m, year));
                if (absDate <= temp)
                    break;
                m++;
            }
            month = m;

            /* Calculate the day by subtraction */
            temp = absoluteFromGregorianDate(new CalendarDate(1, month, year));
            day = absDate - temp + 1;

            return new CalendarDate(day, month, year);
        }

        public boolean hebrewLeapYear(int year)
        {
            if ((((year * 7) + 1) % 19) < 7)
                return true;
            else
                return false;
        }

        public int getLastMonthOfJewishYear(int year)
        {
            if (hebrewLeapYear(year))
                return 13;
            else
                return 12;
        }

        public int getLastDayOfJewishMonth(int month, int year)
        {
            if ((month == 2) || (month == 4) || (month == 6) || (month == 10) || (month == 13))
                return 29;
            if ((month == 12) && (!hebrewLeapYear(year)))
                return 29;
            if ((month == 8) && (!longHeshvan(year)))
                return 29;
            if ((month == 9) && (shortKislev(year)))
                return 29;
            return 30;
        }

        private int hebrewCalendarElapsedDays(int year)
        {
            int value, monthsElapsed, partsElapsed, hoursElapsed;
            int day, parts, alternativeDay;

            /* Months in complete cycles so far */
            value = 235 * ((year - 1) / 19);
            monthsElapsed = value;

            /* Regular months in this cycle */
            value = 12 * ((year - 1) % 19);
            monthsElapsed += value;

            /* Leap months this cycle */
            value = ((((year - 1) % 19) * 7) + 1) / 19;
            monthsElapsed += value;

            partsElapsed = (((monthsElapsed % 1080) * 793) + 204);
            hoursElapsed = (5 + (monthsElapsed * 12) + ((monthsElapsed / 1080) * 793) + (partsElapsed / 1080));

            /* Conjunction day */
            day = 1 + (29 * monthsElapsed) + (hoursElapsed / 24);

            /* Conjunction parts */
            parts = ((hoursElapsed % 24) * 1080) + (partsElapsed % 1080);

            /* If new moon is at or after midday, */
            if ((parts >= 19440) ||

            /* ...or is on a Tuesday... */
                            (((day % 7) == 2) &&
                            /* at 9 hours, 204 parts or later */
                                            (parts >= 9924) &&
                                            /* of a common year */
                                            (!hebrewLeapYear(year)))
                            ||

                            /* ...or is on a Monday at... */
                            (((day % 7) == 1) &&
                            /* 15 hours, 589 parts or later... */
                                            (parts >= 16789) &&
                                            /* at the end of a leap year */
                                            (hebrewLeapYear(year - 1))))
                /* Then postpone Rosh HaShanah one day */
                alternativeDay = day + 1;
            else
                alternativeDay = day;

            /* If Rosh HaShanah would occur on Sunday, Wednesday, */
            /* or Friday */
            if (((alternativeDay % 7) == 0) || ((alternativeDay % 7) == 3) || ((alternativeDay % 7) == 5))
                /* Then postpone it one (more) day and return */
                alternativeDay++;

            return (alternativeDay);
        }

        private int daysInHebrewYear(int year)
        {
            return (hebrewCalendarElapsedDays(year + 1) - hebrewCalendarElapsedDays(year));
        }

        private boolean longHeshvan(int year)
        {
            if ((daysInHebrewYear(year) % 10) == 5)
                return true;
            else
                return false;
        }

        private boolean shortKislev(int year)
        {
            if ((daysInHebrewYear(year) % 10) == 3)
                return true;
            else
                return false;
        }

        public int absoluteFromJewishDate(CalendarDate date)
        {
            int value, returnValue, m;

            /* Days so far this month */
            value = date.getDay();
            returnValue = value;

            /* If before Tishri */
            if (date.getMonth() < 7)
            {
                /* Then add days in prior months this year before and */
                /* after Nisan. */
                for (m = 7; m <= getLastMonthOfJewishYear(date.getYear()); m++)
                {
                    value = getLastDayOfJewishMonth(m, date.getYear());
                    returnValue += value;
                }
                for (m = 1; m < date.getMonth(); m++)
                {
                    value = getLastDayOfJewishMonth(m, date.getYear());
                    returnValue += value;
                }
            }
            else
            {
                for (m = 7; m < date.getMonth(); m++)
                {
                    value = getLastDayOfJewishMonth(m, date.getYear());
                    returnValue += value;
                }
            }

            /* Days in prior years */
            value = hebrewCalendarElapsedDays(date.getYear());
            returnValue += value;

            /* Days elapsed before absolute date 1 */
            value = 1373429;
            returnValue -= value;

            return (returnValue);
        }

        public CalendarDate jewishDateFromAbsolute(int absDate)
        {
            int approx, y, m, year, month, day, temp, start;

            /* Approximation */
            approx = (absDate + 1373429) / 366;

            /* Search forward from the approximation */
            y = approx;
            for (;;)
            {
                temp = absoluteFromJewishDate(new CalendarDate(1, 7, y + 1));
                if (absDate < temp)
                    break;
                y++;
            }
            year = y;

            /* Starting month for search for month */
            temp = absoluteFromJewishDate(new CalendarDate(1, 1, year));
            if (absDate < temp)
                start = 7;
            else
                start = 1;

            /* Search forward from either Tishri or Nisan */
            m = start;
            for (;;)
            {
                temp = absoluteFromJewishDate(new CalendarDate(getLastDayOfJewishMonth(m, year), m, year));
                if (absDate <= temp)
                    break;
                m++;
            }
            month = m;

            /* Calculate the day by subtraction */
            temp = absoluteFromJewishDate(new CalendarDate(1, month, year));
            day = absDate - temp + 1;

            return new CalendarDate(day, month, year);
        }
        // ------------------------------------------------
    }

    private static class MoveIf
    {
        private final DayOfWeek dayOfWeek;
        private final int daysToAdd;

        @SuppressWarnings("unused")
        public MoveIf(DayOfWeek dayOfWeek, int daysToAdd)
        {
            this.dayOfWeek = dayOfWeek;
            this.daysToAdd = daysToAdd;
        }

        @SuppressWarnings("unused")
        public LocalDate apply(LocalDate date)
        {
            return date.getDayOfWeek() == dayOfWeek ? date.plusDays(daysToAdd) : date;
        }
    }

    private static class FixedJewishCalendarHolidayType extends TLVHolidayType
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
            CalendarDate jewishDate = JewishHoliday(year, this.jewishMonth, this.jewishDayOfMonth, this.dayToAdd);
            LocalDate date = LocalDate.of(jewishDate.getYear(), jewishDate.getMonth(), jewishDate.getDay());
            return new Holiday(getName(), date);
        }
    }

    private static final HolidayName EMPTY = null;

    /******************************************************************/
    @SuppressWarnings("unused")
    private final HolidayName name;

    @SuppressWarnings("unused")
    private int validFrom = -1;
    @SuppressWarnings("unused")
    private int validTo = -1;
    @SuppressWarnings("unused")
    private final Set<Integer> exceptIn = new HashSet<>();

    @SuppressWarnings("unused")
    private final List<MoveIf> moveIf = new ArrayList<>();

    @SuppressWarnings("unused")
    private DayOfWeek moveTo = null;

    public TLVHolidayType(HolidayName name)
    {
        super(name);
        this.name = name;

    }


    public static TLVHolidayType fixedJewishDate(HolidayName name, int jewishMonth, int jewishDayOfMonth,
                    int daysToAdd)
    {
        return new FixedJewishCalendarHolidayType(name, jewishMonth, jewishDayOfMonth, daysToAdd);
    }


    
    /******************
     * Based on https://www.david-greve.de/luach-code/jewish-java.html#holidays
     * Source code Copyright © by Ulrich and David Greve (2005)
     ******************/
    @SuppressWarnings("unused")
    private boolean hebrewLeapYear(int year)
    {
        if ((((year * 7) + 1) % 19) < 7)
            return true;
        else
            return false;
    }

    private static int getWeekdayOfHebrewDate(int hebDay, int hebMonth, int hebYear, CalendarImpl i)
    {
        int absDate = i.absoluteFromJewishDate(new CalendarDate(hebDay, hebMonth, hebYear));
        return absDate % 7;
    }


    public LocalDate getErevPassover(int year)
    {
        CalendarDate date = JewishHoliday(year, 1, 14, 0);
        return LocalDate.of(date.getYear(), date.getMonth(), date.getDay());
    }

    public LocalDate getPassoverI(int year)
    {
        CalendarDate date = JewishHoliday(year, 1, 14, 1);
        return LocalDate.of(date.getYear(), date.getMonth(), date.getDay());
    }

    public LocalDate getPassoverII(int year)
    {
        CalendarDate date = JewishHoliday(year, 1, 14, 2);
        return LocalDate.of(date.getYear(), date.getMonth(), date.getDay());
    }

    public LocalDate getLagBaOmer(int year)
    {
        CalendarDate date = JewishHoliday(year, 2, 18, 0);
        return LocalDate.of(date.getYear(), date.getMonth(), date.getDay());
    }

    public LocalDate getYomYerushalayim(int year)
    {
        CalendarDate date = JewishHoliday(year, 2, 28, 0);
        return LocalDate.of(date.getYear(), date.getMonth(), date.getDay());
    }

    public LocalDate getErevShavuot(int year)
    {
        CalendarDate date = JewishHoliday(year, 3, 5, 0);
        return LocalDate.of(date.getYear(), date.getMonth(), date.getDay());
    }


    public LocalDate getPurim(int georg_year)
    {

        CalendarImpl i = new CalendarImpl();
        int absDateStartOfGeogYear = i.absoluteFromGregorianDate(new CalendarDate(1, 1, georg_year));

        CalendarDate jewishDateStart = i.jewishDateFromAbsolute(absDateStartOfGeogYear);

        CalendarDate hebDate = calcPurimDateHeb(i, jewishDateStart.getYear(), georg_year);
        CalendarDate geogDate = i.gregorianDateFromAbsolute(i.absoluteFromJewishDate(hebDate));

        return LocalDate.of(geogDate.getYear(), geogDate.getMonth(), geogDate.getDay());
    }



    public LocalDate YomHashoah(int georg_year)
    {
        CalendarImpl i = new CalendarImpl();
        int absDateStartOfGeogYear = i.absoluteFromGregorianDate(new CalendarDate(1, 1, georg_year));

        CalendarDate jewishDateStart = i.jewishDateFromAbsolute(absDateStartOfGeogYear);

        CalendarDate hebDate = calcYomHashoahDateHeb(i, jewishDateStart.getYear(), georg_year);
        CalendarDate geogDate = i.gregorianDateFromAbsolute(i.absoluteFromJewishDate(hebDate));

        return LocalDate.of(geogDate.getYear(), geogDate.getMonth(), geogDate.getDay());
    }

    public LocalDate MemorialDay(int georg_year)
    {
        CalendarImpl i = new CalendarImpl();
        int absDateStartOfGeogYear = i.absoluteFromGregorianDate(new CalendarDate(1, 1, georg_year));

        CalendarDate jewishDateStart = i.jewishDateFromAbsolute(absDateStartOfGeogYear);
        CalendarDate hebDate = calcYomHazikaron(i, jewishDateStart.getYear(), georg_year);
        CalendarDate geogDate = i.gregorianDateFromAbsolute(i.absoluteFromJewishDate(hebDate));

        return LocalDate.of(geogDate.getYear(), geogDate.getMonth(), geogDate.getDay());
    }

    public LocalDate YomHaAtzmaut(int georg_year)
    {
        return MemorialDay(georg_year).plusDays(1);
    }

    public LocalDate getErevRoshHashanah(int georgYear)
    {
        CalendarDate date = JewishHoliday(georgYear, 6, 29, 0);
        return LocalDate.of(date.getYear(), date.getMonth(), date.getDay());
    }

    public LocalDate getRoshHashanahI(int georgYear)
    {
        return getErevRoshHashanah(georgYear).plusDays(1);
    }

    public LocalDate getRoshHashanahII(int georgYear)
    {
        return getErevRoshHashanah(georgYear).plusDays(2);
    }


    public LocalDate getErevYomKippur(int georgYear)
    {
        CalendarDate date = JewishHoliday(georgYear, 7, 9, 0);
        return LocalDate.of(date.getYear(), date.getMonth(), date.getDay());
    }

    public LocalDate getYomKippur(int georgYear)
    {
        return getErevYomKippur(georgYear).plusDays(1);
    }

    public LocalDate getTishBAv(int georgYear)
    {
        CalendarDate date = JewishHoliday(georgYear, 5, 9, 0);
        return LocalDate.of(date.getYear(), date.getMonth(), date.getDay());
    }

    public LocalDate getErevSukkot(int georgYear)
    {
        CalendarDate date = JewishHoliday(georgYear, 7, 14, 0);
        return LocalDate.of(date.getYear(), date.getMonth(), date.getDay());
    }

    public LocalDate getSukkot(int georgYear)
    {
        CalendarDate date = JewishHoliday(georgYear, 7, 15, 0);
        return LocalDate.of(date.getYear(), date.getMonth(), date.getDay());
    }

    public LocalDate getErevSimhatTorah(int georgYear)
    {
        CalendarDate date = JewishHoliday(georgYear, 7, 21, 0);
        return LocalDate.of(date.getYear(), date.getMonth(), date.getDay());
    }

    public LocalDate getSimhatTorah(int georgYear)
    {
        CalendarDate date = JewishHoliday(georgYear, 7, 22, 0);
        return LocalDate.of(date.getYear(), date.getMonth(), date.getDay());
    }


    /***********************************************************/

    private CalendarDate calcPurimDateHeb(CalendarImpl i, int hebYear, int georgYear)
    {

        int monthEsther;
        if (i.hebrewLeapYear(hebYear))
            monthEsther = 13;
        else
            monthEsther = 12;

        return new CalendarDate(14, monthEsther, hebYear);

    }

    private CalendarDate calcYomHazikaron(CalendarImpl i, int hebYear, int georg_year)
    {
        if (getWeekdayOfHebrewDate(4, 2, hebYear, i) == 5)
        {
            return new CalendarDate(2, 2, hebYear);
        }
        else if (getWeekdayOfHebrewDate(4, 2, hebYear, i) == 4)
        {
            return new CalendarDate(3, 2, hebYear);
        }
        else if (hebYear >= 5764 && getWeekdayOfHebrewDate(4, 2, hebYear, i) == 0)
        {
            return new CalendarDate(5, 3, hebYear);
        }
        else
        {
            return new CalendarDate(4, 3, hebYear);
        }

    }
    private CalendarDate calcYomHashoahDateHeb(CalendarImpl i, int hebYear, int georg_year)
    {
        if (getWeekdayOfHebrewDate(27, 1, hebYear, i) == 5)
        {
            return new CalendarDate(26, 1, hebYear);
        }
        else if (hebYear >= 5757 && getWeekdayOfHebrewDate(27, 1, hebYear, i) == 0)
        {
            return new CalendarDate(28, 1, hebYear);
        }
        else
        {
            return new CalendarDate(27, 1, hebYear);
        }
    }


    public JewishCalendarDate JewishHoliday(int georg_year, int jewish_month, int jewish_day, int add)
    {
        CalendarImpl i = new CalendarImpl();
        
        int absDateGeogYear = i.absoluteFromGregorianDate(new CalendarDate(1, 1, georg_year));
        int absDateNextGeogYear = i.absoluteFromGregorianDate(new CalendarDate(31, 12, georg_year));

        CalendarDate jewishDateSameYear = i.jewishDateFromAbsolute(absDateGeogYear);
        CalendarDate jewishDateNextYear = i.jewishDateFromAbsolute(absDateNextGeogYear);
        
        int hebDay = jewish_day + add;
        int hebMonth = jewish_month;

        CalendarDate JewishDateSameYear = new CalendarDate(hebDay, hebMonth, jewishDateSameYear.getYear());
        CalendarDate JewishDateNextYear = new CalendarDate(hebDay, hebMonth, jewishDateNextYear.getYear());

        CalendarDate GeorgeanDateSameYear = i.gregorianDateFromAbsolute(i.absoluteFromJewishDate(JewishDateSameYear));
        CalendarDate GeorgeanDateNextYear = i.gregorianDateFromAbsolute(i.absoluteFromJewishDate(JewishDateNextYear));


        if (GeorgeanDateSameYear.getYear() == georg_year)
            return GeorgeanDateSameYear;

        return GeorgeanDateNextYear;
    }

    @Override
    protected Holiday doGetHoliday(int year)
    {
        // TODO Auto-generated method stub
        return null;
    }


}
