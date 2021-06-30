package name.abuchen.portfolio.snapshot.reportingperiod;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.ReportingPeriod.PreviousXTradingDays;
import name.abuchen.portfolio.snapshot.ReportingPeriodType;
import name.abuchen.portfolio.util.Interval;

public class PreviousXTradingDaysTest
{

    @Test
    public void testTradingDaysMonday()
    {
        assertThat(PreviousXTradingDays.tradingDaysUntil(LocalDate.parse("2016-07-08"), 1), //$NON-NLS-1$
                        is(LocalDate.parse("2016-07-07"))); //$NON-NLS-1$

        assertThat(PreviousXTradingDays.tradingDaysUntil(LocalDate.parse("2016-07-09"), 1), //$NON-NLS-1$
                        is(LocalDate.parse("2016-07-07"))); //$NON-NLS-1$

        assertThat(PreviousXTradingDays.tradingDaysUntil(LocalDate.parse("2016-07-10"), 1), //$NON-NLS-1$
                        is(LocalDate.parse("2016-07-07"))); //$NON-NLS-1$

        assertThat(PreviousXTradingDays.tradingDaysUntil(LocalDate.parse("2016-07-11"), 1), //$NON-NLS-1$
                        is(LocalDate.parse("2016-07-08"))); //$NON-NLS-1$

        assertThat(PreviousXTradingDays.tradingDaysUntil(LocalDate.parse("2016-07-12"), 1), //$NON-NLS-1$
                        is(LocalDate.parse("2016-07-11"))); //$NON-NLS-1$
    }

    @Test
    public void testLegacyContructor() throws IOException
    {
        String code = "T10";
        ReportingPeriod period = ReportingPeriod.from(code);

        assertEquals(period, new PreviousXTradingDays(10)); //NOSONAR
    }

    @Test
    public void testSerializationDeserializationRoundtrip() throws IOException
    {
        ReportingPeriod period = new PreviousXTradingDays(90);

        StringBuilder strb = new StringBuilder();
        period.writeTo(strb);
        String serialized = strb.toString();
        assertTrue(serialized.contains(ReportingPeriodType.PREVIOUS_X_TRADING_DAYS.name())); //NOSONAR

        ReportingPeriod deserialized = ReportingPeriod.from(serialized);
        assertEquals(deserialized, period);
    }
    
    @Test
    public void testToInterval()
    {
        ReportingPeriod period = new PreviousXTradingDays(10);
        
        LocalDate intervalStart = LocalDate.of(2020, 03, 20);
        LocalDate intervalEnd = LocalDate.of(2020, 04, 04);

        Interval result = period.toInterval(intervalEnd);

        assertEquals(result, Interval.of(intervalStart, intervalEnd));
    }

    @Test
    public void testEquals()
    {
        ReportingPeriod equal1 = new PreviousXTradingDays(90);
        ReportingPeriod equal2 = new PreviousXTradingDays(90);
        ReportingPeriod notEqualSameClass = new PreviousXTradingDays(91);
        ReportingPeriod notEqualDifferentClass = new PreviousXTradingDays(10);

        assertNotEquals(equal1, null);
        assertNotEquals(equal1, notEqualSameClass);
        assertNotEquals(equal1, notEqualDifferentClass);

        assertEquals(equal1, equal1);
        assertEquals(equal1, equal2);
    }
}
