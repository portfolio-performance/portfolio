package name.abuchen.portfolio.util;

import java.time.LocalDate;
import java.util.Vector;

/*
 * Based on https://www.david-greve.de/luach-code/jewish-java.html#holidays
 */

public class TLVHolidayType
{
    static class CalendarDate
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
        /*
         * public static int getWeekday(int absDate) public int
         * getLastDayOfGregorianMonth(int month, int year) public int
         * absoluteFromGregorianDate(CalendarDate date) public CalendarDate
         * gregorianDateFromAbsolute(int absDate) public int
         * getLastMonthOfJewishYear(int year) public int
         * getLastDayOfJewishMonth(int month, int year) public int
         * absoluteFromJewishDate(CalendarDate date) public CalendarDate
         * jewishDateFromAbsolute(int absDate)
         */

        // ------------------------------------------------
        public static int getWeekday(int absDate)
        {
            return (absDate % 7);
        }

        private int month_list[] = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

        public int getLastDayOfGregorianMonth(int month, int year)
        {
            if ((month == 2) && ((year % 4) == 0) && ((year % 400) != 100) && ((year % 400) != 200)
                            && ((year % 400) != 300))
                return 29;
            else
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

    public Vector holidays = null;

    public TLVHolidayType()
    {
        CalendarDate startOfYear = new CalendarDate(23, 9, 2025);
        CalendarImpl calImp = new CalendarImpl();
        this.holidays = this.getHolidayForDate(startOfYear, calImp, false);

    }


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

    //@formatter:off
    /* Current Algo
    * Input: GeorgeDate
    * Convert go absolute Date
    * Convert to jewishDate
    * Check if JewishDate is a holiday
    * 
    * Needed Algo
    * Ask for Holiday, Year
    * Create 1.1.year as one Date, 31.12.year as anothe date
    * Calculare jewish years for both
    * Find out Jewish Month and Day for that holiday on both years
    * Convert back to Georgean
    * Return the date which matches the Georgean year 
    * 
    */
    //@formatter:on
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

    /***********************************************************/

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


    private CalendarDate JewishHoliday(int georg_year, int jewish_month, int jewish_day, int add)
    {
        CalendarImpl i = new CalendarImpl();
        
        int absDateStartOfGeogYear = i.absoluteFromGregorianDate(new CalendarDate(1, 1, georg_year));
        int absDateEndOfGeogYear = i.absoluteFromGregorianDate(new CalendarDate(31, 12, georg_year));

        CalendarDate jewishDateStart = i.jewishDateFromAbsolute(absDateStartOfGeogYear);
        CalendarDate jewishDateEnd = i.jewishDateFromAbsolute(absDateEndOfGeogYear);
        
        int hebDay = jewish_day + add;
        int hebMonth = jewish_month;

        CalendarDate JewishRoshStart = new CalendarDate(hebDay, hebMonth, jewishDateStart.getYear());
        CalendarDate JewishRoshEnd = new CalendarDate(hebDay, hebMonth, jewishDateEnd.getYear());

        CalendarDate GeorgeanRoshStart = i.gregorianDateFromAbsolute(i.absoluteFromJewishDate(JewishRoshStart));
        CalendarDate GeorgeanRoshEnd = i.gregorianDateFromAbsolute(i.absoluteFromJewishDate(JewishRoshStart));

        if (GeorgeanRoshStart.getYear() == georg_year)
            return GeorgeanRoshStart;
        return GeorgeanRoshEnd;

    }

    /*****************************
     * Remove at End
     ******************************/
    private static Vector getHolidayForDate(CalendarDate gdate, CalendarImpl i, boolean diaspora)
    {
        int absDate = i.absoluteFromGregorianDate(gdate);
        CalendarDate jewishDate = i.jewishDateFromAbsolute(absDate);
        int hebDay = jewishDate.getDay();
        int hebMonth = jewishDate.getMonth();
        int hebYear = jewishDate.getYear();

        Vector listHolidays = new Vector();

        // Holidays in Nisan

        int hagadolDay = 14;
        while (getWeekdayOfHebrewDate(hagadolDay, 1, hebYear, i) != 6)
            hagadolDay -= 1;
        if (hebDay == hagadolDay && hebMonth == 1)
            listHolidays.addElement("Shabat Hagadol");

        if (hebDay == 14 && hebMonth == 1)
            listHolidays.addElement("Erev Pesach");
        if (hebDay == 15 && hebMonth == 1)
            listHolidays.addElement("Pesach I");
        if (hebDay == 16 && hebMonth == 1)
        {
            if (diaspora)
            {
                listHolidays.addElement("Pesach II");
            }
            else
            {
                listHolidays.addElement("Chol Hamoed");
            }
        }
        if (hebDay == 17 && hebMonth == 1)
            listHolidays.addElement("Chol Hamoed");
        if (hebDay == 18 && hebMonth == 1)
            listHolidays.addElement("Chol Hamoed");
        if (hebDay == 19 && hebMonth == 1)
            listHolidays.addElement("Chol Hamoed");
        if (hebDay == 20 && hebMonth == 1)
            listHolidays.addElement("Chol Hamoed");
        if (hebDay == 21 && hebMonth == 1)
        {
            if (!diaspora)
                listHolidays.addElement("Pesach VII (Yizkor)");
            else
                listHolidays.addElement("Pesach VII");
        }
        if (hebDay == 22 && hebMonth == 1)
        {
            if (diaspora)
                listHolidays.addElement("Pesach VIII (Yizkor)");
        }

        // Yom Hashoah

        if (getWeekdayOfHebrewDate(27, 1, hebYear, i) == 5)
        {
            if (hebDay == 26 && hebMonth == 1)
                listHolidays.addElement("Yom Hashoah");
        }
        else if (hebYear >= 5757 && getWeekdayOfHebrewDate(27, 1, hebYear, i) == 0)
        {
            if (hebDay == 28 && hebMonth == 1)
                listHolidays.addElement("Yom Hashoah");
        }
        else
        {
            if (hebDay == 27 && hebMonth == 1)
                listHolidays.addElement("Yom Hashoah");
        }

        // Holidays in Iyar

        // Yom Hazikaron

        if (getWeekdayOfHebrewDate(4, 2, hebYear, i) == 5)
        { // If 4th of Iyar is a Thursday ...
            if (hebDay == 2 && hebMonth == 2) // ... then Yom Hazicaron is on
                                              // 2th of Iyar
                listHolidays.addElement("Yom Hazikaron");
        }
        else if (getWeekdayOfHebrewDate(4, 2, hebYear, i) == 4)
        {
            if (hebDay == 3 && hebMonth == 2)
                listHolidays.addElement("Yom Hazikaron");
        }
        else if (hebYear >= 5764 && getWeekdayOfHebrewDate(4, 2, hebYear, i) == 0)
        {
            if (hebDay == 5 && hebMonth == 2)
                listHolidays.addElement("Yom Hazikaron");
        }
        else
        {
            if (hebDay == 4 && hebMonth == 2)
                listHolidays.addElement("Yom Hazikaron");
        }

        // Yom Ha'Azmaut

        if (getWeekdayOfHebrewDate(5, 2, hebYear, i) == 6)
        {
            if (hebDay == 3 && hebMonth == 2)
                listHolidays.addElement("Yom Ha'Atzmaut");
        }
        else if (getWeekdayOfHebrewDate(5, 2, hebYear, i) == 5)
        {
            if (hebDay == 4 && hebMonth == 2)
                listHolidays.addElement("Yom Ha'Atzmaut");
        }
        else if (hebYear >= 5764 && getWeekdayOfHebrewDate(4, 2, hebYear, i) == 0)
        {
            if (hebDay == 6 && hebMonth == 2)
                listHolidays.addElement("Yom Ha'Atzmaut");
        }
        else
        {
            if (hebDay == 5 && hebMonth == 2)
                listHolidays.addElement("Yom Ha'Atzmaut");
        }
        if (hebDay == 14 && hebMonth == 2)
            listHolidays.addElement("Pesach Sheni");
        if (hebDay == 18 && hebMonth == 2)
            listHolidays.addElement("Lag BaOmer");
        if (hebDay == 28 && hebMonth == 2)
            listHolidays.addElement("Yom Yerushalayim");

        // Holidays in Sivan

        if (hebDay == 5 && hebMonth == 3)
            listHolidays.addElement("Erev Shavuot");
        if (hebDay == 6 && hebMonth == 3)
        {
            if (diaspora)
                listHolidays.addElement("Shavuot I");
            else
                listHolidays.addElement("Shavuot (Yizkor)");
        }
        if (hebDay == 7 && hebMonth == 3)
        {
            if (diaspora)
                listHolidays.addElement("Shavuot II (Yizkor)");
        }

        // Holidays in Tammuz

        if (getWeekdayOfHebrewDate(17, 4, hebYear, i) == 6)
        {
            if (hebDay == 18 && hebMonth == 4)
                listHolidays.addElement("Fast of Tammuz");
        }
        else
        {
            if (hebDay == 17 && hebMonth == 4)
                listHolidays.addElement("Fast of Tammuz");
        }

        // Holidays in Av

        if (getWeekdayOfHebrewDate(9, 5, hebYear, i) == 6)
        {
            if (hebDay == 10 && hebMonth == 5)
                listHolidays.addElement("Fast of Av");
        }
        else
        {
            if (hebDay == 9 && hebMonth == 5)
                listHolidays.addElement("Fast of Av");
        }
        if (hebDay == 15 && hebMonth == 5)
            listHolidays.addElement("Tu B'Av");

        // Holidays in Elul

        if (hebDay == 29 && hebMonth == 6)
            listHolidays.addElement("Erev Rosh Hashana");

        // Holidays in Tishri

        if (hebDay == 1 && hebMonth == 7)
            listHolidays.addElement("Rosh Hashana I");
        if (hebDay == 2 && hebMonth == 7)
            listHolidays.addElement("Rosh Hashana II");
        if (getWeekdayOfHebrewDate(3, 7, hebYear, i) == 6)
        {
            if (hebDay == 4 && hebMonth == 7)
                listHolidays.addElement("Tzom Gedaliah");
        }
        else
        {
            if (hebDay == 3 && hebMonth == 7)
                listHolidays.addElement("Tzom Gedaliah");
        }
        if (hebDay == 9 && hebMonth == 7)
            listHolidays.addElement("Erev Yom Kippur");
        if (hebDay == 10 && hebMonth == 7)
            listHolidays.addElement("Yom Kippur (Yizkor)");
        if (hebDay == 14 && hebMonth == 7)
            listHolidays.addElement("Erev Sukkot");
        if (hebDay == 15 && hebMonth == 7)
        {
            if (diaspora)
                listHolidays.addElement("Sukkot I");
            else
                listHolidays.addElement("Sukkot");
        }
        if (hebDay == 16 && hebMonth == 7)
        {
            if (diaspora)
                listHolidays.addElement("Sukkot II");
            else
                listHolidays.addElement("Chol Hamoed");
        }
        if (hebDay == 17 && hebMonth == 7)
            listHolidays.addElement("Chol Hamoed");
        if (hebDay == 18 && hebMonth == 7)
            listHolidays.addElement("Chol Hamoed");
        if (hebDay == 19 && hebMonth == 7)
            listHolidays.addElement("Chol Hamoed");
        if (hebDay == 20 && hebMonth == 7)
            listHolidays.addElement("Chol Hamoed");
        if (hebDay == 21 && hebMonth == 7)
            listHolidays.addElement("Hoshana Raba");
        if (hebDay == 22 && hebMonth == 7)
        {
            if (!diaspora)
            {
                listHolidays.addElement("Shemini Atzereth (Yizkor)");
                listHolidays.addElement("Simchat Torah");
            }
            else
            {
                listHolidays.addElement("Shemini Atzereth (Yizkor)");
            }
        }
        if (hebDay == 23 && hebMonth == 7)
        {
            if (diaspora)
                listHolidays.addElement("Simchat Torah");
        }

        // Holidays in Kislev

        if (hebDay == 25 && hebMonth == 9)
            listHolidays.addElement("Chanukka I");
        if (hebDay == 26 && hebMonth == 9)
            listHolidays.addElement("Chanukka II");
        if (hebDay == 27 && hebMonth == 9)
            listHolidays.addElement("Chanukka III");
        if (hebDay == 28 && hebMonth == 9)
            listHolidays.addElement("Chanukka IV");
        if (hebDay == 29 && hebMonth == 9)
            listHolidays.addElement("Chanukka V");

        // Holidays in Tevet

        if (hebDay == 10 && hebMonth == 10)
            listHolidays.addElement("Fast of Tevet");

        if (i.getLastDayOfJewishMonth(9, hebYear) == 30)
        {
            if (hebDay == 30 && hebMonth == 9)
                listHolidays.addElement("Chanukka VI");
            if (hebDay == 1 && hebMonth == 10)
                listHolidays.addElement("Chanukka VII");
            if (hebDay == 2 && hebMonth == 10)
                listHolidays.addElement("Chanukka VIII");
        }
        if (i.getLastDayOfJewishMonth(9, hebYear) == 29)
        {
            if (hebDay == 1 && hebMonth == 10)
                listHolidays.addElement("Chanukka VI");
            if (hebDay == 2 && hebMonth == 10)
                listHolidays.addElement("Chanukka VII");
            if (hebDay == 3 && hebMonth == 10)
                listHolidays.addElement("Chanukka VIII");
        }

        // Holidays in Shevat

        if (hebDay == 15 && hebMonth == 11)
            listHolidays.addElement("Tu B'Shevat");

        // Holidays in Adar (I)/Adar II

        int monthEsther;
        if (i.hebrewLeapYear(hebYear))
            monthEsther = 13;
        else
            monthEsther = 12;

        if (getWeekdayOfHebrewDate(13, monthEsther, hebYear, i) == 6)
        {
            if (hebDay == 11 && hebMonth == monthEsther)
                listHolidays.addElement("Fast of Esther");
        }
        else
        {
            if (hebDay == 13 && hebMonth == monthEsther)
                listHolidays.addElement("Fast of Esther");
        }

        if (hebDay == 14 && hebMonth == monthEsther)
            listHolidays.addElement("Purim");
        if (hebDay == 15 && hebMonth == monthEsther)
            listHolidays.addElement("Shushan Purim");

        if (i.hebrewLeapYear(hebYear))
        {
            if (hebDay == 14 && hebMonth == 12)
                listHolidays.addElement("Purim Katan");
            if (hebDay == 15 && hebMonth == 12)
                listHolidays.addElement("Shushan Purim Katan");
        }
        return listHolidays;
    }

    /********************************************************/

    // @Override
    protected Holiday doGetHoliday(int year)
    {
        // TODO Auto-generated method stub
        return null;
    }

    // public static HolidayType roshHashana(HolidayName name, int daysToAdd)
    // {
    // return new RelativeToRoshHashanaHolidayType(name, daysToAdd);
    // }

}
