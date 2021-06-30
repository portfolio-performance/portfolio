package name.abuchen.portfolio.snapshot.reportingperiod;

import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.ReportingPeriod.CurrentQuarter;
import name.abuchen.portfolio.snapshot.ReportingPeriod.LastXTradingDays;
import name.abuchen.portfolio.snapshot.ReportingPeriodType;
import name.abuchen.portfolio.util.Interval;

public class CurrentQuarterTest
{
    @Test
    public void testLegacyContructor() throws IOException
    {
        String code = "Q";
        ReportingPeriod period = ReportingPeriod.from(code);

        assertEquals(period.getClass(), CurrentQuarter.class); // NOSONAR
    }

    @Test
    public void testSerializationDeserializationRoundtrip() throws IOException
    {
        ReportingPeriod period = new CurrentQuarter();

        StringBuilder strb = new StringBuilder();
        period.writeTo(strb);
        String serialized = strb.toString();
        assertTrue(serialized.contains(ReportingPeriodType.CURRENT_QUARTER.name()));

        ReportingPeriod deserialized = ReportingPeriod.from(serialized);
        assertEquals(deserialized, period);
    }
    
    @Test
    public void testToIntervalForQ1()
    {
        LocalDate dateQ1 = LocalDate.of(2021, 2, 10);

        ReportingPeriod period = new CurrentQuarter();

        Interval result = period.toInterval(dateQ1);

        LocalDate expectedStartDate = LocalDate.of(2020, 12, 31);
        LocalDate expectedEndDate = LocalDate.of(2021, 3, 1).with(lastDayOfMonth());
        assertEquals(result, Interval.of(expectedStartDate, expectedEndDate));
    }

    @Test
    public void testToIntervalForQ2()
    {
        ReportingPeriod period = new CurrentQuarter();
        
        LocalDate dateQ2 = LocalDate.of(2021, 5, 10);

        Interval result = period.toInterval(dateQ2);

        LocalDate expectedStartDate = LocalDate.of(2021, 3, 31);
        LocalDate expectedEndDate = LocalDate.of(2021, 6, 1).with(lastDayOfMonth());
        assertEquals(result, Interval.of(expectedStartDate, expectedEndDate));
    }

    @Test
    public void testToIntervalForQ3()
    {
        ReportingPeriod period = new CurrentQuarter();
        
        LocalDate dateQ3 = LocalDate.of(2021, 8, 10);

        Interval result = period.toInterval(dateQ3);

        LocalDate expectedStartDate = LocalDate.of(2021, 6, 30);
        LocalDate expectedEndDate = LocalDate.of(2021, 9, 1).with(lastDayOfMonth());
        assertEquals(result, Interval.of(expectedStartDate, expectedEndDate));
    }

    @Test
    public void testToIntervalForQ4()
    {
        ReportingPeriod period = new CurrentQuarter();
        
        LocalDate dateQ4 = LocalDate.of(2021, 10, 10);

        Interval result = period.toInterval(dateQ4);

        LocalDate expectedStartDate = LocalDate.of(2021, 9, 30);
        LocalDate expectedEndDate = LocalDate.of(2021, 12, 1).with(lastDayOfMonth());
        assertEquals(result, Interval.of(expectedStartDate, expectedEndDate));
    }

    @Test
    public void testEquals()
    {
        ReportingPeriod equal1 = new CurrentQuarter();
        ReportingPeriod equal2 = new CurrentQuarter();
        ReportingPeriod notEqualDifferentClass = new LastXTradingDays(10);

        assertNotEquals(equal1, null);
        assertNotEquals(equal1, notEqualDifferentClass);

        assertEquals(equal1, equal1);
        assertEquals(equal1, equal2);
    }
}
