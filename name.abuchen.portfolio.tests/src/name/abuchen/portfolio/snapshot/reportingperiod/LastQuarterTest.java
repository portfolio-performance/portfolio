package name.abuchen.portfolio.snapshot.reportingperiod;

import static java.time.temporal.IsoFields.DAY_OF_QUARTER;
import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.ReportingPeriod.LastQuarter;
import name.abuchen.portfolio.util.Interval;

@SuppressWarnings("nls")
public class LastQuarterTest
{
    @Test
    public void testContructor() throws IOException
    {
        String code = "B";
        ReportingPeriod period = ReportingPeriod.from(code);

        assertEquals(period.getClass(), LastQuarter.class);
    }

    @Test
    public void testWriteTo() throws IOException
    {
        String code = "B";
        StringBuilder strb = new StringBuilder();

        ReportingPeriod period = new ReportingPeriod.LastQuarter();
        period.writeTo(strb);

        assertEquals(strb.toString(), code);
    }

    @Test
    public void testToInterval() throws IOException
    {
        LocalDate today = LocalDate.now();
        
        LocalDate firstDayOfCurrentQuarter = today.with(DAY_OF_QUARTER, 1L);
        
        LocalDate firstDayOfLastQuarter = firstDayOfCurrentQuarter.minusMonths(3);
        LocalDate lastDayOfLastQuarter = firstDayOfLastQuarter.plusMonths(2).with(lastDayOfMonth());

        LocalDate intervalStart = firstDayOfLastQuarter.minusDays(1);
        LocalDate intervalEnd = lastDayOfLastQuarter;
        
        ReportingPeriod period = ReportingPeriod.from("B");

        Interval result = period.toInterval(today);

        assertEquals(result, Interval.of(intervalStart, intervalEnd));
    }

    @Test
    public void testEquals() throws IOException
    {
        ReportingPeriod equal1 = ReportingPeriod.from("B");
        ReportingPeriod equal2 = ReportingPeriod.from("B");
        ReportingPeriod notEqualDifferentClass = ReportingPeriod.from("T10");

        assertNotEquals(equal1, null);
        assertNotEquals(equal1, notEqualDifferentClass);

        assertEquals(equal1, equal1);
        assertEquals(equal1, equal2);
    }
}
