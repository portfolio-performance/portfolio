package name.abuchen.portfolio.snapshot.reportingperiod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.ReportingPeriod.SinceX;
import name.abuchen.portfolio.util.Interval;

@SuppressWarnings("nls")
public class SinceXTest
{
    @Test
    public void testContructor() throws IOException
    {
        String code = "S2020-04-04";
        ReportingPeriod period = ReportingPeriod.from(code);

        assertEquals(period.getClass(), SinceX.class);
    }

    @Test
    public void testWriteTo() throws IOException
    {
        String code = "S2020-04-04";

        ReportingPeriod period = ReportingPeriod.from(code);

        assertEquals(period.getCode(), code);
    }

    @Test
    public void testToInterval() throws IOException
    {
        LocalDate intervalStart = LocalDate.of(2020, 4, 4);
        LocalDate intervalEnd = LocalDate.of(2020, 4, 8);
        ReportingPeriod period = ReportingPeriod.from("S2020-04-04");

        Interval result = period.toInterval(intervalEnd);

        assertEquals(result, Interval.of(intervalStart, intervalEnd));
    }

    @Test
    public void testEquals() throws IOException
    {
        ReportingPeriod equal1 = ReportingPeriod.from("S2020-04-04");
        ReportingPeriod equal2 = ReportingPeriod.from("S2020-04-04");
        ReportingPeriod notEqualSameClass = ReportingPeriod.from("S2020-04-05");
        ReportingPeriod notEqualDifferentClass = ReportingPeriod.from("T10");

        assertNotEquals(equal1, null);
        assertNotEquals(equal1, notEqualSameClass);
        assertNotEquals(equal1, notEqualDifferentClass);

        assertEquals(equal1, equal1);
        assertEquals(equal1, equal2);
    }
}
