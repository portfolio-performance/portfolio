package name.abuchen.portfolio.snapshot.reportingperiod;

import static java.time.temporal.IsoFields.DAY_OF_QUARTER;
import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.ReportingPeriod.PreviousQuarter;
import name.abuchen.portfolio.snapshot.ReportingPeriod.PreviousXTradingDays;
import name.abuchen.portfolio.snapshot.ReportingPeriodType;
import name.abuchen.portfolio.util.Interval;

public class PreviousQuarterTest
{
    @Test
    public void testSerializationDeserializationRoundtrip() throws IOException
    {
        ReportingPeriod period = new PreviousQuarter();

        StringBuilder strb = new StringBuilder();
        period.writeTo(strb);
        String serialized = strb.toString();
        assertTrue(serialized.contains(ReportingPeriodType.PREVIOUS_QUARTER.name())); //NOSONAR

        ReportingPeriod deserialized = ReportingPeriod.from(serialized);
        assertEquals(deserialized, period);
    }

    @Test
    public void testToInterval()
    {
        ReportingPeriod period = new PreviousQuarter();
        
        LocalDate today = LocalDate.now();
        
        LocalDate firstDayOfCurrentQuarter = today.with(DAY_OF_QUARTER, 1L);
        
        LocalDate firstDayOfLastQuarter = firstDayOfCurrentQuarter.minusMonths(3);
        LocalDate lastDayOfLastQuarter = firstDayOfLastQuarter.plusMonths(2).with(lastDayOfMonth());

        LocalDate intervalStart = firstDayOfLastQuarter.minusDays(1);
        LocalDate intervalEnd = lastDayOfLastQuarter;

        Interval result = period.toInterval(today);

        assertEquals(result, Interval.of(intervalStart, intervalEnd));
    }

    @Test
    public void testEquals()
    {
        ReportingPeriod equal1 = new PreviousQuarter();
        ReportingPeriod equal2 = new PreviousQuarter();
        ReportingPeriod notEqualDifferentClass = new PreviousXTradingDays(10);

        assertNotEquals(equal1, null);
        assertNotEquals(equal1, notEqualDifferentClass);

        assertEquals(equal1, equal1);
        assertEquals(equal1, equal2);
    }
}
