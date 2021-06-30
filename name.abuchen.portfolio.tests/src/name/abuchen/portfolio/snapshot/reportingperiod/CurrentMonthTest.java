package name.abuchen.portfolio.snapshot.reportingperiod;

import static name.abuchen.portfolio.snapshot.ReportingPeriodType.CURRENT_MONTH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.ReportingPeriod.CurrentMonth;
import name.abuchen.portfolio.snapshot.ReportingPeriod.PreviousXTradingDays;
import name.abuchen.portfolio.util.Interval;

public class CurrentMonthTest
{
    @Test
    public void testLegacyContructor() throws IOException
    {
        String code = "M";
        ReportingPeriod period = ReportingPeriod.from(code);

        assertEquals(period, new CurrentMonth()); // NOSONAR
    }

    @Test
    public void testSerializationDeserializationRoundtrip() throws IOException
    {
        ReportingPeriod period = new CurrentMonth();

        StringBuilder strb = new StringBuilder();
        period.writeTo(strb);
        String serialized = strb.toString();
        assertTrue(serialized.contains(CURRENT_MONTH.name()));

        ReportingPeriod deserialized = ReportingPeriod.from(serialized);
        assertEquals(deserialized, period);
    }
    
    @Test
    public void testToInterval()
    {
        ReportingPeriod period = new CurrentMonth();
        
        LocalDate today = LocalDate.now();
        LocalDate intervalStart = LocalDate.of(today.getYear(), today.getMonthValue(), 1).minusDays(1);
        LocalDate intervalEnd = LocalDate.of(today.getYear(), today.getMonthValue(), today.lengthOfMonth());

        Interval result = period.toInterval(intervalEnd);

        assertEquals(result, Interval.of(intervalStart, intervalEnd));
    }

    @Test
    public void testEquals()
    {
        ReportingPeriod equal1 = new CurrentMonth();
        ReportingPeriod equal2 = new CurrentMonth();
        ReportingPeriod notEqualDifferentClass = new PreviousXTradingDays(10);

        assertNotEquals(equal1, null);
        assertNotEquals(equal1, notEqualDifferentClass);

        assertEquals(equal1, equal1);
        assertEquals(equal1, equal2);
    }
}
