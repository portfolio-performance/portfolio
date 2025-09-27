package name.abuchen.portfolio.util;

import java.time.LocalDate;

/**
 * Hebrew Calendar implementation for date conversion between Gregorian and
 * Hebrew calendars. Based on algorithms from David Greve's implementation:
 * https://www.david-greve.de/luach-code/jewish-java.html
 */
/* package */ class HebrewCalendar
{
    private static final int[] GREGORIAN_MONTH_DAYS = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

    /** Days elapsed before absolute date 1 */
    private static final int HEBREW_EPOCH = 1373429;

    /** Months in 19-year Metonic cycle */
    private static final int METONIC_CYCLE_MONTHS = 235;
    private static final int METONIC_CYCLE_YEARS = 19;
    private static final int PARTS_PER_HOUR = 1080;
    private static final int HOURS_PER_DAY = 24;

    /**
     * Returns the last day of a Gregorian month, accounting for leap years.
     */
    private int getLastDayOfGregorianMonth(int month, int year)
    {
        if (month == 2 && isGregorianLeapYear(year))
            return 29;
        return GREGORIAN_MONTH_DAYS[month - 1];
    }

    /**
     * Checks if a Gregorian year is a leap year.
     */
    private boolean isGregorianLeapYear(int year)
    {
        return (year % 4 == 0) && (year % 100 != 0 || year % 400 == 0);
    }

    /**
     * Converts a Gregorian date to absolute day number.
     */
    public int getAbsoluteFromGregorianDate(LocalDate gregorianDate)
    {
        var absoluteDay = gregorianDate.getDayOfMonth();

        // Add days from previous months in current year
        for (var month = 1; month < gregorianDate.getMonthValue(); month++)
        {
            absoluteDay += getLastDayOfGregorianMonth(month, gregorianDate.getYear());
        }

        var year = gregorianDate.getYear();
        var priorYears = year - 1;

        // Add days from previous years
        absoluteDay += 365 * priorYears;
        absoluteDay += priorYears / 4; // Julian leap days
        absoluteDay -= priorYears / 100; // Century years (not leap)
        absoluteDay += priorYears / 400; // 400-year leap years

        return absoluteDay;
    }

    /**
     * Converts absolute day number to Gregorian date.
     */
    public LocalDate getGregorianDateFromAbsolute(int absoluteDate)
    {
        // Approximate year
        var year = absoluteDate / 366;

        // Find exact year
        while (getAbsoluteFromGregorianDate(LocalDate.of(year + 1, 1, 1)) <= absoluteDate)
        {
            year++;
        }

        // Find month
        var month = 1;
        while (getAbsoluteFromGregorianDate(
                        LocalDate.of(year, month, getLastDayOfGregorianMonth(month, year))) < absoluteDate)
        {
            month++;
        }

        // Calculate day
        var day = absoluteDate - getAbsoluteFromGregorianDate(LocalDate.of(year, month, 1)) + 1;

        return LocalDate.of(year, month, day);
    }

    /**
     * Determines if a Hebrew year is a leap year.
     */
    public boolean isHebrewLeapYear(int year)
    {
        return ((year * 7 + 1) % METONIC_CYCLE_YEARS) < 7;
    }

    /**
     * Returns the last month of a Hebrew year (12 or 13).
     */
    public int getLastMonthOfHebrewYear(int year)
    {
        return isHebrewLeapYear(year) ? 13 : 12;
    }

    /**
     * Returns the number of days in a Hebrew month.
     */
    public int getLastDayOfHebrewMonth(int month, int year)
    {
        // Months with 29 days: Iyyar(2), Tammuz(4), Elul(6), Tevet(10),
        // Adar II(13)
        if (month == 2 || month == 4 || month == 6 || month == 10 || month == 13)
            return 29;

        // Adar in non-leap year has 29 days
        if (month == 12 && !isHebrewLeapYear(year))
            return 29;

        // Heshvan can have 29 days (short year)
        if (month == 8 && !isLongHeshvan(year))
            return 29;

        // Kislev can have 29 days (deficient year)
        if (month == 9 && isShortKislev(year))
            return 29;

        return 30; // Default month length
    }

    /**
     * Calculates days elapsed since Hebrew calendar epoch for given year.
     */
    private int hebrewCalendarElapsedDays(int year)
    {
        var priorYear = year - 1;

        // Calculate months elapsed until start of given year
        var monthsElapsed = METONIC_CYCLE_MONTHS * (priorYear / METONIC_CYCLE_YEARS);
        monthsElapsed += 12 * (priorYear % METONIC_CYCLE_YEARS);
        monthsElapsed += ((priorYear % METONIC_CYCLE_YEARS) * 7 + 1) / METONIC_CYCLE_YEARS;

        // Calculate conjunction time
        var partsElapsed = (monthsElapsed % PARTS_PER_HOUR) * 793 + 204;
        var hoursElapsed = 5 + monthsElapsed * 12 + (monthsElapsed / PARTS_PER_HOUR) * 793
                        + (partsElapsed / PARTS_PER_HOUR);

        var conjunctionDay = 1 + 29 * monthsElapsed + hoursElapsed / HOURS_PER_DAY;
        var conjunctionParts = (hoursElapsed % HOURS_PER_DAY) * PARTS_PER_HOUR + (partsElapsed % PARTS_PER_HOUR);

        // Apply Rosh Hashanah postponement rules
        var roshHashanah = conjunctionDay;

        // Rule 1: If molad is at or after midday
        if (conjunctionParts >= 19440 ||
        // Rule 2: Tuesday molad in common year after 9:204
                        (conjunctionDay % 7 == 2 && conjunctionParts >= 9924 && !isHebrewLeapYear(year)) ||
                        // Rule 3: Monday molad after leap year after 15:589
                        (conjunctionDay % 7 == 1 && conjunctionParts >= 16789 && isHebrewLeapYear(year - 1)))
        {
            roshHashanah++;
        }

        // Rule 4: Avoid Sunday, Wednesday, Friday
        var dayOfWeek = roshHashanah % 7;
        if (dayOfWeek == 0 || dayOfWeek == 3 || dayOfWeek == 5)
        {
            roshHashanah++;
        }

        return roshHashanah;
    }

    /**
     * Returns the number of days in a Hebrew year.
     */
    private int getDaysInHebrewYear(int year)
    {
        return hebrewCalendarElapsedDays(year + 1) - hebrewCalendarElapsedDays(year);
    }

    /**
     * Determines if Heshvan has 30 days (long year).
     */
    private boolean isLongHeshvan(int year)
    {
        return getDaysInHebrewYear(year) % 10 == 5;
    }

    /**
     * Determines if Kislev has 29 days (deficient year).
     */
    private boolean isShortKislev(int year)
    {
        return getDaysInHebrewYear(year) % 10 == 3;
    }

    /**
     * Converts Hebrew date to absolute day number.
     */
    public int getAbsoluteFromHebrewDate(HebrewDate date)
    {
        var absoluteDay = date.getDay();

        // Add days from previous months in current year
        if (date.getMonth() < 7)
        {
            // Before Tishri: add months from Tishri to end of year, then
            // Nisan to current month
            for (var month = 7; month <= getLastMonthOfHebrewYear(date.getYear()); month++)
            {
                absoluteDay += getLastDayOfHebrewMonth(month, date.getYear());
            }
            for (var month = 1; month < date.getMonth(); month++)
            {
                absoluteDay += getLastDayOfHebrewMonth(month, date.getYear());
            }
        }
        else
        {
            // After/including Tishri: add months from Tishri to current
            // month
            for (var month = 7; month < date.getMonth(); month++)
            {
                absoluteDay += getLastDayOfHebrewMonth(month, date.getYear());
            }
        }

        // Add days from previous years and adjust for epoch
        absoluteDay += hebrewCalendarElapsedDays(date.getYear()) - HEBREW_EPOCH;

        return absoluteDay;
    }

    /**
     * Converts absolute day number to Hebrew date.
     */
    public HebrewDate getHebrewDateFromAbsolute(int absoluteDate)
    {
        // Approximate year
        var year = (absoluteDate + HEBREW_EPOCH) / 366;

        // Find exact year
        while (getAbsoluteFromHebrewDate(HebrewDate.of(year + 1, 7, 1)) <= absoluteDate)
        {
            year++;
        }

        // Determine starting month for search
        var startMonth = getAbsoluteFromHebrewDate(HebrewDate.of(year, 1, 1)) <= absoluteDate ? 1 : 7;

        // Find month with protection against infinite loops
        var month = startMonth;
        var monthsChecked = 0;
        var maxMonths = getLastMonthOfHebrewYear(year);

        while (getAbsoluteFromHebrewDate(
                        HebrewDate.of(year, month, getLastDayOfHebrewMonth(month, year))) < absoluteDate)
        {
            month++;
            monthsChecked++;

            // Wrap around if necessary
            if (month > maxMonths)
            {
                month = 1;
            }

            // Prevent infinite loop - if we've checked all months twice,
            // break
            if (monthsChecked > maxMonths * 2)
            {
                throw new IllegalArgumentException(
                                "Unable to find valid Hebrew month for absolute date: " + absoluteDate); //$NON-NLS-1$
            }
        }

        // Calculate day
        var day = absoluteDate - getAbsoluteFromHebrewDate(HebrewDate.of(year, month, 1)) + 1;

        // Validate the calculated day
        if (day < 1 || day > getLastDayOfHebrewMonth(month, year))
        {
            throw new IllegalArgumentException(
                            "Calculated day " + day + " is invalid for Hebrew month " + month + " in year " + year); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }

        return HebrewDate.of(year, month, day);
    }

    /**
     * Immutable date representation for both Gregorian and Hebrew calendars.
     */
    public static class HebrewDate
    {
        private final int year;
        private final int month;
        private final int day;

        /**
         * Creates a new calendar date.
         *
         * @param day
         *            Day of month (1-31)
         * @param month
         *            Month (1-13)
         * @param year
         *            Year
         */
        private HebrewDate(int year, int month, int day)
        {
            if (day < 1 || day > 31)
            {
                throw new IllegalArgumentException("Day must be between 1 and 31: " + day); //$NON-NLS-1$
            }
            if (month < 1 || month > 13)
            {
                throw new IllegalArgumentException("Month must be between 1 and 13: " + month); //$NON-NLS-1$
            }
            if (year < 1)
            {
                throw new IllegalArgumentException("Year must be positive: " + year); //$NON-NLS-1$
            }

            this.day = day;
            this.month = month;
            this.year = year;
        }

        public static HebrewDate of(int year, int month, int day)
        {
            return new HebrewDate(year, month, day);
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
    }
}
