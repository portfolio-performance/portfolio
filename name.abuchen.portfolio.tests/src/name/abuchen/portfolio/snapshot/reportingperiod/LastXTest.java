package name.abuchen.portfolio.snapshot.reportingperiod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.ReportingPeriod.LastX;
import name.abuchen.portfolio.util.Interval;

@SuppressWarnings("nls")
public class LastXTest
{
    @Test
    public void testContructor() throws IOException
    {
        String code = "L2Y6";
        ReportingPeriod period = ReportingPeriod.from(code);

        assertEquals(period.getClass(), LastX.class);
    }

    @Test
    public void testWriteTo() throws IOException
    {
        String code = "L2Y6";

        ReportingPeriod period = ReportingPeriod.from(code);

        assertEquals(period.getCode(), code);
    }

    @Test
    public void testToInterval() throws IOException
    {
        LocalDate intervalStart = LocalDate.of(2019, 03, 04);
        LocalDate intervalEnd = LocalDate.of(2020, 04, 04);
        ReportingPeriod period = ReportingPeriod.from("L1Y1");

        Interval result = period.toInterval(intervalEnd);

        assertEquals(result, Interval.of(intervalStart, intervalEnd));
    }

    @Test
    public void testEquals() throws IOException
    {
        ReportingPeriod equal1 = ReportingPeriod.from("L2Y0");
        ReportingPeriod equal2 = ReportingPeriod.from("L2Y0");
        ReportingPeriod notEqualSameClass = ReportingPeriod.from("L2Y1");
        ReportingPeriod notEqualDifferentClass = ReportingPeriod.from("T10");

        assertNotEquals(equal1, null);
        assertNotEquals(equal1, notEqualSameClass);
        assertNotEquals(equal1, notEqualDifferentClass);

        assertEquals(equal1, equal1);
        assertEquals(equal1, equal2);
    }

    @Test
    public void testBackwardsCompability() throws IOException
    {
        String code = "2Y";
        ReportingPeriod period = ReportingPeriod.from(code);

        assertEquals(period.getClass(), LastX.class);
    }

}
