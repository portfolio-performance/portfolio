package name.abuchen.portfolio.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

import java.time.LocalDate;

import org.junit.Test;

/**
 * Test for Tel Aviv Stock Exchange (TASE) trading calendar.
 */
@SuppressWarnings("nls")
public class TelAvivStockExchangeTradeCalendarTest
{

    @Test
    public void testRegularLunarCalendarHolidays()
    {
        var calendar = TradeCalendarManager.getInstance("tlv");

        // Passover I Evening and Day, Passover II Evening and Day
        assertThat(calendar.isHoliday(LocalDate.parse("2024-04-22")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2024-04-23")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2024-04-28")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2024-04-29")), is(true));

        assertThat(calendar.isHoliday(LocalDate.parse("2025-04-12")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2025-04-13")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2025-04-18")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2025-04-19")), is(true));

        assertThat(calendar.isHoliday(LocalDate.parse("2026-04-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2026-04-02")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2026-04-07")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2026-04-08")), is(true));

        // Jewish New Years Eve
        assertThat(calendar.isHoliday(LocalDate.parse("2024-10-02")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2025-09-22")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2026-09-11")), is(true));

        // Jewish New Years Day I
        assertThat(calendar.isHoliday(LocalDate.parse("2024-10-03")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2025-09-23")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2026-09-12")), is(true));

        // Jewish New Years Day II
        assertThat(calendar.isHoliday(LocalDate.parse("2024-10-04")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2025-09-24")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2026-09-13")), is(true));

        // YOM_KIPUR_EVE, if (hebDay == 9 && hebMonth == 7)
        assertThat(calendar.isHoliday(LocalDate.parse("2024-10-11")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2025-10-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2026-09-20")), is(true));

        // YOM_KIPUR_DAY
        assertThat(calendar.isHoliday(LocalDate.parse("2024-10-12")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2025-10-02")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2026-09-21")), is(true));

        // FAST_DAY Tisha B'Av
        assertThat(calendar.isHoliday(LocalDate.parse("2024-08-13")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2025-08-03")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2026-07-23")), is(true));

        // SIMCHAT_TORA_EVE, if (hebDay == 22 && hebMonth == 7)
        assertThat(calendar.isHoliday(LocalDate.parse("2024-10-23")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2025-10-13")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2026-10-02")), is(true));

        // SIMCHAT_TORA; if (hebDay == 23 & hebMonth == 7)
        assertThat(calendar.isHoliday(LocalDate.parse("2024-10-24")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2025-10-14")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2026-10-03")), is(true));

        // SHAVUOT_EVE and SHAVUOT
        assertThat(calendar.isHoliday(LocalDate.parse("2024-06-11")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2024-06-12")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2025-06-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2025-06-02")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2026-05-21")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2026-05-22")), is(true));

        // SUKKOTH_EVE, if (hebDay == 14 && hebMonth == 7)
        assertThat(calendar.isHoliday(LocalDate.parse("2024-10-16")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2025-10-06")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2026-09-25")), is(true));

        // SUKKOTH, if (hebDay == 15 && hebMonth == 7)
        assertThat(calendar.isHoliday(LocalDate.parse("2024-10-17")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2025-10-07")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2026-09-26")), is(true));
    }

    @Test
    public void testSpecialJewishCalendarHolidays()
    {
        var calendar = TradeCalendarManager.getInstance("tlv");

        // PURIM
        assertThat(calendar.isHoliday(LocalDate.parse("2024-03-24")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2025-03-14")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2026-03-03")), is(true));
    }

    @Test
    public void testIsraeliMemorialDay()
    {
        var calendar = TradeCalendarManager.getInstance("tlv");

        // Memorial Day (Yom HaZikaron)
        assertThat(calendar.isHoliday(LocalDate.parse("2024-05-13")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2025-04-30")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2026-04-21")), is(true));
    }

    @Test
    public void testIsraeliIndependenceDay()
    {
        var calendar = TradeCalendarManager.getInstance("tlv");

        // Independence Day (Yom Ha'atzmaut)
        assertThat(calendar.isHoliday(LocalDate.parse("2024-05-14")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2025-05-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2026-04-22")), is(true));
    }

    @Test
    public void testWeekendsBefore2026()
    {
        var calendar = TradeCalendarManager.getInstance("tlv");

        assertNotNull(calendar);
        assertThat(calendar.isHoliday(LocalDate.parse("2025-09-18")), is(false)); // Thursday
        assertThat(calendar.isHoliday(LocalDate.parse("2025-09-19")), is(true)); // Friday
        assertThat(calendar.isHoliday(LocalDate.parse("2025-09-20")), is(true)); // Saturday
        assertThat(calendar.isHoliday(LocalDate.parse("2025-09-21")), is(false)); // Sunday

        // new year eve
        assertThat(calendar.isHoliday(LocalDate.parse("2025-09-22")), is(true));
    }

    @Test
    public void testWeekendsAfter2026()
    {
        var calendar = TradeCalendarManager.getInstance("tlv");

        assertNotNull(calendar);
        assertThat(calendar.isHoliday(LocalDate.parse("2026-01-09")), is(false)); // Friday
        assertThat(calendar.isHoliday(LocalDate.parse("2026-01-10")), is(true)); // Saturday
        assertThat(calendar.isHoliday(LocalDate.parse("2026-01-11")), is(true)); // Sunday
        assertThat(calendar.isHoliday(LocalDate.parse("2026-01-12")), is(false)); // Monday
    }

    @Test
    public void testRegularWorkingDays()
    {
        var calendar = TradeCalendarManager.getInstance("tlv");

        // Regular weekdays that are not holidays
        assertThat(calendar.isHoliday(LocalDate.parse("2024-01-02")), is(false)); // Tuesday
        assertThat(calendar.isHoliday(LocalDate.parse("2024-01-03")), is(false)); // Wednesday
        assertThat(calendar.isHoliday(LocalDate.parse("2024-01-04")), is(false)); // Thursday

        assertThat(calendar.isHoliday(LocalDate.parse("2025-01-06")), is(false)); // Monday
        assertThat(calendar.isHoliday(LocalDate.parse("2025-01-07")), is(false)); // Tuesday

        assertThat(calendar.isHoliday(LocalDate.parse("2026-01-06")), is(false)); // Monday
        assertThat(calendar.isHoliday(LocalDate.parse("2026-01-07")), is(false)); // Tuesday
        assertThat(calendar.isHoliday(LocalDate.parse("2026-01-08")), is(false)); // Wednesday
    }

    @Test
    public void testEdgeCasesWithSabbathAdjustments()
    {
        var calendar = TradeCalendarManager.getInstance("tlv");

        // Memorial Day adjusted
        assertThat(calendar.isHoliday(LocalDate.parse("2024-05-13")), is(true));
        // Independence Day adjusted
        assertThat(calendar.isHoliday(LocalDate.parse("2024-05-14")), is(true));
    }

    @Test
    public void testMultiYearConsistency()
    {
        var calendar = TradeCalendarManager.getInstance("tlv");

        // Test multiple years to ensure calendar consistency
        int[] testYears = { 2024, 2025, 2026 };

        for (int year : testYears)
        {
            // Each year should have Rosh Hashanah (2 days)
            var roshHashanahStart = LocalDate.of(year, 9, 1); // approximate
            var foundRoshHashanah = false;

            // Search for Rosh Hashanah in September/October
            for (var day = 1; day <= 60; day++)
            {
                var testDate = roshHashanahStart.plusDays(day);
                if (testDate.getYear() > year)
                    break;

                if (calendar.isHoliday(testDate) && calendar.isHoliday(testDate.plusDays(1)))
                {
                    foundRoshHashanah = true;
                    break;
                }
            }

            assertThat("Rosh Hashanah not found for year " + year, foundRoshHashanah, is(true));
        }
    }
}
