package name.abuchen.portfolio.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;

import java.time.LocalDate;

import org.junit.Test;

public class TLVTradeCalendarTest
{

    @Test
    public void test()
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

    }

    /*
     * @Ignore
     * @Test public void testJewishCalendar() { HolidayType type = new
     * TLVHolidayType(PURIM); // EREV_PASSOVER, // PASSOVERI, // PASSOVERII,
     * assertThat(type.getErevPassover(2024), is(LocalDate.of(2024, 4, 22)));
     * assertThat(type.getPassoverI(2024), is(LocalDate.of(2024, 4, 23)));
     * assertThat(type.getPassoverII(2024), is(LocalDate.of(2024, 4, 24)));
     * assertThat(type.getErevPassover(2025), is(LocalDate.of(2025, 4, 12)));
     * assertThat(type.getPassoverI(2025), is(LocalDate.of(2025, 4, 13)));
     * assertThat(type.getPassoverII(2025), is(LocalDate.of(2025, 4, 14)));
     * assertThat(type.getErevPassover(2026), is(LocalDate.of(2026, 4, 1)));
     * assertThat(type.getPassoverI(2026), is(LocalDate.of(2026, 4, 2)));
     * assertThat(type.getPassoverII(2026), is(LocalDate.of(2026, 4, 3))); //
     * Not Needed assertThat(type.YomHashoah(2024), is(LocalDate.of(2024, 5,
     * 6))); assertThat(type.YomHashoah(2025), is(LocalDate.of(2025, 4, 24)));
     * assertThat(type.YomHashoah(2026), is(LocalDate.of(2026, 4, 14)));
     * //MEMORIAL_DAY, // assertThat(type.MemorialDay(2024),
     * is(LocalDate.of(2024, 5, 13))); assertThat(type.MemorialDay(2025),
     * is(LocalDate.of(2025, 4, 30))); // assertThat(type.MemorialDay(2026),
     * is(LocalDate.of(2026, 4, 21))); //INDEPENDENCE_DAY, //
     * assertThat(type.YomHaAtzmaut(2024), is(LocalDate.of(2024, 5, 14)));
     * assertThat(type.YomHaAtzmaut(2025), is(LocalDate.of(2025, 5, 1))); //
     * assertThat(type.YomHaAtzmaut(2026), is(LocalDate.of(2026, 4, 22))); //
     * Not Needed // assertThat(type.getLagBaOmer(2024), is(LocalDate.of(2024,
     * 5, 25))); // assertThat(type.getLagBaOmer(2025), is(LocalDate.of(2025, 4,
     * 15))); // assertThat(type.getLagBaOmer(2026), is(LocalDate.of(2026, 4,
     * 4))); // Not Needed assertThat(type.getYomYerushalayim(2024),
     * is(LocalDate.of(2024, 6, 5))); assertThat(type.getYomYerushalayim(2025),
     * is(LocalDate.of(2025, 5, 26))); assertThat(type.getYomYerushalayim(2026),
     * is(LocalDate.of(2026, 5, 15))); // SAVHUOT_EVE, // SHAVUOT,
     * assertThat(type.getErevShavuot(2024), is(LocalDate.of(2024, 6, 11)));
     * assertThat(type.getErevShavuot(2025), is(LocalDate.of(2025, 6, 1)));
     * assertThat(type.getErevShavuot(2026), is(LocalDate.of(2026, 5, 21)));
     * //PURIM, assertThat(type.getPurim(2024), is(LocalDate.of(2024, 3, 24)));
     * assertThat(type.getPurim(2025), is(LocalDate.of(2025, 3, 14)));
     * assertThat(type.getPurim(2026), is(LocalDate.of(2026, 3, 3))); //
     * FAST_DAY Tisha B'Av assertThat(type.getTishBAv(2024),
     * is(LocalDate.of(2024, 8, 13))); assertThat(type.getTishBAv(2025),
     * is(LocalDate.of(2025, 8, 3))); assertThat(type.getTishBAv(2026),
     * is(LocalDate.of(2026, 7, 23))); // NEW_YEAR_EVE,
     * assertThat(type.getErevRoshHashanah(2024), is(LocalDate.of(2024, 10,
     * 2))); assertThat(type.getErevRoshHashanah(2025), is(LocalDate.of(2025, 9,
     * 22))); assertThat(type.getErevRoshHashanah(2026), is(LocalDate.of(2026,
     * 9, 11))); // NEWYEARI, assertThat(type.getRoshHashanahI(2024),
     * is(LocalDate.of(2024, 10, 3))); assertThat(type.getRoshHashanahI(2025),
     * is(LocalDate.of(2025, 9, 23))); assertThat(type.getRoshHashanahI(2026),
     * is(LocalDate.of(2026, 9, 12))); // NEWYEARII,
     * assertThat(type.getRoshHashanahII(2024), is(LocalDate.of(2024, 10, 4)));
     * assertThat(type.getRoshHashanahII(2025), is(LocalDate.of(2025, 9, 24)));
     * assertThat(type.getRoshHashanahII(2026), is(LocalDate.of(2026, 9, 13)));
     * // YOM_KIPUR_EVE, if (hebDay == 9 && hebMonth == 7)
     * assertThat(type.getErevYomKippur(2024), is(LocalDate.of(2024, 10, 11)));
     * assertThat(type.getErevYomKippur(2025), is(LocalDate.of(2025, 10, 1)));
     * assertThat(type.getErevYomKippur(2026), is(LocalDate.of(2026, 9, 20)));
     * // YOM_KIPUR, assertThat(type.getYomKippur(2024), is(LocalDate.of(2024,
     * 10, 12))); assertThat(type.getYomKippur(2025), is(LocalDate.of(2025, 10,
     * 2))); assertThat(type.getYomKippur(2026), is(LocalDate.of(2026, 9, 21)));
     * // SUKKOTH_EVE, if (hebDay == 14 && hebMonth == 7)
     * assertThat(type.getErevSukkot(2024), is(LocalDate.of(2024, 10, 16)));
     * assertThat(type.getErevSukkot(2025), is(LocalDate.of(2025, 10, 6)));
     * assertThat(type.getErevSukkot(2026), is(LocalDate.of(2026, 9, 25))); //
     * SUKKOTH, if (hebDay == 15 && hebMonth == 7)
     * assertThat(type.getSukkot(2024), is(LocalDate.of(2024, 10, 17)));
     * assertThat(type.getSukkot(2025), is(LocalDate.of(2025, 10, 7)));
     * assertThat(type.getSukkot(2026), is(LocalDate.of(2026, 9, 26))); //
     * SIMCHAT_TORA_EVE, if (hebDay == 22 && hebMonth == 7)
     * assertThat(type.getErevSimhatTorah(2024), is(LocalDate.of(2024, 10,
     * 23))); assertThat(type.getErevSimhatTorah(2025), is(LocalDate.of(2025,
     * 10, 13))); assertThat(type.getErevSimhatTorah(2026),
     * is(LocalDate.of(2026, 10, 2))); // SIMCHAT_TORA; if (hebDay == 23 &&
     * hebMonth == 7) assertThat(type.getSimhatTorah(2024),
     * is(LocalDate.of(2024, 10, 24))); assertThat(type.getSimhatTorah(2025),
     * is(LocalDate.of(2025, 10, 14))); assertThat(type.getSimhatTorah(2026),
     * is(LocalDate.of(2026, 10, 3))); }
     */

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



}
