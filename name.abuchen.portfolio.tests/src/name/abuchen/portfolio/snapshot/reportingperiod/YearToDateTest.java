package name.abuchen.portfolio.snapshot.reportingperiod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.ReportingPeriod.YearToDate;
import name.abuchen.portfolio.util.Interval;

@SuppressWarnings("nls")
public class YearToDateTest
{
    @Test
    public void testContructor() throws IOException
    {
        String code = "X";
        ReportingPeriod period = ReportingPeriod.from(code);

        assertEquals(period.getClass(), YearToDate.class);
    }

    @Test
    public void testWriteTo() throws IOException
    {
        String code = "X";
        StringBuilder strb = new StringBuilder();

        ReportingPeriod period = ReportingPeriod.from(code);
        period.writeTo(strb);

        assertEquals(strb.toString(), code);
    }

    @Test
    public void testToInterval() throws IOException
    {
        LocalDate today = LocalDate.now();
        LocalDate intervalStart = LocalDate.of(today.getYear(), 1, 1).minusDays(1);
        LocalDate intervalEnd = LocalDate.of(today.getYear(), 12, 31);
        ReportingPeriod period = ReportingPeriod.from("X");

        Interval result = period.toInterval(intervalEnd);

        assertEquals(result, Interval.of(intervalStart, intervalEnd));
    }

    @Test
    public void testEquals() throws IOException
    {
        ReportingPeriod equal1 = ReportingPeriod.from("X");
        ReportingPeriod equal2 = ReportingPeriod.from("X");
        ReportingPeriod notEqualDifferentClass = ReportingPeriod.from("T10");

        assertNotEquals(equal1, null);
        assertNotEquals(equal1, notEqualDifferentClass);

        assertEquals(equal1, equal1);
        assertEquals(equal1, equal2);
    }
}
