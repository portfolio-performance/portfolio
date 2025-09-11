package name.abuchen.portfolio.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;

import java.time.LocalDate;

import org.junit.Ignore;
import org.junit.Test;

public class TLVTradeCalendarTest
{

    @Test
    public void Regular_Lunar_Calendar_Holidays_should_be_a_holiday()
    {
        TradeCalendar calendar = TradeCalendarManager.getInstance("tlv");

        // Passover Night - Erev Passover, PassoverI and Passover II
        assertThat(calendar.isHoliday(LocalDate.parse("2024-04-22")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2024-04-23")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2024-04-24")), is(true));

        assertThat(calendar.isHoliday(LocalDate.parse("2025-04-12")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2025-04-13")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2025-04-14")), is(true));

        assertThat(calendar.isHoliday(LocalDate.parse("2026-04-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2026-04-02")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2026-04-03")), is(true));

        // JEWISH NEW YEARS EVE
        assertThat(calendar.isHoliday(LocalDate.parse("2024-10-02")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2025-09-22")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2026-09-11")), is(true));

        // JEWISH NEW YEARS DAY I
        assertThat(calendar.isHoliday(LocalDate.parse("2024-10-03")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2025-09-23")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2026-09-12")), is(true));

        // JEWISH NEW YEARS DAY II
        assertThat(calendar.isHoliday(LocalDate.parse("2024-10-04")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2025-09-24")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2026-09-13")), is(true));

        
        //YOM_KIPUR_EVE, if (hebDay == 9 && hebMonth == 7)
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

        // SAVHUOT_EVE, // SHAVUOT,
        assertThat(calendar.isHoliday(LocalDate.parse("2024-06-11")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2025-06-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2026-05-21")), is(true));

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
    public void Special_Jewish_Calendar_Holidays_should_be_a_holiday()
    {
        // PURIM
        TradeCalendar calendar = TradeCalendarManager.getInstance("tlv");
        assertThat(calendar.isHoliday(LocalDate.parse("2024-03-24")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2025-03-14")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2026-03-03")), is(true));

    }

    @Ignore
    @Test
    public void Israeli_Memorial_Day_should_be_a_holiday()
    {
        TradeCalendar calendar = TradeCalendarManager.getInstance("tlv");
        assertThat(calendar.isHoliday(LocalDate.parse("2024-05-13")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2025-04-30")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2026-04-21")), is(true));
    }

    @Ignore
    @Test
    public void Israeli_Independence_Day_should_be_a_holiday()
    {
        TradeCalendar calendar = TradeCalendarManager.getInstance("tlv");

        assertThat(calendar.isHoliday(LocalDate.parse("2024-05-14")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2025-05-01")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2026-04-22")), is(true));
      
    }



    @Test
    public void testWeekendsAfter2016()
    {
        TradeCalendar calendar = TradeCalendarManager.getInstance("tlv");

        assertFalse(calendar == null);
        assertThat(calendar.isHoliday(LocalDate.parse("2026-01-10")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2026-01-11")), is(true));
    }

    @Ignore("Test is not ready")
    @Test
    public void testWeekendsBefore2016()
    {
        TradeCalendar calendar = TradeCalendarManager.getInstance("tlv");

        assertFalse(calendar == null);
        assertThat(calendar.isHoliday(LocalDate.parse("2025-09-12")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2025-09-13")), is(true));

    }



}
