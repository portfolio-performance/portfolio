package name.abuchen.portfolio.snapshot.reportingperiod;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.snapshot.ReportingPeriod.LastXTradingDays;
import name.abuchen.portfolio.util.Interval;
import name.abuchen.portfolio.util.TradeCalendarManager;

@SuppressWarnings("nls")
public class LastXTradingDaysIntervalTest
{
    @Test
    public void testCachedIntervalMatchesCalculation()
    {
        var period = new LastXTradingDays(10);

        var start = LocalDate.parse("2015-11-01");
        for (int ii = 0; ii < 500; ii++)
        {
            // alternate between two dates to cover cache hits and misses
            var date = start.plusDays(ii / 2 + (ii % 2) * 7L);

            var expected = Interval.of(LastXTradingDays.tradingDaysUntil(date, 10), date);

            assertThat(period.toInterval(date), is(expected));
            assertThat(period.toInterval(date), is(expected));
        }
    }

    @Test
    public void testIntervalIsCachedForSameDate()
    {
        var period = new LastXTradingDays(5);
        var date = LocalDate.parse("2016-07-08");

        var first = period.toInterval(date);
        assertThat(period.toInterval(date), is(sameInstance(first)));
    }

    @Test
    public void testIntervalIsRecalculatedIfDefaultCalendarChanges()
    {
        var period = new LastXTradingDays(1);

        // 2016-07-04 is a trading day in the default calendar, but a holiday
        // (Independence Day) at the NYSE
        var date = LocalDate.parse("2016-07-05");

        try
        {
            TradeCalendarManager.setDefaultCalendarCode("default");
            assertThat(period.toInterval(date).getStart(), is(LocalDate.parse("2016-07-04")));

            TradeCalendarManager.setDefaultCalendarCode("nyse");
            assertThat(period.toInterval(date).getStart(), is(LocalDate.parse("2016-07-01")));
        }
        finally
        {
            TradeCalendarManager.setDefaultCalendarCode("default");
        }
    }
}
