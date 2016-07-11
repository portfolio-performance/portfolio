package name.abuchen.portfolio.snapshot;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.snapshot.ReportingPeriod.LastXTradingDays;

public class ReportingPeriodTest
{
    @Test
    public void testTradingDaysMonday()
    {
        assertThat(LastXTradingDays.tradingDaysSince(LocalDate.parse("2016-07-08"), 1), //$NON-NLS-1$
                        is(LocalDate.parse("2016-07-07"))); //$NON-NLS-1$

        assertThat(LastXTradingDays.tradingDaysSince(LocalDate.parse("2016-07-09"), 1), //$NON-NLS-1$
                        is(LocalDate.parse("2016-07-07"))); //$NON-NLS-1$

        assertThat(LastXTradingDays.tradingDaysSince(LocalDate.parse("2016-07-10"), 1), //$NON-NLS-1$
                        is(LocalDate.parse("2016-07-07"))); //$NON-NLS-1$

        assertThat(LastXTradingDays.tradingDaysSince(LocalDate.parse("2016-07-11"), 1), //$NON-NLS-1$
                        is(LocalDate.parse("2016-07-08"))); //$NON-NLS-1$

        assertThat(LastXTradingDays.tradingDaysSince(LocalDate.parse("2016-07-12"), 1), //$NON-NLS-1$
                        is(LocalDate.parse("2016-07-11"))); //$NON-NLS-1$

    }

}
