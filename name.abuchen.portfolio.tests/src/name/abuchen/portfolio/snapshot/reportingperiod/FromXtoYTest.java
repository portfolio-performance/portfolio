package name.abuchen.portfolio.snapshot.reportingperiod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.ReportingPeriod.FromXtoY;
import name.abuchen.portfolio.snapshot.ReportingPeriod.PreviousXTradingDays;
import name.abuchen.portfolio.snapshot.ReportingPeriodType;
import name.abuchen.portfolio.util.Interval;

public class FromXtoYTest
{
    @Test
    public void testLegacyContructor() throws IOException
    {
        String code = "F2020-04-04_2020-04-08";
        ReportingPeriod period = ReportingPeriod.from(code);

        assertEquals(period, new FromXtoY(LocalDate.of(2020, 4, 4), LocalDate.of(2020, 4, 8))); //NOSONAR
    }
    
    @Test
    public void testSerializationDeserializationRoundtrip() throws IOException
    {
        LocalDate startDate = LocalDate.of(2020, 4, 4);
        LocalDate endDate = LocalDate.of(2020, 4, 8);
        ReportingPeriod period = new FromXtoY(startDate, endDate);

        StringBuilder strb = new StringBuilder();
        period.writeTo(strb);
        String serialized = strb.toString();
        assertTrue(serialized.contains(ReportingPeriodType.FROM_X_TO_Y.name()));

        ReportingPeriod deserialized = ReportingPeriod.from(serialized);
        assertEquals(deserialized, period);
    }

    @Test
    public void testToInterval()
    {
        LocalDate intervalStart = LocalDate.of(2020, 4, 4);
        LocalDate intervalEnd = LocalDate.of(2020, 4, 8);
        ReportingPeriod period = new FromXtoY(intervalStart, intervalEnd);

        Interval result = period.toInterval(intervalEnd);

        assertEquals(result, Interval.of(intervalStart, intervalEnd));
    }

    @Test
    public void testEquals()
    {
        LocalDate startDate = LocalDate.of(2020, 4, 4);
        LocalDate endDate = LocalDate.of(2020, 4, 8);
        
        ReportingPeriod equal1 = new FromXtoY(startDate, endDate);
        ReportingPeriod equal2 = new FromXtoY(startDate, endDate);
        ReportingPeriod notEqualSameClass = new FromXtoY(startDate, endDate.plusDays(1));
        ReportingPeriod notEqualDifferentClass = new PreviousXTradingDays(10);

        assertNotEquals(equal1, null);
        assertNotEquals(equal1, notEqualSameClass);
        assertNotEquals(equal1, notEqualDifferentClass);

        assertEquals(equal1, equal1);
        assertEquals(equal1, equal2);
    }
}
