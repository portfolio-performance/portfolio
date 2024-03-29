package name.abuchen.portfolio.snapshot.reportingperiod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.ReportingPeriod.YearX;
import name.abuchen.portfolio.util.Interval;

@SuppressWarnings("nls")
public class YearXTest
{
    @Test
    public void testContructor() throws IOException
    {
        String code = "Y2019";
        ReportingPeriod period = ReportingPeriod.from(code);

        assertEquals(YearX.class, period.getClass());
    }

    @Test
    public void testWriteTo() throws IOException
    {
        String code = "Y2019";

        ReportingPeriod period = ReportingPeriod.from(code);

        assertEquals(code, period.getCode());
    }

    @Test
    public void testToInterval() throws IOException
    {
        LocalDate intervalStart = LocalDate.of(2018, 12, 31);
        LocalDate intervalEnd = LocalDate.of(2019, 12, 31);
        ReportingPeriod period = ReportingPeriod.from("Y2019");

        // The input of toInterval will be ignored
        Interval result = period.toInterval(LocalDate.of(2020, 12, 31));

        assertEquals(Interval.of(intervalStart, intervalEnd), result);
    }

    @Test
    public void testEquals() throws IOException
    {
        ReportingPeriod equal1 = ReportingPeriod.from("Y2019");
        ReportingPeriod equal2 = ReportingPeriod.from("Y2019");
        ReportingPeriod notEqualSameClass = ReportingPeriod.from("Y2020");
        ReportingPeriod notEqualDifferentClass = ReportingPeriod.from("T10");

        assertNotNull(equal1);
        assertNotEquals(equal1, notEqualSameClass);
        assertNotEquals(equal1, notEqualDifferentClass);

        assertEquals(equal1, equal1);
        assertEquals(equal1, equal2);
    }
}
