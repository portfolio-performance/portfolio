package name.abuchen.portfolio.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;

import java.time.LocalDate;

import org.junit.Test;

public class IsraelTradeCalendarTest
{

    @Test
    public void testJewishCalendar()
    {
        TLVHolidayType type = new TLVHolidayType();

        // EREV_PASSOVER,
        // PASSOVERI,
        // PASSOVERII,
        assertThat(type.getErevPassover(2024), is(LocalDate.of(2024, 4, 22)));
        assertThat(type.getPassoverI(2024), is(LocalDate.of(2024, 4, 23)));
        assertThat(type.getPassoverII(2024), is(LocalDate.of(2024, 4, 24)));

        assertThat(type.getErevPassover(2025), is(LocalDate.of(2025, 4, 12)));
        assertThat(type.getPassoverI(2025), is(LocalDate.of(2025, 4, 13)));
        assertThat(type.getPassoverII(2025), is(LocalDate.of(2025, 4, 14)));

        assertThat(type.getErevPassover(2026), is(LocalDate.of(2026, 4, 1)));
        assertThat(type.getPassoverI(2026), is(LocalDate.of(2026, 4, 2)));
        assertThat(type.getPassoverII(2026), is(LocalDate.of(2026, 4, 3)));

        // Not Needed
        assertThat(type.YomHashoah(2024), is(LocalDate.of(2024, 5, 6))); 
        assertThat(type.YomHashoah(2025), is(LocalDate.of(2025, 4, 24)));
        assertThat(type.YomHashoah(2026), is(LocalDate.of(2026, 4, 14)));
        
        //MEMORIAL_DAY,
        // assertThat(type.MemorialDay(2024), is(LocalDate.of(2024, 5, 13)));
        assertThat(type.MemorialDay(2025), is(LocalDate.of(2025, 4, 30)));
        // assertThat(type.MemorialDay(2026), is(LocalDate.of(2026, 4, 21)));

        //INDEPENDENCE_DAY,
        // assertThat(type.YomHaAtzmaut(2024), is(LocalDate.of(2024, 5, 14)));
        assertThat(type.YomHaAtzmaut(2025), is(LocalDate.of(2025, 5, 1)));
        // assertThat(type.YomHaAtzmaut(2026), is(LocalDate.of(2026, 4, 22)));

        // Not Needed
        // assertThat(type.getLagBaOmer(2024), is(LocalDate.of(2024, 5, 25)));
        // assertThat(type.getLagBaOmer(2025), is(LocalDate.of(2025, 4, 15)));
        // assertThat(type.getLagBaOmer(2026), is(LocalDate.of(2026, 4, 4)));

        // Not Needed
        assertThat(type.getYomYerushalayim(2024), is(LocalDate.of(2024, 6, 5)));
        assertThat(type.getYomYerushalayim(2025), is(LocalDate.of(2025, 5, 26)));
        assertThat(type.getYomYerushalayim(2026), is(LocalDate.of(2026, 5, 15)));

        // SAVHUOT_EVE,
        // SHAVUOT,
        assertThat(type.getErevShavuot(2024), is(LocalDate.of(2024, 6, 11)));
        assertThat(type.getErevShavuot(2025), is(LocalDate.of(2025, 6, 1)));
        assertThat(type.getErevShavuot(2026), is(LocalDate.of(2026, 5, 21)));
        
        //PURIM,
        
        
        
        // FAST_DAY Tisha B'Av

        // NEW_YEAR_EVE,
        // NEWYEARI,
        // NEWYEARII,
        // YOM_KIPUR_EVE,
        // YOM_KIPUR,
        // SUKKOTH_EVE,
        // SUKKOTH,
        // SIMCHAT_TORA_EVE,
        // SIMCHAT_TORA;

        for (int year = 2024; year < 2027; year++)
        {
            System.out.println(type.getErevPassover(year));
            System.out.println(type.getPassoverI(year));
            System.out.println(type.getPassoverII(year));
        }

    }

    @Test
    public void testWeekends()
    {
        TradeCalendar calendar = TradeCalendarManager.getInstance("tlv2025");

        assertFalse(calendar == null);
        assertThat(calendar.isHoliday(LocalDate.parse("2025-09-12")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2025-09-13")), is(true));

        // After 2016 will move to International weekends
        calendar = TradeCalendarManager.getInstance("tlv");

        assertThat(calendar.isHoliday(LocalDate.parse("2026-01-10")), is(true));
        assertThat(calendar.isHoliday(LocalDate.parse("2026-01-11")), is(true));

    }

    // Test Purim
    // Test PAssover Day
    // Test 7th Passover
    // Test Rememberance Day
    // Test Independence Day
    // Test Shavuot
    // Test 9th Beav
    // Test Rosh Hashana Evening, 1st and 2nd Day
    // Test Kippur Evening and Day
    // Test Sukkot Eveneng and Day
    // Test Simhat Torah Evening and Day

}
