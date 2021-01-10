package name.abuchen.portfolio.snapshot.reportingperiod;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.ReportingPeriod.LastXTradingDays;
import name.abuchen.portfolio.util.Interval;

@SuppressWarnings("nls")
public class LastXTradingDaysTest
{

    @Test
    public void testTradingDaysMonday()
    {
        assertThat(LastXTradingDays.tradingDaysUntil(LocalDate.parse("2016-07-08"), 1), //$NON-NLS-1$
                        is(LocalDate.parse("2016-07-07"))); //$NON-NLS-1$

        assertThat(LastXTradingDays.tradingDaysUntil(LocalDate.parse("2016-07-09"), 1), //$NON-NLS-1$
                        is(LocalDate.parse("2016-07-07"))); //$NON-NLS-1$

        assertThat(LastXTradingDays.tradingDaysUntil(LocalDate.parse("2016-07-10"), 1), //$NON-NLS-1$
                        is(LocalDate.parse("2016-07-07"))); //$NON-NLS-1$

        assertThat(LastXTradingDays.tradingDaysUntil(LocalDate.parse("2016-07-11"), 1), //$NON-NLS-1$
                        is(LocalDate.parse("2016-07-08"))); //$NON-NLS-1$

        assertThat(LastXTradingDays.tradingDaysUntil(LocalDate.parse("2016-07-12"), 1), //$NON-NLS-1$
                        is(LocalDate.parse("2016-07-11"))); //$NON-NLS-1$
    }

    @Test
    public void testContructor() throws IOException
    {
        String code = "T10";
        ReportingPeriod period = ReportingPeriod.from(code);

        assertEquals(period.getClass(), LastXTradingDays.class);
    }

    @Test
    public void testWriteTo() throws IOException
    {
        String code = "T10";
        StringBuilder strb = new StringBuilder();

        ReportingPeriod period = ReportingPeriod.from(code);
        period.writeTo(strb);

        assertEquals(strb.toString(), code);
    }

    @Test
    public void testToInterval() throws IOException
    {
        LocalDate intervalStart = LocalDate.of(2020, 03, 20);
        LocalDate intervalEnd = LocalDate.of(2020, 04, 04);
        ReportingPeriod period = ReportingPeriod.from("T10");

        Interval result = period.toInterval(intervalEnd);

        assertEquals(result, Interval.of(intervalStart, intervalEnd));
    }

    @Test
    public void testEquals() throws IOException
    {
        ReportingPeriod equal1 = ReportingPeriod.from("T10");
        ReportingPeriod equal2 = ReportingPeriod.from("T10");
        ReportingPeriod notEqualSameClass = ReportingPeriod.from("T11");
        ReportingPeriod notEqualDifferentClass = ReportingPeriod.from("D90");

        assertNotEquals(equal1, null);
        assertNotEquals(equal1, notEqualSameClass);
        assertNotEquals(equal1, notEqualDifferentClass);

        assertEquals(equal1, equal1);
        assertEquals(equal1, equal2);
    }
}
