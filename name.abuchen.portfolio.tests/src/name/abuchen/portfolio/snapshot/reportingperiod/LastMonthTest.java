package name.abuchen.portfolio.snapshot.reportingperiod;

import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;
import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.ReportingPeriod.LastMonth;
import name.abuchen.portfolio.snapshot.ReportingPeriod.LastXTradingDays;
import name.abuchen.portfolio.snapshot.ReportingPeriodType;
import name.abuchen.portfolio.util.Interval;

public class LastMonthTest
{
    @Test
    public void testSerializationDeserializationRoundtrip() throws IOException
    {
        ReportingPeriod period = new LastMonth();

        StringBuilder strb = new StringBuilder();
        period.writeTo(strb);
        String serialized = strb.toString();
        assertTrue(serialized.contains(ReportingPeriodType.PREVIOUS_MONTH.name())); //NOSONAR

        ReportingPeriod deserialized = ReportingPeriod.from(serialized);
        assertEquals(deserialized, period);
    }

    @Test
    public void testToInterval()
    {
        ReportingPeriod period = new LastMonth();
        
        LocalDate today = LocalDate.now();
        
        LocalDate startMonth = today.minusMonths(1).with(firstDayOfMonth());
        LocalDate endMonth = today.minusMonths(1).with(lastDayOfMonth());
        
        LocalDate intervalStart = startMonth.minusDays(1);
        LocalDate intervalEnd = endMonth;

        Interval result = period.toInterval(today);

        assertEquals(result, Interval.of(intervalStart, intervalEnd));
    }

    @Test
    public void testEquals()
    {
        ReportingPeriod equal1 = new LastMonth();
        ReportingPeriod equal2 = new LastMonth();
        ReportingPeriod notEqualDifferentClass = new LastXTradingDays(10);

        assertNotEquals(equal1, null);
        assertNotEquals(equal1, notEqualDifferentClass);

        assertEquals(equal1, equal1);
        assertEquals(equal1, equal2);
    }
}
