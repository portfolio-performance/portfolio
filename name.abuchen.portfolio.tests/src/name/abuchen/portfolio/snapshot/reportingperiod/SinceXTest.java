package name.abuchen.portfolio.snapshot.reportingperiod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.ReportingPeriod.LastXTradingDays;
import name.abuchen.portfolio.snapshot.ReportingPeriod.SinceX;
import name.abuchen.portfolio.snapshot.ReportingPeriodType;
import name.abuchen.portfolio.util.Interval;

public class SinceXTest
{
    @Test
    public void testLegacyContructor() throws IOException
    {
        String code = "S2020-04-04";
        ReportingPeriod period = ReportingPeriod.from(code);

        assertEquals(period, new SinceX(LocalDate.of(2020, 4, 4))); //NOSONAR
    }

    @Test
    public void testSerializationDeserializationRoundtrip() throws IOException
    {
        LocalDate startDate = LocalDate.of(2020, 4, 4);
        ReportingPeriod period = new SinceX(startDate);

        StringBuilder strb = new StringBuilder();
        period.writeTo(strb);
        String serialized = strb.toString();
        assertTrue(serialized.contains(ReportingPeriodType.SINCE_X.name()));

        ReportingPeriod deserialized = ReportingPeriod.from(serialized);
        assertEquals(deserialized, period);
    }
    
    @Test
    public void testToInterval()
    {
        LocalDate intervalStart = LocalDate.of(2020, 4, 4);
        LocalDate intervalEnd = LocalDate.of(2020, 4, 8);
        
        ReportingPeriod period = new SinceX(intervalStart);

        Interval result = period.toInterval(intervalEnd);

        assertEquals(result, Interval.of(intervalStart, intervalEnd));
    }

    @Test
    public void testEquals()
    {
        LocalDate startDate = LocalDate.of(2020, 4, 4);
        ReportingPeriod equal1 = new SinceX(startDate);
        ReportingPeriod equal2 = new SinceX(startDate);
        ReportingPeriod notEqualSameClass = new SinceX(startDate.plusDays(1));
        ReportingPeriod notEqualDifferentClass = new LastXTradingDays(10);

        assertNotEquals(equal1, null);
        assertNotEquals(equal1, notEqualSameClass);
        assertNotEquals(equal1, notEqualDifferentClass);

        assertEquals(equal1, equal1);
        assertEquals(equal1, equal2);
    }
}
