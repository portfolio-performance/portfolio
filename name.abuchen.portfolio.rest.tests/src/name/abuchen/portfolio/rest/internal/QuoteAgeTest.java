package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.util.TradeCalendar;
import name.abuchen.portfolio.util.TradeCalendarManager;

/**
 * A quote is stale when trading days passed without one - not when the calendar
 * says it is a day or two old. The dates are October 2026: the 23rd is a Friday.
 */
@SuppressWarnings("nls")
public class QuoteAgeTest
{
    private static final LocalDate FRIDAY = LocalDate.of(2026, 10, 23);

    private static final TradeCalendar GERMANY = TradeCalendarManager.getInstance("de");

    /** trades every day: crypto, for instance */
    private static final TradeCalendar EVERY_DAY = TradeCalendarManager.getInstance("empty");

    private static QuoteAge.Age age(LocalDate quote, LocalDate valuation, TradeCalendar calendar)
    {
        return QuoteAge.of(quote, valuation, calendar);
    }

    @Test
    public void testTheWeekendIsNotAMissedQuote()
    {
        var sunday = age(FRIDAY, FRIDAY.plusDays(2), GERMANY);
        assertThat(sunday.calendarDays(), is(2L));
        assertThat(sunday.missedTradingDays(), is(0L));
        assertThat(sunday.isStale(), is(false));

        // Monday morning: Monday's own close cannot be known yet
        assertThat(age(FRIDAY, FRIDAY.plusDays(3), GERMANY).missedTradingDays(), is(0L));
    }

    /** a fund that publishes a day late has missed one trading day, which is tolerated */
    @Test
    public void testOneMissedTradingDayIsTolerated()
    {
        var tuesday = age(FRIDAY, FRIDAY.plusDays(4), GERMANY);
        assertThat(tuesday.missedTradingDays(), is(1L));
        assertThat(tuesday.isStale(), is(false));
    }

    @Test
    public void testTwoMissedTradingDaysAreStale()
    {
        var wednesday = age(FRIDAY, FRIDAY.plusDays(5), GERMANY);
        assertThat(wednesday.missedTradingDays(), is(2L));
        assertThat(wednesday.isStale(), is(true));
    }

    /** the instrument's holidays are not trading days either: Christmas in Frankfurt */
    @Test
    public void testHolidaysOfTheCalendarAreNotMissedQuotes()
    {
        var age = age(LocalDate.of(2026, 12, 23), LocalDate.of(2026, 12, 28), GERMANY);
        assertThat(age.missedTradingDays(), is(0L));
        assertThat(age.isStale(), is(false));
    }

    /** an instrument that trades every day misses the weekend */
    @Test
    public void testACalendarWithoutWeekendCountsEveryDay()
    {
        assertThat(age(FRIDAY, FRIDAY.plusDays(2), EVERY_DAY).missedTradingDays(), is(1L));
        assertThat(age(FRIDAY, FRIDAY.plusDays(3), EVERY_DAY).isStale(), is(true));
    }

    /** a quote on or after the valuation date is as young as it gets */
    @Test
    public void testAQuoteOnOrAfterTheValuationDateIsFresh()
    {
        assertThat(age(FRIDAY, FRIDAY, GERMANY).calendarDays(), is(0L));
        assertThat(age(FRIDAY.plusDays(3), FRIDAY, GERMANY).calendarDays(), is(0L));
        assertThat(age(FRIDAY.plusDays(3), FRIDAY, GERMANY).missedTradingDays(), is(0L));
    }
}
