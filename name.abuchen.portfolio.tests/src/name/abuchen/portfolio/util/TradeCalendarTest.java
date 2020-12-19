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
    }

    @Test
    public void testNYSE()
    {
        // See https://www.nyse.com/markets/hours-calendars

        TradeCalendar calendar = TradeCalendarManager.getInstance("nyse");

        assertThat(calendar.isHoliday(LocalDate.parse("2020-01-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2020-01-20")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2020-02-17")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2020-04-10")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2020-05-25")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2020-07-03")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2020-09-07")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2020-11-26")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2020-12-25")), is(true));

        assertThat(calendar.isHoliday(LocalDate.parse("2021-01-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-01-18")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-02-15")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-04-02")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-05-31")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-07-05")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-09-06")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-11-25")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2021-12-24")), is(true));
    }

    @Test
    public void testLSE()
    {
        // See
        // https://www.lseg.com/areas-expertise/our-markets/london-stock-exchange/equities-markets/trading-services/business-days

        TradeCalendar calendar = TradeCalendarManager.getInstance("lse");

        assertThat(calendar.isHoliday(LocalDate.parse("2019-01-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2019-04-19")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2019-04-22")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2019-05-06")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2019-05-27")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2019-08-26")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2019-12-25")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2019-12-26")), is(true));
    }

    @Test
    public void testEuronext()
    {
        // See https://www.euronext.com/en/trading-calendars-hours

        TradeCalendar calendar = TradeCalendarManager.getInstance("euronext");

        assertThat(calendar.isHoliday(LocalDate.parse("2019-01-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2019-04-19")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2019-04-22")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2019-05-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2019-12-25")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2019-12-26")), is(true));
    }

    @Test
    public void testEmptyCalendar()
    {
        TradeCalendar calendar = TradeCalendarManager.getInstance(TradeCalendar.EMPTY_CODE);

        // we generate a random day
        long minDay = LocalDate.of(2000, 1, 1).toEpochDay();
        long maxDay = LocalDate.of(2020, 12, 31).toEpochDay();
        long randomDay = ThreadLocalRandom.current().nextLong(minDay, maxDay);
        LocalDate randomDate = LocalDate.ofEpochDay(randomDay);

        // in the empty calendar, every day is a (potential) trading day
        assertThat(calendar.isHoliday(randomDate), is(false));
    }
}
