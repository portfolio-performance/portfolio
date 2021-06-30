package name.abuchen.portfolio.snapshot.reportingperiod;

import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.ReportingPeriod.LastXTradingDays;
import name.abuchen.portfolio.snapshot.ReportingPeriod.LastYear;
import name.abuchen.portfolio.snapshot.ReportingPeriodType;
import name.abuchen.portfolio.util.Interval;

public class LastYearTest
{
    @Test
    public void testSerializationDeserializationRoundtrip() throws IOException
    {
        ReportingPeriod period = new LastYear();

        StringBuilder strb = new StringBuilder();
        period.writeTo(strb);
        String serialized = strb.toString();
        assertTrue(serialized.contains(ReportingPeriodType.PREVIOUS_YEAR.name())); //NOSONAR

        ReportingPeriod deserialized = ReportingPeriod.from(serialized);
        assertEquals(deserialized, period);
    }

    @Test
    public void testToInterval()
    {
        ReportingPeriod period = new LastYear();
        
        LocalDate today = LocalDate.now();
        
        LocalDate firstDayOfLastYear = today.withDayOfMonth(1).withMonth(1).minusYears(1);
        LocalDate lastDayOfLastYear = firstDayOfLastYear.withMonth(12).with(lastDayOfMonth());

        LocalDate intervalStart = firstDayOfLastYear.minusDays(1);
        LocalDate intervalEnd = lastDayOfLastYear;

        Interval result = period.toInterval(today);

        assertEquals(result, Interval.of(intervalStart, intervalEnd));
    }

    @Test
    public void testEquals()
    {
        ReportingPeriod equal1 = new LastYear();
        ReportingPeriod equal2 = new LastYear();
        ReportingPeriod notEqualDifferentClass = new LastXTradingDays(10);

        assertNotEquals(equal1, null);
        assertNotEquals(equal1, notEqualDifferentClass);

        assertEquals(equal1, equal1);
        assertEquals(equal1, equal2);
    }
}
