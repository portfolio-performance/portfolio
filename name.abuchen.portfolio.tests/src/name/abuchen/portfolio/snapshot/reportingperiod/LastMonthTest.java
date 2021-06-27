package name.abuchen.portfolio.snapshot.reportingperiod;

import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;
import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.ReportingPeriod.LastMonth;
import name.abuchen.portfolio.util.Interval;

@SuppressWarnings("nls")
public class LastMonthTest
{
    @Test
    public void testContructor() throws IOException
    {
        String code = "V";
        ReportingPeriod period = ReportingPeriod.from(code);

        assertEquals(period.getClass(), LastMonth.class);
    }

    @Test
    public void testWriteTo() throws IOException
    {
        String code = "V";
        StringBuilder strb = new StringBuilder();

        ReportingPeriod period = ReportingPeriod.from(code);
        period.writeTo(strb);

        assertEquals(strb.toString(), code);
    }

    @Test
    public void testToInterval() throws IOException
    {
        LocalDate today = LocalDate.now();
        
        LocalDate startMonth = today.minusMonths(1).with(firstDayOfMonth());
        LocalDate endMonth = today.minusMonths(1).with(lastDayOfMonth());
        
        LocalDate intervalStart = startMonth.minusDays(1);
        LocalDate intervalEnd = endMonth;

        ReportingPeriod period = ReportingPeriod.from("V");

        Interval result = period.toInterval(today);

        assertEquals(result, Interval.of(intervalStart, intervalEnd));
    }

    @Test
    public void testEquals() throws IOException
    {
        ReportingPeriod equal1 = ReportingPeriod.from("V");
        ReportingPeriod equal2 = ReportingPeriod.from("V");
        ReportingPeriod notEqualDifferentClass = ReportingPeriod.from("T10");

        assertNotEquals(equal1, null);
        assertNotEquals(equal1, notEqualDifferentClass);

        assertEquals(equal1, equal1);
        assertEquals(equal1, equal2);
    }
}
