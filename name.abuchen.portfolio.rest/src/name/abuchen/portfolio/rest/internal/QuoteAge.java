package name.abuchen.portfolio.rest.internal;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import name.abuchen.portfolio.util.TradeCalendar;

/**
 * How old a quote is, and whether that makes it stale.
 * <p/>
 * Calendar days alone decide nothing: on a Sunday every Friday close is two days
 * old, and before the morning price update every quote is a day old - a flag
 * that says "stale" for all of them says nothing. What does mark a quote stale
 * is a trading day that passed without one. So the age is also counted in the
 * trading days of the instrument's own calendar - the one the application's data
 * quality view uses - that lie strictly between the quote and the valuation
 * date. The valuation date itself does not count: its close cannot be known
 * before the day is over.
 * <p/>
 * One missed trading day is tolerated. Funds publish their price a day late, and
 * a fund valued on a Tuesday morning with Friday's price has missed Monday
 * without anything being wrong.
 */
/* package */ final class QuoteAge
{
    /** how many trading days may pass without a quote before it counts as stale */
    /* package */ static final int TOLERATED_MISSED_TRADING_DAYS = 1;

    /**
     * @param calendarDays
     *            days from the quote to the valuation date, never negative - a
     *            quote dated after the valuation date is younger than it
     * @param missedTradingDays
     *            trading days strictly between the two that have no quote
     */
    /* package */ record Age(long calendarDays, long missedTradingDays)
    {
        /** whether more trading days passed without a quote than are tolerated */
        public boolean isStale()
        {
            return missedTradingDays > TOLERATED_MISSED_TRADING_DAYS;
        }
    }

    private QuoteAge()
    {
    }

    /* package */ static Age of(LocalDate quoteDate, LocalDate valuationDate, TradeCalendar calendar)
    {
        var calendarDays = Math.max(0L, ChronoUnit.DAYS.between(quoteDate, valuationDate));

        var missed = 0L;
        for (var day = quoteDate.plusDays(1); day.isBefore(valuationDate); day = day.plusDays(1))
        {
            if (!calendar.isHoliday(day))
                missed++;
        }

        return new Age(calendarDays, missed);
    }
}
