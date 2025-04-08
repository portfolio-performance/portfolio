package name.abuchen.portfolio.snapshot.reportingperiod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

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

        assertEquals(YearToDate.class, period.getClass());
    }

    @Test
    public void testWriteTo() throws IOException
    {
        String code = "X";

        ReportingPeriod period = ReportingPeriod.from(code);

        assertEquals(code, period.getCode());
    }

    @Test
    public void testToInterval() throws IOException
    {
        LocalDate today = LocalDate.now();
        LocalDate intervalStart = LocalDate.of(today.getYear(), 1, 1).minusDays(1);
        LocalDate intervalEnd = LocalDate.of(today.getYear(), 12, 31);
        ReportingPeriod period = ReportingPeriod.from("X");

        Interval result = period.toInterval(intervalEnd);

        assertEquals(Interval.of(intervalStart, intervalEnd), result);
    }

    @Test
    public void testEquals() throws IOException
    {
        ReportingPeriod equal1 = ReportingPeriod.from("X");
        ReportingPeriod equal2 = ReportingPeriod.from("X");
        ReportingPeriod notEqualDifferentClass = ReportingPeriod.from("T10");

        assertNotNull(equal1);
        assertNotEquals(equal1, notEqualDifferentClass);

        assertEquals(equal1, equal1);
        assertEquals(equal1, equal2);
    }
}
