package name.abuchen.portfolio.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.Test;

@SuppressWarnings("nls")
public class TradeCalendarTest
{
    @Test
    public void testEasterHolidays()
    {
        TradeCalendar calendar = TradeCalendarManager.getInstance("de");

        assertThat(calendar.isHoliday(LocalDate.parse("2015-04-02")), is(false));
        assertThat(calendar.isHoliday(LocalDate.parse("2015-04-03")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2015-04-04")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2015-04-05")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2015-04-06")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2015-04-07")), is(false));

        assertThat(calendar.isHoliday(LocalDate.parse("2016-03-25")), is(true));
    }

    @Test
    public void testWeekends()
    {
        TradeCalendar calendar = TradeCalendarManager.getInstance("de");

        assertThat(calendar.isHoliday(LocalDate.parse("2015-01-31")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2015-02-01")), is(true));
    }

    @Test
    public void testFixedPublicHolidays()
    {
        TradeCalendar calendar = TradeCalendarManager.getInstance("de");

        assertThat(calendar.isHoliday(LocalDate.parse("2015-01-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2015-05-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2015-12-24")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2015-12-25")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2015-12-26")), is(true));

        assertThat(calendar.isHoliday(LocalDate.parse("1980-06-17")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("1990-06-17")), is(true)); // German Unity Day and Sunday
        assertThat(calendar.isHoliday(LocalDate.parse("1991-06-17")), is(false));

        assertThat(calendar.isHoliday(LocalDate.parse("1989-10-03")), is(false));
        assertThat(calendar.isHoliday(LocalDate.parse("1990-10-03")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("1991-10-03")), is(true));

        assertThat(calendar.isHoliday(LocalDate.parse("2008-05-01")), is(true)); // Ascension and Labour Day coincide
    }

    @Test
    public void testDayOfRepentanceAndPrayer()
    {
        TradeCalendar calendar = TradeCalendarManager.getInstance("de");

        assertThat(calendar.isHoliday(LocalDate.parse("1989-11-22")), is(true)); // latest possible day
        assertThat(calendar.isHoliday(LocalDate.parse("1990-11-21")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("1991-11-20")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("1992-11-18")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("1993-11-17")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("1994-11-16")), is(true)); // earliest possible day
        assertThat(calendar.isHoliday(LocalDate.parse("1995-11-22")), is(false));
    }

    @Test
    public void testGermanHolidaysWithTrading()
    {
        TradeCalendar calendar = TradeCalendarManager.getInstance("de");

        // Ascension
        assertThat(calendar.isHoliday(LocalDate.parse("1999-05-13")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2000-06-01")), is(false)); // trading despite public holiday
        assertThat(calendar.isHoliday(LocalDate.parse("2013-05-09")), is(false)); // trading despite public holiday
        assertThat(calendar.isHoliday(LocalDate.parse("2014-05-29")), is(false)); // trading despite public holiday
        assertThat(calendar.isHoliday(LocalDate.parse("2015-05-14")), is(false)); // trading despite public holiday
        assertThat(calendar.isHoliday(LocalDate.parse("2021-05-13")), is(false)); // trading despite public holiday

        // Whit Monday
        assertThat(calendar.isHoliday(LocalDate.parse("1999-05-24")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2000-06-12")), is(false)); // trading despite public holiday
        assertThat(calendar.isHoliday(LocalDate.parse("2006-06-05")), is(false)); // trading despite public holiday
        assertThat(calendar.isHoliday(LocalDate.parse("2007-05-28")), is(true)); // no trading only because Whit Monday coincides with U.S. Memorial Day
        assertThat(calendar.isHoliday(LocalDate.parse("2008-05-12")), is(false)); // trading despite public holiday
        assertThat(calendar.isHoliday(LocalDate.parse("2013-05-20")), is(false)); // trading despite public holiday
        assertThat(calendar.isHoliday(LocalDate.parse("2014-06-09")), is(false)); // trading despite public holiday
        assertThat(calendar.isHoliday(LocalDate.parse("2015-05-25")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-05-24")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-06-06")), is(false)); // trading despite public holiday

        // Corpus Christi
        assertThat(calendar.isHoliday(LocalDate.parse("1999-06-03")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2000-06-22")), is(false)); // trading despite public holiday
        assertThat(calendar.isHoliday(LocalDate.parse("2013-05-30")), is(false)); // trading despite public holiday
        assertThat(calendar.isHoliday(LocalDate.parse("2014-06-19")), is(false)); // trading despite public holiday
        assertThat(calendar.isHoliday(LocalDate.parse("2015-06-04")), is(false)); // trading despite public holiday
        assertThat(calendar.isHoliday(LocalDate.parse("2021-06-03")), is(false)); // trading despite public holiday

        // German Unity Day
        assertThat(calendar.isHoliday(LocalDate.parse("1999-10-03")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2000-10-03")), is(true)); // no trading (despite other plans) because of union protests
        assertThat(calendar.isHoliday(LocalDate.parse("2001-10-03")), is(false)); // trading despite public holiday
        assertThat(calendar.isHoliday(LocalDate.parse("2013-10-03")), is(false)); // trading despite public holiday
        assertThat(calendar.isHoliday(LocalDate.parse("2014-10-03")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-10-03")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-10-03")), is(false)); // trading despite public holiday
    }

    @Test
    public void testHolidaysWithCondition()
    {
        TradeCalendar calendar = TradeCalendarManager.getInstance("nyse");

        assertThat(calendar.isHoliday(LocalDate.parse("2019-10-29")), is(false));
        assertThat(calendar.isHoliday(LocalDate.parse("2013-10-29")), is(false));
        assertThat(calendar.isHoliday(LocalDate.parse("2012-10-29")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2011-10-29")), is(true)); // weekend!
        assertThat(calendar.isHoliday(LocalDate.parse("2019-10-29")), is(false));
    }

    @Test
    public void testMovingHolidays()
    {
        TradeCalendar calendar = TradeCalendarManager.getInstance("nyse");

        assertThat(calendar.isHoliday(LocalDate.parse("2018-12-25")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2018-12-26")), is(false));

        assertThat(calendar.isHoliday(LocalDate.parse("2016-12-25")), is(true)); // weekend
        assertThat(calendar.isHoliday(LocalDate.parse("2016-12-26")), is(true));

        assertThat(calendar.isHoliday(LocalDate.parse("2015-12-24")), is(false));
        assertThat(calendar.isHoliday(LocalDate.parse("2015-12-25")), is(true)); // Friday
        assertThat(calendar.isHoliday(LocalDate.parse("2015-12-26")), is(true));

        assertThat(calendar.isHoliday(LocalDate.parse("2010-12-24")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2010-12-25")), is(true)); // Saturday
        assertThat(calendar.isHoliday(LocalDate.parse("2010-12-26")), is(true));

        assertThat(calendar.isHoliday(LocalDate.parse("2021-12-31")), is(false));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-01-01")), is(true)); // Saturday and New Year, not moved
        assertThat(calendar.isHoliday(LocalDate.parse("2022-01-02")), is(true)); // Sunday
        assertThat(calendar.isHoliday(LocalDate.parse("2022-01-03")), is(false));

        assertThat(calendar.isHoliday(LocalDate.parse("2022-12-30")), is(false));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-12-31")), is(true)); // Saturday
        assertThat(calendar.isHoliday(LocalDate.parse("2023-01-01")), is(true)); // Sunday (and New Year)
        assertThat(calendar.isHoliday(LocalDate.parse("2023-01-02")), is(true)); // Monday (New Year observed)
        assertThat(calendar.isHoliday(LocalDate.parse("2023-01-03")), is(false));
    }

    @Test
    public void testTradeCalenderNYSE()
    {
        // See https://www.nyse.com/markets/hours-calendars

        TradeCalendar calendar = TradeCalendarManager.getInstance("nyse");

        // New Year
        assertThat(calendar.isHoliday(LocalDate.parse("2020-01-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-01-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-01-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-01-01")), is(true));

        // Martin Luther King day
        assertThat(calendar.isHoliday(LocalDate.parse("2020-01-20")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-01-18")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-01-17")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-01-16")), is(true));

        // Washington's Birthday
        assertThat(calendar.isHoliday(LocalDate.parse("2020-02-17")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-02-15")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-02-21")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-02-20")), is(true));

        // Good Friday
        assertThat(calendar.isHoliday(LocalDate.parse("2020-04-10")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-04-02")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-04-15")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-04-07")), is(true));

        // Memorial Day
        assertThat(calendar.isHoliday(LocalDate.parse("2020-05-25")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-05-31")), is(true));

        // Juneteenth
        assertThat(calendar.isHoliday(LocalDate.parse("2020-06-19")), is(false));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-06-18")), is(false));
                // federal holiday for the first time in 2021, but not observed
                // by NYSE until 2022
        assertThat(calendar.isHoliday(LocalDate.parse("2022-06-20")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-06-19")), is(true));

        // Independence Day
        assertThat(calendar.isHoliday(LocalDate.parse("2020-07-03")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-07-05")), is(true));

        // Labour day
        assertThat(calendar.isHoliday(LocalDate.parse("2020-09-07")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-09-06")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-09-05")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-09-04")), is(true));

        // Thanksgiving
        assertThat(calendar.isHoliday(LocalDate.parse("2020-11-26")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-11-25")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-11-24")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-11-23")), is(true));

        // Christmas
        assertThat(calendar.isHoliday(LocalDate.parse("2020-12-25")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-12-24")), is(true));

        // Special Holidays
        assertThat(calendar.isHoliday(LocalDate.parse("1994-04-27")), is(true)); // President Nixon
        assertThat(calendar.isHoliday(LocalDate.parse("2004-06-11")), is(true)); // President Reagan
        assertThat(calendar.isHoliday(LocalDate.parse("2007-01-02")), is(true)); // President Ford
        assertThat(calendar.isHoliday(LocalDate.parse("2012-10-29")), is(true)); // Hurricane Sandy
        assertThat(calendar.isHoliday(LocalDate.parse("2012-10-30")), is(true)); // Hurricane Sandy
        assertThat(calendar.isHoliday(LocalDate.parse("2018-12-05")), is(true)); // President Bush Sr.

        // terrorist attacks (9/11)
        assertThat(calendar.isHoliday(LocalDate.parse("2001-09-10")), is(false)); // open market
        assertThat(calendar.isHoliday(LocalDate.parse("2001-09-11")), is(true)); // closed after terrorist attacks (9/11)
        assertThat(calendar.isHoliday(LocalDate.parse("2001-09-12")), is(true)); // closed after terrorist attacks (9/11)
        assertThat(calendar.isHoliday(LocalDate.parse("2001-09-13")), is(true)); // closed after terrorist attacks (9/11)
        assertThat(calendar.isHoliday(LocalDate.parse("2001-09-14")), is(true)); // closed after terrorist attacks (9/11)
        assertThat(calendar.isHoliday(LocalDate.parse("2001-09-17")), is(false)); // reopen market (Monday)
    }

    @Test
    public void testTradeCalenderLSE()
    {
        // See https://www.londonstockexchange.com/trade/trading-access/business-days

        TradeCalendar calendar = TradeCalendarManager.getInstance("lse");

        // New Year
        assertThat(calendar.isHoliday(LocalDate.parse("2019-01-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-01-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-01-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-01-01")), is(true));

        // Good Friday
        assertThat(calendar.isHoliday(LocalDate.parse("2019-04-19")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-04-02")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-04-15")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-04-07")), is(true));

        // Easter Monday
        assertThat(calendar.isHoliday(LocalDate.parse("2019-04-22")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-04-07")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-04-10")), is(true));

        // Early May Bank Holiday moved to VE Day in 1995 and 2020
        assertThat(calendar.isHoliday(LocalDate.parse("1995-05-01")), is(false));
        assertThat(calendar.isHoliday(LocalDate.parse("1995-05-08")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2020-05-04")), is(false));
        assertThat(calendar.isHoliday(LocalDate.parse("2020-05-08")), is(true));

        // Spring Bank Holiday moved for royal jubilees in 1977, 2002, 2012, 2022
        assertThat(calendar.isHoliday(LocalDate.parse("1977-05-30")), is(false));
        assertThat(calendar.isHoliday(LocalDate.parse("1977-06-06")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("1977-06-07")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2002-05-27")), is(false));
        assertThat(calendar.isHoliday(LocalDate.parse("2002-06-03")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2002-06-04")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2012-05-28")), is(false));
        assertThat(calendar.isHoliday(LocalDate.parse("2012-06-04")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2012-06-05")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-05-30")), is(false));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-06-02")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-06-03")), is(true));

        assertThat(calendar.isHoliday(LocalDate.parse("2019-04-22")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2019-05-06")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2019-05-27")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2019-08-26")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2019-12-25")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2019-12-26")), is(true));

        assertThat(calendar.isHoliday(LocalDate.parse("2021-12-24")), is(false));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-12-25")), is(true)); // Saturday and Christmas Day
        assertThat(calendar.isHoliday(LocalDate.parse("2021-12-26")), is(true)); // Sunday and Boxing Day
        assertThat(calendar.isHoliday(LocalDate.parse("2021-12-27")), is(true)); // Monday, substitute for holiday
        assertThat(calendar.isHoliday(LocalDate.parse("2021-12-28")), is(true)); // Tuesday, substitute for holiday
        assertThat(calendar.isHoliday(LocalDate.parse("2021-12-29")), is(false));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-12-30")), is(false));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-12-31")), is(false));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-01-01")), is(true)); // Saturday and New Year
        assertThat(calendar.isHoliday(LocalDate.parse("2022-01-02")), is(true)); // Sunday
        assertThat(calendar.isHoliday(LocalDate.parse("2022-01-03")), is(true)); // Monday, substitute for holiday
        assertThat(calendar.isHoliday(LocalDate.parse("2022-01-04")), is(false));

        // special holidays
        assertThat(calendar.isHoliday(LocalDate.parse("1973-11-14")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("1981-07-29")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("1999-12-31")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2011-04-29")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-09-19")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-05-01")), is(true)); // regular Early May Bank Holiday
        assertThat(calendar.isHoliday(LocalDate.parse("2023-05-08")), is(true));
    }

    @Test
    public void testTradeCalenderEuronext()
    {
        // See https://www.euronext.com/en/trading-calendars-hours

        TradeCalendar calendar = TradeCalendarManager.getInstance("euronext");

        // New Year
        assertThat(calendar.isHoliday(LocalDate.parse("2019-01-01")), is(true));

        // Good Friday
        assertThat(calendar.isHoliday(LocalDate.parse("2019-04-19")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-04-02")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-04-15")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-04-07")), is(true));

        // Easter Monday
        assertThat(calendar.isHoliday(LocalDate.parse("2019-04-22")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-04-07")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-04-10")), is(true));

        // Labour day
        assertThat(calendar.isHoliday(LocalDate.parse("2019-05-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2020-05-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-05-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-05-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-05-01")), is(true));

        // Christmas
        assertThat(calendar.isHoliday(LocalDate.parse("2019-12-25")), is(true));

        // Saint Stephen
        assertThat(calendar.isHoliday(LocalDate.parse("2019-12-26")), is(true));
    }

    @Test
    public void testTradeCalenderTSX()
    {
        // See https://www.tsx.com/trading/calendars-and-trading-hours/calendar

        TradeCalendar calendar = TradeCalendarManager.getInstance("tsx");

        // New Year
        assertThat(calendar.isHoliday(LocalDate.parse("2011-01-03")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2012-01-02")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2017-01-02")), is(true));

        // Family day
        assertThat(calendar.isHoliday(LocalDate.parse("2021-02-15")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-02-21")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-02-20")), is(true));

        // Good Friday
        assertThat(calendar.isHoliday(LocalDate.parse("2021-04-02")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-04-15")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-04-07")), is(true));

        // Victoria day
        assertThat(calendar.isHoliday(LocalDate.parse("2021-05-24")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-05-23")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-05-22")), is(true));

        // National day
        assertThat(calendar.isHoliday(LocalDate.parse("2021-07-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-07-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-07-03")), is(true));

        // Civic Day
        assertThat(calendar.isHoliday(LocalDate.parse("2021-08-02")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-08-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-08-07")), is(true));

        // Labour day
        assertThat(calendar.isHoliday(LocalDate.parse("2021-09-06")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-09-05")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-09-04")), is(true));

        // Thanksgiving
        assertThat(calendar.isHoliday(LocalDate.parse("2021-10-11")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-10-10")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-10-09")), is(true));

        // Christmas and Boxing Day
        assertThat(calendar.isHoliday(LocalDate.parse("2016-12-26")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2016-12-27")), is(true));

        // Canada Day
        assertThat(calendar.isHoliday(LocalDate.parse("2017-07-03")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2018-07-02")), is(true));

        // 2020: Note Christmas and Boxing Day
        assertThat(calendar.isHoliday(LocalDate.parse("2020-01-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2020-02-17")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2020-04-10")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2020-05-18")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2020-07-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2020-08-03")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2020-09-07")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2020-10-12")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2020-12-25")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2020-12-28")), is(true));

        // 2021: Note Christmas and Boxing Day
        assertThat(calendar.isHoliday(LocalDate.parse("2021-01-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-02-15")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-04-02")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-05-24")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-07-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-08-02")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-09-06")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-10-11")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-12-27")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-12-28")), is(true));
    }

    @Test
    public void testTradeCalenderIBOV()
    {
        // See https://www.b3.com.br/pt_br/solucoes/plataformas/puma-trading-system/para-participantes-e-traders/calendario-de-negociacao/feriados/

        TradeCalendar calendar = TradeCalendarManager.getInstance("ibov");

        // New Year
        assertThat(calendar.isHoliday(LocalDate.parse("2023-01-01")), is(true));

        // Carnival
        assertThat(calendar.isHoliday(LocalDate.parse("2021-02-15")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-02-16")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-02-28")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-03-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-02-20")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-02-21")), is(true));

        // Easter Monday
        assertThat(calendar.isHoliday(LocalDate.parse("2021-04-02")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-04-15")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-04-07")), is(true));

        // Tiradentes' Day
        assertThat(calendar.isHoliday(LocalDate.parse("2023-04-21")), is(true));

        // Labour day
        assertThat(calendar.isHoliday(LocalDate.parse("2019-05-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2020-05-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-05-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-05-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-05-01")), is(true));

        // Corpus Christi
        assertThat(calendar.isHoliday(LocalDate.parse("2021-06-03")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-06-16")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-06-08")), is(true));

        // Independence day
        assertThat(calendar.isHoliday(LocalDate.parse("2023-09-07")), is(true));

        // Patron day
        assertThat(calendar.isHoliday(LocalDate.parse("2023-10-12")), is(true));

        // All soul day
        assertThat(calendar.isHoliday(LocalDate.parse("2023-11-02")), is(true));

        // Republic Proclamation day
        assertThat(calendar.isHoliday(LocalDate.parse("2023-11-02")), is(true));

        // Christmas
        assertThat(calendar.isHoliday(LocalDate.parse("2023-12-25")), is(true));
    }

    @Test
    public void testTradeCalenderSIX()
    {
        // See https://six-group.com/exchanges/exchange_traded_products/trading/trading_and_settlement_calendar_de.html

        TradeCalendar calendar = TradeCalendarManager.getInstance("six");

        // New Year
        assertThat(calendar.isHoliday(LocalDate.parse("2023-01-01")), is(true));

        // Extra Holiday
        assertThat(calendar.isHoliday(LocalDate.parse("2000-01-03")), is(true));

        // Easter Monday
        assertThat(calendar.isHoliday(LocalDate.parse("2023-04-07")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-04-10")), is(true));

        // Labour day
        assertThat(calendar.isHoliday(LocalDate.parse("2019-05-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2020-05-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-05-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-05-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-05-01")), is(true));

        // Ascension
        assertThat(calendar.isHoliday(LocalDate.parse("2001-05-24")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2002-05-09")), is(false));
        assertThat(calendar.isHoliday(LocalDate.parse("2003-05-29")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-05-18")), is(true));

        // Whit Monday
        assertThat(calendar.isHoliday(LocalDate.parse("2001-06-04")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2002-05-20")), is(false));
        assertThat(calendar.isHoliday(LocalDate.parse("2003-06-09")), is(true));

        // National day
        assertThat(calendar.isHoliday(LocalDate.parse("1999-08-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2000-08-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2001-08-01")), is(false));
        assertThat(calendar.isHoliday(LocalDate.parse("2002-08-01")), is(false));
        assertThat(calendar.isHoliday(LocalDate.parse("2003-08-01")), is(false));
        assertThat(calendar.isHoliday(LocalDate.parse("2004-08-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2005-08-01")), is(false));
        assertThat(calendar.isHoliday(LocalDate.parse("2006-08-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2007-08-01")), is(true));

        // Christmas
        assertThat(calendar.isHoliday(LocalDate.parse("2023-12-24")), is(true));

        // Saint Stephen
        assertThat(calendar.isHoliday(LocalDate.parse("2023-12-26")), is(true));
    }

    @Test
    public void testTradeCalenderISE()
    {
        // See https://www.borsaitaliana.it/borsaitaliana/calendario-e-orari-di-negoziazione/calendario-borsa-orari-di-negoziazione.en.htm

        TradeCalendar calendar = TradeCalendarManager.getInstance("ise");

        // New Year
        assertThat(calendar.isHoliday(LocalDate.parse("2022-12-31")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-01-01")), is(true));

        // Good Friday
        assertThat(calendar.isHoliday(LocalDate.parse("2021-04-02")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-04-15")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-04-07")), is(true));

        // Easter Monday
        assertThat(calendar.isHoliday(LocalDate.parse("2023-04-07")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-04-10")), is(true));

        // Labour day
        assertThat(calendar.isHoliday(LocalDate.parse("2019-05-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2020-05-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-05-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-05-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-05-01")), is(true));

        // Assumption day
        assertThat(calendar.isHoliday(LocalDate.parse("2023-08-15")), is(true));

        // Christmas
        assertThat(calendar.isHoliday(LocalDate.parse("2023-12-24")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-12-25")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-12-26")), is(true));
    }

    @Test
    public void testTradeCalenderVSE()
    {
        // See https://www.wienerborse.at/handel/handelsinformationen/handelskalender/

        TradeCalendar calendar = TradeCalendarManager.getInstance("vse");

        // New Year
        assertThat(calendar.isHoliday(LocalDate.parse("2022-12-31")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-01-01")), is(true));

        // Easter Monday
        assertThat(calendar.isHoliday(LocalDate.parse("2023-04-07")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-04-10")), is(true));

        // Whit Monday
        assertThat(calendar.isHoliday(LocalDate.parse("2021-05-24")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-06-06")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-05-30")), is(false));

        // Labour day
        assertThat(calendar.isHoliday(LocalDate.parse("2019-05-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2020-05-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-05-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-05-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-05-01")), is(true));

        // National day
        assertThat(calendar.isHoliday(LocalDate.parse("2023-10-26")), is(true));

        // Christmas
        assertThat(calendar.isHoliday(LocalDate.parse("2023-12-24")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-12-25")), is(true));

        // Saint Stephen
        assertThat(calendar.isHoliday(LocalDate.parse("2023-12-26")), is(true));
    }

    @Test
    public void testTradeCalenderMICEXRTS()
    {
        TradeCalendar calendar = TradeCalendarManager.getInstance("MICEX-RTS");

        // New Year
        assertThat(calendar.isHoliday(LocalDate.parse("2023-01-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-01-02")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-01-03")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-01-04")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-01-05")), is(true));

        // Christmas eve (Russia)
        assertThat(calendar.isHoliday(LocalDate.parse("2023-01-07")), is(true));

        // Defender of the fatherland day
        assertThat(calendar.isHoliday(LocalDate.parse("2023-02-23")), is(true));

        // International womens day
        assertThat(calendar.isHoliday(LocalDate.parse("2014-03-10")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2020-03-09")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-03-08")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-03-08")), is(true));

        // Labour day
        assertThat(calendar.isHoliday(LocalDate.parse("2019-05-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2020-05-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-05-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-05-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-05-01")), is(true));

        // Victory day
        assertThat(calendar.isHoliday(LocalDate.parse("2014-05-09")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2015-05-11")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2020-05-11")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-05-09")), is(true));

        // National day
        assertThat(calendar.isHoliday(LocalDate.parse("2020-06-12")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-06-14")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-06-13")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-06-12")), is(true));

        // Unity day
        assertThat(calendar.isHoliday(LocalDate.parse("2018-11-05")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-11-04")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-11-04")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-11-06")), is(true));
    }

    @Test
    public void testTradeCalendarSSE()
    {
        // Initialize the trade calendar for "sse"
        TradeCalendar calendar = TradeCalendarManager.getInstance("sse");


        // Test New Year
        assertThat(calendar.isHoliday(LocalDate.parse("2021-01-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-01-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-01-01")), is(true));

        // Test Good Friday (Easter-related)
        assertThat(calendar.isHoliday(LocalDate.parse("2021-04-02")), is(true)); // Good Friday in 2021
        assertThat(calendar.isHoliday(LocalDate.parse("2022-04-15")), is(true)); // Good Friday in 2022
        assertThat(calendar.isHoliday(LocalDate.parse("2023-04-07")), is(true)); // Good Friday in 2023

        // Test Labour Day
        assertThat(calendar.isHoliday(LocalDate.parse("2021-05-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-05-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-05-01")), is(true));

        // Test Chile Navy Day
        assertThat(calendar.isHoliday(LocalDate.parse("2021-05-21")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-05-21")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-05-21")), is(true));

        // Test Indigenous People Day
        assertThat(calendar.isHoliday(LocalDate.parse("2021-06-20")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-06-20")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-06-20")), is(true));

        // Test Saint Peter and Paul Day
        assertThat(calendar.isHoliday(LocalDate.parse("2021-06-29")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-06-29")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-06-29")), is(true));

        // Test Virgin of Carmen Day
        assertThat(calendar.isHoliday(LocalDate.parse("2021-07-16")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-07-16")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-07-16")), is(true));

        // Test Ascension Day
        assertThat(calendar.isHoliday(LocalDate.parse("2021-08-15")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-08-15")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-08-15")), is(true));

        // Test Chile National Day
        assertThat(calendar.isHoliday(LocalDate.parse("2021-09-18")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-09-18")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-09-18")), is(true));

        // Test Chile Army Day
        assertThat(calendar.isHoliday(LocalDate.parse("2021-09-19")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-09-19")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-09-19")), is(true));

        // Test Chile Extra Day
        assertThat(calendar.isHoliday(LocalDate.parse("2021-09-20")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-09-20")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-09-20")), is(true));

        // Test Columbus Day
        assertThat(calendar.isHoliday(LocalDate.parse("2021-10-12")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-10-12")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-10-12")), is(true));

        // Test Reformation Day
        assertThat(calendar.isHoliday(LocalDate.parse("2021-10-31")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-10-31")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-10-31")), is(true));

        // Test All Saints' Day
        assertThat(calendar.isHoliday(LocalDate.parse("2021-11-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-11-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-11-01")), is(true));

        // Test Inmaculate Conception Day
        assertThat(calendar.isHoliday(LocalDate.parse("2021-12-08")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-12-08")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-12-08")), is(true));

        // Test First Christmas Day
        assertThat(calendar.isHoliday(LocalDate.parse("2021-12-25")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2022-12-25")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-12-25")), is(true));
    }

    @Test
    public void testEmptyCalendar()
    {
        TradeCalendar calendar = TradeCalendarManager.getInstance("empty");

        // we generate a random day
        long minDay = LocalDate.of(2000, 1, 1).toEpochDay();
        long maxDay = LocalDate.of(2020, 12, 31).toEpochDay();
        long randomDay = ThreadLocalRandom.current().nextLong(minDay, maxDay);
        LocalDate randomDate = LocalDate.ofEpochDay(randomDay);

        // in the empty calendar, every day is a (potential) trading day
        assertThat(calendar.isHoliday(randomDate), is(false));
    }
}
