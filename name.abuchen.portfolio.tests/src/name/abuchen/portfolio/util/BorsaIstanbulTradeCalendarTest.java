package name.abuchen.portfolio.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;

import org.junit.Test;

/**
 * See https://www.borsaistanbul.com/en/official-holidays
 */
@SuppressWarnings("nls")
public class BorsaIstanbulTradeCalendarTest
{
    @Test
    public void testRamadanFeast()
    {
        var calendar = TradeCalendarManager.getInstance("bist");

        // Regular years
        assertThat(calendar.isHoliday(LocalDate.parse("2026-03-19")), is(false)); // Thursday before feast
        assertThat(calendar.isHoliday(LocalDate.parse("2026-03-20")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2026-03-22")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2026-03-23")), is(false));

        // 2016: feast shifted one day earlier by BIST (days 6–8 → 5–7)
        assertThat(calendar.isHoliday(LocalDate.parse("2016-07-04")), is(false));
        assertThat(calendar.isHoliday(LocalDate.parse("2016-07-05")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2016-07-06")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2016-07-07")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2016-07-08")), is(false));
    }

    @Test
    public void testSacrificeFeast()
    {
        var calendar = TradeCalendarManager.getInstance("bist");

        // Regular years (2023: days 1–3 on weekdays; day 4 = Saturday)
        assertThat(calendar.isHoliday(LocalDate.parse("2023-06-28")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-06-29")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2023-06-30")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2025-06-06")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2025-06-09")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2025-06-10")), is(false));

        // 2012: feast shifted one day earlier by BIST (days 26–29 → 25–28)
        assertThat(calendar.isHoliday(LocalDate.parse("2012-10-24")), is(false));
        assertThat(calendar.isHoliday(LocalDate.parse("2012-10-25")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2012-10-26")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2012-10-28")), is(true)); // also Republic Day
        assertThat(calendar.isHoliday(LocalDate.parse("2012-10-30")), is(false));

        // 2015: feast shifted one day later by BIST (days 23–26 → 24–27)
        assertThat(calendar.isHoliday(LocalDate.parse("2015-09-23")), is(false));
        assertThat(calendar.isHoliday(LocalDate.parse("2015-09-24")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2015-09-25")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2015-09-27")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2015-09-28")), is(false));

        // 2016: feast shifted one day later by BIST (days 11–14 → 12–15)
        assertThat(calendar.isHoliday(LocalDate.parse("2016-09-09")), is(false)); // Friday before feast
        assertThat(calendar.isHoliday(LocalDate.parse("2016-09-12")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2016-09-13")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2016-09-14")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2016-09-15")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2016-09-16")), is(false));
    }

    @Test
    public void testStaticHolidays()
    {
        var calendar = TradeCalendarManager.getInstance("bist");

        // New Year
        assertThat(calendar.isHoliday(LocalDate.parse("2012-01-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2026-01-01")), is(true));

        // National Sovereignty and Children's Day (Apr 23)
        assertThat(calendar.isHoliday(LocalDate.parse("2012-04-23")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2013-04-23")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2025-04-23")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2026-04-23")), is(true));

        // Labour Day (May 1)
        assertThat(calendar.isHoliday(LocalDate.parse("2012-05-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2013-05-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2020-05-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2025-05-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2026-05-01")), is(true));

        // Youth and Sports Day (May 19)
        assertThat(calendar.isHoliday(LocalDate.parse("2012-05-19")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2013-05-19")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2025-05-19")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2026-05-19")), is(true));

        // Victory Day (Aug 30)
        assertThat(calendar.isHoliday(LocalDate.parse("2012-08-30")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2013-08-30")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2024-08-30")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2025-08-30")), is(true));
    }

    @Test
    public void testDemocracyAndNationalUnityDay()
    {
        var calendar = TradeCalendarManager.getInstance("bist");

        // Democracy and National Unity Day (Jul 15) — introduced in 2017
        assertThat(calendar.isHoliday(LocalDate.parse("2016-07-15")), is(false));
        assertThat(calendar.isHoliday(LocalDate.parse("2017-07-15")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2018-07-15")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2024-07-15")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2025-07-15")), is(true));
    }

    @Test
    public void testRepublicDay()
    {
        var calendar = TradeCalendarManager.getInstance("bist");

        // Oct 28: half-day in most years, full holiday only in 2012, 2017, 2018
        assertThat(calendar.isHoliday(LocalDate.parse("2012-10-28")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2017-10-28")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2018-10-28")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2020-10-28")), is(false));
        assertThat(calendar.isHoliday(LocalDate.parse("2025-10-28")), is(false));

        // Oct 29: full holiday every year
        assertThat(calendar.isHoliday(LocalDate.parse("2012-10-29")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2013-10-29")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2024-10-29")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2025-10-29")), is(true));
    }

    @Test
    public void testWeekendsAndWorkingDays()
    {
        var calendar = TradeCalendarManager.getInstance("bist");

        // Standard weekend: Saturday and Sunday are holidays
        assertThat(calendar.isHoliday(LocalDate.parse("2025-01-04")), is(true)); // Saturday
        assertThat(calendar.isHoliday(LocalDate.parse("2025-01-05")), is(true)); // Sunday

        // Regular weekdays without holidays are working days
        assertThat(calendar.isHoliday(LocalDate.parse("2025-01-06")), is(false)); // Monday
        assertThat(calendar.isHoliday(LocalDate.parse("2025-01-07")), is(false)); // Tuesday
        assertThat(calendar.isHoliday(LocalDate.parse("2025-01-08")), is(false)); // Wednesday
        assertThat(calendar.isHoliday(LocalDate.parse("2025-01-09")), is(false)); // Thursday
        assertThat(calendar.isHoliday(LocalDate.parse("2025-01-10")), is(false)); // Friday
    }

    @Test
    public void testOutOfScopeHijriDatesDoNotThrow()
    {
        var calendar = TradeCalendarManager.getInstance("bist");

        assertThat(calendar.isHoliday(LocalDate.parse("1800-01-02")), is(false));
    }
}
