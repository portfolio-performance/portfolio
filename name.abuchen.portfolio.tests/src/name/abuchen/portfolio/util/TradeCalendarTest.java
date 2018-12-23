package name.abuchen.portfolio.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.time.LocalDate;

import org.junit.Test;

@SuppressWarnings("nls")
public class TradeCalendarTest
{

    @Test
    public void testEasterHolidays()
    {
        TradeCalendar calendar = TradeCalendarManager.getInstance("trade-calendar-de");

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
        TradeCalendar calendar = TradeCalendarManager.getInstance("trade-calendar-de");

        assertThat(calendar.isHoliday(LocalDate.parse("2015-01-31")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2015-02-01")), is(true));
    }

    @Test
    public void testFixedPublicHolidays()
    {
        TradeCalendar calendar = TradeCalendarManager.getInstance("trade-calendar-de");

        assertThat(calendar.isHoliday(LocalDate.parse("2015-01-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2015-05-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2015-12-24")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2015-12-25")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2015-12-26")), is(true));
    }
}
